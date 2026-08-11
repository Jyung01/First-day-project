# FirstDay 데이터베이스 v10

첫출근 프로젝트의 MySQL 업무 DB와 PostgreSQL pgvector DB 파일을 관리한다.

## 구조

```text
firstday_database_v7/
├─ mysql/
│  ├─ ddl/
│  ├─ dummy-data/
│  └─ migration/
├─ postgresql/
│  ├─ ddl/
│  ├─ dummy-data/
│  └─ migration/
└─ docs/
```

## 사용 기준

- 새 MySQL DB: `mysql/ddl/firstday_mysql_current.sql` 실행
- 기존 V9 MySQL DB: `mysql/migration/V10__add_admin_memo_to_interview_reviews.sql` 실행
- PostgreSQL: `postgresql/ddl/firstday_postgresql_current.sql` 확인
- 테이블 정의 확인: `docs/firstday_table_column_dictionary_v10.docx` (V10 반영 완료)

## MySQL V6 변경사항

- `job_postings.job_category_id`를 NULL 허용으로 변경
- `employment_type`, `career_type`, `education_level`, `work_region`, `headcount`, `main_tasks`, `qualifications`를 NULL 허용으로 변경
- 문자열 컬럼에 `utf8mb4_unicode_ci` collation 명시

- `job_postings.status` 허용값에 `모집예정` 추가
- 기존 상태 CHECK 제약을 삭제한 뒤 동일 이름으로 재생성

## MySQL V7 변경사항

- `applications.cover_letter_id`(BIGINT UNSIGNED NULL) 추가 — 지원에 사용한 자기소개서 원본 참조, `cover_letters.cover_letter_id` FK, `ON DELETE SET NULL`
- `applications.cover_letter_snapshot_json`(JSON NULL) 추가 — 지원 완료 시점의 자기소개서 문항·답변 스냅샷, `resume_snapshot_json`과 동일 패턴
- 인덱스 `idx_applications_cover_letter (cover_letter_id)` 추가

## MySQL V8 변경사항

- 기존 `uk_applications_user_posting (applicant_user_id, job_posting_id)` UNIQUE 삭제
- `active_application_guard` TINYINT GENERATED ALWAYS AS (STORED) 컬럼 추가 — `current_status = '지원취소'`면 NULL, 그 외 1
- 새 UNIQUE `uk_applications_active (applicant_user_id, job_posting_id, active_application_guard)` 추가 — 지원취소 후 같은 공고 재지원 가능

## MySQL V9 변경사항

- `company_reviews.admin_memo`(VARCHAR(2000) NULL) 추가 — 관리자 후기 처리 메모, `hidden_by` 다음 위치

## MySQL V10 변경사항

- `interview_reviews.admin_memo`(VARCHAR(2000) NULL) 추가 — 관리자 후기 처리 메모, `hidden_by` 다음 위치
