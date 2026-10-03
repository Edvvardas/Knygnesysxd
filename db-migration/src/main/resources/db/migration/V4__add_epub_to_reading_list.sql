-- Renamed from V2 to V4 — V2 was already taken by V2__spring_session.sql
-- Adds epub_path to store the file path of an uploaded EPUB for a reading list entry
ALTER TABLE reading_list ADD COLUMN epub_path VARCHAR(500);
