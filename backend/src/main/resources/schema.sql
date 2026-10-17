-- v2.0 SQLite 四表（03-生产规范.md §1.7），幂等执行
CREATE TABLE IF NOT EXISTS document (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    title       TEXT    NOT NULL,
    genre       TEXT    NOT NULL DEFAULT 'NOVEL',
    content     TEXT    NOT NULL DEFAULT '',
    char_count  INTEGER NOT NULL DEFAULT 0,
    created_at  TEXT    NOT NULL,
    updated_at  TEXT    NOT NULL
);

CREATE TABLE IF NOT EXISTS doc_version (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    document_id  INTEGER NOT NULL,
    content_hash TEXT    NOT NULL,
    content      TEXT    NOT NULL,
    char_count   INTEGER NOT NULL DEFAULT 0,
    created_at   TEXT    NOT NULL,
    FOREIGN KEY (document_id) REFERENCES document (id) ON DELETE CASCADE
    );
CREATE UNIQUE INDEX IF NOT EXISTS idx_doc_version_hash ON doc_version (document_id, content_hash);
CREATE INDEX IF NOT EXISTS idx_doc_version_doc ON doc_version (document_id);

CREATE TABLE IF NOT EXISTS prompt_template (
   id         INTEGER PRIMARY KEY AUTOINCREMENT,
   name       TEXT NOT NULL,
   prompt     TEXT NOT NULL,
   genre      TEXT NOT NULL,
   category   TEXT,
   is_system  INTEGER NOT NULL DEFAULT 0,
   created_at TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_template_genre ON prompt_template (genre);

CREATE TABLE IF NOT EXISTS material (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    genre      TEXT NOT NULL,
    kind       TEXT,
    content    TEXT NOT NULL,
    source     TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_material_genre ON material (genre);