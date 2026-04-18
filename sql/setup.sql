-- ============================================================
-- ReelVault — Supabase PostgreSQL Setup Script
-- Run this in your Supabase project: SQL Editor → New Query
-- ============================================================

-- Step 1: Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Step 2: Create the main knowledge_items table
CREATE TABLE IF NOT EXISTS knowledge_items (
  id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  title         TEXT        NOT NULL,
  description   TEXT,
  original_url  TEXT,
  source_url    TEXT,
  content_type  TEXT        CHECK (content_type IN ('tool','article','video','paper','course','note')),
  tags          TEXT[],
  personal_notes TEXT,
  embedding     vector(384),
  created_at    TIMESTAMPTZ DEFAULT NOW(),
  updated_at    TIMESTAMPTZ DEFAULT NOW()
);

-- Step 3: Create HNSW index for fast cosine similarity search
-- HNSW is better than IVFFlat for personal knowledge bases (works well at ANY size,
-- no minimum row requirement, no training phase, better recall by default).
-- If upgrading from a previous version, run: DROP INDEX IF EXISTS knowledge_items_embedding_idx;
CREATE INDEX IF NOT EXISTS knowledge_items_embedding_idx
  ON knowledge_items
  USING hnsw (embedding vector_cosine_ops)
  WITH (m = 16, ef_construction = 64);

-- Step 4: Index on content_type for filter queries
CREATE INDEX IF NOT EXISTS knowledge_items_content_type_idx
  ON knowledge_items (content_type);

-- Step 5: Index on created_at for date-range filter and recent queries
CREATE INDEX IF NOT EXISTS knowledge_items_created_at_idx
  ON knowledge_items (created_at DESC);

-- Step 6: GIN index on tags array for tag-based filtering
CREATE INDEX IF NOT EXISTS knowledge_items_tags_idx
  ON knowledge_items USING gin (tags);

-- Step 7: Auto-update trigger for updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS set_updated_at ON knowledge_items;
CREATE TRIGGER set_updated_at
  BEFORE UPDATE ON knowledge_items
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- Verification: Run these queries to verify the setup
-- ============================================================

-- Check extension is enabled:
-- SELECT * FROM pg_extension WHERE extname = 'vector';

-- Check table structure:
-- \d knowledge_items

-- Insert a test row (optional):
-- INSERT INTO knowledge_items (title, description, content_type, tags)
-- VALUES ('Test Item', 'A test knowledge item', 'note', ARRAY['test', 'demo']);

-- SELECT id, title, tags, created_at FROM knowledge_items;

-- Clean up test row:
-- DELETE FROM knowledge_items WHERE title = 'Test Item';

-- ============================================================
-- Done! Your Supabase database is ready for ReelVault.
-- ============================================================
