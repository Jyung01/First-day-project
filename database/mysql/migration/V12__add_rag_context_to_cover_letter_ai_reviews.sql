-- =========================================================
-- FirstDay MySQL Migration V12
-- cover_letter_ai_reviews에 RAG 검색 근거 문단을 추가한다.
-- 첨삭 생성 시 pgvector에서 검색해 프롬프트에 넣은 유사 공고 문단을
-- 문항 순서와 같은 순서의 배열로 남겨, 나중에 어떤 근거로 첨삭이
-- 만들어졌는지 되짚을 수 있게 한다. (REQ-903)
-- 기존 행에는 근거가 없으므로 NULL을 허용한다.
-- 적용 대상: V11까지 적용된 기존 MySQL DB
-- =========================================================

ALTER TABLE cover_letter_ai_reviews
    ADD COLUMN rag_context JSON NULL
        COMMENT '문항별 RAG 검색 근거 문단; 문항 순서와 같은 순서의 배열'
        AFTER feedback;

-- 적용 확인
SHOW COLUMNS FROM cover_letter_ai_reviews LIKE 'rag_context';
SHOW CREATE TABLE cover_letter_ai_reviews;
