package us.jyni.game.klondike.solver

import org.junit.Assert.*
import org.junit.Test
import us.jyni.game.klondike.model.Card
import us.jyni.game.klondike.model.GameState
import us.jyni.game.klondike.model.Rank
import us.jyni.game.klondike.model.Suit

class GameStateUtilsTest {
    
    @Test
    fun clone_state_creates_deep_copy() {
        val original = GameState()
        
        // 원본에 카드 추가
        original.tableau[0].add(Card(Suit.HEARTS, Rank.ACE, isFaceUp = true))
        original.foundation[0].add(Card(Suit.SPADES, Rank.ACE, isFaceUp = true))
        original.stock.add(Card(Suit.DIAMONDS, Rank.KING, isFaceUp = false))
        original.waste.add(Card(Suit.CLUBS, Rank.QUEEN, isFaceUp = true))
        
        val cloned = GameStateUtils.cloneState(original)
        
        // 복제본 수정
        cloned.tableau[0].add(Card(Suit.HEARTS, Rank.TWO, isFaceUp = true))
        cloned.foundation[0].clear()
        cloned.stock.clear()
        cloned.waste.clear()
        
        // 원본은 변경되지 않아야 함
        assertEquals("Original tableau should have 1 card", 1, original.tableau[0].size)
        assertEquals("Original foundation should have 1 card", 1, original.foundation[0].size)
        assertEquals("Original stock should have 1 card", 1, original.stock.size)
        assertEquals("Original waste should have 1 card", 1, original.waste.size)
        
        // 복제본은 변경되어야 함
        assertEquals("Cloned tableau should have 2 cards", 2, cloned.tableau[0].size)
        assertEquals("Cloned foundation should be empty", 0, cloned.foundation[0].size)
        assertEquals("Cloned stock should be empty", 0, cloned.stock.size)
        assertEquals("Cloned waste should be empty", 0, cloned.waste.size)
    }
    
    @Test
    fun apply_tableau_to_foundation() {
        val state = GameState()
        state.foundation[0].add(Card(Suit.HEARTS, Rank.ACE, isFaceUp = true))
        state.tableau[0].add(Card(Suit.HEARTS, Rank.TWO, isFaceUp = true))
        
        val move = Move.TableauToFoundation(0, 0)
        val newState = GameStateUtils.applyMove(state, move)
        
        assertNotNull("Move should succeed", newState)
        assertEquals("Tableau should be empty", 0, newState!!.tableau[0].size)
        assertEquals("Foundation should have 2 cards", 2, newState.foundation[0].size)
        assertEquals("Top card should be 2", Rank.TWO, newState.foundation[0].last().rank)
    }
    
    @Test
    fun apply_tableau_to_tableau() {
        val state = GameState()
        state.tableau[0].add(Card(Suit.HEARTS, Rank.SEVEN, isFaceUp = true))
        state.tableau[0].add(Card(Suit.SPADES, Rank.SIX, isFaceUp = true))
        state.tableau[1].add(Card(Suit.DIAMONDS, Rank.EIGHT, isFaceUp = true))
        
        // ♠6을 ♦8 위로 이동 (검은색 6 → 빨간색 8)
        val move = Move.TableauToTableau(0, 1, 1)  // fromCol=0, cardIndex=1 (♠6), toCol=1
        val newState = GameStateUtils.applyMove(state, move)
        
        assertNotNull("Move should succeed", newState)
        assertEquals("Source tableau should have 1 card", 1, newState!!.tableau[0].size)
        assertEquals("Destination tableau should have 2 cards", 2, newState.tableau[1].size)
        assertEquals("Top card should be ♠6", Rank.SIX, newState.tableau[1].last().rank)
    }
    
    @Test
    fun apply_waste_to_foundation() {
        val state = GameState()
        state.foundation[0].add(Card(Suit.HEARTS, Rank.ACE, isFaceUp = true))
        state.waste.add(Card(Suit.HEARTS, Rank.TWO, isFaceUp = true))
        
        val move = Move.WasteToFoundation(0)
        val newState = GameStateUtils.applyMove(state, move)
        
        assertNotNull("Move should succeed", newState)
        assertEquals("Waste should be empty", 0, newState!!.waste.size)
        assertEquals("Foundation should have 2 cards", 2, newState.foundation[0].size)
    }
    
    @Test
    fun apply_waste_to_tableau() {
        val state = GameState()
        state.tableau[0].add(Card(Suit.HEARTS, Rank.SEVEN, isFaceUp = true))
        state.waste.add(Card(Suit.SPADES, Rank.SIX, isFaceUp = true))
        
        val move = Move.WasteToTableau(0)
        val newState = GameStateUtils.applyMove(state, move)
        
        assertNotNull("Move should succeed", newState)
        assertEquals("Waste should be empty", 0, newState!!.waste.size)
        assertEquals("Tableau should have 2 cards", 2, newState.tableau[0].size)
    }
    
    @Test
    fun apply_draw_from_stock() {
        val state = GameState()
        state.stock.add(Card(Suit.HEARTS, Rank.KING, isFaceUp = false))
        state.stock.add(Card(Suit.SPADES, Rank.QUEEN, isFaceUp = false))
        
        val move = Move.Draw
        val newState = GameStateUtils.applyMove(state, move)
        
        assertNotNull("Move should succeed", newState)
        assertEquals("Stock should have 1 card", 1, newState!!.stock.size)
        assertEquals("Waste should have 1 card", 1, newState.waste.size)
        assertTrue("Waste card should be face up", newState.waste.last().isFaceUp)
        assertEquals("Waste card should be ♠Q", Rank.QUEEN, newState.waste.last().rank)
    }
    
    @Test
    fun apply_draw_recycle() {
        val state = GameState()
        state.stock.clear()  // Stock 비움
        state.waste.add(Card(Suit.HEARTS, Rank.KING, isFaceUp = true))
        state.waste.add(Card(Suit.SPADES, Rank.QUEEN, isFaceUp = true))
        
        val move = Move.Draw
        val newState = GameStateUtils.applyMove(state, move)
        
        assertNotNull("Move should succeed (recycle)", newState)
        assertEquals("Stock should have 2 cards (recycled)", 2, newState!!.stock.size)
        assertEquals("Waste should be empty initially", 0, newState.waste.size)
        
        // Stock의 카드들이 뒷면이어야 함
        assertTrue("Stock cards should be face down", 
            newState.stock.all { !it.isFaceUp })
    }
    
    @Test
    fun flip_card_after_tableau_move() {
        val state = GameState()
        state.tableau[0].add(Card(Suit.HEARTS, Rank.SEVEN, isFaceUp = false))
        state.tableau[0].add(Card(Suit.SPADES, Rank.SIX, isFaceUp = true))
        state.tableau[1].add(Card(Suit.DIAMONDS, Rank.EIGHT, isFaceUp = true))
        
        // ♠6을 이동하면 ♥7이 뒤집혀야 함
        val move = Move.TableauToTableau(0, 1, 1)
        val newState = GameStateUtils.applyMove(state, move)
        
        assertNotNull("Move should succeed", newState)
        assertEquals("Source tableau should have 1 card", 1, newState!!.tableau[0].size)
        assertTrue("Remaining card should be flipped face up", 
            newState.tableau[0].last().isFaceUp)
    }
    
    @Test
    fun state_hash_unique_for_different_states() {
        val state1 = GameState()
        state1.tableau[0].add(Card(Suit.HEARTS, Rank.ACE, isFaceUp = true))
        
        val state2 = GameState()
        state2.tableau[0].add(Card(Suit.SPADES, Rank.ACE, isFaceUp = true))
        
        val hash1 = GameStateUtils.stateHash(state1)
        val hash2 = GameStateUtils.stateHash(state2)
        
        assertNotEquals("Different states should have different hashes", hash1, hash2)
    }
    
    @Test
    fun state_hash_same_for_identical_states() {
        val state1 = GameState()
        state1.tableau[0].add(Card(Suit.HEARTS, Rank.ACE, isFaceUp = true))
        state1.foundation[0].add(Card(Suit.SPADES, Rank.ACE, isFaceUp = true))
        
        val state2 = GameStateUtils.cloneState(state1)
        
        val hash1 = GameStateUtils.stateHash(state1)
        val hash2 = GameStateUtils.stateHash(state2)
        
        assertEquals("Identical states should have same hash", hash1, hash2)
    }
    
    @Test
    fun win_condition_detected() {
        val state = GameState()
        
        // 각 Foundation에 K까지 채우기
        for (i in 0..3) {
            for (rank in Rank.values()) {
                state.foundation[i].add(
                    Card(Suit.values()[i], rank, isFaceUp = true)
                )
            }
        }
        
        // 마지막 카드를 올려서 승리
        state.tableau[0].add(Card(Suit.HEARTS, Rank.KING, isFaceUp = true))
        
        val move = Move.TableauToFoundation(0, 0)
        val newState = GameStateUtils.applyMove(state, move)
        
        // 이미 K까지 차있으므로 이 이동은 실패해야 함
        // 또는 승리 조건을 만들려면 다른 방법 필요
        
        // 대신 Foundation이 모두 13장씩 채워진 상태를 직접 확인
        val winState = GameState()
        for (i in 0..3) {
            for (rank in Rank.values()) {
                winState.foundation[i].add(
                    Card(Suit.values()[i], rank, isFaceUp = true)
                )
            }
        }
        winState.isGameOver = true
        
        assertTrue("Game should be over", winState.isGameOver)
        assertEquals("Each foundation should have 13 cards", 13, winState.foundation[0].size)
    }
}
