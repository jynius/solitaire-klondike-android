package us.jyni.game.klondike.solver

import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import us.jyni.game.klondike.engine.GameEngine
import us.jyni.game.klondike.util.sync.Ruleset

/**
 * State Cycle 검출 테스트
 * 
 * State Cycle은 게임 진행 중 이전에 방문한 상태로 돌아오는 무한 루프를 감지합니다.
 */
class StateCycleDetectionTest {

    private lateinit var engine: GameEngine
    private lateinit var detector: UnsolvableDetector

    @Before
    fun setup() {
        engine = GameEngine()
        detector = UnsolvableDetector(engine)
    }

    @Test
    fun `test new game - no cycle`() {
        // 게임 시작
        engine.startGame(seed = 12345uL, rules = Ruleset())
        
        // Cycle 체크 - 첫 상태이므로 cycle 없어야 함
        // checkStateCycle()이 내부적으로 recordCurrentState()를 호출
        val cycle = detector.checkStateCycle()
        assertNull("첫 상태에서는 Cycle이 없어야 함", cycle)
        
        // 히스토리에 상태가 기록되었는지 확인
        assertEquals("상태가 히스토리에 기록되어야 함", 1, engine.getStateHistory().size)
    }

    @Test
    fun `test different states - no cycle`() {
        // 게임 시작
        engine.startGame(seed = 12345uL, rules = Ruleset())
        
        // 첫 상태 체크 (히스토리에 추가됨)
        val cycle1 = detector.checkStateCycle()
        assertNull("첫 상태에서는 Cycle이 없어야 함", cycle1)
        
        // 이동 수행 - 상태 변경
        engine.draw()
        
        // 새로운 상태 체크 - 다른 상태이므로 cycle 없어야 함
        val cycle2 = detector.checkStateCycle()
        assertNull("다른 상태에서는 Cycle이 없어야 함", cycle2)
        
        // 두 개의 서로 다른 상태가 히스토리에 있어야 함
        assertEquals("두 개의 다른 상태가 기록되어야 함", 2, engine.getStateHistory().size)
    }

    @Test
    fun `test cycle detection - move and undo`() {
        // 게임 시작
        engine.startGame(seed = 12345uL, rules = Ruleset())
        
        // 첫 상태 기록
        engine.recordCurrentState()
        
        // 이동 수행
        val moved = engine.draw()
        if (moved > 0) {
            // 이동 후 상태 기록
            engine.recordCurrentState()
            
            // Undo로 원래 상태로 돌아감
            engine.undo()
            
            // 원래 상태 다시 기록 시도 - Cycle 감지되어야 함
            val cycle = detector.checkStateCycle()
            
            assertNotNull("Undo로 돌아간 상태는 Cycle이어야 함", cycle)
            assertTrue("Cycle 타입이어야 함", cycle is UnsolvableReason.StateCycle)
        }
    }

    @Test
    fun `test multiple different moves - no cycle`() {
        // 게임 시작
        engine.startGame(seed = 12345uL, rules = Ruleset())
        
        // 여러 이동 수행하며 상태 기록
        for (i in 1..5) {
            val cycle = detector.checkStateCycle()
            assertNull("이동 $i: 다른 경로는 Cycle이 없어야 함", cycle)
            
            // 다음 이동
            engine.draw()
        }
    }

    @Test
    fun `test state history cleared on new game`() {
        // 첫 게임
        engine.startGame(seed = 12345uL, rules = Ruleset())
        engine.recordCurrentState()
        engine.draw()
        engine.recordCurrentState()
        
        val history1 = engine.getStateHistory()
        assertTrue("히스토리가 있어야 함", history1.isNotEmpty())
        
        // 새 게임 시작
        engine.startGame(seed = 99999uL, rules = Ruleset())
        
        val history2 = engine.getStateHistory()
        assertTrue("새 게임에서는 히스토리가 비어있어야 함", history2.isEmpty())
        
        // 새 게임에서 첫 상태 기록
        val isNew = engine.recordCurrentState()
        assertTrue("새 게임의 첫 상태는 새로워야 함", isNew)
    }

    @Test
    fun `test checkUnwinnableState includes cycle detection`() {
        // 게임 시작
        engine.startGame(seed = 12345uL, rules = Ruleset())
        
        // 첫 상태 기록
        var unwinnable = detector.checkUnwinnableState(engine.getGameState())
        assertNull("첫 상태는 Unwinnable이 아님", unwinnable)
        
        // 이동
        engine.draw()
        unwinnable = detector.checkUnwinnableState(engine.getGameState())
        assertNull("다른 상태는 Unwinnable이 아님", unwinnable)
        
        // Undo로 돌아감
        engine.undo()
        unwinnable = detector.checkUnwinnableState(engine.getGameState())
        
        // Cycle 감지되어야 함
        assertNotNull("Cycle은 Unwinnable State", unwinnable)
        assertTrue("StateCycle 타입이어야 함", unwinnable is UnsolvableReason.StateCycle)
    }
}
