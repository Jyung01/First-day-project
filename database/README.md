# FirstDay 데이터베이스 v16

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
- 기존 V15 MySQL DB: `mysql/migration/V16__add_updated_by_to_notices_and_faqs.sql` 실행 (공용 DB는 `mysql/migration/README.md`의 "V16 적용 시 주의" 참고)
- PostgreSQL: `postgresql/ddl/firstday_postgresql_current.sql` 확인
- 테이블 정의 확인: `docs/firstday_table_column_dictionary_v15.docx` (V15까지 반영. **V16의 `notices.updated_by`·`faqs.updated_by`는 아직 미반영**)

## 버전 이력

MySQL 버전별 변경사항은 `mysql/migration/README.md`에서 관리한다. 여기서는 중복 기록하지 않는다.
