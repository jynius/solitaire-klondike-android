package us.jyni.game.klondike.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.CoreMatchers.*
import org.junit.Test
import org.junit.runner.RunWith
import us.jyni.R

@RunWith(AndroidJUnit4::class)
class GameActivityMoreUiTest {

    @Test
    fun rules_change_changes_dealId() {
        ActivityScenario.launch(GameActivity::class.java).use {
            var before: String? = null
            onView(withId(R.id.status_text)).check { v, _ -> before = (v as android.widget.TextView).text.toString() }

            // Toggle draw rule to the other option
            onView(withId(R.id.rules_draw_spinner)).perform(click())
            val entry = if (before!!.contains("D1")) ApplicationStrings.rules_draw_3() else ApplicationStrings.rules_draw_1()
            onData(`is`(entry)).perform(click())

            var after: String? = null
            onView(withId(R.id.status_text)).check { v, _ -> after = (v as android.widget.TextView).text.toString() }

            val dealBefore = Regex("Deal:([A-Za-z0-9_]+)").find(before!!)?.groupValues?.get(1)
            val dealAfter = Regex("Deal:([A-Za-z0-9_]+)").find(after!!)?.groupValues?.get(1)
            org.junit.Assert.assertNotEquals("Deal ID should change when rules change with same seed", dealBefore, dealAfter)
        }
    }

    @Test
    fun long_press_on_status_copies_deal() {
        ActivityScenario.launch(GameActivity::class.java).use {
            onView(withId(R.id.status_text)).perform(longClick())
            val ctx = ApplicationProvider.getApplicationContext<Context>()
            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val text = cm.primaryClip?.getItemAt(0)?.text?.toString()
            org.hamcrest.MatcherAssert.assertThat(text, notNullValue())
            org.hamcrest.MatcherAssert.assertThat(text, startsWith("DL1_"))
        }
    }

    @Test
    fun waste_selection_highlights_foundation_targets() {
        ActivityScenario.launch(GameActivity::class.java).use {
            // Draw until ACE appears on waste to ensure a valid foundation target exists
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
            // foundation_0 should have a non-null background (target highlight)
            onView(withContentDescription("foundation_0")).check { v, _ ->
                assert(v.background != null)
            }
        }
    }

    @Test
    fun rules_selection_persists_across_recreation() {
        ActivityScenario.launch(GameActivity::class.java).use { scenario ->
            // Change all rules widgets
            onView(withId(R.id.rules_draw_spinner)).perform(click())
            onData(`is`(ApplicationStrings.rules_draw_3())).perform(click())
            onView(withId(R.id.rules_recycle_spinner)).perform(click())
            onData(`is`(ApplicationStrings.rules_recycle_reverse())).perform(click())
            onView(withId(R.id.rules_redeals_spinner)).perform(click())
            onData(`is`(ApplicationStrings.rules_redeals_0())).perform(click())
            onView(withId(R.id.rules_allow_f2t)).perform(click())

            // Recreate activity
            scenario.recreate()

            // Status text should reflect D3, R:REV, 0 redeals, F→T:on
            onView(withId(R.id.status_text)).check(matches(withText(containsString("D3"))))
            onView(withId(R.id.status_text)).check(matches(withText(containsString("R:REV"))))
            onView(withId(R.id.status_text)).check(matches(withText(containsString(" 0 "))))
            onView(withId(R.id.status_text)).check(matches(withText(containsString("F→T:on"))))
        }
    }

    @Test
    fun undo_redo_buttons_enablement_reflects_history() {
        ActivityScenario.launch(GameActivity::class.java).use {
            // Initially, undo is disabled, redo is disabled
            onView(withId(R.id.undo_button)).check(matches(not(isEnabled())))
            onView(withId(R.id.redo_button)).check(matches(not(isEnabled())))

            // Perform an action (draw)
            onView(withId(R.id.draw_button)).perform(click())
            // Undo should be enabled, redo disabled
            onView(withId(R.id.undo_button)).check(matches(isEnabled()))
            onView(withId(R.id.redo_button)).check(matches(not(isEnabled())))

            // Undo the draw
            onView(withId(R.id.undo_button)).perform(click())
            // Redo should now be enabled, undo may be disabled or enabled depending on history depth
            onView(withId(R.id.redo_button)).check(matches(isEnabled()))
        }
    }
}
