-- =========================================================
-- FirstDay MySQL Migration V4
-- companies에 가장 최근 기업가입 반려 사유 코드를 추가한다.
-- 적용 대상: V3까지 적용된 기존 MySQL DB
-- =========================================================

ALTER TABLE companies
    ADD COLUMN latest_rejection_code VARCHAR(50) NULL
        AFTER company_status,
    ADD CONSTRAINT chk_companies_rejection_code
        CHECK (
            latest_rejection_code IS NULL
            OR latest_rejection_code IN (
                'MISSING_INFORMATION',
                'FORMAT_ERROR',
                'INAPPROPRIATE_INFORMATION'
            )
        );

-- 적용 확인
SHOW COLUMNS FROM companies LIKE 'latest_rejection_code';
SHOW CREATE TABLE companies;
