# Sfarim (בלכתך בדרך)

`com.future.sfarim` — Torah text library.

Reader/browser for a local library of Torah texts, backed by raw SQLite (`LibraryDatabase`, no Room). The most refined navigation pattern in the suite (manual backstack `Route`, `T9Search`, `FocusableItem`, `ScreenTopBar`) — used as the template for [Music](../Music/)'s architecture. `FocusableItem` and `ScreenTopBar` are now thin theme-adapting wrappers around the shared `SharedKeypadNav` module (`../SharedKeypadNav/`), which also houses the shared T9 digit-map used by `dialer`, `Music`, and `Keyboard`.

**The underlying database, `tools/output/sefaria.db` (~1.55GB), is present on disk but not tracked in git** — GitHub blocks files over 100MB. It was built with `tools/build_library.py` (pulls the table of contents from the Sefaria API and Hebrew text from the Sefaria-Export GCS bucket; resumable) followed by `tools/add_root_category.py` and `tools/add_fulltext_search.py`. To rebuild it from scratch:

```
python tools/build_library.py --with-fts
python tools/add_root_category.py
python tools/add_fulltext_search.py
```

(`tools/output/reassemble_sefaria_db.sh` documents an earlier, abandoned plan to split/reassemble a pre-existing dump — no longer needed now that the DB is built directly from Sefaria's public sources.)
