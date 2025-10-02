# Klondike Solitaire 시나리오(초안)

본 문서는 엔진/UI 구현의 기준이 되는 핵심 게임 시나리오를 간단히 정리합니다. 향후 테스트 케이스와 동기화합니다.

## 용어
- Tableau(테이블): 7개 열(왼쪽부터 1~7장, 마지막 장만 앞면)
- Foundation(기초): 4개 수트별 더미, A→K 오름차순
- Stock(스톡): 남은 뒷면 카드 더미
- Waste(웨이스트): 스톡에서 뒤집어 낸 앞면 카드 더미

## 초기화 시나리오
1. 새 게임 시작 시
   - 스톡을 섞는다.
   - 테이블 1~7열에 각각 1~7장 배치, 각 열의 맨 위 카드만 앞면, 나머지는 뒷면.
   - 파운데이션은 비어 있다.
   - 스톡에는 남은 카드(52-28=24장)가 뒷면으로 남아 있다.
   - 웨이스트는 비어 있다.

## 드로우/리사이클 시나리오
2. 스톡에서 카드 1장 드로우(1-Draw 규칙)
   - 스톡 맨 위 카드를 앞면으로 뒤집어 웨이스트 맨 위에 놓는다.
   - 스톡이 빌 때까지 반복 가능.
3. 리사이클(스톡이 비었고 웨이스트가 비어있지 않음)
   - 웨이스트의 카드를 순서 유지 혹은 역순(채택 규칙에 따름)으로 모두 뒤집어 스톡으로 되돌린다(뒷면).
   - 드로우 횟수 제한은 기본 없음(무한 리딜)로 가정.

## 이동 규칙 시나리오(테이블)
4. 웨이스트→테이블 이동
   - 색상 교차(빨강↔검정), 랭크 내림차순(예: 7♣는 8♥/8♦ 위에만 가능).
   - 빈 테이블 칸에는 K 또는 K로 시작하는 연속 시퀀스만 놓을 수 있다.
5. 테이블→테이블 이동
   - 위와 동일한 규칙으로, 앞면 연속 시퀀스를 통째로 이동 가능.
   - 이동 후 맨 위가 뒷면 카드라면 자동으로 한 장 뒤집는다.

## 이동 규칙 시나리오(파운데이션)
6. 웨이스트/테이블→파운데이션 이동
   - 동일 수트, A부터 시작하여 오름차순(예: A♠ → 2♠ → … → K♠).

## 자동 동작/보조 기능 시나리오
7. 자동 뒤집기
   - 테이블 열의 맨 위 카드가 뒷면이고, 그 위의 앞면 카드가 모두 이동되어 사라졌다면 즉시 한 장 뒤집어 앞면으로 만든다.
8. 자동 이동(옵션)
   - 확정적으로 유리한 이동(A를 파운데이션으로) 등은 자동 수행 옵션을 제공.
9. 언두/리두
   - 모든 이동/뒤집기/리사이클은 스택에 기록되어 한 단계씩 되돌릴 수 있다.

## 승리/종료 시나리오
10. 승리 조건
    - 4개 파운데이션이 모두 K까지 완성되면 즉시 승리.
11. 막힘 상태(선택)
    - 더 이상 유효한 이동이 없고 리사이클도 불가하면 막힘 상태로 표시(패배 처리는 선택).

## 채점/타이머(선택)
12. 점수/시간
    - 이동/파운데이션 적립/언두 등에 가중치를 두어 점수 계산.
    - 게임 시간 측정 및 통계 저장.

## 테스트 포인트(예시)
- 초기 배치가 정확한가(각 열 카드 수, 앞/뒷면, 스톡/웨이스트/파운데이션 크기)
- 스톡 드로우/리사이클 동작 검증
- 웨이스트→테이블, 테이블→테이블, →파운데이션 유효/무효 이동 검증
- 빈 칸에 K/시퀀스만 놓이는지 확인
- 자동 뒤집기 트리거 확인
- 전체 이동 히스토리 기반 언두 작동
- 모든 파운데이션 완성 시 승리 신호

> 구현 시 세부 규칙(리사이클 시 순서 유지/역순, 자동 이동 범위 등)은 설정값으로 분리하여 테스트에서 명시적으로 고정하세요.

## 배치(Deal) 식별과 해결 가능성 관리

초기 카드 배치는 이론상 52! 가지가 될 수 있으나, 모든 배치를 전수 분류하는 것은 실용적이지 않습니다. 대신 ‘재현 가능한 시드 기반 배치’와 ‘해결 가능성(솔버 결과) 레지스트리’를 도입해 관리합니다.

### 1) 재현 가능한 Deal ID
- RNG 시드(u64) + 규칙셋(예: draw=1|3, redeals=unlimited|n) + 셔플 알고리즘(Fisher–Yates, Kotlin Random(seed))을 계약으로 고정합니다.
- 초기 덱 순서를 생성한 뒤, `(ruleset || seed || deckOrder)`를 해시(SHA-256)한 값을 Base32/58로 인코딩한 문자열을 Deal ID로 사용합니다.
- 이 방식은 같은 시드와 같은 규칙에서 언제나 동일한 배치를 재현합니다(플랫폼 독립적 구현 권장).

권장 계약(Contract):
- Input: `ruleset { drawCount: Int, redeals: Int|-1, recycleOrder: keep|reverse }`, `seed: ULong`
- Process: `deck = shuffleFisherYates(seed)`, `layout = dealToTableau(deck)`
- Output: `dealId: String`, `initialLayout: Layout`
- Error modes: 시드 재현 실패/플랫폼별 Random 차이를 방지하기 위해 자체 난수 구현을 포함.

### 1-1) Layout ID(초기 배치 고유 식별자)
- 입력이나 셔플 방식에 관계없이, 최종 "초기 배치(Layout)"가 동일하면 동일한 ID를 부여합니다.
- 직렬화 규칙(공백 없음/필드순서 고정)으로 `tableau/foundation/stock/waste`를 문자열화 → SHA-256 → Base58 → `L1_...` 형식으로 생성.
- 활용:
   - 중복 제거: 서로 다른 seed/rules라도 결과 레이아웃이 같다면 같은 Layout ID로 묶어 통계/리더보드를 공유 가능.
   - 캐시/재현: Layout ID만으로도 같은 초기 배치를 받아 재시작(필요 시 ruleset은 별도 메타로 병행 저장).
- Deal ID와의 관계:
   - Deal ID는 생성 과정(규칙/시드/버전)을 포함한 ‘경로’의 식별자.
   - Layout ID는 결과 레이아웃 그 ‘결과물’의 식별자. 둘을 함께 저장하면 운영과 분석 모두 유연해집니다.

직렬화 규칙 요약(Layout ID)
- 버전: `lv`는 0부터 시작(초기 계약 버전 = 0). 직렬화 포맷이 바뀌면 lv를 증가.
- 대상 상태: “게임 초기화 직후 상태”를 그대로 직렬화합니다.
   - foundation: 4개 빈 배열로 고정(`[[],[],[],[]]`).
   - waste: 빈 배열로 고정(`[]`).
   - tableau: 7개 열, 각 열은 아래에서 위 순서(또는 위에서 아래) 중 한 가지로 계약. 예: 아래에서 위 순서로 나열.
      - 각 카드 표기: `"수트:랭크:면"` 예) `"S:A:u"`(앞면), `"D:K:d"`(뒷면)
      - 초기화 규칙상 각 열의 맨 위 카드만 앞면(`...:u`), 나머지는 모두 뒷면(`...:d`).
   - stock: 초기 스톡 순서를 배열로 직렬화(맨 위가 배열의 마지막 요소인지 등 방향도 계약에 명시). 예: “맨 위가 마지막”.
- JSON 형식: 공백/줄바꿈 없음, 필드 순서 고정: `{"lv":<int>,"tableau":[...],"foundation":[...],"stock":[...],"waste":[...]}`
- 해시/인코딩: 위 JSON → SHA-256 → Base58 → 접두사 `L1_`
- 주의: 10은 `"10"`으로 표기, 로케일 비의존(ASCII 고정)

### 2) 해결 가능성(Solvability) 레지스트리
- 각 Deal에 대해 솔버가 탐색을 수행하고 결과를 저장합니다.
- 상태 분류:
   - `unknown`: 아직 시도하지 않음
   - `solvable`: 해를 찾음(해 길이/시간 기록)
   - `unsolved_timeout`: 시간/노드 한도 내에서 해를 찾지 못함(‘불가증명’ 아님)
   - `unsolvable_proven`: 완전 탐색으로 해가 없음을 증명(현실적으로 희귀/비용 큼)

메타데이터 예시(JSON):
```json
{
   "dealId": "DEAL_3C2P...",
   "ruleset": {"draw": 1, "redeals": -1, "recycleOrder": "reverse"},
   "seed": "18446744073709551615",
   "status": "solvable",
   "solveTimeMs": 742,
   "minMoves": 113,
   "solver": {"algo": "IDA*", "heuristics": ["foundation-priority","waste-mobility"], "limits": {"timeMs": 30000, "nodes": 2000000}}
}
```

### 3) 솔버(개략)
- 탐색: DFS/IDA*/A* 등 반복 심화 탐색 + 휴리스틱(파운데이션 가중, 이동 가능성, 숨겨진 카드 노출 우선 등)
- 프루닝: 같은 상태 반복 방문 캐싱(Zobrist 해시 등), 지배적 열악 상태 제거, 무의미 순환 방지
- 한도: 시간/노드/깊이 제한으로 `unsolved_timeout`을 구분(‘불가’와 동일 취급 금지)

### 4) 운영 전략
- ‘데일리 딜’: 날짜 기반 시드(`seed = YYYYMMDD` 등)로 재현 가능한 공용 배치 제공
- ‘즐겨찾기/공유’: Deal ID로 공유/재플레이 가능
- ‘큐레이션’: 내부적으로 `solvable`로 확인된 딜을 난이도(최소 이동수/탐색 난도)별로 선별
- 통계: 사용자 풀이 성공률/시간 집계(로컬 우선, 프라이버시 고려)

### 5) 중복/등가성에 대해
- 수트 재명명 등 이론적 등가 분류는 파운데이션이 수트별 완성을 요구하므로 실제 게임 규칙에서는 동일 배치로 간주하지 않습니다.
- 중복 관리는 Deal ID(덱 순서 기반 해시)로 해결합니다.

### 6) 엔지니어링 할일(To-do)
- [x] `GameEngine.startGame(seed: ULong, rules: Ruleset)` 구현(플랫폼 독립 셔플 포함)
- [x] `DealId.from(seed, rules, deckOrder)` 고정 규칙 정의 + 테스트
- [x] 저장/복원 SaveCodec(엔진 전체 상태 + 규칙 + redealsRemaining)
- [x] SolveStats 수집/직렬화(SV1;key=value;...) 및 유닛 테스트
- [x] 로컬 큐(파일 기반 JSONL/SV1) 보관소 및 단위 테스트
- [x] 업로드 파이프라인 스켈레톤(Retrofit DTO + WorkManager 워커 + 스케줄러)
- [ ] Room 전환(파일 큐 → DB), 마이그레이션
- [ ] 프라이버시/동의 UI, 업로드 플래그 노출
- [ ] 서버 통합(실 주소/인증/에러 처리), 스모크 테스트 2~3개

---

## 수집/저장/업로드 시나리오(클라이언트)

목표: 서버가 준비되지 않아도 오프라인으로 완결되도록 하되, 서버가 열리면 스위치만 켜서 바로 업로드가 되도록 구성합니다.

### 수집: SolveStats
- 이벤트 소스: `GameEngine`
   - `startGame` 시 `startedAt` 기록, `seed`/`ruleset`/`dealId` 고정
   - `draw`/모든 이동 시 `moveCount += 1`
   - 테이블→파운데이션 이동 후 승리 판정 시 `outcome = win`, `finishedAt` 기록
   - 중도 포기/타임아웃은 호출 측에서 `outcomeOverride`로 지정 가능
- 스냅샷: `getSolveStatsSnapshot(outcomeOverride?)`
- 직렬화: `SV1;dealId=...;seed=...;draw=...;redeals=...;recycle=keep|reverse;f2t=true|false;started=...;finished=...;dur=...;moves=...;outcome=...;layoutId=...;client=...;plat=...`
   - 텍스트 1줄로 저장/전송 용이, 의존성 없음

### 저장: 로컬 큐
- 보관소: `pending.sv1`(라인 단위), `uploaded.sv1`(성공 보관)
- 연산:
   - `appendPending(stats)` → SV1 라인 추가
   - `readPending(limit)` → 업로드 후보 읽기
   - `markUploaded(lines)` → 성공 항목 제거+보관

### 업로드: 백그라운드
- 워커: `UploadWorker`(WorkManager)
   - 입력: `baseUrl`, `userId`
   - 동작: `pending`에서 최대 N개 → 디코드 → DTO 매핑 → POST `/v1/solves` → 성공분만 `markUploaded`
- 스케줄러: `UploadScheduler`
   - `schedulePeriodic(context, hours=12)` 네트워크 연결 시 주기 실행
   - `triggerOnce(context)` 즉시 1회 업로드
- 설정: `SyncSettings`
   - `userId`(익명, 최초 생성), `baseUrl`, `uploadEnabled`(동의 플래그)
   - 업로드는 플래그 ON일 때만 작동(기본 OFF)

### 디버그 UI(선택)
- 메인 화면에 토글/트리거/상태 표시를 배치해 서버 없이도 동작 점검이 가능

### 에러/재시도
- 네트워크/서버 오류 시 워커 단위 성공/무시 처리, 주기 워크는 지수 백오프
- 부분 성공 시 성공 항목만 제거하여 중복 업로드 방지

---

## 데일리 퍼즐/리더보드(클라이언트 관점)

- 오프라인:
   - 데일리 seed 룰을 문서(SYNC.md)로 고정(HMAC(date, secret) 등), 서버 준비 전엔 로컬 연산으로 데일리 시뮬레이션 가능
- 온라인:
   - `GET /v1/daily` → `{ dealId, seed, ruleset, validFrom, validTo }`
   - `GET /v1/leaderboards/{dealId}` → 상위 N 엔트리
   - 계약은 OpenAPI(`docs/openapi.yaml`)에 고정되어 서버/클라 병행 개발 가능

---

## 프라이버시/동의

- 기본 OFF(Opt-in)
- 익명 `userId` 생성/보관(로테이션 정책은 추후)
- 서버 업로드는 동의 플래그로 게이팅
- 필수 최소 필드만 전송, 삭제 요청 고려(추후)

---

## 테스트 시나리오(동기화)

- SolveStats 단위 테스트
   - draw/이동에 따른 `moveCount` 증가
   - 동일 seed+rules → DealId 안정성
- 보관소 테스트
   - append → read → markUploaded 순환
- 계약 테스트(MockWebServer)
   - POST `/v1/solves` 요청 본문의 필수 키 존재
   - 성공 응답 파싱(accepted/rank/personalBest)

---

## 버저닝/계약 관리

- DealId: `v`(dealContractVersion), `sv`(shuffleVersion)
- LayoutId: `lv`(layout version)
- SolveStats(SV1): 포맷 접두사 `SV1;`로 호환성 분리
- 직렬화는 공백/순서 고정, 로케일 비의존(ASCII)
