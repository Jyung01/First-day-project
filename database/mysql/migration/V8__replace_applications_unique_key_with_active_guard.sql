-- =========================================================
-- FirstDay MySQL Migration V8
-- applications의 지원 유일성 제약을 '지원취소' 건은 제외하도록 변경한다.
-- 지원취소 후에는 같은 공고에 재지원할 수 있게 된다.
-- 적용 대상: V7까지 적용된 기존 MySQL DB
-- =========================================================

ALTER TABLE applications
    DROP INDEX uk_applications_user_posting,
    ADD COLUMN active_application_guard TINYINT
        GENERATED ALWAYS AS (
            CASE
                WHEN current_status = '지원취소' THEN NULL
                ELSE 1
            END
        ) STORED,
    ADD UNIQUE KEY uk_applications_active (
        applicant_user_id,
        job_posting_id,
        active_application_guard
    );

-- 적용 확인
SHOW COLUMNS FROM applications LIKE 'active_application_guard';
SHOW CREATE TABLE applications;
