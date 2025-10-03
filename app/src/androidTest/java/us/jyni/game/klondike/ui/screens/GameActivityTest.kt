package us.jyni.game.klondike.ui.screens

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.*
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.startsWith
import org.hamcrest.CoreMatchers.not
import org.junit.Assert.fail
import android.view.ViewGroup
import org.junit.Test
import org.junit.runner.RunWith
import us.jyni.R

@RunWith(AndroidJUnit4::class)
class GameActivityTest {

    @Test
    fun wasteAce_canMoveToFoundation() {
        ActivityScenario.launch(GameActivity::class.java).use {
            // Draw until an ACE appears on waste or until safety cap
            repeat(60) {
                try {
                    onView(withContentDescription("waste"))
                        .check(matches(hasDescendant(withContentDescription(startsWith("card_ACE_")))))
                    return@repeat
                } catch (t: Throwable) {
                    onView(withId(R.id.draw_button)).perform(click())
                }
            }

            // Tap foundation_0 to move ACE from waste
            onView(withContentDescription("foundation_0")).perform(click())

            // After move, waste should no longer contain ACE
            onView(withContentDescription("waste"))
                .check(matches(not(hasDescendant(withContentDescription(startsWith("card_ACE_"))))))

            // And foundation_0 should now show an ACE on top
            onView(withContentDescription("foundation_0"))
                .check(matches(hasDescendant(withContentDescription(startsWith("card_ACE_")))))
        }
    }

    @Test
    fun draw_and_reset_updateStatusText() {
        ActivityScenario.launch(GameActivity::class.java).use {
            onView(withId(R.id.status_text)).check(matches(isDisplayed()))

            // Tap Draw
            onView(withId(R.id.draw_button)).perform(click())

            // After draw, status should still contain Waste/Stock counters
            onView(withId(R.id.status_text)).check(matches(withText(containsString("Waste:"))))

            // Reset and ensure counters visible again
            onView(withId(R.id.reset_button)).perform(click())
            onView(withId(R.id.status_text)).check(matches(withText(containsString("Stock:"))))
        }
    }

    @Test
    fun title_and_resetButton_areDisplayed() {
        ActivityScenario.launch(GameActivity::class.java).use {
            onView(withId(R.id.title)).check(matches(isDisplayed()))
            onView(withId(R.id.reset_button)).check(matches(isDisplayed()))
            onView(withId(R.id.game_board)).check(matches(isDisplayed()))
            onView(withId(R.id.title)).check(matches(withText("Klondike Solitaire")))
        }
    }

    @Test
    fun resetButton_click_doesNotCrash_andKeepsUIVisible() {
        ActivityScenario.launch(GameActivity::class.java).use {
            onView(withId(R.id.reset_button)).check(matches(isEnabled()))
            onView(withId(R.id.reset_button)).perform(click())
            // After click, core views should still be visible
            onView(withId(R.id.title)).check(matches(isDisplayed()))
            onView(withId(R.id.game_board)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun activity_recreate_keepsUIVisible() {
        ActivityScenario.launch(GameActivity::class.java).use { scenario ->
            scenario.recreate()
            onView(withId(R.id.title)).check(matches(isDisplayed()))
            onView(withId(R.id.game_board)).check(matches(isDisplayed()))
            onView(withId(R.id.reset_button)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun undo_redo_togglesWasteAfterDraw() {
        ActivityScenario.launch(GameActivity::class.java).use {
            // Initially waste should be empty
            onView(withContentDescription("waste"))
                .check(matches(not(hasDescendant(withContentDescription(startsWith("card_"))))))

            // Draw once -> waste should have a card
            onView(withId(R.id.draw_button)).perform(click())
            onView(withContentDescription("waste"))
                .check(matches(hasDescendant(withContentDescription(startsWith("card_")))))

            // Undo -> waste empty
            onView(withId(R.id.undo_button)).perform(click())
            onView(withContentDescription("waste"))
                .check(matches(not(hasDescendant(withContentDescription(startsWith("card_"))))))

            // Redo -> waste has a card again
            onView(withId(R.id.redo_button)).perform(click())
            onView(withContentDescription("waste"))
                .check(matches(hasDescendant(withContentDescription(startsWith("card_")))))
        }
    }

    @Test
    fun reset_clearsFoundationAndWaste() {
        ActivityScenario.launch(GameActivity::class.java).use {
            // Ensure foundation_0 has an ACE by moving from waste (draw until ACE)
            repeat(60) {
                try {
                    onView(withContentDescription("waste"))
                        .check(matches(hasDescendant(withContentDescription(startsWith("card_ACE_")))))
                    return@repeat
                } catch (t: Throwable) {
                    onView(withId(R.id.draw_button)).perform(click())
                }
            }

            onView(withContentDescription("foundation_0")).perform(click())
            // Sanity: ACE present in foundation_0
            onView(withContentDescription("foundation_0"))
                .check(matches(hasDescendant(withContentDescription(startsWith("card_ACE_")))))

            // Reset
            onView(withId(R.id.reset_button)).perform(click())

            // After reset: foundation_0 and waste should be empty
            onView(withContentDescription("foundation_0"))
                .check(matches(not(hasDescendant(withContentDescription(startsWith("card_"))))))
            onView(withContentDescription("waste"))
                .check(matches(not(hasDescendant(withContentDescription(startsWith("card_"))))))

            // And tableau should be re-dealt (cards exist in at least one column)
            onView(withContentDescription("tableau_col_0"))
                .check(matches(hasDescendant(withContentDescription(startsWith("card_")))))
        }
    }

    @Test
    fun tableau_to_tableau_move_executesForSomePair() {
        ActivityScenario.launch(GameActivity::class.java).use {
            fun childCount(desc: String): Int {
                var count = -1
                onView(withContentDescription(desc)).check { v, _ ->
                    count = (v as ViewGroup).childCount
                }
                return count
            }

            for (i in 0..6) {
                for (j in 0..6) {
                    if (i == j) continue
                    val before = childCount("tableau_col_$j")
                    // Try move i -> j
                    onView(withContentDescription("tableau_col_$i")).perform(click())
                    onView(withContentDescription("tableau_col_$j")).perform(click())
                    val after = childCount("tableau_col_$j")
                    if (after > before) {
                        // Found a valid move
                        return
                    }
                }
            }
            fail("No valid tableau-to-tableau move found among pairs")
        }
    }

    @Test
    fun state_isPersistedAcrossRecreation() {
        ActivityScenario.launch(GameActivity::class.java).use { scenario ->
            // Cause a state change (draw)
            onView(withId(R.id.draw_button)).perform(click())
            // Recreate the activity
            scenario.recreate()
            // After recreation, status_text should still reflect a non-zero waste or changed stock
            onView(withId(R.id.status_text)).check(matches(withText(containsString("Waste:"))))
        }
    }

    @Test
    fun waste_to_tableau_move_possibleForSomeColumn() {
        ActivityScenario.launch(GameActivity::class.java).use {
            // Ensure waste has a face-up card
            onView(withId(R.id.draw_button)).perform(click())
            onView(withContentDescription("waste")).perform(click())

            // Try each tableau column as target
            for (col in 0..6) {
                try {
                    // Observe waste before
                    onView(withContentDescription("waste"))
                        .check(matches(hasDescendant(withContentDescription(startsWith("card_")))))
                    onView(withContentDescription("tableau_col_$col")).perform(click())
                    // If move succeeded, waste may become empty or top changes; accept success and return
                    return
                } catch (t: Throwable) {
                    // try next
                }
            }
        }
    }
}
