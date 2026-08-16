# ERD 초안

## 1차 테이블

- `users`
- `products`
- `cards`
- `card_customizations`
- `social_accounts`

## 관계

```text
users 1 ─── N cards
products 1 ─── N cards
cards 1 ─── 1 card_customizations
users 1 ─── N social_accounts
```

## 설계 메모

- `cards.card_token`은 유일해야 한다.
- `cards.owner_id`가 이미 존재하면 다른 계정의 등록을 차단한다.
- 카드 등록은 동시 요청에서도 한 계정만 성공하도록 처리한다.
- `social_accounts(provider, provider_user_id)`는 유일해야 한다.
- 소셜 로그인 회원은 `password_hash`가 없을 수 있다.
