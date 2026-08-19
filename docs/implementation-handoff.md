# 구현 현황 및 다음 작업 인수인계

> 2026-08-19 기준 현행 문서다. 초기 데이터 작업 기록은 [SY_WORK_LOG_2026-08-17.md](../DataBase/SY_WORK_LOG_2026-08-17.md), API 상세 계약은 [api-contract.md](./api-contract.md)에서 확인한다.
> 오늘 작업 기록과 다음 작업 순서는 [2026-08-19 구현 진행 기록](./implementation-progress-2026-08-19.md)에서 확인한다.

## 1. 프로젝트 개요

럭셔리 상품·경험 구매 정보를 구매 QR로 인증하고, 디지털 카드·AI 리소스·사용자 컬렉션·리워드로 확장하는 Spring Boot 백엔드다.

현재 구현은 다음 흐름을 지원한다.

```text
상품·매장·템플릿
→ 구매 QR 인증
→ 디지털 카드 발급
→ 카드 커스터마이징 또는 AI 리소스 생성
→ AI 리소스 조합 및 카드 적용
```

## 2. 기술 스택 및 실행 환경

- Java 17 이상
- Spring Boot 3.4.5
- Spring Web, Spring Data JPA, Spring Security
- JWT, OAuth2 Client
- Flyway 10.20.1
- 로컬: H2 PostgreSQL 호환 모드
- 검증 DB: PostgreSQL 16
- 빌드: Gradle Wrapper

기본 실행:

```powershell
cd D:\cupToLion\LIKELION-Hackathon-BackEnd
.\gradlew.bat bootRun --no-daemon
```

테스트:

```powershell
.\gradlew.bat test --no-daemon
```

PostgreSQL 실행 시에는 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`를 환경변수로 설정하고 `prod` 프로필을 사용한다.

## 3. 구현 완료 범위

### 인증

- 회원가입·로그인·JWT Access Token
- BCrypt 비밀번호 암호화
- `/auth/me` 조회 및 회원 탈퇴
- 탈퇴 사용자 JWT 차단
- Google·Kakao OAuth 로그인과 일회성 코드 교환

### 상품·컬렉션 조회

- 상품 목록·상세 조회
- `offeringType`, `category`, `theme`, `season`, `region`, `limited` 필터
- 페이지네이션
- 공식 컬렉션 목록·상세·소속 상품 조회
- 카드 템플릿 목록 조회
- 비활성 상품은 공개 상품 조회에서 제외

### 카드

- 구매 QR 기반 카드 발급
- QR 조회 시 비관적 잠금
- QR 1개당 카드 1장 발급
- QR 만료·중복 사용 차단
- 상품 `is_limited`에 따른 BASIC·COLLECTOR 최초 타입 결정
- 내 카드 목록·상세 조회
- 카드 상태 `ACTIVE`, `BLOCKED`, `REVOKED` 검증
- 카드 소유자·템플릿 브랜드·템플릿 활성 상태 검증
- 카드 커스터마이징 Mock 생성·선택·원본 복원
- `cards.selected_customization_id`로 현재 선택 결과 관리

### AI 리소스

- 배경·테두리·패턴·장식·색상·문구·레이아웃 등 리소스 요청
- `PENDING → COMPLETED | FAILED | REJECTED | ARCHIVED` 상태 관리
- 비동기 Worker
- OpenAI Images API Provider
- 로컬 생성 이미지 저장소
- 완료된 AI 리소스 여러 개를 카드에 조합
- 조합 결과를 `card_customizations`에 저장하고 카드 타입을 `CUSTOMIZE`로 변경

AI 리소스 생성은 상품 원본 이미지와 독립적으로 수행한다. 상품 이미지는 AI 리소스가 아니라 `PRODUCT` 레이어의 기본 이미지로 사용한다.

## 4. 데이터베이스 및 마이그레이션

실제 애플리케이션 DB의 기준은 Flyway SQL이다. `DataBase/Schema.sql`과 기존 MySQL seed는 참고·시연용으로 사용한다.

```text
V1__init.sql
V2__add_social_accounts.sql
V3__add_user_withdrawal.sql
V4__expand_product_and_card_domain.sql
V5__add_ai_resource_generations.sql
```

주요 테이블:

```text
users, social_accounts
brands, stores, products
product_collections, product_collection_items
purchase_qrs, cards, card_templates, card_customizations
ai_resource_generations
```

확정된 규칙:

- 상품과 공식 컬렉션은 `product_collection_items`로 연결한다.
- `(product_collection_id, product_id)`는 UNIQUE다.
- DB 컬럼은 `purchase_store_id`를 유지하고 API 응답에서는 `storeId`로 제공한다.
- 카드 원본 타입은 `original_card_type`에 보존한다.
- AI 선택 결과는 `cards.selected_customization_id`가 참조한다.
- 새 DB 변경은 V6 이후 마이그레이션으로 추가한다.

PostgreSQL 테스트 데이터:

```text
DataBase/test_seed_postgresql.sql
```

## 5. 주요 API

기본 경로는 `/api/v1`이다. 인증이 필요한 API는 `Authorization: Bearer {accessToken}`을 사용한다.

```text
POST /auth/signup
POST /auth/login
GET  /products
GET  /products/{productId}
GET  /product-collections
GET  /product-collections/{collectionId}
GET  /product-collections/{collectionId}/products
GET  /card-templates
POST /cards/registrations
GET  /cards
GET  /cards/{cardId}
GET  /cards/{cardId}/customizations
POST /cards/{cardId}/customizations
POST /cards/{cardId}/customizations/{customizationId}/select
POST /cards/{cardId}/restore-original
POST /cards/{cardId}/ai-resources
POST /cards/{cardId}/ai-resources/batch
GET  /cards/{cardId}/ai-resources
GET  /cards/{cardId}/ai-resources/{resourceId}
POST /cards/{cardId}/ai-resources/compose
```

요청·응답 예시는 [api-contract.md](./api-contract.md)를 기준으로 한다.

배치 생성은 리소스 종류를 최대 8개까지 한 번에 등록할 수 있으며, 각 리소스 종류마다 3~4개의 후보를 생성한다. 카드의 구매 매장 도시를 기반으로 지역 문맥과 후보 번호를 자동 부여하므로, 서울 구매 건은 광화문·남산·한강 등 서로 다른 지역 후보를 사용한다. 같은 종류의 후보는 `candidateGroupId`로 묶이고, 각 그룹에서 하나만 선택해 `compose`에 전달한다.

## 6. 실제 AI 연동 상태

코드와 Worker 연동이 완료됐고, 실제 OpenAI `BACKGROUND`·`BORDER` 이미지 생성 및 결과 저장까지 검증했다. 이후 다른 리소스 타입과 운영용 이미지 저장소를 추가 검증한다.

최근 검증 결과:

```text
gpt-image-2: Project 접근 가능
API 요청: 정상 전달
애플리케이션 상태 전이: PENDING → FAILED 정상
실패 원인: OpenAI 프로젝트 이미지 API 한도 0
```

크레딧·사용 한도 문제가 해결되면 서버 재실행 후 새 AI 리소스 요청으로 재검증한다. 기존 `FAILED` 이력은 삭제하지 않는다.

운영 환경에서는 로컬 저장소를 S3 등 영구 저장소로 교체해야 한다.

## 7. 테스트 현황

- 인증 통합 테스트
- 카드 발급·목록·상세 테스트
- QR 중복 사용 및 동시 등록 테스트
- 커스터마이징 선택·원본 복원 테스트
- 상품 조회 및 필터 테스트
- AI 리소스 PENDING·Worker·조합 테스트
- H2 기반 Gradle 테스트 통과
- PostgreSQL V1~V4 마이그레이션 및 애플리케이션 기동 검증

## 8. 앞으로의 작업 순서

### 1순위: 문서·계약 동기화

- README·MVP 범위·인수인계 문서의 구현 상태 일치 확인
- API 응답 예시와 실제 DTO 비교
- 프론트엔드가 사용할 오류 코드·빈 상태·로딩 상태 확정

### 2순위: 상품·카드 API 테스트 강화

- 상품 필터 조합·페이지 경계 테스트
- 공식 컬렉션과 상품 연결 무결성 테스트
- 비활성 상품·템플릿 발급 차단 테스트
- 카드 상태·소유권·브랜드 권한 테스트
- PostgreSQL UNIQUE·FK 오류를 도메인 오류로 변환

### 3순위: 실제 AI 운영 검증

- OpenAI 이미지 한도 해결 후 배경·테두리·상품 기본 이미지 조합 테스트
- 타임아웃·재시도·정책 거절 처리
- 생성 결과 영구 저장소 연결
- 비용·응답 시간·실패 로그 모니터링

### 4순위: 사용자 컬렉션

새로운 저장 관계가 필요하므로 다음 순서로 진행한다.

```text
컬렉션 ERD 확정
→ V6 마이그레이션
→ Collection Entity·Repository
→ 생성·수정·삭제 API
→ 카드 추가·제거 API
→ 달성률 계산
→ 권한·중복·빈 컬렉션 테스트
```

### 5순위: 리워드·이벤트

- 컬렉션 달성 조건
- 리워드 발급·수령·만료
- 이벤트 기간·정원 검증
- 중복 수령 방지

### 6순위: 실물 카드

- `physical_cards` 마이그레이션·Entity
- `physical_token`, `digital_card_id`, `user_reward_id` 연결
- 구매 카드와 리워드 패스 구분
- 활성화·사용·만료 처리
- 디지털 카드당 실물 카드 1장 제한

새 기능의 기본 구현 순서는 다음과 같다.

```text
ERD·상태 전이 확정
→ Flyway 마이그레이션
→ Entity
→ Repository
→ Service
→ Controller/API
→ 통합 테스트
→ API 문서 갱신
```

## 9. 주의사항

- V1~V5 기존 마이그레이션은 수정하지 않는다.
- JWT Secret·OAuth Secret·OpenAI API Key는 환경변수로만 관리한다.
- API Key를 Git이나 문서에 기록하지 않는다.
- `DataBase/seed_data.sql`은 MySQL 참고용이며 Flyway 마이그레이션으로 사용하지 않는다.
- 컬렉션·리워드·이벤트·실물 카드는 아직 Entity와 API가 구현되지 않았다.
