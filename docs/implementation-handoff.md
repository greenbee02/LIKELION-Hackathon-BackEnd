# 럭셔리 디지털 카드 컬렉션

## 1. 프로젝트 개요

럭셔리 제품 구매 경험을 디지털 카드로 기록·수집하고, 카드 커스텀·컬렉션·리워드·케어 서비스로 연결하는 Spring Boot 백엔드 프로젝트다.

현재 저장소에는 MVP 인증 기반이 구현되어 있다. 다음 개발자는 이 인증 구조 위에 상품·카드·컬렉션·리워드 기능을 확장하면 된다.

## 2. 기술 스택

- Java 17 기준
- 실제 검증 환경: Java 19.0.1
- Spring Boot 3.4.5
- Spring Web
- Spring Data JPA
- Spring Security
- Spring OAuth2 Client
- JWT: JJWT 0.12.6
- Flyway 10.20.1
- 로컬 DB: H2 인메모리
- 운영 DB: PostgreSQL
- 빌드: Gradle Wrapper

## 3. 현재 구현 완료 범위

### 일반 인증

- 회원가입
- BCrypt 비밀번호 암호화
- 로그인 및 자체 JWT Access Token 발급
- JWT 인증 필터
- 인증 사용자 조회
- 이메일 중복 검사
- 잘못된 인증 정보 처리

### 소셜 인증 기반

- Google OAuth2 설정
- Kakao OAuth2 설정
- 소셜 사용자 정보 매핑
- 기존 소셜 계정 조회
- 인증된 이메일 기준 기존 계정 연결
- 신규 소셜 사용자 생성
- 소셜 로그인 후 자체 JWT 발급
- 일회성 OAuth 교환 코드 발급 및 교환

### 계정 관리

- 회원 탈퇴
- 카드 및 구매 이력 보존을 위한 소프트 삭제
- 탈퇴한 사용자의 기존 JWT 인증 차단

## 4. 구현하지 않은 기능

- 상품 Entity 및 API
- 디지털 카드 Entity 및 API
- QR/NFC 카드 등록
- 카드 상세 조회
- 카드 커스텀
- 컬렉션 관리
- 리워드 및 매장 수령
- AI 추천
- 운영자·매장 직원 기능

## 5. 디렉터리 구조

```text
src/main/java/com/cju/likelion/cardcollection/
├─ auth/
│  ├─ controller/       인증 API
│  ├─ domain/           User, SocialAccount, Role, Provider
│  ├─ dto/              인증 요청·응답 DTO
│  ├─ exception/        인증 예외
│  ├─ repository/       UserRepository, SocialAccountRepository
│  ├─ security/         JWT 및 OAuth2 핸들러
│  └─ service/          일반·소셜 인증 서비스
├─ common/
│  ├─ api/              공통 응답 형식
│  ├─ config/            Security 설정
│  └─ exception/        전역 예외 처리
└─ CardCollectionApplication.java

src/main/resources/
├─ application.yml
├─ application-local.yml
├─ application-oauth.yml
├─ application-prod.yml
└─ db/migration/
   ├─ V1__init.sql
   ├─ V2__add_social_accounts.sql
   └─ V3__add_user_withdrawal.sql
```

## 6. 주요 API

### 회원가입

```http
POST /api/v1/auth/signup
Content-Type: application/json
```

```json
{
  "email": "user@example.com",
  "password": "password123",
  "name": "홍길동"
}
```

성공 상태 코드는 `201 Created`다.

### 로그인

```http
POST /api/v1/auth/login
Content-Type: application/json
```

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

응답의 `data.accessToken`을 이후 API 요청에 사용한다.

```http
Authorization: Bearer {accessToken}
```

### 내 정보 조회

```http
GET /api/v1/auth/me
Authorization: Bearer {accessToken}
```

### 회원 탈퇴

```http
DELETE /api/v1/auth/me
Authorization: Bearer {accessToken}
```

실제 삭제가 아니라 `users.deleted_at`을 기록한다. 탈퇴 후 기존 JWT는 `401 Unauthorized`가 된다.

### OAuth 로그인 시작

OAuth 프로필을 활성화한 경우 다음 주소로 이동한다.

```text
GET /oauth2/authorization/google
GET /oauth2/authorization/kakao
```

### OAuth 코드 교환

소셜 로그인 성공 후 백엔드는 프론트엔드의 다음 주소로 리다이렉트한다.

```text
/oauth/callback?code={one-time-code}
```

프론트엔드는 코드를 한 번 교환한다.

```http
POST /api/v1/auth/oauth/exchange
Content-Type: application/json
```

```json
{
  "code": "one-time-code"
}
```

교환 결과는 일반 로그인과 동일한 자체 JWT다. 코드는 2분 동안 한 번만 유효하다.

## 7. 데이터베이스 구조

현재 마이그레이션에는 다음 테이블이 있다.

```text
users
├─ id
├─ email
├─ password_hash       소셜 전용 계정은 NULL 가능
├─ name
├─ role                CUSTOMER, STAFF, ADMIN
├─ created_at
├─ updated_at
└─ deleted_at          소프트 삭제 시각

social_accounts
├─ id
├─ user_id
├─ provider             GOOGLE, KAKAO
├─ provider_user_id
├─ created_at
└─ updated_at
```

`social_accounts(provider, provider_user_id)`에는 유니크 제약이 있다.

기존 ERD 초안은 [erd.md](./erd.md)에서 확인할 수 있다.

## 8. 로컬 실행

### 기본 실행

```powershell
.\gradlew.bat bootRun --no-daemon
```

기본적으로 `local` 프로필과 H2 인메모리 DB를 사용한다.

### 테스트 실행

```powershell
.\gradlew.bat test --no-daemon
```

현재 자동 테스트는 다음을 검증한다.

- 회원가입
- 로그인
- JWT로 `/me` 조회
- 중복 이메일 차단
- 잘못된 비밀번호 차단
- 탈퇴 후 기존 JWT 차단

## 9. Google·Kakao OAuth 실행

다음 환경 변수가 필요하다.

```text
GOOGLE_CLIENT_ID
GOOGLE_CLIENT_SECRET
KAKAO_CLIENT_ID
KAKAO_CLIENT_SECRET
FRONTEND_URL
```

Spring Boot 실행:

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=local,oauth" --no-daemon
```

Provider 콘솔에 등록할 백엔드 Redirect URI:

```text
http://localhost:8080/login/oauth2/code/google
http://localhost:8080/login/oauth2/code/kakao
```

프론트엔드 개발 서버는 `FRONTEND_URL`에 설정한다. 기본값은 `http://localhost:3000`이다.

실제 provider 자격증명이 없을 때도 더미 값으로 애플리케이션 기동과 OAuth 시작 리다이렉트까지 확인할 수 있다. 실제 계정 인증과 콜백 완료는 유효한 Client ID·Secret이 필요하다.

## 10. 테스트 확인 결과

최종 확인 결과:

```text
Gradle 자동 테스트       BUILD SUCCESSFUL
회원가입                 201
로그인                   200
JWT /me 조회             200
중복 이메일              409
잘못된 JWT               401
회원 탈퇴                204
탈퇴 후 JWT 사용         401
Google OAuth 시작        302
Kakao OAuth 시작         302
```

H2 2.3과 Flyway가 권장하는 H2 버전 사이의 지원 경고가 출력되지만, 현재 테스트와 애플리케이션 기동에는 영향을 주지 않는다.

## 11. 다음 개발 순서

1. `Product` Entity 및 Repository 구현
2. `Card` Entity 및 Repository 구현
3. 상품·카드 샘플 데이터와 Flyway 마이그레이션 추가
4. 카드 등록 API 구현
5. 카드 목록·상세 API 구현
6. 중복 카드 등록 및 동시 등록 예외 처리
7. 카드 커스텀 기능 구현
8. 컬렉션 기능 구현
9. 리워드 및 매장 수령 기능 구현

각 기능은 Java 클래스부터 만들지 않고, 아래 선행조건을 충족한 뒤 구현을 시작한다.

### 11.1 상품 Entity 및 Repository

구현 전에 결정할 것:

- 상품의 필수 필드: 상품명, 제품군, 시즌, 지역, 소재, 색상
- 보증 정보와 케어 안내의 저장 형식
- 상품 삭제 정책: 실제 삭제 또는 비활성 상태 유지
- 브랜드와 상품의 관계

필수 준비물:

- `products` 테이블 ERD
- 상품 컬럼의 필수값·최대 길이
- 상품 등록·조회 API 계약
- 테스트용 상품 1개 이상

### 11.2 카드 Entity 및 Repository

구현 전에 결정할 것:

- 카드가 상품과 1:1인지, 상품 하나에 여러 카드가 발급될 수 있는지
- QR/NFC 식별자 저장 및 해시 여부
- 카드 상태값: `UNREGISTERED`, `REGISTERED`, `BLOCKED` 등
- 구매일·구매 매장·소유자 변경 가능 여부
- 카드 등록 동시 요청 처리 방식

필수 준비물:

- `cards` 테이블 ERD
- `products`, `users`, `cards` 간 FK 관계
- `card_token` 유니크 제약
- 소유자 최초 등록 규칙
- 유효·무효·중복 카드 테스트 데이터

### 11.3 카드 등록 API

구현 전에 결정할 것:

- QR/NFC에서 백엔드로 전달되는 값의 형식
- 비로그인 사용자의 처리 방식
- 이미 등록된 카드의 응답 상태 코드와 메시지
- 잘못된 식별자와 차단 카드의 구분
- 등록 성공 후 반환할 카드 정보

필수 준비물:

- 카드 등록 API 요청·응답 명세
- 인증 필요 여부
- `201`, `404`, `409` 등 오류 코드
- 카드 등록 성공·중복·식별 실패 통합 테스트 시나리오

### 11.4 카드 목록·상세 API

구현 전에 결정할 것:

- 사용자가 볼 수 있는 제품·구매 정보 범위
- 보증 정보와 케어 안내의 공개 범위
- 카드 정렬 및 필터 조건
- 다른 사용자의 카드 접근 차단 방식

필수 준비물:

- 카드 조회 API 계약
- 사용자와 카드 소유권 검사 규칙
- 목록 응답 필드와 페이지네이션 여부
- 소유 카드·타인 카드·존재하지 않는 카드 테스트 데이터

### 11.5 카드 커스텀

구현 전에 결정할 것:

- 템플릿 Entity와 카드 커스텀의 관계
- 이미지 저장 위치와 URL 발급 방식
- 문구 최대 길이
- 카드 수정 가능 횟수 또는 제한
- 민감정보·부적절한 콘텐츠 검수 방식

필수 준비물:

- `templates`, `card_customizations` ERD
- 이미지 업로드 방식 또는 MVP용 `imageUrl` 계약
- 템플릿 활성·비활성 규칙
- 카드 소유자만 수정할 수 있다는 권한 규칙
- 저장·수정·잘못된 카드 접근 테스트

### 11.6 컬렉션

구현 전에 결정할 것:

- 컬렉션과 카드의 다대다 관계
- 컬렉션 이름·설명·대표 이미지 필드
- 컬렉션에서 카드 제거 시 카드 자체의 보유 상태 유지 여부
- 완성도 계산 방식
- 컬렉션 공개·비공개 여부

필수 준비물:

- `collections`, `collection_cards` ERD
- 컬렉션 생성·수정·카드 추가·삭제 API 계약
- 카드 소유권 검사 규칙
- 빈 컬렉션·중복 카드·존재하지 않는 카드 테스트

### 11.7 리워드 및 매장 수령

구현 전에 결정할 것:

- 리워드 자격 조건과 계산 방식
- 리워드 상태값과 만료 정책
- 수령 코드 형식과 유효 시간
- 중복 수령 방지 방식
- 고객·매장 직원·운영자별 권한

필수 준비물:

- `rewards`, `reward_claims` ERD
- 리워드 조건과 상태 전이표
- 고객용·직원용 API 계약
- 리워드 수령 이력 보존 정책
- 정상 수령·만료·중복 수령·권한 없음 테스트

### 11.8 AI 추천 및 외부 연동

구현 전에 결정할 것:

- 실제 AI 사용 여부와 MVP Mock 응답 범위
- 외부 API 실패 시 대체 동작
- 추천 결과 저장 여부
- 개인정보를 외부 서비스로 전송할 수 있는 범위
- API Key와 Secret의 환경 변수 관리 방식

필수 준비물:

- 외부 API 요청·응답 명세
- 타임아웃·재시도·실패 응답 규칙
- Mock 데이터 또는 테스트용 Stub
- 민감정보 제외 필드 목록

### 11.9 운영자·매장 직원 기능

구현 전에 결정할 것:

- `STAFF`, `ADMIN` 권한 범위
- 매장 직원이 접근할 수 있는 매장 범위
- 템플릿·상품·리워드 수정 권한
- 운영 데이터의 브랜드별 분리 여부
- 감사 로그 필요 여부

필수 준비물:

- 역할×기능 권한표
- 운영자·직원 API 계약
- 관리 대상 Entity와 상태값
- 권한 없음·다른 매장 접근·비활성 데이터 테스트

새로운 기능을 추가할 때는 다음 순서를 유지한다.

```text
ERD 수정
→ Flyway 마이그레이션
→ Entity
→ Repository
→ Service
→ Controller/API
→ 통합 테스트
```

## 13. 기능 구현 착수 공통 조건

다음 조건을 모두 충족한 기능만 구현을 시작한다.

### 데이터 조건

- ERD에 테이블과 관계가 정의되어 있다.
- PK, FK, 유니크 제약, 필수값이 정해져 있다.
- 상태값과 상태 전이가 정해져 있다.
- 데이터 삭제·보존 정책이 정해져 있다.

### API 조건

- Endpoint와 HTTP Method가 정해져 있다.
- 요청·응답 JSON 필드가 정해져 있다.
- 성공·실패 HTTP 상태 코드가 정해져 있다.
- 인증 및 역할별 접근 권한이 정해져 있다.

### 화면·사용자 흐름 조건

- 유저플로우에서 진입점과 다음 화면이 정의되어 있다.
- 빈 상태·로딩·실패 상태가 정의되어 있다.
- 사용자가 성공 여부를 확인할 수 있는 결과가 정의되어 있다.

### 테스트 조건

- 정상 시나리오가 최소 1개 있다.
- 잘못된 입력 또는 권한 오류 시나리오가 있다.
- 중복 제출·반복 요청·존재하지 않는 데이터 시나리오가 있다.
- 테스트용 초기 데이터가 준비되어 있다.

### 완료 조건

- Flyway 마이그레이션이 적용된다.
- Entity와 Repository가 동작한다.
- Service의 핵심 규칙이 구현된다.
- Controller API가 명세와 일치한다.
- 자동 테스트가 통과한다.
- 프론트엔드가 사용할 요청·응답 예시가 문서화되어 있다.

ERD가 필요한 정도는 기능마다 다르지만, **새로운 데이터를 저장하거나 관계를 추가하는 기능은 반드시 ERD와 마이그레이션을 먼저 확정**해야 한다. 단순 조회나 기존 데이터의 상태 변경 기능도 API 계약·권한·상태 전이는 먼저 확정해야 한다.

## 14. 주의사항

- JWT Secret은 운영 환경에서 반드시 `JWT_SECRET` 환경 변수로 교체한다.
- OAuth Client Secret을 Git에 커밋하지 않는다.
- OAuth 교환 코드는 현재 단일 서버용 메모리 저장소를 사용한다. 다중 서버 운영 시 Redis 또는 영속 저장소로 교체해야 한다.
- 소셜 계정은 provider의 인증된 이메일을 기준으로 기존 계정에 연결한다.
- 카드와 구매 이력을 보존해야 하므로 사용자 탈퇴 시 실제 User row를 삭제하지 않는다.
