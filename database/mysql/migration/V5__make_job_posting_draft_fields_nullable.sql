-- V5: 채용공고 임시저장 단계에서 일부 입력 항목을 선택값(NULL 허용)으로 변경한다.
-- 적용 대상: V4까지 적용된 MySQL 스키마

ALTER TABLE job_postings
    MODIFY employment_type
        VARCHAR(20)
        COLLATE utf8mb4_unicode_ci
        NULL,

    MODIFY career_type
        VARCHAR(20)
        COLLATE utf8mb4_unicode_ci
        NULL
        DEFAULT NULL,

    MODIFY education_level
        VARCHAR(30)
        COLLATE utf8mb4_unicode_ci
        NULL
        DEFAULT NULL,

    MODIFY work_region
        VARCHAR(100)
        COLLATE utf8mb4_unicode_ci
        NULL,

    MODIFY headcount
        SMALLINT UNSIGNED
        NULL
        DEFAULT NULL,

    MODIFY main_tasks
        LONGTEXT
        COLLATE utf8mb4_unicode_ci
        NULL,

    MODIFY qualifications
        LONGTEXT
        COLLATE utf8mb4_unicode_ci
        NULL;

ALTER TABLE job_postings
    MODIFY job_category_id BIGINT UNSIGNED NULL;

SHOW COLUMNS FROM job_postings;
