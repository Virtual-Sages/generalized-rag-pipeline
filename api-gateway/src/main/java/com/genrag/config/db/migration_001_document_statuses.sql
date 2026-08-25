-- ============================================================
-- migration_001_document_statuses.sql
--
-- Purpose : Replace the original four-value CHECK constraint on
--           documents.status with the full 18-status set that
--           DocumentStatus.getInternalStatuses() produces.
--
-- How to apply : Run once in pgAdmin Query Tool against the
--                genrag database (Tools > Query Tool, F5).
--                Safe to run on an empty table; the UPDATEs
--                will simply be no-ops.
--
-- See also : ai-service/README.md §2.3
-- ============================================================

-- ── 0. Pre-flight ────────────────────────────────────────────
-- Run this SELECT first; review the output before proceeding.
-- If you see values other than those handled below, add UPDATE
-- remappings before swapping the constraint.
SELECT status, count(*) FROM documents GROUP BY status;

-- ── 1. Remap legacy values ───────────────────────────────────
-- 'PROCESSING' mapped to the nearest equivalent internal status.
UPDATE documents SET status = 'READY_FOR_PROCESSING' WHERE status = 'PROCESSING';

-- 'FAILED' is now user-visible only (never stored). Map to the
-- earliest failure status that could have produced a bare FAILED
-- in the old schema. Adjust if your data came from a later stage.
UPDATE documents SET status = 'UPLOADING_FAILED'      WHERE status = 'FAILED';

-- 'UPLOADED' and 'INDEXED' are valid in the new constraint; no
-- remapping needed.

-- ── 2. Swap the constraint ───────────────────────────────────
ALTER TABLE documents
    DROP CONSTRAINT IF EXISTS documents_status_check;

ALTER TABLE documents
    ADD CONSTRAINT documents_status_check
    CHECK (status IN (
        'UPLOADING',            'UPLOADED',          'UPLOADING_FAILED',
        'READY_FOR_PROCESSING', 'ACCESS_FAILED',
        'PARSING',              'PARSED',            'PARSING_FAILED',
        'CHUNKING',             'CHUNKED',           'CHUNKING_FAILED',
        'EMBEDDING',            'EMBEDDED',          'EMBEDDING_FAILED',
        'INDEXING',             'INDEXED',           'INDEXING_FAILED',
        'PROCESSED'
    ));
