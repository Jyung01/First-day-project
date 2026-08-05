# MySQL 최신 전체 DDL

`firstday_mysql_current.sql`은 V1부터 V5까지의 모든 변경사항을 반영한 최신 전체 스키마다.

## 사용 방법

- 비어 있는 새 데이터베이스를 생성할 때 실행한다.
- 이미 V1 이상이 적용된 DB에는 전체 DDL을 다시 실행하지 않고 `../migration/`의 다음 버전 SQL만 적용한다.

## V5 반영 내용

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
