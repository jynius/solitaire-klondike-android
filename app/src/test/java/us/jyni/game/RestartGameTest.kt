package us.jyni.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import us.jyni.game.klondike.engine.GameEngine
import us.jyni.game.klondike.model.GameState
import us.jyni.game.klondike.util.sync.Ruleset
import us.jyni.game.klondike.util.sync.RecycleOrder

/**
 * 게임 재시작 기능 테스트
 * "다시시작" 버튼 클릭 시 같은 배치로 게임이 시작되는지 확인
 */
class RestartGameTest {

    @Test
    fun getSeed_preservesOriginalSeed_afterMoves() {
        val engine = GameEngine()
        val originalSeed = 42uL

        // 게임 시작
        engine.startGame(originalSeed)
        
        // 몇 가지 이동
        engine.draw()
        engine.draw()
        
        // Seed가 여전히 동일한지 확인
        assertEquals("Seed should be preserved after moves", 
            originalSeed, engine.getSeed())
    }

    @Test
    fun restartGame_sameLayoutId_asInitial() {
        val engine = GameEngine()
        val seed = 12345uL

        // 1. 게임 시작하고 Layout ID 저장
        engine.startGame(seed)
        val initialLayoutId = engine.getLayoutId()
        val initialFirstCard = engine.getGameState().tableau[0].last().copy()

        // 2. 몇 가지 이동 수행
        engine.draw()
        engine.draw()
        engine.draw()

        // 3. getSeed()로 현재 시드를 가져와서 다시 시작
        val currentSeed = engine.getSeed()
        engine.startGame(currentSeed)
        
        // 4. Layout ID가 동일한지 확인
        val restartedLayoutId = engine.getLayoutId()
        assertEquals("Layout ID should match after restart with same seed",
            initialLayoutId, restartedLayoutId)
        
        // 5. 첫 번째 카드도 동일한지 확인
        val restartedFirstCard = engine.getGameState().tableau[0].last()
        assertEquals("First card should match",
            initialFirstCard, restartedFirstCard)
    }

    @Test
    fun restartGame_sameLayout_asInitialState() {
        val engine = GameEngine()
        val seed = 12345uL
        val rules = Ruleset() // 기본 규칙 사용

        // 1. 특정 시드로 게임 시작
        engine.startGame(seed, rules)
        val initialState = engine.getGameState()

        // 초기 배치 저장 (deep copy)
        val initialTableau = initialState.tableau.map { column -> 
            column.map { it.copy() } 
        }
        val initialStock = initialState.stock.map { it.copy() }

        // 2. 몇 가지 이동 수행하여 게임 상태 변경
        engine.draw() // 스톡에서 카드 뽑기
        engine.draw()
        engine.draw()

        // 게임 상태가 변경되었는지 확인
        val changedState = engine.getGameState()
        assertTrue("Waste should not be empty after draws", changedState.waste.isNotEmpty())
        assertEquals(21, changedState.stock.size) // 24 - 3 = 21

        // 3. 같은 시드로 재시작 (getSeed()로 현재 시드 가져오기)
        val currentSeed = engine.getSeed()
        val currentRules = engine.getRules()
        assertEquals("Seed should be preserved", seed, currentSeed)
        
        engine.startGame(currentSeed, currentRules)
        val restartedState = engine.getGameState()

        // 4. 재시작 후 배치가 초기 배치와 동일한지 확인
        
        // Tableau 비교
        assertEquals("Tableau column count should match", 
            initialTableau.size, restartedState.tableau.size)
        
        initialTableau.forEachIndexed { colIdx, initialColumn ->
            val restartedColumn = restartedState.tableau[colIdx]
            assertEquals("Column $colIdx size should match", 
                initialColumn.size, restartedColumn.size)
            
            initialColumn.forEachIndexed { cardIdx, initialCard ->
                val restartedCard = restartedColumn[cardIdx]
                assertEquals("Card at tableau[$colIdx][$cardIdx] should match",
                    initialCard, restartedCard)
            }
        }

        // Stock 비교
        assertEquals("Stock size should match", initialStock.size, restartedState.stock.size)
        initialStock.forEachIndexed { idx, initialCard ->
            assertEquals("Stock card at index $idx should match",
                initialCard, restartedState.stock[idx])
        }

        // Foundation 비교 (초기에는 모두 비어있음)
        assertEquals("Foundation should be empty", 4, restartedState.foundation.size)
        assertTrue("All foundation piles should be empty",
            restartedState.foundation.all { it.isEmpty() })

        // Waste 비교 (초기에는 비어있음)
        assertTrue("Waste should be empty", restartedState.waste.isEmpty())
    }

    @Test
    fun restartGame_preservesSeed_notRandomSeed() {
        val engine = GameEngine()
        val originalSeed = 99999uL

        // 1. 원본 시드로 시작
        engine.startGame(originalSeed)
        val firstState = engine.getGameState()

        // 2. 몇 가지 이동
        engine.draw()
        engine.draw()

        // 3. 재시작 (내부적으로 현재 시드를 사용해야 함)
        val currentSeed = engine.getSeed()
        assertEquals("Seed should be preserved", originalSeed, currentSeed)
        
        engine.startGame(currentSeed, engine.getRules())
        val restartedState = engine.getGameState()

        // 4. 첫 시작과 재시작이 동일한 배치인지 확인
        assertEquals("Tableau should match after restart",
            firstState.tableau.size, restartedState.tableau.size)
        
        firstState.tableau.forEachIndexed { colIdx, firstColumn ->
            val restartedColumn = restartedState.tableau[colIdx]
            assertEquals("Column $colIdx should have same size",
                firstColumn.size, restartedColumn.size)
            
            firstColumn.forEachIndexed { cardIdx, firstCard ->
                assertEquals("Card at [$colIdx][$cardIdx] should match",
                    firstCard, restartedColumn[cardIdx])
            }
        }
    }

    @Test
    fun restartGame_differentSeed_differentLayout() {
        val engine = GameEngine()
        val seed1 = 111uL
        val seed2 = 222uL

        // 1. 첫 번째 시드로 시작
        engine.startGame(seed1)
        val state1 = engine.getGameState()

        // 2. 두 번째 시드로 시작 (다른 게임)
        engine.startGame(seed2)
        val state2 = engine.getGameState()

        // 3. 배치가 달라야 함
        var hasDifference = false
        for (colIdx in 0..6) {
            val col1 = state1.tableau[colIdx]
            val col2 = state2.tableau[colIdx]
            
            if (col1.size != col2.size) {
                hasDifference = true
                break
            }
            
            for (cardIdx in col1.indices) {
                if (col1[cardIdx] != col2[cardIdx]) {
                    hasDifference = true
                    break
                }
            }
            if (hasDifference) break
        }

        assertTrue("Different seeds should produce different layouts", hasDifference)
    }

    @Test
    fun restartGame_afterManyMoves_resetsToInitial() {
        val engine = GameEngine()
        val seed = 54321uL

        // 1. 게임 시작 및 초기 상태 저장
        engine.startGame(seed)
        val initialLayoutId = engine.getLayoutId()
        val initialTableauTopCards = engine.getGameState().tableau.map { 
            it.lastOrNull()?.copy() 
        }

        // 2. 많은 이동 수행
        repeat(10) { engine.draw() }
        
        // Tableau에서 이동 가능한 카드 찾아서 이동 시도
        for (fromCol in 0..6) {
            for (toCol in 0..6) {
                if (fromCol != toCol) {
                    engine.moveTableauToTableau(fromCol, toCol)
                }
            }
        }

        // 상태가 변경되었는지 확인
        val changedState = engine.getGameState()
        assertTrue("Some changes should have occurred",
            changedState.waste.isNotEmpty() || 
            changedState.stock.size < 24)

        // 3. 재시작
        engine.startGame(seed, engine.getRules())
        val restartedLayoutId = engine.getLayoutId()
        val restartedState = engine.getGameState()

        // 4. 검증
        assertEquals("Layout ID should match initial layout",
            initialLayoutId, restartedLayoutId)
        
        assertEquals("Stock should be back to 24", 24, restartedState.stock.size)
        assertTrue("Waste should be empty", restartedState.waste.isEmpty())
        
        // Tableau 맨 위 카드들도 동일해야 함
        restartedState.tableau.forEachIndexed { idx, column ->
            assertEquals("Top card of column $idx should match",
                initialTableauTopCards[idx], column.lastOrNull())
        }
    }

    @Test
    fun restartGame_preservesRules() {
        val engine = GameEngine()
        val seed = 777uL
        val customRules = Ruleset(
            draw = 3,
            redeals = 2,
            recycle = RecycleOrder.KEEP,
            allowFoundationToTableau = false
        )

        // 1. 커스텀 규칙으로 시작
        engine.startGame(seed, customRules)
        val initialRules = engine.getRules()

        // 2. 이동 후 재시작
        engine.draw()
        engine.startGame(seed, engine.getRules())

        // 3. 규칙이 유지되는지 확인
        val restartedRules = engine.getRules()
        assertEquals("Draw count should be preserved", 
            initialRules.draw, restartedRules.draw)
        assertEquals("Redeals should be preserved",
            initialRules.redeals, restartedRules.redeals)
        assertEquals("Recycle order should be preserved",
            initialRules.recycle, restartedRules.recycle)
        assertEquals("Foundation to tableau rule should be preserved",
            initialRules.allowFoundationToTableau, restartedRules.allowFoundationToTableau)
    }
}
