package us.jyni.game.klondike.ui.components

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import us.jyni.game.klondike.model.Card
import us.jyni.game.klondike.model.Suit
import us.jyni.game.klondike.model.Rank
import us.jyni.R

class CardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val suitPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var card: Card? = null
    private var lastFaceUp: Boolean? = null
    private var isEmpty = false

    init {
        // Set card size to square 80dp x 80dp
        val cardSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 80f, context.resources.displayMetrics
        ).toInt()
        minimumWidth = cardSize
        minimumHeight = cardSize
        
        // Default empty state
        setEmpty()
    }

    fun setCard(card: Card) {
        val prevFace = lastFaceUp
        val nextFace = card.isFaceUp
        this.card = card
        this.isEmpty = false
        
        // Update background based on card state
        updateBackground()
        
        if (prevFace != null && prevFace != nextFace) {
            // Instant flip without animation
            invalidate()
        } else {
            invalidate()
        }
        lastFaceUp = nextFace
    }
    
    fun setEmpty() {
        this.card = null
        this.isEmpty = true
        this.lastFaceUp = null
        background = ContextCompat.getDrawable(context, R.drawable.card_empty)
        invalidate()
    }
    
    fun setCardSelected(selected: Boolean) {
        if (selected) {
            background = ContextCompat.getDrawable(context, R.drawable.card_selected)
        } else {
            updateBackground()
        }
    }
    
    private fun updateBackground() {
        background = when {
            isEmpty -> ContextCompat.getDrawable(context, R.drawable.card_empty)
            card?.isFaceUp == true -> ContextCompat.getDrawable(context, R.drawable.card_face)
            else -> ContextCompat.getDrawable(context, R.drawable.card_back)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val card = this.card ?: return
        if (!card.isFaceUp || isEmpty) return
        
        val w = width.toFloat()
        val h = height.toFloat()
        val padding = w * 0.05f
        
        // Set text size based on card size - increased 1.2x for better visibility
        val baseTextSize = w * 0.26f  // increased by 1.2x from 0.22f
        textPaint.textSize = baseTextSize
        textPaint.typeface = Typeface.DEFAULT_BOLD
        
        // Set color based on suit
        val color = when (card.suit) {
            Suit.HEARTS, Suit.DIAMONDS -> Color.RED
            Suit.SPADES, Suit.CLUBS -> Color.BLACK
        }
        textPaint.color = color
        suitPaint.color = color
        
        // Get rank and suit symbols
        val rankText = getRankText(card.rank)
        val suitText = getSuitSymbol(card.suit)
        
        // Measure text dimensions for horizontal layout
        val rankBounds = Rect()
        textPaint.getTextBounds(rankText, 0, rankText.length, rankBounds)
        suitPaint.textSize = baseTextSize
        val suitBounds = Rect()
        suitPaint.getTextBounds(suitText, 0, suitText.length, suitBounds)
        
        // Draw suit and rank horizontally in top-left corner (suit first, then rank)
        val topY = padding + baseTextSize
        canvas.drawText(suitText, padding, topY, suitPaint)
        canvas.drawText(rankText, padding + suitBounds.width() + dp(2).toFloat(), topY, textPaint)
        
        // Draw large suit symbol in center
        suitPaint.textSize = baseTextSize * 2f
        val centerSuitBounds = Rect()
        suitPaint.getTextBounds(suitText, 0, suitText.length, centerSuitBounds)
        val centerX = w / 2f - centerSuitBounds.width() / 2f
        val centerY = h / 2f + centerSuitBounds.height() / 2f
        canvas.drawText(suitText, centerX, centerY, suitPaint)
        
        // Removed bottom-right corner text for cleaner appearance
    }
    
    private fun getRankText(rank: Rank): String {
        return when (rank) {
            Rank.ACE -> "A"
            Rank.TWO -> "2"
            Rank.THREE -> "3"
            Rank.FOUR -> "4"
            Rank.FIVE -> "5"
            Rank.SIX -> "6"
            Rank.SEVEN -> "7"
            Rank.EIGHT -> "8"
            Rank.NINE -> "9"
            Rank.TEN -> "10"
            Rank.JACK -> "J"
            Rank.QUEEN -> "Q"
            Rank.KING -> "K"
        }
    }
    
    private fun getSuitSymbol(suit: Suit): String {
        return when (suit) {
            Suit.HEARTS -> "♥"
            Suit.DIAMONDS -> "♦"
            Suit.CLUBS -> "♣"
            Suit.SPADES -> "♠"
        }
    }
    
    private fun dp(value: Int): Int {
        return (context.resources.displayMetrics.density * value).toInt()
    }
}