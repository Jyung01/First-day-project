# DB 문서

- `firstday_db_structure_overview_v15.docx`: 데이터베이스 구성과 핵심 관계 개요
- `firstday_table_column_dictionary_v15.docx`: 최신 테이블·컬럼 명세서

DDL 변경 시 컬럼명, 자료형, NULL, 기본값, 키, 제약조건 및 설명을 최신 상태로 함께 수정한다.

## 미반영 사항

두 문서는 V15까지 반영된 상태다. 다음 변경은 아직 문서에 없다.

- V16: `notices.updated_by`, `faqs.updated_by`(BIGINT UNSIGNED NULL)와 외래키 `fk_notices_updater`, `fk_faqs_updater`
