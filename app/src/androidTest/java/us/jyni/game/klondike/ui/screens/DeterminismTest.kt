package us.jyni.game.klondike.ui.screens

import android.content.Intent
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import org.hamcrest.CoreMatchers.startsWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import us.jyni.R

class DeterminismTest {

    private fun launchWithSeed(seed: Long): ActivityScenario<GameActivity> {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(ctx, GameActivity::class.java).putExtra(GameActivity.EXTRA_SEED, seed)
        return ActivityScenario.launch(intent)
    }

    private fun captureWasteTopSequence(draws: Int): List<String?> {
        val seq = mutableListOf<String?>()
        repeat(draws) {
            onView(withContentDescription("stock")).perform(click()) // draw
            // capture waste top contentDescription
            var desc: String? = null
            onView(withContentDescription("waste")).check { v, _ ->
                val vg = v as ViewGroup
                if (vg.childCount > 0) {
                    desc = vg.getChildAt(vg.childCount - 1).contentDescription?.toString()
                }
            }
            seq.add(desc)
        }
        return seq
    }

    @Test
    fun sameSeed_producesSameWasteSequence() {
        val draws = 8
        val seed = 123456789L
        val seq1 = mutableListOf<String?>()
        val seq2 = mutableListOf<String?>()

        launchWithSeed(seed).use {
            seq1 += captureWasteTopSequence(draws)
        }
        launchWithSeed(seed).use {
            seq2 += captureWasteTopSequence(draws)
        }
        assertEquals(seq1, seq2)
    }

    @Test
    fun differentSeeds_likelyProduceDifferentWasteSequence() {
        val draws = 10
        val seedA = 111L
        val seedB = 222L
        val seqA = mutableListOf<String?>()
        val seqB = mutableListOf<String?>()

        launchWithSeed(seedA).use {
            seqA += captureWasteTopSequence(draws)
        }
        launchWithSeed(seedB).use {
            seqB += captureWasteTopSequence(draws)
        }
        // Not a strict guarantee, but extremely unlikely to be identical
        assertNotEquals(seqA, seqB)
    }
}
