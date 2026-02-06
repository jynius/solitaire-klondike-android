package us.jyni.game.klondike.ui

import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Recycle 로직 디버깅을 위한 간단한 테스트
 */
class RecycleDebugTest {
    
    private lateinit var viewModel: GameViewModel
    
    @Before
    fun setup() {
        viewModel = GameViewModel()
    }
    
    @Test
    fun `test_recycle_한_번만_허용`() {
        println("=== Recycle 한 번만 허용 테스트 ===\n")
        
        viewModel.startGame(1UL)
        
        // 15번 draw해서 중간 상태로 만들기
        repeat(15) { viewModel.draw() }
        
        val before = viewModel.state.value
        val beforeFoundation = before.foundation.sumOf { it.size }
        
        println("Before autoComplete:")
        println("  Stock: ${before.stock.size}")
        println("  Waste: ${before.waste.size}")
        println("  Foundation: $beforeFoundation")
        println()
        
        // autoComplete 실행
        val moves1 = viewModel.autoComplete()
        
        val after1 = viewModel.state.value
        val after1Foundation = after1.foundation.sumOf { it.size }
        
        println("After 1st autoComplete ($moves1 moves):")
        println("  Stock: ${after1.stock.size}")
        println("  Waste: ${after1.waste.size}")
        println("  Foundation: $after1Foundation")
        println()
        
        // 두 번째 autoComplete - 0이어야 함
        println("Running 2nd autoComplete...")
        val moves2 = viewModel.autoComplete()
        
        val after2 = viewModel.state.value
        val after2Foundation = after2.foundation.sumOf { it.size }
        
        println("After 2nd autoComplete ($moves2 moves):")
        println("  Stock: ${after2.stock.size}")
        println("  Waste: ${after2.waste.size}")
        println("  Foundation: $after2Foundation")
        println("  Expected: 0 (no more moves)")
        println()
        
        if (moves2 == 0) {
            println("✅ 성공: 두 번째 autoComplete가 0번 이동 (Recycle 무한 루프 방지 성공!)")
        } else {
            println("⚠ 경고: 두 번째 autoComplete가 $moves2 번 이동했습니다!")
            println("이것은 Recycle 무한 루프가 발생한 것입니다.")
        }
        
        assertEquals("두 번째 autoComplete는 0이어야 함 (더 이상 진행 불가)", 0, moves2)
    }
    
    @Test
    fun `test_여러_시드로_반복_테스트`() {
        println("=== 여러 시드로 반복 autoComplete 테스트 ===\n")
        
        for (seed in listOf(1UL, 12345UL, 54321UL, 99999UL)) {
            viewModel.startGame(seed)
            
            repeat(10) { viewModel.draw() }
            
            val moves1 = viewModel.autoComplete()
            val foundation1 = viewModel.state.value.foundation.sumOf { it.size }
            
            val moves2 = viewModel.autoComplete()
            val foundation2 = viewModel.state.value.foundation.sumOf { it.size }
            
            println("Seed $seed:")
            println("  1st: $moves1 moves, Foundation=$foundation1")
            println("  2nd: $moves2 moves, Foundation=$foundation2")
            
            assertEquals("Seed $seed: 두 번째 autoComplete는 0이어야 함", 0, moves2)
            assertEquals("Seed $seed: Foundation이 변경되지 않아야 함", foundation1, foundation2)
            println()
        }
    }
}
