# MySQL 변경 이력

기존 DB를 다음 버전으로 변경하기 위한 SQL을 순서대로 보관한다.

## 버전

| 버전 | 파일 | 내용 |
|---|---|---|
| V1 | `V1__initial_schema.sql` | 최초 MySQL 스키마 |
| V2 | `V2__change_skills_to_hierarchy.sql` | `skills`에서 `skill_group`을 제거하고 `parent_id`, `depth` 기반 1·2차 계층 구조 적용 |
| V3 | `V3__change_skills_unique_key.sql` | 기술명 고유 기준을 전체 기술명에서 `(parent_id, skill_name)` 조합으로 변경 |
| V4 | `V4__add_company_rejection_code.sql` | `companies.latest_rejection_code`와 허용 코드 CHECK 제약 추가 |
| V5 | `V5__make_job_posting_draft_fields_nullable.sql` | 채용공고 임시저장 입력 항목 8개의 NULL 허용 및 문자열 collation 명시 |
| V6 | `V6__add_scheduled_job_posting_status.sql` | 채용공고 상태 CHECK에 `모집예정` 추가 |
| V7 | `V7__add_cover_letter_to_applications.sql` | `applications`에 `cover_letter_id`, `cover_letter_snapshot_json` 및 FK 추가 |
| V8 | `V8__replace_applications_unique_key_with_active_guard.sql` | `applications` 유일성 제약을 `active_application_guard` 생성 컬럼 기반으로 변경, 지원취소 후 재지원 허용 |
| V9 | `V9__add_admin_memo_to_company_reviews.sql` | `company_reviews.admin_memo` 추가 |
| V10 | `V10__add_admin_memo_to_interview_reviews.sql` | `interview_reviews.admin_memo` 추가 |

## 적용 규칙

- 이미 팀 공용 DB에 적용한 migration 파일은 수정하지 않는다.
- 변경사항은 V7, V8처럼 다음 번호의 새 파일로 추가한다.
- V1부터 현재 버전까지 순서대로 적용한다.
- 실행 전 DB를 백업하고, 실행 후 컬럼·인덱스·외래키·CHECK 제약을 확인한다.

## V3 주의사항

- `UNIQUE (parent_id, skill_name)`은 동일 부모 아래의 2차 기술명 중복을 차단한다.
- MySQL은 `NULL`이 포함된 UNIQUE 조합을 여러 건 허용하므로 `parent_id IS NULL`인 1차 기술명 중복은 이 제약만으로 차단되지 않는다.

## V4 반영 내용

- `companies.latest_rejection_code`에 최근 가입 심사 반려 유형 코드를 저장한다.
- 허용값은 `MISSING_INFORMATION`, `FORMAT_ERROR`, `INAPPROPRIATE_INFORMATION`이다.

## V5 반영 내용

- `job_postings.job_category_id`, `employment_type`, `career_type`, `education_level`, `work_region`, `headcount`, `main_tasks`, `qualifications`를 NULL 허용으로 변경한다.
- 문자열 컬럼에 `utf8mb4_unicode_ci` collation을 명시한다.
- 기존 V4 DB에는 `V5__make_job_posting_draft_fields_nullable.sql`을 한 번 실행한다.
- 공고를 `모집중`으로 공개할 때 필수 입력값 검증은 서비스 계층에서 수행해야 한다.

## V6 반영 내용

- `job_postings.status` 허용값에 `모집예정`을 추가한다.
- 기존 V5 DB에는 `V6__add_scheduled_job_posting_status.sql`을 한 번 실행한다.
- 기존 CHECK 제약 `chk_job_posting_status`를 삭제한 뒤 같은 이름으로 다시 생성한다.
- 허용 상태: `임시저장`, `모집예정`, `모집중`, `마감`, `숨김`, `재검토요청`, `삭제`

## V7 반영 내용

- `applications.cover_letter_id`(BIGINT UNSIGNED NULL)에 지원 시 첨부한 자기소개서 원본을 참조한다. `cover_letters.cover_letter_id`를 FK로 참조하며 원본 삭제 시 `ON DELETE SET NULL`로 처리한다.
- `applications.cover_letter_snapshot_json`(JSON NULL)에 지원 완료 시점의 자기소개서 문항·답변 스냅샷을 저장한다. `resume_snapshot_json`과 동일한 패턴이며, 자소서를 첨부하지 않은 지원 건은 NULL이다.
- 인덱스 `idx_applications_cover_letter (cover_letter_id)`를 추가한다.
- 기존 V6 DB에는 `V7__add_cover_letter_to_applications.sql`을 한 번 실행한다.
- 자소서 첨부 필수/선택 여부는 서비스 계층(`JobApplicationService`)에서 요구사항 기준으로 검증한다.

## V8 반영 내용

- 기존 `uk_applications_user_posting (applicant_user_id, job_posting_id)` UNIQUE를 삭제한다.
- `active_application_guard` TINYINT GENERATED ALWAYS AS (STORED) 컬럼을 추가한다. `current_status = '지원취소'`이면 `NULL`, 그 외에는 `1`이다.
- 새 UNIQUE `uk_applications_active (applicant_user_id, job_posting_id, active_application_guard)`를 추가한다. MySQL은 UNIQUE 조합에 `NULL`이 포함되면 중복을 허용하므로, 지원취소 건은 유일성 제약에서 사실상 제외되어 같은 공고에 재지원할 수 있다. 취소하지 않은 지원(`active_application_guard = 1`)은 회원·공고 조합당 1건으로 계속 제한된다.
- 기존 V7 DB에는 `V8__replace_applications_unique_key_with_active_guard.sql`을 한 번 실행한다.

## V9 반영 내용

- `company_reviews.admin_memo`(VARCHAR(2000) NULL)에 관리자가 후기를 숨김/삭제 등으로 처리할 때 남기는 메모를 저장한다. `hidden_by` 다음 위치에 추가한다.
- 기존 V8 DB에는 `V9__add_admin_memo_to_company_reviews.sql`을 한 번 실행한다.

## V10 반영 내용

- `interview_reviews.admin_memo`(VARCHAR(2000) NULL)에 관리자가 후기를 숨김/삭제 등으로 처리할 때 남기는 메모를 저장한다. `hidden_by` 다음 위치에 추가한다.
- 기존 V9 DB에는 `V10__add_admin_memo_to_interview_reviews.sql`을 한 번 실행한다.
