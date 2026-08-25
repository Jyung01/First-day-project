-- =========================================================
-- FirstDay MySQL Migration V16
-- notices, faqs에 "마지막으로 수정한 관리자"를 기록할 updated_by를 추가한다.
--
-- 기존에는 created_by(최초 등록자)만 남아, 공지·FAQ를 나중에 누가 고쳤는지
-- 알 수 없었다. inquiries는 answered_by가 이미 같은 역할을 하고 있다.
--
-- NULL 허용이다. 이 마이그레이션 이전에 수정된 행은 수정자를 알 수 없고,
-- 앞으로도 등록만 하고 한 번도 수정하지 않은 행은 NULL로 남는다.
--
-- 적용 대상: V15까지 적용된 기존 MySQL DB
--
-- ---------------------------------------------------------
-- !! 팀 공용 DB(EC2)에 적용할 때 주의 !!
--
-- 공용 DB에는 notices.updated_by가 이 마이그레이션 없이 이미 추가되어 있다.
-- 다만 타입이 bigint(signed)이고 COMMENT와 외래키가 빠져 있어,
-- 이 파일의 notices 블록을 그대로 실행하면 Duplicate column name으로 실패한다.
--
-- 공용 DB에서는 아래 [A] 대신 [B]를 실행한다.
-- 새로 구축하는 환경은 V1부터 순서대로 돌리므로 [A]를 그대로 실행하면 된다.
-- =========================================================


-- ---------------------------------------------------------
-- [A] 새 환경용 - notices에 컬럼 신규 추가
--     (공용 DB에서는 이 블록을 건너뛰고 [B]를 실행할 것)
-- ---------------------------------------------------------
ALTER TABLE notices
    ADD COLUMN updated_by BIGINT UNSIGNED NULL
        COMMENT '마지막으로 수정한 관리자; 최초 등록자는 created_by'
        AFTER created_by,
    ADD CONSTRAINT fk_notices_updater
        FOREIGN KEY (updated_by) REFERENCES users(user_id)
        ON DELETE SET NULL;


-- ---------------------------------------------------------
-- [B] 공용 DB용 - 이미 있는 notices.updated_by를 규격에 맞춘다.
--     [A]를 실행한 새 환경에서는 실행하지 않는다.
--
--     signed -> unsigned 변환이라 기존 양수 값은 그대로 보존된다.
--     users.user_id가 BIGINT UNSIGNED라 타입을 먼저 맞춰야 외래키를 걸 수 있다.
-- ---------------------------------------------------------
-- 외래키를 걸기 전에 users에 없는 값이 섞여 있는지 먼저 확인한다.
-- 결과가 0이 아니면 아래 ALTER를 실행하지 말고 해당 행을 먼저 정리한다.
-- SELECT COUNT(*) AS 고아값
--   FROM notices n
--   LEFT JOIN users u ON u.user_id = n.updated_by
--  WHERE n.updated_by IS NOT NULL AND u.user_id IS NULL;
--
-- ALTER TABLE notices
--     MODIFY COLUMN updated_by BIGINT UNSIGNED NULL
--         COMMENT '마지막으로 수정한 관리자; 최초 등록자는 created_by',
--     ADD CONSTRAINT fk_notices_updater
--         FOREIGN KEY (updated_by) REFERENCES users(user_id)
--         ON DELETE SET NULL;


-- ---------------------------------------------------------
-- faqs - 모든 환경 공통. 공용 DB에도 이 컬럼은 없다.
-- ---------------------------------------------------------
ALTER TABLE faqs
    ADD COLUMN updated_by BIGINT UNSIGNED NULL
        COMMENT '마지막으로 수정한 관리자; 최초 등록자는 created_by'
        AFTER created_by,
    ADD CONSTRAINT fk_faqs_updater
        FOREIGN KEY (updated_by) REFERENCES users(user_id)
        ON DELETE SET NULL;


-- 적용 확인
SHOW COLUMNS FROM notices LIKE 'updated_by';
SHOW COLUMNS FROM faqs LIKE 'updated_by';
SHOW CREATE TABLE notices;
SHOW CREATE TABLE faqs;
