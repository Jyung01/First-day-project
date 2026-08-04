-- 첫출근 통합 DB 설계 v1 - PostgreSQL / pgvector
-- Target: PostgreSQL 17 + pgvector
-- Embedding model baseline: qwen3-embedding:0.6b, dimension 1024
-- 원칙: PostgreSQL은 임베딩 검색용 파생 데이터만 저장한다.
--       회원·기업·공고·지원의 원본은 MySQL이며 DB 간 외래키는 만들지 않는다.

BEGIN;

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE SCHEMA IF NOT EXISTS ai;

-- Spring AI PgVectorStore 기본 컬럼(id/content/metadata/embedding)을 유지한다.
CREATE TABLE IF NOT EXISTS ai.vector_store (
  id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  content              TEXT NOT NULL,
  metadata             JSONB NOT NULL DEFAULT '{}'::jsonb,
  embedding            VECTOR(1024) NOT NULL,
  created_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_vector_store_metadata
  ON ai.vector_store USING GIN (metadata);

CREATE INDEX IF NOT EXISTS idx_vector_store_embedding_hnsw
  ON ai.vector_store
  USING HNSW (embedding vector_cosine_ops)
  WITH (m = 16, ef_construction = 64);

COMMENT ON SCHEMA ai IS
  '첫출근 AI 임베딩 검색용 파생 데이터';
COMMENT ON TABLE ai.vector_store IS
  'Spring AI 호환 임베딩 저장소; VECTOR(1024)와 cosine HNSW 사용';
COMMENT ON COLUMN ai.vector_store.metadata IS
  'source_type, source_id, job_category_id 등 검색 필터용 JSONB';

COMMIT;

-- Spring AI 설정 권장값
-- spring.ai.vectorstore.pgvector.schema-name=ai
-- spring.ai.vectorstore.pgvector.table-name=vector_store
-- spring.ai.vectorstore.pgvector.dimensions=1024
-- spring.ai.vectorstore.pgvector.distance-type=COSINE_DISTANCE
-- spring.ai.vectorstore.pgvector.initialize-schema=false
--
-- 주의: 임베딩 모델 차원이 바뀌면 VECTOR 차원을 맞춰 테이블을 재구성하고
--       기존 데이터를 새 모델로 전체 재임베딩해야 한다.
