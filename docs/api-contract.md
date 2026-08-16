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
