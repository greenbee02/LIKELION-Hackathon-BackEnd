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
- PostgreSQL 16 (개발·통합 테스트·운영 기준)
- H2 (기존 local 프로필 호환용, V7~V8 전체 마이그레이션 검증 대상 아님)
- Gradle Wrapper

## 실행

### PostgreSQL 기준 실행

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:54329/cardcollection"
$env:DB_USERNAME = "cardcollection"
$env:DB_PASSWORD = "팀에서_정한_DB_비밀번호"
$env:SPRING_PROFILES_ACTIVE = "prod"

.\gradlew.bat bootRun --no-daemon
```

### PostgreSQL 통합 테스트

테스트 전용 DB와 환경변수 설정은 [PostgreSQL 테스트 가이드](./docs/POSTGRESQL_TEST_GUIDE.md)를 따른다.

```powershell
.\gradlew.bat test --no-daemon
```

`DataBase/test_seed_postgresql.sql`은 현재 저장소에 존재하지 않는다. 시연 데이터는 Flyway `V7__insert_demo_seed_data.sql`을 기준으로 관리하며, 추가 시드는 새 Flyway 마이그레이션으로 추가한다.

## AI 설정

```powershell
$env:AI_ENABLED = "true"
$env:OPENAI_API_KEY = "발급받은_API_KEY"
$env:OPENAI_IMAGE_MODEL = "gpt-image-2"
```

API Key는 Git, 문서, 채팅에 저장하지 않는다.

생성 결과는 현재 `build/generated-ai-resources` 로컬 디스크에 저장된다. 재배포·빌드 정리 시 유실될 수 있으므로 S3 등 영구 저장소 전환이 필요하지만, 해당 전환은 현재 MVP 범위 밖이다.

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
V6__add_collection_reward_and_ai_domain.sql
V7__insert_demo_seed_data.sql
V8__add_ai_resource_candidate_groups.sql
V9__add_ai_resource_worker_operability.sql
```

새로운 DB 변경은 V10 이후 마이그레이션으로 추가한다. PostgreSQL Flyway V1~V9이 실행·운영 기준이며, `DataBase/Schema.sql`과 `seed_data.sql`은 MySQL 참고용이다.

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
