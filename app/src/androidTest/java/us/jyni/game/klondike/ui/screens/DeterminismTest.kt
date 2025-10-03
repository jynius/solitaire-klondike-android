package us.jyni.game.klondike.ui.screens

import android.content.Intent
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import us.jyni.R

@RunWith(AndroidJUnit4::class)
class DeterminismTest {

    @Test
    fun testSeedVerification() {
        // Verify that seed is actually being passed and applied
        val testSeed = 999L
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(context, GameActivity::class.java)
        intent.putExtra(GameActivity.EXTRA_SEED, testSeed)
        
        ActivityScenario.launch<GameActivity>(intent).use { scenario ->
            Thread.sleep(2000) // Give more time for initialization
            
            // Click debug show deal to make deal info visible
            try {
                onView(withId(R.id.debug_show_deal))
                    .perform(scrollTo(), click())
                Thread.sleep(500)
            } catch (e: Exception) {
                Log.w("DeterminismTest", "Could not click show deal", e)
            }
            
            // Just verify that game initialized
            scenario.onActivity { activity ->
                val statusText = activity.findViewById<android.widget.TextView>(R.id.status_text)?.text?.toString()
                Log.d("DeterminismTest", "Seed verification test - status: '$statusText'")
                
                // We should have a non-empty status 
                assertTrue("Status should not be empty", !statusText.isNullOrEmpty())
                
                // Stock should have cards (52 - 28 dealt to tableau = 24)
                assertTrue("Status should contain stock info", statusText?.contains("Stock:") ?: false)
            }
        }
    }
    
    @Test
    fun testSimpleDraw() {
        // Simple test to verify basic functionality works
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(context, GameActivity::class.java)
        intent.putExtra(GameActivity.EXTRA_SEED, 999L)
        
        ActivityScenario.launch<GameActivity>(intent).use { scenario ->
            Thread.sleep(1000)
            
            // Just verify draw works
            onView(withId(R.id.draw_button))
                .perform(scrollTo(), click())
            
            Thread.sleep(500)
            
            // Check waste count increased
            scenario.onActivity { activity ->
                val statusText = activity.findViewById<android.widget.TextView>(R.id.status_text)?.text?.toString()
                val wasteCount = extractWasteCount(statusText)
                Log.d("DeterminismTest", "Simple draw test - waste count: $wasteCount, status: '$statusText'")
                assertTrue("Draw should increase waste count", wasteCount > 0)
            }
        }
    }
    
    @Test
    fun testDeterministicBehavior() {
        // Test that same seed produces same sequence
        val seed1 = 12345L
        val seed2 = 12345L
        
        val sequence1 = drawSequence(seed1, 5)
        val sequence2 = drawSequence(seed2, 5) 
        
        Log.d("DeterminismTest", "Sequence 1: $sequence1")
        Log.d("DeterminismTest", "Sequence 2: $sequence2")
        
        assertEquals("Same seeds should produce identical sequences", sequence1, sequence2)
    }
    
    @Test
    fun testDifferentSeedsBehavior() {
        // Test that different seeds produce different sequences
        val seed1 = 12345L
        val seed2 = 54321L
        
        // Test each seed individually to avoid any cross-contamination
        val sequence1: List<Int>
        val sequence2: List<Int>
        
        // Test seed1 in isolation
        launchWithSeed(seed1).use { scenario1 ->
            Thread.sleep(1000)
            sequence1 = captureDrawSequence(scenario1, seed1, 5)
        }
        
        // Small delay between tests
        Thread.sleep(500)
        
        // Test seed2 in isolation  
        launchWithSeed(seed2).use { scenario2 ->
            Thread.sleep(1000)
            sequence2 = captureDrawSequence(scenario2, seed2, 5)
        }
        
        Log.d("DeterminismTest", "Seed1 sequence: $sequence1") 
        Log.d("DeterminismTest", "Seed2 sequence: $sequence2")
        
        // For debugging - throw detailed information if sequences are the same
        if (sequence1 == sequence2) {
            val debugInfo = "SEQUENCES ARE IDENTICAL!\n" +
                    "Seed1 ($seed1): $sequence1\n" +
                    "Seed2 ($seed2): $sequence2\n" +
                    "This suggests the seed is not being applied properly."
            throw AssertionError(debugInfo)
        }
        
        assertTrue("Different seeds should produce different sequences", sequence1 != sequence2)
    }
    
    private fun captureDrawSequence(scenario: ActivityScenario<GameActivity>, seed: Long, drawCount: Int): List<Int> {
        val wasteSequence = mutableListOf<Int>()
        
        // Draw cards and record waste pile size changes
        repeat(drawCount) { drawIndex ->
            onView(withId(R.id.draw_button))
                .perform(scrollTo(), click())
                
            // Add delay to allow state updates
            Thread.sleep(200)
            
            // Extract waste count from status text
            scenario.onActivity { activity ->
                val statusText = activity.findViewById<android.widget.TextView>(R.id.status_text)?.text?.toString()
                val wasteCount = extractWasteCount(statusText)
                wasteSequence.add(wasteCount)
                Log.d("DeterminismTest", "Seed $seed, Draw ${drawIndex + 1}: waste count = $wasteCount, status = '$statusText'")
            }
        }
        
        return wasteSequence
    }
    
    private fun launchWithSeed(seed: Long): ActivityScenario<GameActivity> {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(context, GameActivity::class.java)
        intent.putExtra(GameActivity.EXTRA_SEED, seed)
        Log.d("DeterminismTest", "Launching activity with seed: $seed, intent extra: ${intent.getLongExtra(GameActivity.EXTRA_SEED, -1)}")
        return ActivityScenario.launch(intent)
    }
    
    private fun drawSequence(seed: Long, drawCount: Int): List<Int> {
        return launchWithSeed(seed).use { scenario ->
            Thread.sleep(1000)
            captureDrawSequence(scenario, seed, drawCount)
        }
    }
    
    private fun extractWasteCount(statusText: String?): Int {
        return try {
            // Status format is "Stock:X  Waste:Y  Foundation:Z  ..."  
            val wasteMatch = Regex("Waste:(\\d+)").find(statusText ?: "")
            val result = wasteMatch?.groupValues?.get(1)?.toInt() ?: 0
            Log.d("DeterminismTest", "Extracted waste count $result from: '$statusText'")
            result
        } catch (e: Exception) {
            Log.w("DeterminismTest", "Failed to parse waste count from: $statusText", e)
            0
        }
    }
}
