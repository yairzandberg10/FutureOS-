#!/usr/bin/env python3
"""
Adds books.root_category - the top-level corpus name (Tanakh/Mishnah/Talmud/...)
for each book, computed by walking the categories.parent_id chain to its root.
Purely local, no network needed.
"""

import argparse
import sqlite3
from pathlib import Path


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--db", default=str(Path(__file__).parent / "output" / "sefaria.db"))
    args = parser.parse_args()

    conn = sqlite3.connect(args.db)

    cols = [r[1] for r in conn.execute("PRAGMA table_info(books)")]
    if "root_category" not in cols:
        conn.execute("ALTER TABLE books ADD COLUMN root_category TEXT")

    cats = {row[0]: row[1] for row in conn.execute("SELECT id, parent_id FROM categories")}
    names = {row[0]: row[1] for row in conn.execute("SELECT id, name_en FROM categories")}

    def root_of(cat_id):
        cur = cat_id
        while cats.get(cur) is not None:
            cur = cats[cur]
        return names.get(cur)

    root_cache = {cid: root_of(cid) for cid in cats}

    rows = conn.execute("SELECT id, category_id FROM books").fetchall()
    updates = [(root_cache.get(cat_id), bid) for bid, cat_id in rows]
    conn.executemany("UPDATE books SET root_category = ? WHERE id = ?", updates)
    conn.commit()

    print("sample:")
    for row in conn.execute("SELECT title_en, root_category FROM books WHERE title_en IN "
                             "('Genesis','Rashi on Genesis','Mishnah Berakhot','Berakhot','Shulchan Arukh, Orach Chayim')"):
        print(" ", row)
    n = conn.execute("SELECT COUNT(*) FROM books WHERE root_category IS NULL").fetchone()[0]
    print(f"books with NULL root_category: {n}")
    conn.execute("PRAGMA optimize")
    conn.close()


if __name__ == "__main__":
    main()
