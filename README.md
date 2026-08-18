# Luxury Digital Card Collection

럭셔리 상품·경험 구매를 디지털 카드로 기록하고, 카드 커스터마이징과 컬렉션으로 확장하는 Spring Boot 백엔드다.

## 현재 구현 상태

- 회원가입·로그인·JWT·OAuth2 인증
- 상품·경험 목록·상세·필터 조회
- 공식 컬렉션 및 카드 템플릿 조회
- 구매 QR 기반 디지털 카드 발급
- 카드 목록·상세 조회
- 카드 커스터마이징 Mock 생성·선택·원본 복원
- AI 리소스 생성 요청·비동기 Worker·OpenAI Provider
- AI 리소스 조합 및 카드 적용

실제 OpenAI 이미지 생성은 `BACKGROUND`와 `BORDER` 리소스로 검증했으며, 결과 이미지는 로컬 저장소에서 조회된다.

## 기술 스택

- Java 17+
- Spring Boot 3.4.5
- Spring Data JPA
- Spring Security, JWT, OAuth2 Client
- Flyway
- H2 / PostgreSQL
- Gradle Wrapper

## 실행

### H2 로컬 실행

```powershell
cd D:\cupToLion\LIKELION-Hackathon-BackEnd
.\gradlew.bat bootRun --no-daemon
```

### 테스트

```powershell
.\gradlew.bat test --no-daemon
```

### PostgreSQL 실행

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:54329/cardcollection"
$env:DB_USERNAME = "cardcollection"
$env:DB_PASSWORD = "cardcollection"

.\gradlew.bat bootRun --args="--spring.profiles.active=prod" --no-daemon
```

PostgreSQL 테스트 시드는 [DataBase/test_seed_postgresql.sql](./DataBase/test_seed_postgresql.sql)이다.

## AI 설정

```powershell
$env:AI_ENABLED = "true"
$env:OPENAI_API_KEY = "발급받은_API_KEY"
$env:OPENAI_IMAGE_MODEL = "gpt-image-2"
```

API Key는 Git, 문서, 채팅에 저장하지 않는다.

생성 결과는 기본적으로 `build/generated-ai-resources`에 저장된다. 운영 환경에서는 S3 등 영구 저장소로 교체해야 한다.

## 주요 API

기본 경로는 `/api/v1`이다.

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
POST /cards/{cardId}/customizations
GET  /cards/{cardId}/customizations
POST /cards/{cardId}/customizations/{customizationId}/select
POST /cards/{cardId}/restore-original
POST /cards/{cardId}/ai-resources
GET  /cards/{cardId}/ai-resources
GET  /cards/{cardId}/ai-resources/{resourceId}
POST /cards/{cardId}/ai-resources/compose
```

상품·공식 컬렉션·템플릿 조회는 로그인 없이 사용할 수 있다. 카드·AI API는 JWT 인증이 필요하다.

## 데이터베이스

실제 애플리케이션 DB의 기준은 Flyway 마이그레이션이다.

```text
V1__init.sql
V2__add_social_accounts.sql
V3__add_user_withdrawal.sql
V4__expand_product_and_card_domain.sql
V5__add_ai_resource_generations.sql
```

새로운 DB 변경은 V6 이후 마이그레이션으로 추가한다. `DataBase/Schema.sql`과 `seed_data.sql`은 참고용이다.

## 다음 작업 순서

1. API 계약·문서와 실제 코드 동기화
2. 상품·카드 API 통합 테스트 강화
3. OpenAI 이미지 한도 해결 후 실제 AI 생성 검증
4. 사용자 컬렉션 구현
5. 리워드·이벤트 구현
6. 실물 카드 구현

새로운 기능은 다음 순서를 따른다.

```text
ERD·상태 전이
→ Flyway
→ Entity
→ Repository
→ Service
→ Controller/API
→ 통합 테스트
→ 문서 갱신
```

## 문서

- [구현 현황 및 인수인계](./docs/implementation-handoff.md)
- [2026-08-19 구현 진행 기록 및 다음 작업 순서](./docs/implementation-progress-2026-08-19.md)
- [API 계약](./docs/api-contract.md)
- [ERD](./docs/erd.md)
- [MVP 범위](./docs/mvp-scope.md)
- [초기 데이터 작업 로그](./DataBase/SY_WORK_LOG_2026-08-17.md)
