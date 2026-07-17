-- Document chunks + embeddings (FastAPI/RAG side)
-- Requires the pgvector extension.
CREATE EXTENSION IF NOT EXISTS vector;

-- Embedding dimension depends on the embedding model — TBD (768 = placeholder)
CREATE TABLE document_chunks (
    id          BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    document_id UUID        NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    chunk_index INT         NOT NULL,
    content     TEXT        NOT NULL,
    embedding   VECTOR(768) NOT NULL,              -- dim = PLACEHOLDER, see note
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (document_id, chunk_index)
);

-- Approximate-NN index; revisit lists/params after chunking R&D.
CREATE INDEX idx_chunks_embedding
    ON document_chunks USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);
