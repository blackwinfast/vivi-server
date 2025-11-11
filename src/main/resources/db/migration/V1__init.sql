CREATE TABLE IF NOT EXISTS anniversaries (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL,
  title TEXT NOT NULL,
  date TEXT NOT NULL,
  created_by TEXT NOT NULL,
  description TEXT,
  recurring BOOLEAN NOT NULL
);

CREATE TABLE IF NOT EXISTS emotion_memories (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id TEXT NOT NULL,
  event TEXT NOT NULL,
  emotion_type TEXT NOT NULL,
  intensity REAL NOT NULL,
  timestamp TEXT NOT NULL,
  event_type TEXT NOT NULL
);
