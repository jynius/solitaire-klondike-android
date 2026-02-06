# Klondike Solitaire Solver 설계 문서

## 📊 개요

솔리테어 게임의 승리 가능성을 판단하고, 최적의 이동 경로를 제시하는 Solver 시스템을 설계합니다.

### 목표
1. **승리 가능성 판단**: 현재 게임 상태에서 승리가 가능한지 판단
2. **힌트 제공**: 최적의 다음 이동 제시
3. **자동 플레이**: 승리 경로를 따라 자동으로 게임 진행

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

#### 2. 순환 의존성 (Circular Dependency)
```kotlin
// 예: 카드 A를 얻으려면 B가 필요하고, B를 얻으려면 A가 필요
// 
// Tableau[0]: [... ♥7(뒷면), ♠6(앞면)]
// Tableau[1]: [... ♠6(뒷면), ♥7(앞면)]
// 
// ♠6을 옮기려면 ♥7이 필요 (검은색은 빨간색 위로)
// ♥7을 옮기려면 ♠6이 필요 (빨간색은 검은색 위로)
// → 불가능

fun hasCircularDependency(state: GameState): Boolean {
    val blockedCards = mutableSetOf<Card>()
    
    for (pile in state.tableau) {
        for ((index, card) in pile.withIndex()) {
            if (!card.isFaceUp) {
                // 뒷면 카드를 공개하려면 위의 카드들을 옮겨야 함
                val blockingCards = pile.subList(index + 1, pile.size)
                // 이 카드들을 옮기기 위해 필요한 카드가 뒷면에 있는지 체크
                // (복잡한 의존성 분석 필요)
            }
        }
    }
    
    return false  // 구현 필요
}
```

#### 3. 색상 블록 (Same Color Block)
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

fun hasSameColorBlock(state: GameState): Boolean {
    for ((suitIndex, foundation) in state.foundation.withIndex()) {
        val nextRankNeeded = foundation.size + 1  // A=1, 2=2, ..., K=13
        
        if (nextRankNeeded > 13) continue  // 이미 완성됨
        
        // 이 무늬의 다음 카드를 찾기
        val targetSuit = foundation.firstOrNull()?.suit ?: continue
        
        for (pile in state.tableau) {
            for ((index, card) in pile.withIndex()) {
                if (card.suit == targetSuit && card.rank.value == nextRankNeeded) {
                    // 필요한 카드 발견! 위에 같은 무늬 카드가 있나?
                    val cardsAbove = pile.subList(index + 1, pile.size)
                    val hasSameSuitAbove = cardsAbove.any { 
                        it.suit == targetSuit && it.rank.value > nextRankNeeded 
                    }
                    if (hasSameSuitAbove) {
                        // 같은 무늬의 더 높은 카드가 위에 있음 → 블록
                        return true
                    }
                }
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
        
        if (hasSameColorBlock(state)) {
            return UnsolvableReason.SameColorBlock
        }
        
        // 더 복잡한 체크들...
        
        return null  // Solvable 또는 판단 불가
    }
}

sealed class UnsolvableReason {
    object DeadEnd : UnsolvableReason()
    object KingDeadlock : UnsolvableReason()
    object SameColorBlock : UnsolvableReason()
    object CircularDependency : UnsolvableReason()
    object UnreachableCard : UnsolvableReason()
}
```

### 구현 우선순위
1. **Phase 1** (즉시 구현): DeadEnd, KingDeadlock
2. **Phase 2** (1주일 후): SameColorBlock
3. **Phase 3** (향후): CircularDependency, UnreachableCard

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

## 🏗️ 구현 단계

### Step 1: 기본 구조 (1-2일)
- [ ] Move 타입 정의
- [ ] GameState 복제 함수
- [ ] getAllPossibleMoves() 구현
- [ ] applyMove() 구현

### Step 2: BFS Solver (2-3일)
- [ ] BFS 알고리즘 구현
- [ ] 상태 해싱
- [ ] 경로 재구성
- [ ] 단위 테스트 (승리 가능/불가능 케이스)

### Step 3: UI 통합 (1일)
- [ ] 백그라운드 스레드 처리 (Coroutine)
- [ ] 힌트 버튼 연결
- [ ] 카드 강조 표시
- [ ] 로딩 인디케이터

### Step 4: Auto Play (1일)
- [ ] 순차 실행 로직
- [ ] 애니메이션 통합
- [ ] 진행률 표시

### Step 5: 최적화 (2-3일)
- [ ] A* 알고리즘 구현
- [ ] 휴리스틱 튜닝
- [ ] 성능 측정 및 개선

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

## 📝 구현 우선순위

1. **High**: BFS Solver + 힌트 기능
2. **Medium**: Auto Play + 애니메이션
3. **Low**: A* 최적화 + 승리 가능성 표시
4. **Future**: 고급 기능 (확률 분석, 난이도 평가)
