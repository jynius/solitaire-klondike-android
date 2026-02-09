package us.jyni.game

import org.junit.Assert.*
import org.junit.Test
import us.jyni.game.klondike.engine.GameEngine
import us.jyni.game.klondike.util.sync.Ruleset

/**
 * 이동 수 정확성 테스트
 * 
 * 게임 중 이동 수와 저장되는 이동 수가 일치하는지 확인
 * 특히 Stock draw가 제대로 카운트되는지 검증
 */
class MoveCountAccuracyTest {
    
    @Test
    fun `Stock draw should increment move count`() {
        val engine = GameEngine()
        engine.startGame(seed = 12345uL, rules = Ruleset(draw = 1))
        
        val initialMoves = engine.getMoveCount()
        assertEquals("초기 이동 수는 0", 0, initialMoves)
        
        // Stock에서 카드 뽑기
        val drawn = engine.draw()
        assertTrue("카드가 뽑혀야 함", drawn > 0)
        
        val afterDraw = engine.getMoveCount()
        assertEquals("Stock draw 후 이동 수가 1 증가해야 함", initialMoves + 1, afterDraw)
    }
    
    @Test
    fun `Multiple draws should increment move count correctly`() {
        val engine = GameEngine()
        engine.startGame(seed = 12345uL, rules = Ruleset(draw = 3))
        
        val initialMoves = engine.getMoveCount()
        
        // 5번 뽑기
        repeat(5) {
            engine.draw()
        }
        
        val afterDraws = engine.getMoveCount()
        assertEquals("5번 뽑으면 이동 수가 5 증가해야 함", initialMoves + 5, afterDraws)
    }
    
    @Test
    fun `Tableau moves should increment move count`() {
        val engine = GameEngine()
        engine.startGame(seed = 12345uL)
        
        val initialMoves = engine.getMoveCount()
        
        // Tableau 이동 시도 (가능한 이동 찾기)
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
            val afterMove = engine.getMoveCount()
            assertEquals("Tableau 이동 후 이동 수가 1 증가해야 함", initialMoves + 1, afterMove)
        }
    }
    
    @Test
    fun `Mixed moves should all be counted`() {
        val engine = GameEngine()
        engine.startGame(seed = 12345uL, rules = Ruleset(draw = 1))
        
        val initialMoves = engine.getMoveCount()
        var expectedMoves = 0
        
        // Draw 3번
        repeat(3) {
            if (engine.draw() > 0) {
                expectedMoves++
            }
        }
        
        // Tableau 이동 시도
        for (from in 0..6) {
            for (to in 0..6) {
                if (from != to && engine.moveTableauToTableau(from, to)) {
                    expectedMoves++
                    break
                }
            }
        }
        
        // Waste to Foundation 시도
        for (f in 0..3) {
            if (engine.moveWasteToFoundation(f)) {
                expectedMoves++
                break
            }
        }
        
        val finalMoves = engine.getMoveCount()
        assertEquals("모든 이동이 카운트되어야 함", initialMoves + expectedMoves, finalMoves)
    }
    
    @Test
    fun `Saved stats should have correct move count`() {
        val engine = GameEngine()
        engine.startGame(seed = 99999uL, rules = Ruleset(draw = 1))
        
        // 여러 이동 수행
        var actualMoves = 0
        
        // Draw 5번
        repeat(5) {
            if (engine.draw() > 0) {
                actualMoves++
            }
        }
        
        // Tableau 이동 3번 시도
        var tableauMoves = 0
        for (from in 0..6) {
            if (tableauMoves >= 3) break
            for (to in 0..6) {
                if (from != to && engine.moveTableauToTableau(from, to)) {
                    actualMoves++
                    tableauMoves++
                    break
                }
            }
        }
        
        // Engine의 moveCount와 실제 이동 수가 일치하는지 확인
        assertEquals("Engine moveCount가 실제 이동 수와 일치해야 함", 
            actualMoves, engine.getMoveCount())
        
        // Stats snapshot 확인
        val stats = engine.getSolveStatsSnapshot("resign")
        assertEquals("저장되는 Stats의 moveCount가 정확해야 함", 
            actualMoves, stats.moveCount)
    }
    
    @Test
    fun `Stock draw with draw=3 should count as one move`() {
        val engine = GameEngine()
        engine.startGame(seed = 12345uL, rules = Ruleset(draw = 3))
        
        val before = engine.getMoveCount()
        
        // draw=3이지만 한 번의 draw() 호출
        engine.draw()
        
        val after = engine.getMoveCount()
        assertEquals("draw=3이어도 한 번의 이동으로 카운트", before + 1, after)
    }
}
