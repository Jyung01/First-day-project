# MySQL 데이터베이스

첫출근 서비스의 회원, 기업, 공고, 지원, 후기, 고객센터, 관리자 업무 데이터를 저장한다.

## 폴더

- `ddl/`: 모든 변경이 반영된 최신 전체 DDL
- `dummy-data/`: 기준 데이터와 시연용 데이터
- `migration/`: 기존 DB에 순서대로 적용하는 변경 SQL

## 현재 버전

- 최신 변경 버전: V13
- 신규 DB 생성: `ddl/firstday_mysql_current.sql` 실행
- V12 DB 업데이트: `migration/V13__add_member_withdrawal_to_termination_reason.sql` 실행

버전별 변경사항 상세는 `migration/README.md`에서 관리한다.
