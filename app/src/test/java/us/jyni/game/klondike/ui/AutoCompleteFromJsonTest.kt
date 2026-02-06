package us.jyni.game.klondike.ui

import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * 실제 디바이스에서 발생한 문제를 JSON 상태로 재현하는 테스트
 * 
 * 사용법:
 * 1. 디바이스에서 "JSON복사" 버튼 클릭
 * 2. 복사된 JSON을 이 테스트의 json 변수에 붙여넣기
 * 3. 테스트 실행해서 문제 재현 및 검증
 */
class AutoCompleteFromJsonTest {
    
    private lateinit var viewModel: GameViewModel
    
    @Before
    fun setup() {
        viewModel = GameViewModel()
    }
    
    @Test
    fun `test_디바이스_상태_재현_템플릿`() {
        // TODO: 디바이스에서 "JSON복사" 버튼을 눌러 얻은 JSON을 여기에 붙여넣기
        val json = """
            {
                "placeholder": "디바이스에서 복사한 JSON을 여기에 붙여넣으세요"
            }
        """.trimIndent()
        
        // JSON이 실제 데이터인지 확인
        if (json.contains("placeholder")) {
            println("⚠ 아직 디바이스 JSON이 입력되지 않았습니다")
            println("디바이스에서:")
            println("1. 문제가 발생하는 게임 상태로 이동")
            println("2. 'JSON복사' 버튼 클릭")
            println("3. 복사된 내용을 이 테스트의 json 변수에 붙여넣기")
            return // 실제 JSON이 없으면 테스트 스킵
        }
        
        // JSON으로 상태 복원
        val restored = viewModel.restoreStateString(json)
        assertTrue("JSON 복원 실패", restored)
        
        val beforeState = viewModel.state.value
        println("=== 복원된 상태 ===")
        println("Stock: ${beforeState.stock.size}, Waste: ${beforeState.waste.size}")
        println("Foundation: ${beforeState.foundation.map { it.size }}")
        
        beforeState.tableau.forEachIndexed { i, pile ->
            val faceUp = pile.count { it.isFaceUp }
            println("T[$i]: ${pile.size} cards ($faceUp face-up)")
        }
        
        // autoComplete 실행
        println("\n=== autoComplete 실행 ===")
        val moves = viewModel.autoComplete()
        
        val afterState = viewModel.state.value
        println("\n=== 결과 ===")
        println("Moves reported: $moves")
        println("Stock: ${beforeState.stock.size} → ${afterState.stock.size}")
        println("Waste: ${beforeState.waste.size} → ${afterState.waste.size}")
        println("Foundation: ${beforeState.foundation.sumOf { it.size }} → ${afterState.foundation.sumOf { it.size }}")
        
        // 분석: moves > 0인데 실제로는 아무것도 안 변했는지 확인
        val foundationChanged = afterState.foundation.sumOf { it.size } != beforeState.foundation.sumOf { it.size }
        val tableauChanged = afterState.tableau.any { pile -> 
            val beforePile = beforeState.tableau[afterState.tableau.indexOf(pile)]
            pile.size != beforePile.size || pile.count { it.isFaceUp } != beforePile.count { it.isFaceUp }
        }
        
        println("\n=== 분석 ===")
        println("Foundation changed: $foundationChanged")
        println("Tableau changed: $tableauChanged")
        
        if (moves > 0 && !foundationChanged && !tableauChanged) {
            val stockWasteChanged = (beforeState.stock.size != afterState.stock.size) || 
                                   (beforeState.waste.size != afterState.waste.size)
            
            if (stockWasteChanged) {
                println("⚠ 경고: $moves 번 이동했다고 하지만 Draw만 반복된 것으로 보입니다!")
                fail("autoComplete가 Draw만 반복했습니다. Foundation이나 Tableau에 실제 변화가 없습니다.")
            }
        }
    }
    
    @Test
    fun `test_5장_이동_문제_재현`() {
        // 사용자가 보고한 "5장의 카드를 자동으로 이동했다"는 메시지가 반복되는 문제
        // 실제로는 이동하지 않는 상황을 재현
        
        println("=== 5장 이동 메시지 반복 문제 테스트 ===")
        println("\n이 테스트는 디바이스에서 복사한 JSON이 필요합니다.")
        println("현재는 여러 시드로 유사한 상황을 찾습니다.\n")
        
        var found = false
        
        for (seed in 1UL..100UL) {
            viewModel.startGame(seed)
            
            // 몇 번 진행
            repeat(10) { viewModel.draw() }
            
            val beforeState = viewModel.state.value
            val beforeFoundation = beforeState.foundation.sumOf { it.size }
            val beforeStock = beforeState.stock.size
            val beforeWaste = beforeState.waste.size
            
            val moves = viewModel.autoComplete()
            
            val afterState = viewModel.state.value
            val afterFoundation = afterState.foundation.sumOf { it.size }
            val afterStock = afterState.stock.size
            val afterWaste = afterState.waste.size
            
            // 조건: moves가 3~7 사이이고, Foundation 변화가 없으면
            if (moves in 3..7 && afterFoundation == beforeFoundation) {
                val stockWasteChanged = (beforeStock != afterStock) || (beforeWaste != afterWaste)
                
                if (stockWasteChanged) {
                    println("✓ 유사한 상황 발견 (seed=$seed)")
                    println("  Moves: $moves")
                    println("  Foundation: $beforeFoundation → $afterFoundation (변화 없음)")
                    println("  Stock/Waste: 변화 있음 (Draw만 반복)")
                    println("  → 이것이 사용자가 보고한 문제와 유사합니다!")
                    found = true
                    
                    // 한 번 더 autoComplete 실행
                    val moves2 = viewModel.autoComplete()
                    println("  두 번째 autoComplete: $moves2 moves")
                    
                    if (moves2 == moves) {
                        println("  ⚠ 같은 횟수 반복! 무한 루프 가능성!")
                    }
                    
                    break
                }
            }
        }
        
        if (!found) {
            println("⚠ 유사한 상황을 찾지 못했습니다.")
            println("디바이스에서 'JSON복사'로 정확한 상태를 가져와 테스트하세요.")
        }
    }
    
    @Test
    fun `test_autoComplete_반환값_검증`() {
        println("=== autoComplete 반환값 검증 ===\n")
        
        viewModel.startGame(12345UL)
        repeat(5) { viewModel.draw() }
        
        val beforeState = viewModel.state.value
        val beforeFoundation = beforeState.foundation.sumOf { it.size }
        val beforeTableauFaceUp = beforeState.tableau.sumOf { it.count { c -> c.isFaceUp } }
        
        println("Before:")
        println("  Foundation: $beforeFoundation")
        println("  Tableau face-up: $beforeTableauFaceUp")
        
        val moves = viewModel.autoComplete()
        
        val afterState = viewModel.state.value
        val afterFoundation = afterState.foundation.sumOf { it.size }
        val afterTableauFaceUp = afterState.tableau.sumOf { it.count { c -> c.isFaceUp } }
        
        println("\nAfter $moves moves:")
        val foundationDiff = afterFoundation - beforeFoundation
        val tableauDiff = afterTableauFaceUp - beforeTableauFaceUp
        println("  Foundation: $afterFoundation (${if (foundationDiff >= 0) "+" else ""}$foundationDiff)")
        println("  Tableau face-up: $afterTableauFaceUp (${if (tableauDiff >= 0) "+" else ""}$tableauDiff)")
        
        // 검증: moves > 0이면 실제로 게임이 진행되어야 함
        if (moves > 0) {
            val actualProgress = (afterFoundation > beforeFoundation) || 
                                (afterTableauFaceUp > beforeTableauFaceUp)
            
            assertTrue(
                "autoComplete가 $moves 번 이동했다고 하지만 실제 진행이 없습니다",
                actualProgress
            )
        }
    }
}
