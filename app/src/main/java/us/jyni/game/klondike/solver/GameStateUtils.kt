package us.jyni.game.klondike.solver

import us.jyni.game.klondike.model.Card
import us.jyni.game.klondike.model.GameState

/**
 * GameState 복제 및 이동 적용 유틸리티
 */
object GameStateUtils {
    
    /**
     * GameState를 깊은 복사
     */
    fun cloneState(state: GameState): GameState {
        return GameState(
            tableau = state.tableau.map { pile -> 
                pile.map { card -> card.copy() }.toMutableList() 
            },
            foundation = state.foundation.map { pile -> 
                pile.map { card -> card.copy() }.toMutableList() 
            },
            stock = state.stock.map { it.copy() }.toMutableList(),
            waste = state.waste.map { it.copy() }.toMutableList(),
            isGameOver = state.isGameOver,
            score = state.score
        )
    }
    
    /**
     * 상태에 이동을 적용한 새로운 상태 반환
     * @return 새로운 GameState 또는 이동이 불가능하면 null
     */
    fun applyMove(state: GameState, move: Move): GameState? {
        val newState = cloneState(state)
        
        return when (move) {
            is Move.TableauToTableau -> {
                applyTableauToTableau(newState, move)
            }
            is Move.TableauToFoundation -> {
                applyTableauToFoundation(newState, move)
            }
            is Move.WasteToTableau -> {
                applyWasteToTableau(newState, move)
            }
            is Move.WasteToFoundation -> {
                applyWasteToFoundation(newState, move)
            }
            is Move.FoundationToTableau -> {
                applyFoundationToTableau(newState, move)
            }
            is Move.Draw -> {
                applyDraw(newState)
            }
        }
    }
    
    private fun applyTableauToTableau(state: GameState, move: Move.TableauToTableau): GameState? {
        val src = state.tableau[move.fromCol]
        val dst = state.tableau[move.toCol]
        
        if (move.cardIndex < 0 || move.cardIndex >= src.size) return null
        
        // 이동할 카드들
        val movingCards = src.subList(move.cardIndex, src.size).toList()
        
        // 이동
        src.removeAll(movingCards)
        dst.addAll(movingCards)
        
        // 뒤집기: 출발지에 카드가 남아있고 맨 위가 뒷면이면 앞면으로
        if (src.isNotEmpty() && !src.last().isFaceUp) {
            src.last().isFaceUp = true
        }
        
        return state
    }
    
    private fun applyTableauToFoundation(state: GameState, move: Move.TableauToFoundation): GameState? {
        val src = state.tableau[move.fromCol]
        val fnd = state.foundation[move.foundationIndex]
        
        if (src.isEmpty()) return null
        
        val card = src.removeAt(src.lastIndex)
        fnd.add(card)
        
        // 뒤집기
        if (src.isNotEmpty() && !src.last().isFaceUp) {
            src.last().isFaceUp = true
        }
        
        // 승리 체크
        if (state.foundation.all { it.size == 13 }) {
            state.isGameOver = true
        }
        
        return state
    }
    
    private fun applyWasteToTableau(state: GameState, move: Move.WasteToTableau): GameState? {
        if (state.waste.isEmpty()) return null
        
        val card = state.waste.removeAt(state.waste.lastIndex)
        state.tableau[move.toCol].add(card)
        
        return state
    }
    
    private fun applyWasteToFoundation(state: GameState, move: Move.WasteToFoundation): GameState? {
        if (state.waste.isEmpty()) return null
        
        val card = state.waste.removeAt(state.waste.lastIndex)
        state.foundation[move.foundationIndex].add(card)
        
        // 승리 체크
        if (state.foundation.all { it.size == 13 }) {
            state.isGameOver = true
        }
        
        return state
    }
    
    private fun applyFoundationToTableau(state: GameState, move: Move.FoundationToTableau): GameState? {
        val fnd = state.foundation[move.foundationIndex]
        if (fnd.isEmpty()) return null
        
        val card = fnd.removeAt(fnd.lastIndex)
        state.tableau[move.toCol].add(card)
        
        return state
    }
    
    private fun applyDraw(state: GameState): GameState? {
        if (state.stock.isEmpty()) {
            // Recycle: Waste를 Stock으로 (역순)
            if (state.waste.isEmpty()) return null
            
            state.stock.addAll(state.waste.reversed())
            state.waste.clear()
            
            // 모든 카드를 뒷면으로
            state.stock.forEach { it.isFaceUp = false }
        }
        
        // Draw 1장
        if (state.stock.isNotEmpty()) {
            val card = state.stock.removeAt(state.stock.lastIndex)
            card.isFaceUp = true
            state.waste.add(card)
        }
        
        return state
    }
    
    /**
     * 상태의 해시값 계산 (중복 방지용)
     */
    fun stateHash(state: GameState): String {
        val sb = StringBuilder()
        
        // Tableau
        for (pile in state.tableau) {
            sb.append(pile.joinToString(",") { "${it.suit.ordinal}${it.rank.value}${if(it.isFaceUp) 'U' else 'D'}" })
            sb.append("|")
        }
        
        sb.append("//")
        
        // Foundation
        for (pile in state.foundation) {
            sb.append(pile.size)
            sb.append(",")
        }
        
        sb.append("//")
        
        // Stock size (순서는 변하지 않으므로 크기만)
        sb.append(state.stock.size)
        
        sb.append("//")
        
        // Waste
        if (state.waste.isNotEmpty()) {
            val top = state.waste.last()
            sb.append("${top.suit.ordinal}${top.rank.value}")
        }
        
        return sb.toString()
    }
}
