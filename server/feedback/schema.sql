-- סכמת D1 (SQLite) של שרת המשוב. ראו docs/update-feedback-design.md סעיף 7.
-- הרצה: wrangler d1 execute futureos --file=schema.sql (מקומית: --local)

CREATE TABLE IF NOT EXISTS devices (
  id            TEXT PRIMARY KEY,          -- UUID שהשרת מנפיק; לא IMEI
  token_hash    TEXT NOT NULL,             -- SHA-256 של הטוקן, הטוקן עצמו לא נשמר
  model         TEXT,
  build         TEXT,
  channel       TEXT NOT NULL DEFAULT 'stable', -- dev | beta | stable (ערוץ עדכונים; dev רק לטלפון של המשתמש)
  created_at    INTEGER NOT NULL,
  last_seen     INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS feedback (
  id            TEXT PRIMARY KEY,
  device_id     TEXT NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
  client_id     TEXT NOT NULL,             -- מזהה שהמכשיר יוצר, מונע כפילות בשליחה חוזרת
  type          TEXT NOT NULL,             -- bug | idea | other
  text          TEXT NOT NULL,             -- אחרי סינון פרטים אישיים
  app_pkg       TEXT NOT NULL,
  app_version   TEXT,
  build         TEXT,
  log_key       TEXT,                      -- מפתח ב-R2, אם המשתמש צירף לוג
  status        TEXT NOT NULL DEFAULT 'new', -- new | triaged | in_progress | needs_info | wontfix | fixed
  cluster_id    TEXT REFERENCES clusters(id),
  severity      INTEGER,
  decision      TEXT,
  created_at    INTEGER NOT NULL,
  UNIQUE (device_id, client_id)
);
CREATE INDEX IF NOT EXISTS feedback_status ON feedback(status);
CREATE INDEX IF NOT EXISTS feedback_device ON feedback(device_id);

CREATE TABLE IF NOT EXISTS clusters (
  id            TEXT PRIMARY KEY,
  app_pkg       TEXT NOT NULL,
  title_he      TEXT NOT NULL,
  severity      INTEGER NOT NULL DEFAULT 3,
  state         TEXT NOT NULL DEFAULT 'open', -- open | deep | wontfix | fixed
  embedding     TEXT NOT NULL,             -- JSON של וקטור המרכז
  members       INTEGER NOT NULL DEFAULT 0,
  gh_issue      INTEGER,
  fixed_in      TEXT,
  created_at    INTEGER NOT NULL,
  updated_at    INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS clusters_app ON clusters(app_pkg, state);

CREATE TABLE IF NOT EXISTS inbox (
  id            TEXT PRIMARY KEY,
  device_id     TEXT NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
  feedback_id   TEXT REFERENCES feedback(id) ON DELETE CASCADE,
  kind          TEXT NOT NULL,             -- in_progress | needs_info | fixed
  text_he       TEXT NOT NULL,
  created_at    INTEGER NOT NULL,
  delivered_at  INTEGER,
  UNIQUE (feedback_id, kind)
);
CREATE INDEX IF NOT EXISTS inbox_device ON inbox(device_id, created_at);

-- מונים לחלונות זמן (הגבלת קצב ומכסה יומית). key = "<סוג>:<מזהה>:<חלון>"
CREATE TABLE IF NOT EXISTS counters (
  key           TEXT PRIMARY KEY,
  count         INTEGER NOT NULL,
  expires_at    INTEGER NOT NULL
);

-- ---------- עדכונים (docs/update-feedback-design.md סעיף 2) ----------
-- גרסה אחת = שורה אחת. kind=apk היא עדכון של אפליקציה בודדת (סעיף 2.5, אושר 2026-10-16).
-- kind=ota שמור ל-FutureUpdater של ה-AOSP: אותו פרוטוקול, ומכשיר מקבל אותו רק אם הצהיר שהוא תומך.
CREATE TABLE IF NOT EXISTS releases (
  id             TEXT PRIMARY KEY,
  kind           TEXT NOT NULL DEFAULT 'apk',   -- apk | ota
  pkg            TEXT NOT NULL,                 -- שם החבילה; ב-ota: com.future.system
  version_code   INTEGER NOT NULL,
  version_name   TEXT NOT NULL,
  channel        TEXT NOT NULL DEFAULT 'stable', -- dev | beta | stable
  blob_key       TEXT,                          -- מפתח ב-R2 אחרי העלאה
  size           INTEGER,
  sha256         TEXT,                          -- מחושב בשרת בזמן ההעלאה
  notes_he       TEXT NOT NULL DEFAULT '',
  fixed_clusters TEXT NOT NULL DEFAULT '[]',    -- JSON: אשכולות משוב שהגרסה מתקנת
  pct            INTEGER NOT NULL DEFAULT 0,    -- אחוז המכשירים שמקבלים (0-100)
  state          TEXT NOT NULL DEFAULT 'draft', -- draft | active | paused
  pause_reason   TEXT,
  created_at     INTEGER NOT NULL,
  updated_at     INTEGER NOT NULL,
  UNIQUE (kind, pkg, version_code)
);
CREATE INDEX IF NOT EXISTS releases_active ON releases(state, kind, pkg);

-- מה קרה לכל גרסה בכל מכשיר. שורה אחת לכל (מכשיר, גרסה, אירוע); at מתעדכן בכל חזרה.
CREATE TABLE IF NOT EXISTS update_events (
  device_id   TEXT NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
  release_id  TEXT NOT NULL REFERENCES releases(id) ON DELETE CASCADE,
  event       TEXT NOT NULL,                   -- offered | served (השרת הגיש את הקובץ) | downloaded | installed | failed
  error       TEXT,
  at          INTEGER NOT NULL,
  PRIMARY KEY (device_id, release_id, event)
);
CREATE INDEX IF NOT EXISTS update_events_release ON update_events(release_id, event);

-- ---------- דיווחי דרך של הקהילה (בסגנון Waze) ----------
-- דיווח חדש קרוב לקיים מאותו סוג מאשר אותו במקום ליצור כפילות.
CREATE TABLE IF NOT EXISTS road_reports (
  id          TEXT PRIMARY KEY,
  type        TEXT NOT NULL,                -- police | accident | hazard | traffic | closure | camera
  lat         REAL NOT NULL,
  lng         REAL NOT NULL,
  device_id   TEXT NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
  confirms    INTEGER NOT NULL DEFAULT 1,
  denies      INTEGER NOT NULL DEFAULT 0,
  created_at  INTEGER NOT NULL,
  updated_at  INTEGER NOT NULL,
  expires_at  INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS road_reports_area ON road_reports(lat, lng);
CREATE INDEX IF NOT EXISTS road_reports_expiry ON road_reports(expires_at);

-- הצבעה אחת למכשיר על דיווח
CREATE TABLE IF NOT EXISTS road_report_votes (
  report_id   TEXT NOT NULL REFERENCES road_reports(id) ON DELETE CASCADE,
  device_id   TEXT NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
  up          INTEGER NOT NULL,
  PRIMARY KEY (report_id, device_id)
);
