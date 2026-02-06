package us.jyni.game.klondike.ui

import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * 실제로는 solvable한 게임인데 autoComplete가 너무 일찍 포기하는 문제 테스트
 * 
 * 문제: "더 이상 진행할 수 없다"고 멈추지만, 수동으로 하면 완료 가능
 */
class AutoCompleteSolvableTest {
    
    private lateinit var viewModel: GameViewModel
    
    @Before
    fun setup() {
        viewModel = GameViewModel()
    }
    
    @Test
    fun `test_seed_17848904495592789619_solvable`() {
        println("=== Seed 17848904495592789619 테스트 ===\n")
        
        // 사용자가 보고한 seed
        viewModel.startGame(17848904495592789619UL)
        
        val initialState = viewModel.state.value
        println("Initial state:")
        println("  Stock: ${initialState.stock.size}")
        println("  Waste: ${initialState.waste.size}")
        println("  Foundation: ${initialState.foundation.sumOf { it.size }}")
        println()
        
        // autoComplete 실행
        val moves = viewModel.autoComplete()
        
        val afterState = viewModel.state.value
        val afterFoundation = afterState.foundation.sumOf { it.size }
        
        println("After autoComplete ($moves moves):")
        println("  Stock: ${afterState.stock.size}")
        println("  Waste: ${afterState.waste.size}")
        println("  Foundation: $afterFoundation")
        println()
        
        afterState.tableau.forEachIndexed { i, pile ->
            val faceUp = pile.count { it.isFaceUp }
            val top = pile.lastOrNull { it.isFaceUp }
            println("  T[$i]: ${pile.size} cards ($faceUp face-up), top=${top?.let { "${it.suit} ${it.rank}" } ?: "empty/hidden"}")
        }
        
        println()
        
        // 이 게임이 실제로 solvable한지 확인하기 위해
        // Foundation에 52장이 모두 들어가야 함
        if (afterFoundation == 52) {
            println("✅ 완전히 해결됨!")
        } else {
            println("⚠ Foundation: $afterFoundation/52")
            println("현재 로직으로는 $moves 번 이동 후 멈췄습니다.")
            
            // Waste 상태 확인
            if (afterState.waste.isNotEmpty()) {
                println("\nWaste cards:")
                afterState.waste.takeLast(6).forEach {
                    println("  ${it.suit} ${it.rank}")
                }
            }
        }
        
        // 목표: 이 테스트가 통과하도록 autoComplete 로직 개선
        // assertTrue("이 게임은 solvable해야 합니다", afterFoundation == 52)
    }
    
    @Test
    fun `test_다양한_seed_solvability`() {
        println("=== 다양한 Seed Solvability 테스트 ===\n")
        
        val seeds = listOf(
            17848904495592789619UL,  // 사용자 보고
            12345UL,
            54321UL,
            1UL
        )
        
        seeds.forEach { seed ->
            viewModel.startGame(seed)
            
            val before = viewModel.state.value.foundation.sumOf { it.size }
            val moves = viewModel.autoComplete()
            val after = viewModel.state.value.foundation.sumOf { it.size }
            
            println("Seed $seed:")
            println("  Moves: $moves")
            println("  Foundation: $before → $after")
            
            if (after == 52) {
                println("  ✅ Solved!")
            } else {
                println("  ⚠ Incomplete (${after}/52)")
            }
            println()
        }
    }
}
