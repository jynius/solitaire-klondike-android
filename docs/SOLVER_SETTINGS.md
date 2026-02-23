# Solver 설정 및 아키텍처

## 개요

사용자가 힌트(Hint)와 자동 플레이(Auto Play) 기능에 사용할 Solver 알고리즘을 선택할 수 있는 기능입니다.

- **BFS Solver**: 최적 경로 보장, 메모리 사용량 많음, 안정적
- **A* Solver**: 휴리스틱 기반 빠른 실행, 복잡한 게임에 유리

## 아키텍처

### 레이어 구조

```
┌─────────────────────────────────────────────────────────────┐
│                        UI Layer                              │
│  ┌──────────────────┐              ┌──────────────────┐     │
│  │  RulesActivity   │              │  GameActivity    │     │
│  │  - 설정 변경      │              │  - Solver 사용   │     │
│  │  - 저장          │              │  - Hint/Auto     │     │
│  └──────────────────┘              └──────────────────┘     │
│         │ writes                            │ reads          │
│         ▼                                   ▼                │
│  ┌────────────────────────────────────────────────┐         │
│  │         SharedPreferences                      │         │
│  │         Preference: "app_prefs"                │         │
│  │         Key: "solver_type"                     │         │
│  │         Values: "BFS" | "ASTAR"                │         │
│  └────────────────────────────────────────────────┘         │
└─────────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                   ViewModel Layer                            │
│  ┌──────────────────────────────────────────────┐           │
│  │           GameViewModel                      │           │
│  │  - engine: GameEngine                        │           │
│  │  + getEngine(): GameEngine  (read-only)      │           │
│  │  + startGame(), move*(), undo(), ...         │           │
│  └──────────────────────────────────────────────┘           │
│                                                              │
│  Note: Solver/UnsolvableDetector 미소유 (메모리 효율)        │
└─────────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                   Domain Layer                               │
│  ┌─────────────┐    ┌──────────────────────────┐           │
│  │ GameEngine  │    │  <<interface>> Solver    │           │
│  │             │    ├──────────────────────────┤           │
│  │ (게임 로직)  │◄───│ + solve(state)           │           │
│  └─────────────┘    │ + findBestMove(state)    │           │
│         ▲           └──────────────────────────┘           │
│         │                    △                              │
│         │           ┌────────┴────────┐                    │
│         │           │                 │                     │
│    (used by)  ┌──────────┐     ┌──────────┐               │
│         │     │BFSSolver │     │AStarSolver│               │
│         │     └──────────┘     └──────────┘               │
│         │           │                 │                     │
│         └───────────┴─────────────────┘                    │
│         │      (requires GameEngine)                        │
│         │                                                    │
│  ┌─────────────────────────────────────┐                   │
│  │   UnsolvableDetector                │                   │
│  ├─────────────────────────────────────┤                   │
│  │ - engine: GameEngine                │                   │
│  ├─────────────────────────────────────┤                   │
│  │ + checkInherentlyUnsolvable(state)  │                   │
│  │   → 초기 배치 문제 검출              │                   │
│  │ + checkUnwinnableState(state)       │                   │
│  │   → Dead End + State Cycle 검사    │                   │
│  │ + check(state)                      │                   │
│  │   → 통합 검사 (Legacy)               │                   │
│  └─────────────────────────────────────┘                   │
│         △                                                    │
│         │                                                    │
│    (requires GameEngine)                                    │
└─────────────────────────────────────────────────────────────┘
```

### 컴포넌트 다이어그램

```
┌────────────────────────┐
│   RulesActivity        │
├────────────────────────┤
│ - solverRadioGroup     │
├────────────────────────┤
│ + loadCurrentSolver()  │
│ + saveSolver()         │
└────────────────────────┘
         │ uses
         ▼
┌─────────────────────────────────┐
│    SharedPreferences             │
├─────────────────────────────────┤
│ getString("solver_type")        │
│ putString("solver_type", value) │
└─────────────────────────────────┘
         △
         │ uses
┌────────────────────────┐
│   GameActivity         │
├────────────────────────┤
│ - viewModel            │────────┐
├────────────────────────┤        │
│ + onHintClick()        │        │ has
│ - createSolver()       │        │
│ - createDetector()     │        │
└────────────────────────┘        │
         │ creates                 ▼
         │              ┌──────────────────────┐
         │              │   GameViewModel      │
         │              ├──────────────────────┤
         │              │ - engine: GameEngine │
         │              ├──────────────────────┤
         │              │ + getEngine()        │
         │              │ + startGame()        │
         │              │ + move*()            │
         └──────────────│ + getState()         │
                        └──────────────────────┘
         │                        │ owns
         │                        ▼
         │              ┌──────────────────────┐
         │              │    GameEngine        │
         │              ├──────────────────────┤
         │              │ + getGameState()     │
         │              │ + move*()            │
         │              │ + recordCurrentState()│
         │              │ + isStateInHistory() │
         │              └──────────────────────┘
         │                        △
         │                        │ requires
         │                        │
         ▼              ┌─────────┴────────────┐
┌──────────────────────┐          │            │
│  <<interface>>       │          │            │
│     Solver           │          │            │
├──────────────────────┤          │            │
│ + solve(state)       │          │            │
│ + findBestMove(state)│          │            │
└──────────────────────┘          │            │
         △                        │            │
         │ implements              │            │
    ┌────┴─────┐                 │            │
    │          │                  │            │
┌───────┐  ┌───────┐             │            │
│  BFS  │  │ A*    │             │            │
│Solver │  │Solver │─────────────┘            │
└───────┘  └───────┘                          │
                                               │
                        ┌──────────────────────┴────┐
                        │  UnsolvableDetector       │
                        ├───────────────────────────┤
                        │ - engine: GameEngine      │
                        ├───────────────────────────┤
                        │ + checkInherentlyUnsolvable│
                        │ + checkUnwinnableState    │
                        │ + check()                 │
                        │ + checkStateCycle()       │
                        └───────────────────────────┘
```
         │              ├──────────────────────┤
         │              │ + getEngine()        │
         │              │ + startGame()        │
         │              │ + move*()            │
         └──────────────│ + getState()         │
                        └──────────────────────┘
         │                        │ owns
         │                        ▼
         │              ┌──────────────────────┐
         │              │    GameEngine        │
         │              ├──────────────────────┤
         │              │ + getGameState()     │
         │              │ + move*()            │
         │              └──────────────────────┘
         │                        △
         │                        │ requires
         ▼                        │
┌──────────────────────┐          │
│  <<interface>>       │          │
│     Solver           │          │
├──────────────────────┤          │
│ + solve(state)       │          │
│ + findBestMove(state)│          │
└──────────────────────┘          │
         △                        │
         │ implements              │
    ┌────┴─────┐                 │
    │          │                  │
┌───────┐  ┌───────┐             │
│  BFS  │  │ A*    │             │
│Solver │  │Solver │─────────────┘
└───────┘  └───────┘
```

## 구현 세부사항

### 0. Unsolvable 검사 (UnsolvableDetector)

**위치**: `app/src/main/java/us/jyni/game/klondike/solver/UnsolvableDetector.kt`

UnsolvableDetector는 게임이 해결 불가능한 상태를 조기에 감지합니다.

#### 0.1. 아키텍처 위치 (Solver와 동일)

```
GameActivity (필요 시 생성)
    ├── createDetector() → new UnsolvableDetector(viewModel.getEngine())
    └── createSolver() → new Solver(viewModel.getEngine())
```

- **GameViewModel이 소유하지 않음** (Solver와 동일한 이유)
- 필요할 때만 GameActivity에서 생성
- GameEngine을 생성자에서 주입받아 상태 히스토리 접근

#### 0.2. 실행 시점 (Solver와 다름) ⭐

| 구분 | 실행 시점 | 빈도 | 필수 여부 | 목적 |
|------|----------|------|----------|------|
| **UnsolvableDetector** | 게임 시작 시 **(자동)** | 게임당 1회 | ✅ 필수 | UI 상태 표시 |
| **Solver** | Hint/Auto 버튼 **(수동)** | 사용자 요청 시 | ❌ 선택적 | 힌트/자동 플레이 |

**핵심 차이**:
- UnsolvableDetector: `startNewGame()` 내에서 **자동 호출**되어 UI에 이모지 표시
- Solver: 사용자가 버튼 클릭할 때만 **수동 호출**

#### 0.3. 검사 타입

**Inherently Unsolvable (게임 속성)**: 초기 배치 자체의 구조적 문제
```kotlin
fun checkInherentlyUnsolvable(state: GameState): UnsolvableReason? {
    // N-Pile Irretrievable (N=1~5)
    // King Irretrievable
    // ...
}
```
- **특징**: Seed + Rules가 동일하면 언제나 동일한 결과
- **검사 시점**: 게임 시작 시 (한 번만)
- **비용**: O(7ms) 이내
- **패턴**: 
  - N-Pile Irretrievable: N개 pile이 서로의 필요 카드를 모두 막음
  - King Irretrievable: King이 특정 카드들을 영구히 차단

**Unwinnable State (플레이 상태)**: 현재 진행 상황에 따른 막힌 상태
```kotlin
fun checkUnwinnableState(state: GameState): UnsolvableReason? {
    // Dead End: 모든 이동 불가능
    // State Cycle: 이전 상태로 돌아옴
    // ...
}
```
- **특징**: 플레이어의 선택에 따라 매 이동마다 결과가 달라짐
- **검사 시점**: 매 이동 후
- **비용**: O(1ms) 미만
- **패턴**:
  - Dead End: Stock/Waste 비었고 가능한 이동이 없음
  - State Cycle: 이전 방문한 상태로 돌아와 무한 루프 형성

#### 0.4. GameEngine과의 상호작용

**State History 공유**:
```kotlin
class GameEngine {
    private val stateHistory = mutableSetOf<Int>()
    
    fun recordCurrentState(): Boolean {
        val stateHash = calculateStateHash(gameState)
        return stateHistory.add(stateHash)
    }
    
    fun isStateInHistory(): Boolean {
        val stateHash = calculateStateHash(gameState)
        return stateHistory.contains(stateHash)
    }
}
```

**UnsolvableDetector에서 활용**:
```kotlin
fun checkStateCycle(): UnsolvableReason? {
    val isInHistory = engine.isStateInHistory()
    
    if (isInHistory) {
        return UnsolvableReason.StateCycle("이전 상태로 돌아왔습니다")
    }
    
    engine.recordCurrentState()
    return null
}
```

#### 0.5. 사용 시나리오

**시나리오 1: 게임 시작 시 자동 검사** ✅ 필수
```kotlin
// GameActivity - 게임 시작
private fun startNewGame(seed: ULong, rules: Ruleset) {
    viewModel.startGame(seed, rules)
    
    // ✅ 자동으로 Inherently Unsolvable 검사 (UI 표시용)
    val inherentReason = checkInherentlyUnsolvable()
    inherentStatusEmoji = if (inherentReason != null) {
        getString(R.string.state_inherently_unsolvable)  // 🔴 이모지
    } else {
        getString(R.string.state_inherently_solvable)    // 🟢 이모지
    }
    // → 화면에 게임 상태 이모지 표시
}

private fun checkInherentlyUnsolvable(): UnsolvableReason? {
    val detector = createDetector()  // 임시 생성
    return detector.checkInherentlyUnsolvable(viewModel.getState())
    // detector는 함수 종료 후 GC
}
```

**시나리오 2: Solver 실행 전 검사** (선택적 최적화)
```kotlin
// GameActivity - Hint 버튼 (사용자가 클릭할 때만)
findViewById<ImageButton>(R.id.hint_button).setOnClickListener {
    solverScope.launch {
        // 먼저 Unsolvable 검사 (빠름, 7ms)
        val detector = createDetector()
        val unsolvable = detector.check(viewModel.getState())
        
        if (unsolvable != null) {
            // Solver 실행하지 않고 종료 (10초 절약!)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@GameActivity, 
                    "해결 불가능: ${unsolvable.reason}", ...).show()
            }
            return@launch
        }
        
        // Solver 실행 (비용 높음, 10초)
        val solver = createSolverFromSettings()
        val hint = solver.findBestMove(viewModel.getState())
        // ...
    }
}
```

**핵심**:
- UnsolvableDetector: 게임 시작 시 **자동 실행** (항상)
- Solver: 버튼 클릭 시 **수동 실행** (선택적)

#### 0.6. 성능 특성

| 검사 타입 | 시점 | 빈도 | 비용 | 목적 |
|---------|------|------|------|------|
| Inherently Unsolvable | 게임 시작 | 1회 | 7ms | 초기 배치 문제 조기 발견 |
| Unwinnable State | 매 이동 후 | N회 | <1ms | 막힌 상태 즉시 감지 |

**최적화 효과**:
- Solver 실행 전 빠른 검사로 불필요한 BFS/A* 탐색 방지
- 평균 10초 소요되는 Solver를 7ms 검사로 대체 가능
- 사용자에게 즉각적인 피드백 제공

#### 0.7. UnsolvableReason 타입 계층

```kotlin
sealed class UnsolvableReason(val reason: String) {
    // Inherently Unsolvable
    data class NPileIrretrievable(val n: Int, val piles: List<Int>) : UnsolvableReason(...)
    data class KingIrretrievable(val pileIndex: Int, val blockedCards: List<Card>) : UnsolvableReason(...)
    
    // Unwinnable State
    data class DeadEnd(val reason: String) : UnsolvableReason(reason)
    data class StateCycle(val reason: String) : UnsolvableReason(reason)
}
```

**타입 구분의 중요성**:
- Inherently Unsolvable: "이 게임은 처음부터 해결 불가능합니다"
- Unwinnable State: "현재 상태는 막혔지만, 다른 경로로 해결 가능했을 수 있습니다"

### 1. 설정 저장 (RulesActivity)

**위치**: `app/src/main/java/us/jyni/game/klondike/ui/screens/RulesActivity.kt`

```kotlin
private fun saveSolver() {
    val selectedSolver = when (solverRadioGroup.checkedRadioButtonId) {
        R.id.solver_bfs -> SolverType.BFS.name
        R.id.solver_astar -> SolverType.ASTAR.name
        else -> SolverType.BFS.name
    }
    getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        .edit()
        .putString("solver_type", selectedSolver)
        .apply()
}
```

**UI 구성**: `app/src/main/res/layout/activity_rules.xml`
- 언어 선택 RadioGroup 아래에 위치
- Solver 알고리즘 선택 RadioGroup
- BFS와 A* 두 가지 옵션 제공

### 2. Solver 및 Detector 생성 (GameActivity)

**위치**: `app/src/main/java/us/jyni/game/klondike/ui/screens/GameActivity.kt`

```kotlin
private fun createSolverFromSettings(): Solver {
    val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val solverTypeName = prefs.getString("solver_type", SolverType.BFS.name) 
        ?: SolverType.BFS.name
    
    val solverType = try {
        SolverType.valueOf(solverTypeName)
    } catch (e: IllegalArgumentException) {
        SolverType.BFS  // 기본값
    }
    
    // GameActivity가 직접 Solver 생성
    val engine = viewModel.getEngine()
    return when (solverType) {
        SolverType.BFS -> BFSSolver(engine)
        SolverType.ASTAR -> AStarSolver(engine)
    }
}

private fun createDetector(): UnsolvableDetector {
    // UnsolvableDetector도 Solver처럼 필요 시 생성
    return UnsolvableDetector(viewModel.getEngine())
}
```

**사용 예시**:
```kotlin
// Hint 버튼
findViewById<ImageButton>(R.id.hint_button).setOnClickListener {
    solverScope.launch {
        // 먼저 빠른 Unsolvable 검사 (7ms)
        val detector = createDetector()
        val unsolvable = detector.check(viewModel.getState())
        
        if (unsolvable != null) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@GameActivity, 
                    "해결 불가능: ${unsolvable.reason}", ...).show()
            }
            return@launch
        }
        
        // Solver 실행 (10초)
        val solver = createSolverFromSettings()
        val hint = solver.findBestMove(viewModel.getState())
        
        withContext(Dispatchers.Main) {
            if (hint != null) {
                Toast.makeText(this@GameActivity, "힌트: ${hint}", ...).show()
            }
        }
    }
}

// Auto 버튼
findViewById<ImageButton>(R.id.auto_button).setOnClickListener {
    solverScope.launch {
        val solver = createSolverFromSettings()
        val result = solver.solve(viewModel.getState())
        
        withContext(Dispatchers.Main) {
            when (result) {
                is SolverResult.Success -> { /* 자동 플레이 실행 */ }
                // ...
            }
        }
    }
}
```

### 3. ViewModel 구조 (GameViewModel)

**위치**: `app/src/main/java/us/jyni/game/klondike/ui/GameViewModel.kt`

```kotlin
class GameViewModel : ViewModel() {
    private val engine = GameEngine()
    private val unsolvableDetector = UnsolvableDetector(engine)
    
    /**
     * GameEngine 접근자 (읽기 전용)
     * 
     * 외부에서 Solver 생성 등의 목적으로 GameEngine이 필요할 때 사용합니다.
     * GameEngine을 직접 수정하지 말고, GameViewModel의 메서드를 통해 조작하세요.
     */
    fun getEngine(): GameEngine = engine
    
    // 게임 로직 메서드들
    fun startGame(seed: ULong, rules: Ruleset) { ... }
    fun move*(...) { ... }
    fun undo() { ... }
    // ...
}
```

**중요**: GameViewModel은 Solver를 **소유하지 않습니다**.
- Solver 관련 import 없음
- Solver 인스턴스 없음
- 메모리 효율적
- 관심사 분리

## 데이터 흐름

### 시나리오 1: Solver 설정 변경

```
[사용자가 RulesActivity에서 A* 선택]
        ↓
[saveSolver() 호출]
        ↓
[SharedPreferences에 저장: "solver_type" = "ASTAR"]
        ↓
[Activity 종료, GameActivity로 돌아감]
        ↓
[설정 완료 - 다음 Hint/Auto 실행 시 A* 사용]
```

### 시나리오 2: Hint 요청 (최적화)

```
[사용자가 GameActivity에서 Hint 버튼 클릭]
        ↓
[createDetector() 호출] ⚡
        ↓
[new UnsolvableDetector(viewModel.getEngine())]
        ↓
[detector.check(viewModel.getState())] (7ms)
        ↓
    unsolvable?
        ↓ Yes
    [Toast: "해결 불가능"] → 종료
    [Detector 폐기 (GC)]
        ↓ No
[Detector 폐기 (GC)]
        ↓
[createSolverFromSettings() 호출]
        ↓
[SharedPreferences에서 "solver_type" 읽기 → "ASTAR"]
        ↓
[new AStarSolver(viewModel.getEngine())]
        ↓
[solver.findBestMove(viewModel.getState())] (10초)
        ↓
[최선의 Move 반환]
        ↓
[Toast로 힌트 표시]
        ↓
[Solver 폐기 (GC)]
```

### 시나리오 3: 게임 시작 시 자동 검사 ✅

```
[GameActivity.startNewGame(seed, rules)]
        ↓
[viewModel.startGame(seed, rules)]  // 게임 초기화
        ↓
[checkInherentlyUnsolvable() 호출]  ⭐ 자동
        ↓
[createDetector() → UnsolvableDetector(engine)]
        ↓
[detector.checkInherentlyUnsolvable()] (7ms)
        ↓
    Inherently Unsolvable?
        ↓ Yes
    [inherentStatusEmoji = 🔴 "Inherently Unsolvable"]
        ↓ No
    [inherentStatusEmoji = 🟢 "Inherently Solvable"]
        ↓
[Detector 폐기 (GC)]
        ↓
[UI 업데이트 - 이모지 표시]
        ↓
[게임 진행]
```

## 설계 원칙

### ✅ 적용된 패턴

1. **MVVM (Model-View-ViewModel)**
   - View: GameActivity, RulesActivity
   - ViewModel: GameViewModel
   - Model: GameEngine, GameState

2. **Strategy Pattern**
   - Solver 인터페이스
   - BFSSolver, AStarSolver 구현체
   - 런타임에 전략 선택 가능

3. **Separation of Concerns**
   - GameViewModel: 게임 로직과 상태 관리
   - GameActivity: UI 이벤트 및 Solver 관리
   - RulesActivity: 설정 관리
   - Solver: 게임 분석 (보조 기능)

4. **Dependency Inversion**
   - 고수준 모듈이 저수준 모듈에 의존하지 않음
   - Solver 인터페이스로 추상화

### 🎯 설계 결정 근거

#### Q: 왜 GameViewModel이 Solver를 소유하지 않는가?

**이유**:
1. **메모리 효율**: Hint/Auto를 사용하지 않는 사용자도 Solver 생성 안 함
2. **관심사 분리**: Solver는 보조 기능, 핵심 게임 로직과 무관
3. **즉시 반영**: 설정 변경 즉시 다음 실행에 반영 (ViewModel 재생성 불필요)

#### Q: 왜 GameViewModel이 UnsolvableDetector도 소유하지 않는가?

**이유** (Solver와 동일):
1. **Stateless**: UnsolvableDetector는 상태를 저장하지 않음
2. **히스토리는 GameEngine이 관리**: UnsolvableDetector는 단지 조회만 함
3. **사용 빈도**: 실제로는 매 이동마다 검사하지 않음 (필요 시에만)
4. **메모리 효율**: 대부분의 사용자는 Unsolvable 검사를 사용하지 않음

**Solver vs UnsolvableDetector 비교**:
| 특성 | Solver | UnsolvableDetector |
|------|--------|-------------------|
| 실행 시점 | Hint/Auto 버튼 **(수동)** | 게임 시작 시 **(자동)** |
| 실행 빈도 | 드물게 (사용자 요청 시) | 게임당 1회 (필수) |
| 실행 비용 | 높음 (10초) | 낮음 (7ms) |
| 목적 | 힌트/자동 플레이 제공 | UI 상태 표시 |
| 필수 여부 | ❌ 선택적 | ✅ 필수 |
| 상태 유지 | 불필요 (stateless) | 불필요 (stateless) |
| 소유자 | Activity (임시) | Activity (임시) |

**실제 사용 패턴**:
- UnsolvableDetector: `startNewGame()` 내에서 **자동 호출**
- Solver: 사용자가 버튼 클릭할 때만 **수동 호출**
- 둘 다 필요할 때 생성하고 즉시 폐기 (stateless)

#### Q: 왜 getEngine()을 노출하는가?

**이유**:
1. Solver 생성 시 GameEngine 필요
2. UnsolvableDetector 생성 시 GameEngine 필요
3. GameEngine은 읽기만 하므로 안전
4. 팩토리 메서드로 감싸면 GameViewModel이 Solver를 알게 됨 (관심사 분리 위반)

**주의사항**:
- GameActivity는 `engine.startGame()` 등 직접 호출하면 안 됨
- 반드시 `viewModel.startGame()` 사용
- getEngine()은 **Solver/UnsolvableDetector 생성 목적으로만** 사용

#### Q: 왜 GameActivity가 Solver를 생성하는가?

**이유**:
1. **생명주기**: Activity는 화면 회전 시 재생성 → Solver도 재생성 (문제없음)
2. **일회성**: Solver는 한 번 사용하고 폐기 (상태 없음)
3. **UI 레이어의 책임**: 사용자 선택(설정)에 따라 도구 선택

#### Q: UnsolvableDetector는 언제 검사하는가?

**실제 검사 시점** (필요 시에만):
1. **Solver 실행 전**: 최적화 목적 ⭐ 주요 사용
   - 불필요한 Solver 실행 방지 (10초 → 7ms)
   - Hint/Auto 요청 시 먼저 빠른 검사
   
2. **게임 시작 시**: 선택적
   - 초기 배치의 구조적 문제 조기 발견
   - UI에 경고 표시 가능

3. **상태 표시**: UI 업데이트 시
   - 게임 상태를 표시할 때 필요 시 검사
   - 매 이동마다는 아님

**성능 특성**:
- Detector 생성/사용/폐기: 7ms
- Solver 실행 방지: -10초 절약
- **결론**: 필요할 때만 생성해도 충분히 효율적

## 파일 구조

```
app/src/main/
├── java/us/jyni/game/klondike/
│   ├── ui/
│   │   ├── GameViewModel.kt          # UnsolvableDetector 소유, getEngine() 제공
│   │   └── screens/
│   │       ├── GameActivity.kt       # Solver 생성 및 사용
│   │       └── RulesActivity.kt      # Solver 설정 저장
│   │
│   ├── solver/
│   │   ├── Solver.kt                 # 인터페이스
│   │   ├── SolverType.kt             # enum (BFS, ASTAR)
│   │   ├── BFSSolver.kt              # 구현체 1
│   │   ├── AStarSolver.kt            # 구현체 2
│   │   ├── UnsolvableDetector.kt     # ⭐ Unsolvable 검사기
│   │   └── UnsolvableReason.kt       # Unsolvable 이유 sealed class
│   │
│   └── engine/
│       └── GameEngine.kt             # 게임 로직 + 상태 히스토리
│
└── res/
    ├── layout/
    │   └── activity_rules.xml        # Solver 선택 UI
    │
    └── values/
        └── strings.xml               # Solver 관련 문자열

app/src/test/
└── java/us/jyni/game/klondike/
    └── solver/
        ├── UnsolvableDetectorTest.kt      # Unsolvable 검사 테스트
        └── StateCycleDetectionTest.kt     # State Cycle 검출 테스트
```

## 주요 API

### RulesActivity

```kotlin
class RulesActivity : AppCompatActivity() {
    private lateinit var solverRadioGroup: RadioGroup
    
    // 현재 설정된 Solver 로드
    private fun loadCurrentSolver()
    
    // 선택한 Solver 저장
    private fun saveSolver()
}
```

### GameActivity

```kotlin
class GameActivity : AppCompatActivity() {
    // 설정에서 Solver 생성
    private fun createSolverFromSettings(): Solver
}
```

### GameViewModel

```kotlin
class GameViewModel : ViewModel() {
    // GameEngine 접근자 (읽기 전용)
    fun getEngine(): GameEngine
    
    // 게임 상태
    fun getState(): GameState
    
    // 게임 조작
    fun startGame(seed: ULong, rules: Ruleset)
    fun move*(...): Boolean
    fun undo()
    // ...
}
```

**참고**: `checkUnsolvable()` 같은 메서드가 있을 수 있으나, 권장 방식은 GameActivity에서 직접 UnsolvableDetector를 생성하여 사용하는 것입니다.

### Solver (Interface)

```kotlin
interface Solver {
    // 전체 게임 해결
    fun solve(initialState: GameState): SolverResult
    
    // 다음 최선의 수 찾기 (힌트용)
    fun findBestMove(state: GameState): Move?
}
```

### UnsolvableDetector ⭐

```kotlin
class UnsolvableDetector(private val engine: GameEngine) {
    /**
     * 게임의 Inherent 속성 검사
     * - 초기 배치의 구조적 문제
     * - Seed + Rules가 동일하면 결과 동일
     */
    fun checkInherentlyUnsolvable(state: GameState): UnsolvableReason?
    
    /**
     * 현재 플레이 상태 검사
     * - Dead End, State Cycle 검출
     * - 매 이동마다 결과가 달라질 수 있음
     */
    fun checkUnwinnableState(state: GameState): UnsolvableReason?
    
    /**
     * 통합 검사 (Legacy 호환)
     * - Inherently + Unwinnable 순차 검사
     */
    fun check(state: GameState): UnsolvableReason?
    
    /**
     * State Cycle 검사
     * - GameEngine 히스토리 활용
     */
    fun checkStateCycle(): UnsolvableReason?
    
    /**
     * 디버그 정보 포함 검사
     * - 검사 과정 로그 반환
     */
    fun checkInherentlyUnsolvableWithDebug(state: GameState): Pair<UnsolvableReason?, String>
}
```

### UnsolvableReason (Sealed Class)

```kotlin
sealed class UnsolvableReason(val reason: String) {
    // === Inherently Unsolvable ===
    data class NPileIrretrievable(
        val n: Int,                    // 관련된 pile 개수
        val piles: List<Int>,          // pile 인덱스들
        val reason: String
    ) : UnsolvableReason(reason)
    
    data class KingIrretrievable(
        val pileIndex: Int,            // King이 있는 pile
        val blockedCards: List<Card>,  // 차단된 카드들
        val reason: String
    ) : UnsolvableReason(reason)
    
    // === Unwinnable State ===
    data class DeadEnd(
        val reason: String
    ) : UnsolvableReason(reason)
    
    data class StateCycle(
        val reason: String
    ) : UnsolvableReason(reason)
}
```

## 테스트 고려사항

### 단위 테스트

1. **RulesActivity**
   - Solver 설정 저장 검증
   - RadioButton 선택 → SharedPreferences 업데이트

2. **GameActivity**
   - createSolverFromSettings() 로직 검증
   - BFS/A* 올바른 인스턴스 생성 확인

3. **GameViewModel**
   - getEngine() 반환값 검증
   - UnsolvableDetector 생명주기 확인

4. **UnsolvableDetector** ⭐
   - **Inherently Unsolvable 검사**
     - N-Pile Irretrievable 패턴 감지
     - King Irretrievable 패턴 감지
     - 정상 게임은 null 반환
   
   - **Unwinnable State 검사**
     - Dead End 상황 감지
     - State Cycle 감지
     - 정상 진행 시 null 반환
   
   - **Regression Tests**
     - 실제 게임 코드로 검증
     - False positive 방지 테스트

**테스트 파일**:
- `UnsolvableDetectorTest.kt`: 기본 패턴 검사
- `StateCycleDetectionTest.kt`: State Cycle 전용 테스트

### 통합 테스트

1. **Solver + UnsolvableDetector 통합**
   - Unsolvable 게임에서 Solver 실패 확인
   - Solvable 게임에서 Solver 성공 확인

2. **설정 변경**
   - 설정 변경 후 Hint/Auto 실행 시 올바른 Solver 사용
   - 화면 회전 후에도 설정 유지
   - 기본값(BFS) 동작 확인

3. **게임 진행 흐름**
   - 게임 시작 → Inherently Unsolvable 감지 → UI 업데이트
   - 이동 → Dead End → UI 업데이트
   - 이동 → State Cycle → UI 업데이트

### 성능 테스트

1. **UnsolvableDetector 성능**
   - checkInherentlyUnsolvable: <7ms
   - checkUnwinnableState: <1ms
   - 매 이동 후 검사 오버헤드 측정

2. **Solver 실행 최적화**
   - Unsolvable 조기 발견으로 Solver 실행 방지
   - 10초 Solver vs 7ms Detector 비교

### 엣지 케이스

1. **UnsolvableDetector**
   - 모든 카드가 face-up인 경우
   - Foundation에 일부 카드가 있는 경우
   - Stock/Waste에 필요 카드가 있는 경우
   - 여러 패턴이 동시에 발생하는 경우

2. **State Cycle**
   - 짧은 사이클 (2-3 상태)
   - 긴 사이클 (10+ 상태)
   - Undo 후 재진행 시 히스토리 처리

## 관련 문서

- [SOLVER_DESIGN.md](SOLVER_DESIGN.md) - Solver 알고리즘 및 UnsolvableDetector 상세 설계
- [SOLVER_USAGE.md](SOLVER_USAGE.md) - Solver 사용법
- [TEST_GUIDE.md](TEST_GUIDE.md) - 테스트 가이드

## 변경 이력

- 2026-02-23: UnsolvableDetector 완전 분리 및 실행 시점 명확화
  - GameViewModel에서 UnsolvableDetector 완전 제거
  - GameActivity에서 필요 시 생성하도록 변경 (Solver와 동일)
  - **실행 시점 차이 강조**: UnsolvableDetector (자동), Solver (수동)
  - 게임 시작 시 자동 검사로 UI 상태 이모지 표시
  - 레이어 및 컴포넌트 다이어그램 업데이트
  - Inherently Unsolvable vs Unwinnable State 구분 설명
  - GameEngine과의 상태 히스토리 공유 메커니즘 설명
  - 데이터 흐름 시나리오 업데이트
  - API 문서 및 테스트 가이드 확장

- 2026-02-22: 초기 설계 및 구현
  - Solver 선택 UI 추가 (RulesActivity)
  - GameActivity에서 Solver 동적 생성
  - GameViewModel에서 Solver 완전 분리
  - SharedPreferences 기반 설정 저장
