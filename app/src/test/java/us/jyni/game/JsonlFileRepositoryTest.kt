package us.jyni.game

import org.junit.Assert.*
import org.junit.Test
import us.jyni.game.klondike.sync.JsonlFileRepository
import us.jyni.game.klondike.util.stats.SolveStats
import us.jyni.game.klondike.util.sync.RecycleOrder
import us.jyni.game.klondike.util.sync.Ruleset

class JsonlFileRepositoryTest {
    @Test
    fun append_read_markUploaded_cycle() {
        val tmp = createTempDir(prefix = "jsonl_repo_")
        try {
            val repo = JsonlFileRepository(tmp)
            val rules = Ruleset(1, -1, RecycleOrder.REVERSE, false)
            val s1 = SolveStats("DL1_x", 1uL, rules, 1L, null, 100L, 3, null)
            val s2 = SolveStats("DL1_y", 2uL, rules, 2L, null, 200L, 5, null)
            repo.appendPending(s1)
            repo.appendPending(s2)
            val read = repo.readPending()
            assertEquals(2, read.size)
            repo.markUploaded(listOf(read[0]))
            val read2 = repo.readPending()
            assertEquals(1, read2.size)
        } finally {
            tmp.deleteRecursively()
        }
    }
}
