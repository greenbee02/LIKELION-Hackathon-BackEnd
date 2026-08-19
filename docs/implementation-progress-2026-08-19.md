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

AI가 생성하는 배경·테두리·패턴·상품 각도 등의 이미지가 정사각형으로 저장되지 않도록 국제 카드 표준인 ISO/IEC 7810 ID-1 비율을 세로 방향으로 적용했다.

- 기준 비율: 85.60×53.98mm, 약 `1.586:1`
- OpenAI 요청 기본 크기: `1024x1536` 세로형
- 저장 전처리: 중앙 크롭 후 `1000x1586`으로 리사이즈
- 프롬프트: 세로형 카드 캔버스와 안전 영역 사용 지시 추가
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

### 2.5 지역 기반 AI 리소스 및 배치 생성

구매 매장에 따라 카드 리소스의 분위기가 달라지도록 카드의 `purchaseStore.city`를 AI 프롬프트에 자동으로 반영했다.

- 서울: 광화문, 남산서울타워, 한강 야경, 북촌 기와 후보 사용
- 부산: 광안대교, 해운대, 감천문화마을, 부산항 후보 사용
- 제주: 한라산, 현무암 돌담, 성산일출봉, 제주 해안 후보 사용
- 도쿄·뉴욕 등 주요 도시 후보 지원
- 등록되지 않은 도시는 해당 도시의 지역 건축·풍경을 일반 문맥으로 사용
- 같은 카드에서 이미 사용한 지역 변형 번호를 확인해 다음 후보를 사용
- 이미지·로고·읽을 수 있는 문구를 그대로 복제하지 않고 지역의 실루엣·건축·분위기만 참고하도록 프롬프트 구성

기존에는 AI 리소스를 한 번에 하나씩 요청했지만, 다음 배치 API를 추가했다.

~~~text
POST /api/v1/cards/{cardId}/ai-resources/batch
~~~

- 한 번에 3~4개 요청
- 각 리소스는 독립적인 `PENDING` 이력으로 저장
- `BACKGROUND`, `BORDER`, `PATTERN`, `PRODUCT_ANGLE` 등 서로 다른 유형을 조합 가능
- 완료된 결과는 기존 `compose` API에서 사용자가 선택해 카드에 적용

현재 worker는 배치 요청을 한 번에 접수한 뒤 PENDING 작업을 순차 처리한다. 실제 AI 요청을 병렬로 실행하려면 작업 점유 상태와 동시성 제어를 추가해야 한다.

수정 파일:

~~~text
src/main/java/com/cju/likelion/cardcollection/ai/dto/AiResourceBatchGenerationRequest.java
src/main/java/com/cju/likelion/cardcollection/ai/controller/AiResourceGenerationController.java
src/main/java/com/cju/likelion/cardcollection/ai/service/AiResourceGenerationService.java
src/main/java/com/cju/likelion/cardcollection/ai/provider/OpenAiImageProvider.java
src/test/java/com/cju/likelion/cardcollection/card/CardControllerIntegrationTest.java
~~~

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
POST /api/v1/cards/{cardId}/ai-resources/batch
GET  /api/v1/cards/{cardId}/ai-resources
GET  /api/v1/cards/{cardId}/ai-resources/{resourceId}
POST /api/v1/cards/{cardId}/ai-resources/compose
~~~

지원 리소스 유형은 BACKGROUND, BORDER, PATTERN, PRODUCT_ANGLE, DECORATION, COLOR_PALETTE, TEXT_STYLE, COMPOSITION이다.

### AI 지역 문맥 및 배치 생성

- 카드의 구매 매장 `stores.city`를 AI 프롬프트에 자동 반영한다.
- 서울은 광화문, 남산서울타워, 한강 야경, 북촌 기와 등 지역 후보를 순환 사용한다.
- 같은 카드에서 이전에 사용한 지역 변형 수를 기준으로 다음 후보를 선택해 반복을 줄인다.
- `POST /api/v1/cards/{cardId}/ai-resources/batch`로 3~4개 리소스를 한 번에 `PENDING` 등록한다.
- 각 항목은 독립적인 생성 이력으로 저장되며, 완료 후 기존 compose API에서 원하는 결과만 조합한다.

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

1. ~~gpt-image-2 배경 생성~~ (작업 완료)
2. ~~테두리 생성~~ (작업 완료)
3. PRODUCT_ANGLE 상품 각도 생성
4. 지역 기반 배경 3~4개 배치 생성
5. 각 결과의 `COMPLETED`와 `generatedImageUrl` 확인
6. `FAILED`, `REJECTED`, 타임아웃 확인
7. AI 리소스 조합 API로 카드 적용
8. 비용과 응답 시간 기록

현재 생성 결과는 build/generated-ai-resources에 저장된다. 운영 배포 전 S3 등 영구 저장소로 교체한다.

### 2.5순위: 지역 배치 생성 및 카드 편집 연동

오늘 추가한 기능을 실제 사용자 흐름으로 검증한다.

1. 서버 재시작 후 `/ai-resources/batch` 호출
2. 서울·부산·제주 매장별 배경 후보 생성
3. 같은 카드에서 두 번째 배치 요청 시 지역 후보 중복 여부 확인
4. 3~4개 결과가 모두 `COMPLETED`가 될 때까지 상태 조회
5. 완료된 배경·테두리·패턴 중 원하는 리소스만 `compose`에 전달
6. 카드가 `CUSTOMIZE`로 변경되고 `card_customizations.customization_data`에 조합 정보가 저장되는지 확인

### 2.6순위: 구조화 추천 전환

`COLOR_PALETTE`, `TEXT_STYLE`, `COMPOSITION`은 이미지 생성 없이 OpenAI Structured Outputs를 이용한 JSON 추천으로 처리한다. 완료 결과는 이미지 URL 없이 `generatedData`에 저장한다.

- `COLOR_PALETTE`: 팔레트명, 주요·보조·강조·배경·텍스트 색상, rationale
- `TEXT_STYLE`: 폰트 계열, 굵기, 크기, 자간, 줄 간격, 글자 색상, 정렬, 최대 줄 수, 정규화 좌표
- `COMPOSITION`: `1000x1586` 캔버스, 배경색, 레이어별 유형·정규화 좌표·크기·회전·투명도·순서
- AI 결과를 프론트엔드 미리보기 카드로 표시
- 선택된 추천값을 `layoutData`와 `customization_data`에 저장

### 2.7순위: 프론트엔드 카드 편집기 연동

AI는 추천값과 리소스를 제공하고, 실제 배치 조작은 프론트엔드가 담당한다.

- 텍스트·배경·테두리·상품 이미지 레이어 표시
- 드래그, 크기 조절, 회전, 정렬, 투명도 조절
- 카드 세로 비율 `1000x1586` 유지
- 안전 영역 밖 요소 경고
- 편집 완료 시 `POST /api/v1/cards/{cardId}/ai-resources/compose` 호출
- 저장 후 카드 상세에서 선택된 커스터마이징 조회

### 2.8순위: 실제 동시 처리 및 운영 저장소

- worker에 PENDING 작업 점유 상태 추가
- 동일 리소스의 중복 처리 방지
- 3~4개 AI 요청 병렬 처리 여부 결정
- OpenAI rate limit에 맞춘 동시 실행 수 제한
- 실패한 작업 재시도 정책 확정
- `build/generated-ai-resources`를 S3 등 영구 저장소로 교체
- 이미지 접근 권한과 만료 URL 정책 확정

### 2.9순위: 기본 카드 레이어 기반 커스터마이징 백엔드

`compose` API에 레이어 기반 저장 구조를 추가했다.

- 기본 템플릿 앞면 이미지를 `BACKGROUND` 슬롯의 기본 레이어로 자동 포함
- 기본 레이어는 `sourceType = TEMPLATE`, `isDefault = true`, `replaceable = true`로 저장
- 사용자가 같은 슬롯의 AI 리소스를 선택하면 기본 템플릿 레이어를 교체
- `BACKGROUND`, `PRODUCT`, `BORDER`, `PATTERN`, `DECORATION`, `TEXT`, `FINISH` 레이어 지원
- 좌표·크기는 카드 전체 기준 `0~1` 정규화 값 사용
- AI 리소스 레이어는 요청한 카드의 `COMPLETED` 리소스만 사용 가능
- 레이어 유형과 AI 리소스 유형이 맞지 않으면 저장 거부
- `TEXT`는 AI 리소스 없이 문구와 `styleData` 저장 가능
- `PRODUCT`는 AI 상품 각도 리소스 또는 상품 기본 이미지 사용 가능
- 결과는 기존 `card_customizations.customization_data`에 `layers` 배열로 저장

기존 `resourceIds`와 `layoutData` 요청 형식은 호환성을 위해 유지한다.

### 2.10 작업 완료: 교체 가능한 기본 레이어 구조

기존에는 `BASE_CARD`를 `locked = true`로 자동 추가해 사용자가 기본 카드 전체를 바꿀 수 없었다. 현재는 기본 카드 이미지를 잠그는 대신, 교체 가능한 기본 레이어로 저장하도록 변경했다.

#### 변경 전·후

~~~text
변경 전
템플릿 기본 카드 전체
→ BASE_CARD 자동 추가
→ locked = true
→ 사용자가 기본 카드 영역을 직접 교체할 수 없음

변경 후
템플릿 front_image_url
→ BACKGROUND 슬롯의 기본 레이어로 자동 추가
→ sourceType = TEMPLATE
→ isDefault = true
→ replaceable = true
→ 같은 슬롯의 새 리소스가 선택되면 기본 레이어 교체
~~~

#### 동작 규칙

- 레이어 없이 compose하면 템플릿의 `front_image_url`이 기본 `BACKGROUND`로 유지된다.
- `slot = BACKGROUND`, `type = BACKGROUND`인 완료 AI 리소스를 선택하면 템플릿 기본 배경을 제거하고 선택한 리소스를 저장한다.
- `PRODUCT`, `BORDER`, `PATTERN`, `DECORATION`, `TEXT`, `FINISH`는 기본 배경 위에 추가 레이어로 저장할 수 있다.
- compose할 때마다 `card_customizations`에 새 이력이 생성된다.
- 현재 선택된 결과는 기존 `cards.selected_customization_id`로 가리킨다.
- AI 결과와 기존 리소스 이력은 삭제하지 않는다.

#### 레이어 요청 필드

`layers` 배열의 각 요소는 다음 정보를 가진다.

- `id`, `type`, `slot`
- `resourceId`
- `x`, `y`, `width`, `height`: 카드 전체 기준 `0~1` 정규화 좌표
- `rotation`, `opacity`, `zIndex`
- `visible`, `locked`
- `text`, `styleData`

`slot`이 없으면 `type`을 기본 슬롯으로 사용한다. 레이어가 같은 슬롯에 존재하면 새 레이어가 기본 레이어를 교체하는 방식이다.

#### 검증 규칙

- 카드가 존재하고 현재 사용자의 카드이며 `ACTIVE` 상태여야 한다.
- `resourceId`는 해당 카드에 연결된 `COMPLETED` AI 리소스여야 한다.
- 레이어 유형과 AI 리소스 유형이 일치해야 한다.
- 기존 전체 카드 교체 방식인 `BASE_CARD` 요청은 더 이상 허용하지 않는다.
- `TEXT`는 AI 리소스 없이 `text`와 `styleData`만으로 저장할 수 있다.
- `PRODUCT`는 `PRODUCT_ANGLE` 리소스를 사용하거나 리소스 없이 상품 기본 이미지를 사용할 수 있다.

#### 저장 형식 및 호환성

- `customization_data`에 `composition-v2`와 `layers` 배열을 저장한다.
- 기존 클라이언트와의 호환성을 위해 `resourceIds`, `message`, `layoutData` 요청 필드는 유지한다.
- 현재 템플릿 기본 레이어는 기존 `front_image_url`을 fallback으로 사용한다.
- 앞면 배경·상품·테두리 같은 자산을 템플릿에서 독립적으로 관리하려면 추후 `card_template_layers` 테이블 또는 템플릿 레이어 JSON 구조를 추가해야 한다. 해당 확장은 아직 구현하지 않았다.

#### 검증 결과

~~~text
.\\gradlew.bat test --no-daemon
BUILD SUCCESSFUL
4 actionable tasks: 3 executed, 1 up-to-date
~~~

#### 이번 변경 파일

- `src/main/java/com/cju/likelion/cardcollection/card/domain/CardLayerType.java`
- `src/main/java/com/cju/likelion/cardcollection/card/dto/CardLayerRequest.java`
- `src/main/java/com/cju/likelion/cardcollection/ai/dto/AiResourceCompositionRequest.java`
- `src/main/java/com/cju/likelion/cardcollection/ai/service/AiResourceCompositionService.java`
- `src/test/java/com/cju/likelion/cardcollection/card/CardControllerIntegrationTest.java`
- `docs/api-contract.md`

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

다음 작업자는 먼저 상품·카드 API 통합 테스트를 보강하고, 오늘 추가한 지역 기반 배치 생성 API를 실제 서버에서 검증한다. 레이어 편집에서는 템플릿 기본 `BACKGROUND` 교체와 `TEXT`·`PRODUCT` 추가를 우선 확인한다. 이후 `PRODUCT_ANGLE`을 테스트하고 `TEXT_STYLE`·`COLOR_PALETTE` 구조화 추천과 프론트엔드 카드 편집기를 연결한다. AI 편집 흐름이 안정화되면 사용자 컬렉션의 공개 범위·달성률 규칙을 확정한 뒤 V6 마이그레이션부터 컬렉션 구현을 시작한다.

## 9. 2026-08-19 작업 완료 요약

~~~text
[작업 완료] 세로형 AI 카드 이미지 규격 적용
  - OpenAI 요청 기본 크기: 1024x1536
  - 최종 저장 크기: 1000x1586
  - 중앙 크롭·리사이즈 및 안전 영역 프롬프트 적용

[작업 완료] 실제 OpenAI 이미지 생성 검증
  - BACKGROUND COMPLETED
  - BORDER COMPLETED
  - generatedImageUrl 접근 확인

[작업 완료] 구매 지역 기반 AI 프롬프트
  - purchaseStore.city 자동 반영
  - 지역 대표 요소 후보 순환
  - 같은 카드의 반복 후보 감소

[작업 완료] AI 리소스 배치 생성 API
  - 3~4개 리소스 일괄 PENDING 등록
  - 기존 단건 API와 compose API 유지
  - 통합 테스트 통과

[작업 완료] 기본 카드 레이어 교체 구조
  - 템플릿 front_image_url을 기본 BACKGROUND 레이어로 저장
  - 같은 슬롯의 완료 AI 리소스로 기본 레이어 교체
  - BACKGROUND·PRODUCT·BORDER·PATTERN·DECORATION·TEXT·FINISH 지원
  - 레이어 좌표·크기 0~1 정규화
  - composition-v2와 card_customizations.layers 저장
  - BASE_CARD 전체 교체 요청은 CARD_BASE_LAYER_DEPRECATED로 거부
  - 기존 resourceIds·layoutData 요청 형식 유지
~~~

다음 작업자는 먼저 서버를 재시작하고 `POST /api/v1/cards/{cardId}/ai-resources/batch`를 호출해 실제 지역 기반 결과를 확인한다. 레이어 compose는 템플릿 기본 배경 유지, AI 배경 교체, 텍스트 추가, 상품 기본 이미지 사용 순서로 검증한다. 그 다음 `PRODUCT_ANGLE` → 구조화된 `TEXT_STYLE` 추천 → 프론트엔드 레이어 편집기 → 실제 병렬 worker 순서로 진행한다.
