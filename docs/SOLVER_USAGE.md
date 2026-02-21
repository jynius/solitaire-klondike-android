# Solver 사용 가이드

## Strategy 패턴을 통한 Solver 선택

Klondike Solitaire는 Strategy 패턴을 사용하여 다양한 Solver 알고리즘을 지원합니다.

## 사용 가능한 Solver

### 1. BFSSolver (기본)
- **알고리즘**: Breadth-First Search
- **장점**: 최적 경로 보장, 안정적
- **단점**: 메모리 사용량 많음, 상대적으로 느림
- **제약**: MAX_STATES=10,000, TIMEOUT=5초

### 2. AStarSolver
- **알고리즘**: A* with Heuristic
- **장점**: 빠른 탐색, 더 많은 상태 탐색 가능
- **단점**: 휴리스틱 설계에 따라 성능 차이
- **제약**: MAX_STATES=100,000, TIMEOUT=5초

## 사용 방법

### 기본 사용 (BFS)

```kotlin
val viewModel = GameViewModel()  // 기본값: SolverType.BFS
```

### A* 사용

```kotlin
val viewModel = GameViewModel(solverType = SolverType.ASTAR)
```

### 현재 Solver 타입 확인

```kotlin
val currentType = viewModel.getSolverType()
println("현재 Solver: $currentType")  // BFS 또는 ASTAR
```

## 코드 예제

### Hint 기능

```kotlin
// Solver 타입에 관계없이 동일하게 사용
val hint = viewModel.findHint()
if (hint != null) {
    println("추천 이동: $hint")
} else {
    println("힌트를 찾을 수 없습니다")
}
```

### Auto Play

```kotlin
solverScope.launch {
    val result = viewModel.solve()
    
    when (result) {
        is SolverResult.Success -> {
            // 순차 실행
            for (move in result.moves) {
                delay(300)
                viewModel.applyMove(move)
            }
        }
        is SolverResult.Timeout -> {
            println("타임아웃: 게임이 너무 복잡합니다")
        }
        is SolverResult.TooComplex -> {
            println("상태 수 초과")
        }
        else -> {
            println("솔루션을 찾을 수 없습니다")
        }
    }
}
```

## 새로운 Solver 추가 방법

### 1. Solver 인터페이스 구현

```kotlin
class MyCustomSolver(private val engine: GameEngine) : Solver {
    override fun solve(initialState: GameState): SolverResult {
        // 커스텀 알고리즘 구현
    }
    
    override fun findBestMove(state: GameState): Move? {
        // 힌트 로직 구현
    }
}
```

### 2. SolverType enum 추가

```kotlin
enum class SolverType {
    BFS,
    ASTAR,
    CUSTOM  // ← 추가
}
```

### 3. GameViewModel에서 사용

```kotlin
private val solver: Solver = when (solverType) {
    SolverType.BFS -> BFSSolver(engine)
    SolverType.ASTAR -> AStarSolver(engine)
    SolverType.CUSTOM -> MyCustomSolver(engine)  // ← 추가
}
```

## 성능 비교

| Solver | 평균 탐색 시간 | 평균 탐색 상태 수 | 메모리 사용 |
|--------|--------------|----------------|-----------|
| BFS    | ~2초         | ~5,000         | 높음      |
| A*     | ~1초         | ~3,000         | 중간      |

*실제 성능은 게임 복잡도에 따라 다를 수 있습니다*

## 권장사항

- **일반 사용**: BFSSolver (안정적, 최적 경로)
- **복잡한 게임**: AStarSolver (빠름, 더 많은 상태 탐색)
- **커스텀 로직**: Solver 인터페이스 구현

## 참고 문서

- [SOLVER_DESIGN.md](docs/SOLVER_DESIGN.md) - 상세 설계 문서
- [SOLVER_STRUCTURE_ANALYSIS.md](SOLVER_STRUCTURE_ANALYSIS.md) - 구조 분석
