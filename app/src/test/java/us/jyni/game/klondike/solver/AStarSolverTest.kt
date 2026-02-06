package us.jyni.game.klondike.solver

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import us.jyni.game.klondike.model.Card
import us.jyni.game.klondike.model.GameState
import us.jyni.game.klondike.model.Rank
import us.jyni.game.klondike.model.Suit
import us.jyni.game.klondike.engine.GameEngine

class AStarSolverTest {
    
    private lateinit var solver: AStarSolver
    
    @Before
    fun setup() {
        val mockEngine = GameEngine()
        solver = AStarSolver(mockEngine)
    }
    
    @Test
    fun simple_tableau_move_available() {
        // 간단한 Tableau 이동 가능한 상태
        val state = GameState(
            tableau = List(7) { mutableListOf<Card>() },
            foundation = List(4) { mutableListOf<Card>() },
            stock = mutableListOf(),
            waste = mutableListOf(),
            isGameOver = false,
            score = 0
        )
        
        // T[0]: ♥7 (앞면)
        state.tableau[0].add(Card(Suit.HEARTS, Rank.SEVEN, isFaceUp = true))
        
        // T[1]: ♠8 (앞면) - ♥7을 올릴 수 있음
        state.tableau[1].add(Card(Suit.SPADES, Rank.EIGHT, isFaceUp = true))
        
        val hint = solver.findBestMove(state)
        
        assertNotNull("힌트를 찾아야 함", hint)
        assertTrue("Tableau 이동이어야 함", hint is Move.TableauToTableau)
        
        val move = hint as Move.TableauToTableau
        assertEquals("T[0]에서 이동", 0, move.fromCol)
        assertEquals("T[1]로 이동", 1, move.toCol)
    }
    
    @Test
    fun tableau_move_with_facedown_cards() {
        // 뒷면 카드가 있는 상태에서 Tableau 이동
        val state = GameState(
            tableau = List(7) { mutableListOf<Card>() },
            foundation = List(4) { mutableListOf<Card>() },
            stock = mutableListOf(),
            waste = mutableListOf(),
            isGameOver = false,
            score = 0
        )
        
        // T[0]: ♣2(뒷면), ♦3(뒷면), ♥5 (앞면)
        state.tableau[0].add(Card(Suit.CLUBS, Rank.TWO, isFaceUp = false))
        state.tableau[0].add(Card(Suit.DIAMONDS, Rank.THREE, isFaceUp = false))
        state.tableau[0].add(Card(Suit.HEARTS, Rank.FIVE, isFaceUp = true))
        
        // T[1]: ♠6 (앞면) - ♥5를 올릴 수 있음
        state.tableau[1].add(Card(Suit.SPADES, Rank.SIX, isFaceUp = true))
        
        val hint = solver.findBestMove(state)
        
        assertNotNull("힌트를 찾아야 함", hint)
        assertTrue("Tableau 이동이어야 함", hint is Move.TableauToTableau)
        
        val move = hint as Move.TableauToTableau
        assertEquals("T[0]에서 이동", 0, move.fromCol)
        assertEquals("T[1]로 이동", 1, move.toCol)
        assertEquals("앞면 카드 인덱스에서 이동", 2, move.cardIndex)
    }
    
    @Test
    fun multiple_tableau_moves_available() {
        // 여러 Tableau 이동 가능
        val state = GameState(
            tableau = List(7) { mutableListOf<Card>() },
            foundation = List(4) { mutableListOf<Card>() },
            stock = mutableListOf(),
            waste = mutableListOf(),
            isGameOver = false,
            score = 0
        )
        
        // T[0]: ♥3
        state.tableau[0].add(Card(Suit.HEARTS, Rank.THREE, isFaceUp = true))
        
        // T[1]: ♠4 - ♥3 올릴 수 있음
        state.tableau[1].add(Card(Suit.SPADES, Rank.FOUR, isFaceUp = true))
        
        // T[2]: ♣4 - ♥3 올릴 수 있음
        state.tableau[2].add(Card(Suit.CLUBS, Rank.FOUR, isFaceUp = true))
        
        val hint = solver.findBestMove(state)
        
        assertNotNull("힌트를 찾아야 함", hint)
        assertTrue("Tableau 이동이어야 함", hint is Move.TableauToTableau)
        
        val move = hint as Move.TableauToTableau
        assertEquals("T[0]에서 이동", 0, move.fromCol)
        assertTrue("T[1] 또는 T[2]로 이동", move.toCol == 1 || move.toCol == 2)
    }
    
    @Test
    fun no_moves_only_draw() {
        // Tableau 이동 불가능, Draw만 가능
        val state = GameState(
            tableau = List(7) { mutableListOf<Card>() },
            foundation = List(4) { mutableListOf<Card>() },
            stock = mutableListOf(),
            waste = mutableListOf(),
            isGameOver = false,
            score = 0
        )
        
        // T[0]: ♥7
        state.tableau[0].add(Card(Suit.HEARTS, Rank.SEVEN, isFaceUp = true))
        
        // T[1]: ♦5 - ♥7을 올릴 수 없음 (색상 같음)
        state.tableau[1].add(Card(Suit.DIAMONDS, Rank.FIVE, isFaceUp = true))
        
        // Stock에 카드 있음
        state.stock.add(Card(Suit.CLUBS, Rank.ACE, isFaceUp = false))
        
        val hint = solver.findBestMove(state)
        
        assertNotNull("힌트를 찾아야 함", hint)
        assertTrue("Draw여야 함", hint is Move.Draw)
    }
    
    @Test
    fun foundation_move_priority() {
        // Foundation 이동이 우선순위
        val state = GameState(
            tableau = List(7) { mutableListOf<Card>() },
            foundation = List(4) { mutableListOf<Card>() },
            stock = mutableListOf(),
            waste = mutableListOf(),
            isGameOver = false,
            score = 0
        )
        
        // Foundation[0]: ♥A, ♥2
        state.foundation[0].add(Card(Suit.HEARTS, Rank.ACE, isFaceUp = true))
        state.foundation[0].add(Card(Suit.HEARTS, Rank.TWO, isFaceUp = true))
        
        // T[0]: ♥3 - Foundation으로 갈 수 있음
        state.tableau[0].add(Card(Suit.HEARTS, Rank.THREE, isFaceUp = true))
        
        // T[1]: ♠4 - ♥3을 올릴 수도 있음
        state.tableau[1].add(Card(Suit.SPADES, Rank.FOUR, isFaceUp = true))
        
        val hint = solver.findBestMove(state)
        
        assertNotNull("힌트를 찾아야 함", hint)
        // Foundation 이동이 우선순위가 높아야 함
        println("실제 힌트: $hint")
        assertTrue("Foundation 이동이어야 함 (실제: $hint)", hint is Move.TableauToFoundation)
    }
    
    @Test
    fun king_to_empty_space() {
        // King을 빈 공간으로
        val state = GameState(
            tableau = List(7) { mutableListOf<Card>() },
            foundation = List(4) { mutableListOf<Card>() },
            stock = mutableListOf(),
            waste = mutableListOf(),
            isGameOver = false,
            score = 0
        )
        
        // T[0]: ♣2(뒷면), ♠K(앞면)
        state.tableau[0].add(Card(Suit.CLUBS, Rank.TWO, isFaceUp = false))
        state.tableau[0].add(Card(Suit.SPADES, Rank.KING, isFaceUp = true))
        
        // T[1]: 비어있음 - King을 옮길 수 있음
        // (다른 컬럼도 비어있음)
        
        val hint = solver.findBestMove(state)
        
        assertNotNull("힌트를 찾아야 함", hint)
        assertTrue("Tableau 이동이어야 함", hint is Move.TableauToTableau)
        
        val move = hint as Move.TableauToTableau
        assertEquals("T[0]에서 이동", 0, move.fromCol)
        assertTrue("빈 컬럼으로 이동", state.tableau[move.toCol].isEmpty())
    }
    
    @Test
    fun waste_to_tableau_move() {
        // Waste에서 Tableau로 이동
        val state = GameState(
            tableau = List(7) { mutableListOf<Card>() },
            foundation = List(4) { mutableListOf<Card>() },
            stock = mutableListOf(),
            waste = mutableListOf(),
            isGameOver = false,
            score = 0
        )
        
        // T[0]: ♠8
        state.tableau[0].add(Card(Suit.SPADES, Rank.EIGHT, isFaceUp = true))
        
        // Waste: ♥7 - T[0]에 올릴 수 있음
        state.waste.add(Card(Suit.HEARTS, Rank.SEVEN, isFaceUp = true))
        
        val hint = solver.findBestMove(state)
        
        assertNotNull("힌트를 찾아야 함", hint)
        assertTrue("Waste 이동이어야 함", hint is Move.WasteToTableau)
        
        val move = hint as Move.WasteToTableau
        assertEquals("T[0]로 이동", 0, move.toCol)
    }
    
    @Test
    fun sequence_move() {
        // Stock이 비어있으면 Draw 이동이 생성되지 않아야 함
        val state = GameState(
            tableau = List(7) { mutableListOf<Card>() },
            foundation = List(4) { mutableListOf<Card>() },
            stock = mutableListOf(),  // Stock 비어있음
            waste = mutableListOf(),   // Waste도 비어있음
            isGameOver = false,
            score = 0
        )
        
        // T[0]: ♠8
        state.tableau[0].add(Card(Suit.SPADES, Rank.EIGHT, isFaceUp = true))
        
        // T[1]: ♦7
        state.tableau[1].add(Card(Suit.DIAMONDS, Rank.SEVEN, isFaceUp = true))
        
        val hint = solver.findBestMove(state)
        
        assertNotNull("힌트를 찾아야 함 (♦7 -> ♠8)", hint)
        assertTrue("Tableau 이동이어야 함", hint is Move.TableauToTableau)
    }
    
    @Test
    fun all_facedown_no_moves() {
        // 모든 카드가 뒷면, 이동 불가
        val state = GameState(
            tableau = List(7) { mutableListOf<Card>() },
            foundation = List(4) { mutableListOf<Card>() },
            stock = mutableListOf(),
            waste = mutableListOf(),
            isGameOver = false,
            score = 0
        )
        
        // T[0]: ♣7(뒷면)
        state.tableau[0].add(Card(Suit.CLUBS, Rank.SEVEN, isFaceUp = false))
        
        // T[1]: ♠8(뒷면)
        state.tableau[1].add(Card(Suit.SPADES, Rank.EIGHT, isFaceUp = false))
        
        // Stock에 카드 있음
        state.stock.add(Card(Suit.HEARTS, Rank.ACE, isFaceUp = false))
        
        val hint = solver.findBestMove(state)
        
        assertNotNull("힌트를 찾아야 함", hint)
        assertTrue("Draw여야 함", hint is Move.Draw)
    }
    
    @Test
    fun complex_state_multiple_options() {
        // 복잡한 상태, 여러 옵션 중 하나 선택
        val state = GameState(
            tableau = List(7) { mutableListOf<Card>() },
            foundation = List(4) { mutableListOf<Card>() },
            stock = mutableListOf(),
            waste = mutableListOf(),
            isGameOver = false,
            score = 0
        )
        
        // T[0]: ♣A(뒷면), ♥5
        state.tableau[0].add(Card(Suit.CLUBS, Rank.ACE, isFaceUp = false))
        state.tableau[0].add(Card(Suit.HEARTS, Rank.FIVE, isFaceUp = true))
        
        // T[1]: ♦2(뒷면), ♠6 - ♥5를 올릴 수 있음
        state.tableau[1].add(Card(Suit.DIAMONDS, Rank.TWO, isFaceUp = false))
        state.tableau[1].add(Card(Suit.SPADES, Rank.SIX, isFaceUp = true))
        
        // T[2]: ♥3
        state.tableau[2].add(Card(Suit.HEARTS, Rank.THREE, isFaceUp = true))
        
        // T[3]: ♠4 - ♥3을 올릴 수 있음
        state.tableau[3].add(Card(Suit.SPADES, Rank.FOUR, isFaceUp = true))
        
        val hint = solver.findBestMove(state)
        
        assertNotNull("힌트를 찾아야 함", hint)
        assertTrue("Tableau 이동이어야 함", hint is Move.TableauToTableau)
        // 어떤 이동이든 유효하면 OK
    }
    
    @Test
    fun prefer_flip_move() {
        // 뒤집기 이동 우선
        val state = GameState(
            tableau = List(7) { mutableListOf<Card>() },
            foundation = List(4) { mutableListOf<Card>() },
            stock = mutableListOf(),
            waste = mutableListOf(),
            isGameOver = false,
            score = 0
        )
        
        // T[0]: ♣K(뒷면), ♥5 - 이동하면 뒷면 뒤집힌다
        state.tableau[0].add(Card(Suit.CLUBS, Rank.KING, isFaceUp = false))
        state.tableau[0].add(Card(Suit.HEARTS, Rank.FIVE, isFaceUp = true))
        
        // T[1]: ♠6 - ♥5를 올릴 수 있음
        state.tableau[1].add(Card(Suit.SPADES, Rank.SIX, isFaceUp = true))
        
        // T[2]: ♦7 - 뒤집기 효과 없음
        state.tableau[2].add(Card(Suit.DIAMONDS, Rank.SEVEN, isFaceUp = true))
        
        val hint = solver.findBestMove(state)
        
        assertNotNull("힌트를 찾아야 함", hint)
        assertTrue("Tableau 이동이어야 함", hint is Move.TableauToTableau)
        
        val move = hint as Move.TableauToTableau
        // Flip 우선순위가 높으므로 T[0]에서 이동해야 함
        assertEquals("뒤집기 효과가 있는 T[0]에서 이동", 0, move.fromCol)
    }
    
    @Test
    fun ace_to_foundation() {
        // Ace를 Foundation으로
        val state = GameState(
            tableau = List(7) { mutableListOf<Card>() },
            foundation = List(4) { mutableListOf<Card>() },
            stock = mutableListOf(),
            waste = mutableListOf(),
            isGameOver = false,
            score = 0
        )
        
        // T[0]: ♥A
        state.tableau[0].add(Card(Suit.HEARTS, Rank.ACE, isFaceUp = true))
        
        val hint = solver.findBestMove(state)
        
        assertNotNull("힌트를 찾아야 함", hint)
        assertTrue("Foundation 이동이어야 함", hint is Move.TableauToFoundation)
        
        val move = hint as Move.TableauToFoundation
        assertEquals("T[0]에서 이동", 0, move.fromCol)
    }
    
    @Test
    fun empty_tableau_only_non_king() {
        // 빈 Tableau가 있지만 King이 아니면 이동 불가
        val state = GameState(
            tableau = List(7) { mutableListOf<Card>() },
            foundation = List(4) { mutableListOf<Card>() },
            stock = mutableListOf(),
            waste = mutableListOf(),
            isGameOver = false,
            score = 0
        )
        
        // T[0]: ♥7
        state.tableau[0].add(Card(Suit.HEARTS, Rank.SEVEN, isFaceUp = true))
        
        // T[1]: 비어있음 (하지만 King만 놓을 수 있음)
        
        // T[2]: ♠8 - ♥7을 올릴 수 있음
        state.tableau[2].add(Card(Suit.SPADES, Rank.EIGHT, isFaceUp = true))
        
        val hint = solver.findBestMove(state)
        
        assertNotNull("힌트를 찾아야 함", hint)
        assertTrue("Tableau 이동이어야 함", hint is Move.TableauToTableau)
        
        val move = hint as Move.TableauToTableau
        assertEquals("T[0]에서 이동", 0, move.fromCol)
        assertEquals("T[2]로 이동 (빈 공간 아님)", 2, move.toCol)
    }
}
