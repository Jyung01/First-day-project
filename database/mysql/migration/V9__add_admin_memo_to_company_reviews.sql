-- =========================================================
-- FirstDay MySQL Migration V9
-- company_reviews에 관리자 후기 처리 메모 컬럼을 추가한다.
-- 적용 대상: V8까지 적용된 기존 MySQL DB
-- =========================================================

ALTER TABLE company_reviews
    ADD COLUMN admin_memo VARCHAR(2000) NULL
        COMMENT '관리자 후기 처리 메모'
        AFTER hidden_by;

-- 적용 확인
SHOW COLUMNS FROM company_reviews LIKE 'admin_memo';
