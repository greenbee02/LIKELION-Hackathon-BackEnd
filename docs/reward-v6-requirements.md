# 리워드·이벤트 요구사항 초안

## 기준

- DBMS: PostgreSQL
- 마이그레이션: Flyway
- 적용 파일: V6 스키마 마이그레이션, V7 데모 시드 마이그레이션
- 리워드 조건 기준: 공식 컬렉션(`product_collections`)
- 참조 스키마: `DataBase/Schema.sql`

## 확정된 정책

### 공식 컬렉션 달성률

- 리워드와 이벤트는 개인 `CUSTOM` 컬렉션이 아니라 공식 컬렉션 달성률로 해금한다.
- 달성률 분모는 해당 공식 컬렉션에 연결된 `is_required = TRUE` 상품이다.
- `is_required = FALSE` 상품은 공식 컬렉션에 표시할 수 있지만, 리워드 달성률에는 반영하지 않는다.
- 같은 상품의 카드를 여러 장 보유해도 `product_id` 기준으로 한 번만 인정한다.
- 개인 `CUSTOM` 컬렉션은 리워드 조건에 직접 사용하지 않는다.
- `collection_rewards.required_percentage` 이상이면 연결된 리워드 또는 이벤트를 해금한다.

## 현재 시드 조건 예시

| 공식 컬렉션 | 달성률 조건 | 해금 대상 |
| --- | ---: | --- |
| Seoul Exclusive | 66.67% | 이벤트 |
| Seoul Exclusive | 100% | Seoul Collector Pass |
| 2026 New Arrivals | 100% | AW26 Limited Card Holder |
| Women's Signature | 60% | 이벤트 |
| Global Travel Collection | 100% | 이벤트 |
| MCM Icons | 100% | Premium Care |

## DB 팀 파일 구성

- V6 스키마: `collections`, `collection_cards`, `rewards`, `events`, `collection_rewards`, `user_rewards`
- V7 시드: 사용자·브랜드·매장·상품·공식 컬렉션·연결 항목·템플릿·QR·리워드·이벤트·조건 데이터
- 카드·개인 컬렉션·사용자 리워드 데이터는 API 흐름으로 생성하거나 시연용 시드 여부를 별도로 결정한다.

## 이후 구현 순서

1. 개인 컬렉션 CRUD와 카드 추가·제거 API
2. 공식 컬렉션 달성률 계산 API
3. 조건 충족 리워드·이벤트 해금과 `user_rewards` 생성
4. 리워드 수령 처리 및 통합 테스트
