#!/usr/bin/env python3
"""
Builds sefaria.db: a single offline SQLite library of every Hebrew text on
Sefaria (sefaria.org), for the FutureOS Sfarim app.

Pulls the category/book table of contents from the Sefaria API and the actual
text content (best available Hebrew "merged" version per title) from the
public Sefaria-Export GCS bucket. Resumable: re-running skips books already
fully imported (checked via segment count in the existing DB).

Usage:
    python build_library.py [--out PATH] [--workers N] [--limit N] [--with-fts]
"""

import argparse
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
    """title -> best Hebrew json_url (prefers versionTitle == 'merged')."""
    by_title = {}
    for b in books_json["books"]:
        if b.get("language") != "Hebrew":
            continue
        title = b["title"]
        is_merged = b.get("versionTitle", "").lower() == "merged"
        if title not in by_title or (is_merged and not by_title[title][1]):
            by_title[title] = (b["json_url"], is_merged)
    return {title: url for title, (url, _merged) in by_title.items()}


def flatten_text(node, section_names, path, out):
    """Recursively flattens Sefaria's nested text array into (path, depth, text) leaves."""
    if isinstance(node, list):
        for i, child in enumerate(node):
            flatten_text(child, section_names, path + [i + 1], out)
    elif isinstance(node, str):
        cleaned = node.strip()
        if cleaned:
            out.append((path, cleaned))


def make_ref_display(title_he: str, section_names: list, path: list) -> str:
    parts = []
    for depth_idx, idx in enumerate(path):
        label = HEBREW_SECTION_NAMES.get(
            section_names[depth_idx] if depth_idx < len(section_names) else "", None
        )
        numeral = to_hebrew_numeral(idx)
        parts.append(f"{label} {numeral}" if label else numeral)
    return f"{title_he} " + ", ".join(parts) if title_he else ", ".join(parts)


def fetch_book_text(title: str, json_url: str):
    data = fetch_json(json_url)
    section_names = data.get("sectionNames") or []
    leaves = []
    flatten_text(data.get("text"), section_names, [], leaves)
    return {
        "title": title,
        "title_he": data.get("heTitle"),
        "section_names": section_names,
        "leaves": leaves,
    }


def already_imported(conn: sqlite3.Connection, title: str) -> bool:
    row = conn.execute(
        "SELECT b.id FROM books b JOIN segments s ON s.book_id = b.id WHERE b.title_en = ? LIMIT 1",
        (title,),
    ).fetchone()
    return row is not None


def insert_book(conn: sqlite3.Connection, title_en, category_id, order, book_data):
    cur = conn.cursor()
    cur.execute(
        "INSERT OR REPLACE INTO books (category_id, title_en, title_he, section_names, sort_order) "
        "VALUES (?, ?, ?, ?, ?)",
        (category_id, title_en, book_data["title_he"], json.dumps(book_data["section_names"], ensure_ascii=False), order),
    )
    book_id = cur.lastrowid
    section_names = book_data["section_names"]
    title_he = book_data["title_he"]
    rows = []
    for sort_order, (path, text) in enumerate(book_data["leaves"]):
        ref_display = make_ref_display(title_he, section_names, path)
        top_index = path[0] if path else 0
        rows.append((book_id, top_index, json.dumps(path), len(path), ref_display, text, sort_order))
    cur.executemany(
        "INSERT INTO segments (book_id, top_index, path, depth, ref_display, text_he, sort_order) "
        "VALUES (?, ?, ?, ?, ?, ?, ?)",
        rows,
    )
    conn.commit()
    return len(rows)


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

    to_import = [(title, url) for title, url in hebrew_map.items() if title in book_category]
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
    errors = []

    with ThreadPoolExecutor(max_workers=args.workers) as pool:
        future_to_title = {
            pool.submit(fetch_book_text, title, url): title for title, url in pending
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

    if args.with_fts:
        print("building FTS5 title search index ...", flush=True)
        build_fts(conn)

    n_books = conn.execute("SELECT COUNT(*) FROM books").fetchone()[0]
    n_segments = conn.execute("SELECT COUNT(*) FROM segments").fetchone()[0]
    n_categories = conn.execute("SELECT COUNT(*) FROM categories").fetchone()[0]
    conn.close()

    print("\n=== DONE ===")
    print(f"categories: {n_categories} | books: {n_books} | segments: {n_segments}")
    print(f"db size: {out_path.stat().st_size / 1024 / 1024:.1f} MB at {out_path}")
    if errors:
        print(f"{len(errors)} books failed (re-run the script to retry them, it's resumable):")
        for title, err in errors[:20]:
            print(f"  - {title}: {err}")


if __name__ == "__main__":
    main()
