package us.jyni.game.klondike.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import us.jyni.R

class PileView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        orientation = VERTICAL
        setBackgroundColor(ContextCompat.getColor(context, R.color.pile_color))
    }

    fun updatePile(cards: List<CardView>) {
        removeAllViews()
        cards.forEach { cardView ->
            addView(cardView)
        }
    }
}