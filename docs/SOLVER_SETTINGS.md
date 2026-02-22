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
│  │  - unsolvableDetector: UnsolvableDetector    │           │
│  │  + getEngine(): GameEngine  (read-only)      │           │
│  │  + startGame(), move*(), undo(), ...         │           │
│  └──────────────────────────────────────────────┘           │
│                                                              │
│  Note: Solver를 소유하지 않음 (메모리 효율)                    │
└─────────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                   Domain Layer                               │
│  ┌─────────────┐    ┌──────────────────────────┐           │
│  │ GameEngine  │    │  <<interface>> Solver    │           │
│  │             │    ├──────────────────────────┤           │
│  │ (게임 로직)  │    │ + solve(state)           │           │
│  └─────────────┘    │ + findBestMove(state)    │           │
│         △           └──────────────────────────┘           │
│         │                    △                              │
│         │           ┌────────┴────────┐                    │
│         │           │                 │                     │
│    (used by)  ┌──────────┐     ┌──────────┐               │
│         │     │BFSSolver │     │AStarSolver│               │
│         │     └──────────┘     └──────────┘               │
│         │           │                 │                     │
│         └───────────┴─────────────────┘                    │
│                (requires GameEngine)                        │
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

### 2. Solver 생성 (GameActivity)

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
```

**사용 예시**:
```kotlin
// Hint 버튼
findViewById<ImageButton>(R.id.hint_button).setOnClickListener {
    solverScope.launch {
        val solver = createSolverFromSettings()  // 설정 읽고 Solver 생성
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

### 시나리오 2: Hint 요청

```
[사용자가 GameActivity에서 Hint 버튼 클릭]
        ↓
[createSolverFromSettings() 호출]
        ↓
[SharedPreferences에서 "solver_type" 읽기 → "ASTAR"]
        ↓
[viewModel.getEngine()으로 GameEngine 참조 획득]
        ↓
[new AStarSolver(engine) 생성]
        ↓
[solver.findBestMove(viewModel.getState()) 호출]
        ↓
[Solver가 GameEngine의 상태를 분석]
        ↓
[최선의 Move 반환]
        ↓
[Toast로 힌트 표시]
        ↓
[Solver 인스턴스 폐기 (GC)]
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

#### Q: 왜 getEngine()을 노출하는가?

**이유**:
1. Solver 생성 시 GameEngine 필요
2. GameEngine은 읽기만 하므로 안전
3. 팩토리 메서드로 감싸면 GameViewModel이 Solver를 알게 됨 (관심사 분리 위반)

**주의사항**:
- GameActivity는 `engine.startGame()` 등 직접 호출하면 안 됨
- 반드시 `viewModel.startGame()` 사용
- getEngine()은 **Solver 생성 목적으로만** 사용

#### Q: 왜 GameActivity가 Solver를 생성하는가?

**이유**:
1. **생명주기**: Activity는 화면 회전 시 재생성 → Solver도 재생성 (문제없음)
2. **일회성**: Solver는 한 번 사용하고 폐기 (상태 없음)
3. **UI 레이어의 책임**: 사용자 선택(설정)에 따라 도구 선택

## 파일 구조

```
app/src/main/
├── java/us/jyni/game/klondike/
│   ├── ui/
│   │   ├── GameViewModel.kt          # Solver 없음, getEngine() 제공
│   │   └── screens/
│   │       ├── GameActivity.kt       # Solver 생성 및 사용
│   │       └── RulesActivity.kt      # Solver 설정 저장
│   │
│   ├── solver/
│   │   ├── Solver.kt                 # 인터페이스
│   │   ├── SolverType.kt             # enum (BFS, ASTAR)
│   │   ├── BFSSolver.kt              # 구현체 1
│   │   └── AStarSolver.kt            # 구현체 2
│   │
│   └── engine/
│       └── GameEngine.kt             # 게임 로직
│
└── res/
    ├── layout/
    │   └── activity_rules.xml        # Solver 선택 UI
    │
    └── values/
        └── strings.xml               # Solver 관련 문자열
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

### Solver (Interface)

```kotlin
interface Solver {
    // 전체 게임 해결
    fun solve(initialState: GameState): SolverResult
    
    // 다음 최선의 수 찾기 (힌트용)
    fun findBestMove(state: GameState): Move?
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
   - Solver 관련 코드 없음 확인

### 통합 테스트

1. 설정 변경 후 Hint/Auto 실행 시 올바른 Solver 사용
2. 화면 회전 후에도 설정 유지
3. 기본값(BFS) 동작 확인

## 관련 문서

- [SOLVER_DESIGN.md](SOLVER_DESIGN.md) - Solver 알고리즘 설계
- [SOLVER_USAGE.md](SOLVER_USAGE.md) - Solver 사용법

## 변경 이력

- 2026-02-22: 초기 설계 및 구현
  - Solver 선택 UI 추가 (RulesActivity)
  - GameActivity에서 Solver 동적 생성
  - GameViewModel에서 Solver 완전 분리
  - SharedPreferences 기반 설정 저장
