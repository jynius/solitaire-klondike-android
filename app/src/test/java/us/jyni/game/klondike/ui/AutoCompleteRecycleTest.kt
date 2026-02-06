package us.jyni.game.klondike.ui

import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Recycle 무한 루프 방지 테스트
 * 
 * 문제: Stock 6장 → Draw 6번 → Recycle → Stock 6장 → Draw 6번... 무한 반복
 * 해결: Recycle 후 Waste를 사용하지 않고 두 번째 recycle이 발생하면 중단
 */
class AutoCompleteRecycleTest {
    
    private lateinit var viewModel: GameViewModel
    
    @Before
    fun setup() {
        viewModel = GameViewModel()
    }
    
    @Test
    fun `test_recycle_무한루프_방지`() {
        println("=== Recycle 무한 루프 방지 테스트 ===\n")
        
        // 다양한 시드로 테스트
        var foundRecycleLoop = false
        
        for (seed in 1UL..100UL) {
            viewModel.startGame(seed)
            
            // 게임을 조금 진행
            repeat(10) { viewModel.draw() }
            
            val beforeState = viewModel.state.value
            val beforeStock = beforeState.stock.size
            val beforeWaste = beforeState.waste.size
            val beforeFoundation = beforeState.foundation.sumOf { it.size }
            
            // Stock + Waste가 있어야 recycle 가능
            if (beforeStock + beforeWaste == 0) continue
            
            val moves = viewModel.autoComplete()
            
            val afterState = viewModel.state.value
            val afterFoundation = afterState.foundation.sumOf { it.size }
            
            // Recycle 루프 상황: moves > 0이지만 Foundation 변화 없음
            if (moves > 0 && afterFoundation == beforeFoundation) {
                println("Seed $seed:")
                println("  Before: Stock=$beforeStock, Waste=$beforeWaste, Foundation=$beforeFoundation")
                println("  Moves: $moves")
                println("  After: Stock=${afterState.stock.size}, Waste=${afterState.waste.size}, Foundation=$afterFoundation")
                
                // 이제 중단되어야 함
                val maxAllowed = (beforeStock + beforeWaste) * 3
                assertTrue(
                    "Seed $seed: $moves 번 이동 후 중단되었지만, moves가 너무 큼 (Stock+Waste=${beforeStock + beforeWaste})",
                    moves <= maxAllowed  // 최대 3 사이클 정도
                )
                
                foundRecycleLoop = true
                
                // 한 번 더 autoComplete - 이제는 0이어야 함
                val moves2 = viewModel.autoComplete()
                println("  두 번째 autoComplete: $moves2 moves")
                assertEquals("두 번째 autoComplete는 이동 없이 즉시 중단되어야 함", 0, moves2)
                
                if (foundRecycleLoop) break
            }
        }
        
        if (foundRecycleLoop) {
            println("\n✓ Recycle 무한 루프 방지 확인됨")
        } else {
            println("\n⚠ Recycle 루프 상황을 찾지 못했습니다 (1~100 seed)")
        }
    }
    
    @Test
    fun `test_waste_사용_후_recycle은_허용`() {
        println("=== Waste 사용 후 Recycle은 허용되어야 함 ===\n")
        
        viewModel.startGame(54321UL)
        
        // 게임 진행
        repeat(5) { viewModel.draw() }
        
        val beforeFoundation = viewModel.state.value.foundation.sumOf { it.size }
        
        val moves = viewModel.autoComplete()
        
        val afterFoundation = viewModel.state.value.foundation.sumOf { it.size }
        
        println("Moves: $moves")
        println("Foundation: $beforeFoundation → $afterFoundation")
        
        // Foundation이 증가했다면 정상 진행
        if (afterFoundation > beforeFoundation) {
            println("✓ Waste를 사용하면서 정상 진행됨")
            assertTrue("Waste를 사용한 경우 충분히 진행되어야 함", moves >= 10)
        }
    }
    
    @Test
    fun `test_stock만_6장_반복_문제`() {
        println("=== Stock 6장 반복 문제 재현 ===\n")
        
        // 사용자가 보고한 상황: Stock 6장만 남아서 계속 반복
        // 이 상황을 만들기 위한 시드 찾기
        
        for (seed in 1UL..200UL) {
            viewModel.startGame(seed)
            
            // 중간까지 진행
            repeat(15) { viewModel.draw() }
            
            val state = viewModel.state.value
            val stockSize = state.stock.size
            val wasteSize = state.waste.size
            
            // Stock이 작고 (1~10장), Waste가 어느 정도 있는 상황
            if (stockSize in 1..10 && wasteSize > 0) {
                println("Seed $seed: Stock=$stockSize, Waste=$wasteSize")
                
                val moves = viewModel.autoComplete()
                
                println("  AutoComplete moves: $moves")
                println("  Final: Stock=${viewModel.state.value.stock.size}, Waste=${viewModel.state.value.waste.size}")
                
                // 무한 루프가 아니라면 적절한 횟수에서 중단되어야 함
                val totalCards = stockSize + wasteSize
                val maxMoves = totalCards * 3
                assertTrue(
                    "Seed $seed: moves=$moves 너무 많음 (total cards=$totalCards)",
                    moves <= maxMoves
                )
                
                // 다시 실행하면 0이어야 함
                val moves2 = viewModel.autoComplete()
                assertEquals("Seed $seed: 두 번째 실행은 0이어야 함", 0, moves2)
                
                println("  ✓ 정상 중단 확인\n")
                
                // 여러 케이스 확인
                if (seed.toLong() > 50) break
            }
        }
    }
    
    @Test
    fun `test_정확히_6장_반복_케이스`() {
        println("=== 정확히 Stock 6장 반복 케이스 ===\n")
        
        // Stock 6장, Waste 비어있거나 사용 불가한 상황
        for (seed in 1UL..300UL) {
            viewModel.startGame(seed)
            
            // 적절히 진행
            repeat(20) { viewModel.draw() }
            
            val state = viewModel.state.value
            
            // Stock이 정확히 6장 (또는 작은 수)
            if (state.stock.size in 3..8 && state.waste.size > 0) {
                val beforeFoundation = state.foundation.sumOf { it.size }
                
                val moves = viewModel.autoComplete()
                
                val afterFoundation = viewModel.state.value.foundation.sumOf { it.size }
                
                if (moves > 0 && afterFoundation == beforeFoundation) {
                    println("발견! Seed $seed:")
                    println("  Stock=${state.stock.size}, Waste=${state.waste.size}")
                    println("  Moves=$moves (Foundation 변화 없음)")
                    println("  이것이 사용자가 보고한 '${moves}장의 카드를 자동으로 이동' 메시지!")
                    
                    // Stock 크기보다 많이 이동하면 안 됨 (최대 2 사이클)
                    val maxAllowedMoves = (state.stock.size + state.waste.size) * 2
                    val isWithinLimit = moves <= maxAllowedMoves
                    assertTrue(
                        "Recycle 루프: moves=$moves 너무 많음 (max=$maxAllowedMoves)",
                        isWithinLimit
                    )
                    
                    println("  ✓ Recycle 2회 이내에 중단됨\n")
                    break
                }
            }
        }
    }
}
