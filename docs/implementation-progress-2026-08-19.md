# 2026-08-19 구현 진행 기록 및 다음 작업 순서

## 1. 문서 목적

이 문서는 2026-08-19까지 진행한 작업과 다음 작업자가 바로 이어서 개발할 수 있도록 현재 상태, 검증 결과, 작업 순서를 정리한 문서다.

현행 기준 문서는 다음과 같다.

- implementation-handoff.md
- api-contract.md
- erd.md
- mvp-scope.md

## 2. 이번 작업에서 완료한 내용

### 2.1 문서 상태 동기화

다음 문서의 오래된 구현 상태를 현재 코드 기준으로 수정했다.

- README.md
- docs/implementation-handoff.md
- docs/mvp-scope.md
- docs/api-contract.md
- DataBase/SY_WORK_LOG_2026-08-17.md

현재 기준:

- 상품·경험 조회 API는 구현 완료다.
- 카드 발급·목록·상세 API는 구현 완료다.
- 카드 커스터마이징 Mock은 구현 완료다.
- AI 리소스 요청·Worker·OpenAI Provider·카드 조합은 구현되어 있다.
- 사용자 컬렉션·리워드·이벤트·실물 카드는 아직 구현 전이다.
- 이후 프로젝트 이미지 API 한도 설정을 해소해 `BACKGROUND`와 `BORDER` 실제 생성까지 검증했다.

### 2.3 AI 카드 리소스 비율 표준화

AI가 생성하는 배경·테두리·패턴·상품 각도 등의 이미지가 정사각형으로 저장되지 않도록 국제 카드 표준인 ISO/IEC 7810 ID-1 비율을 적용했다.

- 기준 비율: 85.60×53.98mm, 약 `1.586:1`
- OpenAI 요청 기본 크기: `1536x1024` 가로형
- 저장 전처리: 중앙 크롭 후 `1586x1000`으로 리사이즈
- 프롬프트: 가로형 카드 캔버스와 안전 영역 사용 지시 추가
- 적용 위치: AI Worker가 provider 결과를 이미지 저장소에 저장하기 직전

### 2.2 AI 배경 생성 경로 수정

기존에는 모든 AI 리소스 요청에서 상품 image_url이 자동으로 sourceImageUrl에 들어갔다. 그 결과 배경 생성도 이미지 편집 경로로 처리될 수 있었다.

현재 규칙:

~~~text
BACKGROUND, BORDER, PATTERN, DECORATION,
COLOR_PALETTE, TEXT_STYLE, COMPOSITION
→ 원본 이미지 없이 생성 API 사용

PRODUCT_ANGLE
→ sourceImageUrl 또는 상품 image_url을 원본으로 사용
~~~

수정 파일:

~~~text
src/main/java/com/cju/likelion/cardcollection/ai/service/AiResourceGenerationService.java
~~~

배경 생성 요청의 sourceImageUrl이 비어 있는 것을 확인했다.

### 2.3 PostgreSQL 테스트 데이터 보완

다음 테스트 시드 파일을 추가했다.

~~~text
DataBase/test_seed_postgresql.sql
~~~

포함 데이터:

- 테스트 브랜드
- 테스트 매장
- 테스트 상품
- 활성 카드 템플릿
- 미사용 구매 QR 2개

테스트 QR:

~~~text
MCM-DEMO-2026-001
MCM-AI-TEST-2026-002
~~~

QR은 1회만 사용할 수 있으므로 이미 사용된 QR은 재사용하지 않는다.

### 2.4 실제 서버 검증

완료한 검증:

- PostgreSQL 연결
- Flyway 마이그레이션 적용
- 회원가입·로그인
- JWT 인증
- 구매 QR 기반 카드 발급
- 카드 ID 확보
- AI 리소스 생성 요청 저장
- PENDING → FAILED 상태 전이
- OpenAI API 요청 전달
- `BACKGROUND` 실제 생성: `COMPLETED`
- `BORDER` 실제 생성: `COMPLETED`
- 생성 이미지 URL 접근 및 로컬 저장소 조회

초기에는 다음 제한으로 실패했지만, 이후 계정 설정을 해결한 뒤 재검증에 성공했다.

~~~text
OpenAI project image API limit: 0
~~~

## 3. 현재 구현된 API

기본 경로는 /api/v1이다.

### 인증

~~~text
POST /api/v1/auth/signup
POST /api/v1/auth/login
GET  /api/v1/auth/me
DELETE /api/v1/auth/me
~~~

### 상품·공식 컬렉션·템플릿 조회

~~~text
GET /api/v1/products
GET /api/v1/products/{productId}
GET /api/v1/product-collections
GET /api/v1/product-collections/{collectionId}
GET /api/v1/product-collections/{collectionId}/products
GET /api/v1/card-templates
~~~

상품 목록 필터는 offeringType, category, theme, season, region, limited, page, size다.

### 카드

~~~text
POST /api/v1/cards/registrations
GET  /api/v1/cards
GET  /api/v1/cards/{cardId}
GET  /api/v1/cards/{cardId}/customizations
POST /api/v1/cards/{cardId}/customizations
POST /api/v1/cards/{cardId}/customizations/{customizationId}/select
POST /api/v1/cards/{cardId}/restore-original
~~~

### AI 리소스

~~~text
POST /api/v1/cards/{cardId}/ai-resources
GET  /api/v1/cards/{cardId}/ai-resources
GET  /api/v1/cards/{cardId}/ai-resources/{resourceId}
POST /api/v1/cards/{cardId}/ai-resources/compose
~~~

지원 리소스 유형은 BACKGROUND, BORDER, PATTERN, PRODUCT_ANGLE, DECORATION, COLOR_PALETTE, TEXT_STYLE, COMPOSITION이다.

## 4. 실행 및 검증 방법

### H2 테스트

~~~powershell
cd D:\cupToLion\LIKELION-Hackathon-BackEnd
.\gradlew.bat test --no-daemon
~~~

### PostgreSQL 실행

~~~powershell
$env:DB_URL = "jdbc:postgresql://localhost:54329/cardcollection"
$env:DB_USERNAME = "cardcollection"
$env:DB_PASSWORD = "cardcollection"

.\gradlew.bat bootRun --args="--spring.profiles.active=prod" --no-daemon
~~~

### AI 활성화

~~~powershell
$env:AI_ENABLED = "true"
$env:OPENAI_API_KEY = "발급받은_API_KEY"
$env:OPENAI_IMAGE_MODEL = "gpt-image-2"
~~~

API Key는 문서·Git·채팅에 기록하지 않는다.

### PostgreSQL 테스트 시드

PowerShell 파이프 방식은 한글 인코딩 문제가 발생할 수 있으므로 파일을 컨테이너에 복사해 실행한다.

~~~powershell
docker cp .\DataBase\test_seed_postgresql.sql luxury-card-postgres:/tmp/test_seed_postgresql.sql
docker exec luxury-card-postgres psql -v ON_ERROR_STOP=1 -U cardcollection -d cardcollection -f /tmp/test_seed_postgresql.sql
~~~

## 5. 다음 작업 순서

### 1순위: 상품·카드 API 통합 테스트 보강

#### 상품·컬렉션 테스트

- 상품 목록 기본 조회
- offeringType, category, theme, season, region, limited 필터
- 여러 필터 동시 적용
- 페이지 번호·크기 경계값
- 비활성 상품 목록 제외
- 상품 상세 조회
- 공식 컬렉션 목록·상세·소속 상품 조회
- required, displayOrder 확인
- 존재하지 않는 상품·컬렉션 오류

#### 카드 테스트

- 유효 QR 카드 발급
- 이미 사용된 QR 재사용 차단
- 만료 QR 차단
- 비활성 상품 QR 차단
- 브랜드가 다른 템플릿 차단
- 비활성 템플릿 차단
- BLOCKED·REVOKED 카드 변경 차단
- 다른 사용자의 카드 조회 차단
- 동일 QR 동시 등록
- DB UNIQUE 오류 응답 확인

완료 기준:

~~~text
H2 통합 테스트 통과
PostgreSQL 핵심 시나리오 수동 확인
API 계약 문서와 실제 응답 일치
~~~

### 2순위: 실제 AI 운영 검증

OpenAI 계정 한도 문제가 해결된 후 진행한다.

1. gpt-image-2 배경 생성
2. 테두리 생성
3. PRODUCT_ANGLE 상품 각도 생성
4. COMPLETED와 generatedImageUrl 확인
5. FAILED, REJECTED, 타임아웃 확인
6. AI 리소스 조합 API로 카드 적용
7. 비용과 응답 시간 기록

현재 생성 결과는 build/generated-ai-resources에 저장된다. 운영 배포 전 S3 등 영구 저장소로 교체한다.

### 3순위: 사용자 컬렉션 구현

현재 구현되지 않은 테이블은 collections와 collection_cards다.

~~~text
컬렉션 공개·비공개와 달성률 규칙 확정
→ ERD 수정
→ V6 마이그레이션
→ Collection·CollectionCard Entity
→ Repository
→ Service
→ 생성·수정·삭제 API
→ 카드 추가·제거 API
→ 달성률 계산
→ 권한·중복·빈 컬렉션 테스트
→ API 문서 갱신
~~~

먼저 결정할 내용:

- 컬렉션 공개·비공개 여부
- 컬렉션 이름·설명·대표 이미지 필수값
- 카드 제거 시 카드 보유 상태 유지 여부
- 달성률 분모를 공식 컬렉션 전체 상품으로 할지 목표 카드로 할지
- 컬렉션 중복 카드 허용 여부

### 4순위: 리워드·이벤트

- 컬렉션 달성 조건 계산
- 리워드 발급·수령·만료 상태 전이
- 이벤트 기간·정원 검증
- 중복 수령 방지
- 고객·매장 직원·관리자 권한 구분

### 5순위: 실물 카드

- physical_cards V7 마이그레이션
- physical_token UNIQUE
- digital_card_id, user_reward_id 연결
- PURCHASE_CARD와 REWARD_PASS 구분
- 디지털 카드당 실물 카드 1장 제한
- 활성화·사용·만료 처리

## 6. 작업자가 지켜야 할 구현 순서

~~~text
요구사항·상태 전이 확정
→ ERD 수정
→ Flyway 마이그레이션
→ Entity
→ Repository
→ Service
→ Controller/API
→ 통합 테스트
→ API 계약·인수인계 문서 갱신
~~~

기존 V1~V5 마이그레이션은 수정하지 않는다. 새 DB 변경은 V6부터 추가한다.

## 7. 작업 시작 전 확인 파일

1. docs/implementation-handoff.md
2. docs/api-contract.md
3. docs/erd.md
4. src/main/resources/db/migration/V4__expand_product_and_card_domain.sql
5. src/main/resources/db/migration/V5__add_ai_resource_generations.sql
6. src/main/java/com/cju/likelion/cardcollection/card/service/CardService.java
7. src/main/java/com/cju/likelion/cardcollection/ai/service/AiResourceGenerationService.java
8. src/test/java/com/cju/likelion/cardcollection/card/CardControllerIntegrationTest.java

## 8. 현재 작업 인수인계 결론

다음 작업자는 먼저 상품·카드 API 통합 테스트를 보강한다. 테스트가 통과하면 OpenAI 한도 문제 해결 여부를 확인하고 실제 AI 생성 검증을 진행한다. 그 후 사용자 컬렉션의 공개 범위·달성률 규칙을 확정한 뒤 V6 마이그레이션부터 컬렉션 구현을 시작한다.
