-- =========================================================
-- FirstDay MySQL Migration V14
-- companies에 심사 요청 시각을 추가한다.
--
-- 기존에는 기업 가입 직후 곧바로 approval_status='승인대기'가 되어
-- 관리자 심사 큐에 올라갔다. 그러나 가입 단계에서 채워지는 값은
-- 사업자번호·기업명·업종·규모·주소뿐이라, 대표자명·설립일·기업소개·복지가
-- 빈 상태로 심사를 받게 된다.
--
-- 이 컬럼으로 '작성 중'과 '심사 요청됨'을 구분한다.
--   NULL     = 가입은 했으나 아직 심사를 요청하지 않음(기업정보 작성 중)
--   NOT NULL = 기업이 심사를 요청함. 관리자 심사 큐 노출 대상
--
-- 기존 reapply_requested_at은 의미를 바꾸지 않는다.
-- 그 컬럼은 신규심사(NEW)와 재심사(REVIEW) 구분에 계속 사용한다.
-- 적용 대상: V13까지 적용된 기존 MySQL DB
-- =========================================================

ALTER TABLE companies
    ADD COLUMN review_requested_at DATETIME(6) NULL
        COMMENT '가장 최근 심사 요청 시각; NULL이면 기업정보 작성 중이라 심사 큐에 노출하지 않음'
        AFTER reapply_requested_at,
    ADD KEY idx_companies_review_queue
        (approval_status, company_status, review_requested_at);

-- 기존 승인대기 기업이 심사 큐에서 사라지지 않도록 백필한다.
-- 재심사 요청분은 그 시각을, 신규 가입분은 가입 시각을 심사 요청 시각으로 본다.
UPDATE companies
   SET review_requested_at = COALESCE(reapply_requested_at, created_at)
 WHERE approval_status = '승인대기'
   AND review_requested_at IS NULL;

-- 적용 확인
SHOW COLUMNS FROM companies LIKE 'review_requested_at';
SELECT approval_status,
       COUNT(*)                                                  AS 전체,
       SUM(review_requested_at IS NOT NULL)                      AS 심사요청됨,
       SUM(review_requested_at IS NULL)                          AS 작성중
  FROM companies
 GROUP BY approval_status;
SHOW CREATE TABLE companies;
