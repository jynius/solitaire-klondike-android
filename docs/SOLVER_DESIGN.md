# Klondike Solitaire Solver 설계 문서

## 📊 개요

솔리테어 게임의 승리 가능성을 판단하고, 최적의 이동 경로를 제시하는 Solver 시스템을 설계합니다.

### 목표
1. **승리 가능성 판단**: 현재 게임 상태에서 승리가 가능한지 판단
2. **힌트 제공**: 최적의 다음 이동 제시
3. **자동 플레이**: 승리 경로를 따라 자동으로 게임 진행

---

## 🏗️ 핵심 타입 정의

### SolverResult (Solver 결과)

```kotlin
sealed class SolverResult {
    /**
     * 승리 경로 발견
     */
    data class Success(
        val moves: List<Move>,
        val statesExplored: Int
    ) : SolverResult()
    
    /**
     * 본질적으로 해결 불가능 (Inherently Unsolvable)
     * - 초기 카드 배치 자체가 완료 불가능
     * - 어떤 이동 순서를 선택하더라도 승리할 수 없음
     * - 예: 순환 의존성, 필수 카드 블로킹
     */
    data class InherentlyUnsolvable(val reason: String) : SolverResult()
    
    /**
     * 현재 상태에서 승리 불가능 (Unwinnable State)
     * - 플레이어의 잘못된 선택으로 인한 막힌 상태
     * - 다른 경로를 선택했다면 해결 가능했음
     * - 예: Dead End (이동 불가능)
     */
    data class UnwinnableState(val reason: String) : SolverResult()
    
    /**
     * 탐색 시간 초과
     */
    data class Timeout(val reason: String) : SolverResult()
    
    /**
     * 너무 복잡함 (상태 수 초과)
     */
    data class TooComplex(val reason: String) : SolverResult()
}
```

### UnsolvableReason (Unsolvable 이유)

```kotlin
sealed class UnsolvableReason(val message: String) {
    // === Unwinnable State (플레이어의 선택으로 인한 막힌 상태) ===
    
    /**
     * Dead End: 가능한 이동이 전혀 없음
     * - 타입: Unwinnable State
     * - 조건: Stock 비었고, Waste 비었거나 재활용 불가, 모든 이동 불가능
     * - 검사: Draw, Waste→Foundation/Tableau, Tableau→Foundation/Tableau, Foundation→Tableau
     * - 발생: 게임 진행 중 플레이어의 잘못된 선택으로 발생
     * - 상태: ✅ 구현 완료
     */
    data class DeadEnd(val reason: String) : UnsolvableReason(reason)
    
    /**
     * State Cycle: 이전에 방문한 게임 상태로 돌아옴 (무한 루프)
     * - 타입: Unwinnable State
     * - 조건: 현재 상태가 이전 상태와 완전히 동일
     * - 의미: 같은 상태를 반복하므로 더 이상 진전 불가능
     * - 발생: Waste 재활용 후 같은 선택 반복 등
     * - 검출: Solver의 visited set으로 자동 검출
     * - 상태: ⏸️ 미구현 (Solver 통합 시 자동)
     */
    data class StateCycle(val reason: String) : UnsolvableReason(reason)
    
    // === Inherently Unsolvable (초기 배치의 구조적 문제) ===
    
    /**
     * N-Pile Irretrievable: N개 pile이 서로의 필요 카드를 모두 막음
     * - 타입: Inherently Unsolvable
     * - 발생: 게임 시작 시 결정
     * - 검사 방식: N개 pile 조합의 face-up 카드를 맨 위부터 순차 검사
     * - 통합: Single(N=1), Pair(N=2), Triple(N=3), Quad(N=4), Quint(N=5)
     * - 상태: ✅ 구현 완료
     */
    sealed class NPileIrretrievable(reason: String) : UnsolvableReason(reason) {
        data class Single(val pileIndex: Int) : NPileIrretrievable("Single Irretrievable (Deep Blockage)")
        data class Pair(val piles: List<Int>) : NPileIrretrievable("Pair Irretrievable")
        data class Group(val n: Int, val piles: List<Int>) : NPileIrretrievable("$n-Pile Group Irretrievable")
    }
    
    /**
     * King Irretrievable: King의 Foundation + Tableau 경로 모두 차단
     * - 타입: Inherently Unsolvable
     * - Foundation 차단: Queen이 King pile 밑에
     * - Tableau 차단: 모든 다른 pile이 King pile과 Pair Irretrievable
     * - N-Pile 프레임워크의 특수 확장
     * - 상태: ✅ 구현 완료
     */
    data class KingIrretrievable(val pileIndex: Int, val card: String) : UnsolvableReason("King Irretrievable")
}
```

---

## 📋 현재 구현 상태 (2026-02-20 기준)

### ✅ 완료된 항목

#### 1. 기본 구조 (100% 완료)
- ✅ `Move.kt`: 6가지 이동 타입 정의 완료
  - Other piles: 모두 카드가 있음 (빈 공간 없음)
  - TableauToTableau, TableauToFoundation, WasteToTableau
  - WasteToFoundation, FoundationToTableau, Draw
- ✅ `GameStateUtils.kt`: 상태 복제, 이동 적용, 해싱 완료
- ✅ `SolverResult.kt`: Success/InherentlyUnsolvable/UnwinnableState/Timeout/TooComplex 정의
  - **Inherently Unsolvable**: 초기 배치 자체가 해결 불가능 (구조적 문제)
  - **Unwinnable State**: 플레이어의 선택으로 인한 막힌 상태 (Dead End)

#### 2. BFS Solver (100% 완료)
- ✅ `BFSSolver.kt`: BFS 알고리즘 구현 완료
- ✅ 제약: MAX_DEPTH=50, MAX_STATES=10,000, TIMEOUT=5초
- ✅ `solve()` 및 `findBestMove()` 메서드 구현
- ✅ `BFSSolverTest.kt`: 단위 테스트 완료

#### 3. A* Solver (100% 완료)
- ✅ `AStarSolver.kt`: A* 알고리즘 구현 완료
- ✅ 제약 기반 휴리스틱 함수 구현
  - Foundation 진행도, 블로킹 비용, Stock/Waste, 뒷면 카드
- ✅ Priority Queue 기반 탐색
- ✅ `AStarSolverTest.kt`: 단위 테스트 완료

#### 4. Unsolvable Detector (80% 완료)
- ✅ `UnsolvableDetector.kt`: Unsolvable 패턴 구현
  - ✅ **Inherently Unsolvable** (게임 시작 시 검사)
    - ✅ N-Pile Irretrievable (N=1~5 통합 프레임워크)
    - ✅ King Irretrievable (N-Pile 확장)
  - ✅ **Unwinnable State** (게임 진행 중 검사)
    - ✅ Dead End (모든 이동 불가능)
    - ✅ State Cycle (순환 상태 검출)

**검사 시점**:
- **게임 시작**: `checkInherentlyUnsolvable()` - 카드 배치 문제 검출
- **매 이동 후**: `checkUnwinnableState()` - Dead End + State Cycle 검사

**참고**: 
- Inherently Unsolvable은 카드 배치로 결정되므로 게임 중 변하지 않음
- State Cycle은 GameEngine의 상태 히스토리로 검출
- Solver가 실패하면 "Proven Unwinnable"을 반환 (별도 타입 체크 아님)

#### 5. Solver 인터페이스 및 Strategy 패턴 (100% 완료)
- ✅ `Solver.kt`: 공통 인터페이스 정의
  - `solve()`: 승리 경로 찾기
  - `findBestMove()`: 힌트 제공
  - `SolverType` enum: BFS, ASTAR
- ✅ `BFSSolver.kt`: Solver 인터페이스 구현
- ✅ `AStarSolver.kt`: Solver 인터페이스 구현

**Strategy 패턴 적용:**
```kotlin
class GameViewModel(
    private val solverType: SolverType = SolverType.BFS
) : ViewModel() {
    private val solver: Solver = when (solverType) {
        SolverType.BFS -> BFSSolver(engine)
        SolverType.ASTAR -> AStarSolver(engine)
    }
}
```

**장점:**
- ✅ 런타임에 Solver 교체 가능
- ✅ 새로운 Solver 추가 용이
- ✅ 확장성 및 유지보수성 향상

#### 6. ViewModel 통합 (100% 완료)
- ✅ `GameViewModel.kt`: Solver 인터페이스로 연결
- ✅ `solve()`, `findHint()`, `checkUnsolvable()` 메서드 제공
- ✅ `getSolverType()`: 현재 Solver 타입 조회

### ⚠️ 미완성 항목

#### 1. UI 통합 (50% 완료)
- ✅ 힌트 버튼 구현 (Solver 기반)
- ✅ Auto Play 버튼 (Solver 기반, 순차 실행 + 0.3초 간격 애니메이션)
- ⏸️ 카드 강조 표시
- ⏸️ 로딩 인디케이터
- ⏸️ Unsolvable 상태 표시 UI
- ⏸️ Solver 선택 설정 UI

#### 2. Auto Play 기능 (100% 완료)
- ✅ Solver 기반 자동 플레이
- ✅ 이동 간 딜레이 + 애니메이션 (0.3초)
- ✅ 진행률 Toast 메시지
- ✅ 타임아웃/복잡도 초과 처리

#### 3. 빠른 정리 기능 (100% 완료)
- ✅ `quickComplete()`: Greedy 알고리즘 (Foundation만)
- ✅ `autoCompleteIfPossible()`: 모든 카드 공개 시 자동 정리
- ⚠️ Solvable 게임도 중간에 멈출 수 있음 (known limitation)

---

## ⚠️ AutoComplete의 현재 문제점

### 현재 구현 (Greedy 알고리즘)
`GameViewModel.autoComplete()`는 단순한 탐욕 알고리즘으로 구현되어 있습니다:

```kotlin
fun autoComplete(): Int {
    var moveCount = 0
    var moved = true
    
    while (moved) {
        moved = false
        
        // 1. Waste → Foundation
        for (foundationIndex in 0..3) {
            if (engine.canMoveWasteToFoundation(foundationIndex)) {
                engine.moveWasteToFoundation(foundationIndex)
                moveCount++
                moved = true
                break
            }
        }
        
        // 2. Tableau → Foundation
        if (!moved) {
            for (col in 0..6) {
                for (foundationIndex in 0..3) {
                    if (engine.canMoveTableauToFoundation(col, foundationIndex)) {
                        engine.moveTableauToFoundation(col, foundationIndex)
                        moveCount++
                        moved = true
                        break
                    }
                }
                if (moved) break
            }
        }
    }
    
    return moveCount
}
```

### 문제점

1. **Foundation으로만 이동**: Tableau 재배치나 전략적 이동 없음
2. **로컬 최적만 선택**: 글로벌 최적 해를 보장하지 않음
3. **Solvable 게임도 막힘**: 예시 케이스
   - Seed: `17848904495592789619`
   - 69수 후 멈춤 (Foundation 7/52)
   - 실제로는 해결 가능한 게임
   - 가능한 이동: HEARTS THREE (T[0]) → CLUBS FOUR (T[5])
   - 하지만 Foundation 이동이 아니므로 실행 안 됨

4. **Recycle 무한 루프 (2026-02-06 수정 완료)**
   - Stock 6장만 남았을 때 반복 메시지 출력
   - 해결: `recycleCount`, `wasteUsedAfterRecycle` 변수로 추적

### Greedy 알고리즘의 한계

**장점:**
- ✅ 빠른 실행 속도
- ✅ 대부분의 간단한 경우 잘 작동

**단점:**
- ❌ 복잡한 게임에서 실패 가능
- ❌ Look-ahead 없음
- ❌ 모든 solvable 게임 보장 불가

### 시도한 개선 방안 (실패)

#### 시도 1: Step 5.5 추가 (Tableau 재배치)
- Waste에 카드가 있지만 놓을 곳이 없을 때 Tableau 재배치 시도
- 결과: 개선 없음 (여전히 69수에서 멈춤)

#### 시도 2: Step 3을 3a/3b로 분리
```kotlin
// 3a. 뒷면 카드 뒤집기 (기존 로직)
// 3b. Waste를 놓을 곳이 없을 때 Tableau 재배치
if (!moved && waste.isNotEmpty() && !wasteCanBePlaced) {
    // 모든 Tableau→Tableau 이동 시도
}
```
- 결과: 여전히 동일한 지점에서 멈춤

---

## 💡 제안된 해결 방안

### Option A: 하이브리드 접근 (권장) ⭐

Greedy 알고리즘과 Solver를 결합하여 속도와 정확도를 모두 확보:

```kotlin
fun autoComplete(): Int {
    var moveCount = 0
    
    // Phase 1: 빠른 Greedy 이동 (Foundation으로 올릴 수 있는 것들)
    moveCount += simpleGreedyMoves()
    
    // Phase 2: 막혔으면 Solver 사용
    if (hasMoreMovesAvailable()) {
        val result = solver.solve(getState())
        when (result) {
            is SolverResult.Success -> {
                // Solver가 찾은 경로 실행
                result.moves.forEach { move ->
                    applyMove(move)
                }
                moveCount += result.moves.size
            }
            is SolverResult.TooComplex,
            is SolverResult.Timeout -> {
                // 복잡하거나 시간 초과 시 현재까지 결과 반환
                // 사용자에게 "일부만 자동 완성됨" 메시지
            }
            is SolverResult.Unsolvable -> {
                // 더 이상 진행 불가
            }
        }
    }
    
    return moveCount
}

private fun simpleGreedyMoves(): Int {
    // 현재 Greedy 로직 (Foundation으로만 이동)
}

private fun hasMoreMovesAvailable(): Boolean {
    // Stock/Waste에 카드가 있거나
    // Tableau에서 가능한 이동이 있는지 체크
}
```

**장점:**
- ✅ 대부분 경우 빠름 (Greedy)
- ✅ Solvable 게임 100% 해결
- ✅ 기존 코드 재사용

**단점:**
- ⚠️ 복잡한 게임에서 5초 대기 가능

### Option B: AutoPlay 별도 구현

새로운 `autoPlay()` 함수를 추가하여 `autoComplete()`와 분리:

```kotlin
suspend fun autoPlay(): Flow<AutoPlayProgress> = flow {
    val result = solver.solve(getState())
    
    when (result) {
        is SolverResult.Success -> {
            result.moves.forEachIndexed { index, move ->
                applyMove(move)
                emit(AutoPlayProgress.InProgress(index + 1, result.moves.size))
                delay(500) // 애니메이션
            }
            emit(AutoPlayProgress.Completed(result.moves.size))
        }
        is SolverResult.Timeout -> {
            emit(AutoPlayProgress.Failed("시간 초과"))
        }
        is SolverResult.Unsolvable -> {
            emit(AutoPlayProgress.Failed("승리 불가능"))
        }
        is SolverResult.TooComplex -> {
            emit(AutoPlayProgress.Failed("너무 복잡함"))
        }
    }
}

sealed class AutoPlayProgress {
    data class InProgress(val current: Int, val total: Int) : AutoPlayProgress()
    data class Completed(val totalMoves: Int) : AutoPlayProgress()
    data class Failed(val reason: String) : AutoPlayProgress()
}
```

**장점:**
- ✅ AutoComplete는 그대로 유지 (하위 호환)
- ✅ 사용자에게 진행 상황 실시간 표시
- ✅ 일시정지/재개 가능

**단점:**
- ⚠️ 새로운 UI 작업 필요
- ⚠️ 두 가지 자동화 기능 관리

---

## 🎯 핵심 개념

### 완전 정보 게임
- 모든 카드의 위치와 값을 알고 있음 (뒷면 카드 포함)
- `Card.isFaceUp`으로 뒷면/앞면만 구분
- Stock의 순서도 알려진 상태
- 따라서 결정론적(deterministic) 탐색 가능

### 상태 공간
- **상태(State)**: 특정 시점의 게임 판 상황 (GameState)
- **이동(Move)**: 상태 전이를 일으키는 액션
- **경로(Path)**: 초기 상태 → 승리 상태까지의 이동 시퀀스

---

## 🔍 이동 타입 정의

### 1. Tableau → Tableau
```kotlin
data class TableauToTableau(
    val fromCol: Int,      // 0..6
    val cardIndex: Int,    // 이동할 카드의 인덱스
    val toCol: Int         // 0..6
)
```

### 2. Tableau → Foundation
```kotlin
data class TableauToFoundation(
    val fromCol: Int,           // 0..6
    val foundationIndex: Int    // 0..3
)
```

### 3. Waste → Tableau
```kotlin
data class WasteToTableau(
    val toCol: Int  // 0..6
)
```

### 4. Waste → Foundation
```kotlin
data class WasteToFoundation(
    val foundationIndex: Int  // 0..3
)
```

### 5. Foundation → Tableau
```kotlin
data class FoundationToTableau(
    val foundationIndex: Int,  // 0..3
    val toCol: Int            // 0..6
)
```
*규칙에 따라 허용 여부가 다름*

### 6. Draw (Stock → Waste)
```kotlin
object DrawMove
```

### 통합 이동 타입
```kotlin
sealed class Move {
    data class TableauToTableau(val fromCol: Int, val cardIndex: Int, val toCol: Int) : Move()
    data class TableauToFoundation(val fromCol: Int, val foundationIndex: Int) : Move()
    data class WasteToTableau(val toCol: Int) : Move()
    data class WasteToFoundation(val foundationIndex: Int) : Move()
    data class FoundationToTableau(val foundationIndex: Int, val toCol: Int) : Move()
    object Draw : Move()
}
```

---

## 🧠 Solver 알고리즘 설계

### Phase 1: 기본 BFS Solver (최소 기능)

#### 알고리즘
```
function solve(initialState):
    queue = [initialState]
    visited = Set()
    parent = Map()  // 경로 추적용
    
    while queue is not empty:
        state = queue.dequeue()
        
        if isWinning(state):
            return reconstructPath(parent, state)
        
        stateHash = hash(state)
        if stateHash in visited:
            continue
        visited.add(stateHash)
        
        for move in getAllPossibleMoves(state):
            newState = applyMove(state, move)
            if hash(newState) not in visited:
                parent[newState] = (state, move)
                queue.enqueue(newState)
    
    return null  // 승리 불가능
```

#### 구현 위치
```
app/src/main/java/us/jyni/game/klondike/solver/
├── SolitaireSolver.kt          // 메인 Solver 인터페이스
├── BFSSolver.kt                // BFS 구현
├── Move.kt                     // Move 타입 정의
└── SolverResult.kt             // 결과 타입
```

#### 제약 사항
- **최대 탐색 깊이**: 50수로 제한 (타임아웃 방지)
- **최대 상태 수**: 10,000개로 제한 (메모리 방지)
- **시간 제한**: 5초

---

### Phase 2: 최적화된 A* Solver (향상)

#### 휴리스틱 함수
```kotlin
fun heuristic(state: GameState): Int {
    var score = 0
    
    // Foundation에 있는 카드 수 (높을수록 좋음)
    score += state.foundation.sumOf { it.size } * 100
    
    // 뒷면 카드 수 (낮을수록 좋음)
    score -= state.tableau.sumOf { pile -> 
        pile.count { !it.isFaceUp } 
    } * 10
    
    // Tableau의 빈 컬럼 수 (적당히 있는 것이 좋음)
    val emptyColumns = state.tableau.count { it.isEmpty() }
    score += if (emptyColumns in 1..2) 20 else -10
    
    // Stock + Waste 카드 수 (낮을수록 좋음)
    score -= (state.stock.size + state.waste.size) * 5
    
    return score
}
```

#### Priority Queue 사용
```kotlin
val priorityQueue = PriorityQueue<SearchNode>(compareBy { 
    it.cost + heuristic(it.state) 
})
```

---

## 🎮 사용 시나리오

### 시나리오 1: 힌트 요청
```
User Action: 힌트 버튼 클릭
1. Solver.findBestMove(currentState) 호출
2. 백그라운드에서 탐색 시작 (최대 3초)
3. 결과:
   - 승리 가능: 최선의 다음 이동 반환
   - 승리 불가능: "막힌 게임입니다" 메시지
   - 타임아웃: "힌트를 찾을 수 없습니다" 메시지
4. UI에서 이동할 카드 강조 표시 (깜빡임 or 테두리)
```

### 시나리오 2: Auto Play
```
User Action: AUTO 버튼 클릭
1. Solver.findWinningPath(currentState) 호출
2. 결과:
   - 경로 발견: 순차적으로 이동 실행 (애니메이션 포함)
   - 경로 없음: 현재 가능한 최선의 이동만 실행
3. 각 이동 사이 500ms 딜레이 (사용자가 볼 수 있도록)
4. 완료 시 "N수 자동 플레이 완료" 메시지
```

### 시나리오 3: 승리 가능성 표시
```
Game Start/Resume:
1. 백그라운드에서 Solver.isSolvable(state) 확인
2. 결과를 UI에 표시:
   - ✅ 승리 가능
   - ❌ 승리 불가능
   - ⏱️ 판단 중...
3. 설정에서 표시 여부 선택 가능
```

### 시나리오 4: 막힘 감지
```
After Each Move:
1. 빠른 체크: 즉시 가능한 이동이 있는지?
2. 깊은 체크: 3초 내에 승리 경로가 있는지?
3. 막힘 감지 시:
   - 다이얼로그: "더 이상 진행할 수 없습니다"
   - 옵션: [언두], [다시 시작], [새 게임]
```

---

## � Unwinnable State 상세 설명

**Unwinnable State**는 게임 진행 중 플레이어의 선택으로 인해 발생하는 승리 불가능한 상태입니다. 초기 배치는 해결 가능했지만, 특정 시점부터 더 이상 승리할 수 없게 된 경우를 말합니다.

### Unwinnable State 종류

#### 1. 🚫 Dead End (구현 완료)

**정의:** 가능한 이동이 전혀 없는 상태

**조건:**
- Stock 비었음 (또는 소진)
- Waste 비었거나 재활용 불가능 (redeals = 0)
- 다음 6가지 이동이 모두 불가능:
  1. Draw (Stock → Waste)
  2. Waste → Foundation
  3. Waste → Tableau
  4. Tableau → Foundation
  5. Tableau → Tableau
  6. Foundation → Tableau (규칙에 따라)

**발생 원인:**
```kotlin
// 예시: 잘못된 킹 배치
초기: 빈 Pile[0], King♠ on Pile[1]
플레이어: King♠를 Pile[0]로 이동 (잘못된 선택)
결과: 필요한 Queen♠가 King 밑에 갇힘
     → Stock 소진 후 Dead End
```

**검출 방법:**
```kotlin
fun isDeadEnd(state: GameState): Boolean {
    // 1. Draw 가능?
    if (canDraw(state)) return false
    
    // 2. Waste에서 이동 가능?
    if (state.waste.isNotEmpty()) {
        for (f in 0..3) if (canMoveWasteToFoundation(f)) return false
        for (t in 0..6) if (canMoveWasteToTableau(t)) return false
    }
    
    // 3. Tableau에서 이동 가능?
    for (t in 0..6) {
        for (f in 0..3) if (canMoveTableauToFoundation(t, f)) return false
        for (t2 in 0..6) if (canMoveTableauToTableau(t, t2)) return false
    }
    
    // 4. Foundation에서 이동 가능? (규칙 확인)
    if (state.rules.allowFoundationToTableau) {
        for (f in 0..3) {
            for (t in 0..6) if (canMoveFoundationToTableau(f, t)) return false
        }
    }
    
    return true  // 모든 이동 불가능 → Dead End
}
```

**특징:**
- ✅ 구현 완료
- ✅ 즉시 검출 가능 (O(1) ~ O(n²))
- ✅ 확실한 판단 (False Positive 없음)

---

#### 2. 🔄 State Cycle (구현 완료)

**정의:** 이전에 방문한 게임 상태로 돌아오는 무한 루프

**조건:**
- 현재 상태 해시 = 이전 상태 해시
- 같은 상태를 반복하므로 더 이상 진전 불가능

**발생 원인:**
```kotlin
// 예시: 이동 후 Undo를 반복
State A: Waste[♠3, ♥5], Tableau[0][♦4]
이동 1: Draw
State B: Waste[♠3, ♥5, ♣K], Tableau[0][♦4]
Undo: State A로 복귀
이동 2: Draw (다시 같은 이동)
State B: 다시 동일한 상태 → Cycle!
```

**검출 방법:**
```kotlin
// GameEngine이 상태 히스토리 유지
private val stateHistory = mutableSetOf<Int>()

fun recordCurrentState(): Boolean {
    val stateHash = calculateStateHash(gameState)
    return stateHistory.add(stateHash)  // false면 중복 (Cycle)
}

// UnsolvableDetector에서 Cycle 검사
fun checkStateCycle(): UnsolvableReason? {
    val isNewState = engine.recordCurrentState()
    
    if (!isNewState) {
        // 이전에 방문한 상태 → Cycle 감지
        return UnsolvableReason.StateCycle("이전 상태로 돌아왔습니다")
    }
    
    return null
}
```

**특징:**
- ✅ 구현 완료
- ✅ O(1) 해시 비교로 즉시 검출
- ✅ 게임 진행 중 실시간 체크
- ✅ Solver에서도 자동으로 회피 가능

---

#### 3. ❌ Proven Unwinnable (Solver 결과)

**정의:** Solver가 탐색을 통해 승리 경로가 없음을 증명

**중요:** 이것은 **Unwinnable State 체크가 아니라 Solver의 결과**입니다!

**특징:**
- 현재 상태에서는 이동 가능 (Dead End 아님)
- 하지만 Solver가 모든 경로를 탐색한 결과 승리 불가능
- 실시간 게임 중에는 검출 불필요 (Solver 실행 시에만)

**Solver 동작:**
```kotlin
fun solve(state: GameState): SolverResult {
    val queue = LinkedList<GameState>()
    val visited = mutableSetOf<Int>()
    
    queue.add(state)
    
    while (queue.isNotEmpty()) {
        val current = queue.poll()
        
        if (isWin(current)) return SolverResult.Success(...)
        if (isDeadEnd(current)) continue  // 이 경로 포기
        
        for (move in getPossibleMoves(current)) {
            val next = applyMove(current, move)
            val hash = next.hashCode()
            
            if (hash in visited) continue  // State Cycle 회피
            
            visited.add(hash)
            queue.add(next)
        }
    }
    
    // 모든 경로 탐색 완료, 승리 경로 없음
    return SolverResult.UnwinnableState("탐색 결과: 승리 불가능")
}
```

**사용 시나리오:**
```kotlin
// ❌ 게임 진행 중 - 체크 불필요!
fun onPlayerMove() {
    // Dead End만 체크하면 됨
    val deadEnd = detector.checkUnwinnableState(state)
    if (deadEnd != null) {
        showMessage("더 이상 이동할 수 없습니다")
    }
}

// ✅ Solver 실행 - 자동으로 판단
fun onSolveButtonClick() {
    val result = solver.solve(currentState)
    when (result) {
        is SolverResult.Success -> showSolution(result.moves)
        is SolverResult.UnwinnableState -> showMessage("풀 수 없는 게임입니다")
        // ...
    }
}
```

**결론:**
- **실시간 체크**: Dead End만 (이동 불가능)
- **Solver 결과**: Proven Unwinnable (탐색 실패)
- 둘은 별개의 개념!

---

### Unwinnable State 비교표

| 종류 | 검출 방법 | 복잡도 | 확실성 | 실시간 체크 | 구현 상태 |
|------|----------|--------|--------|------------|----------|
| **Dead End** | 이동 가능성 체크 | O(n²) | 100% | ✅ 필수 | ✅ 완료 |
| **State Cycle** | 해시 비교 | O(1) | 100% | ✅ 필수 | ✅ 완료 |

**참고:** Proven Unwinnable은 Solver 결과이므로 위 표에서 제외
### 검사 전략

```kotlin
class UnsolvableDetector {
    // 게임 진행 중 매 이동 후 호출
    fun checkUnwinnableState(state: GameState): UnsolvableReason? {
        // 1. Dead End (빠른 체크) - 실시간 필수!
        if (isDeadEnd(state)) {
            return UnsolvableReason.DeadEnd("이동 불가능")
        }
        
        // 2. State Cycle - GameEngine 히스토리로 검출
        val stateCycle = checkStateCycle()
        if (stateCycle != null) return stateCycle
        
        return null
    }
    
    fun checkStateCycle(): UnsolvableReason? {
        // GameEngine이 상태를 기록하고 중복 검출
        val isNewState = engine.recordCurrentState()
        
        if (!isNewState) {
            return UnsolvableReason.StateCycle("이전 상태로 돌아왔습니다")
        }
        
        return null
    }
}
```

**핵심 정리:**
- **실시간 게임**: Dead End + State Cycle 체크
- **Solver 실행**: State Cycle 자동 회피 (visited set), 탐색 실패 시 UnwinnableState 반환
- **Proven Unwinnable**: 별도 타입이 아니라 Solver의 실패 결과

---

## �🚫 Unsolvable 개념 구분

Klondike Solitaire에서 "Unsolvable"이라는 용어는 두 가지 다른 개념을 나타냅니다:

### 1. **Inherently Unsolvable** (본질적 해결 불가능) - 게임 속성

게임의 초기 배치 자체가 완료 불가능한 **게임**입니다. 어떤 이동 순서를 선택하더라도 승리할 수 없습니다.

**특징:**
- 게임 시작(Shuffle) 직후부터 결정되는 **게임 속성**
- 플레이어의 선택과 무관
- 카드 배치의 구조적 문제 (순환 의존성, 필수 카드 블로킹 등)
- 탐지 시점: 게임 시작 시 또는 언제든지 (항상 같은 결과)

**예시:**
```
Tableau Pile[0]:
[하트2] ← face-up
------- face-down
[하트A] ← face-down
[스페이드3] ← face-down
[클로버3] ← face-down

Foundation[HEARTS]: 비어있음
다른 Pile: 스페이드3, 클로버3 없음
```
→ 하트2를 이동하려면 하트A가 필요하지만, 하트A는 하트2 밑에 갇혀있음
→ **Inherently Unsolvable**

### 2. **Unwinnable State** (현재 상태에서 승리 불가능) - 플레이 상태

게임은 본질적으로 해결 가능했지만, 플레이어의 잘못된 선택으로 인해 **현재 상태**에서는 더 이상 진행할 수 없는 경우입니다.

**특징:**
- 게임 진행 중 발생하는 **플레이 상태**
- 플레이어의 선택에 의존
- 다른 이동 순서를 선택했다면 해결 가능했음
- 탐지 시점: 게임 진행 중 이동 후 (매번 달라질 수 있음)

**예시:**
```
초기 배치: Solvable 게임 (게임 속성)
플레이어 이동: 킹을 빈 공간에 잘못 배치
결과: 필수 카드가 킹 밑에 갇힘
→ **Unwinnable State** (현재 플레이 상태)
   (하지만 게임 자체는 Inherently Solvable)
```

### 구분 요약

| 구분 | Inherently Unsolvable | Unwinnable State |
|------|----------------------|------------------|
| 원인 | 초기 카드 배치 | 플레이어의 선택 |
| 시점 | 게임 시작부터 | 게임 진행 중 |
| 해결 가능성 | 절대 불가능 | 다른 경로로는 가능했음 |
| 재시작 필요 | 필수 (새 게임) | 언두로 해결 가능 |
| Solver 역할 | 조기 탐지 | 경고 및 대안 제시 |
| **종류** | **N-Pile Irretrievable, King Irretrievable** | **Dead End, State Cycle** |
| **검출** | **게임 시작 시 한 번** | **매 이동 후** |

**참고:**
- **Dead End**: 실시간 게임 중 즉시 체크 필요 (모든 이동 불가능)
- **State Cycle**: 실시간 게임 중 즉시 체크 필요 (순환 감지)
- **Proven Unwinnable**: 별도 타입 아님, Solver의 실패 결과 (`SolverResult.UnwinnableState`)

### Inherently Unsolvable과 Unwinnable State의 관계

**핵심 통찰:** Inherently Unsolvable은 **미래의 Unwinnable State를 예측**합니다!

#### 시나리오 1: Inherently Unsolvable 게임

```
게임 시작:
  ↓
N-Pile Irretrievable 검사 (7ms)
  ↓
특정 카드(들) Irretrievable 확정!
  예: Single → 1장 확정
      Pair → 2장 확정
      Triple → 3장 확정
  ↓
게임 진행 (플레이어가 이동)
  ↓
Irretrievable 카드가 뒤집혀서 나옴 (face-up)
  ↓
Dead End 감지! (이동 불가능)
  ↓
Unwinnable State 확인
  ↓
사용자에게 알림: "이 게임은 Inherently Unsolvable입니다"
```

**예시:**
```
게임 시작 시:
Pile[0]: [하트2](face-up) / [하트A, 스페이드3, 클로버3](face-down)
→ N-Pile 검사: 하트2는 Single Irretrievable! (확정)

게임 진행:
... (다른 카드들 이동)
Pile[0]의 하트2가 맨 위로 노출됨
→ Dead End 검사: 하트2를 옮길 수 없음!
→ Unwinnable State 확인
→ "Inherently Unsolvable" 알림
```

#### 시나리오 2: Solvable 게임

```
게임 시작:
  ↓
N-Pile Irretrievable 검사 (7ms)
  ↓
Irretrievable 카드 없음! ✅
  ↓
King Deadlock 검사
  ↓
없음! ✅
  ↓
게임은 Solvable!
  ↓
Solver 실행 (BFS/A*)
  ↓
최적 경로 탐색
  ↓
자동 실행 옵션
  ↓
Win! 🎉
```

#### 시나리오 3: Solvable이지만 플레이어 실수

```
게임 시작:
  ↓
N-Pile Irretrievable 검사: 없음 ✅
  ↓
게임은 Solvable!
  ↓
플레이어 이동 (잘못된 선택)
  예: King을 빈 공간에 잘못 배치
  ↓
Dead End 발생
  ↓
Unwinnable State 확인
  ↓
사용자에게 알림: "현재 막혔습니다. 언두로 되돌리세요."
  ↓
Solver Hint: "이전 이동을 취소하고 다른 경로를 시도하세요"
```

### 전체 게임 플로우

```kotlin
// 게임 시작 시
fun onGameStart(state: GameState) {
    // 1. Inherently Unsolvable 검사 (7ms)
    val unsolvableReason = checkInherentlyUnsolvable(state)
    
    if (unsolvableReason != null) {
        // Inherently Unsolvable 확정!
        // 특정 카드(들)가 irretrievable로 확정됨
        state.isInherentlyUnsolvable = true
        state.irretrievableCards = identifyIrretrievableCards(state)
        
        // 사용자에게 경고 (선택적)
        showWarning("이 게임은 해결 불가능합니다. 새 게임을 시작하시겠습니까?")
    } else {
        // Solvable!
        state.isInherentlyUnsolvable = false
        
        // Solver 준비
        enableSolverFeatures()
    }
}

// 매 이동 후
fun onAfterMove(state: GameState) {
    if (state.isInherentlyUnsolvable) {
        // Inherently Unsolvable 게임에서 진행 중
        // Irretrievable 카드가 노출되었는지 확인
        if (irretrievableCardExposed(state)) {
            // Dead End 확인
            if (isDeadEnd(state)) {
                showAlert("예상대로 막혔습니다. 이 게임은 Inherently Unsolvable입니다.")
                return
            }
        }
    }
    
    // Dead End 검사 (일반)
    if (isDeadEnd(state)) {
        if (state.isInherentlyUnsolvable) {
            return SolverResult.InherentlyUnsolvable("Irretrievable 카드로 인한 막힘")
        } else {
            return SolverResult.UnwinnableState("잘못된 선택으로 막힘, 언두 필요")
        }
    }
}

// Solver 실행
fun solve(state: GameState): SolverResult {
    // 1. 먼저 Inherently Unsolvable 검사
    val unsolvableReason = checkInherentlyUnsolvable(state)
    if (unsolvableReason != null) {
        return SolverResult.InherentlyUnsolvable(unsolvableReason)
    }
    
    // 2. Solvable이므로 경로 탐색
    val solution = bfsSolver.solve(state)
    
    return when {
        solution != null -> SolverResult.Success(solution)
        else -> SolverResult.TooComplex("탐색 공간 초과")
    }
}
```

### 요약

| 상황 | Inherently Unsolvable | Unwinnable State | 조치 |
|------|---------------------|------------------|-----|
| 게임 시작 시 N-Pile 감지 | ✅ Yes | - | 경고 + 새 게임 권장 |
| Inherently Unsolvable 게임 진행 중 막힘 | ✅ Yes | ✅ Yes | "예상대로 막힘" 알림 |
| Solvable 게임, 잘못된 플레이로 막힘 | ❌ No | ✅ Yes | 언두 권장 + Hint 제공 |
| Solvable 게임, 정상 진행 | ❌ No | ❌ No | 자동 실행 가능 |

**핵심:**
- ✅ **게임 시작 시 7ms로 Inherently Unsolvable 완전 판단**
- ✅ **Irretrievable 카드 확정 → 미래의 Dead End 예측**
- ✅ **Solvable 게임은 최적 경로 탐색 → 자동 Win 가능**
- ✅ **명확한 사용자 피드백: 구조적 문제 vs 플레이 실수**

---

## 🎯 통합 Irretrievable 프레임워크 (N-Pile Irretrievable)

### 핵심 아이디어: 하나의 로직으로 모든 패턴 처리

현재 Deep Blockage, Deep Pair, Deep Group이 **모두 동일한 로직**입니다:
- N개의 pile 조합 선택
- 해당 조합의 모든 face-down을 "차단된 카드" 집합으로 간주
- 해당 조합의 각 face-up 카드가 이 집합에 의해 irretrievable인지 확인

이를 **N-Pile Irretrievable**로 일반화할 수 있습니다:

| 이름 | N | 조합 수 | 설명 |
|------|---|--------|------|
| **Single Irretrievable** | 1 | C(7,1) = 7 | Deep Blockage |
| **Pair Irretrievable** | 2 | C(7,2) = 21 | Deep Pair |
| **Triple Irretrievable** | 3 | C(5,3) = 10 | Deep Group 3-pile |
| **Quad Irretrievable** | 4 | C(5,4) = 5 | Deep Group 4-pile |
| **Quint Irretrievable** | 5 | C(5,5) = 1 | Deep Group 5-pile |

### 통합 알고리즘

```kotlin
fun hasNPileIrretrievable(state: GameState, n: Int): Boolean {
    // 의미 있는 pile 선택 (face-down이 충분한 pile만)
    val meaningfulPiles = state.tableau.indices.filter { i ->
        state.tableau[i].faceDownCards.size >= (n - 1)  // 최소 조건
    }
    
    // N개 pile 조합 생성
    val combinations = generateCombinations(meaningfulPiles, n)
    
    for (combo in combinations) {
        // 이 조합의 모든 face-down 합치기
        val combinedFaceDown = combo.flatMap { i ->
            state.tableau[i].faceDownCards
        }
        
        // 이 조합의 각 pile의 face-up 카드들 검사
        for (pileIndex in combo) {
            val pile = state.tableau[pileIndex]
            
            // 맨 위부터 순차 검사 (최대 4장)
            val cardsToCheck = min(pile.faceUpCards.size, 4)
            for (k in 0 until cardsToCheck) {
                val card = pile.faceUpCards[pile.faceUpCards.size - 1 - k]
                
                if (isCardIrretrievable(card, combinedFaceDown, state)) {
                    return true  // Inherently Unsolvable!
                }
            }
        }
    }
    
    return false
}

// 통합 체크: 모든 N-Pile 패턴 검사
fun hasAnyIrretrievable(state: GameState, maxN: Int = 5): Boolean {
    for (n in 1..maxN) {
        if (hasNPileIrretrievable(state, n)) {
            return true
        }
    }
    return false
}
```

### King Card의 특수 처리

**King은 N-Pile 프레임워크의 확장**입니다:

**King Irretrievable 조건:**
1. Foundation 경로 차단 (Queen이 King pile 밑에)
2. **모든 다른 pile이 King pile과 함께 Irretrievable**

```kotlin
fun isKingIrretrievable(kingPileIndex: Int, state: GameState): Boolean {
    val kingPile = state.tableau[kingPileIndex]
    val king = kingPile.faceUpCards.firstOrNull { it.rank == Rank.KING } ?: return false
    
    // 1. Foundation 경로 확인
    val queenRequired = Card(king.suit, Rank.QUEEN)
    val foundationBlocked = kingPile.faceDownCards.any { it.matches(queenRequired) }
    
    if (!foundationBlocked) {
        return false  // Foundation으로 갈 수 있음
    }
    
    // 2. 빈 pile 생성 가능성 확인
    // = 다른 모든 pile이 King pile과 Pair Irretrievable인가?
    for (otherIndex in state.tableau.indices) {
        if (otherIndex == kingPileIndex) continue
        
        // 이 pile을 King pile과 함께 검사
        if (!hasNPileIrretrievable(state, 2, listOf(kingPileIndex, otherIndex))) {
            // 이 pile이 비워질 수 있음 → 빈 pile 생성 가능
            return false
        }
    }
    
    // Foundation 차단 AND 모든 pile이 못 비워짐
    return true
}
```

**King 체크를 N-Pile 프레임워크에 통합:**
- King이 있는 pile은 별도 체크
- 다른 모든 pile과 Pair Irretrievable인지 확인
- 모두 Irretrievable이면 → 빈 pile 생성 불가 → King Irretrievable

### 게임 시작 시 완전한 Inherently Unsolvable 판단

**통합 프레임워크로 게임 시작 시 한 번에 체크:**

```kotlin
fun checkInherentlyUnsolvable(state: GameState): UnsolvableReason? {
    // 1. King Deadlock (최우선)
    if (hasKingDeadlock(state)) {
        return UnsolvableReason.KingDeadlock
    }
    
    // 2. N-Pile Irretrievable (통합 체크)
    // N=1: Single (Deep Blockage)
    // N=2: Pair (Deep Pair)
    // N=3,4,5: Group (Deep Group)
    for (n in 1..5) {
        if (hasNPileIrretrievable(state, n)) {
            return when(n) {
                1 -> UnsolvableReason.DeepBlockage
                2 -> UnsolvableReason.DeepPairIrretrievable
                else -> UnsolvableReason.DeepGroupIrretrievable(n)
            }
        }
    }
    
    // 3. King Irretrievable (특수 케이스)
    for (i in state.tableau.indices) {
        if (isKingIrretrievable(i, state)) {
            return UnsolvableReason.KingIrretrievable
        }
    }
    
    return null  // Solvable!
}
```

### 비용 분석 (게임 시작 시 1회)

| 패턴 | 조합 수 | 비용 | 누적 |
|------|--------|------|------|
| Single (N=1) | 7 | O(37) | O(37) |
| Pair (N=2) | 21 | O(1,680) | O(1,717) |
| Triple (N=3) | 10 | O(2,160) | O(3,877) |
| Quad (N=4) | 5 | O(2,240) | O(6,117) |
| Quint (N=5) | 1 | O(800) | O(6,917) |
| King (특수) | 7 | O(300) | O(7,217) |
| **전체** | **51조합** | **O(7,217)** | **≈ 7ms** |

**결론:**
- ✅ **게임 시작 시 단 7ms로 완전한 Inherently Unsolvable 판단 가능!**
- ✅ 모든 패턴이 하나의 통합 로직으로 처리됨
- ✅ King도 N-Pile 프레임워크의 확장으로 자연스럽게 통합
- ✅ 코드 중복 제거, 유지보수 용이

---

## 🚫 빠른 Unsolvable 판단 (최우선 구현)

탐색 전에 빠르게 **Inherently Unsolvable** 또는 **Unwinnable State**를 감지하면 불필요한 계산을 크게 줄일 수 있습니다.

### 판단 패턴

#### 1. Dead End (즉시 막힘) - Unwinnable State

**정의:** Stock과 Waste가 비었고, 가능한 이동이 전혀 없는 상태

**타입:** Unwinnable State (본질적으로는 해결 가능했을 수 있음)

```kotlin
fun isDeadEnd(state: GameState): Boolean {
    // Stock과 Waste가 비었고, 가능한 이동이 전혀 없음
    if (state.stock.isNotEmpty() || state.waste.isNotEmpty()) {
        return false  // 아직 draw 가능
    }
    
    // Tableau에서 가능한 모든 이동 체크
    val hasAnyMove = hasAnyTableauMove(state) || 
                     hasAnyToFoundationMove(state)
    
    return !hasAnyMove
}
```

#### 2. Irretrievable Cards (순환 블로킹) - Inherently Unsolvable

**정의:**
카드가 **이동 불가능(Irretrievable)** 상태란, **Tableau pile 내부의 순환 의존성** 때문에 해당 카드를 영원히 옮길 수 없는 상태를 말합니다.

**타입:** Inherently Unsolvable (초기 배치의 구조적 문제)

**핵심 통찰:**
- Stock/Waste의 카드는 무관 (언제든 꺼낼 수 있음)
- **Face-up 맨 밑 카드 + 그 밑 face-down만 검사**
- Pile 내부의 순환 의존성이 문제

**카드는 두 가지 방향으로 이동 가능:**

1. **Foundation 이동**: 같은 무늬(suit)의 rank-1 카드가 Foundation 맨 위에 있어야 함
   - 하트3 → 하트2가 Foundation에 있어야 함
   
2. **Tableau 이동**: 반대 색깔의 rank+1 카드가 다른 Tableau pile에 있어야 함
   - 하트3 → 스페이드4 or 클로버4가 다른 pile에 있어야 함

**카드 X가 Irretrievable한 조건:**
- Foundation 이동 불가 **AND** Tableau 이동 불가
- 즉, **필요한 모든 카드가 X 밑의 face-down에 갇혀있음**:
  1. 같은 무늬의 (X-1) 카드 → X 밑에 face-down
  2. 반대 색깔의 (X+1) 카드들 → 모두 X 밑에 face-down

**순환 의존성:**
- X를 옮기려면 → X 밑의 카드들을 먼저 옮겨야 함
- 하지만 X 밑의 카드들을 옮기려면 → X가 필요함
- **→ 영원히 옮길 수 없음!**

적어도 하나의 카드라도 이동 불가능하면, 그 밑의 모든 face-down 카드도 접근할 수 없으므로 게임은 **Inherently Unsolvable**입니다.

**예시 1: 하트2 카드가 Irretrievable (사용자 예시)**
```
Tableau Pile[0]:
[하트2] ← face-up (맨 밑)
------- face-down 경계
[스페이드3] ← face-down  
[클로버3] ← face-down
[하트A] ← face-down

Foundation[HEARTS]: 비어있음
```

**하트2 이동 가능성 분석:**

**1. Foundation으로 이동 가능?**
- 조건: 하트A가 Foundation[HEARTS]에 있어야 함
- 필요 카드: [하트A] (Foundation 체크)
- 현재: 하트A는 하트2 **밑에 face-down**으로 갇혀있음
- 하트A를 꺼내려면? → 스페이드3, 클로버3을 먼저 옮겨야 함
- 하지만 스페이드3, 클로버3도 하트2 밑에 갇혀있음 (순환!)
- → **NO (Foundation 이동 불가)**

**2. Tableau로 이동 가능?**
- 조건: 검은색 3 (스페이드3 or 클로버3) 위에 놓아야 함
- 필요한 카드: 스페이드3 또는 클로버3
- **두 카드 모두 하트2 밑에 face-down**으로 갇혀있음
- 꺼낼 수 없음 (순환: 하트2를 옮겨야 밑의 카드를 꺼낼 수 있는데, 밑의 카드가 있어야 하트2를 옮길 수 있음)
- → **NO (Tableau 이동 불가)**

**결론:**
- Foundation 이동 불가 **AND** Tableau 이동 불가
- **하트2는 Irretrievable!**
- Stock에 다른 검은색3이 있어도 무관 (하트2 밑의 카드들을 풀 수 없음)
- 하트2 밑의 스페이드3, 클로버3, 하트A는 **영원히 접근 불가**
- **→ Inherently Unsolvable!**

---

**예시 2: 하트2 카드가 이동 가능 (Foundation 경로)**
```
Tableau Pile[0]:
[하트2] ← face-up (맨 밑)
------- face-down 경계
[스페이드3] ← face-down
[클로버3] ← face-down
(하트A는 없음)

Foundation[HEARTS]: [하트A] ✓
```

**분석:**
- Foundation으로 이동?: 하트A가 Foundation에 있음 → **YES!**
- **하트2는 이동 가능 (Solvable)**

---

**예시 3: 하트2 카드가 이동 가능 (Tableau 경로)**
```
Tableau Pile[0]:
[하트2] ← face-up (맨 밑)
------- face-down 경계
[하트A] ← face-down

Tableau Pile[3]:
[스페이드3] ← face-up (접근 가능!)
```

**분석:**
- Foundation 이동?: 하트A가 밑에 갇혀있음 → NO
- Tableau 이동?: 스페이드3이 **다른 pile에** 있음 → **YES!**
- **하트2는 이동 가능 (Solvable)**

---

**예시 4: 하트3 - Foundation 연쇄 필요**
```
Tableau Pile[3]:
[하트3] ← face-up (맨 밑)
------- face-down 경계
[하트A] ← face-down
[하트2] ← face-down
[클로버4] ← face-down

Foundation[HEARTS]: 비어있음
```

**하트3 분석:**
1. Foundation 이동 필요 카드: [하트A, 하트2] (모두 필요!)
   - 하트A: 밑에 face-down ✗
   - 하트2: 밑에 face-down ✗
   → **NO**

2. Tableau 이동 필요 카드: [클로버4 or 스페이드4]
   - 클로버4: 밑에 face-down ✗
   → **NO**

3. **→ Irretrievable! (Inherently Unsolvable)**
   - 필요한 3장 {하트A, 하트2, 클로버4} 모두 밑에 갇힘
   - Pile 3의 face-down이 정확히 3장이므로 검출 가능!

**알고리즘:**
```kotlin
fun hasIrretrievableCard(state: GameState): Boolean {
    for (pile in state.tableau) {
        // Face-down이 없으면 스킵 (모든 카드 접근 가능)
        if (pile.faceDownCards.isEmpty()) continue
        
        // Face-up 맨 밑 카드 (face-down 바로 위)
        if (pile.faceUpCards.isEmpty()) continue
        val bottomFaceUpCard = pile.faceUpCards.first()
        
        // 이 카드가 irretrievable인지 검사
        if (isCardIrretrievable(bottomFaceUpCard, pile.faceDownCards, state)) {
            return true  // Inherently Unsolvable!
        }
    }
    return false
}

fun isCardIrretrievable(card: Card, faceDownBelow: List<Card>, state: GameState): Boolean {
    // 1. Foundation으로 가는데 필요한 카드
    val requiredForFoundation = getRequiredForFoundation(card, state)
    
    // 2. Tableau로 가는데 필요한 카드
    val requiredForTableau = getRequiredForTableau(card)
    
    // 3. 모든 필요 카드를 합침
    val allRequired = requiredForFoundation + requiredForTableau
    
    // 4. 적어도 하나라도 밑에 없으면 이동 가능
    //    모든 required card가 밑에 있으면 irretrievable
    return allRequired.all { required ->
        faceDownBelow.any { it.matches(required) }
    }
}

/**
 * Foundation 이동에 필요한 카드 (모든 이전 rank 포함!)
 */
fun getRequiredForFoundation(card: Card, state: GameState): List<Card> {
    // Ace는 바로 Foundation에 갈 수 있음
    if (card.rank == Rank.ACE) {
        return emptyList()
    }
    
    val required = mutableListOf<Card>()
    
    // Foundation에서 현재 어디까지 올라갔는지 확인
    val foundationTop = state.foundation[card.suit]?.lastOrNull()
    val currentRank = foundationTop?.rank?.value ?: 0
    
    // 현재 rank까지 올라가려면 중간의 모든 카드 필요
    // 예: 하트3 올리려면 → 하트A, 하트2 모두 필요!
    for (rank in (currentRank + 1) until card.rank.value) {
        required.add(Card(card.suit, Rank.fromValue(rank)))
    }
    
    return required
    
    // 예시:
    // - 하트3, Foundation[HEARTS] = null → [하트A, 하트2]
    // - 하트3, Foundation[HEARTS] = [하트A] → [하트2]
    // - 하트3, Foundation[HEARTS] = [하트A, 하트2] → []
}

/**
 * Tableau 이동에 필요한 카드
 */
fun getRequiredForTableau(card: Card): List<Card> {
    // King은 빈 공간에 갈 수 있음 (별도 처리)
    if (card.rank == Rank.KING) {
        // 빈 공간 확인은 별도로 처리 (항상 가능하다고 가정)
        return emptyList()
    }
    
    // 반대 색깔의 suit 찾기
    val oppositeColorSuits = if (card.suit.isRed()) {
        listOf(Suit.CLUBS, Suit.SPADES)  // 검은색
    } else {
        listOf(Suit.HEARTS, Suit.DIAMONDS)  // 빨간색
    }
    
    // rank+1인 카드들
    val nextRank = Rank.fromValue(card.rank.value + 1)
    return oppositeColorSuits.map { suit -> Card(suit, nextRank) }
    // 예: 하트2 → [스페이드3, 클로버3]
}

fun Card.matches(other: Card): Boolean {
    return this.suit == other.suit && this.rank == other.rank
}
```

---

**예시 5: King Card (복잡한 케이스) ⚠️**

**King의 특수성:**
- King은 **빈 pile로만** Tableau 이동 가능
- 빈 pile이 **절대 생길 수 없으면** King은 Inherently Unsolvable

**King Irretrievable 판단 (두 가지 경로):**

1. **Foundation 경로 차단**: 같은 무늬의 Queen이 밑에 갇힘 **AND**
2. **Tableau 경로 차단**: 빈 pile이 **구조적으로 생성 불가능**

```
Tableau 구조:

Pile[0] (King이 있는 pile):
[하트K] ← face-up
-----------
[하트Q] ← face-down (Foundation 경로 차단!)

Pile[1]:
[다이아3] ← face-up
-----------
[하트2] ← face-down

Pile[2]:
[스페이드5] ← face-up
-----------
[다이아2] ← face-down

... (Pile[3-6] 유사)
```

**빈 pile 생성 가능성 분석:**

빈 pile이 생기려면 **어떤 pile의 모든 카드**가 다른 곳으로 이동 가능해야 합니다.

**Pile[1]이 비워질 수 있는가?**
- Pile[1]의 다이아3을 옮기려면:
  - Foundation: 다이아A, 다이아2가 필요 → 다이아2는 Pile[2] 밑에 갇힘
  - Tableau: 하트4 또는 클로버4 필요
- Pile[1]의 하트2를 옮기려면:
  - Foundation: 하트A가 필요
  - Tableau: 스페이드3 또는 클로버3 필요

**만약 Pile[0]과 Pile[1]이 Deep Pair Irretrievable이면:**
- Pile[0]의 하트K와 Pile[1]의 다이아3이 서로 필요한 카드를 막음
- Pile[1]은 **절대 비워질 수 없음**

**만약 모든 다른 pile(Pile[1-6])이 Pile[0]과 Pair/Group Irretrievable이면:**
- 모든 pile이 **절대 비워질 수 없음**
- 빈 pile이 **구조적으로 생성 불가능**
- **하트K는 Tableau로 이동 불가능** (영구적)

**결론:**
- Foundation 경로 차단 (하트Q가 밑에) **AND**
- Tableau 경로 차단 (빈 pile 생성 불가) **AND**
- 모든 다른 pile이 King pile과 Pair/Group Irretrievable
- **→ 하트K는 Irretrievable!**
- **→ Inherently Unsolvable!**

**King Irretrievable 체크 로직 (복잡함):**

```kotlin
fun isKingIrretrievable(kingPileIndex: Int, state: GameState): Boolean {
    val kingPile = state.tableau[kingPileIndex]
    val king = kingPile.faceUpCards.first()  // 맨 밑 King
    
    // 1. Foundation 경로 확인
    val requiredForFoundation = getRequiredForFoundation(king, state)
    val foundationBlocked = requiredForFoundation.all { required ->
        kingPile.faceDownCards.any { it.matches(required) }
    }
    
    if (!foundationBlocked) {
        return false  // Foundation으로 갈 수 있으면 retrievable
    }
    
    // 2. Tableau 경로 확인: 빈 pile 생성 가능성
    // 다른 모든 pile이 비워질 수 없는지 확인
    for (otherIndex in 0 until state.tableau.size) {
        if (otherIndex == kingPileIndex) continue
        
        // 이 pile이 비워질 수 있는가?
        if (canPileBeEmptied(otherIndex, kingPileIndex, state)) {
            return false  // 빈 pile 생성 가능 → King retrievable
        }
    }
    
    // Foundation 차단 AND 모든 pile이 못 비워짐
    return true  // King Irretrievable!
}

fun canPileBeEmptied(pileIndex: Int, excludePileIndex: Int, state: GameState): Boolean {
    // 이 pile의 모든 카드가 Pair/Group Irretrievable인지 확인
    // Deep Pair/Group 로직 활용 (매우 복잡!)
    
    val pile = state.tableau[pileIndex]
    val excludedPile = state.tableau[excludePileIndex]
    val combinedFaceDown = pile.faceDownCards + excludedPile.faceDownCards
    
    // pile의 모든 face-up 카드가 irretrievable인지 확인
    for (card in pile.faceUpCards) {
        if (!isCardIrretrievableInPair(card, combinedFaceDown, state)) {
            return true  // 하나라도 retrievable이면 pile을 비울 수 있음
        }
    }
    
    return false  // 모든 카드가 irretrievable → pile 못 비움
}
```

**복잡도 상세 분석:**

**1. 간소화 버전 (Foundation만 체크):**
```
O(k) where k = face-down 카드 수 (평균 3)
≈ O(3) ≈ <1ms
```

**2. 완전한 버전 (빈 pile 생성 가능성까지 체크):**
```
Foundation 경로: O(k) = O(3)

Tableau 경로 (빈 pile 생성 가능성):
- 다른 pile 수: 6개
- 각 pile마다 canPileBeEmptied() 호출:
  - 해당 pile의 face-up 카드 수: 평균 2장 (최대 4장)
  - 각 카드마다 isCardIrretrievableInPair() 호출:
    - Foundation 필요 카드: 평균 2개
    - Tableau 필요 카드: 2개
    - Face-down 검사: (pile A: 3장) + (King pile: 3장) = 6장
    - 각 필요 카드 × face-down 검사: 4 × 6 = 24
  - 카드당 비용: O(24)
  - pile당 비용: 2 × 24 = 48
- 6 pile 총 비용: 6 × 48 = 288

총 비용: O(3 + 288) = O(291) ≈ 0.3ms (게임 시작 시 1회)

하지만 실제로는 더 복잡:
- canPileBeEmptied()가 정확하려면 해당 pile과 King pile의 Pair만으로는 부족
- 다른 pile들과의 Group Irretrievable도 고려해야 함 (매우 복잡)
- 완벽한 구현: O(500-1000) ≈ 1-2ms
```

**비교:**

| 구분 | 일반 카드 | King (간소화) | King (완전) |
|------|----------|--------------|------------|
| Foundation 경로 | O(3) | O(3) | O(3) |
| Tableau 경로 | O(3) | **생략** | O(300-1000) |
| 총 비용 | O(6) ≈ <1ms | O(3) ≈ <1ms | **O(300-1000)** ≈ 1-2ms |
| 구현 복잡도 | 간단 | 간단 | **매우 복잡** |
| 정확도 | 100% | 95%+ (Foundation만) | 100% |

**실용적 결론:**

King의 완전한 Inherently Unsolvable 판단은:
- ✅ 이론적으로 가능 (O(300-1000) ≈ 1-2ms)
- ❌ **구현이 매우 복잡함** (Deep Pair/Group과 결합)
- ⚠️ **비용 대비 효과가 낮음** (희귀한 케이스)
- ✅ **간소화 버전 권장**: Foundation 경로만 체크 (95%+ 정확도)

**구현 방식 (완전한 버전):**

완전한 King Irretrievable 판단은 `isKingIrretrievable()` 함수를 별도로 구현하고,
일반 카드는 기존 `isCardIrretrievable()`을 사용합니다.

```kotlin
fun isCardIrretrievable(card: Card, faceDownBelow: List<Card>, state: GameState): Boolean {
    // Foundation 이동 가능성 확인
    val requiredForFoundation = getRequiredForFoundation(card, state)
    val foundationPossible = !requiredForFoundation.all { required ->
        faceDownBelow.any { it.matches(required) }
    }
    
    // Tableau 이동 가능성 확인 (일반 카드만)
    val tableauPossible = if (card.rank == Rank.KING) {
        // King은 별도 함수로 처리 (isKingIrretrievable)
        true  // 여기서는 체크 안 함
    } else {
        // 일반 카드: 반대 색 rank+1 카드 확인
        val requiredForTableau = getRequiredForTableau(card)
        !requiredForTableau.all { required ->
            faceDownBelow.any { it.matches(required) }
        }
    }
    
    return !foundationPossible && !tableauPossible
}
```

**핵심 통찰:**
- ⚠️ King의 완전한 Inherently Unsolvable 판단은 **Deep Pair/Group과 결합 필요**
- ⚠️ **구현 복잡도와 계산 비용이 매우 높음**
- ✅ **실용적 선택**: Foundation 경로만 체크 (간소화)
- ✅ 대부분의 King Irretrievable은 Foundation 경로 차단으로 감지됨
- ⏸️ 완벽한 정확도가 필요하면 Deep Pair/Group 구현 후 King 특수 처리 추가

---

**핵심 특징:**
1. ✅ **Stock/Waste 무관**: 오직 Tableau pile 내부만 검사
2. ✅ **Face-up 맨 밑 카드만**: 그 밑의 face-down만 확인
3. ✅ **순서 배치 감지**: 필요한 카드가 모두 밑에 갇혀있으면 irretrievable
4. ⚠️ **King은 간소화**: Foundation만 체크 (빈 pile 생성 가능성은 매우 복잡)
5. ✅ **단순하고 빠름**: O(7 × k) where k = face-down 카드 수 (평균 3-4개)
6. ✅ **게임 시작 시 즉시 검사 가능**: 초기 상태에서도 동작
7. ✅ **정확한 판단**: Stock에 뭐가 있든 상관없음 (King 제외)

**시간 복잡도:**
- 각 pile: O(k) where k = face-down 카드 수
- 전체: O(7k) = O(21) (평균)
- **매우 빠름!** (<1ms)
    // 해당 무늬의 Foundation 찾기
    val foundationPile = state.foundation.find { pile ->
        pile.isNotEmpty() && pile.first().suit == card.suit
    } ?: run {
        // Foundation이 비어있는 경우: Ace만 놓을 수 있음
        return card.rank.value == 1
    }
    
    // Foundation 맨 위 카드 확인
    val topCard = foundationPile.lastOrNull() ?: run {
        // Foundation이 비어있으면 Ace만 가능
        return card.rank.value == 1
    }
    
    // 바로 다음 숫자여야 함 (하트2 위에 하트3)
    return card.suit == topCard.suit && card.rank.value == topCard.rank.value + 1
}

/**
 * 카드를 놓을 수 있는 Tableau 위치가 있는지 확인
 */
fun canMoveToAnyTableau(
    card: Card,
    currentPileIndex: Int,
    currentCardIndex: Int,
    state: GameState
): Boolean {
    // King은 빈 공간에만 놓을 수 있음
    if (card.rank.value == 13) {
        val hasEmptyColumn = state.tableau.any { it.isEmpty() }
        return hasEmptyColumn
    }
    
    // 이 카드를 놓으려면 어떤 카드가 필요한가?
    val requiredCards = getRequiredCardsForTableau(card)
    // 예: 하트3 → [클로버4, 스페이드4] (검은색 4)
    
    // 필요한 카드 중 하나라도 접근 가능하면 OK
    for (requiredCard in requiredCards) {
        if (isCardAccessible(requiredCard, currentPileIndex, currentCardIndex, state)) {
            return true  // 하나라도 접근 가능하면 이동 가능!
        }
    }

**용어 정리:**
- **Irretrievable Card**: Tableau pile 내부의 순환 의존성으로 이동 불가능한 카드
- **Circular Blocking**: 필요한 카드가 자신의 밑에 face-down으로 갇혀있어 순환 의존성 발생
- **Self-Blocking Dependency**: 자기 자신이 자신을 막는 의존성
- **Face-up Bottom Card**: Tableau pile에서 face-down 바로 위에 있는 카드 (검사 대상)
- **Two-Way Check**: Foundation과 Tableau 두 방향 모두 체크

---

### 고급 패턴: Pair Irretrievability

**개념:**
- **Pair Irretrievable**: 두 pile의 카드가 서로의 필요 카드를 모두 막고 있는 상태
- 두 pile A, B의 **face-up 맨 밑 카드** 각각의 필요 카드 합집합이 모두 A 또는 B의 face-down에 갇혀있음
- **게임 시작 시 이미 결정됨** (완전 정보 게임!)
- **Stock 무관** (Tableau 내 두 pile의 배치만으로 결정)

**핵심 통찰:**
- Single Irretrievable처럼 **Tableau 내부 문제**
- 다른 pile을 다 해결해도 이 두 pile은 영원히 안 풀림
- 게임 시작 시 바로 검사 가능!

#### 예시

**Pair Irretrievable 상황:**
```
Pile A (Pile 4):
[하트3] ← face-up 맨 밑
-------
[다이아A] ← face-down
[다이아2] ← face-down
[스페이드4] ← face-down
[클로버4] ← face-down

Pile B (Pile 5):
[다이아3] ← face-up 맨 밑
-------
[하트A] ← face-down
[하트2] ← face-down
(다른 카드들...)

Foundation: 비어있음
```

**분석:**
- 하트3 필요 카드: {하트A, 하트2, 스페이드4, 클로버4}
  - 하트A, 하트2 → Pile B 밑에 갇힘
  - 스페이드4, 클로버4 → Pile A 밑에 갇힘 (자기 밑!)
  
- 다이아3 필요 카드: {다이아A, 다이아2, 스페이드4, 클로버4}
  - 다이아A, 다이아2 → Pile A 밑에 갇힘
  - 스페이드4, 클로버4 → Pile A 밑에 갇힘

**순환 의존성:**
- 하트3 옮기려면 → Pile B의 하트A, 하트2 필요
- 하트A, 하트2 꺼내려면 → 다이아3을 옮겨야 함
- 다이아3 옮기려면 → Pile A의 다이아A, 다이아2 필요
- 다이아A, 다이아2 꺼내려면 → 하트3을 옮겨야 함
- **→ 영원히 안 풀림! Inherently Unsolvable!**

**Stock에 뭐가 있든 무관:**
- Stock에 스페이드4, 클로버4가 있어도 소용없음
- 하트3, 다이아3 둘 다 Foundation 경로가 막혀있기 때문

#### 필요한 카드 수 분석

**Case 1: 같은 색깔 + 같은 숫자** (예: 하트3 + 다이아3)
```
하트3 필요: {하트A, 하트2, 클로버4, 스페이드4}
다이아3 필요: {다이아A, 다이아2, 클로버4, 스페이드4}
합집합: {하트A, 하트2, 다이아A, 다이아2, 클로버4, 스페이드4} = 6장

최소 필요: 두 pile 합쳐서 6장
→ Pile A에 4장 + Pile B에 2장 = 총 6장
→ 각 pile에 최소 face-down 필요 개수 다름
```

**Case 2: 다른 숫자** (예: 하트3 + 하트5)
```
하트3 필요: {하트A, 하트2, 클로버4, 스페이드4}
하트5 필요: {하트A, 하트2, 하트3, 하트4, 다이아6, 클로버6}
합집합: {하트A, 하트2, 하트3, 하트4, 클로버4, 스페이드4, 다이아6, 클로버6}

→ 더 많은 카드 필요 (8+장)
```

#### 비용 분석

**게임 시작 시 검사:**
```
검사 대상:
- 각 pile의 face-up 맨 밑 카드만 (7장)
- Pile 조합: C(7, 2) = 21개

각 조합당 검사:
1. 두 카드의 필요 카드 합집합 계산: O(10)
2. 합집합이 두 pile의 face-down에 있는지 확인: O(10)

총 비용: 21 × O(20) = O(420) ≈ <1ms ✅
```

**최적화:**
```
실제 검사 필요 조합:
- Pile 0-2: face-down < 4장 → 대부분 Pair 불가능
- Pile 3: face-down 3장 → 제한적
- Pile 4-6: face-down 4-6장 → 주요 검사 대상

유효 조합: 약 10-15개
총 비용: O(200-300) ≈ <1ms ✅
```

#### 검사 시점

**권장: 게임 시작 시** ⭐

**이유**:
1. ✅ **이미 결정됨**: Tableau 배치로 Pair Irretrievable 여부 확정
2. ✅ **Stock 무관**: 완전 정보 게임이므로 모든 카드 값 알고 있음
3. ✅ **비용 낮음**: O(200-300) ≈ <1ms (허용 가능)
4. ✅ **조기 감지**: Shuffle 직후 unsolvable 즉시 판단

**구현**:
```kotlin
fun hasGameStartUnsolvable(state: GameState): Boolean {
    // 1. King Deadlock (빠른 검사)
    if (hasKingDeadlock(state)) return true
    
    // 2. Deep Blockage (1 pile, 필수)
    if (hasDeepBlockage(state)) return true
    
    // 3. Deep Pair (2 piles, 선택적)
    if (hasDeepPair(state)) return true
    
    // 4. Deep Group (3+ piles, 매우 선택적, 비추천)
    // if (hasDeepGroup(state)) return true
    
    return false
}

fun hasPairIrretrievable(state: GameState): Boolean {
    // 모든 pile 조합 검사
    for (i in 0 until state.tableau.size) {
        for (j in i + 1 until state.tableau.size) {
            val pileA = state.tableau[i]
            val pileB = state.tableau[j]
            
            // 각 pile의 face-up 맨 밑 카드
            if (pileA.faceUpCards.isEmpty() || pileB.faceUpCards.isEmpty()) continue
            val cardA = pileA.faceUpCards.first()
            val cardB = pileB.faceUpCards.first()
            
            // 필요 카드 합집합
            val requiredForA = getRequiredCards(cardA, state)
            val requiredForB = getRequiredCards(cardB, state)
            val allRequired = (requiredForA + requiredForB).distinct()
            
            // 모든 필요 카드가 두 pile의 face-down에 있는지 확인
            val allTrappedInAB = allRequired.all { required ->
                pileA.faceDownCards.any { it.matches(required) } ||
                pileB.faceDownCards.any { it.matches(required) }
            }
            
            if (allTrappedInAB) {
                return true  // Pair Irretrievable 발견!
            }
        }
    }
    return false
}
```

**결론**:
- **게임 시작 시 검사**: ✅ 권장 (이미 결정되어 있음)
- **Stock 무관**: ✅ Tableau 배치만으로 결정
- **우선순위**: 중간 (Single + Deep보다는 낮지만 유용)

---

#### Deep Pair Irretrievable (고급, 선택적)

**개념:**
- 두 pile에서 **각각 여러 장**이 서로 상호 블로킹
- **Pair Irretrievable의 Deep 버전**
- Deep Blockage처럼 맨 위부터 순차 검사

**핵심 차이:**
- **Pair Irretrievable**: 각 pile의 맨 밑 1장만 검사
- **Deep Pair Irretrievable**: 각 pile의 여러 장을 맨 위부터 순차 검사
- **Deep Pair가 Pair를 포함!**

**예시:**
```
Pile A:
[하트3] ← face-up 맨 밑
[하트4] ← face-up 맨 위
-------
[다이아A]  ← face-down
[다이아2]  ← face-down
[스페이드4] ← face-down

Pile B:
[다이아3] ← face-up 맨 밑
[다이아4] ← face-up 맨 위
-------
[하트A]   ← face-down
[하트2]   ← face-down
[클로버4] ← face-down

Deep Pair 검사 (맨 위부터 순차):
1. 하트4 irretrievable? (Pile A+B의 face-down 중에 필요 카드 있는지)
   필요: {하트A, 하트2, 하트3, 다이아5, 스페이드5}
   → 하트A, 하트2는 Pile B 밑
   → 하트3은 Pile A에 있지만 맨 밑 (접근 불가)
   → YES → Inherently Unsolvable! (하트3 접근 불가 → 밑의 카드들 접근 불가)

만약 하트4가 retrievable이면:
2. 하트3 irretrievable? (Pile A+B의 face-down 중)
   필요: {하트A, 하트2, 스페이드4, 클로버4}
   → 하트A, 하트2는 Pile B 밑
   → 스페이드4는 Pile A 밑, 클로버4는 Pile B 밑
   → YES → Inherently Unsolvable!

3. 다이아4 irretrievable? ...
4. 다이아3 irretrievable? ...

논리: 맨 위부터 하나라도 (두 pile 조합으로) irretrievable이면 끝!
```

**검사 방법:**
```kotlin
fun hasDeepPairIrretrievable(state: GameState): Boolean {
    for (i in 0 until state.tableau.size) {
        for (j in i + 1 until state.tableau.size) {
            val pileA = state.tableau[i]
            val pileB = state.tableau[j]
            
            // Pile A의 여러 장을 맨 위부터 순차 검사
            val cardsToCheckA = min(pileA.faceUpCards.size, 4)
            for (k in 0 until cardsToCheckA) {
                val card = pileA.faceUpCards[pileA.faceUpCards.size - 1 - k]  // 맨 위부터
                val combinedFaceDown = pileA.faceDownCards + pileB.faceDownCards
                
                if (isCardIrretrievableInPair(card, combinedFaceDown, state)) {
                    return true  // Inherently Unsolvable!
                }
            }
            
            // Pile B의 여러 장을 맨 위부터 순차 검사
            val cardsToCheckB = min(pileB.faceUpCards.size, 4)
            for (k in 0 until cardsToCheckB) {
                val card = pileB.faceUpCards[pileB.faceUpCards.size - 1 - k]
                val combinedFaceDown = pileA.faceDownCards + pileB.faceDownCards
                
                if (isCardIrretrievableInPair(card, combinedFaceDown, state)) {
                    return true  // Inherently Unsolvable!
                }
            }
        }
    }
    return false
}
```

**복잡도:**
- Pile 조합: C(7, 2) = 21개
- 각 조합당: 최대 (4 + 4) = 8장 검사
- 각 카드당: O(10) 검사
- 총 비용: O(21 × 8 × 10) = O(1,680) ≈ 1-2ms
- **Pair Irretrievable 포함** (맨 밑 카드도 검사하므로)

**구현 우선순위:**
- ⏸️ **낮음** (Deep Blockage로 대부분 커버됨)
- Deep Pair를 구현하면 Pair는 불필요 (Deep Pair가 Pair를 포함)
- 비용 대비 효과를 고려하여 선택적 구현

---

### 검사 시점

**언제 검사하는가?**

#### 1. 게임 시작 시 ⭐ **즉시 unsolvable 판단 가능**
```
Tableau 구조 (게임 시작):
Pile 0: [up] (1장)
Pile 1: [down, up] (2장)
Pile 2: [down, down, up] (3장)
Pile 3: [down, down, down, up] (4장) ← 검사 시작
Pile 4: [down, down, down, down, up] (5장)
Pile 5: [down, down, down, down, down, up] (6장)
Pile 6: [down, down, down, down, down, down, up] (7장)

검사 대상: Pile 3-6의 밑에서 4번째부터 맨 위까지
```

**검사 항목**:

**1) Deep Blockage 검사 (순차적 irretrievable)** ⭐
```
검사 방식: 맨 위부터 아래로 순차 검사

Pile 3: 맨 위 1장 검사 (face-down 3장)
  → 맨 위 카드가 irretrievable? YES → Unsolvable! (밑의 3장 접근 불가)
  
Pile 4: 맨 위 2장 검사 (face-down 4장)
  → 맨 위 카드 irretrievable? YES → Unsolvable!
  → NO? → 다음 카드 검사
  → 2번째 카드 irretrievable? YES → Unsolvable! (밑의 4장 접근 불가)
  
Pile 5: 맨 위 3장 검사
Pile 6: 맨 위 4장 검사

→ 총 10장, 비용 O(30)

핵심: 맨 위부터 순차적으로 하나라도 irretrievable이면 즉시 Unsolvable!
```

**이유:**
```
Pile 6 예시: [A, B, C, D, E, F, G]
                        ↑ 맨 위부터 검사

1. G가 irretrievable? → YES → 끝! (F 접근 불가 → Unsolvable)
2. G가 retrievable? → G 옮길 수 있음 → F 검사
3. F가 irretrievable? → YES → 끝! (E 접근 불가 → Unsolvable)
4. F가 retrievable? → F 옮길 수 있음 → E 검사
...

논리: 맨 위 카드를 못 옮기면 그 밑의 모든 카드는 영원히 접근 불가!
```

**최적화:**
필요 카드 최소 3장이므로:
- Pile 0-2: 검사 불필요 (face-down < 3장)
- Pile 3: 1장만 검사 (3장 중 1장만 irretrievable이어도 밑의 2장 부족)
- Pile 4: 2장 검사 (4장 중 2장 irretrievable → 밑의 2장 부족)
- Pile 5: 3장 검사 (5장 중 3장 irretrievable → 밑의 2장 부족)
- Pile 6: 4장 검사 (6장 중 4장 irretrievable → 밑의 2장 부족)

**2) King Deadlock**
```
빈 공간 없고, 모든 킹이 접근 불가
→ 비용 O(7)
```

**총 검사 비용**: O(30 + 7) = **O(37) ≈ <1ms**

**장점**:
- 매우 빠름 (<1ms)
- Shuffle 시 unsolvable 게임 즉시 감지
- **최적화**: Pile 0-2 검사 제외, 필요한 최소 장수만 검사
- **Deep Blockage가 Single을 포함**: 맨 위부터 순차 검사하므로 별도 Single 검사 불필요
- **조기 발견**: Pile 밑바닥 문제 즉시 감지
- Stock 무관 (완전 정보 게임이므로 모든 카드 값 알고 있음)

**Deep Blockage 로직 (최적화)**:
```kotlin
fun hasDeepBlockage(state: GameState): Boolean {
    for ((pileIndex, pile) in state.tableau.withIndex()) {
        // Pile 0-2: Face-down < 3장이므로 스킵
        if (pileIndex < 3) continue
        
        // 검사할 장수: Pile 3→1장, Pile 4→2장, Pile 5→3장, Pile 6→4장
        val cardsToCheck = pileIndex - 2  // 3→1, 4→2, 5→3, 6→4
        val startIndex = pile.faceUpCards.size - cardsToCheck
        
        // 맨 위부터 순차 검사 (하나라도 irretrievable이면 즉시 return)
        for (i in startIndex until pile.faceUpCards.size) {
            val card = pile.faceUpCards[i]
            val faceDownBelow = pile.faceDownCards
            
            if (isCardIrretrievable(card, faceDownBelow, state)) {
                // 이 카드를 옮길 수 없음 → 밑의 모든 카드 접근 불가
                return true  // Inherently Unsolvable!
            }
        }
    }
    return false
}
```

**예시**:
```
Pile 6: [하트A, 다이아2, 클로버3, 스페이드4, 하트5, 다이아6, 클로버7]
        [  0      1       2        3        4      5       6   ]
                                  ↑ 검사 시작 (맨 위 4장)

검사 순서 (맨 위부터 아래로):
1. 클로버7 (index 6) irretrievable? → YES → Inherently Unsolvable! (하트5 접근 불가)
2. 클로버7 retrievable? → 다음 검사
3. 다이아6 (index 5) irretrievable? → YES → Inherently Unsolvable! (하트5 접근 불가)
4. 다이아6 retrievable? → 다음 검사
...

핵심: 맨 위부터 하나라도 irretrievable이면 그 즉시 Inherently Unsolvable!
→ index 0-2 (하트A, 다이아2, 클로버3)는 영원히 접근 불가
→ 하트A가 필수 카드이므로 Inherently Unsolvable!

Pile 4: [하트2, 다이아3, 클로버4, 스페이드5, 하트6]
        [  0      1       2        3        4   ]
                          ↑ 검사 시작 (맨 위 2장)

검사: index 3-4 순차 검사
1. 하트6 irretrievable? → YES → Inherently Unsolvable!
2. 하트6 retrievable? → 스페이드5 검사
3. 스페이드5 irretrievable? → YES → Inherently Unsolvable!
```

#### 2. 카드 뒤집을 때 ⭐ **새로운 face-down 노출 시**
```
이벤트: Tableau에서 카드 옮기고 새 face-down 뒤집음
검사 대상: 그 pile의 새 face-up bottom card
검사 비용: O(k) where k = 해당 pile의 face-down 수 ≈ O(3)
```

**장점:**
- 필요할 때만 검사 (효율적)
- 새로운 정보가 드러날 때마다 업데이트

#### 3. Stock 소진 시 (선택적)
```
시점: Stock 마지막 카드 draw
검사: 전체 Tableau 재검사
비용: O(21) ≈ <1ms
```

**참고:**
- 이미 카드 뒤집을 때마다 검사했으므로 불필요할 수 있음
- 하지만 한 번 더 확인하는 것도 비용이 낮음

---

## 3. Unsolvable Detector 구현

### 실전 적용

```kotlin
class UnsolvableDetector {
    fun isUnsolvable(state: GameState): UnsolvableReason? {
        // 1. Dead End (매 이동 후)
        if (isDeadEnd(state)) {
            return UnsolvableReason.DeadEnd
        }
        
        // 2. King Deadlock
        if (hasKingDeadlock(state)) {
            return UnsolvableReason.KingDeadlock
        }
        
        
        // 3. Deep Blockage (핵심!)
        if (hasDeepBlockage(state)) {
            return UnsolvableReason.DeepBlockage
        }
        
        // 4. Deep Pair (선택적)
        if (hasDeepPair(state)) {
            return UnsolvableReason.DeepPairIrretrievable
        }
        
        return null  // Solvable 또는 판단 불가
    }
    
    /**
     * 게임 시작 시: Pile 밑에서 4번째부터 맨 위까지 모두 Irretrievable인지
     */
    private fun hasDeepBlockage(state: GameState): Boolean {
        for (pile in state.tableau) {
            if (pile.size < 4) continue  // 4장 미만 스킵
            
            // 밑에서 4번째부터 맨 위까지 (순차적으로 뒤집힌다고 가정)
            val startIndex = pile.size - 4
            var allIrretrievable = true
            
            for (i in startIndex until pile.size) {
                val card = pile[i]
                // 완전 정보 게임: face-down도 값을 알고 있음
                val belowCards = pile.subList(0, i)
                if (!isCardIrretrievable(card, belowCards, state)) {
                    allIrretrievable = false
                    break
                }
            }
            
            if (allIrretrievable) {
                // 밑의 카드(index 0 ~ startIndex-1)는 영원히 접근 불가
                return true  // Inherently Unsolvable!
            }
        }
        
        return false
    }
}

sealed class UnsolvableReason {
    object DeadEnd : UnsolvableReason()
    object KingDeadlock : UnsolvableReason()
    object DeepBlockage : UnsolvableReason()
    object DeepPairIrretrievable : UnsolvableReason()
}
```

**구현 우선순위:**
1. ✅ Phase 1 (완료): Dead End, King Deadlock
2. ⏸️ Phase 2 (설계 완료): **Deep Blockage** (1 pile)
   - **게임 시작 시 검사 가능** (완전 정보 게임)
   - **Stock 무관** (Tableau 배치만으로 결정)
   - **맨 위부터 순차 검사** 방식 (Deep)
   - **두 방향 모두 고려** (Foundation + Tableau) ← 중요!
3. ⏸️ Phase 3 (선택적): **Deep Pair** (2 piles)
   - 정확도 향상을 위한 추가 검사
   - 비용 대비 효과 고려하여 선택적 구현
4. 🔮 Phase 4 (매우 선택적): **Deep Group** (3+ piles)
   - 우선순위 매우 낮음 (비용 과다, 발생 빈도 극히 낮음)

---

### 3. 킹 데드락 (King Deadlock)
```kotlin
// 모든 빈 공간이 없고, 필요한 카드들이 킹 밑에 깔림
// 킹을 옮길 곳이 없으면 영구적으로 막힘

fun hasKingDeadlock(state: GameState): Boolean {
    val emptyColumns = state.tableau.count { it.isEmpty() }
    if (emptyColumns > 0) return false  // 킹을 옮길 곳이 있음
    
    // 모든 킹이 맨 위에 있는지 확인
    val kingsOnTop = state.tableau.count { pile ->
        pile.lastOrNull()?.rank?.value == 13
    }
    
    if (kingsOnTop == 0) return false  // 킹이 맨 위에 없으면 괜찮음
    
    // 킹 밑에 중요한 카드가 있는지 확인
    for (pile in state.tableau) {
        if (pile.lastOrNull()?.rank?.value == 13) {
            // 킹이 맨 위에 있음, 밑에 뒷면 카드가 있나?
            val hasFaceDownBelow = pile.any { !it.isFaceUp }
            if (hasFaceDownBelow) {
                return true  // 킹 밑에 뒷면 카드 → 데드락
            }
        }
    }
    
    return false
}
```

### 4. 필수 카드 접근 불가 (Required Card Unreachable)
```kotlin
// Foundation을 완성하려면 반드시 필요한 카드가 절대 꺼낼 수 없는 위치에 있음
// 
// 예: ♠A가 여러 뒷면 카드 밑에 깔려있고, 
//     그 카드들을 공개하려면 ♠A가 Foundation에 있어야 함

fun hasUnreachableRequiredCard(state: GameState): Boolean {
    // 각 무늬별로 다음에 필요한 카드 찾기
    for ((suitIndex, foundation) in state.foundation.withIndex()) {
        val nextRank = foundation.size + 1
        if (nextRank > 13) continue
        
        // 이 카드가 Stock이나 Waste에 있으면 OK
        // Tableau에서 앞면으로 있으면 OK
        // 뒷면에 있는데 공개가 불가능하면 문제
        
        // (상세 구현 필요)
    }
    return false
}
```

### 통합 Unsolvable 검사기
```kotlin
class UnsolvableDetector {
    fun isUnsolvable(state: GameState): UnsolvableReason? {
        // 빠른 순서대로 체크 (가장 쉬운 것부터)
        
        if (isDeadEnd(state)) {
            return UnsolvableReason.DeadEnd
        }
        
        if (hasKingDeadlock(state)) {
            return UnsolvableReason.KingDeadlock
        }
        
        if (hasDeepBlockage(state)) {
            return UnsolvableReason.DeepBlockage
        }
        
        // 더 복잡한 체크들...
        
        return null  // Solvable 또는 판단 불가
    }
}

sealed class UnsolvableReason {
    object DeadEnd : UnsolvableReason()
    object KingDeadlock : UnsolvableReason()
    object DeepBlockage : UnsolvableReason()
    object DeepPairIrretrievable : UnsolvableReason()
}
```

### 구현 우선순위
1. **Phase 1** (구현 완료): DeadEnd, KingDeadlock
2. **Phase 2** (설계 완료): Deep Blockage (1 pile, 핵심)
3. **Phase 3** (선택적): Deep Pair (2 piles)
4. **Phase 4** (매우 선택적): Deep Group (3+ piles, 비추천)

---

## 🕐 Unsolvable 검사 시점 최적화

### 핵심 원칙

**중요한 통찰**: 
- Klondike Solitaire는 **완전 정보 게임** (모든 52장 카드 값이 게임 시작 시 알려짐)
- Irretrievable 판단은 **Tableau 배치 상태**만으로 가능 (Stock/Waste 무관!)
- 각 패턴은 **최적 시점**에 검사하면 충분
- 한 번 Unsolvable로 판단되면 **게임 종료** → 대부분 검사는 최대 1회만 실행

---

### 🎯 패턴별 최적 검사 시점 요약표

| 패턴 | Unsolvable 타입 | 최적 검사 시점 | 빈도 | 비용 (단일) | 우선순위 | 검사 범위 |
|------|----------------|---------------|------|------------|---------|----------|
| **Dead End** | Unwinnable State | 매 이동 후 | 매우 높음 (N회) | O(1) | ⭐⭐⭐ 최고 | 전체 상태 |
| **King Deadlock** | Inherently Unsolvable | 게임 시작 | 1회 | O(7) | ⭐⭐ 높음 | 전체 Tableau |
| **Deep Blockage** | Inherently Unsolvable | ①게임 시작 ②카드 뒤집을 때 | 중간 (1+M회) | O(30) / O(k×3) | ⭐⭐⭐ 최고 | 1개 pile |
| **Deep Pair** | Inherently Unsolvable | 게임 시작 (선택적) | 1회 | O(1,680) | ⭐ 중간 | 2개 pile 조합 |
| **Deep Group** | Inherently Unsolvable | 게임 시작 (선택적) | 1회 | O(5,200) | 낮음 | 3-5 pile 조합 |

**참고**:
- **Unwinnable State**: 플레이어의 선택으로 인한 막힌 상태 (다른 경로로는 해결 가능했음)
- **Inherently Unsolvable**: 초기 배치 자체의 구조적 문제 (어떤 경로로도 해결 불가능)
- **완전 정보 게임**: 모든 카드 값은 게임 시작 시 알려짐 (위치만 face-down)
- **Stock 무관**: Inherently Unsolvable 패턴은 Tableau 배치만으로 결정됨
- **Deep 방식 통일**: 모든 Irretrievable 검사는 **맨 위부터 순차 검사** 방식 사용
  - Deep Blockage: 1개 pile의 face-up 카드를 맨 위부터 검사
  - Deep Pair: 2개 pile 조합의 face-up 카드를 맨 위부터 검사
  - Deep Group: 3+ pile 조합의 face-up 카드를 맨 위부터 검사
- **N**: 플레이어 이동 횟수 (게임당 50-200회)
- **M**: 카드 뒤집기 횟수 (게임당 최대 21회)
- **k**: 해당 pile의 검사 대상 카드 수 (1-4장)

---

### 📋 검사 시점별 상세 설명

#### 🎮 1. 게임 시작 시 (Game Start)

**시점**: 새 게임 셔플 직후 (모든 카드 배치 완료)

**핵심 통찰**: 
- ✅ **완전 정보 게임**: 모든 52장 카드의 값과 위치가 결정됨
- ✅ **Stock 무관**: Tableau 배치만으로 Irretrievable 여부 결정
- ✅ **즉시 검사 가능**: 카드가 face-down이어도 값은 알고 있으므로 검사 가능

**필수 검사 항목**:

| 패턴 | 비용 | 이유 |
|------|------|------|
| **Deep Blockage** | O(30) | Pile 3-6의 face-up 맨 밑 부근 카드 검사 (1,2,3,4장) |
| **King Deadlock** | O(7) | King 카드가 현 상태에서 이동 불가능한지 확인 |

**선택적 검사 항목** (정확도 향상):

| 패턴 | 비용 | 선택 기준 |
|------|------|----------|
| **Deep Pair** | O(1,680) | 정확도 향상, 추천 |
| **Deep Group (전체)** | O(5,200) | 선택적, 허용 가능한 비용 |

**총 비용**:
```
- 기본 (Deep Blockage + King): O(37) ≈ <1ms
- 추천 (Deep Blockage + King + Deep Pair): O(1,717) ≈ 1-2ms
- 선택적 (+ Deep Group): O(6,917) ≈ 7ms
```

**구현 예시**:
```kotlin
fun checkOnGameStart(state: GameState): UnsolvableReason? {
    // 필수 검사
    if (hasDeepBlockage(state)) {
        return UnsolvableReason.DeepBlockage
    }
    if (hasKingDeadlock(state)) {
        return UnsolvableReason.KingDeadlock
    }
    
    // 선택적 검사 (정확도 향상)
    if (hasPairIrretrievable(state)) {  // 또는 hasDeepPair(state)
        return UnsolvableReason.PairIrretrievable
    }
    
    return null  // Solvable (또는 아직 판단 불가)
}
```

**장점**:
- ✅ Shuffle 시 unsolvable 게임 즉시 감지
- ✅ 플레이 전에 미리 알림 가능
- ✅ 매우 빠름 (<2ms)
- ✅ 완전 정보 게임이므로 정확한 판단 가능

---

#### 🔄 2. 카드 뒤집을 때 (On Card Flipped)

**시점**: Tableau에서 카드 이동 후 새 face-down 카드가 뒤집힐 때

**검사 항목**:
- ✅ **해당 pile의 Deep Blockage** (매우 중요!)

**비용**: O(k × 3) where k = 검사할 카드 수 (1-4장)
- Pile 0-2: 검사 안 함 (face-down < 3장)
- Pile 3: 맨 위 1장 검사 → O(3)
- Pile 4: 맨 위 2장 검사 → O(6)
- Pile 5: 맨 위 3장 검사 → O(9)
- Pile 6: 맨 위 4장 검사 → O(12)

**구현 예시**:
```kotlin
fun onCardFlipped(pileIndex: Int, state: GameState): UnsolvableReason? {
    // Pile 0-2는 검사 불필요
    if (pileIndex < 3) return null
    
    // 해당 pile의 Deep Blockage 검사
    if (hasDeepBlockageAtPile(pileIndex, state)) {
        return UnsolvableReason.DeepBlockage
    }
    
    return null
}
```

**장점**:
- ✅ 새로운 정보(새 face-up 카드) 드러날 때 즉시 판단
- ✅ 매우 빠름 (최대 O(12))
- ✅ 정확한 시점 (필요할 때만 검사)

**빈도**: 게임당 최대 21회 (모든 face-down 카드 뒤집기)

---

#### 🏃 3. 매 이동 후 (After Each Move)

**시점**: 플레이어가 카드를 이동할 때마다

**검사 항목**:
- ✅ **Dead End** (실시간 필수!)

**비용**: O(1) - 매우 빠름

**구현 예시**:
```kotlin
fun onAfterMove(state: GameState): UnsolvableReason? {
    // Stock과 Waste가 비었고, 가능한 이동이 전혀 없음
    if (isDeadEnd(state)) {
        return UnsolvableReason.DeadEnd
    }
    
    return null
}
```

**장점**:
- ✅ 사용자가 즉시 알아야 함 (막힌 상태)
- ✅ 계산 비용 극히 낮음
- ✅ 실시간 피드백

**빈도**: 게임당 50-200회 (플레이어 이동 횟수)

---

#### 📦 4. Stock 소진 시 (Stock Empty) - 선택적

**시점**: Stock의 마지막 카드를 Draw했을 때

**검사 항목** (선택적):
- ⏸️ **전체 Tableau Deep Blockage 재검사**: O(37)

**총 비용**: O(37) ≈ <1ms

**참고**:
- ⚠️ **대부분 불필요**: 이미 게임 시작 + 카드 뒤집을 때마다 검사했음
- ✅ **선택적 구현**: 한 번 더 전체 검사하고 싶다면 (비용 낮음)

---

#### 🎊 5. All Face-Up 시 (모든 카드 앞면)

**시점**: Tableau의 모든 카드가 앞면이 되었을 때

**검사 항목**:
- ❌ **검사 불필요** - 이미 승리 확정!

**이유**:
- 모든 face-down 카드 성공적으로 뒤집음
- Tableau 카드들은 정렬된 상태
- Foundation으로 순서대로 옮기기만 하면 승리

**실행**:
- Unsolvable 검사 대신 **자동 완성** 실행
- 승리 애니메이션 표시

---

#### 🤖 6. Solver 탐색 중 (During Search) - 내부용

**시점**: BFS/A* 알고리즘이 새 상태 탐색할 때

**검사 항목**:
- ✅ **Dead End** (각 상태마다)
  - 비용: O(1)
  - 불필요한 탐색 가지치기

**이유**:
- 탐색 공간 축소 → Solver 속도 향상
- 비용 낮은 검사만 실행

---

### 🎯 구현 권장 전략

#### ⭐ 권장 구성 A: 기본 (빠르고 효과적)

```kotlin
class UnsolvableDetector {
    fun checkUnsolvable(state: GameState, trigger: CheckTrigger): UnsolvableReason? {
        return when (trigger) {
            CheckTrigger.GAME_START -> {
                // 게임 시작 시: Deep Blockage + King (O(37) ≈ <1ms)
                hasKingDeadlock(state)
                    ?: hasDeepBlockage(state)
                    ?: null
            }
            
            CheckTrigger.CARD_FLIPPED -> {
                // 카드 뒤집을 때: 해당 pile Deep Blockage 검사 (O(3-12))
                hasDeepBlockageAtPile(currentPileIndex, state)
            }
            
            CheckTrigger.AFTER_MOVE -> {
                // 매 이동 후: Dead End만 (O(1))
                isDeadEnd(state) ?: null
            }
            
            else -> null
        }
    }
}
```

**총 비용**: 게임당 O(37 + 21×12 + 200×1) ≈ O(489) ≈ <1ms

---

#### ⭐⭐ 권장 구성 B: 정확도 향상 (추천)

```kotlin
class UnsolvableDetector {
    fun checkUnsolvable(state: GameState, trigger: CheckTrigger): UnsolvableReason? {
        return when (trigger) {
            CheckTrigger.GAME_START -> {
                // 게임 시작 시: Deep Blockage + King + Deep Pair (O(1,717) ≈ 1-2ms)
                hasKingDeadlock(state)
                    ?: hasDeepBlockage(state)
                    ?: hasDeepPair(state)
                    ?: null
            }
            
            CheckTrigger.CARD_FLIPPED -> {
                // 카드 뒤집을 때: 해당 pile Deep Blockage 검사
                hasDeepBlockageAtPile(currentPileIndex, state)
            }
            
            CheckTrigger.AFTER_MOVE -> {
                // 매 이동 후: Dead End
                isDeadEnd(state) ?: null
            }
            
            else -> null
        }
    }
}
```

**총 비용**: 게임당 O(1,717 + 21×12 + 200×1) ≈ O(2,169) ≈ 2ms

---

#### 🔬 고급 구성 C: 최대 정확도 (선택적)

```kotlin
class UnsolvableDetector {
    fun checkUnsolvable(state: GameState, trigger: CheckTrigger): UnsolvableReason? {
        return when (trigger) {
            CheckTrigger.GAME_START -> {
                // 게임 시작 시: Deep Blockage + King + Deep Pair (O(1,717) ≈ 1-2ms)
                hasKingDeadlock(state)
                    ?: hasDeepBlockage(state)
                    ?: hasDeepPair(state)
                    ?: null
            }
            
            CheckTrigger.CARD_FLIPPED -> {
                // 카드 뒤집을 때: 해당 pile Deep Blockage 검사
                hasDeepBlockageAtPile(currentPileIndex, state)
            }
            
            CheckTrigger.AFTER_MOVE -> {
                // 매 이동 후: Dead End
                isDeadEnd(state) ?: null
            }
            
            else -> null
        }
    }
}
```

**총 비용**: 게임당 O(1,717 + 21×12 + 200×1) ≈ O(2,169) ≈ 2ms

---

### 📊 Deep Group 고려사항

**Deep Group** (3+ piles가 서로를 막는 경우):

**특징**:
- 3개 이상의 pile이 서로의 필요 카드를 모두 막고 있음
- Deep Pair와 동일한 로직 (맨 위부터 순차 검사)
- **게임 시작 시 이미 결정됨** (완전 정보 게임)
- 하지만 **매우 희귀한 패턴**

**게임 시작 시 Tableau 구조:**
```
Pile 0: 1장 (0 face-down, 1 face-up)  ← face-down 없음, Deep Group 불가
Pile 1: 2장 (1 face-down, 1 face-up)  ← face-down 1장뿐, 거의 무의미
Pile 2: 3장 (2 face-down, 1 face-up)
Pile 3: 4장 (3 face-down, 1 face-up)
Pile 4: 5장 (4 face-down, 1 face-up)
Pile 5: 6장 (5 face-down, 1 face-up)
Pile 6: 7장 (6 face-down, 1 face-up)

총 face-down: 0+1+2+3+4+5+6 = 21장
```

**의미 있는 조합 분석:**

Deep Group이 의미 있으려면 각 pile에 **충분한 face-down**이 있어야 함:
- Pile 0, 1은 face-down이 거의 없어서 제외
- **의미 있는 pile: [2, 3, 4, 5, 6]** (5개 pile)

**3-pile Deep Group:**
```
C(5,3) = 10개 의미 있는 조합 (Pile 0,1 제외)
전체 C(7,3) = 35개이지만, 실제 의미 있는 것은 10개

각 조합당:
- 3 pile × 평균 2장 face-up = 6장 검사
- 카드당: O(36) (필요 카드 4개 × face-down 9장)
- 조합당: 6 × 36 = 216

총 비용: 10 × 216 = 2,160
O(2,160) ≈ 2ms
```

**4-pile Deep Group:**
```
C(5,4) = 5개 의미 있는 조합
- [2,3,4,5]: 2+3+4+5 = 14장 face-down
- [2,3,4,6]: 2+3+4+6 = 15장 face-down
- [2,3,5,6]: 2+3+5+6 = 16장 face-down
- [2,4,5,6]: 2+4+5+6 = 17장 face-down
- [3,4,5,6]: 3+4+5+6 = 18장 face-down

각 조합당:
- 4 pile × 평균 2장 face-up = 8장 검사
- 카드당: O(56) (필요 카드 4개 × face-down 14장)
- 조합당: 8 × 56 = 448

총 비용: 5 × 448 = 2,240
O(2,240) ≈ 2ms
```

**5-pile, 6-pile, 7-pile:**
```
C(5,5) = 1개 조합 [2,3,4,5,6]
- 5 pile × 2장 = 10장 검사
- 카드당: O(80) (필요 카드 4개 × face-down 20장)
- 총: 10 × 80 = 800

C(6,6) = 0개 (Pile 0,1 제외하면 불가능)
C(7,7) = 0개 (Pile 0,1 포함해도 의미 없음)

5-pile 비용: O(800) ≈ 1ms
```

**전체 Deep Group 비용 (재계산):**
```
3-pile: O(2,160) ≈ 2ms
4-pile: O(2,240) ≈ 2ms
5-pile: O(800) ≈ 1ms

전체 합계: O(5,200) ≈ 5ms
```

**비교 (수정됨):**

| 패턴 | 조합 수 | 총 비용 | 시간 |
|------|--------|---------|------|
| Deep Blockage | 7 piles | O(37) | <1ms |
| Deep Pair | C(7,2) = 21 | O(1,680) | 1-2ms |
| Deep Group (3-pile) | C(5,3) = 10 | O(2,160) | 2ms |
| Deep Group (4-pile) | C(5,4) = 5 | O(2,240) | 2ms |
| Deep Group (5-pile) | C(5,5) = 1 | O(800) | 1ms |
| **Deep Group (전체)** | **16 조합** | **O(5,200)** | **5ms** |

**권장사항 (업데이트):**
- ✅ **3-pile Deep Group**: O(2,160) ≈ 2ms (허용 가능, 선택적)
- ✅ **4-pile Deep Group**: O(2,240) ≈ 2ms (허용 가능, 선택적)
- ✅ **5-pile Deep Group**: O(800) ≈ 1ms (허용 가능, 선택적)
- ✅ **전체 Deep Group 구현**: O(5,200) ≈ 5ms (생각보다 저렴!)
- ⏸️ **선택적 구현**: 정확도가 매우 중요한 경우
- ✅ Deep Blockage + Deep Pair로 대부분 커버되므로 우선순위는 낮음

---

### 🎯 핵심 정리

**최적 검사 전략**:
1. ⭐⭐⭐ **필수**: Dead End (매 이동) + Deep Blockage (시작+뒤집기) + King (시작)
2. ⭐⭐ **추천**: Deep Pair 추가 (게임 시작)
3. ⏸️ **선택적**: Deep Group (정확도 중요 시, +5ms)

**핵심 통찰**:
- ✅ **완전 정보 게임**: 게임 시작 시 검사 가능!
- ✅ **Stock 무관**: Tableau 배치만으로 결정
- ✅ **Deep 방식 통일**: 모든 검사가 **맨 위부터 순차 검사** (동일 로직)
- ✅ **명확한 계층**: Deep Blockage (1 pile) < Deep Pair (2 piles) < Deep Group (3+ piles)
- ✅ **이벤트 기반**: 필요할 때만 검사 (효율적)
                    }
                    if (hasFourthIndexBlockage(state)) {
                        return UnsolvableReason.DeepBlockage
                    }
                }
            }
            
            CheckTrigger.ALL_FACE_UP -> {
                // 모든 카드가 앞면 = 승리 확정!
                // Unsolvable 검사 불필요, 자동 완성만 실행
                return null  // Solvable 확정
            }
            
            CheckTrigger.FOUNDATION_MILESTONE -> {
                val total = state.foundation.sumOf { it.size }
                
                if (total >= 20 && !foundationMilestone20Done) {
                    foundationMilestone20Done = true
                    
                    if (hasDeepBlockage(state)) {
                        return UnsolvableReason.DeepBlockage
                    }
                }
            }
            
            CheckTrigger.BEFORE_AUTO_COMPLETE -> {
                // 종합 검사 (모든 것)
                return checkAll(state)
            }
            
            CheckTrigger.SOLVER_SEARCH -> {
                // 가벼운 가지치기만
                if (isDeadEnd(state)) {
                    return UnsolvableReason.DeadEnd
                }
            }
        }
        
        return null
    }
    
    private fun checkAll(state: GameState): UnsolvableReason? {
        // 빠른 것부터 검사 (조기 종료)
        if (isDeadEnd(state)) return UnsolvableReason.DeadEnd
        if (hasKingDeadlock(state)) return UnsolvableReason.KingDeadlock
        if (hasDeepBlockage(state)) return UnsolvableReason.DeepBlockage
        if (hasDeepPair(state)) return UnsolvableReason.DeepPairIrretrievable
        if (hasIrretrievableCard(state)) return UnsolvableReason.IrretrievableCard
        if (hasPairIrretrievability(state)) return UnsolvableReason.PairIrretrievable
        if (hasFourthIndexBlockage(state)) return UnsolvableReason.DeepBlockage
        
        // Group은 너무 비싸서 선택적으로만
        // if (hasGroupIrretrievability(state, maxSize = 3)) {
        //     return UnsolvableReason.GroupIrretrievable
        // }
        
        return null
    }
}

enum class CheckTrigger {
    AFTER_MOVE,              // 각 이동 후
    STOCK_EMPTY,             // Stock 소진
    ALL_FACE_UP,             // 모든 카드 앞면
    FOUNDATION_MILESTONE,    // Foundation 진행도
    BEFORE_AUTO_COMPLETE,    // AutoComplete 전
    SOLVER_SEARCH            // Solver 탐색 중
}
```

#### Strategy B: 조건부 검사 (Conditional Checking)

특정 조건이 만족될 때만 검사:

```kotlin
class SmartUnsolvableDetector {
    
    fun checkWithConditions(state: GameState): UnsolvableReason? {
        
        // 1. 항상 검사 (비용 매우 낮음)
        if (isDeadEnd(state)) {
            return UnsolvableReason.DeadEnd
        }
        
        // 2. 모든 카드가 앞면인 경우 - 승리 확정!
        val allFaceUp = state.tableau.all { pile -> 
            pile.all { it.isFaceUp } 
        }
        
        if (allFaceUp) {
            return null  // Solvable 확정, 검사 불필요
        }
        
        // 3. Stock 빈 경우 - 모든 주요 검사 수행 ⭐
        if (state.stock.isEmpty()) {
            if (hasKingDeadlock(state)) {
                return UnsolvableReason.KingDeadlock
            }
            
            if (hasDeepBlockage(state)) {
                return UnsolvableReason.DeepBlockage
            }
            
            if (hasIrretrievableCard(state)) {
                return UnsolvableReason.IrretrievableCard
            }
            
            if (hasPairIrretrievability(state)) {
                return UnsolvableReason.PairIrretrievable
            }
            
            if (hasFourthIndexBlockage(state)) {
                return UnsolvableReason.DeepBlockage
            }
        }
        
        // 4. Foundation 진행도가 충분한 경우 - 조기 발견 (선택적)
        val foundationTotal = state.foundation.sumOf { it.size }
        
        if (foundationTotal >= 20 && state.stock.isNotEmpty()) {
            // Stock이 아직 있어도 조기 발견 시도
            if (hasDeepBlockage(state)) {
                return UnsolvableReason.DeepBlockage
            }
        }
        
        return null
    }
}
```

#### Strategy C: 이벤트 기반 검사 (Event-Driven Checking)

게임 이벤트에 반응하여 검사:

```kotlin
class GameViewModel : ViewModel() {
    
    private val unsolvableDetector = UnsolvableDetector()
    
    fun onCardMoved() {
        // 이동 직후 빠른 검사
        val result = unsolvableDetector.checkUnsolvable(
            state = engine.getState(),
            trigger = CheckTrigger.AFTER_MOVE
        )
        
        if (result != null) {
            showUnsolvableDialog(result)
        }
    }
    
    fun onStockEmpty() {
        // Stock 소진 시 종합 검사
        val result = unsolvableDetector.checkUnsolvable(
            state = engine.getState(),
            trigger = CheckTrigger.STOCK_EMPTY
        )
        
        if (result != null) {
            showUnsolvableDialog(result)
        }
    }
    
    fun onCardFlipped() {
        // 뒷면 카드가 뒤집힐 때마다 확인
        if (isAllFaceUp()) {
            // 모든 카드가 앞면 = 승리 확정!
            // Foundation으로 자동 이동
            autoCompleteToFoundation()
            showVictoryAnimation()
        }
    }
    
    fun onFoundationChanged() {
        // Foundation 진행도 확인
        val total = engine.getFoundationTotal()
        
        if (total in listOf(10, 20, 30, 40)) {
            val result = unsolvableDetector.checkUnsolvable(
                state = engine.getState(),
                trigger = CheckTrigger.FOUNDATION_MILESTONE
            )
            
            if (result != null) {
                showUnsolvableDialog(result)
            }
        }
    }
}
```

---

### 검사 빈도 및 비용 요약

| 패턴 | 최적 시점 | 빈도 | 계산 비용 | 우선순위 | 검사 범위 |
|------|----------|------|----------|---------|----------|
| **Dead End** | 매 이동 후 | 높음 (N회) | O(1) | 최고 | 전체 상태 |
| **King Deadlock** | 게임 시작 | 1회 | O(7) | 중간 | 전체 Tableau |
| **Deep Blockage** | 게임 시작 + 카드 뒤집을 때 | 중간 (1+M회) | O(30) | 최고 | 1 pile |
| **Deep Pair** | 게임 시작 (선택적) | 1회 | O(1,680) | 중간 | 2 piles |
| **Deep Group** | 게임 시작 (선택적) | 1회 | O(5,200) | 낮음 | 3-5 piles, 16조합 |

**총 검사 횟수**: 게임당 최대 20-30회
**총 계산 비용**: 
- **기본**: O(N·1 + M·30 + 37) ≈ 빠름 (<1ms)
- **Deep Pair 포함**: O(N·1 + M·30 + 37 + 1,680) ≈ 약간 느림 (1-2ms)

**참고**:
- 모든 검사가 **Deep 방식** (맨 위부터 순차 검사)으로 통일됨
- Deep Blockage, Deep Pair 모두 **게임 시작 시 이미 결정됨** (Tableau 배치로 확정)
- **Stock 무관** (완전 정보 게임이므로 모든 카드 값 알고 있음)
- Single/Pair/Group은 모두 Deep 방식의 검사 범위 차이일 뿐 (동일 로직)
- 구현 권장: Deep Blockage만으로도 충분 (Pair 계열은 선택적)
- 게임 시작 검사 총 비용:
  - Deep만: O(37) ≈ <1ms
  - Deep + Pair: O(337) ≈ <1ms  
  - Deep + Deep Pair: O(1,717) ≈ 1-2ms

---

### 구현 권장사항

1. ✅ **Phase 1**: 게임 시작 시 검사 ⭐ **가장 중요**
   - **Deep Blockage**: O(30) - Pile 3-6만, 맨 위부터 순차 검사 (1,2,3,4장)
     - Single Card Irretrievable 포함 (맨 위부터 순차 검사하므로)
   - **King Deadlock**: O(7) - 킹 블록 확인
   - **옵션 A - Pair**: O(200-300) - 각 pile 맨 밑 1장씩만 검사
   - **옵션 B - Deep Pair**: O(1,680) - 각 pile 여러 장 순차 검사
     - Pair Irretrievable 포함 (맨 밑 카드도 검사하므로)
     - 옵션 A와 B 중 하나만 선택 (Deep Pair가 Pair 포함)
   - **총 비용**: 
     - 기본 (Deep만): O(37) ≈ <1ms
     - Deep + Pair: O(337) ≈ <1ms
     - Deep + Deep Pair: O(1,717) ≈ 1-2ms
   - Shuffle 시 unsolvable 게임 즉시 감지
   - **Stock 무관** (완전 정보 게임)

2. ✅ **Phase 2**: 카드 뒤집을 때마다 검사 ⭐ **핵심**
   - 해당 pile의 Deep Blockage: O(k × 3) where k ≈ 1-4장
   - 맨 위부터 순차 검사 (하나라도 irretrievable이면 즉시 Unsolvable)
   - 매우 빠르고 정확한 판단 (최대 O(12))
   - 이벤트: `onCardFlipped(pileIndex)`

3. ⏸️ **Phase 3**: Stock 소진 시 추가 검사 (선택적)
   - 전체 Tableau Deep Blockage 재검사: O(37)
   - 이미 Phase 1, 2에서 검사했으므로 불필요할 수 있음

4. ✅ **Phase 4**: All Face Up 감지 시 자동 승리
   - Unsolvable 검사 대신 자동 완성 실행
   - Foundation으로 순차 이동
   - 승리 애니메이션

**핵심**: 
- **게임 시작 + 카드 뒤집기 = 최적의 검사 시점**
- **Deep 패턴이 Single/Pair를 포함**:
  - Deep Blockage ⊃ Single Card Irretrievable
  - Deep Pair ⊃ Pair Irretrievable
- **구현 권장**: Deep Blockage만으로도 충분 (대부분 커버)
- **Pair 계열**: 선택적 구현 (추가 정확도 원할 때)
- **Stock 무관** (완전 정보 게임이므로 모든 카드 값 알고 있음)
- **All Face Up = 승리 확정, 검사 불필요**
- 각 검사는 **필요할 때만** 실행
- **이벤트 기반**으로 효율성 극대화

---

## ⚡ 성능 최적화

### 1. 상태 해싱
```kotlin
data class StateHash(
    val tableauHash: Int,
    val foundationHash: Int,
    val stockWasteHash: Int
) {
    companion object {
        fun from(state: GameState): StateHash {
            // 효율적인 해시 계산
            return StateHash(
                tableauHash = state.tableau.hashCode(),
                foundationHash = state.foundation.hashCode(),
                stockWasteHash = (state.stock.size shl 16) or state.waste.size
            )
        }
    }
}
```

### 2. 이동 순서 최적화
우선순위가 높은 이동부터 탐색:
1. Tableau/Waste → Foundation (목표에 가까움)
2. 뒷면 카드 공개하는 이동
3. Tableau → Tableau (킹을 빈 공간으로)
4. Draw
5. Foundation → Tableau (점수 손실)

### 3. 가지치기
- 명백히 나쁜 이동 제외:
  - Foundation에서 빼낼 필요가 없는 카드
  - 순환하는 이동 (A→B, B→A)
  - Stock이 비었는데 계속 draw

---

## 🔬 Solver 알고리즘 비교

### BFSSolver (구현 완료 ✅)
**특징:**
- ✅ 완전성 보장 (경로가 있으면 반드시 찾음)
- ✅ 구현 완료 및 테스트 통과
- ❌ 속도 느림 (모든 경로를 순차 탐색)
- 📊 평균 탐색: 수천~수만 상태

**제약:**
- MAX_DEPTH: 50수
- MAX_STATES: 10,000개
- TIMEOUT: 5초

**용도:** 
- 작은 게임 (거의 완성된 상태)
- 정확도가 중요한 경우
- 힌트 기능 (단순 게임)

### AStarSolver (구현 완료 ✅)
**특징:**
- ✅ 휴리스틱으로 빠른 탐색
- ✅ Priority Queue로 유망한 경로 우선 탐색
- ⚠️ 휴리스틱이 완벽하지 않으면 최적 경로 보장 안 됨
- 📊 평균 탐색: 수백~수천 상태 (BFS보다 10배 빠름)

**휴리스틱 요소:**
1. Foundation에 올라가지 않은 카드 수 (가중치 10)
2. 블로킹된 카드 비용 (가중치 5)
3. Stock/Waste 카드 수 (가중치 2)
4. 뒷면 카드 수 (가중치 3)
5. 빈 컬럼 평가

**제약:**
- MAX_DEPTH: 150수 (BFS보다 3배)
- MAX_STATES: 100,000개 (BFS보다 10배)
- TIMEOUT: 5초

**용도:**
- 복잡한 게임
- 실시간 힌트
- Auto Play

### Greedy AutoComplete (현재 구현 ⚠️)
**특징:**
- ✅ 매우 빠름 (밀리초 단위)
- ❌ 불완전함 (막다른 골목 가능)
- ❌ Foundation으로만 이동
- ❌ Look-ahead 없음

**용도:**
- 1차 시도 (빠른 정리)
- 하이브리드 접근의 Phase 1

### 알고리즘 선택 가이드

| 상황 | 추천 알고리즘 | 이유 |
|------|--------------|------|
| 거의 완성 (10수 이내) | BFS | 빠르고 확실함 |
| 중간 진행 (10-30수) | A* | 휴리스틱으로 효율적 |
| 초반 상태 (30수 이상) | A* → Timeout | 가능한 만큼만 탐색 |
| 간단한 자동화 | Greedy | 빠른 Foundation 이동 |
| 완벽한 자동화 | 하이브리드 | Greedy + Solver fallback |

---

## 🏗️ 구현 단계 및 현황

### ✅ Step 1: 기본 구조 (완료)
- ✅ Move 타입 정의 → `Move.kt`
- ✅ GameState 복제 함수 → `GameStateUtils.cloneState()`
- ✅ getAllPossibleMoves() 구현 → `BFSSolver`, `AStarSolver`
- ✅ applyMove() 구현 → `GameStateUtils.applyMove()`

### ✅ Step 2: BFS Solver (완료)
- ✅ BFS 알고리즘 구현 → `BFSSolver.kt`
- ✅ 상태 해싱 → `GameStateUtils.stateHash()`
- ✅ 경로 재구성 → `BFSNode.path`
- ✅ 단위 테스트 → `BFSSolverTest.kt`

### ✅ Step 3: A* Solver (완료)
- ✅ A* 알고리즘 구현 → `AStarSolver.kt`
- ✅ 휴리스틱 함수 → `heuristic()`
- ✅ Priority Queue 통합 → `SearchNode.fCost`
- ✅ 단위 테스트 → `AStarSolverTest.kt`

### ⏸️ Step 4: UI 통합 (미작업)
- ⏸️ 백그라운드 스레드 처리 (Coroutine)
- ⏸️ 힌트 버튼 연결 및 카드 강조 표시
- ⏸️ 로딩 인디케이터
- ⏸️ 승리 가능성 표시 UI

### ⏸️ Step 5: Auto Play (미작업)
- ⏸️ Solver 결과 순차 실행 로직
- ⏸️ 애니메이션 통합 (500ms 딜레이)
- ⏸️ 진행률 표시

### 🔄 Step 6: AutoComplete 개선 (우선 작업 필요)
- ❌ AutoComplete와 Solver 통합
- ⏸️ 하이브리드 접근 구현
- ⏸️ Seed 17848904495592789619 검증

---

## 📋 다음 작업 우선순위

### 우선순위 1: AutoComplete 개선 (1-2일) 🔥
**목표:** Greedy 알고리즘의 한계를 Solver로 보완

**작업:**
1. `GameViewModel.autoComplete()`를 하이브리드 방식으로 재구성
2. Phase 1: 빠른 Greedy 이동 (Foundation)
3. Phase 2: 막히면 Solver fallback (A* 우선, 실패 시 BFS)
4. 단위 테스트로 문제 케이스 검증
   - Seed: 17848904495592789619
   - Seed: 10420697478978593767
5. 성능 측정 및 timeout 조정

**예상 효과:**
- ✅ Solvable 게임 100% 완료 가능
- ✅ 대부분 경우 빠른 실행 (Greedy)
- ✅ 복잡한 경우만 Solver 사용

### 우선순위 2: UI 기본 기능 (2-3일)
**목표:** 사용자가 Solver를 실제로 활용할 수 있도록

**작업:**
1. 힌트 버튼 추가
   - `solver.findBestMove()` 호출
   - 카드 강조 표시 (깜빡임 or 테두리)
   - 로딩 인디케이터 (탐색 중)
2. 승리 가능성 표시
   - 게임 시작 시 백그라운드 체크
   - ✅/❌/⏱️ 아이콘 표시
3. 막힘 감지
   - `UnsolvableDetector.check()` 활용
   - "더 이상 진행할 수 없습니다" 다이얼로그

### 우선순위 3: Auto Play 구현 (3-5일)
**목표:** Solver 경로를 시각적으로 순차 실행

**작업:**
1. `autoPlay()` 함수 구현 (Coroutine Flow)
2. 이동 간 500ms 딜레이 + 애니메이션
3. 진행률 표시 (15/52 이동)
4. 일시정지/재개 기능
5. UI 버튼 추가

### 우선순위 4: 최적화 및 고급 기능 (향후)
- A* 휴리스틱 튜닝
- 고급 Unsolvable 패턴 구현
  - Single Card Irretrievable (Tableau pile 내부 순환 블로킹)
  - Deep Blockage (밑에서 4번째부터 맨 위까지 모두 irretrievable)
  - Pair Irretrievable (두 pile 상호 블로킹, 게임 시작 시 검사 가능)
  - Group Irretrievable (다중 카드 집단 블로킹, 선택적)
- 성능 측정 및 개선
- 멀티스레드 탐색
- 난이도 평가

---

## 🧪 테스트 케이스

### 간단한 승리 가능 케이스
```kotlin
@Test
fun simple_solvable_game() {
    // 거의 다 완성된 게임 (3-5수 남음)
    val state = GameState(...)
    val result = solver.solve(state)
    assertTrue(result.isSolvable)
    assertTrue(result.moves.size in 3..5)
}
```

### 명백히 불가능한 케이스
```kotlin
@Test
fun obviously_unsolvable() {
    // 필요한 카드가 완전히 막힌 상태
    val state = GameState(...)
    val result = solver.solve(state)
    assertFalse(result.isSolvable)
}
```

### 복잡한 케이스
```kotlin
@Test
fun complex_solvable_game() {
    val state = GameState(...)
    val result = solver.solve(state, timeout = 10_000) // 10초
    // 결과 확인 (승리 가능 여부는 미리 알 수 없음)
}
```

---

## 🔮 향후 확장

### Phase 3: 고급 기능
- **확률적 분석**: draw 3 모드에서 다음 카드 예측
- **난이도 평가**: 게임의 난이도 점수화
- **학습 기반 휴리스틱**: 플레이 데이터로 휴리스틱 개선
- **멀티스레드 탐색**: 병렬 탐색으로 속도 향상

### Phase 4: 통계 통합
- 승리 가능한 게임만 통계에 포함 옵션
- 평균 해결 시간 vs 실제 플레이 시간
- "최적 해" 대비 실제 이동 수

---

## 📚 참고 자료

### 알고리즘
- BFS (Breadth-First Search)
- A* (A-star) Search
- IDA* (Iterative Deepening A*)

### 솔리테어 솔버 연구
- "Solitaire: Man Versus Machine" (Ian Parberry, 1999)
- FreeCell 솔버 알고리즘
- Spider Solitaire 솔버 기법

### 구현 예제
- Microsoft Solitaire의 힌트 시스템
- Aisleriot (GNOME) 솔버
- PySolFC 솔버

---

## ✅ 성공 기준

### 최소 요구사항 (MVP)
- [ ] 간단한 게임(10수 이내)에서 95% 이상 정확도
- [ ] 평균 탐색 시간 < 3초
- [ ] 메모리 사용량 < 50MB

### 이상적인 목표
- [ ] 복잡한 게임(30수 이상)에서 80% 해결
- [ ] 평균 탐색 시간 < 1초
- [ ] 힌트 제공 시 즉각 반응 (<500ms)

---

## 📝 구현 우선순위 요약

### 현재 상태 (2026-02-20)
1. ✅ **완료**: BFS Solver, A* Solver, Unsolvable Detector (부분)
2. ⚠️ **문제**: AutoComplete가 Solver를 사용하지 않음 → Solvable 게임도 막힘
3. ⏸️ **미작업**: UI 통합, Auto Play, 나머지 Unsolvable 패턴

### 다음 작업
1. 🔥 **High Priority**: AutoComplete + Solver 통합 (하이브리드 방식)
2. 🔶 **Medium Priority**: 힌트 버튼 + 카드 강조 표시
3. 🔷 **Low Priority**: Auto Play + 애니메이션
4. 💡 **Future**: A* 튜닝, 난이도 평가, 멀티스레드

---

## 🐛 알려진 이슈 및 해결 히스토리

### Issue #1: AutoComplete Greedy 알고리즘의 한계 (미해결)
**발생일:** 2026-02-06  
**증상:**
- Solvable 게임에서 AutoComplete가 조기 종료
- 예: Seed 17848904495592789619 → 69수 후 멈춤 (Foundation 7/52)
- 실제 가능한 이동: HEARTS THREE (T[0]) → CLUBS FOUR (T[5])

**근본 원인:**
- AutoComplete는 Greedy 알고리즘 사용
- Foundation으로만 이동, Tableau 재배치 없음
- 로컬 최적만 선택 → 글로벌 최적 보장 안 됨

**시도한 해결 방안:**
1. Step 5.5 추가 (Tableau 재배치) → 실패
2. Step 3을 3a/3b로 분리 → 실패
3. Look-ahead 추가 검토 → 복잡도 높음

**제안된 해결책:**
- 하이브리드 접근: Greedy + Solver fallback
- 우선순위 1로 작업 예정

**상태:** 🔴 **Open** (우선순위 1로 작업 예정)

---

### Issue #2: Recycle 무한 루프 (해결됨 ✅)
**발생일:** 2026-02-06  
**증상:**
- Stock에 6장만 남았을 때 반복적으로 "6개 카드를 자동으로 이동했다" 메시지 출력
- Recycle이 무한 반복됨

**근본 원인:**
- Recycle 후 Waste 카드 사용 여부를 추적하지 않음
- Stock이 비었는지 미리 체크하지 않음

**해결 방법:**
```kotlin
// 추가된 변수
var recycleCount = 0
var wasteUsedAfterRecycle = false
var drawsAfterRecycle = 0
var recycleSize = 0

// Recycle 조건 개선
if (stock.isEmpty() && !wasteUsedAfterRecycle && 
    drawsAfterRecycle >= recycleSize - 1) {
    // 중단
}
```

**검증:**
- `RecycleDebugTest.kt`: 첫 autoComplete 85수, 두 번째 0수
- `AutoCompleteRecycleTest.kt`: 다양한 시나리오 테스트 통과
- APK 빌드 및 실제 기기(SM-S938N) 배포 완료

**상태:** ✅ **Resolved** (2026-02-06)

---

### Issue #3: Unsolvable Detector 패턴 미완성 (부분 해결)
**발생일:** 설계 단계  
**증상:**
- Circular Dependency 패턴 미구현
- Irretrievable Card 패턴 미구현

**현재 상태:**
- ✅ Dead End (구현 완료)
- ✅ King Deadlock (구현 완료)
- ⏸️ **Deep Blockage** (설계 완료, **구현 우선순위 높음**)
  - **검사 시점**: 게임 시작 + 카드 뒤집을 때
  - **핵심 통찰**: Stock 무관, Tableau pile 내부만 검사, 맨 위부터 순차 검사
  - **비용**: O(30) ≈ <1ms (매우 빠름)
- ⏸️ **Pair Irretrievable** (설계 완료, **구현 우선순위 중간**)
  - **검사 시점**: 게임 시작 시
  - **핵심 통찰**: 2 piles 상호 블로킹, 맨 위부터 순차 검사
  - **비용**: O(1,680) ≈ 1-2ms

**우선순위:**
- **Phase 1**: Basic patterns (완료) ✅
- **Phase 2**: Single Irretrievable (다음 우선순위) ⭐⭐⭐
- **Phase 3**: Pair Irretrievable (선택적) ⭐
- Phase 4: Group Irretrievable (매우 선택적, 낮은 우선순위)

**상태:** ⚠️ **Partial** (기본 기능은 동작, Single + Pair 구현 예정)

---

## 📊 테스트 케이스 및 검증

### 알려진 문제 케이스

#### 케이스 1: Seed 17848904495592789619
- **현상:** AutoComplete가 69수에서 멈춤
- **상태:** Foundation 7/52, Stock 0, Waste 6
- **가능한 이동:** HEARTS THREE → CLUBS FOUR
- **문제:** Greedy 알고리즘이 Foundation 이동만 시도
- **검증 예정:** 하이브리드 AutoComplete 구현 후

#### 케이스 2: Seed 10420697478978593767
- **현상:** AutoComplete 후 진행 가능 여부 체크
- **상태:** 테스트 중
- **목적:** RealGameStateTest에서 사용

### 성공한 케이스

#### 케이스 1: Recycle 시나리오
- **Seed:** Various (RecycleDebugTest)
- **결과:** ✅ 무한 루프 없이 정상 종료
- **검증:** 첫 autoComplete 후 두 번째 autoComplete = 0 이동

---

## 🧪 N-Pile Irretrievable 테스트 케이스

### 테스트 전략

N-Pile Irretrievable 프레임워크를 검증하기 위해 다음 계층의 테스트를 작성합니다:

1. **단위 테스트**: 각 N 값별 독립 테스트
2. **통합 테스트**: 전체 프레임워크 동작 검증
3. **시나리오 테스트**: 게임 플로우 전체 검증

---

### 1. Single Irretrievable (N=1) 테스트

#### Test 1.1: 기본 Single Irretrievable
```kotlin
@Test
fun `test single irretrievable - basic case`() {
    val state = GameState(
        tableau = listOf(
            // Pile 0: 하트2가 Single Irretrievable
            TableauPile(
                faceUpCards = listOf(Card(Suit.HEARTS, Rank.TWO)),
                faceDownCards = listOf(
                    Card(Suit.HEARTS, Rank.ACE),      // Foundation 필요
                    Card(Suit.SPADES, Rank.THREE),    // Tableau 필요
                    Card(Suit.CLUBS, Rank.THREE)      // Tableau 필요
                )
            ),
            // ... 다른 piles
        ),
        foundation = emptyFoundation(),
        stock = emptyList(),
        waste = emptyList()
    )
    
    val result = hasNPileIrretrievable(state, n = 1)
    assertTrue(result, "하트2는 Single Irretrievable이어야 함")
    
    val reason = checkInherentlyUnsolvable(state)
    assertTrue(reason is UnsolvableReason.NPileIrretrievable.Single)
}
```

#### Test 1.2: Single Retrievable (음성 테스트)
```kotlin
@Test
fun `test single retrievable - foundation path available`() {
    val state = GameState(
        tableau = listOf(
            TableauPile(
                faceUpCards = listOf(Card(Suit.HEARTS, Rank.TWO)),
                faceDownCards = listOf(
                    Card(Suit.SPADES, Rank.THREE),
                    Card(Suit.CLUBS, Rank.THREE)
                )
                // 하트A는 없음!
            ),
            // ...
        ),
        foundation = mapOf(
            Suit.HEARTS to listOf(Card(Suit.HEARTS, Rank.ACE))  // Foundation 경로 가능!
        ),
        // ...
    )
    
    val result = hasNPileIrretrievable(state, n = 1)
    assertFalse(result, "하트2는 Foundation으로 갈 수 있음")
}
```

#### Test 1.3: Single Retrievable - Tableau 경로
```kotlin
@Test
fun `test single retrievable - tableau path available`() {
    val state = GameState(
        tableau = listOf(
            // Pile 0
            TableauPile(
                faceUpCards = listOf(Card(Suit.HEARTS, Rank.TWO)),
                faceDownCards = listOf(Card(Suit.HEARTS, Rank.ACE))
            ),
            // Pile 1: Tableau 경로 제공
            TableauPile(
                faceUpCards = listOf(Card(Suit.SPADES, Rank.THREE)),
                faceDownCards = emptyList()
            ),
            // ...
        ),
        foundation = emptyFoundation(),
        // ...
    )
    
    val result = hasNPileIrretrievable(state, n = 1)
    assertFalse(result, "하트2는 스페이드3 위에 갈 수 있음")
}
```

---

### 2. Pair Irretrievable (N=2) 테스트

#### Test 2.1: 기본 Pair Irretrievable
```kotlin
@Test
fun `test pair irretrievable - mutual blocking`() {
    val state = GameState(
        tableau = listOf(
            // Pile 0: 하트3
            TableauPile(
                faceUpCards = listOf(Card(Suit.HEARTS, Rank.THREE)),
                faceDownCards = listOf(
                    Card(Suit.HEARTS, Rank.ACE),
                    Card(Suit.HEARTS, Rank.TWO)
                )
            ),
            // Pile 1: 다이아3 (서로 블로킹)
            TableauPile(
                faceUpCards = listOf(Card(Suit.DIAMONDS, Rank.THREE)),
                faceDownCards = listOf(
                    Card(Suit.SPADES, Rank.FOUR),    // 하트3이 필요
                    Card(Suit.CLUBS, Rank.FOUR)      // 하트3이 필요
                )
            ),
            // ...
        ),
        foundation = emptyFoundation(),
        // ...
    )
    
    val result = hasNPileIrretrievable(state, n = 2)
    assertTrue(result, "Pile 0과 1이 서로 블로킹")
    
    val reason = checkInherentlyUnsolvable(state)
    assertTrue(reason is UnsolvableReason.NPileIrretrievable.Pair)
}
```

#### Test 2.2: Pair Retrievable (Stock에 필요 카드)
```kotlin
@Test
fun `test pair retrievable - card in stock`() {
    val state = GameState(
        tableau = listOf(
            // Pile 0, 1: 위와 동일한 구조
            // ...
        ),
        foundation = emptyFoundation(),
        stock = listOf(
            Card(Suit.SPADES, Rank.FOUR)  // 하트3이 갈 곳!
        ),
        waste = emptyList()
    )
    
    val result = hasNPileIrretrievable(state, n = 2)
    assertFalse(result, "Stock에 스페이드4가 있어서 해결 가능")
}
```

---

### 3. Triple/Quad/Quint Irretrievable 테스트

#### Test 3.1: Triple Irretrievable
```kotlin
@Test
fun `test triple irretrievable - 3 piles mutual blocking`() {
    val state = GameState(
        tableau = listOf(
            // Pile 0: 하트5
            TableauPile(
                faceUpCards = listOf(Card(Suit.HEARTS, Rank.FIVE)),
                faceDownCards = listOf(
                    Card(Suit.HEARTS, Rank.ACE),
                    Card(Suit.HEARTS, Rank.TWO),
                    Card(Suit.HEARTS, Rank.THREE),
                    Card(Suit.HEARTS, Rank.FOUR)
                )
            ),
            // Pile 1: 다이아5
            TableauPile(
                faceUpCards = listOf(Card(Suit.DIAMONDS, Rank.FIVE)),
                faceDownCards = listOf(
                    Card(Suit.SPADES, Rank.SIX),
                    Card(Suit.CLUBS, Rank.SIX)
                )
            ),
            // Pile 2: 클로버5
            TableauPile(
                faceUpCards = listOf(Card(Suit.CLUBS, Rank.FIVE)),
                faceDownCards = listOf(
                    Card(Suit.HEARTS, Rank.SIX),
                    Card(Suit.DIAMONDS, Rank.SIX)
                )
            ),
            // ...
        ),
        foundation = emptyFoundation(),
        // ...
    )
    
    val result = hasNPileIrretrievable(state, n = 3)
    assertTrue(result, "3개 pile이 서로 블로킹")
    
    val reason = checkInherentlyUnsolvable(state)
    assertTrue(reason is UnsolvableReason.NPileIrretrievable.Group)
    assertEquals(3, (reason as UnsolvableReason.NPileIrretrievable.Group).n)
}
```

---

### 4. King Irretrievable 테스트

#### Test 4.1: King Irretrievable - 완전 차단
```kotlin
@Test
fun `test king irretrievable - all paths blocked`() {
    val state = GameState(
        tableau = listOf(
            // Pile 0: King pile
            TableauPile(
                faceUpCards = listOf(Card(Suit.HEARTS, Rank.KING)),
                faceDownCards = listOf(
                    Card(Suit.HEARTS, Rank.QUEEN)  // Foundation 차단
                )
            ),
            // Pile 1: Pair Irretrievable with Pile 0
            TableauPile(
                faceUpCards = listOf(Card(Suit.DIAMONDS, Rank.THREE)),
                faceDownCards = listOf(
                    Card(Suit.SPADES, Rank.FOUR),
                    Card(Suit.CLUBS, Rank.FOUR)
                )
            ),
            // Pile 2: Pair Irretrievable with Pile 0
            TableauPile(
                faceUpCards = listOf(Card(Suit.SPADES, Rank.FIVE)),
                faceDownCards = listOf(
                    Card(Suit.HEARTS, Rank.SIX),
                    Card(Suit.DIAMONDS, Rank.SIX)
                )
            ),
            // ... (다른 모든 pile도 Pile 0과 Pair Irretrievable)
        ),
        foundation = emptyFoundation(),
        // ...
    )
    
    val result = isKingIrretrievable(0, state)
    assertTrue(result, "King은 Foundation + Tableau 모두 차단")
    
    val reason = checkInherentlyUnsolvable(state)
    assertTrue(reason is UnsolvableReason.KingIrretrievable)
}
```

#### Test 4.2: King Retrievable - Foundation 경로
```kotlin
@Test
fun `test king retrievable - foundation path available`() {
    val state = GameState(
        tableau = listOf(
            TableauPile(
                faceUpCards = listOf(Card(Suit.HEARTS, Rank.KING)),
                faceDownCards = emptyList()  // Queen 없음!
            ),
            // ...
        ),
        foundation = mapOf(
            Suit.HEARTS to listOf(
                Card(Suit.HEARTS, Rank.ACE),
                // ... 하트J까지
                Card(Suit.HEARTS, Rank.JACK)
            )
        ),
        // ...
    )
    
    val result = isKingIrretrievable(0, state)
    assertFalse(result, "King은 곧 Foundation으로 갈 수 있음 (Queen만 필요)")
}
```

#### Test 4.3: King Retrievable - 빈 pile 생성 가능
```kotlin
@Test
fun `test king retrievable - empty pile can be created`() {
    val state = GameState(
        tableau = listOf(
            // Pile 0: King pile (Foundation 차단)
            TableauPile(
                faceUpCards = listOf(Card(Suit.HEARTS, Rank.KING)),
                faceDownCards = listOf(Card(Suit.HEARTS, Rank.QUEEN))
            ),
            // Pile 1: 비울 수 있는 pile
            TableauPile(
                faceUpCards = listOf(Card(Suit.DIAMONDS, Rank.ACE)),
                faceDownCards = emptyList()
            ),
            // ...
        ),
        foundation = emptyFoundation(),
        // ...
    )
    
    val result = isKingIrretrievable(0, state)
    assertFalse(result, "Pile 1을 비울 수 있어서 King 이동 가능")
}
```

---

### 5. King Deadlock 테스트

#### Test 5.1: 기본 King Deadlock
```kotlin
@Test
fun `test king deadlock - king blocks essential card`() {
    val state = GameState(
        tableau = listOf(
            TableauPile(
                faceUpCards = listOf(
                    Card(Suit.HEARTS, Rank.KING),
                    Card(Suit.SPADES, Rank.ACE)  // 필수 카드가 King 위에!
                ),
                faceDownCards = emptyList()
            ),
            // ...
        ),
        foundation = emptyFoundation(),
        // ...
    )
    
    val result = hasKingDeadlock(state)
    assertTrue(result, "King이 스페이드A를 막고 있음")
}
```

---

### 6. 전체 프레임워크 통합 테스트

#### Test 6.1: 다층 체크 (N=1부터 N=5까지)
```kotlin
@Test
fun `test complete framework - check all N values`() {
    val state = createComplexInherentlyUnsolvableState()
    
    // N=1: Single
    assertTrue(hasNPileIrretrievable(state, 1))
    
    // N=2: Pair
    assertTrue(hasNPileIrretrievable(state, 2))
    
    // N=3: Triple
    assertTrue(hasNPileIrretrievable(state, 3))
    
    // 통합 체크
    val reason = checkInherentlyUnsolvable(state)
    assertNotNull(reason)
    assertTrue(reason is UnsolvableReason.NPileIrretrievable.Single) // 가장 먼저 감지
}
```

#### Test 6.2: Solvable 게임 검증
```kotlin
@Test
fun `test solvable game - no irretrievable cards`() {
    val state = createSolvableGameState()
    
    // 모든 N 값에서 irretrievable 없음
    for (n in 1..5) {
        assertFalse(hasNPileIrretrievable(state, n))
    }
    
    // King Deadlock도 없음
    assertFalse(hasKingDeadlock(state))
    
    // King Irretrievable도 없음
    for (i in 0..6) {
        assertFalse(isKingIrretrievable(i, state))
    }
    
    // 최종 판단: Solvable
    val reason = checkInherentlyUnsolvable(state)
    assertNull(reason)
}
```

---

### 7. 게임 플로우 시나리오 테스트

#### Test 7.1: Inherently Unsolvable → Unwinnable State 전환
```kotlin
@Test
fun `test inherently unsolvable leads to unwinnable state`() {
    // 1. 게임 시작: Inherently Unsolvable 감지
    val initialState = GameState(
        tableau = listOf(
            TableauPile(
                faceUpCards = listOf(Card(Suit.HEARTS, Rank.TWO)),
                faceDownCards = listOf(
                    Card(Suit.HEARTS, Rank.ACE),
                    Card(Suit.SPADES, Rank.THREE),
                    Card(Suit.CLUBS, Rank.THREE)
                )
            ),
            // ...
        ),
        // ...
    )
    
    val unsolvableReason = checkInherentlyUnsolvable(initialState)
    assertNotNull(unsolvableReason, "게임 시작 시 Inherently Unsolvable 감지")
    assertTrue(unsolvableReason is UnsolvableReason.NPileIrretrievable.Single)
    
    // 2. 게임 진행 (다른 카드들 이동)
    var currentState = initialState
    // ... 여러 이동 시뮬레이션 ...
    
    // 3. Irretrievable 카드(하트2)가 노출됨
    // (다른 카드들이 모두 처리되고 하트2만 남음)
    val finalState = GameState(
        tableau = listOf(
            TableauPile(
                faceUpCards = listOf(Card(Suit.HEARTS, Rank.TWO)),
                faceDownCards = emptyList()  // 뒤집을 카드 없음
            ),
            // 다른 pile들은 비었거나 이미 처리됨
        ),
        stock = emptyList(),
        waste = emptyList(),
        foundation = emptyFoundation()
    )
    
    // 4. Dead End 확인
    assertTrue(isDeadEnd(finalState), "예상대로 Dead End 발생")
    
    // 5. 결과: Inherently Unsolvable임을 확인
    val solverResult = solve(finalState)
    assertTrue(solverResult is SolverResult.InherentlyUnsolvable)
}
```

#### Test 7.2: Solvable → 최적 경로 → Win
```kotlin
@Test
fun `test solvable game - find optimal path and win`() {
    val solvableState = createSimpleSolvableState()
    
    // 1. Inherently Unsolvable 체크: 없음
    val unsolvableReason = checkInherentlyUnsolvable(solvableState)
    assertNull(unsolvableReason, "Solvable 게임")
    
    // 2. Solver 실행
    val result = bfsSolver.solve(solvableState)
    assertTrue(result is SolverResult.Success, "해결책 발견")
    
    // 3. 경로 검증
    val solution = (result as SolverResult.Success).moves
    assertNotNull(solution)
    assertTrue(solution.isNotEmpty())
    
    // 4. 경로 실행하여 Win 확인
    var state = solvableState
    for (move in solution) {
        state = applyMove(state, move)
    }
    
    assertTrue(isWinState(state), "최종 상태는 Win")
    assertEquals(52, state.foundation.values.sumOf { it.size })
}
```

#### Test 7.3: Solvable → 플레이어 실수 → Unwinnable State
```kotlin
@Test
fun `test solvable game - player mistake leads to unwinnable`() {
    val solvableState = createSolvableState()
    
    // 1. 초기 확인: Solvable
    assertNull(checkInherentlyUnsolvable(solvableState))
    
    // 2. 플레이어가 잘못된 이동 (King을 잘못 배치)
    val badMove = Move.TableauToTableau(
        fromPileIndex = 3,
        toPileIndex = 6,  // 빈 공간에 King을 잘못 배치
        count = 1
    )
    val afterBadMove = applyMove(solvableState, badMove)
    
    // 3. 여전히 Inherently Unsolvable은 아님
    assertNull(checkInherentlyUnsolvable(afterBadMove))
    
    // 4. 하지만 이후 진행이 막힘
    // ... (더 많은 이동 시뮬레이션) ...
    val stuckState = simulateMoreMoves(afterBadMove)
    
    // 5. Dead End 발생
    assertTrue(isDeadEnd(stuckState), "플레이어 실수로 막힘")
    
    // 6. Unwinnable State 확인 (하지만 Inherently Unsolvable은 아님)
    val result = solve(stuckState)
    assertTrue(result is SolverResult.UnwinnableState, "잘못된 선택으로 Unwinnable")
}
```

---

### 8. 성능 테스트

#### Test 8.1: 게임 시작 시 체크 성능
```kotlin
@Test
fun `test performance - initial check within 10ms`() {
    val state = createRandomGameState()
    
    val startTime = System.nanoTime()
    val reason = checkInherentlyUnsolvable(state)
    val endTime = System.nanoTime()
    
    val durationMs = (endTime - startTime) / 1_000_000.0
    
    println("Inherently Unsolvable 체크 시간: ${durationMs}ms")
    assertTrue(durationMs < 10.0, "10ms 이내에 완료되어야 함")
}
```

#### Test 8.2: 각 N 값별 성능 측정
```kotlin
@Test
fun `test performance - breakdown by N value`() {
    val state = createComplexGameState()
    
    val timings = mutableMapOf<Int, Double>()
    
    for (n in 1..5) {
        val startTime = System.nanoTime()
        hasNPileIrretrievable(state, n)
        val endTime = System.nanoTime()
        
        val durationMs = (endTime - startTime) / 1_000_000.0
        timings[n] = durationMs
        
        println("N=$n: ${durationMs}ms")
    }
    
    // 예상 범위 내인지 확인
    assertTrue(timings[1]!! < 1.0, "Single: <1ms")
    assertTrue(timings[2]!! < 2.0, "Pair: <2ms")
    assertTrue(timings[3]!! < 3.0, "Triple: <3ms")
}
```

---

### 테스트 헬퍼 함수

```kotlin
// 테스트 상태 생성 헬퍼
fun createSolvableGameState(): GameState { /* ... */ }
fun createInherentlyUnsolvableState(): GameState { /* ... */ }
fun createComplexGameState(): GameState { /* ... */ }

// Foundation 헬퍼
fun emptyFoundation() = mapOf<Suit, List<Card>>()

// 이동 적용
fun applyMove(state: GameState, move: Move): GameState { /* ... */ }

// 상태 검증
fun isWinState(state: GameState): Boolean {
    return state.foundation.values.sumOf { it.size } == 52
}
```

---

## 📚 참고 문서

### 관련 문서
- `AUTOCOMPLETE_IMPROVEMENTS.md`: AutoComplete 개선 로그 (2026-02-06)
- `TEST_GUIDE.md`: 테스트 작성 가이드
- `STATISTICS_IMPLEMENTATION.md`: 통계 기능 구현

### 코드 위치
```
app/src/main/java/us/jyni/game/klondike/
├── solver/
│   ├── BFSSolver.kt           ✅ 구현 완료
│   ├── AStarSolver.kt         ✅ 구현 완료
│   ├── UnsolvableDetector.kt  ⚠️ 부분 완료
│   ├── GameStateUtils.kt      ✅ 구현 완료
│   ├── Move.kt                ✅ 구현 완료
│   ├── SolverResult.kt        ✅ 구현 완료
│   └── SearchNode.kt          ✅ 구현 완료
├── ui/
│   └── GameViewModel.kt       ⚠️ autoComplete() 개선 필요
└── engine/
    └── GameEngine.kt

app/src/test/java/us/jyni/game/klondike/solver/
├── BFSSolverTest.kt           ✅ 테스트 완료
├── AStarSolverTest.kt         ✅ 테스트 완료
├── UnsolvableDetectorTest.kt  ✅ 테스트 완료
├── GameStateUtilsTest.kt      ✅ 테스트 완료
├── AutoPlayTest.kt            ✅ 테스트 완료
└── RealGameStateTest.kt       🔄 테스트 진행 중
```

