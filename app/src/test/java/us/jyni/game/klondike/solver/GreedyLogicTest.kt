package us.jyni.game.klondike.solver

import org.junit.Test
import us.jyni.game.klondike.engine.GameEngine
import us.jyni.game.klondike.engine.KlondikeRules
import us.jyni.game.klondike.model.Card
import us.jyni.game.klondike.model.Rank
import us.jyni.game.klondike.model.Suit

class GreedyLogicTest {
    
    @Test
    fun `test foundation move detection`() {
        val engine = GameEngine()
        engine.startGame(seed = 1uL)
        val state = engine.getGameState()
        
        val solver = GreedySolver()
        val rules = KlondikeRules()
        
        println("=== Testing Foundation Move Detection ===")
        
        // Tableau 확인
        state.tableau.forEachIndexed { col, pile ->
            if (pile.isNotEmpty()) {
                val top = pile.last()
                println("T[$col] top: $top (isFaceUp=${top.isFaceUp})")
                
                // 이 카드를 foundation으로 옮길 수 있나?
                for (f in 0..3) {
                    val canMove = rules.canMoveTableauToFoundation(pile, state.foundation[f])
                    if (canMove) {
                        println("  ✓ Can move to F[$f]")
                    }
                }
            }
        }
        
        // Solver가 찾는 move
        val foundationMove = solver.findBestMove(state)
        println("\nSolver found: $foundationMove")
        
        // 실제로 이동 가능한지 확인
        if (foundationMove != null) {
            val newState = GameStateUtils.applyMove(state, foundationMove)
            if (newState != null) {
                println("✓ Move applied successfully")
                println("Foundation after: ${newState.foundation.map { it.size }}")
            } else {
                println("❌ Move FAILED!")
            }
        }
    }
    
    @Test
    fun `test if Ace is detected`() {
        val engine = GameEngine()
        engine.startGame(seed = 1uL)
        var state = engine.getGameState()
        
        println("=== Searching for ACE ===")
        
        // Tableau에서 Ace 찾기
        state.tableau.forEachIndexed { col, pile ->
            pile.forEachIndexed { idx, card ->
                if (card.rank == Rank.ACE) {
                    println("Found ACE at T[$col][$idx]: $card (isFaceUp=${card.isFaceUp})")
                }
            }
        }
        
        // Stock에서 Ace 찾기
        state.stock.forEachIndexed { idx, card ->
            if (card.rank == Rank.ACE) {
                println("Found ACE in Stock[$idx]: $card")
            }
        }
        
        val solver = GreedySolver()
        val rules = KlondikeRules()
        
        // 몇 번 이동해서 Ace를 찾을 때까지
        var drawCount = 0
        var foundationMoveCount = 0
        var tableauMoveCount = 0
        var wasteMoveCount = 0
        
        for (step in 1..100) {
            val move = solver.findBestMove(state)
            if (move == null) {
                println("\nStep $step: No move available - STUCK!")
                break
            }
            
            when (move) {
                is Move.Draw -> drawCount++
                is Move.TableauToFoundation, is Move.WasteToFoundation -> foundationMoveCount++
                is Move.TableauToTableau -> tableauMoveCount++
                is Move.WasteToTableau -> wasteMoveCount++
                else -> {}
            }
            
            val newState = GameStateUtils.applyMove(state, move)
            if (newState == null) {
                println("\nStep $step: Move failed!")
                break
            }
            
            state = newState
            
            // Foundation에 카드가 들어갔는지 확인
            val foundationCount = state.foundation.sumOf { it.size }
            if (foundationCount > 0 && step <= 5) {
                println("\nStep $step: Foundation has $foundationCount cards!")
                state.foundation.forEachIndexed { f, pile ->
                    if (pile.isNotEmpty()) {
                        println("  F[$f]: ${pile.map { it.toString() }}")
                    }
                }
            }
            
            if (step % 20 == 0) {
                println("Step $step: Foundation=${foundationCount}, Moves: Draw=$drawCount, Foundation=$foundationMoveCount, Tableau=$tableauMoveCount, Waste=$wasteMoveCount")
                
                // 현재 상태 확인
                println("  Current tableau tops:")
                state.tableau.forEachIndexed { col, pile ->
                    if (pile.isNotEmpty()) {
                        val top = pile.last()
                        println("    T[$col]: $top")
                    }
                }
                println("  Waste: ${state.waste.lastOrNull()}")
            }
        }
        
        println("\n=== Final Stats ===")
        println("Draw: $drawCount, Foundation: $foundationMoveCount, Tableau: $tableauMoveCount, Waste: $wasteMoveCount")
        println("Foundation cards: ${state.foundation.sumOf { it.size }}")
    }
    
    @Test
    fun `test tableau move detection`() {
        val engine = GameEngine()
        engine.startGame(seed = 1uL)
        val state = engine.getGameState()
        
        val rules = KlondikeRules()
        
        println("=== Testing Tableau Move Detection ===")
        
        var moveCount = 0
        for (fromCol in 0..6) {
            val fromPile = state.tableau[fromCol]
            if (fromPile.isEmpty()) continue
            
            val firstFaceUpIdx = fromPile.indexOfFirst { it.isFaceUp }
            if (firstFaceUpIdx == -1) continue
            
            for (cardIdx in firstFaceUpIdx until fromPile.size) {
                val movingCards = fromPile.subList(cardIdx, fromPile.size)
                val isBottom = (cardIdx == firstFaceUpIdx)
                
                for (toCol in 0..6) {
                    if (fromCol == toCol) continue
                    
                    if (rules.canMoveSequenceToTableau(movingCards, state.tableau[toCol])) {
                        println("T[$fromCol][$cardIdx] -> T[$toCol]: ${movingCards.first()} (bottom=$isBottom)")
                        moveCount++
                    }
                }
            }
        }
        
        println("\nTotal possible tableau moves: $moveCount")
        
        val solver = GreedySolver()
        val tableauMove = solver.findBestMove(state)
        println("Solver would choose: $tableauMove")
    }
}
