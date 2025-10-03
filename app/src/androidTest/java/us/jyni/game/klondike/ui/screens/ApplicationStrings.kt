package us.jyni.game.klondike.ui.screens

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import us.jyni.R

object ApplicationStrings {
    fun rules_draw_1(): String = getString(R.string.rules_draw_1)
    fun rules_draw_3(): String = getString(R.string.rules_draw_3)
    fun rules_recycle_keep(): String = getString(R.string.rules_recycle_keep)
    fun rules_recycle_reverse(): String = getString(R.string.rules_recycle_reverse)
    fun rules_redeals_0(): String = getString(R.string.rules_redeals_0)

    private fun getString(id: Int): String {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        return ctx.getString(id)
    }
}
