# FirstDay 데이터베이스 v13

첫출근 프로젝트의 MySQL 업무 DB와 PostgreSQL pgvector DB 파일을 관리한다.

## 구조

```text
database/
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
- 기존 V12 MySQL DB: `mysql/migration/V13__add_member_withdrawal_to_termination_reason.sql` 실행
- PostgreSQL: `postgresql/ddl/firstday_postgresql_current.sql` 확인
- 테이블 정의 확인: `docs/firstday_table_column_dictionary_v13.docx` (V13 반영 완료)

## 버전 이력

MySQL 버전별 변경사항은 `mysql/migration/README.md`에서 관리한다. 여기서는 중복 기록하지 않는다.
