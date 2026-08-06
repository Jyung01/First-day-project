# FirstDay 데이터베이스 v6

첫출근 프로젝트의 MySQL 업무 DB와 PostgreSQL pgvector DB 파일을 관리한다.

## 구조

```text
firstday_database_v6/
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
- 기존 V5 MySQL DB: `mysql/migration/V6__add_scheduled_job_posting_status.sql` 실행
- PostgreSQL: `postgresql/ddl/firstday_postgresql_current.sql` 확인
- 테이블 정의 확인: `docs/firstday_table_column_dictionary_v6.docx`

## MySQL V6 변경사항

- `job_postings.job_category_id`를 NULL 허용으로 변경
- `employment_type`, `career_type`, `education_level`, `work_region`, `headcount`, `main_tasks`, `qualifications`를 NULL 허용으로 변경
- 문자열 컬럼에 `utf8mb4_unicode_ci` collation 명시

- `job_postings.status` 허용값에 `모집예정` 추가
- 기존 상태 CHECK 제약을 삭제한 뒤 동일 이름으로 재생성
