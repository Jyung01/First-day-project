-- =========================================================
-- FirstDay MySQL Migration V11
-- cover_letter_ai_reviews에 첨삭 대상 채용공고 참조를 추가한다.
-- 재요청마다 새 행을 쌓는 이력 구조를 지원하기 위해, 각 첨삭 결과가
-- 어느 공고를 기준으로 생성됐는지 저장한다.
-- 적용 대상: V10까지 적용된 기존 MySQL DB
-- =========================================================

ALTER TABLE cover_letter_ai_reviews
    ADD COLUMN job_posting_id BIGINT UNSIGNED NOT NULL
        COMMENT '첨삭 대상으로 선택한 채용공고'
        AFTER cover_letter_id,
    ADD KEY idx_cover_letter_ai_reviews_job_posting (job_posting_id),
    ADD CONSTRAINT fk_cover_letter_ai_reviews_job_posting
        FOREIGN KEY (job_posting_id) REFERENCES job_postings (job_posting_id);

-- 적용 확인
SHOW COLUMNS FROM cover_letter_ai_reviews LIKE 'job_posting_id';
SHOW CREATE TABLE cover_letter_ai_reviews;
