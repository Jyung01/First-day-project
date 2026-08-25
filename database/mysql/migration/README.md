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
| V11 | `V11__add_job_posting_to_cover_letter_ai_reviews.sql` | `cover_letter_ai_reviews.job_posting_id` 추가 |
| V12 | `V12__add_rag_context_to_cover_letter_ai_reviews.sql` | `cover_letter_ai_reviews.rag_context` 추가 |
| V13 | `V13__add_member_withdrawal_to_termination_reason.sql` | `applications.termination_reason` CHECK 허용값에 `회원탈퇴` 추가 |
| V14 | `V14__add_review_requested_at_to_companies.sql` | `companies.review_requested_at` 추가, 심사 큐 인덱스 및 기존 승인대기 기업 백필 |
| V15 | `V15__add_offer_declined_to_application_status.sql` | `applications.current_status` CHECK 허용값에 `입사포기` 추가 |
| V16 | `V16__add_updated_by_to_notices_and_faqs.sql` | `notices.updated_by`, `faqs.updated_by`와 각 외래키 추가 (공지·FAQ 최종 수정자 기록) |

### V16 적용 시 주의

팀 공용 DB에는 `notices.updated_by`가 이 migration 없이 이미 추가되어 있다.
타입이 `bigint`(signed)이고 COMMENT·외래키가 빠진 상태라, V16의 `[A]` 블록을
그대로 실행하면 `Duplicate column name`으로 실패한다.

- **공용 DB**: `[A]`를 건너뛰고 파일 안의 `[B]`(주석 해제 후 실행)로 타입을 교정하고
  외래키를 추가한다. `faqs` 블록은 그대로 실행한다.
- **새로 구축하는 환경**: V1부터 순서대로 돌리므로 `[A]`를 그대로 실행하면 된다.

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

## V11 반영 내용

- `cover_letter_ai_reviews.job_posting_id`(BIGINT UNSIGNED NOT NULL)에 첨삭 대상으로 선택한 채용공고를 저장한다. `cover_letter_id` 다음 위치에 추가하며 `job_postings.job_posting_id`를 FK로 참조한다.
- 인덱스 `idx_cover_letter_ai_reviews_job_posting (job_posting_id)`를 추가한다.
- 재요청마다 새 행을 쌓는 이력 구조에서, 첨삭 결과가 어느 공고를 기준으로 생성됐는지 구분할 수 있게 한다.
- 기존 V10 DB에는 `V11__add_job_posting_to_cover_letter_ai_reviews.sql`을 한 번 실행한다.

## V12 반영 내용

- `cover_letter_ai_reviews.rag_context`(JSON NULL)에 첨삭 생성 시 pgvector에서 검색해 프롬프트에 넣은 유사 공고 문단을 문항 순서와 같은 순서의 배열로 저장한다. `feedback` 다음 위치에 추가한다. (REQ-903)
- 나중에 어떤 근거로 첨삭이 만들어졌는지 되짚을 수 있게 한다. 기존 행에는 근거가 없으므로 NULL을 허용한다.
- 기존 V11 DB에는 `V12__add_rag_context_to_cover_letter_ai_reviews.sql`을 한 번 실행한다.

## V13 반영 내용

- `applications.termination_reason` CHECK 제약(`chk_applications_termination`) 허용값에 `회원탈퇴`를 추가한다. 기존에는 `기업탈퇴`만 허용했다.
- 회원(지원자) 탈퇴로 진행 중이던 지원이 채용종료되는 경우를 기업탈퇴와 구분해서 저장할 수 있게 한다.
- 기존 V12 DB에는 `V13__add_member_withdrawal_to_termination_reason.sql`을 한 번 실행한다.

## V14 반영 내용

- `companies.review_requested_at`(DATETIME(6) NULL)에 가장 최근 심사 요청 시각을 저장한다. `reapply_requested_at` 다음 위치에 추가한다.
- NULL이면 가입은 했으나 아직 심사를 요청하지 않은(기업정보 작성 중) 상태이고, NOT NULL이면 심사를 요청해 관리자 심사 큐 노출 대상이라는 뜻이다.
- 인덱스 `idx_companies_review_queue (approval_status, company_status, review_requested_at)`를 추가한다.
- `reapply_requested_at`은 의미를 바꾸지 않는다. 그 컬럼은 신규심사(NEW)와 재심사(REVIEW) 구분에 계속 사용한다.
- 기존 승인대기 기업이 심사 큐에서 사라지지 않도록 백필한다. 재심사 요청분은 그 시각을, 신규 가입분은 가입 시각을 심사 요청 시각으로 본다.
- 기존 V13 DB에는 `V14__add_review_requested_at_to_companies.sql`을 한 번 실행한다.

## V15 반영 내용

- `applications.current_status` CHECK 제약(`chk_applications_status`)에 `입사포기`를 추가한다. 기존 허용값에 이어 총 11개 상태를 허용한다.
- 최종합격 후 입사를 포기하는 경우를 입사완료·불합격과 구분해서 표현할 수 있게 한다.
- 기존 V14 DB에는 `V15__add_offer_declined_to_application_status.sql`을 한 번 실행한다.

## V16 반영 내용

- `notices.updated_by`, `faqs.updated_by`(BIGINT UNSIGNED NULL)에 마지막으로 수정한 관리자를 저장한다. 두 테이블 모두 `created_by` 다음 위치에 추가한다.
- `created_by`는 최초 등록자로 그대로 두고, 수정할 때마다 `updated_by`만 갱신한다. `inquiries.answered_by`가 이미 같은 역할을 하고 있어 고객센터 3개 화면의 기록 방식을 맞춘다.
- 각각 `fk_notices_updater`, `fk_faqs_updater`로 `users(user_id)`를 참조한다. 관리자 계정이 삭제되면 `ON DELETE SET NULL`로 기록만 비운다.
- NULL 허용이다. 이 migration 이전에 수정된 행과, 등록 후 한 번도 수정하지 않은 행은 NULL로 남는다.
- 기존 V15 DB에는 `V16__add_updated_by_to_notices_and_faqs.sql`을 한 번 실행한다. 단 **팀 공용 DB는 실행 블록이 다르다** — 위 "V16 적용 시 주의"를 먼저 읽을 것.
