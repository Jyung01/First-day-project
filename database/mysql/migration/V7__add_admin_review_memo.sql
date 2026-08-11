-- 후기 관리 화면에서 남기는 내부 처리 메모를 후기별로 보존한다.
ALTER TABLE company_reviews
    ADD COLUMN admin_memo VARCHAR(2000) NULL
    COMMENT '관리자 후기 처리 메모' AFTER hidden_by;

ALTER TABLE interview_reviews
    ADD COLUMN admin_memo VARCHAR(2000) NULL
    COMMENT '관리자 후기 처리 메모' AFTER hidden_by;
