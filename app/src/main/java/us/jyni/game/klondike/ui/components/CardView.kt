package us.jyni.game.klondike.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import us.jyni.game.klondike.model.Card
import android.graphics.Color

class CardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint()
    private var card: Card? = null

    fun setCard(card: Card) {
        this.card = card
        invalidate() // Redraw the view
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        card?.let {
            // Draw the card based on its properties
            paint.color = if (it.isFaceUp) Color.WHITE else Color.GRAY
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            // Additional drawing logic for card details (rank, suit, etc.)
        }
    }
}