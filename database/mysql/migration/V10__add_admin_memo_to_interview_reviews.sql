-- =========================================================
-- FirstDay MySQL Migration V10
-- interview_reviews에 관리자 후기 처리 메모 컬럼을 추가한다.
-- 적용 대상: V9까지 적용된 기존 MySQL DB
-- =========================================================

ALTER TABLE interview_reviews
    ADD COLUMN admin_memo VARCHAR(2000) NULL
        COMMENT '관리자 후기 처리 메모'
        AFTER hidden_by;

-- 적용 확인
SHOW COLUMNS FROM interview_reviews LIKE 'admin_memo';
