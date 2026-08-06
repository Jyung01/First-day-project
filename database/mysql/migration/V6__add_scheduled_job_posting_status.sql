-- V6: 채용공고 상태에 '모집예정'을 추가한다.
-- 모집 시작일 이전에 공고를 미리 작성·등록하고, 예정 상태로 관리하기 위한 변경이다.

ALTER TABLE job_postings
  DROP CHECK chk_job_posting_status;

ALTER TABLE job_postings
  ADD CONSTRAINT chk_job_posting_status
  CHECK (
    status IN (
      '임시저장',
      '모집예정',
      '모집중',
      '마감',
      '숨김',
      '재검토요청',
      '삭제'
    )
  );
