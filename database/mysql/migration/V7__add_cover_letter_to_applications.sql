-- =========================================================
-- FirstDay MySQL Migration V7
-- applications에 지원 시 첨부한 자기소개서 원본 참조와
-- 지원 완료 시점의 자기소개서 스냅샷을 추가한다.
-- 적용 대상: V6까지 적용된 기존 MySQL DB
-- =========================================================

ALTER TABLE applications
    ADD COLUMN cover_letter_id BIGINT UNSIGNED NULL
        COMMENT '지원에 사용한 자기소개서 원본; 원본 삭제 후에는 NULL'
        AFTER resume_snapshot_json,
    ADD COLUMN cover_letter_snapshot_json JSON NULL
        COMMENT '지원 완료 시점의 자기소개서 문항·답변 스냅샷; 자소서 미첨부 시 NULL'
        AFTER cover_letter_id,
    ADD KEY idx_applications_cover_letter (cover_letter_id),
    ADD CONSTRAINT fk_applications_cover_letter
        FOREIGN KEY (cover_letter_id)
        REFERENCES cover_letters (cover_letter_id)
        ON DELETE SET NULL;

-- 적용 확인
SHOW COLUMNS FROM applications LIKE 'cover_letter%';
SHOW CREATE TABLE applications;
