package us.jyni.game.klondike.ui.screens

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.`is`
import org.junit.Test
import org.junit.runner.RunWith
import us.jyni.R

@RunWith(AndroidJUnit4::class)
class GameActivityRulesUiMoreTest {

    @Test
    fun rules_summary_updates_on_spinner_change() {
        ActivityScenario.launch(GameActivity::class.java).use {
            onView(withId(R.id.status_text)).check(matches(withText(containsString("D"))))
            // Toggle draw between 1 and 3 and check status contains D1/D3 accordingly
            onView(withId(R.id.rules_draw_spinner)).perform(click())
            onData(`is`(ApplicationStrings.rules_draw_3())).perform(click())
            onView(withId(R.id.status_text)).check(matches(withText(containsString("D3"))))

            onView(withId(R.id.rules_draw_spinner)).perform(click())
            onData(`is`(ApplicationStrings.rules_draw_1())).perform(click())
            onView(withId(R.id.status_text)).check(matches(withText(containsString("D1"))))

            // Toggle recycle to see R:REV/KEE short tags
            onView(withId(R.id.rules_recycle_spinner)).perform(click())
            onData(`is`(ApplicationStrings.rules_recycle_reverse())).perform(click())
            onView(withId(R.id.status_text)).check(matches(withText(containsString("R:REV"))))

            onView(withId(R.id.rules_recycle_spinner)).perform(click())
            onData(`is`(ApplicationStrings.rules_recycle_keep())).perform(click())
            onView(withId(R.id.status_text)).check(matches(withText(containsString("R:KEE"))))
        }
    }

    @Test
    fun dealId_persists_across_recreation() {
        ActivityScenario.launch(GameActivity::class.java).use { scenario ->
            var before: String? = null
            onView(withId(R.id.status_text)).check { v, _ -> before = (v as android.widget.TextView).text.toString() }
            val dlBefore = Regex("Deal:([A-Za-z0-9_]+)").find(before!!)?.groupValues?.get(1)
            scenario.recreate()
            var after: String? = null
            onView(withId(R.id.status_text)).check { v, _ -> after = (v as android.widget.TextView).text.toString() }
            val dlAfter = Regex("Deal:([A-Za-z0-9_]+)").find(after!!)?.groupValues?.get(1)
            org.junit.Assert.assertEquals(dlBefore, dlAfter)
        }
    }
}
 
