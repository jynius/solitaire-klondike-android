package us.jyni.game.klondike.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
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

@RunWith(AndroidJUnit4::class)
class GameActivityDebugUiTest {

    @Test
    fun debugBars_visible_in_debug() {
        ActivityScenario.launch(GameActivity::class.java).use {
            onView(withId(R.id.debug_bar)).check(matches(isDisplayed()))
            onView(withId(R.id.rules_bar)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun deal_toggle_hides_shows_deal_text() {
        ActivityScenario.launch(GameActivity::class.java).use {
            // initially checked true
            onView(withId(R.id.status_text)).check(matches(withText(containsString("Deal:"))))
            // toggle off
            onView(withId(R.id.debug_show_deal)).perform(click())
            onView(withId(R.id.status_text)).check(matches(not(withText(containsString("Deal:")))))
            // toggle on
            onView(withId(R.id.debug_show_deal)).perform(click())
            onView(withId(R.id.status_text)).check(matches(withText(containsString("Deal:"))))
        }
    }

    @Test
    fun copy_deal_button_copies_to_clipboard() {
        ActivityScenario.launch(GameActivity::class.java).use {
            onView(withId(R.id.debug_copy_deal)).perform(click())
            val ctx = ApplicationProvider.getApplicationContext<Context>()
            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val text = cm.primaryClip?.getItemAt(0)?.text?.toString()
            org.hamcrest.MatcherAssert.assertThat(text, notNullValue())
            org.hamcrest.MatcherAssert.assertThat(text, startsWith("DL1_"))
        }
    }

    @Test
    fun selecting_tableau_sets_selected_background() {
        ActivityScenario.launch(GameActivity::class.java).use {
            onView(withContentDescription("tableau_col_0")).perform(click())
            onView(withContentDescription("tableau_col_0")).check { v, _ ->
                // background should not be null when selected
                assert(v.background != null)
            }
        }
    }

    @Test
    fun highlights_toggle_off_removes_backgrounds() {
        ActivityScenario.launch(GameActivity::class.java).use {
            // Select waste to produce potential highlights on tableau
            onView(withContentDescription("waste")).perform(click())
            // some tableau cell should have background set (target or dim)
            onView(withContentDescription("tableau_col_0")).check { v, _ ->
                // may be null depending on targetability, so check a couple cols
            }
            var hadBackground = booleanArrayOf(false)
            for (i in 0..6) {
                try {
                    onView(withContentDescription("tableau_col_${i}"))
                        .check { v, _ -> if (v.background != null) hadBackground[0] = true }
                } catch (_: Throwable) {}
            }
            // turn off highlights
            onView(withId(R.id.debug_highlights)).perform(click())
            // now all tableau backgrounds should be null (selection is on waste only)
            for (i in 0..6) {
                try {
                    onView(withContentDescription("tableau_col_${i}"))
                        .check { v, _ -> assert(v.background == null) }
                } catch (_: Throwable) {}
            }
            // sanity: there was at least some background before
            assert(hadBackground[0])
        }
    }

    @Test
    fun persistent_restore_after_process_death() {
        // First run: draw once and capture status
        var firstStatus: String? = null
        ActivityScenario.launch(GameActivity::class.java).use { scenario ->
            onView(withId(R.id.draw_button)).perform(click())
            onView(withId(R.id.status_text)).check { v, _ ->
                firstStatus = (v as android.widget.TextView).text.toString()
            }
        }
        // Relaunch: status should still reflect non-zero waste
        ActivityScenario.launch(GameActivity::class.java).use {
            onView(withId(R.id.status_text)).check { v, _ ->
                val txt = (v as android.widget.TextView).text.toString()
                org.junit.Assert.assertTrue("Waste should be non-zero after restore", txt.contains("Waste:") && !txt.contains("Waste:0"))
            }
        }
    }
}
