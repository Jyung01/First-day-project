-- =========================================================
-- FirstDay MySQL Migration V13
-- applications.termination_reason CHECK 허용값에 '회원탈퇴'를 추가한다.
-- 기존에는 기업탈퇴로 인한 채용종료만 표현할 수 있었으나,
-- 회원(지원자) 탈퇴로 진행 중 지원이 채용종료되는 경우도 구분해서 저장한다.
-- 적용 대상: V12까지 적용된 기존 MySQL DB
-- =========================================================

ALTER TABLE applications
  DROP CONSTRAINT chk_applications_termination,
  ADD  CONSTRAINT chk_applications_termination
       CHECK (termination_reason IS NULL OR termination_reason IN ('기업탈퇴','회원탈퇴'));

-- 적용 확인
SHOW CREATE TABLE applications;
