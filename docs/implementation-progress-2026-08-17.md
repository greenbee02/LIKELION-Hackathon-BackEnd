# 구현 진행 현황 및 다음 작업 인수인계

## 1. 현재 상태

럭셔리 상품·경험 구매 정보를 구매 QR로 인증하고, 디지털 카드·커스터마이징·컬렉션·리워드로 확장하는 Spring Boot 백엔드다.

현재는 구매 QR 기반 디지털 카드 발급 MVP까지 구현되어 있다.

## 2. 기존 구조에서 바뀐 방향

기존에는 users, social_accounts, products, cards, card_customizations 5개 테이블 중심이었다.

기존 흐름:

~~~text
card_token 입력
→ cards.owner_id에 사용자 연결
→ 카드 등록
~~~

현재 흐름:

~~~text
브랜드·상품·매장
→ 구매 QR 발급
→ QR 인증
→ 디지털 카드 발급
→ 카드 커스터마이징
→ 사용자 컬렉션
→ 리워드·이벤트·실물 카드
~~~

## 3. 현재 반영된 주요 테이블

### 사용자·인증

- users
- social_accounts

### 브랜드·상품

- brands
- stores
- products
- product_collections
- product_collection_items

### 구매·디지털 카드

- purchase_qrs
- card_templates
- cards
- card_customizations
- ai_resource_generations

### 아직 API 구현 전인 확장 영역

- collections
- collection_cards
- rewards
- events
- collection_rewards
- user_rewards
- physical_cards
- AI 분석·추천 테이블

## 4. 확정된 설계

### 상품

products는 일반 상품과 경험을 함께 관리한다.

~~~text
PRODUCT | ART | GASTRONOMY | TRAVEL | EVENT | OTHER
~~~

상품과 공식 컬렉션은 직접 연결하지 않고 product_collection_items를 사용한다.

~~~text
PRODUCTS N ─── M PRODUCT_COLLECTIONS
                 └─ PRODUCT_COLLECTION_ITEMS
~~~

product_collection_items는 다음 구조다.

~~~text
id PK
product_collection_id FK
product_id FK
is_required
display_order
created_at
updated_at
~~~

(product_collection_id, product_id)는 UNIQUE다.

### 구매 QR

- purchase_qrs.qr_token은 UNIQUE다.
- QR 하나당 디지털 카드 하나만 발급한다.
- QR 조회·카드 생성·QR 사용은 하나의 트랜잭션으로 처리한다.
- QR 조회에는 비관적 잠금을 사용한다.
- 만료 QR과 사용된 QR은 재사용할 수 없다.

### 디지털 카드

주요 컬럼:

~~~text
user_id
product_id
purchase_qr_id
template_id
original_card_type
card_type
status
selected_customization_id
purchase_date
purchase_store_id
serial_number
issued_at
~~~

카드 타입:

~~~text
original_card_type: BASIC | COLLECTOR
card_type: BASIC | CUSTOMIZE | COLLECTOR
~~~

카드 상태:

~~~text
ACTIVE | BLOCKED | REVOKED
~~~

상품의 is_limited가 TRUE이면 최초 카드 타입은 COLLECTOR다.

DB 컬럼명은 purchase_store_id를 유지하고, API 응답에서는 storeId로 제공한다.

### 카드 템플릿

앞면·뒷면 이미지 구조를 유지한다.

~~~text
front_image_url
back_image_url
allowed_card_type
~~~

allowed_card_type이 NULL이면 BASIC·COLLECTOR 원본 카드 모두에서 사용할 수 있다.

### 카드 커스터마이징

카드 하나에 여러 생성 이력을 저장한다.

~~~text
CARDS 1 ─── N CARD_CUSTOMIZATIONS
~~~

현재 생성 결과 컬럼:

~~~text
generated_front_image_url
generated_back_image_url
generated_message
customization_data
~~~

상태:

~~~text
PENDING
COMPLETED
FAILED
REJECTED
ARCHIVED
~~~

- REJECTED: 검수·정책 기준을 통과하지 못한 결과
- ARCHIVED: 정상 생성됐지만 현재 사용하지 않는 과거 이력

현재 선택 결과는 card_customizations.is_selected가 아니라 cards.selected_customization_id로 관리한다.

### 실물 카드

~~~text
physical_token
digital_card_id
user_reward_id
~~~

- PURCHASE_CARD는 digital_card_id만 연결한다.
- REWARD_PASS는 user_reward_id만 연결한다.
- 디지털 카드 하나당 실물 카드 한 장만 허용한다.

## 5. Flyway 마이그레이션

기존 V1~V3는 수정하지 않고 V4를 추가했다.

파일:

src/main/resources/db/migration/V4__expand_product_and_card_domain.sql

데이터 이전 전략:

- 기존 cards.card_token은 레거시 호환을 위해 유지하되 신규 발급에서는 사용하지 않는다.
- 기존 cards.owner_id를 새 user_id로 복사한다.
- 기존 purchased_at을 purchase_date로 복사한다.
- 기존 registered_at 또는 created_at을 issued_at으로 복사한다.
- 기존 card_customizations는 card_customizations_legacy로 보존한다.
- 새 card_customizations 테이블은 1:N 구조로 생성한다.
- 기존 커스터마이징 데이터는 새 테이블로 복사한다.

주의:

- 기존 상품의 brand_id는 브랜드 매핑이 필요하다.
- 기존 카드의 문자열 purchased_store는 stores.id 매핑이 필요하다.
- 기존 카드의 purchase_qr_id는 별도 연결 정책이 필요하다.
- 레거시 컬럼은 데이터 검증 후 별도 마이그레이션에서 제거한다.

## 6. 구현된 Java 코드

### Catalog 도메인

- Brand
- Store
- Product
- ProductCollection
- ProductCollectionItem
- CardTemplate
- 관련 Repository

### Card 도메인

- PurchaseQr
- Card
- CardCustomization
- CardType
- CardStatus
- CustomizationStatus
- 관련 Repository

## 7. 구현된 API

모든 카드 API는 JWT 인증이 필요하다.

### 카드 발급

~~~http
POST /api/v1/cards/registrations
~~~

요청:

~~~json
{
  "qrToken": "MCM-DEMO-2026-001"
}
~~~

### 카드 조회

~~~http
GET /api/v1/cards
GET /api/v1/cards/{cardId}
~~~

### 커스터마이징

~~~http
POST /api/v1/cards/{cardId}/customizations
GET /api/v1/cards/{cardId}/customizations
POST /api/v1/cards/{cardId}/customizations/{customizationId}/select
POST /api/v1/cards/{cardId}/restore-original
~~~

현재 커스터마이징 생성은 실제 AI가 아닌 Mock 방식이다.

- 템플릿 앞·뒷면 이미지를 결과로 사용한다.
- 생성 상태는 즉시 COMPLETED로 저장한다.
- ai_model은 mock-v1로 저장한다.

### AI 리소스 생성

카드 완성본을 AI에게 통째로 맡기지 않고, 사용자가 조합할 후보 리소스를 별도 이력으로 관리한다.

- 배경, 테두리, 패턴, 장식, 색상 조합, 문구 스타일, 상품 각도 이미지, 조합 추천을 지원한다.
- `ai_resource_generations`는 카드·상품·템플릿·요청 옵션·생성 결과·처리 상태를 저장한다.
- 생성 요청은 `PENDING`으로 저장하고, 실제 provider가 결과를 저장할 수 있도록 `COMPLETED`, `FAILED`, `REJECTED`, `ARCHIVED` 상태를 둔다.
- 최종 사용자가 선택한 리소스 조합은 기존 `card_customizations.customization_data`에 기록한다.

API:

~~~text
POST /api/v1/cards/{cardId}/ai-resources
GET  /api/v1/cards/{cardId}/ai-resources
GET  /api/v1/cards/{cardId}/ai-resources/{resourceId}
~~~

현재 구현은 실제 AI 호출 없이 생성 요청을 `PENDING`으로 저장한다. 다음 단계에서 provider worker가 PENDING 작업을 가져가 이미지 저장소와 AI 모델을 호출하고 결과 상태를 갱신해야 한다.

## 8. 검증 결과

### H2

~~~powershell
.\gradlew.bat test --no-daemon
~~~

총 9개 테스트가 통과했다.

- 기존 인증 테스트
- 카드 발급
- 카드 목록·상세 조회
- QR 중복 사용 차단
- 커스터마이징 선택·원본 복원
- 동일 QR 동시 등록

### PostgreSQL

PostgreSQL 16.15에서 V1~V4 마이그레이션과 애플리케이션 기동을 검증했다.

~~~text
Successfully validated 4 migrations
Successfully applied 4 migrations
Started CardCollectionApplication
~~~

## 9. 다음 작업 우선순위

### 작업 의존성 안내

PostgreSQL 시드 데이터와 기존 데이터 보완은 아래 API 작업의 **코딩 선행조건은 아니다**.

다음 작업은 테스트 픽스처로 먼저 구현할 수 있다.

- 상품 목록·상세 조회
- 공식 컬렉션 목록·상세 조회
- 상품 필터링
- 카드 템플릿 조회
- 비활성 상품·템플릿 검증
- 카드 상태·권한 검증

다만 실제 PostgreSQL에서 정상 응답을 확인하려면 브랜드·매장·상품·컬렉션·템플릿·QR 시드 데이터가 필요하다. 따라서 권장 흐름은 다음과 같다.

~~~text
API·서비스·테스트 픽스처 구현
→ H2 통합 테스트
→ PostgreSQL 시드 데이터 등록
→ 실제 PostgreSQL 엔드투엔드 검증
→ 기존 데이터 보완 및 레거시 컬럼 정리
~~~

### 1순위: 데이터와 조회 API

- PostgreSQL 브랜드·매장·상품·템플릿·QR 시드 데이터 등록
- 기존 상품 brand_id 보완
- 기존 카드 매장·QR 매핑
- 상품 목록·상세 조회 API
- 공식 컬렉션 목록·상세 조회 API
- 카드 템플릿 조회 API

예정 API:

~~~text
GET /api/v1/products
GET /api/v1/products/{productId}
GET /api/v1/product-collections
GET /api/v1/product-collections/{collectionId}
GET /api/v1/cards/{cardId}/templates
~~~

### 2순위: 카드 발급 안정화

- 비활성 상품 QR 차단
- 카드 템플릿 브랜드·카드 타입 검증 강화
- 레거시 카드 조회 정책 확정
- PostgreSQL 동시성 테스트 확대
- DB UNIQUE 오류를 도메인 오류로 변환

### 3순위: 실제 AI 리소스 처리

- `ai_resource_generations`의 PENDING 작업을 처리하는 worker 추가
- 이미지 생성 provider와 API 키·모델 설정 연결
- 생성 결과를 영구 이미지 저장소에 업로드
- AI 성공·실패·검수 거절 처리
- 생성 결과를 `card_customizations.customization_data`에 선택 저장
- 사용자가 선택한 배경·테두리·상품 각도 이미지를 조합해 미리보기 제공

### 4순위: 사용자 컬렉션

- 컬렉션 생성·수정·삭제
- 카드 추가·제거
- 카드 소유권 검증
- 컬렉션 달성률 계산

### 5순위: 리워드·이벤트·실물 카드

- 컬렉션 달성 조건 계산
- 리워드 해금·수령·만료
- 이벤트 기간·정원 검증
- physical_token 발급
- 실물 카드 활성화·사용·만료

## 10. 먼저 확인할 파일

1. docs/erd.md
2. DataBase/Schema.sql
3. src/main/resources/db/migration/V4__expand_product_and_card_domain.sql
4. src/main/java/com/cju/likelion/cardcollection/card/service/CardService.java
5. src/main/java/com/cju/likelion/cardcollection/card/controller/CardController.java
6. src/test/java/com/cju/likelion/cardcollection/card/CardControllerIntegrationTest.java
7. docs/api-contract.md

## 11. 실행 방법

### H2 테스트

~~~powershell
cd D:\cupToLion\LIKELION-Hackathon-BackEnd
.\gradlew.bat test --no-daemon
~~~

### PostgreSQL 실행

~~~powershell
docker run --name luxury-card-postgres `
  -e POSTGRES_DB=cardcollection `
  -e POSTGRES_USER=cardcollection `
  -e POSTGRES_PASSWORD=cardcollection `
  -p 54329:5432 `
  -d postgres:16
~~~

### PostgreSQL 프로필 실행

~~~powershell
$env:DB_URL = "jdbc:postgresql://localhost:54329/cardcollection"
$env:DB_USERNAME = "cardcollection"
$env:DB_PASSWORD = "cardcollection"

.\gradlew.bat bootRun --args="--spring.profiles.active=prod" --no-daemon
~~~

성공 로그:

~~~text
Successfully validated 4 migrations
Successfully applied 4 migrations
Started CardCollectionApplication
~~~

## 12. 주의사항

- 기존 Flyway V1~V3는 수정하지 않는다.
- 새 DB 변경은 V5 이후 새 마이그레이션으로 추가한다.
- DataBase/Schema.sql은 MySQL 참고용이고, 실제 애플리케이션 DB 기준은 Flyway SQL이다.
- 카드 커스터마이징은 현재 Mock 방식이다.
- 컬렉션·리워드·이벤트·실물 카드 Entity와 API는 아직 구현 전이다.
- 기존 레거시 컬럼은 데이터 이전 검증 후 제거한다.
