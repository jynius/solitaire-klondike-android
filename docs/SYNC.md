# Sync/Server 설계 초안

WSL에서 로컬로 플레이/솔브를 진행하고, 일정 간격으로 서버와 정보를 공유해 Deal(배치)·리더보드·데일리 퍼즐을 운영하기 위한 기반 설계입니다. 지금은 문서만 추가하고, 점진 구현을 전제로 합니다.

## 목표
- Deal ID(배치 식별자)를 전 세계적으로 유일하고 재현 가능하게 정의
- 로컬 solve 데이터를 서버에 업로드하여 리더보드/통계/큐레이션에 활용
- 서버에서 데일리 퍼즐(모든 사용자에게 동일)을 배포
- 오프라인 우선: 네트워크 불가 시 로컬에 큐잉 후 주기적으로 동기화

## 용어/버전
- dealContractVersion: Deal 생성 계약(셔플/레이아웃/규칙 직렬화) 버전
- rulesetVersion, shuffleVersion: 구성 요소 버전(서버/클라이언트 호환성 판단용)

## Deal ID 설계
- 입력: `ruleset`, `seed`, `shuffleVersion`, `dealContractVersion`
- 과정: 결정적 셔플(Fisher–Yates + 자체 PRNG with u64 seed) → 초기 배치 레이아웃 생성 → `canonicalJson(ruleset, seed, versions, deckOrder)` → SHA-256 → Base58
- 출력: `DL1_<Base58>` (접두사 `DL1_`는 포맷 버전1 의미)

예시 canonical JSON(공백/필드순서 고정):
```json
{"v":1,"sv":1,"rules":{"draw":1,"redeals":-1,"recycle":"reverse"},"seed":"18446744073709551615","deck":"S:A,S:2,...,H:K"}
```
- v: dealContractVersion, sv: shuffleVersion
- deck: 52장 순서를 수트:랭크 축약 표기로 나열(플랫폼 독립 직렬화)

주의: 알고리즘/직렬화가 바뀌면 `v` 또는 `sv`를 올려 동일 seed라도 다른 Deal ID가 생성되도록 합니다.

## 데이터 모델(로컬)
Room 또는 파일 기반(JSON)으로 운영 가능. 초안 스키마:

- deals
  - dealId (PK)
  - ruleset (JSON)
  - seed (u64 string)
  - createdAt
  - tags (daily, curated, etc)

- solves
  - id (PK, local UUID)
  - dealId (FK)
  - userId (hash, 익명)
  - startedAt, finishedAt
  - durationMs, moveCount
  - outcome (win|resign|timeout)
  - moveTraceHash (옵션: 재생성/검증용)
  - clientVersion, deviceInfo
  - uploadState (pending|uploaded|failed)

- leaderboards (옵션, 서버 스냅샷 캐싱)
  - dealId, topN JSON, fetchedAt

- sync_state
  - lastUploadAt, lastFetchAt, backoff

## API(서버)
- POST /v1/clients/register
  - 입력: device fingerprint/anon user seed → userId 발급(또는 로컬에서 생성 후 전송)

- GET /v1/daily?date=YYYY-MM-DD
  - 출력: { dealId, seed, ruleset, validFrom, validTo }
  - 구현: HMAC(date, serverSecret) → seed 생성, 커밋-언락 방식으로 사전 유출 방지 가능

- GET /v1/leaderboards/{dealId}?limit=100
  - 출력: 상위 N명 최소 시간/이동수/플랫폼 등

- POST /v1/solves
  - 입력: { dealId, userId, durationMs, moveCount, startedAt, finishedAt, clientVersion, moveTraceHash? }
  - 출력: { accepted: true, rank?: n, personalBest: true|false }
  - 서버 검증: dealId 포맷/해시 유효성, duration/타임라인 sanity check, 중복 제출 dedupe, 옵션으로 리플레이 검증 큐잉

- GET /v1/deals/{dealId}
  - 출력: 메타데이터(확인용), 큐레이션 태그, 규칙셋 등

배치/관리용:
- POST /v1/deals/curate  (관리자)
- GET /v1/deals/curated?limit=...

## 동기화 전략(클라이언트)
- WorkManager 주기 작업: 6~24시간 간격, 네트워크 연결/충전 중 의존 조건
- 단계:
  1) pending solves 업로드(최대 N개/호출)
  2) 성공 시 uploadState=uploaded로 마킹, 실패 시 지수 백오프
  3) 데일리/큐레이션 받아 캐시 갱신
- 충돌/중복: 서버는 (userId, dealId) 단위로 개인 최고 기록만 유지, 느린 기록은 보관 또는 폐기

## 보안/공정성(안티 치트)
- 기본: 서버측 sanity checks(시간/이동수 범위, 제출 속도, 클라이언트 버전)
- 중급: 리플레이 업로드(압축된 move trace) 후 서버 재검증(유효 이동 여부/시간 일관성) → 비용 높음, 선택 활성화
- 고급: 플랫폼 별 원격 측정/앱 무결성(Play Integrity, SafetyNet) 연동 검토
- 운영: 이상치 감지, 레이트 리밋, 임시 차단, 수동 검토 큐

## 프라이버시/규정
- Opt-in 동의(익명 사용자 ID, 기록 전송 목적 명시)
- 수집 최소화(필수 필드만), 삭제 요청 처리(로컬/서버)
- 지역 규정(GDPR/CCPA) 준수: 가명화, 보존 기간 명시

## 서버 선택지
- 빠른 시작: Firebase(Cloud Functions + Firestore) 또는 Supabase(Postgres + Row Level Security)
- 관리형 Postgres + Ktor/Spring Boot REST API (Docker 배포)
- 서버리스: Cloudflare Workers + D1/KV(R2)로 경량 리더보드/데일리 제공

## 데일리 퍼즐 배포
- 권장: 서버 HMAC(seed = HMAC_SHA256(date, secret).u64) → 전 세계 동일 seed
- 타임존: UTC 기준 날짜 롤오버, 클라이언트는 지역 시간에 표시만 변환
- 사전 유출 방지: 다음 날 seed는 일정 시간 전까지 잠금, 릴리스 시에만 공개

## 단계적 도입 로드맵
1) 클라이언트
   - DealId/Ruleset/seed 생성 유틸(결정적 셔플 포함)
   - 로컬 DB(Room) 테이블: deals/solves/pending_uploads/sync_state
   - WorkManager로 업로드 큐/백오프 스케줄링
   - Retrofit 인터페이스(엔드포인트 스텁)와 기능 플래그(업로드 ON/OFF)

2) 서버(프로토타입)
   - 단일 엔드포인트: POST /v1/solves, GET /v1/daily, GET /v1/leaderboards/{dealId}
   - 인증은 초기엔 키 없이, 이후 앱-키/토큰 도입

3) 운영
   - 데일리/큐레이션 운영, 리더보드 노출, 기본 안티치트 적용

## 예시 페이로드
- POST /v1/solves
```json
{
  "dealId": "DL1_XYZ...",
  "userId": "usr_5Fh9...",
  "ruleset": {"draw":1,"redeals":-1,"recycle":"reverse"},
  "durationMs": 73421,
  "moveCount": 127,
  "startedAt": "2025-10-02T12:03:41Z",
  "finishedAt": "2025-10-02T12:04:54Z",
  "clientVersion": "1.0.0",
  "platform": "android",
  "moveTraceHash": "mth_8aa..."
}
```

## 참고/결론
- ‘완전한 승/패 판정’은 어려우나, Deal ID 기반 큐레이션과 솔브 결과 집계로 실용적인 운영이 가능합니다.
- 오늘은 코드 구현 없이 문서/계약만 정리했습니다. 원하시면 다음 단계로 DealId/Ruleset 유틸과 WorkManager 스켈레톤을 추가해 드릴 수 있습니다.
