# MySQL 최신 전체 DDL

`firstday_mysql_current.sql`은 V1부터 V10까지의 모든 변경사항을 반영한 최신 전체 스키마다.

## 사용 방법

- 비어 있는 새 데이터베이스를 생성할 때 실행한다.
- 이미 V1 이상이 적용된 DB에는 전체 DDL을 다시 실행하지 않고 `../migration/`의 다음 버전 SQL만 적용한다.

## V6 반영 내용

`job_postings`의 다음 컬럼을 NULL 허용으로 변경했다.

- `job_category_id`
- `employment_type`
- `career_type`
- `education_level`
- `work_region`
- `headcount`
- `main_tasks`
- `qualifications`

문자열 컬럼에는 `utf8mb4_unicode_ci` collation을 명시했다.

- `job_postings.status`: `모집예정` 상태 추가

## V7 반영 내용

`applications`에 자기소개서 관련 컬럼을 추가했다.

- `cover_letter_id`: 지원에 사용한 자기소개서 원본 참조 (`cover_letters.cover_letter_id` FK, `ON DELETE SET NULL`)
- `cover_letter_snapshot_json`: 지원 완료 시점의 자기소개서 문항·답변 스냅샷 (`resume_snapshot_json`과 동일 패턴, NULL 허용)
- 인덱스 `idx_applications_cover_letter (cover_letter_id)` 추가

## V8 반영 내용

`applications`의 지원 유일성 제약을 지원취소 건은 제외하도록 변경했다.

- 기존 `uk_applications_user_posting (applicant_user_id, job_posting_id)` UNIQUE 삭제
- `active_application_guard` TINYINT GENERATED ALWAYS AS (STORED) 컬럼 추가 (`지원취소`면 NULL, 그 외 1)
- 새 UNIQUE `uk_applications_active (applicant_user_id, job_posting_id, active_application_guard)` 추가 — 지원취소 후 같은 공고 재지원 가능

## V9 반영 내용

- `company_reviews.admin_memo`(VARCHAR(2000) NULL) 추가 — 관리자 후기 처리 메모

## V10 반영 내용

- `interview_reviews.admin_memo`(VARCHAR(2000) NULL) 추가 — 관리자 후기 처리 메모
