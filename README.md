# subscription-billing

구독 결제 백엔드입니다. 이전 회사(TomYo EdTech)에서 Node.js로 통신사 결제 연동 구독 시스템을 만들었는데, 같은 문제를 Spring Boot로 다시 풀어보면서 그때 부족했던 부분(중복 결제 방지, 동시성, 갱신 실패 처리)을 제대로 다뤄보려고 시작했습니다.

A subscription billing backend in Spring Boot. I built a carrier-billing subscription system in Node.js at my last job, and this is the same problem done again with the parts I wanted to get right: duplicate payment protection, concurrency, and failed renewals. (English below.)

## 기술 스택

Java 21 · Spring Boot 4.1 · Spring Data JPA · PostgreSQL 17 · Flyway · Testcontainers · GitHub Actions

## 실행 방법

```bash
docker compose up -d     # PostgreSQL
./gradlew bootRun        # http://localhost:8080
```

테스트는 Testcontainers로 실제 PostgreSQL을 띄워서 돌립니다. Docker만 켜져 있으면 됩니다.

```bash
./gradlew test
```

## API

| Method | Path | 설명 |
| --- | --- | --- |
| GET | `/api/plans` | 판매 중인 요금제 목록 |
| POST | `/api/subscriptions` | 구독 생성 (`PENDING`, 첫 결제 대기) |
| GET | `/api/subscriptions/{id}` | 구독 조회 |
| POST | `/api/subscriptions/{id}/cancel` | 구독 해지 |

```bash
curl -X POST localhost:8080/api/subscriptions \
  -H 'Content-Type: application/json' \
  -d '{"memberId": 1, "planCode": "PRO"}'
```

에러는 RFC 9457 Problem Details 형식이고, 클라이언트가 분기할 수 있게 `code` 필드를 넣었습니다.

```json
{ "status": 409, "code": "SUBSCRIPTION_ALREADY_EXISTS", "detail": "Member already has a subscription in progress" }
```

## 구독 상태

```
PENDING ──첫 결제 성공──> ACTIVE ──갱신 실패──> PAST_DUE ──유예기간 종료──> EXPIRED
   │                        │
   └──해지──> CANCELED       └──해지──> 결제한 기간 끝날 때까지 유지 후 종료
```

- 결제 전(`PENDING`) 해지는 즉시 `CANCELED`
- 이미 결제한 구독은 바로 끊지 않고 `cancelAtPeriodEnd = true`로 두고 기간 끝까지 사용 가능
- 갱신일은 한국 시간 기준으로 계산합니다. 1월 31일에 결제하면 다음 결제일은 2월 28일입니다.

## 고민했던 부분

### 같은 회원이 동시에 구독 요청을 보내는 경우

버튼 더블클릭이나 앱 재시도 때문에 같은 요청이 거의 동시에 들어올 수 있습니다. 서비스에서 `exists` 체크를 먼저 하지만, 두 요청이 동시에 체크를 통과하면 구독이 두 개 생깁니다.

그래서 DB에 부분 유니크 인덱스를 걸었습니다.

```sql
CREATE UNIQUE INDEX uk_subscriptions_member_live
    ON subscriptions (member_id)
    WHERE status IN ('PENDING', 'ACTIVE', 'PAST_DUE');
```

해지/만료된 구독은 인덱스 대상이 아니라서 다시 구독하는 건 가능합니다. 인덱스 위반은 `saveAndFlush`로 서비스 안에서 바로 잡아서 409로 돌려줍니다.
`ConcurrentSubscribeTest`에서 10개 스레드가 동시에 요청해도 1개만 생성되는지 확인합니다.

## 진행 상황

- [x] 요금제 / 구독 도메인, 상태 전이, API
- [x] 동시 구독 요청 방어 (부분 유니크 인덱스)
- [x] Testcontainers 통합 테스트, GitHub Actions CI
- [ ] 토스페이먼츠 결제 승인 연동 (테스트 키)
- [ ] 멱등성 키로 중복 결제 방지
- [ ] 결제 웹훅 처리 (중복 / 순서 뒤바뀐 이벤트)
- [ ] 정기 결제 갱신 배치, 실패 시 재시도 + 유예기간
- [ ] 선착순 프로모션 동시성 (DB 락 vs Redis 락 비교)
- [ ] k6 부하 테스트 결과
- [ ] 배포

---

## English

### Run it

```bash
docker compose up -d
./gradlew bootRun
./gradlew test   # needs Docker, tests run against a real PostgreSQL via Testcontainers
```

### What's here so far

- Plans and subscriptions with explicit state transitions (`PENDING → ACTIVE → PAST_DUE → EXPIRED`, plus cancel)
- Canceling an active subscription keeps it running until the paid period ends
- Renewal dates are calculated in Korea time (Jan 31 → Feb 28)
- A member can't end up with two live subscriptions even when requests race: there's a partial unique index on `member_id` for live statuses, and a test that fires 10 parallel requests and checks only one wins
- Errors are RFC 9457 problem details with a machine-readable `code`

### Next

Toss Payments integration (test keys), idempotency keys, webhook handling, the renewal job with retries and a grace period, a first-come-first-served promo to compare DB vs Redis locking, and k6 load test numbers.
