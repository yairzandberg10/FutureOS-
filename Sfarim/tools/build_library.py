#!/usr/bin/env python3
"""
Builds sefaria.db: a single offline SQLite library of every Hebrew text on
Sefaria (sefaria.org), for the FutureOS Sfarim app.

Pulls the category/book table of contents from the Sefaria API and the actual
text content (best available Hebrew "merged" version per title) from the
public Sefaria-Export GCS bucket. Resumable: re-running skips books already
fully imported (checked via segment count in the existing DB).

Two kinds of books come out of the export. Simple ones have `text` as a nested
list, numbered by the book's sectionNames (Chapter/Verse, Siman/Seif...).
Complex ones (about 875 titles, mostly Jewish Thought, Chasidut, Liturgy,
Kabbalah and the big halakhic works) have `text` as a dict of named parts,
e.g. Arukh HaShulchan -> Orach Chaim / Yoreh De'ah / ..., possibly nested
several levels (Siddur -> Weekday -> Shacharit -> Modeh Ani). The export's
schema lists those parts by title only, so their sectionNames come from the
Sefaria index API. Each numbered unit of a part becomes one chapter with a
book-wide sequential top_index, and its display label plus the part titles
leading to it go into the `chapters` table - the app can't derive either from
section_names the way it does for simple books.

Texts licensed "Copyright: ..." (the Koren Steinsaltz Tanakh and Mishneh
Torah, Urim titles) are left out of the public export on purpose and are not
fetched from anywhere else.

Usage:
    python build_library.py [--out PATH] [--workers N] [--limit N] [--with-fts]
"""

import argparse
import hashlib
import json
import re
import sqlite3
import sys
import time
import urllib.request
import urllib.error
import urllib.parse
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

TOC_URL = "https://www.sefaria.org/api/index"
BOOKS_JSON_URL = "https://raw.githubusercontent.com/Sefaria/Sefaria-Export/master/books.json"
RAW_INDEX_URL = "https://www.sefaria.org/api/v2/raw/index/"
REQUEST_TIMEOUT = 20
MAX_RETRIES = 3

HEBREW_SECTION_NAMES = {
    "Chapter": "פרק", "Verse": "פסוק", "Daf": "דף", "Line": "שורה",
    "Halakhah": "הלכה", "Siman": "סימן", "Seif": "סעיף", "Se'if": "סעיף",
    "Section": "סימן", "Paragraph": "פסקה", "Mishnah": "משנה",
    "Chelek": "חלק", "Sha'ar": "שער", "Perek": "פרק", "Pasuk": "פסוק",
    "Comment": "ביאור", "Letter": "אות", "Seif Katan": "סעיף קטן",
    "Piska": "פסקה", "Remez": "רמז", "Year": "שנה", "Responsum": "תשובה",
    "Volume": "כרך", "Page": "עמוד", "Book": "ספר", "Part": "חלק",
    "Gate": "שער", "Topic": "נושא", "Passage": "קטע", "Teaching": "לימוד",
}

HEBREW_LETTERS = [
    (1000, "א'"), (900, "תתק"), (400, "ת"), (300, "ש"), (200, "ר"), (100, "ק"),
    (90, "צ"), (80, "פ"), (70, "ע"), (60, "ס"), (50, "נ"), (40, "מ"), (30, "ל"),
    (20, "כ"), (19, "יט"), (18, "יח"), (17, "יז"), (16, "טז"), (15, "טו"),
    (10, "י"), (9, "ט"), (8, "ח"), (7, "ז"), (6, "ו"), (5, "ה"), (4, "ד"),
    (3, "ג"), (2, "ב"), (1, "א"),
]


def to_hebrew_numeral(n: int) -> str:
    """1-based index -> Hebrew gematria numeral (טו/טז special-cased to avoid spelling the Name)."""
    if n <= 0:
        return str(n)
    if n > 999:
        return str(n)
    result = []
    remaining = n
    while remaining > 0:
        for value, letters in HEBREW_LETTERS:
            if remaining >= value:
                result.append(letters)
                remaining -= value
                break
    return "".join(result)


def fetch_json(url: str, retries: int = MAX_RETRIES):
    # books.json ships GCS URLs with literal spaces/unicode in the path (fine for
    # gsutil/gcloud, not for urllib) - percent-encode the path, leave scheme/query intact.
    safe_url = urllib.parse.quote(url, safe=":/?&=%")
    last_err = None
    for attempt in range(retries):
        try:
            req = urllib.request.Request(safe_url, headers={"User-Agent": "Sfarim-FutureOS/1.0"})
            with urllib.request.urlopen(req, timeout=REQUEST_TIMEOUT) as resp:
                return json.loads(resp.read().decode("utf-8"))
        except (urllib.error.URLError, TimeoutError, json.JSONDecodeError) as e:
            last_err = e
            time.sleep(1.5 * (attempt + 1))
    raise RuntimeError(f"failed to fetch {url}: {last_err}")


def init_schema(conn: sqlite3.Connection):
    conn.executescript("""
        CREATE TABLE IF NOT EXISTS categories (
            id INTEGER PRIMARY KEY,
            parent_id INTEGER REFERENCES categories(id),
            name_en TEXT NOT NULL,
            name_he TEXT,
            sort_order INTEGER NOT NULL DEFAULT 0
        );
        CREATE TABLE IF NOT EXISTS books (
            id INTEGER PRIMARY KEY,
            category_id INTEGER REFERENCES categories(id),
            title_en TEXT NOT NULL UNIQUE,
            title_he TEXT,
            section_names TEXT NOT NULL,
            sort_order INTEGER NOT NULL DEFAULT 0
        );
        CREATE TABLE IF NOT EXISTS segments (
            id INTEGER PRIMARY KEY,
            book_id INTEGER NOT NULL REFERENCES books(id),
            top_index INTEGER NOT NULL,
            path TEXT NOT NULL,
            depth INTEGER NOT NULL,
            ref_display TEXT NOT NULL,
            text_he TEXT NOT NULL,
            sort_order INTEGER NOT NULL
        );
        CREATE INDEX IF NOT EXISTS idx_segments_book ON segments(book_id, sort_order);
        CREATE INDEX IF NOT EXISTS idx_segments_book_chapter ON segments(book_id, top_index, sort_order);
        CREATE TABLE IF NOT EXISTS bookmarks (
            id INTEGER PRIMARY KEY,
            book_id INTEGER NOT NULL REFERENCES books(id),
            segment_id INTEGER NOT NULL REFERENCES segments(id),
            note TEXT,
            created_at INTEGER NOT NULL
        );
        CREATE TABLE IF NOT EXISTS reading_progress (
            book_id INTEGER PRIMARY KEY REFERENCES books(id),
            segment_id INTEGER NOT NULL REFERENCES segments(id),
            updated_at INTEGER NOT NULL
        );
        -- Only complex books have rows here. group_path is a JSON list of the
        -- Hebrew part titles above the chapter (outermost first); number is the
        -- chapter's own section number inside its part, for jump-by-number, and
        -- NULL when the chapter is a whole named part.
        CREATE TABLE IF NOT EXISTS chapters (
            book_id INTEGER NOT NULL REFERENCES books(id),
            top_index INTEGER NOT NULL,
            group_path TEXT NOT NULL,
            label TEXT NOT NULL,
            number INTEGER,
            PRIMARY KEY (book_id, top_index)
        ) WITHOUT ROWID;
    """)
    conn.commit()


def build_category_tree(conn: sqlite3.Connection, toc: list) -> dict:
    """Walks the Sefaria TOC, inserts categories (idempotent - safe to call on every
    run, including resumes), returns {book_title_en: (category_id, order)}."""
    book_category = {}
    cur = conn.cursor()

    def find_or_create_category(parent_id, name_en, name_he, order):
        if parent_id is None:
            row = cur.execute(
                "SELECT id FROM categories WHERE parent_id IS NULL AND name_en = ?", (name_en,)
            ).fetchone()
        else:
            row = cur.execute(
                "SELECT id FROM categories WHERE parent_id = ? AND name_en = ?", (parent_id, name_en)
            ).fetchone()
        if row:
            return row[0]
        cur.execute(
            "INSERT INTO categories (parent_id, name_en, name_he, sort_order) VALUES (?, ?, ?, ?)",
            (parent_id, name_en, name_he, order),
        )
        return cur.lastrowid

    def walk(nodes, parent_id):
        for i, node in enumerate(nodes):
            if "contents" in node:
                cat_id = find_or_create_category(
                    parent_id, node.get("category", "?"), node.get("heCategory"), node.get("order", i)
                )
                walk(node["contents"], cat_id)
            elif "title" in node:
                book_category[node["title"]] = (parent_id, node.get("order", i))

    walk(toc, None)
    conn.commit()
    return book_category


def build_hebrew_text_map(books_json: dict) -> dict:
    """title -> Hebrew json_urls to try, the 'merged' version first. More than
    one because books.json sometimes lists a merged file that is not actually in
    the bucket (Chovat HaTalmidim: 404) - the next version of the same book is a
    better fallback than dropping it from the library."""
    by_title = {}
    for b in books_json["books"]:
        if b.get("language") != "Hebrew":
            continue
        urls = by_title.setdefault(b["title"], [])
        if b.get("versionTitle", "").lower() == "merged":
            urls.insert(0, b["json_url"])
        else:
            urls.append(b["json_url"])
    return by_title


def flatten_text(node, section_names, path, out):
    """Recursively flattens Sefaria's nested text array into (path, depth, text) leaves."""
    if isinstance(node, list):
        for i, child in enumerate(node):
            flatten_text(child, section_names, path + [i + 1], out)
    elif isinstance(node, str):
        cleaned = node.strip()
        if cleaned:
            out.append((path, cleaned))


def section_label(section_names: list, depth_idx: int, idx: int) -> str:
    label = HEBREW_SECTION_NAMES.get(
        section_names[depth_idx] if depth_idx < len(section_names) else "", None
    )
    numeral = to_hebrew_numeral(idx)
    return f"{label} {numeral}" if label else numeral


def make_ref_display(title_he: str, section_names: list, path: list, part_titles: list = ()) -> str:
    parts = list(part_titles) + [section_label(section_names, d, idx) for d, idx in enumerate(path)]
    return f"{title_he} " + ", ".join(parts) if title_he else ", ".join(parts)


def fetch_raw_index(title: str, cache_dir: Path):
    """The Sefaria index record for one title (cached). Needed only for complex
    books, whose export schema lacks each part's sectionNames. None on failure -
    the book is still imported, just with bare numerals as chapter labels."""
    cache_path = cache_dir / (hashlib.sha1(title.encode("utf-8")).hexdigest() + ".json")
    if cache_path.exists():
        return json.loads(cache_path.read_text(encoding="utf-8"))
    try:
        data = fetch_json(RAW_INDEX_URL + urllib.parse.quote(title, safe=""))
    except RuntimeError:
        return None
    cache_path.parent.mkdir(parents=True, exist_ok=True)
    cache_path.write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")
    return data


def match_index_node(index_children: list, en_title: str, position: int):
    """The index node for an export schema node: the export names the default
    (untitled) part "" where the index marks it default=True."""
    for node in index_children:
        if (en_title == "" and node.get("default")) or (en_title and node.get("key") == en_title):
            return node
    return index_children[position] if position < len(index_children) else None


def collect_parts(export_node, index_node, text, part_titles: list, out: list):
    """Depth-first over the named parts of a complex text, in schema order.
    Appends (part_titles_he, section_names, jagged_text) for every leaf part."""
    if isinstance(text, dict):
        children = (export_node or {}).get("nodes") or [{"enTitle": k, "heTitle": k} for k in text]
        index_children = (index_node or {}).get("nodes") or []
        for position, child in enumerate(children):
            en_title = child.get("enTitle", "")
            if en_title not in text:
                continue
            child_titles = part_titles + [child.get("heTitle") or en_title] if en_title else part_titles
            collect_parts(
                child, match_index_node(index_children, en_title, position), text[en_title], child_titles, out
            )
    else:
        out.append((part_titles, (index_node or {}).get("sectionNames") or [], text))


def is_numbered_part(section_names: list, node) -> bool:
    if section_names:
        return len(section_names) >= 2
    return isinstance(node, list) and any(isinstance(child, list) for child in node)


def split_complex_book(title_he: str, parts: list):
    """Turns the leaf parts of a complex book into chapters. A part with numbered
    units (Siman, Gate, Chapter...) gives one chapter per unit, grouped under the
    part's titles; any other part is a single chapter named after the part."""
    segments, chapters = [], []
    segment_count_by_names = {}
    top_index = 0
    for part_titles, section_names, node in parts:
        if is_numbered_part(section_names, node):
            units = []
            for i, child in enumerate(node):
                leaves = []
                flatten_text(child, section_names, [i + 1], leaves)
                if leaves:
                    units.append((i + 1, leaves))
            whole_part = len(units) == 1 and bool(part_titles)
        else:
            leaves = []
            flatten_text(node, section_names, [], leaves)
            units = [(None, leaves)] if leaves else []
            whole_part = True
        for number, leaves in units:
            top_index += 1
            if whole_part:
                chapters.append((top_index, part_titles[:-1], part_titles[-1] if part_titles else (title_he or ""), None))
            else:
                chapters.append((top_index, part_titles, section_label(section_names, 0, number), number))
            for path, text in leaves:
                segments.append(
                    (top_index, path, make_ref_display(title_he, section_names, path, part_titles), text)
                )
            key = json.dumps(section_names)
            segment_count_by_names[key] = segment_count_by_names.get(key, 0) + len(leaves)
    # books.section_names drives isCommentary/contentLabel in the app - take the
    # numbering of the part that holds most of the text.
    book_section_names = (
        json.loads(max(segment_count_by_names, key=segment_count_by_names.get)) if segment_count_by_names else []
    )
    return segments, chapters, book_section_names


def fetch_book_text(title: str, json_urls, index_cache_dir: Path):
    if isinstance(json_urls, str):
        json_urls = [json_urls]
    for attempt, url in enumerate(json_urls):
        try:
            data = fetch_json(url)
            break
        except RuntimeError:
            if attempt == len(json_urls) - 1:
                raise
    title_he = data.get("heTitle")
    text = data.get("text")
    if isinstance(text, dict):
        index = fetch_raw_index(title, index_cache_dir) or {}
        parts = []
        collect_parts(data.get("schema"), index.get("schema"), text, [], parts)
        segments, chapters, section_names = split_complex_book(title_he, parts)
    else:
        section_names = data.get("sectionNames") or []
        leaves = []
        flatten_text(text, section_names, [], leaves)
        segments = [
            (path[0] if path else 0, path, make_ref_display(title_he, section_names, path), leaf_text)
            for path, leaf_text in leaves
        ]
        chapters = None
    return {
        "title": title,
        "title_he": title_he,
        "section_names": section_names,
        "segments": segments,
        "chapters": chapters,
    }


def already_imported(conn: sqlite3.Connection, title: str) -> bool:
    row = conn.execute(
        "SELECT b.id FROM books b JOIN segments s ON s.book_id = b.id WHERE b.title_en = ? LIMIT 1",
        (title,),
    ).fetchone()
    return row is not None


def insert_book(conn: sqlite3.Connection, title_en, category_id, order, book_data):
    cur = conn.cursor()
    # An upsert, not INSERT OR REPLACE: a book already in the table (a complex
    # one an older build left with no text) keeps its id, and with it the
    # root_category that add_root_category.py filled in.
    cur.execute(
        "INSERT INTO books (category_id, title_en, title_he, section_names, sort_order) VALUES (?, ?, ?, ?, ?) "
        "ON CONFLICT(title_en) DO UPDATE SET category_id = excluded.category_id, title_he = excluded.title_he, "
        "section_names = excluded.section_names, sort_order = excluded.sort_order",
        (category_id, title_en, book_data["title_he"], json.dumps(book_data["section_names"], ensure_ascii=False), order),
    )
    book_id = cur.execute("SELECT id FROM books WHERE title_en = ?", (title_en,)).fetchone()[0]
    rows = [
        (book_id, top_index, json.dumps(path), len(path), ref_display, text, sort_order)
        for sort_order, (top_index, path, ref_display, text) in enumerate(book_data["segments"])
    ]
    cur.executemany(
        "INSERT INTO segments (book_id, top_index, path, depth, ref_display, text_he, sort_order) "
        "VALUES (?, ?, ?, ?, ?, ?, ?)",
        rows,
    )
    cur.execute("DELETE FROM chapters WHERE book_id = ?", (book_id,))
    if book_data["chapters"]:
        cur.executemany(
            "INSERT INTO chapters (book_id, top_index, group_path, label, number) VALUES (?, ?, ?, ?, ?)",
            [
                (book_id, top_index, json.dumps(group_path, ensure_ascii=False), label, number)
                for top_index, group_path, label, number in book_data["chapters"]
            ],
        )
    conn.commit()
    return len(rows)


def index_new_segments(conn: sqlite3.Connection, after_id: int):
    """Adds segments imported by this run to segments_fts, if the DB already has
    it (add_fulltext_search.py). Incremental on purpose: rebuilding the whole
    index means rewriting ~500MB on a disk that is usually nearly full."""
    if not conn.execute("SELECT 1 FROM sqlite_master WHERE name = 'segments_fts'").fetchone():
        return 0
    cur = conn.execute(
        "INSERT INTO segments_fts(rowid, text_he, ref_display) SELECT id, text_he, ref_display FROM segments WHERE id > ?",
        (after_id,),
    )
    conn.commit()
    return cur.rowcount


def build_fts(conn: sqlite3.Connection):
    conn.executescript("""
        DROP TABLE IF EXISTS search_fts;
        CREATE VIRTUAL TABLE search_fts USING fts5(title_he, title_en, content='books', content_rowid='id');
        INSERT INTO search_fts(rowid, title_he, title_en) SELECT id, title_he, title_en FROM books;
    """)
    conn.commit()


def fetch_json_cached(url: str, cache_path: Path, refresh: bool):
    if not refresh and cache_path.exists():
        return json.loads(cache_path.read_text(encoding="utf-8"))
    data = fetch_json(url)
    cache_path.parent.mkdir(parents=True, exist_ok=True)
    cache_path.write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")
    return data


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", default=str(Path(__file__).parent / "output" / "sefaria.db"))
    parser.add_argument("--workers", type=int, default=12)
    parser.add_argument("--limit", type=int, default=0, help="cap number of books (0 = no cap, for testing)")
    parser.add_argument("--with-fts", action="store_true", help="build title search_fts index at the end")
    parser.add_argument("--refresh-index", action="store_true", help="re-download TOC/books.json instead of using local cache")
    args = parser.parse_args()

    out_path = Path(args.out)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    cache_dir = Path(__file__).parent / "output" / "_cache"

    print(f"[1/4] loading table of contents (cached under {cache_dir}) ...", flush=True)
    toc = fetch_json_cached(TOC_URL, cache_dir / "toc.json", args.refresh_index)

    print(f"[2/4] loading Hebrew text index (books.json, cached) ...", flush=True)
    books_json = fetch_json_cached(BOOKS_JSON_URL, cache_dir / "books.json", args.refresh_index)
    hebrew_map = build_hebrew_text_map(books_json)
    print(f"      {len(hebrew_map)} titles have a Hebrew version available", flush=True)

    conn = sqlite3.connect(str(out_path))
    conn.execute("PRAGMA journal_mode=WAL")
    init_schema(conn)

    print("[3/4] building/verifying category tree (idempotent) ...", flush=True)
    book_category = build_category_tree(conn, toc)

    to_import = [(title, urls) for title, urls in hebrew_map.items() if title in book_category]
    if args.limit:
        to_import = to_import[: args.limit]
    pending = [(t, u) for t, u in to_import if not already_imported(conn, t)]
    print(
        f"[4/4] {len(to_import)} Hebrew books matched to the TOC; "
        f"{len(to_import) - len(pending)} already imported, {len(pending)} to fetch now",
        flush=True,
    )

    start = time.time()
    done = len(to_import) - len(pending)
    total_segments = conn.execute("SELECT COUNT(*) FROM segments").fetchone()[0]
    last_segment_id = conn.execute("SELECT COALESCE(MAX(id), 0) FROM segments").fetchone()[0]
    index_cache_dir = cache_dir / "index"
    errors = []

    with ThreadPoolExecutor(max_workers=args.workers) as pool:
        future_to_title = {
            pool.submit(fetch_book_text, title, url, index_cache_dir): title for title, url in pending
        }
        for future in as_completed(future_to_title):
            title = future_to_title[future]
            category_id, order = book_category[title]
            try:
                book_data = future.result()
                n = insert_book(conn, title, category_id, order, book_data)
                total_segments += n
                done += 1
                if done % 25 == 0 or done == len(to_import):
                    elapsed = time.time() - start
                    rate = (done - (len(to_import) - len(pending))) / elapsed if elapsed > 0 else 0
                    remaining = len(pending) - (done - (len(to_import) - len(pending)))
                    eta_min = (remaining / rate / 60) if rate > 0 else float("inf")
                    print(
                        f"  {done}/{len(to_import)} books | {total_segments} segments | "
                        f"{elapsed:.0f}s elapsed | ETA {eta_min:.1f}min",
                        flush=True,
                    )
            except Exception as e:
                errors.append((title, str(e)))
                print(f"  ERROR importing '{title}': {e}", flush=True)

    n_indexed = index_new_segments(conn, last_segment_id)
    if n_indexed:
        print(f"added {n_indexed} new segments to segments_fts", flush=True)

    if args.with_fts:
        print("building FTS5 title search index ...", flush=True)
        build_fts(conn)

    n_books = conn.execute("SELECT COUNT(*) FROM books").fetchone()[0]
    n_empty = conn.execute(
        "SELECT COUNT(*) FROM books b WHERE NOT EXISTS (SELECT 1 FROM segments s WHERE s.book_id = b.id)"
    ).fetchone()[0]
    n_segments = conn.execute("SELECT COUNT(*) FROM segments").fetchone()[0]
    n_categories = conn.execute("SELECT COUNT(*) FROM categories").fetchone()[0]
    conn.close()

    print("\n=== DONE ===")
    print(f"categories: {n_categories} | books: {n_books} ({n_empty} with no text) | segments: {n_segments}")
    print(f"db size: {out_path.stat().st_size / 1024 / 1024:.1f} MB at {out_path}")
    if errors:
        print(f"{len(errors)} books failed (re-run the script to retry them, it's resumable):")
        for title, err in errors[:20]:
            print(f"  - {title}: {err}")


if __name__ == "__main__":
    main()
