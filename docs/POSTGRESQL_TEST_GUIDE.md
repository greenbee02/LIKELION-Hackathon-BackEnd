# PostgreSQL 테스트 실행 안내

V6~V9 Flyway 파일에는 PostgreSQL 전용 문법이 포함되어 있다. 따라서 DB·Flyway·통합 테스트의 공식 기준은 H2가 아닌 PostgreSQL이다. 기존 H2 local 프로필은 호환 목적으로만 유지하며 V1~V9 전체 마이그레이션 검증에는 사용하지 않는다.

## 1. 사전 조건

- Docker Desktop 실행
- PostgreSQL 컨테이너 실행
- 테스트용 데이터베이스 `cardcollection_test` 생성

PostgreSQL 컨테이너 확인:

```powershell
docker ps
```

컨테이너가 정지되어 있다면 실행한다

```powershell
docker start cardcollection-postgres
```

테스트 DB가 없다면 DBeaver에서 다음 SQL을 실행한다.

```sql
CREATE DATABASE cardcollection_test
WITH OWNER = cardcollection;
```

## 2. 테스트 환경변수 설정

백엔드 프로젝트 최상단에서 PowerShell을 열고 다음 환경변수를 설정한다.

```powershell
$env:TEST_DB_URL = "jdbc:postgresql://localhost:54329/cardcollection_test"
$env:TEST_DB_USERNAME = "cardcollection"
$env:TEST_DB_PASSWORD = "팀에서_정한_테스트_DB_비밀번호"
```

환경변수는 현재 PowerShell 창에서만 유지된다. 실제 비밀번호는 Git이나 설정 파일에 직접 작성하지 않는다.

## 3. 전체 테스트 실행

```powershell
.\gradlew.bat test --no-daemon
```

정상 결과:

```text
BUILD SUCCESSFUL
```

테스트는 `test` 프로필로 고정되며 PostgreSQL 드라이버와 `cardcollection_test` 데이터베이스만 사용한다. 테스트 과정에서 Flyway V1~V9이 적용되고 API 통합 테스트용 데이터가 생성된다. 실제 개발 DB인 `cardcollection`에는 영향을 주지 않는다.

## 4. 개발 서버 실행으로 복귀

테스트 완료 후 실제 개발 DB 주소로 되돌림.

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:54329/cardcollection"
$env:DB_USERNAME = "cardcollection"
$env:DB_PASSWORD = "팀에서_정한_DB_비밀번호"
$env:SPRING_PROFILES_ACTIVE = "prod"
```

백엔드 실행:

```powershell
.\gradlew.bat bootRun --no-daemon
```

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

## 주의사항

- `cardcollection`: 실제 로컬 개발 및 시연 DB
- `cardcollection_test`: PostgreSQL 통합 테스트 전용 DB
- 테스트를 실제 `cardcollection`에 연결하면 테스트 데이터가 추가될 수 있으므로 반드시 테스트 DB를 사용합니다.
- 테스트는 H2를 사용하지 않는다. H2는 `local` 프로필로 애플리케이션을 빠르게 실행할 때만 사용한다.
- H2는 V7의 `jsonb_build_object()`와 V8 이후 PostgreSQL 마이그레이션 문법을 처리하지 못하므로, V1~V9 전체 Flyway 검증에는 사용하지 않는다.
