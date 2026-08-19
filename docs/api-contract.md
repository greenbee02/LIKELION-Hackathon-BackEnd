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

## 사용자 개인 컬렉션 API

개인 컬렉션은 사용자의 보유 카드를 사진첩처럼 분류하는 비공개 `CUSTOM` 컬렉션이다. 모든 API는 JWT 인증이 필요하며, 소유자만 자신의 컬렉션을 조회·변경할 수 있다.

```text
POST   /api/v1/collections
GET    /api/v1/collections
GET    /api/v1/collections/{collectionId}
PATCH  /api/v1/collections/{collectionId}
DELETE /api/v1/collections/{collectionId}
POST   /api/v1/collections/{collectionId}/cards
DELETE /api/v1/collections/{collectionId}/cards/{cardId}
```

컬렉션 생성 요청:

```json
{
  "name": "서울 컬렉션",
  "description": "서울에서 만난 MCM",
  "coverImageUrl": "/images/collections/seoul.png"
}
```

카드 추가 요청:

```json
{
  "cardId": "card-id"
}
```

처리 규칙:

- 생성되는 컬렉션은 항상 `collectionType = CUSTOM`, `isPublic = false`다.
- 빈 컬렉션 생성을 허용한다.
- 같은 카드를 여러 컬렉션에 추가할 수 있다.
- 같은 카드의 동일 컬렉션 중복 추가는 허용하지 않는다.
- 카드 추가·제거 시에도 전체 내 카드 목록에서는 카드가 사라지지 않는다.
- 다른 사용자의 카드 또는 컬렉션에는 접근할 수 없다.
- AI 컬렉션 제안·저장은 후속 작업으로 분리한다.

## 리워드·이벤트 API

리워드 달성률은 공식 컬렉션의 `is_required = true` 상품을 기준으로 계산한다. 같은 상품 카드를 여러 장 보유해도 상품 종류 하나로만 인정하며, `ACTIVE` 카드만 계산에 포함한다. 모든 API는 JWT 인증이 필요하다.

```text
GET  /api/v1/rewards/progress
GET  /api/v1/rewards/my
POST /api/v1/rewards/{userRewardId}/claim
```

`GET /rewards/progress`는 공식 컬렉션별 필수 상품 수, 보유 수, 달성률과 연결된 리워드·이벤트의 해금 여부를 반환한다.

`GET /rewards/my`는 현재 사용자의 해금 리워드·이벤트 목록을 반환한다. 동일 리워드 또는 이벤트는 사용자당 한 번만 해금된다.

`POST /rewards/{userRewardId}/claim`은 `UNLOCKED` 상태의 리워드에 수령 확인용 `claimCode`를 발급한다. 현재 MVP에서는 매장 직원 검증 기능이 없으므로 상태를 `CLAIMED`로 바꾸지 않으며, 실제 수령 처리는 후속 운영자·매장 기능에서 확정한다.


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
BACKGROUND | BORDER | PATTERN | DECORATION
COLOR_PALETTE | TEXT_STYLE | COMPOSITION
```

```text
POST /api/v1/cards/{cardId}/ai-resources
POST /api/v1/cards/{cardId}/ai-resources/batch
GET  /api/v1/cards/{cardId}/ai-resources
GET  /api/v1/cards/{cardId}/ai-resources/{resourceId}
POST /api/v1/cards/{cardId}/ai-resources/compose
```

생성 요청은 비동기 처리를 위해 `202 Accepted`와 함께 후보 그룹을 반환한다. 리소스 종류 하나당 기본 4개 후보가 생성되며, `candidateCount`로 3개 또는 4개를 선택할 수 있다. 후보 하나는 독립적인 `PENDING` 이력으로 저장되고, 같은 리소스의 후보들은 `candidateGroupId`로 묶인다.

배치 API는 이제 리소스 종류를 1~8개까지 받을 수 있다. 즉 배치의 3~4개 제한은 제거되었고, 각 리소스 종류마다 3~4개 후보가 생성된다. 사용자는 그룹별 후보 중 하나를 선택해 카드에 조합한다.

`POST /api/v1/cards/{cardId}/ai-resources/batch`

```json
{
  "resources": [
    {
      "resourceType": "BACKGROUND",
      "candidateCount": 4,
      "prompt": "고급스럽고 차분한 서울 지역 배경",
      "options": { "style": "luxury", "color": "black" }
    },
    {
      "resourceType": "BORDER",
      "prompt": "서울의 건축 디테일에서 영감을 받은 테두리",
      "options": { "color": "gold" }
    },
    {
      "resourceType": "PATTERN",
      "prompt": "상품 카드에 사용할 절제된 지역 패턴",
      "options": { "density": "light" }
    }
  ]
}
```

각 `resources` 항목의 `candidateCount`는 3 또는 4만 허용한다. 생략하면 4개로 처리한다. 구매 카드의 `purchase_store.city`를 기준으로 지역 문맥을 자동 추가하며, 후보 인덱스에 따라 같은 리소스 그룹 안에서도 서로 다른 지역 변형을 사용하도록 한다. 지역이 등록되지 않은 경우에는 매장 도시의 지역 건축·풍경을 일반 문맥으로 사용한다.

응답은 다음처럼 리소스 종류별 후보 그룹으로 반환한다.

```json
{
  "cardId": "card-id",
  "groups": [
    {
      "candidateGroupId": "group-id",
      "resourceType": "BACKGROUND",
      "candidateCount": 4,
      "candidates": [
        { "id": "candidate-1", "candidateIndex": 1, "status": "PENDING" },
        { "id": "candidate-2", "candidateIndex": 2, "status": "PENDING" },
        { "id": "candidate-3", "candidateIndex": 3, "status": "PENDING" },
        { "id": "candidate-4", "candidateIndex": 4, "status": "PENDING" }
      ]
    }
  ]
}
```

상품 이미지는 AI 리소스로 생성하지 않는다. `PRODUCT` 레이어에서 `resourceId`를 생략하면 카드 상품의 `products.image_url`을 그대로 사용한다. 상품 원본 이미지를 AI 생성 요청에 전달하는 `sourceImageUrl` 필드도 신규 요청 계약에서 제외한다.

`BACKGROUND`, `BORDER`, `PATTERN`, `DECORATION`, `COLOR_PALETTE`, `TEXT_STYLE`, `COMPOSITION`은 상품 원본 이미지와 독립적으로 생성한다. 생성이 끝나면 결과 이미지 URL은 `generatedImageUrl`, 이미지 외 결과는 `generatedData`에 저장한다. `generatedData`는 색상·패턴·레이아웃 등 최종 조합에 사용할 JSON 문자열이다.

`COLOR_PALETTE`는 이미지 생성이 아니라 OpenAI Structured Outputs를 이용한 색상 추천 JSON으로 생성된다. 완료 시 `generatedImageUrl`은 `null`이고 `generatedData`에는 다음 필드가 저장된다: `paletteName`, `primary`, `secondary`, `accent`, `background`, `text`, `rationale`. 색상 값은 `#RRGGBB` 형식이다.

`TEXT_STYLE`도 이미지 생성 없이 폰트·크기·굵기·자간·줄 간격·색상·정렬·최대 줄 수·정규화 좌표를 담은 JSON으로 생성된다. `COMPOSITION`은 이미지 대신 `1000x1586` 카드 캔버스, 배경색, 레이어별 유형·좌표·크기·회전·투명도·순서를 담은 JSON으로 생성된다. 두 유형 모두 완료 시 `generatedImageUrl`은 `null`이고 결과는 `generatedData`에 저장된다.

생성 상태는 후보별로 `PENDING → COMPLETED | FAILED | REJECTED`로 관리하며, 기존 결과를 더 이상 후보로 노출하지 않을 때 `ARCHIVED`로 변경한다. 생성 요청은 worker가 후보별로 실제 AI provider에 전달하고 결과를 갱신한다.

모든 AI 이미지 리소스는 ISO/IEC 7810 ID-1 비율을 세로 방향으로 적용한다. API 요청은 세로형 `1024x1536`을 기본으로 사용하고, 저장 직전에 중앙 크롭·리사이즈하여 최종 결과를 `1000x1586`으로 맞춘다. 비율은 가로:세로 약 `1:1.586`이며, 중요한 요소는 카드 안전 영역 안에 배치하도록 프롬프트에 포함한다.

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
  },
  "layers": [
    {
      "id": "background-01",
      "type": "BACKGROUND",
      "resourceId": "resource-id-1",
      "x": 0,
      "y": 0,
      "width": 1,
      "height": 1,
      "opacity": 0.8,
      "zIndex": 1
    },
    {
      "id": "message-01",
      "type": "TEXT",
      "text": "나만의 카드",
      "x": 0.5,
      "y": 0.82,
      "width": 0.7,
      "height": 0.08,
      "styleData": {
        "fontCategory": "SERIF",
        "fontSize": 42,
        "color": "#D4AF37",
        "align": "CENTER"
      },
      "zIndex": 5
    }
  ]
}
```

`layers`의 좌표와 크기는 카드 전체를 `0~1`로 정규화한 값이다. 템플릿 앞면은 `BACKGROUND` 슬롯의 기본 레이어로 자동 포함되며 `sourceType`은 `TEMPLATE`, `isDefault`는 `true`, `replaceable`은 `true`로 저장한다. 사용자가 같은 `BACKGROUND` 슬롯의 AI 리소스를 선택하면 템플릿 기본값을 교체한다. `resourceId`를 사용하는 레이어는 해당 카드에 연결된 `COMPLETED` AI 리소스만 참조할 수 있으며, `TEXT` 레이어는 AI 리소스 없이 문구와 `styleData`를 사용한다. `PRODUCT` 레이어는 `resourceId`가 없으면 카드 상품의 기본 이미지를 사용한다.

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
- `AI_RESOURCE_TYPE_UNSUPPORTED`: 지원하지 않는 AI 리소스 유형 요청
- `TEMPLATE_INACTIVE`: 비활성 카드 템플릿 사용 시도
- `TEMPLATE_CARD_TYPE_NOT_ALLOWED`: 카드 타입에 허용되지 않은 템플릿 사용 시도
- `PRODUCT_INACTIVE`: 비활성 상품 또는 경험의 카드 발급 시도
- `CARD_NOT_ACTIVE`: 차단 또는 폐기 상태 카드의 변경 시도
- `TEMPLATE_BRAND_MISMATCH`: 카드 상품과 다른 브랜드 템플릿 사용 시도
- `DB_CONSTRAINT_VIOLATION`: 유니크 또는 외래키 제약 위반
- `COLLECTION_NOT_FOUND`: 접근할 수 없는 개인 컬렉션 또는 존재하지 않는 컬렉션
- `COLLECTION_CARD_NOT_OWNED`: 본인 소유가 아닌 카드 추가 시도
- `COLLECTION_CARD_ALREADY_ADDED`: 동일 컬렉션에 카드 중복 추가 시도

## 구현·운영 참고

- 상품·공식 컬렉션·템플릿 조회 API는 로그인 없이 사용할 수 있다.
- 카드·커스터마이징·AI 리소스 API는 JWT 인증이 필요하다.
- 실제 DB는 PostgreSQL Flyway V1~V9 마이그레이션을 기준으로 한다. `DataBase/Schema.sql`은 MySQL 참고용이다.
- AI 생성 결과는 현재 `build/generated-ai-resources` 로컬 저장소에 저장된다. 영구 저장소 전환은 필요하지만 현재 MVP 범위 밖이다.
