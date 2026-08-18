# API 계약

## 기본 규칙

- Base URL: `/api/v1`
- 인증: `Authorization: Bearer {accessToken}`
- 성공 응답: `{ "data": ... }`
- 오류 응답: `{ "code": "...", "message": "..." }`
- 식별자: UUID
- 날짜 및 시간: ISO-8601

## 1차 API

| Method | Endpoint | 설명 |
|---|---|---|
| POST | `/auth/signup` | 회원가입 |
| POST | `/auth/login` | 로그인 |
| POST | `/cards/registrations` | 카드 등록 |
| GET | `/cards` | 내 카드 목록 조회 |
| GET | `/cards/{cardId}` | 카드 상세 조회 |

카드 API는 JWT 인증이 필요하다.

## 상품·공식 컬렉션 조회 API

다음 조회 API는 로그인 없이 사용할 수 있다.

```text
GET /api/v1/products
GET /api/v1/products/{productId}
GET /api/v1/product-collections
GET /api/v1/product-collections/{collectionId}
GET /api/v1/product-collections/{collectionId}/products
GET /api/v1/card-templates
```

상품 목록은 다음 선택 필터와 페이지네이션을 지원한다.

```text
offeringType | category | theme | season | region | limited
page (기본 0) | size (기본 20, 최대 100)
```

응답은 `{ items, page, size, totalElements, totalPages }` 형태다. 공식 컬렉션 소속 상품 조회는 각 항목의 `required`, `displayOrder`도 반환한다.


### 카드 등록

`POST /api/v1/cards/registrations`

```json
{
  "qrToken": "MCM-DEMO-2026-001"
}
```

구매 QR이 유효하고 아직 사용되지 않은 경우 디지털 카드를 발급한다. QR 조회·카드 생성·QR 사용 처리는 하나의 트랜잭션으로 처리한다.

성공 응답의 주요 필드:

```json
{
  "data": {
    "id": "card-id",
    "originalCardType": "BASIC",
    "cardType": "BASIC",
    "status": "ACTIVE",
    "purchaseDate": "2026-08-17T10:00:00Z",
    "issuedAt": "2026-08-17T10:00:01Z",
    "product": {
      "id": "product-id",
      "name": "상품명",
      "offeringType": "PRODUCT",
      "limited": false
    },
    "store": {
      "id": "store-id",
      "name": "매장명",
      "country": "KR",
      "city": "Seoul"
    }
  }
}
```

### 카드 커스터마이징

```text
GET  /api/v1/cards/{cardId}/customizations
POST /api/v1/cards/{cardId}/customizations
POST /api/v1/cards/{cardId}/customizations/{customizationId}/select
POST /api/v1/cards/{cardId}/restore-original
```

커스터마이징 생성 요청:

```json
{
  "templateId": "template-id",
  "inputImageUrl": "/images/input.png",
  "inputText": "나의 첫 컬렉션"
}
```

현재 일반 커스터마이징 API는 템플릿 앞·뒷면 이미지를 사용하는 Mock 방식으로 즉시 `COMPLETED` 결과를 저장한다. 실제 AI 리소스 생성은 아래 AI 리소스 API와 비동기 Worker를 사용한다.

현재 선택 결과는 `cards.selected_customization_id`로 관리한다. 원본 복원 시 선택 ID를 NULL로 초기화하고 AI 생성 이력은 보존한다.

### AI 리소스 생성

AI는 카드 완성본을 직접 확정하는 역할이 아니라 사용자가 조합할 후보 리소스를 생성한다. 현재 지원하는 리소스 유형은 다음과 같다.

```text
BACKGROUND | BORDER | PATTERN | PRODUCT_ANGLE
DECORATION | COLOR_PALETTE | TEXT_STYLE | COMPOSITION
```

```text
POST /api/v1/cards/{cardId}/ai-resources
GET  /api/v1/cards/{cardId}/ai-resources
GET  /api/v1/cards/{cardId}/ai-resources/{resourceId}
POST /api/v1/cards/{cardId}/ai-resources/compose
```

생성 요청은 비동기 처리를 위해 `202 Accepted`와 함께 `PENDING` 이력을 반환한다. 백그라운드 worker가 PENDING 작업을 OpenAI Images API로 처리한 뒤 결과 이미지 URL과 상태를 갱신한다.

```json
{
  "resourceType": "PRODUCT_ANGLE",
  "templateId": "template-id",
  "prompt": "상품을 오른쪽 45도에서 본 이미지",
  "sourceImageUrl": "https://cdn.example.com/products/product.png",
  "options": {
    "angle": 45,
    "background": "transparent"
  }
}
```

`PRODUCT_ANGLE` 요청에서 `sourceImageUrl`을 생략하면 카드 상품의 `products.image_url`을 원본으로 사용한다. 상품 각도 이미지 생성에서는 원본 이미지가 외부에서 접근 가능한 HTTP(S) URL이어야 한다. 생성이 끝나면 결과 이미지 URL은 `generatedImageUrl`, 이미지 외 결과는 `generatedData`에 저장한다. `generatedData`는 색상·패턴·레이아웃 등 최종 조합에 사용할 JSON 문자열이다.

`BACKGROUND`, `BORDER`, `PATTERN`, `DECORATION`, `COLOR_PALETTE`, `TEXT_STYLE`, `COMPOSITION`은 원본 상품 이미지를 사용하지 않는 독립 리소스 생성이다. `sourceImageUrl`은 `PRODUCT_ANGLE`에서만 사용한다.

생성 상태는 `PENDING → COMPLETED | FAILED | REJECTED`로 관리하며, 기존 결과를 더 이상 후보로 노출하지 않을 때 `ARCHIVED`로 변경한다. 생성 요청은 worker가 실제 AI provider에 전달하고 결과를 갱신한다.

모든 AI 이미지 리소스는 ISO/IEC 7810 ID-1 카드 비율을 기준으로 생성한다. API 요청은 가로형 `1536x1024`를 기본으로 사용하고, 저장 직전에 중앙 크롭·리사이즈하여 최종 결과를 약 `1.586:1`(85.60×53.98mm) 비율로 맞춘다. 중요한 요소는 카드 안전 영역 안에 배치하도록 프롬프트에 포함한다.

### AI 리소스 조합 및 카드 적용

완료된 AI 리소스 여러 개를 선택해 하나의 카드 커스터마이징 이력으로 저장하고, 해당 결과를 현재 카드에 적용한다.

`POST /api/v1/cards/{cardId}/ai-resources/compose`

요청:

```json
{
  "resourceIds": ["resource-id-1", "resource-id-2"],
  "message": "나만의 카드",
  "layoutData": {
    "productX": 0.5,
    "productY": 0.55
  }
}
```

처리 규칙:

- 카드 소유자만 조합할 수 있다.
- `COMPLETED` 상태의 AI 리소스만 선택할 수 있다.
- 다른 카드의 리소스는 선택할 수 없다.
- 현재 카드가 `ACTIVE` 상태가 아니면 조합할 수 없다.
- 조합 결과는 `card_customizations.customization_data`에 저장한다.
- `cards.selected_customization_id`를 새 조합 결과로 변경한다.
- 카드 타입은 원본 타입에서 `CUSTOMIZE`로 변경한다.
- 조합과 카드 선택 상태 변경은 하나의 트랜잭션으로 처리한다.

## 인증 API 상세

### 회원가입

`POST /api/v1/auth/signup`

```json
{
  "email": "user@example.com",
  "password": "password123",
  "name": "홍길동"
}
```

성공 시 `201 Created`를 반환한다. 비밀번호는 평문으로 저장하지 않고 BCrypt로 해시한다.

### 로그인

`POST /api/v1/auth/login`

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

성공 응답:

```json
{
  "data": {
    "accessToken": "jwt-token",
    "user": {
      "id": "user-id",
      "email": "user@example.com",
      "name": "홍길동",
      "role": "CUSTOMER"
    },
    "expiresInSeconds": 86400
  }
}
```

### 내 정보 조회

`GET /api/v1/auth/me`

`Authorization: Bearer {accessToken}` 헤더가 필요하다.

### 회원 탈퇴

`DELETE /api/v1/auth/me`

탈퇴는 카드·구매 이력을 보존하기 위해 `deleted_at`을 기록하는 소프트 삭제로 처리한다. 탈퇴한 사용자의 기존 JWT는 더 이상 인증되지 않는다.

## 소셜 로그인 API

OAuth 프로필 설정이 활성화된 백엔드는 다음 주소로 소셜 로그인을 시작한다.

```text
GET /oauth2/authorization/google
GET /oauth2/authorization/kakao
```

로그인 성공 후 백엔드는 프론트엔드의 `/oauth/callback?code={one-time-code}`로 이동시킨다. 프론트엔드는 전달받은 코드를 다음 API로 한 번만 교환한다.

```http
POST /api/v1/auth/oauth/exchange
Content-Type: application/json

{
  "code": "one-time-code"
}
```

코드는 2분 동안 한 번만 사용할 수 있으며, 교환 결과는 일반 로그인과 동일한 자체 JWT이다.

## 주요 오류 코드

- `CARD_TOKEN_INVALID`: 유효하지 않은 카드 식별자
- `CARD_ALREADY_REGISTERED`: 이미 등록된 카드
- `EMAIL_ALREADY_EXISTS`: 이미 가입된 이메일
- `INVALID_CREDENTIALS`: 이메일 또는 비밀번호 불일치
- `INVALID_REQUEST`: 요청 값 검증 실패
- `OAUTH_LOGIN_FAILED`: 소셜 로그인 실패 또는 만료된 교환 코드
- `QR_TOKEN_INVALID`: 유효하지 않은 구매 QR
- `QR_ALREADY_USED`: 이미 사용된 구매 QR
- `QR_EXPIRED`: 만료된 구매 QR
- `CARD_NOT_FOUND`: 접근할 수 없는 카드 또는 존재하지 않는 카드
- `CARD_TEMPLATE_NOT_FOUND`: 발급 가능한 카드 템플릿 없음
- `CUSTOMIZATION_NOT_FOUND`: 커스터마이징 이력 없음
- `CUSTOMIZATION_NOT_COMPLETED`: 완료되지 않은 커스터마이징 선택 시도
- `AI_RESOURCE_NOT_FOUND`: AI 리소스 생성 이력 없음
- `AI_SOURCE_IMAGE_REQUIRED`: 상품 각도 이미지 생성에 원본 이미지가 없음
- `TEMPLATE_INACTIVE`: 비활성 카드 템플릿 사용 시도
- `TEMPLATE_CARD_TYPE_NOT_ALLOWED`: 카드 타입에 허용되지 않은 템플릿 사용 시도
- `PRODUCT_INACTIVE`: 비활성 상품 또는 경험의 카드 발급 시도
- `CARD_NOT_ACTIVE`: 차단 또는 폐기 상태 카드의 변경 시도
- `TEMPLATE_BRAND_MISMATCH`: 카드 상품과 다른 브랜드 템플릿 사용 시도
- `DB_CONSTRAINT_VIOLATION`: 유니크 또는 외래키 제약 위반

## 구현·운영 참고

- 상품·공식 컬렉션·템플릿 조회 API는 로그인 없이 사용할 수 있다.
- 카드·커스터마이징·AI 리소스 API는 JWT 인증이 필요하다.
- 실제 DB는 Flyway V1~V5 마이그레이션을 기준으로 한다.
- AI 생성 결과는 현재 로컬 저장소에 저장되며 운영에서는 영구 저장소로 교체해야 한다.
