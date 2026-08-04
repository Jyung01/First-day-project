-- =========================================================
-- FirstDay MySQL migration V2
-- skills 테이블을 단일 분류 문자열 방식에서 1·2차 자기참조 계층 구조로 변경
-- 작성일: 2026-08-04
-- 적용 전 데이터베이스 백업 필수
-- =========================================================

-- 1. 계층 컬럼을 우선 NULL 허용으로 추가한다.
ALTER TABLE skills
    ADD COLUMN parent_id BIGINT UNSIGNED NULL
        AFTER skill_id,
    ADD COLUMN depth TINYINT UNSIGNED NULL
        AFTER parent_id;

-- 2. 기존 기술 데이터는 모두 1차 기술로 간주해 보정한다.
--    기존 행이 있는 상태에서 바로 NOT NULL로 변경하면 실패할 수 있으므로 반드시 선행한다.
UPDATE skills
SET depth = 1
WHERE depth IS NULL;

-- 3. 기존 문자열 그룹 컬럼과 해당 인덱스를 제거한다.
ALTER TABLE skills
    DROP INDEX idx_skills_group_order,
    DROP COLUMN skill_group;

-- 4. 계층 규칙과 자기참조 외래키를 추가한다.
ALTER TABLE skills
    MODIFY COLUMN depth TINYINT UNSIGNED NOT NULL,
    ADD CONSTRAINT fk_skills_parent
        FOREIGN KEY (parent_id)
        REFERENCES skills(skill_id)
        ON DELETE RESTRICT,
    ADD CONSTRAINT chk_skills_depth
        CHECK (depth IN (1, 2)),
    ADD CONSTRAINT chk_skills_parent_depth
        CHECK (
            (depth = 1 AND parent_id IS NULL)
            OR
            (depth = 2 AND parent_id IS NOT NULL)
        );

-- 5. 같은 부모 아래의 활성 기술을 표시 순서대로 조회하기 위한 인덱스를 추가한다.
CREATE INDEX idx_skills_parent_order
    ON skills (
        parent_id,
        is_active,
        display_order
    );

-- 6. 적용 결과 확인
SHOW COLUMNS FROM skills;
SHOW INDEX FROM skills;
