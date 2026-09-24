# Sfarim (בלכתך בדרך)

`com.future.sfarim` — Torah text library.

Reader/browser for a local library of Torah texts, backed by raw SQLite (`LibraryDatabase`, no Room). The most refined navigation pattern in the suite (manual backstack `Route`, `T9Search`, `FocusableItem`, `ScreenTopBar`) — used as the template for [Music](../Music/)'s architecture. `FocusableItem` and `ScreenTopBar` are now thin theme-adapting wrappers around the shared `SharedKeypadNav` module (`../SharedKeypadNav/`), which also houses the shared T9 digit-map used by `dialer`, `Music`, and `Keyboard`.

**The underlying database, `tools/output/sefaria.db` (~4.5GB), is present on disk but not tracked in git** — GitHub blocks files over 100MB. It was built with `tools/build_library.py` (pulls the table of contents from the Sefaria API and Hebrew text from the Sefaria-Export GCS bucket; resumable) followed by `tools/add_root_category.py` and `tools/add_fulltext_search.py`. To rebuild it from scratch:

```
python tools/build_library.py --with-fts
python tools/add_root_category.py
python tools/add_fulltext_search.py
```

Re-running `build_library.py` on an existing DB only fetches what is missing and adds the new segments to `segments_fts` incrementally, so it does not rewrite the whole index.

**Simple books and complex books.** A simple book's text is one nested list numbered by the book's `sectionNames`, and a chapter is just a `top_index` in `segments`. A complex book (Mishnah Berurah, Beit Yosef, Arukh HaShulchan, the Zohar, siddurim and machzorim — 874 titles, 14% of the library) is a tree of *named* parts: Arukh HaShulchan → Orach Chaim → Siman 1, Siddur Ashkenaz → Weekday → Shacharit → Modeh Ani. Those get a row per chapter in the `chapters` table (`group_path` = the Hebrew part titles above it, `label` = its display name, `number` = its number inside its part, for jump-by-number), because neither the label nor the part it belongs to can be derived from `section_names`. `LibraryRepository.getChapters` reads that table when it has rows and falls back to `DISTINCT top_index` otherwise; `BookChaptersScreen` turns `group_path` into one list level per part.

Until 2026-09 the importer dropped every complex book on the floor (it only walked lists, and a complex book's `text` is a dict), which is what left 875 books in the library with no text at all. `books.section_names` for such a book is the numbering of whichever part holds most of its text.

The ~390 titles in the table of contents with no Hebrew text here are not a bug either: most are English- or German-only works, and 124 (the Koren Steinsaltz Tanakh and Mishneh Torah, Urim titles) are copyrighted and deliberately left out of Sefaria's public export — the importer does not go around that. `Chovat HaTalmidim` is the one Hebrew title whose export files 404.

(`tools/output/reassemble_sefaria_db.sh` documents an earlier, abandoned plan to split/reassemble a pre-existing dump — no longer needed now that the DB is built directly from Sefaria's public sources.)
