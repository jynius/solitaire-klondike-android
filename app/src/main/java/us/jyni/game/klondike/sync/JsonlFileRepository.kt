package us.jyni.game.klondike.sync

import android.content.Context
import us.jyni.game.klondike.util.stats.SolveCodec
import us.jyni.game.klondike.util.stats.SolveStats
import java.io.File

/**
 * 매우 단순한 로컬 보관소: 지정된 디렉터리에 JSONL(정확히는 SV1 키=값 세미콜론 포맷)로 보관.
 * Room으로 전환하기 전까지 임시 사용.
 */
class JsonlFileRepository(private val baseDir: File) {
    constructor(context: Context): this(File(context.filesDir, "solves"))
    private fun dir(): File = baseDir.apply { mkdirs() }
    private fun filePending(): File = File(dir(), "pending.sv1")
    private fun fileUploaded(): File = File(dir(), "uploaded.sv1")

    @Synchronized
    fun appendPending(s: SolveStats) {
        filePending().appendText(SolveCodec.encode(s) + "\n")
    }

    @Synchronized
    fun readPending(limit: Int = 100): List<String> {
        val f = filePending()
        if (!f.exists()) return emptyList()
        val lines = f.readLines().filter { it.isNotBlank() }
        return if (lines.size <= limit) lines else lines.subList(0, limit)
    }

    @Synchronized
    fun pendingCount(): Int {
        val f = filePending()
        if (!f.exists()) return 0
        return f.readLines().count { it.isNotBlank() }
    }

    @Synchronized
    fun markUploaded(uploadedLines: List<String>) {
        if (uploadedLines.isEmpty()) return
        val f = filePending()
        if (!f.exists()) return
        val remaining = f.readLines().filter { it.isNotBlank() }.toMutableList()
        uploadedLines.forEach { remaining.remove(it) }
        f.writeText(remaining.joinToString("\n", postfix = if (remaining.isNotEmpty()) "\n" else ""))
        // 보관
        val uf = fileUploaded()
        uf.appendText(uploadedLines.joinToString("\n", postfix = "\n"))
    }
}
