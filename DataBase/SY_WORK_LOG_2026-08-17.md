# 2026-08-17 작업 내역

> 이 문서는 2026-08-17 초기 상품·이미지·MySQL seed 작업의 역사 기록이다. 현재 애플리케이션 구현 상태와 다음 작업은 [docs/implementation-handoff.md](../docs/implementation-handoff.md)를 기준으로 한다. 현재 실제 애플리케이션 DB 마이그레이션 기준은 PostgreSQL Flyway `src/main/resources/db/migration/V1~V9`이며, 이 문서의 MySQL 내용은 참고 기록이다.

## 이미지 리소스 추가

- 상품, 브랜드 및 카드 템플릿용 이미지 경로 구성
- 카드 템플릿 3종 제작
  - MCM Classic Visetos
  - AW26 Sangria Sunset
  - Seoul Night Edition
- 각 카드 템플릿을 앞면·뒷면 이미지로 분리
- 정적 이미지 제공을 위한 `src/main/resources/static/images` 구조 구성

## 데이터베이스 구축 및 Seed Data 작성

- MySQL 기반 Luxury Collection 데이터베이스 스키마 구축
- 브랜드, 매장, 상품, 공식 컬렉션 데이터 작성
- 상품과 컬렉션의 다대다 관계 구성
- 양면 카드 템플릿 구조 적용
- 리워드, 이벤트 및 컬렉션 달성 조건 작성
- 카드 발급 시연을 위한 미사용 Purchase QR 데이터 작성
- 상품 및 카드 이미지 상대경로 연결

## 데이터베이스 테스트 파일 생성

- `database_test.sql` 생성
- 테이블별 데이터 개수 및 상태 조회
- 상품·컬렉션·리워드·이벤트·QR 통합 조회
- 데이터 참조 관계 및 무결성 검사
- 시연 데이터 초기화 및 전체 데이터 삭제 구문을 선택적 주석 형태로 제공

## 주요 생성 파일

```text
DataBase/
├── luxury_collection_mysql.sql
├── seed_data.sql
└── database_test.sql

src/main/resources/static/images/
├── brands/
├── products/
└── templates/
```
