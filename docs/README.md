# Documentation

이 폴더는 Solitaire Klondike Android 프로젝트의 문서를 포함합니다.

## 📚 문서 구조

### 게임 디자인 & 기능

- **[SCENARIOS.md](SCENARIOS.md)** - 게임 시나리오 및 사용 사례
- **[FAVORITES_AND_FILTERING.md](FAVORITES_AND_FILTERING.md)** - 즐겨찾기 및 게임 필터링 기능
- **[STATISTICS_FEATURES.md](STATISTICS_FEATURES.md)** - 게임 통계 및 분석 기능
- **[STATISTICS_IMPLEMENTATION.md](STATISTICS_IMPLEMENTATION.md)** - 통계 기술 구현 상세
- **[GAME_REPLAY_GUIDE.md](GAME_REPLAY_GUIDE.md)** - 게임 세션 저장 및 재생

### Solver & AI

- **[SOLVER_DESIGN.md](SOLVER_DESIGN.md)** - Solver 아키텍처 및 알고리즘 설계
  - Inherently Unsolvable vs Unwinnable State 개념
  - BFS/A* 알고리즘 구현
  - Strategy 패턴 적용
- **[SOLVER_USAGE.md](SOLVER_USAGE.md)** - Solver 시스템 사용 및 확장 가이드
  - BFS vs A* 성능 비교
  - 새로운 Solver 추가 방법
  - 런타임 Solver 선택

### 개발 & 테스트

- **[TEST_GUIDE.md](TEST_GUIDE.md)** - 테스트 전략 및 가이드라인
- **[SYNC.md](SYNC.md)** - 데이터 동기화 상세

### API

- **[openapi.yaml](openapi.yaml)** - RESTful API 스펙

### 스크린샷

- **[screenshot/](screenshot/)** - 앱 스크린샷 및 이미지

## 🔍 주요 개념

### Inherently Unsolvable vs Unwinnable State

게임의 두 가지 "풀 수 없음" 개념을 명확히 구분합니다:

1. **Inherently Unsolvable (게임 속성 - Game Property)**
   - 초기 배치(seed + rules)에 의해 결정
   - 게임 진행과 무관하게 고정
   - 예: N-Pile Irretrievable, King Irretrievable

2. **Unwinnable State (플레이 상태 - Play State)**
   - 현재 게임 상태에 의해 결정
   - 플레이어의 선택에 따라 변화
   - 예: Dead End, State Cycle

자세한 내용은 [SOLVER_DESIGN.md](SOLVER_DESIGN.md)를 참조하세요.

### Solver Strategy 패턴

여러 Solver 알고리즘을 유연하게 사용할 수 있는 구조:

- **Solver 인터페이스**: `solve()`, `findBestMove()`
- **BFSSolver**: 최적 경로 보장, 메모리 집약적
- **AStarSolver**: 빠른 성능, 휴리스틱 기반

자세한 내용은 [SOLVER_USAGE.md](SOLVER_USAGE.md)를 참조하세요.

## 📖 읽는 순서 권장

처음 프로젝트를 접하는 경우:

1. [SCENARIOS.md](SCENARIOS.md) - 프로젝트 전체 개요
2. [SOLVER_DESIGN.md](SOLVER_DESIGN.md) - 핵심 알고리즘 이해
3. [SOLVER_USAGE.md](SOLVER_USAGE.md) - Solver 사용법
4. [TEST_GUIDE.md](TEST_GUIDE.md) - 테스트 방법

기능별 상세 구현을 보려면:

- 통계: [STATISTICS_FEATURES.md](STATISTICS_FEATURES.md) → [STATISTICS_IMPLEMENTATION.md](STATISTICS_IMPLEMENTATION.md)
- 게임 재생: [GAME_REPLAY_GUIDE.md](GAME_REPLAY_GUIDE.md)
- 필터링: [FAVORITES_AND_FILTERING.md](FAVORITES_AND_FILTERING.md)
