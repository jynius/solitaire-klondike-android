# Klondike Solitaire Solver 설계 문서

## 📊 개요

솔리테어 게임의 승리 가능성을 판단하고, 최적의 이동 경로를 제시하는 Solver 시스템을 설계합니다.

### 목표
1. **승리 가능성 판단**: 현재 게임 상태에서 승리가 가능한지 판단
2. **힌트 제공**: 최적의 다음 이동 제시
3. **자동 플레이**: 승리 경로를 따라 자동으로 게임 진행

---

## 📋 현재 구현 상태 (2026-02-20 기준)

### ✅ 완료된 항목

#### 1. 기본 구조 (100% 완료)
- ✅ `Move.kt`: 6가지 이동 타입 정의 완료
  - TableauToTableau, TableauToFoundation, WasteToTableau
  - WasteToFoundation, FoundationToTableau, Draw
- ✅ `GameStateUtils.kt`: 상태 복제, 이동 적용, 해싱 완료
- ✅ `SolverResult.kt`: Success/Unsolvable/Timeout/TooComplex 정의

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

#### 4. Unsolvable Detector (50% 완료)
- ✅ `UnsolvableDetector.kt`: 3가지 기본 패턴 구현
  - ✅ Dead End (이동 불가능)
  - ✅ King Deadlock (킹이 필수 카드를 막음)
  - ✅ Same Suit Block (같은 무늬 카드 블로킹)
  - ⏸️ **Single Irretrievable** (1 pile 내부 순환 블로킹, 맨 위부터 순차 검사, 설계 완료)
  - ⏸️ **Pair Irretrievable** (2 piles 상호 블로킹, 맨 위부터 순차 검사, 설계 완료)
  - ⏸️ **Group Irretrievable** (3+ piles 상호 블로킹, 맨 위부터 순차 검사, 매우 선택적)

**참고**: 모든 Irretrievable 검사는 **맨 위부터 순차 검사** 방식으로 통일

#### 5. ViewModel 통합 (100% 완료)
- ✅ `GameViewModel.kt`: Solver 연결 완료
- ✅ `solve()`, `findHint()`, `checkUnsolvable()` 메서드 제공

### ⚠️ 미완성 항목

#### 1. UI 통합 (0% 완료)
- ⏸️ 힌트 버튼 구현 및 카드 강조 표시
- ⏸️ Auto Play 버튼 (순차 실행 + 애니메이션)
- ⏸️ 로딩 인디케이터
- ⏸️ Unsolvable 상태 표시 UI

#### 2. Auto Play 기능 (0% 완료)
- ⏸️ Solver 결과를 순차 실행
- ⏸️ 이동 간 딜레이 + 애니메이션
- ⏸️ 진행률 표시

#### 3. AutoComplete와 Solver 통합 (0% 완료)
- ❌ 현재 `autoComplete()`는 Solver를 사용하지 않음
- ❌ 단순 Greedy 알고리즘으로 인한 한계 (아래 참조)

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

## 🚫 빠른 Unsolvable 판단 (최우선 구현)

탐색 전에 빠르게 unsolvable 상태를 감지하면 불필요한 계산을 크게 줄일 수 있습니다.

### 판단 패턴

#### 1. 즉시 막힘 (Dead End)
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

#### 2. 순환 블로킹 (Circular Blocking / Self-Blocking Dependency)

**정의:**
카드가 **이동 불가능(Irretrievable)** 상태란, **Tableau pile 내부의 순환 의존성** 때문에 해당 카드를 영원히 옮길 수 없는 상태를 말합니다.

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

적어도 하나의 카드라도 이동 불가능하면, 그 밑의 모든 face-down 카드도 접근할 수 없으므로 게임은 Unsolvable입니다.

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
- **→ Unsolvable!**

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

3. **→ Irretrievable! (Unsolvable)**
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
            return true  // Unsolvable!
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

**예시 5: King Card (특수 케이스) ⭐**

King 카드도 **양방향** 이동이 가능합니다:

1. **Foundation 이동**: 같은 무늬의 Queen이 Foundation에 있어야 함
   - 하트K → 하트Q가 Foundation에 있어야 함 (일반 카드와 동일한 로직!)
   
2. **Tableau 이동**: **빈 pile**로만 이동 가능
   - King 위에는 아무 카드도 못 놓음 (King이 최상위)
   - 따라서 King은 **빈 공간**으로만 옮겨갈 수 있음
   - 빈 공간이 있으면 항상 이동 가능!

```
Tableau Pile[4]:
[하트K] ← face-up (맨 밑)
----------- face-down 경계
[하트Q] ← face-down

Foundation[HEARTS]: [하트J] (Q가 아직 안 올라감)

Other piles: 모두 카드가 있음 (빈 공간 없음)
```

**하트K 이동 가능성 분석:**

**1. Foundation으로 이동?**
- 조건: 하트Q가 Foundation[HEARTS]에 있어야 함
- 현재: Foundation[HEARTS] = [하트A, ..., 하트J] (Q 없음)
- 필요 카드: [하트Q]
- 하트Q는 하트K **밑에 face-down**으로 갇혀있음
- → **NO (Foundation 이동 불가)**

**2. Tableau로 이동?**
- 조건: 빈 pile이 있어야 함
- 현재: 모든 pile에 카드가 있음 (빈 공간 없음)
- → **NO (Tableau 이동 불가)**

**결론:**
- Foundation 이동 불가 **AND** Tableau 이동 불가
- **하트K는 Irretrievable!**
- **→ Unsolvable!**

**King은 일반 로직과 약간 다름:**
```kotlin
fun isCardIrretrievable(card: Card, faceDownBelow: List<Card>, state: GameState): Boolean {
    // 1. Foundation 이동 가능성 확인
    val requiredForFoundation = getRequiredForFoundation(card, state)
    val foundationPossible = !requiredForFoundation.all { required ->
        faceDownBelow.any { it.matches(required) }
    }
    
    // 2. Tableau 이동 가능성 확인
    val tableauPossible = if (card.rank == Rank.KING) {
        // King은 빈 공간으로만 갈 수 있음
        state.tableau.any { it.isEmpty() }
    } else {
        // 일반 카드: 반대 색 rank+1 카드 확인
        val requiredForTableau = getRequiredForTableau(card)
        !requiredForTableau.all { required ->
            faceDownBelow.any { it.matches(required) }
        }
    }
    
    // 둘 다 불가능하면 irretrievable
    return !foundationPossible && !tableauPossible
}
```

**getRequiredForFoundation은 King도 동일:**
```kotlin
fun getRequiredForFoundation(card: Card, state: GameState): List<Card> {
    if (card.rank == Rank.ACE) return emptyList()
    
    val required = mutableListOf<Card>()
    val foundationTop = state.foundation[card.suit]?.lastOrNull()
    val currentRank = foundationTop?.rank?.value ?: 0
    
    // 하트K의 경우: Foundation에 하트J까지 있으면 → [하트Q] 반환
    for (rank in (currentRank + 1) until card.rank.value) {
        required.add(Card(card.suit, Rank.fromValue(rank)))
    }
    
    return required
}
```

**King Irretrievable과 일반 Irretrievable 비교:**

| 구분 | 일반 카드 (예: 하트3) | King 카드 (예: 하트K) |
|------|---------------------|----------------------|
| Foundation 이동 | ✅ 같은 suit의 이전 rank들 필요 | ✅ 같은 suit의 Q 필요 (동일!) |
| Tableau 이동 | ✅ 반대 색 rank+1 카드 필요 | ✅ 빈 pile 필요 (다름!) |
| Irretrievable 조건 | Foundation + Tableau 필요 카드 모두 밑에 | Foundation 필요 카드 밑에 **AND** 빈 pile 없음 |
| 검사 방법 | 순서 배치 (Foundation + Tableau 경로) | 순서 배치 (Foundation) + 빈 공간 확인 |

**핵심 통찰:**
- ✅ King도 **양방향** 이동 가능! (Foundation + Tableau)
- ✅ Foundation은 일반 카드와 동일 (Q 필요)
- ✅ Tableau는 **빈 공간** 확인만 하면 됨 (간단!)
- ✅ 빈 공간 생성은 어떤 카드든 상관없음 (Queen과 무관!)

---

**핵심 특징:**
1. ✅ **Stock/Waste 무관**: 오직 Tableau pile 내부만 검사
2. ✅ **Face-up 맨 밑 카드만**: 그 밑의 face-down만 확인
3. ✅ **순서 배치 감지**: 필요한 카드가 모두 밑에 갇혀있으면 irretrievable
4. ✅ **King도 양방향**: Foundation (Q 필요) + Tableau (빈 공간 확인)
5. ✅ **단순하고 빠름**: O(7 × k) where k = face-down 카드 수 (평균 3-4개)
6. ✅ **게임 시작 시 즉시 검사 가능**: 초기 상태에서도 동작
7. ✅ **정확한 판단**: Stock에 뭐가 있든 상관없음

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
- **→ 영원히 안 풀림! Unsolvable!**

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
    // 1. Deep Blockage
    if (hasDeepBlockage(state)) return true
    
    // 2. Single Irretrievable
    if (hasSingleIrretrievable(state)) return true
    
    // 3. Pair Irretrievable (선택적)
    if (hasPairIrretrievable(state)) return true
    
    // 4. King Deadlock
    if (hasKingDeadlock(state)) return true
    
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
   → YES → Unsolvable! (하트3 접근 불가 → 밑의 카드들 접근 불가)

만약 하트4가 retrievable이면:
2. 하트3 irretrievable? (Pile A+B의 face-down 중)
   필요: {하트A, 하트2, 스페이드4, 클로버4}
   → 하트A, 하트2는 Pile B 밑
   → 스페이드4는 Pile A 밑, 클로버4는 Pile B 밑
   → YES → Unsolvable!

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
                    return true  // Unsolvable!
                }
            }
            
            // Pile B의 여러 장을 맨 위부터 순차 검사
            val cardsToCheckB = min(pileB.faceUpCards.size, 4)
            for (k in 0 until cardsToCheckB) {
                val card = pileB.faceUpCards[pileB.faceUpCards.size - 1 - k]
                val combinedFaceDown = pileA.faceDownCards + pileB.faceDownCards
                
                if (isCardIrretrievableInPair(card, combinedFaceDown, state)) {
                    return true  // Unsolvable!
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
                return true  // Unsolvable!
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
1. 클로버7 (index 6) irretrievable? → YES → Unsolvable! (하트5 접근 불가)
2. 클로버7 retrievable? → 다음 검사
3. 다이아6 (index 5) irretrievable? → YES → Unsolvable! (하트5 접근 불가)
4. 다이아6 retrievable? → 다음 검사
...

핵심: 맨 위부터 하나라도 irretrievable이면 그 즉시 Unsolvable!
→ index 0-2 (하트A, 다이아2, 클로버3)는 영원히 접근 불가
→ 하트A가 필수 카드이므로 Unsolvable!

Pile 4: [하트2, 다이아3, 클로버4, 스페이드5, 하트6]
        [  0      1       2        3        4   ]
                          ↑ 검사 시작 (맨 위 2장)

검사: index 3-4 순차 검사
1. 하트6 irretrievable? → YES → Unsolvable!
2. 하트6 retrievable? → 스페이드5 검사
3. 스페이드5 irretrievable? → YES → Unsolvable!
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
        
        // 3. Same Suit Block
        if (hasSameSuitBlock(state)) {
            return UnsolvableReason.SameSuitBlock
        }
        
        // 4. Single Card Irretrievable
        if (hasIrretrievableCard(state)) {
            return UnsolvableReason.IrretrievableCard
        }
        
        // 5. Deep Blockage (게임 시작 시)
        if (hasDeepBlockage(state)) {
            return UnsolvableReason.DeepBlockage
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
                return true  // Unsolvable!
            }
        }
        
        return false
    }
}

sealed class UnsolvableReason {
    object DeadEnd : UnsolvableReason()
    object KingDeadlock : UnsolvableReason()
    object SameSuitBlock : UnsolvableReason()
    object IrretrievableCard : UnsolvableReason()
    object DeepBlockage : UnsolvableReason()  // 새로 추가!
}
```

**구현 우선순위:**
1. ✅ Phase 1 (완료): Dead End, King Deadlock, Same Suit Block
2. ⏸️ Phase 2 (설계 완료): **Single Irretrievable** + **Pair Irretrievable**
   - **모두 게임 시작 시 검사 가능** (완전 정보 게임)
   - **Stock 무관** (Tableau 배치만으로 결정)
   - **모두 맨 위부터 순차 검사** 방식으로 통일
3. 🔮 Phase 3 (매우 선택적): **Group Irretrievable** (3개 이상 pile 상호 블로킹)
   - 우선순위 매우 낮음 (비용 과다, 발생 빈도 극히 낮음)

---

### 3. 무늬 블록 (Same Suit Block)
```kotlin
// Foundation에 올라가야 할 카드가 같은 무늬의 더 높은 카드 밑에 깔림
// 
// 예: Foundation[♥] = [A, 2]  (♥3 필요)
//     Tableau[0] = [..., ♥5, ♥3(뒷면)]
// 
// ♥3을 얻으려면 ♥5를 치워야 함
// ♥5를 치우려면 ♥4가 Foundation에 있어야 함  
// ♥4를 올리려면 ♥3이 필요
// → 순환!

fun hasSameSuitBlock(state: GameState): Boolean {
    for (foundation in state.foundation {) {
        if (foundation.isEmpty()) continue  // 아직 시작 안 한 Foundation은 스킵
        
        val topCard = foundation.last()
        val targetSuit = topCard.suit
        val nextRankNeeded = topCard.rank.value + 1  // 다음에 필요한 랭크
        
        if (nextRankNeeded > 13) continue  // 이미 K까지 완성됨
        
        // Tableau에서 필요한 카드 찾기
        for (pile in state.tableau) {
            for ((index, card) in pile.withIndex()) {
                if (card.suit == targetSuit && card.rank.value == nextRankNeeded) {
                    // 필요한 카드 발견!
                    
                    // 1. 이 카드가 뒷면이면 문제
                    if (!card.isFaceUp) {
                        // 위에 같은 무늬의 더 높은 카드가 있는지 확인
                        val cardsAbove = pile.subList(index + 1, pile.size)
                        val hasSameSuitAbove = cardsAbove.any { 
                            it.suit == targetSuit && it.rank.value > nextRankNeeded 
                        }
                        if (hasSameSuitAbove) {
                            // 같은 무늬의 더 높은 카드가 위에 있고, 필요한 카드는 뒷면
                            // → 블록 (더 높은 카드를 내리려면 필요한 카드가 필요함)
                            return true
                        }
                    }
                    
                    // 2. 앞면이어도 같은 무늬의 더 높은 카드가 바로 위에 있으면 문제
                    if (card.isFaceUp && index < pile.size - 1) {
                        val cardAbove = pile[index + 1]
                        if (cardAbove.suit == targetSuit && 
                            cardAbove.rank.value > nextRankNeeded &&
                            cardAbove.rank.value <= topCard.rank.value + 2) {
                            // 예: Foundation에 ♥2, Tableau에 [♥3, ♥4]
                            // ♥3을 올리면 ♥4를 내릴 곳이 없을 수 있음
                            // (더 정교한 분석 필요)
                        }
                    }
                }
            }
        }
        
        // Stock/Waste에서도 확인 (완전성을 위해)
        for (card in state.stock + state.waste) {
            if (card.suit == targetSuit && card.rank.value == nextRankNeeded) {
                // Stock이나 Waste에 있으면 접근 가능
                return false
            }
        }
    }
    return false
}
```

#### 4. 킹 데드락 (King Deadlock)
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

#### 5. 필수 카드 접근 불가 (Required Card Unreachable)
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
        
        if (hasSameSuitBlock(state)) {
            return UnsolvableReason.SameSuitBlock
        }
        
        // 더 복잡한 체크들...
        
        return null  // Solvable 또는 판단 불가
    }
}

sealed class UnsolvableReason {
    object DeadEnd : UnsolvableReason()
    object KingDeadlock : UnsolvableReason()
    object SameSuitBlock : UnsolvableReason()
    object IrretrievableCard : UnsolvableReason()  // 이동 불가능한 카드 (순환 블로킹)
    object AdvancedPattern : UnsolvableReason()     // 기타 고급 패턴
}
```

### 구현 우선순위
1. **Phase 1** (구현 완료): DeadEnd, KingDeadlock, SameSuitBlock
2. **Phase 2** (중요): Single Card Irretrievable, Deep Blockage
3. **Phase 3** (향후 추가, 선택적): Pair Irretrievable
4. **Phase 4** (고급, 선택적): Group Irretrievable (크기 3+), 최적화

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

| 패턴 | 최적 검사 시점 | 빈도 | 비용 (단일) | 우선순위 | 포함 관계 |
|------|---------------|------|------------|---------|----------|
| **Dead End** | 매 이동 후 | 매우 높음 (N회) | O(1) | ⭐⭐⭐ 최고 | - |
| **Deep Blockage** | ①게임 시작 ②카드 뒤집을 때 | 중간 (1+M회) | O(30) / O(k×3) | ⭐⭐⭐ 최고 | ⊃ Single |
| **King Deadlock** | 게임 시작 | 1회 | O(7) | ⭐⭐ 높음 | - |
| **Pair Irretrievable** | 게임 시작 | 1회 | O(200-300) | ⭐ 중간 | - |
| **Deep Pair** | 게임 시작 | 1회 | O(1,680) | ⭐ 낮음 | ⊃ Pair |
| **Group Irretrievable** | 게임 시작 (매우 선택적) | 1회 | O(?) 매우 높음 | 매우 낮음 | ⊃ Pair |
| **Same Suit Block** | Stock 소진 시 (선택적) | 0-1회 | O(52) | 낮음 | - |

**참고**:
- **완전 정보 게임**: 모든 카드 값은 게임 시작 시 알려짐 (위치만 face-down)
- **Stock 무관**: Irretrievable 패턴은 Tableau 배치만으로 결정됨
- **검사 방식 통일**: Single/Pair/Group 모두 **맨 위부터 순차 검사** (Deep 방식)
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
| **Pair Irretrievable** | O(200-300) | 빠르고 효과적, 추천 |
| **Deep Pair** | O(1,680) | Pair 포함, 더 정확하지만 느림 |
| **Group Irretrievable** | O(?) 매우 높음 | 매우 희귀한 패턴, 비추천 |

**총 비용**:
```
- 기본 (Deep + King): O(37) ≈ <1ms
- 추천 (Deep + King + Pair): O(337) ≈ <1ms  
- 최대 (Deep + King + Deep Pair): O(1,717) ≈ 1-2ms
- 과도 (Deep + King + Group): O(?) ≈ 수십 ms (비추천)
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
- ⏸️ **Same Suit Block**: O(52)
- ⏸️ **전체 Tableau Deep Blockage 재검사**: O(37)

**총 비용**: O(89) ≈ <1ms

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
                // 게임 시작 시: Single + King만 (O(37) ≈ <1ms)
                hasSingleIrretrievable(state) 
                    ?: hasKingDeadlock(state)
                    ?: null
            }
            
            CheckTrigger.CARD_FLIPPED -> {
                // 카드 뒤집을 때: 해당 pile Single 검사 (O(3-12))
                hasSingleIrretrievableAtPile(currentPileIndex, state)
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
                // 게임 시작 시: Single + King + Pair (O(1,717) ≈ 1-2ms)
                hasSingleIrretrievable(state) 
                    ?: hasKingDeadlock(state)
                    ?: hasPairIrretrievable(state)
                    ?: null
            }
            
            CheckTrigger.CARD_FLIPPED -> {
                // 카드 뒤집을 때: 해당 pile Single 검사
                hasSingleIrretrievableAtPile(currentPileIndex, state)
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

#### 🔬 고급 구성 C: 최대 정확도 + Same Suit (선택적)

```kotlin
class UnsolvableDetector {
    fun checkUnsolvable(state: GameState, trigger: CheckTrigger): UnsolvableReason? {
        return when (trigger) {
            CheckTrigger.GAME_START -> {
                // 게임 시작 시: Single + King + Pair (O(1,717) ≈ 1-2ms)
                hasSingleIrretrievable(state) 
                    ?: hasKingDeadlock(state)
                    ?: hasPairIrretrievable(state)
                    ?: null
            }
            
            CheckTrigger.CARD_FLIPPED -> {
                // 카드 뒤집을 때: 해당 pile Single 검사
                hasSingleIrretrievableAtPile(currentPileIndex, state)
            }
            
            CheckTrigger.AFTER_MOVE -> {
                // 매 이동 후: Dead End
                isDeadEnd(state) ?: null
            }
            
            CheckTrigger.STOCK_EMPTY -> {
                // Stock 소진 시: Same Suit Block (선택적)
                hasSameSuitBlock(state) ?: null
            }
            
            else -> null
        }
    }
}
```

**총 비용**: 게임당 O(1,717 + 21×12 + 200×1 + 52) ≈ O(2,221) ≈ 2-3ms

---

### 📊 Group Irretrievable 고려사항

**Group Irretrievable** (3+ piles가 서로를 막는 경우):

**특징**:
- 3개 이상의 pile이 서로의 필요 카드를 모두 막고 있음
- Pair와 동일하게 **게임 시작 시 이미 결정됨** (완전 정보 게임)
- 하지만 **매우 희귀한 패턴**

**비용**:
- C(7,3) = 35조합 (3-pile)
- C(7,4) = 35조합 (4-pile)
- ...
- 각 조합당 검사 비용 높음
- **총 비용: 매우 높음** (수십 ms 이상)

**권장사항**:
- ❌ **구현 비추천**: 비용 대비 효과 매우 낮음
- ❌ Pair/Deep Pair로도 대부분 커버됨
- ❌ 발생 빈도 극히 낮음
- ✅ 우선순위: 최하위 (Phase 4 이후)

---

### 🎯 핵심 정리

**최적 검사 전략**:
1. ⭐⭐⭐ **필수**: Dead End (매 이동) + Single (시작+뒤집기) + King (시작)
2. ⭐⭐ **추천**: Pair 추가 (게임 시작)
3. ❌ **비추천**: Group Irretrievable (비용 과다, 매우 희귀)

**핵심 통찰**:
- ✅ **완전 정보 게임**: 게임 시작 시 검사 가능!
- ✅ **Stock 무관**: Tableau 배치만으로 결정
- ✅ **검사 방식 통일**: Single/Pair/Group 모두 **맨 위부터 순차 검사**
- ✅ **명확한 계층**: Single (1 pile) < Pair (2 piles) < Group (3+ piles)
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
                    
                    if (hasSameSuitBlock(state)) {
                        return UnsolvableReason.SameSuitBlock
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
        if (hasSameSuitBlock(state)) return UnsolvableReason.SameSuitBlock
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
            
            if (hasSameSuitBlock(state)) {
                return UnsolvableReason.SameSuitBlock
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
            if (hasSameSuitBlock(state)) {
                return UnsolvableReason.SameSuitBlock
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

| 패턴 | 최적 시점 | 빈도 | 계산 비용 | 우선순위 | 포함 관계 |
|------|----------|------|----------|---------|----------|
| **Dead End** | 매 이동 후 | 높음 (N회) | O(1) | 최고 | - |
| **Deep Blockage** | 게임 시작 + 카드 뒤집을 때 | 중간 (1+M회) | O(30) | 최고 | Single 포함 |
| **King Deadlock** | 게임 시작 | 1회 | O(7) | 중간 | - |
| **Pair Irretrievable** | 게임 시작 | 1회 | O(200-300) | 중간 | - |
| **Deep Pair Irretrievable** | 게임 시작 (선택적) | 1회 | O(1,680) | 낮음 | Pair 포함 |
| **Same Suit Block** | Stock 소진 (선택적) | 1회 | O(52) | 낮음 | - |

**총 검사 횟수**: 게임당 최대 20-30회
**총 계산 비용**: 
- **기본**: O(N·1 + M·30 + 37) ≈ 빠름 (<1ms)
- **Pair 포함**: O(N·1 + M·30 + 37 + 300) ≈ 빠름 (<1ms)
- **Deep Pair 포함**: O(N·1 + M·30 + 37 + 1,680 + 52) ≈ 약간 느림 (1-2ms)

**참고**:
- Deep Blockage, Deep Pair 모두 **게임 시작 시 이미 결정됨** (Tableau 배치로 확정)
- **Deep가 Single/Pair를 포함**: 맨 위부터 순차 검사하므로 별도 검사 불필요
  - Deep Blockage ⊃ Single Card Irretrievable
  - Deep Pair ⊃ Pair Irretrievable
- **Stock 무관** (완전 정보 게임이므로 모든 카드 값 알고 있음)
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
   - Same Suit Block: O(52)
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
- ✅ Same Suit Block (구현 완료)
- ⏸️ **Single Irretrievable** (설계 완료, **구현 우선순위 높음**)
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

