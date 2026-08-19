# 사용자 컬렉션 V6 요구사항 초안

## 기준

- DBMS: PostgreSQL
- 마이그레이션: Flyway
- 기능명세서: `럭셔리 디지털 카드 컬렉션_기능명세서_2026-08-18.md` 3.1, 3.2
- 참조 스키마: `DataBase/Schema.sql`

## 확정된 MVP 정책

- 개인 컬렉션은 기본 비공개이며, 소유자만 조회·생성·수정·삭제할 수 있다.
- 다른 고객의 카드 또는 컬렉션 공개 탐색은 MVP 범위에 포함하지 않는다.
- 고객은 자신의 보유 카드만 개인 컬렉션에 추가할 수 있다.
- 고객은 자신이 만든 컬렉션만 수정하거나 삭제할 수 있다.
- 하나의 카드는 여러 개인 컬렉션에 포함될 수 있다.
- 같은 카드의 동일 컬렉션 중복 추가는 허용하지 않는다.
- 카드가 없는 빈 컬렉션은 허용하고, 화면에서 별도 상태로 표시한다.
- 개인 컬렉션은 브랜드 공식 컬렉션을 수정하지 않는다.

## V6에 필요한 테이블

### collections

- `id` (UUID PK)
- `user_id` (FK → users.id)
- `name` (NOT NULL)
- `description` (NULL)
- `cover_image_url` (NULL)
- `collection_type` (`CUSTOM` | `AI`, 기본 `CUSTOM`)
- `generation_reason` (AI 생성 사유, NULL)
- `is_public` (향후 확장용으로 유지하되 MVP에서는 `FALSE`만 저장)
- `created_at`, `updated_at`

### collection_cards

- `collection_id` (FK → collections.id)
- `card_id` (FK → cards.id)
- `added_at`
- PK: `(collection_id, card_id)`

## 제약조건

- 컬렉션 삭제 시 `collection_cards`도 함께 삭제한다.
- 카드 삭제 시 연결된 `collection_cards`를 삭제한다.
- API 서비스에서 카드 소유권을 검증한다. FK만으로는 "내 카드만 추가"를 보장할 수 없다.
- API에서는 `is_public`을 변경하거나 공개 컬렉션을 조회하는 기능을 제공하지 않는다.
- V6에서는 `CUSTOM` 수동 컬렉션 생성·관리만 구현한다.
- AI 컬렉션 제안과 사용자의 저장 확정 기능은 후속 작업으로 분리한다.

## DB 팀 요청 사항

1. 위 구조와 V7 시드가 참조하는 리워드·이벤트 관련 테이블을 PostgreSQL Flyway `V6` 스키마 마이그레이션에 추가한다.
2. 테스트 사용자, 보유 카드, 빈 컬렉션, 카드가 포함된 컬렉션 예시는 V7 데모 시드에 추가한다.
3. 기존 V1~V5 마이그레이션 파일은 수정하지 않는다.

## 후속 작업

- AI가 보유 카드 패턴을 분석해 컬렉션 후보(이름, 포함 카드, 추천 이유)를 제안하는 기능
- 사용자가 제안을 저장하면 `collection_type = AI` 컬렉션을 생성하는 기능

## 다음 결정 필요

- 컬렉션 이름 최대 길이와 설명·대표 이미지 입력 제한
