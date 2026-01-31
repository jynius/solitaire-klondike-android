# Klondike Solitaire 시나리오(초안)

본 문서는 엔진/UI 구현의 기준이 되는 핵심 게임 시나리오를 간단히 정리합니다. 향후 테스트 케이스와 동기화합니다.

---

## 📋 개발 체크리스트 (Development TODO)

### ✅ 완료된 기능 (Completed Features)
- [x] 기본 게임 로직 (초기화, 드로우, 리사이클)
- [x] 카드 이동 규칙 (Tableau, Foundation, Waste)
- [x] 자동 뒤집기
- [x] 언두/리두 시스템
- [x] 점수 시스템 (W→T +5, →F +10, T→F +10, F→T -15)
- [x] 타이머 & 이동 수 추적
- [x] 게임 상태 저장/복원 (GS2 형식)
- [x] 승리 조건 & 다이얼로그
- [x] 폭죽 애니메이션 (승리 시)
- [x] 트레일 애니메이션 (더블 클릭 → Foundation, 600ms)
- [x] 드래그 앤 드롭 UI
- [x] Portrait 전용 모드
- [x] 버튼 재배치 (실행취소, 새로시작, 새게임, 설정)

### 🔄 진행 중 (In Progress)
- [ ] 없음

### 📌 다음 작업 우선순위 (Next Priority - High)
- [ ] **솔버 구현** (Solvability 판단 선행 필요)
  - [ ] 간단한 제한 탐색 솔버 (5-10수 앞)
  - [ ] BFS/A* 알고리즘 기반 솔버
  - [ ] 백그라운드 스레드 처리
  
- [ ] **힌트 기능** (솔버 완성 후)
  - [ ] 솔버 기반 "좋은 이동" 추천
  - [ ] 카드 강조 표시 UI
  
- [ ] **막힘 상태 감지** (솔버 완성 후)
  - [ ] 미래 예측 기반 막힘 감지
  - [ ] 다이얼로그 및 대안 제시

### 📝 중간 우선순위 (Medium Priority)
- [ ] **부분 자동 이동**: 안전한 카드만 자동으로 Foundation에 이동
  - [ ] A, 2 등 낮은 카드 자동 이동 옵션
  - [ ] 설정에서 자동 이동 범위 조절
  
- [ ] **자동 완료 시 트레일 애니메이션**
  - [ ] 각 카드가 순차적으로 트레일과 함께 이동
  - [ ] 애니메이션 속도 조절 가능

- [ ] **통계 화면**
  - [ ] 총 게임 수, 승리 수, 승률
  - [ ] 평균/최고/최저 점수
  - [ ] 평균/최단/최장 시간
  - [ ] 이동 수 통계

### 🔧 낮은 우선순위 (Low Priority)
- [ ] **다국어 지원 (Internationalization)**
  - [ ] strings.xml 리소스 분리
  - [ ] 영어(en), 한국어(ko) 지원
  - [ ] 시스템 언어에 따른 자동 전환
  - [ ] 언어 설정 옵션 추가

- [ ] **버튼 아이콘화**
  - [ ] 텍스트 버튼 → 아이콘 버튼으로 변경
  - [ ] Material Icons 적용 (실행취소, 새로시작, 새게임, 설정 등)
  - [ ] 접근성 고려 (contentDescription)
  - [ ] 아이콘 크기 및 배치 최적화

- [ ] **테마 시스템**
  - [ ] 다크 모드
  - [ ] 카드 뒷면 디자인 선택
  - [ ] 배경 색상/이미지 변경

- [ ] **소리/진동 효과**
  - [ ] 카드 이동 소리
  - [ ] 승리 시 효과음
  - [ ] 진동 피드백 옵션

- [ ] **튜토리얼/도움말**
  - [ ] 게임 규칙 설명
  - [ ] 조작 방법 안내
  - [ ] 팁 & 전략

### 🌐 서버/통계 연동 (Server Integration)
- [ ] **Room DB 전환**: 파일 기반 → 데이터베이스
- [ ] **프라이버시 UI**: 통계 업로드 동의 화면
- [ ] **서버 통합**: 실제 서버 연동 및 테스트
- [ ] **리더보드**: 전체/일일/주간 랭킹

---

## 용어
- Tableau(테이블): 7개 열(왼쪽부터 1~7장, 마지막 장만 앞면)
- Foundation(기초): 4개 수트별 더미, A→K 오름차순
- Stock(스톡): 남은 뒷면 카드 더미
- Waste(웨이스트): 스톡에서 뒤집어 낸 앞면 카드 더미

## 초기화 시나리오 ✅ **[구현됨]**
1. 새 게임 시작 시
   - 스톡을 섞는다 (Fisher-Yates, seed 기반 결정적 셔플).
   - 테이블 1~7열에 각각 1~7장 배치, 각 열의 맨 위 카드만 앞면, 나머지는 뒷면.
   - 파운데이션은 비어 있다.
   - 스톡에는 남은 카드(52-28=24장)가 뒷면으로 남아 있다.
   - 웨이스트는 비어 있다.

## 드로우/리사이클 시나리오 ✅ **[구현됨]**
2. 스톡에서 카드 1장 드로우(1-Draw 규칙)
   - 스톡 맨 위 카드를 앞면으로 뒤집어 웨이스트 맨 위에 놓는다.
   - 스톡이 빌 때까지 반복 가능.
   - 3-Draw 규칙도 지원 (Ruleset에서 설정).
3. 리사이클(스톡이 비었고 웨이스트가 비어있지 않음)
   - 웨이스트의 카드를 순서 유지 혹은 역순(채택 규칙에 따름)으로 모두 뒤집어 스톡으로 되돌린다(뒷면).
   - 드로우 횟수 제한 지원 (무한/-1 또는 n회).

## 이동 규칙 시나리오(테이블) ✅ **[구현됨]**
4. 웨이스트→테이블 이동
   - 색상 교차(빨강↔검정), 랭크 내림차순(예: 7♣는 8♥/8♦ 위에만 가능).
   - 빈 테이블 칸에는 K 또는 K로 시작하는 연속 시퀀스만 놓을 수 있다.
5. 테이블→테이블 이동
   - 위와 동일한 규칙으로, 앞면 연속 시퀀스를 통째로 이동 가능.
   - 이동 후 맨 위가 뒷면 카드라면 자동으로 한 장 뒤집는다.

## 이동 규칙 시나리오(파운데이션) ✅ **[구현됨]**
6. 웨이스트/테이블→파운데이션 이동
   - 동일 수트, A부터 시작하여 오름차순(예: A♠ → 2♠ → … → K♠).
   - 파운데이션→테이블 이동은 Ruleset 옵션으로 제어 가능 (`allowFoundationToTableau`).

## 자동 동작/보조 기능 시나리오
7. 자동 뒤집기 ✅ **[구현됨]**
   - 테이블 열의 맨 위 카드가 뒷면이고, 그 위의 앞면 카드가 모두 이동되어 사라졌다면 즉시 한 장 뒤집어 앞면으로 만든다.
8. 자동 이동 ✅ **[부분 구현됨]**
   - 자동 완료(Auto-complete): 가능한 경우 모든 카드를 파운데이션으로 자동 이동 → **구현됨**
   - ❌ 부분 자동 이동(A만, 2까지만 등) 옵션 → **미구현**
9. 언두/리두 ✅ **[구현됨]**
   - 모든 이동/뒤집기/리사이클은 스택에 기록되어 한 단계씩 되돌릴 수 있다.
10. 힌트 기능 ❌ **[미구현]**
   - 가능한 이동 제안 기능

## 승리/종료 시나리오
10. 승리 조건 ✅ **[구현됨]**
    - 4개 파운데이션이 모두 K까지 완성되면 즉시 승리.
    - ✅ **승리 다이얼로그**: 폭죽 애니메이션과 함께 축하 메시지 표시 → **구현됨**
    - ✅ **버튼**: "새 게임" (다른 시드), "새로 시작" (같은 시드) → **구현됨**
11. 막힘 상태 ⏳ **[선택/미구현]**
    - 더 이상 유효한 이동이 없고 리사이클도 불가하면 막힘 상태로 표시(패배 처리는 선택).

## 채점/타이머 ✅ **[구현됨]**
12. 점수/시간
    - ✅ **점수 시스템**: Waste→Tableau(+5), →Foundation(+10), Tableau→Foundation(+10), Foundation→Tableau(-15) → **구현됨**
    - ✅ **시간 측정**: `startedAt`, `totalPausedMs` 엔진 레벨에서 추적 → **구현됨**
    - ✅ **실시간 타이머 UI**: 게임 화면 헤더에 경과 시간, 점수, 이동 수 표시 → **구현됨**
    - ✅ **일시정지 기능**: 일시정지 시간 추적 및 pause/resume 메서드 → **구현됨**
    - ✅ **게임 상태 저장/복원**: 앱 종료/백그라운드 시 타이머, 점수, 이동 수 자동 저장 (GS2 형식) → **구현됨**

## 애니메이션 ✅ **[구현됨]**
13. 카드 이동 애니메이션
    - ✅ **트레일 애니메이션**: 더블 클릭으로 Foundation에 이동할 때만 트레일 효과 표시 (600ms) → **구현됨**
    - ✅ **폭죽 애니메이션**: 게임 승리 시 다이얼로그에 폭죽 효과 → **구현됨**
    - ❌ 드래그 앤 드롭 스케일/페이드 애니메이션 → **제거됨** (불필요한 지연 제거)
    - ❌ 성공/실패 펄스 효과, 흔들기 효과 → **제거됨** (불필요한 지연 제거)
    - ❌ 자동 완료 애니메이션 → **제거됨** (즉시 완료)
    - ❌ 카드 뒤집기 애니메이션 → **제거됨** (즉시 뒤집기)

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
- 중복 관리는 Deal ID(덱 순서  - 통계/동기화 시스템
- ✅ `GameEngine.startGame(seed: ULong, rules: Ruleset)` 구현(플랫폼 독립 셔플 포함)
- ✅ `DealId.from(seed, rules, deckOrder)` 고정 규칙 정의 + 테스트
- ✅ 저장/복원 SaveCodec(엔진 전체 상태 + 규칙 + redealsRemaining)
- ✅ SolveStats 수집/직렬화(SV1;key=value;...) 및 유닛 테스트
- ✅ 로컬 큐(파일 기반 JSONL/SV1) 보관소 및 단위 테스트
- ✅ 업로드 파이프라인 스켈레톤(Retrofit DTO + WorkManager 워커 + 스케줄러)
- ❌ Room 전환(파일 큐 → DB), 마이그레이션 → **미구현**
- ❌ 프라이버시/동의 UI, 업로드 플래그 노출 → **미구현**
- ❌ 서버 통합(실 주소/인증/에러 처리), 스모크 테스트 2~3개 → **미구현**
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

---

## 게임 기록(History) 기능 시나리오 ❌ **[미구현]**

게임 기록 기능은 사용자가 과거에 플레이한 게임들을 조회하고, 재플레이하거나, 통계를 확인할 수 있도록 합니다.

**현재 상태**: 
- ✅ 엔진 레벨에서 통계 수집 (SolveStats: 시간, 이동 횟수, 결과)
- ✅ 파일 기반 임시 저장소 (JSONL)
- ❌ Room 데이터베이스 마이그레이션
- ❌ UI (기록 목록, 상세, 통계 대시보드)
- ❌ 진행 중 게임 관리
- ❌ 재플레이 기능

### 1) 기록 저장 대상
각 게임 세션마다 다음 정보를 저장합니다:

#### 필수 정보
- `sessionId`: 고유 세션 식별자(UUID)
- `dealId`: 게임 배치 식별자
- `layoutId`: 초기 배치 레이아웃 식별자
- `seed`: 난수 시드
- `ruleset`: 게임 규칙 (draw count, redeals, recycle order 등)
- `startedAt`: 게임 시작 시각(ISO 8601)
- `finishedAt`: 게임 종료 시각(nullable, 진행 중이면 null)
- `duration`: 총 플레이 시간(밀리초, 일시정지 시간 제외)
- `outcome`: 결과 (`win`, `lose`, `abandoned`, `inProgress`)

#### 게임 통계
- `moveCount`: 총 이동 횟수
- `drawCount`: 스톡 드로우 횟수
- `recycleCount`: 리사이클 횟수
- `undoCount`: 언두 사용 횟수
- `hintCount`: 힌트 사용 횟수(선택)
- `autoMoveCount`: 자동 이동 횟수(선택)
- `score`: 점수(점수 시스템 활성화 시)

#### 추가 메타데이터
- `pausedDuration`: 일시정지 누적 시간
- `firstMoveAt`: 첫 이동 시각(사고 시간 분석용)
- `device`: 기기 정보(선택, 프라이버시 고려)
- `appVersion`: 앱 버전

### 2) 기록 조회 시나리오

#### 2-1) 전체 기록 목록
**목적**: 사용자가 과거 플레이한 모든 게임 확인

**UI 요소**:
- 리스트 뷰: 최신순/오래된순/승리만/미완료만 필터링
- 각 항목 표시:
  - 날짜/시간
  - 결과 아이콘 (승리/패배/진행중)
  - 소요 시간
  - 이동 횟수
  - Deal ID (선택)

**정렬/필터 옵션**:
- 정렬: 최신순(기본), 시간순, 이동 횟수순
- 필터: 전체/승리만/패배만/진행중/중단됨
- 검색: Deal ID로 특정 배치의 모든 플레이 기록 검색

#### 2-2) 상세 기록 조회
**목적**: 특정 게임의 자세한 정보 확인

**표시 정보**:
- 기본 정보: 날짜, 소요 시간, 결과
- 통계: 이동/드로우/리사이클/언두 횟수
- 효율성 지표:
  - 평균 이동 시간
  - 언두율 (언두 횟수 / 총 이동)
  - 사고 시간 (첫 이동까지 걸린 시간)
- 규칙 설정: Draw count, Redeals 등

**액션**:
- "다시 플레이": 동일한 배치로 새 게임 시작
- "공유": Deal ID 공유
- "삭제": 기록 삭제

#### 2-3) 통계 대시보드
**목적**: 전체 플레이 통계를 시각화

**표시 정보**:
- 총 게임 수 / 승리 수 / 승률
- 평균 플레이 시간
- 평균 이동 횟수
- 최고 기록:
  - 최소 이동으로 승리
  - 최단 시간 승리
  - 최장 연승 기록
- 그래프/차트:
  - 일별/주별/월별 플레이 횟수
  - 승률 추이
  - 평균 이동 횟수 추이

### 3) 진행 중 게임 관리

#### 3-1) 자동 저장
**시나리오**:
- 앱 종료/백그라운드 진입 시 현재 게임 상태 자동 저장
- 저장 내용: 전체 게임 상태 + 이동 히스토리 + 현재 통계
- 재시작 시 마지막 플레이 게임 복원 옵션 제공

#### 3-2) 진행 중 게임 목록
**시나리오**:
- 사용자가 여러 게임을 동시에 진행 가능
- 메인 화면에서 "게임 선택" → 진행 중인 게임 목록 표시
- 각 게임의 스냅샷(작은 프리뷰) 및 기본 정보 표시
- 선택하여 이어하기 또는 삭제

**저장 정책**:
- 진행 중 게임은 최대 N개(예: 10개)까지 유지
- 초과 시 가장 오래된 게임부터 자동 중단(abandoned) 처리

### 4) 재플레이 시나리오

#### 4-1) 동일 배치 재플레이
**시나리오**:
- 기록 상세 화면에서 "다시 플레이" 선택
- 동일한 seed/ruleset으로 새 게임 생성
- 새 세션으로 기록됨 (sessionId는 새로 생성)
- 이전 기록과 비교 가능 (이동 수, 시간 등)

#### 4-2) 베스트 기록 갱신 추적
**시나리오**:
- 동일 Deal ID를 여러 번 플레이한 경우
- 최소 이동 수, 최단 시간 등 개인 베스트 기록(Personal Best) 자동 추적
- 기록 갱신 시 UI에 표시 및 축하 메시지

### 5) 데이터 저장 구조

#### 5-1) 로컬 DB (Room)
**테이블: game_sessions**
```sql
CREATE TABLE game_sessions (
    session_id TEXT PRIMARY KEY,
    deal_id TEXT NOT NULL,
    layout_id TEXT,
    seed INTEGER NOT NULL,
    ruleset TEXT NOT NULL,  -- JSON
    started_at TEXT NOT NULL,
    finished_at TEXT,
    duration INTEGER,
    outcome TEXT NOT NULL,
    move_count INTEGER DEFAULT 0,
    draw_count INTEGER DEFAULT 0,
    recycle_count INTEGER DEFAULT 0,
    undo_count INTEGER DEFAULT 0,
    hint_count INTEGER DEFAULT 0,
    score INTEGER,
    paused_duration INTEGER DEFAULT 0,
    first_move_at TEXT,
    saved_state BLOB,  -- 진행 중 게임 상태 (압축된 JSON)
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE INDEX idx_deal_id ON game_sessions(deal_id);
CREATE INDEX idx_started_at ON game_sessions(started_at);
CREATE INDEX idx_outcome ON game_sessions(outcome);
```

#### 5-2) DAO 인터페이스
```kotlin
interface GameSessionDao {
    @Query("SELECT * FROM game_sessions ORDER BY started_at DESC LIMIT :limit")
    fun getRecentSessions(limit: Int): List<GameSession>
    
    @Query("SELECT * FROM game_sessions WHERE outcome = 'inProgress'")
    fun getInProgressSessions(): List<GameSession>
    
    @Query("SELECT * FROM game_sessions WHERE deal_id = :dealId ORDER BY started_at DESC")
    fun getSessionsByDeal(dealId: String): List<GameSession>
    
    @Query("SELECT * FROM game_sessions WHERE outcome = 'win' AND deal_id = :dealId ORDER BY move_count ASC LIMIT 1")
    fun getPersonalBest(dealId: String): GameSession?
    
    @Insert
    suspend fun insert(session: GameSession)
    
    @Update
    suspend fun update(session: GameSession)
    
    @Delete
    suspend fun delete(session: GameSession)
}
```

### 6) 기록 관리 정책

#### 6-1) 보관 기간
- 진행 중 게임: 최대 30일 후 자동 중단 처리
- 완료된 게임: 무제한 보관 (사용자가 직접 삭제 가능)
- 저장 공간 제한 시 경고 및 정리 옵션 제공

#### 6-2) 백업/내보내기
**시나리오**:
- 사용자가 전체 기록을 JSON/CSV 파일로 내보내기 가능
- 클라우드 백업(Google Drive 등) 연동 (선택)
- 다른 기기로 가져오기 기능

**내보내기 포맷 (JSON 예시)**:
```json
{
  "version": "1.0",
  "exportedAt": "2026-01-31T12:00:00Z",
  "sessions": [
    {
      "sessionId": "550e8400-e29b-41d4-a716-446655440000",
      "dealId": "DEAL_...",
      "outcome": "win",
      "duration": 180000,
      "moveCount": 95,
      ...
    }
  ]
}
```

### 7) 프라이버시 고려사항
- 기록은 기본적으로 로컬에만 저장
- 서버 동기화는 사용자 동의 시에만 (Opt-in)
- 기기 정보 등 민감 정보는 수집하지 않거나 익명화
- "모든 기록 삭제" 기능 제공

### 8) 테스트 시나리오

#### 8-1) 기록 생성 테스트
- 새 게임 시작 → 이동 → 종료 → DB에 기록 저장 확인
- 진행 중 게임 상태 저장 → 앱 재시작 → 복원 확인

#### 8-2) 조회/필터 테스트
- 다양한 결과(win/lose/abandoned)의 게임 생성
- 필터/정렬 옵션별로 올바른 결과 반환 확인
- 페이징 처리 확인

#### 8-3) 통계 계산 테스트
- 여러 게임 기록 삽입
- 승률, 평균 이동 수 등 통계 정확성 확인
- Personal Best 업데이트 로직 검증

#### 8-4) 재플레이 테스트
- 동일 Deal ID로 새 게임 시작
- 초기 배치가 동일한지 확인
- 별도 세션으로 기록되는지 확인

### 9) UI/UX 권장사항

#### 9-1) 히스토리 화면 구성
```
┌─────────────────────────────┐
│ 게임 기록        [필터] [정렬]│
├─────────────────────────────┤
│ ⚫ 오늘                        │
│ ┌──────────────────────────┐│
│ │🏆 09:32  3분 24초  95수  ││
│ └──────────────────────────┘│
│ ┌──────────────────────────┐│
│ │❌ 08:15  5분 12초  중단   ││
│ └──────────────────────────┘│
│ ⚫ 어제                        │
│ ┌──────────────────────────┐│
│ │🏆 22:45  4분 08초  102수 ││
│ └──────────────────────────┘│
│ ...                          │
└─────────────────────────────┘
```

#### 9-2) 상세 화면
```
┌─────────────────────────────┐
│ ← 게임 상세                   │
├─────────────────────────────┤
│ 2026-01-31 09:32            │
│ 🏆 승리                       │
│                              │
│ 소요 시간: 3분 24초          │
│ 이동 횟수: 95                │
│ 드로우: 48회                 │
│ 언두: 5회                    │
│                              │
│ 효율성                        │
│ • 평균 이동 시간: 2.1초      │
│ • 언두율: 5.3%               │
│                              │
│ [다시 플레이] [공유] [삭제]  │
└─────────────────────────────┘
```

#### 9-3) 통계 대시보드
```
┌─────────────────────────────┐
│ 통계                          │
├─────────────────────────────┤
│ 총 게임: 128                 │
│ 승리: 64 (50.0%)             │
│ 평균 시간: 4분 15초          │
│ 평균 이동: 98.5수            │
│                              │
│ 📊 승률 추이                  │
│ ┌─────────────────────────┐│
│ │     ⚬─⚬─⚬               ││
│ │   ⚬       ⚬             ││
│ │ ⚬                        ││
│ └─────────────────────────┘│
│  월  화  수  목  금  토  일 │
│                              │
│ 🏆 최고 기록                  │
│ • 최소 이동: 78수    ❌:
- [ ] GameSession 엔티티 및 DAO 구현
- [ ] Room 데이터베이스 설정 및 마이그레이션
- [ ] 게임 종료 시 기록 저장
- [ ] 기본 기록 목록 조회 (최신순)
- [ ] 진행 중 게임 저장/복원

**Phase 2** ❌:
- [ ] 필터/정렬 기능
- [ ] 상세 기록 화면
- [ ] 동일 배치 재플레이
- [ ] Personal Best 추적

**Phase 3** ❌:
- [ ] 통계 대시보드
- [ ] 그래프/차트 시각화
- [ ] 내보내기/가져오기
- [ ] 클라우드 백업 (선택)

**추가 기능 (선택)** ❌:
- [ ] 점수 시스템 (시나리오 §12 참조)
- [ ] 실시간 타이머 UI
- [ ] 일시정지 기능
- [ ] 힌트 기능
- [ ] 동일 배치 재플레이
- [ ] Personal Best 추적

**Phase 3**:
- [ ] 통계 대시보드
- [ ] 그래프/차트 시각화
- [ ] 내보내기/가져오기
- [ ] 클라우드 백업 (선택)

---
