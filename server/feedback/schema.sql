-- סכמת D1 (SQLite) של שרת המשוב. ראו docs/update-feedback-design.md סעיף 7.
-- הרצה: wrangler d1 execute futureos --file=schema.sql (מקומית: --local)

CREATE TABLE IF NOT EXISTS devices (
  id            TEXT PRIMARY KEY,          -- UUID שהשרת מנפיק; לא IMEI
  token_hash    TEXT NOT NULL,             -- SHA-256 של הטוקן, הטוקן עצמו לא נשמר
  model         TEXT,
  build         TEXT,
  channel       TEXT NOT NULL DEFAULT 'dev',
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
