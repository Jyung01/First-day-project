# FirstDay 데이터베이스 v5

첫출근 프로젝트의 MySQL 업무 DB와 PostgreSQL pgvector DB 파일을 관리한다.

## 구조

```text
firstday_database_v5/
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
- 기존 V4 MySQL DB: `mysql/migration/V5__make_job_posting_draft_fields_nullable.sql` 실행
- PostgreSQL: `postgresql/ddl/firstday_postgresql_current.sql` 확인
- 테이블 정의 확인: `docs/firstday_table_column_dictionary_v5.docx`

## MySQL V5 변경사항

- `job_postings.job_category_id`를 NULL 허용으로 변경
- `employment_type`, `career_type`, `education_level`, `work_region`, `headcount`, `main_tasks`, `qualifications`를 NULL 허용으로 변경
- 문자열 컬럼에 `utf8mb4_unicode_ci` collation 명시
