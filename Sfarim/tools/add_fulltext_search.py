#!/usr/bin/env python3
"""
Adds a segments_fts FTS5 index to an existing sefaria.db so search can cover
verse/segment text content, not just book titles. Purely local - no network
needed, operates on data already imported by build_library.py.

Usage:
    python add_fulltext_search.py [--db PATH]
"""

import argparse
import sqlite3
import time
from pathlib import Path


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--db", default=str(Path(__file__).parent / "output" / "sefaria.db"))
    args = parser.parse_args()

    conn = sqlite3.connect(args.db)
    conn.execute("PRAGMA journal_mode=WAL")

    print("building segments_fts (full-text index over verse content) ...", flush=True)
    start = time.time()
    conn.executescript("""
        DROP TABLE IF EXISTS segments_fts;
        CREATE VIRTUAL TABLE segments_fts USING fts5(
            text_he,
            ref_display,
            content='segments',
            content_rowid='id',
            tokenize='unicode61 remove_diacritics 2'
        );
        INSERT INTO segments_fts(rowid, text_he, ref_display) SELECT id, text_he, ref_display FROM segments;
    """)
    conn.commit()
    print(f"segments_fts built in {time.time() - start:.1f}s", flush=True)

    print("rebuilding search_fts (book titles) to make sure it's current ...", flush=True)
    conn.executescript("""
        DROP TABLE IF EXISTS search_fts;
        CREATE VIRTUAL TABLE search_fts USING fts5(
            title_he,
            title_en,
            content='books',
            content_rowid='id',
            tokenize='unicode61 remove_diacritics 2'
        );
        INSERT INTO search_fts(rowid, title_he, title_en) SELECT id, title_he, title_en FROM books;
    """)
    conn.commit()

    n = conn.execute("SELECT COUNT(*) FROM segments_fts").fetchone()[0]
    print(f"segments_fts rows: {n}")
    conn.execute("PRAGMA optimize")
    conn.close()

    db_path = Path(args.db)
    print(f"db size now: {db_path.stat().st_size / 1024 / 1024:.1f} MB")


if __name__ == "__main__":
    main()
