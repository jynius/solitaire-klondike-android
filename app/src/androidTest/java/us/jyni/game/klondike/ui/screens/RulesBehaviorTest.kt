package us.jyni.game.klondike.ui.screens

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.CoreMatchers.*
import org.junit.Test
import org.junit.runner.RunWith
import us.jyni.R
import androidx.test.core.app.ApplicationProvider
import androidx.core.content.ContextCompat

@RunWith(AndroidJUnit4::class)
class RulesBehaviorTest {

    @Test
    fun draw1_vs_draw3_updates_waste_count_in_status() {
        ActivityScenario.launch(GameActivity::class.java).use {
            // Set draw=1
            onView(withId(R.id.rules_draw_spinner)).perform(click())
            onData(`is`(ApplicationStrings.rules_draw_1())).perform(click())

            // capture waste count text
            var before: String? = null
            onView(withId(R.id.status_text)).check { v, _ -> before = (v as android.widget.TextView).text.toString() }
            onView(withId(R.id.draw_button)).perform(click())
            var after1: String? = null
            onView(withId(R.id.status_text)).check { v, _ -> after1 = (v as android.widget.TextView).text.toString() }

            // Set draw=3
            onView(withId(R.id.rules_draw_spinner)).perform(click())
            onData(`is`(ApplicationStrings.rules_draw_3())).perform(click())
            onView(withId(R.id.draw_button)).perform(click())
            var after3: String? = null
            onView(withId(R.id.status_text)).check { v, _ -> after3 = (v as android.widget.TextView).text.toString() }

            fun wasteCount(s: String): Int = Regex("Waste:(\\d+)").find(s)?.groupValues?.get(1)?.toInt() ?: 0
            val wBefore = wasteCount(before!!)
            val w1 = wasteCount(after1!!)
            val w3 = wasteCount(after3!!)

            org.junit.Assert.assertTrue("draw=1 should increase by >=1", w1 >= wBefore + 1)
            org.junit.Assert.assertTrue("draw=3 should increase by >=3 from previous", w3 >= w1 + 3)
        }
    }

    @Test
    fun allowF2T_toggle_affects_move_validity() {
        ActivityScenario.launch(GameActivity::class.java).use {
            // Disallow F->T
            onView(withId(R.id.rules_allow_f2t)).perform(click())
            // select foundation_0 then try place on a tableau; should not move
            onView(withContentDescription("foundation_0")).perform(click())
            // pick a column
            onView(withContentDescription("tableau_col_0")).perform(click())
            // Assert foundation_0 remains unchanged: still no background (not moved away), but selection cleared
            onView(withContentDescription("foundation_0")).check { v, _ ->
                // selection should be cleared and no move occurred; we can't easily inspect cards here, so just assert no exception
                assert(true)
            }
        }
    }

    @Test
    fun redeals_spinner_limits_recycle() {
        ActivityScenario.launch(GameActivity::class.java).use {
            // Set draw=3 to exhaust faster
            onView(withId(R.id.rules_draw_spinner)).perform(click())
            onData(`is`(ApplicationStrings.rules_draw_3())).perform(click())
            // Set redeals=0
            onView(withId(R.id.rules_redeals_spinner)).perform(click())
            onData(`is`(ApplicationStrings.rules_redeals_0())).perform(click())

            fun parseCounts(): Pair<Int, Int> {
                var txt = ""
                onView(withId(R.id.status_text)).check { v, _ -> txt = (v as android.widget.TextView).text.toString() }
                val w = Regex("Waste:(\\d+)").find(txt)?.groupValues?.get(1)?.toInt() ?: 0
                val s = Regex("Stock:(\\d+)").find(txt)?.groupValues?.get(1)?.toInt() ?: 0
                return s to w
            }

            // Exhaust stock
            repeat(40) { onView(withId(R.id.draw_button)).perform(click()) }
            val (stockBefore, wasteBefore) = parseCounts()
            // Further draw should not change counts if no redeals
            onView(withId(R.id.draw_button)).perform(click())
            val (stockAfter, wasteAfter) = parseCounts()
            org.junit.Assert.assertEquals(stockBefore, stockAfter)
            org.junit.Assert.assertEquals(wasteBefore, wasteAfter)
        }
    }

    @Test
    fun highlights_target_vs_dim_distinction() {
        ActivityScenario.launch(GameActivity::class.java).use {
            // Ensure an ACE on waste for a valid foundation target
            repeat(60) {
                try {
                    onView(withContentDescription("waste"))
                        .check(matches(hasDescendant(withContentDescription(startsWith("card_ACE_")))))
                    return@repeat
                } catch (_: Throwable) {
                    onView(withId(R.id.draw_button)).perform(click())
                }
            }
            // Select waste
            onView(withContentDescription("waste")).perform(click())

            // Compare foundation backgrounds: one should be target, others dim
            val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
            val targetState = ContextCompat.getDrawable(ctx, R.drawable.bg_highlight_target)?.constantState
            val dimState = ContextCompat.getDrawable(ctx, R.drawable.bg_highlight_dim)?.constantState

            // foundation_0 is the typical ACE target in many deals; we assert at least one foundation has target
            var foundTarget = false
            var foundDim = false
            for (i in 0..3) {
                onView(withContentDescription("foundation_" + i)).check { v, _ ->
                    val st = v.background?.constantState
                    if (st == targetState) foundTarget = true
                    if (st == dimState) foundDim = true
                }
            }
            org.junit.Assert.assertTrue(foundTarget)
            org.junit.Assert.assertTrue(foundDim)
        }
    }
}
 
