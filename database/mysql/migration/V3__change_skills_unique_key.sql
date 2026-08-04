-- =========================================================
-- FirstDay MySQL migration V3
-- skills 기술명 고유 제약을 전체 단위에서 부모 기술 단위로 변경
-- 작성일: 2026-08-04
-- 적용 전 데이터베이스 백업 필수
-- 선행 버전: V2__change_skills_to_hierarchy.sql
-- =========================================================

-- 1. 기존 전체 기술명 UNIQUE 인덱스를 제거한다.
-- 2. 동일한 부모 아래에서는 같은 기술명을 등록할 수 없도록 복합 UNIQUE 제약을 추가한다.
ALTER TABLE skills
    DROP INDEX uk_skills_name,
    ADD CONSTRAINT uk_skills_parent_name
        UNIQUE (parent_id, skill_name);

-- 적용 결과 확인
SHOW INDEX FROM skills;

-- 주의:
-- MySQL UNIQUE 인덱스는 NULL을 서로 다른 값으로 취급한다.
-- 따라서 parent_id가 NULL인 1차 기술은 skill_name 중복이 허용될 수 있다.
-- 이 제약은 같은 parent_id를 가진 2차 기술명의 중복을 방지하는 용도로 동작한다.
