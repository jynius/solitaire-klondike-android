package us.jyni.game

import org.junit.Assert.*
import org.junit.Test
import us.jyni.game.klondike.engine.GameEngine
import us.jyni.game.klondike.util.sync.Ruleset

/**
 * moveCount 정확성 테스트
 * GameEngine이 모든 이동(draw, tableau 이동, waste 이동 등)을 정확하게 카운트하는지 확인
 */
class MoveCountTest {
    
    @Test
    fun `draw 호출 시 moveCount 증가`() {
        val engine = GameEngine()
        engine.startGame(seed = 12345uL, rules = Ruleset(draw = 1))
        
        assertEquals("초기 moveCount는 0", 0, engine.getMoveCount())
        
        // Draw 3번
        engine.draw()
        assertEquals("1번 draw 후 moveCount = 1", 1, engine.getMoveCount())
        
        engine.draw()
        assertEquals("2번 draw 후 moveCount = 2", 2, engine.getMoveCount())
        
        engine.draw()
        assertEquals("3번 draw 후 moveCount = 3", 3, engine.getMoveCount())
    }
    
    @Test
    fun `draw3 규칙에서도 draw 1회당 moveCount 1 증가`() {
        val engine = GameEngine()
        engine.startGame(seed = 12345uL, rules = Ruleset(draw = 3))
        
        assertEquals("초기 moveCount는 0", 0, engine.getMoveCount())
        
        engine.draw()  // 3장 뽑기
        assertEquals("draw=3이지만 moveCount는 1 증가", 1, engine.getMoveCount())
        
        engine.draw()
        assertEquals("두 번째 draw 후 moveCount = 2", 2, engine.getMoveCount())
    }
    
    @Test
    fun `tableau 이동 시 moveCount 증가`() {
        val engine = GameEngine()
        engine.startGame(seed = 12345uL)
        
        assertEquals("초기 moveCount는 0", 0, engine.getMoveCount())
        
        // 이동 가능한 조합 찾기
        var moved = false
        for (from in 0..6) {
            for (to in 0..6) {
                if (from != to && engine.moveTableauToTableau(from, to)) {
                    moved = true
                    break
                }
            }
            if (moved) break
        }
        
        if (moved) {
            assertEquals("tableau 이동 후 moveCount = 1", 1, engine.getMoveCount())
        }
    }
    
    @Test
    fun `여러 종류 이동의 moveCount 누적`() {
        val engine = GameEngine()
        engine.startGame(seed = 99999uL)
        
        var expectedCount = 0
        assertEquals("초기 moveCount는 0", expectedCount, engine.getMoveCount())
        
        // Draw 5번
        repeat(5) {
            engine.draw()
            expectedCount++
        }
        assertEquals("5번 draw 후 moveCount = $expectedCount", expectedCount, engine.getMoveCount())
        
        // Tableau 이동 시도 (성공하면 카운트 증가)
        for (from in 0..6) {
            for (to in 0..6) {
                if (from != to && engine.moveTableauToTableau(from, to)) {
                    expectedCount++
                    break
                }
            }
        }
        
        assertEquals("이동 후 moveCount = $expectedCount", expectedCount, engine.getMoveCount())
        
        // Waste to Foundation 시도
        for (f in 0..3) {
            if (engine.moveWasteToFoundation(f)) {
                expectedCount++
                break
            }
        }
        
        assertEquals("최종 moveCount = $expectedCount", expectedCount, engine.getMoveCount())
        assertTrue("최소 5번 이상 이동했어야 함", expectedCount >= 5)
    }
    
    @Test
    fun `실제 게임 플레이 시나리오 - moveCount 정확성`() {
        val engine = GameEngine()
        val seed = 54321uL
        engine.startGame(seed = seed, rules = Ruleset(draw = 1))
        
        var count = 0
        
        // 1. Draw 10번
        repeat(10) {
            val drawn = engine.draw()
            if (drawn > 0) count++
        }
        assertEquals("10번 draw 후", count, engine.getMoveCount())
        
        // 2. Tableau 이동 시도 (최대 5번)
        var tableauMoves = 0
        for (i in 0..4) {
            var moved = false
            for (from in 0..6) {
                for (to in 0..6) {
                    if (from != to && engine.moveTableauToTableau(from, to)) {
                        tableauMoves++
                        count++
                        moved = true
                        break
                    }
                }
                if (moved) break
            }
        }
        assertEquals("tableau 이동 후", count, engine.getMoveCount())
        
        // 3. Waste to Tableau 시도 (최대 3번)
        var wasteMoves = 0
        for (i in 0..2) {
            var moved = false
            for (col in 0..6) {
                if (engine.moveWasteToTableau(col)) {
                    wasteMoves++
                    count++
                    moved = true
                    break
                }
            }
            if (moved) {
                // Waste가 비었으면 다시 draw
                if (engine.getGameState().waste.isEmpty()) {
                    val drawn = engine.draw()
                    if (drawn > 0) count++
                }
            } else {
                break
            }
        }
        
        assertEquals("모든 이동 후 최종 moveCount", count, engine.getMoveCount())
        
        // 로그 출력
        println("=== 게임 플레이 통계 ===")
        println("Draw: 10+α회")
        println("Tableau 이동: ${tableauMoves}회")
        println("Waste 이동: ${wasteMoves}회")
        println("총 moveCount: ${engine.getMoveCount()}")
        println("예상 count: $count")
    }
    
    @Test
    fun `실패한 이동은 moveCount에 포함되지 않음`() {
        val engine = GameEngine()
        engine.startGame(seed = 11111uL)
        
        val initialCount = engine.getMoveCount()
        
        // 불가능한 이동 시도 (같은 컬럼으로 이동)
        val moved = engine.moveTableauToTableau(0, 0)
        assertFalse("같은 컬럼으로는 이동 불가", moved)
        
        assertEquals("실패한 이동은 카운트 안됨", initialCount, engine.getMoveCount())
    }
    
    @Test
    fun `게임 재시작 시 moveCount 초기화`() {
        val engine = GameEngine()
        val seed = 77777uL
        
        engine.startGame(seed)
        
        // 몇 가지 이동
        engine.draw()
        engine.draw()
        engine.draw()
        
        assertTrue("이동 후 moveCount > 0", engine.getMoveCount() > 0)
        
        // 재시작
        engine.startGame(seed)
        
        assertEquals("재시작 후 moveCount = 0", 0, engine.getMoveCount())
    }
}
