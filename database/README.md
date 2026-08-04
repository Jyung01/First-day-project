# FirstDay 데이터베이스 관리

이 폴더는 Flyway를 사용하지 않는 수동 SQL 관리 구조다.

## 구성

- `mysql/ddl`: 모든 변경사항이 반영된 최신 MySQL 전체 DDL
- `mysql/dummy-data`: 기준 데이터와 시연용 더미데이터
- `mysql/migration`: 최초 스키마와 이후 ALTER 변경 이력
- `postgresql/ddl`: 모든 변경사항이 반영된 최신 PostgreSQL 전체 DDL
- `postgresql/dummy-data`: AI·벡터 저장소 테스트 데이터
- `postgresql/migration`: PostgreSQL 스키마 변경 이력
- `docs`: DB 구조 개요와 테이블·컬럼 명세서

## 관리 원칙

1. `ddl/*_current.sql`은 항상 현재 최종 구조로 갱신한다.
2. `migration`의 기존 파일은 수정하지 않고 다음 버전 파일을 추가한다.
3. 변경 시 최신 DDL, 명세서, 더미데이터 영향 여부를 함께 확인한다.
4. 운영 또는 공용 DB 적용 전 반드시 백업한다.
5. 수동 적용 후 실행한 migration 버전을 팀 채널 또는 PR에 기록한다.


## 현재 버전

- MySQL 최신 변경 버전: V3
- PostgreSQL 최신 변경 버전: V1
