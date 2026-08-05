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

## 적용 규칙

- 이미 팀 공용 DB에 적용한 migration 파일은 수정하지 않는다.
- 변경사항은 V6, V7처럼 다음 번호의 새 파일로 추가한다.
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
