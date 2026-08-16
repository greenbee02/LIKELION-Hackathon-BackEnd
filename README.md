# Luxury Digital Card Collection

럭셔리 제품 구매 경험을 디지털 카드로 기록·수집하고 리워드와 케어 서비스로 연결하는 Spring Boot 백엔드 프로젝트다.

## 현재 MVP

- 회원가입 및 로그인 기반
- QR/NFC 카드 식별자를 통한 카드 등록
- 카드 최초 등록 계정 귀속 및 중복 등록 차단
- 카드 목록 및 상세 조회

## 실행 환경

- Java 17 이상
- Spring Boot 3.4.x
- Gradle
- 로컬: H2 인메모리 데이터베이스
- 운영: PostgreSQL

프로젝트를 IntelliJ IDEA에서 Gradle 프로젝트로 불러온 뒤 `CardCollectionApplication`을 실행하면 기본적으로 `local` 프로필이 사용된다.

테스트 실행:

```powershell
.\gradlew.bat test --no-daemon
```

실행 후 확인할 주소:

- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 Console: http://localhost:8080/h2-console

## 인증 API 예시

회원가입:

```http
POST /api/v1/auth/signup
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123",
  "name": "홍길동"
}
```

로그인:

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

로그인 응답의 `accessToken`을 다음 요청에 사용한다.

```http
Authorization: Bearer {accessToken}
```

## Google·Kakao 로그인

소셜 로그인은 `oauth` 프로필을 활성화했을 때 사용할 수 있다.

필요한 환경 변수:

```text
GOOGLE_CLIENT_ID
GOOGLE_CLIENT_SECRET
KAKAO_CLIENT_ID
KAKAO_CLIENT_SECRET
FRONTEND_URL
```

프론트엔드는 다음 주소로 로그인 플로우를 시작한다.

```text
http://localhost:8080/oauth2/authorization/google
http://localhost:8080/oauth2/authorization/kakao
```

OAuth 인증이 끝나면 백엔드는 프론트엔드의 `/oauth/callback?code=...`로 이동시킨다. 프론트엔드는 해당 코드를 `POST /api/v1/auth/oauth/exchange`로 교환해 자체 JWT를 받는다. 교환 코드는 2분 동안 한 번만 유효하다.

동일한 이메일의 기존 계정이 있으면 이메일 인증이 확인된 소셜 계정을 기존 사용자에 연결한다. 회원 탈퇴는 `DELETE /api/v1/auth/me`로 처리하며 카드 이력을 보존하기 위해 소프트 삭제한다.

로컬에서 OAuth 프로필을 함께 실행할 때는 다음처럼 `local,oauth` 프로필을 사용한다.

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=local,oauth" --no-daemon
```

## 문서

- `docs/mvp-scope.md`: MVP 범위
- `docs/api-contract.md`: API 계약
- `docs/erd.md`: ERD 초안

## 다음 개발 순서

1. 사용자·상품·카드 Entity 및 Repository 구현
2. 카드 등록·목록·상세 API 구현
3. 중복 카드 등록 예외 테스트
