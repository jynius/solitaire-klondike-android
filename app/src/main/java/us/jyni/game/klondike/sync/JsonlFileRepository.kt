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
    private fun fileFavorites(): File = File(dir(), "favorites.txt")

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
    
    /**
     * 모든 게임 기록 조회 (pending + uploaded)
     * 통계 화면에서 사용
     */
    @Synchronized
    fun readAllStats(): List<SolveStats> {
        val allLines = mutableListOf<String>()
        
        // pending 파일에서 읽기
        val pendingFile = filePending()
        if (pendingFile.exists()) {
            allLines.addAll(pendingFile.readLines().filter { it.isNotBlank() })
        }
        
        // uploaded 파일에서 읽기
        val uploadedFile = fileUploaded()
        if (uploadedFile.exists()) {
            allLines.addAll(uploadedFile.readLines().filter { it.isNotBlank() })
        }
        
        // SolveStats 객체로 파싱
        return allLines.mapNotNull { line ->
            try {
                SolveCodec.decode(line)
            } catch (e: Exception) {
                null // 파싱 실패 시 무시
            }
        }
    }
    
    /**
     * 승리한 게임만 조회
     */
    @Synchronized
    fun readWinStats(): List<SolveStats> {
        return readAllStats().filter { it.outcome == "win" }
    }
    
    /**
     * 패배/포기한 게임 조회
     */
    @Synchronized
    fun readLossStats(): List<SolveStats> {
        return readAllStats().filter { it.outcome != "win" && it.outcome != null }
    }
    
    // ==================== 즐겨찾기 관리 ====================
    
    /**
     * 즐겨찾기 ID 생성 (dealId 기반)
     */
    private fun getFavoriteId(stats: SolveStats): String = stats.dealId
    
    /**
     * 즐겨찾기 목록 읽기
     */
    @Synchronized
    private fun readFavoriteIds(): Set<String> {
        val f = fileFavorites()
        if (!f.exists()) return emptySet()
        return f.readLines().filter { it.isNotBlank() }.toSet()
    }
    
    /**
     * 즐겨찾기 목록 저장
     */
    @Synchronized
    private fun writeFavoriteIds(ids: Set<String>) {
        fileFavorites().writeText(ids.joinToString("\n", postfix = if (ids.isNotEmpty()) "\n" else ""))
    }
    
    /**
     * 즐겨찾기 추가
     */
    @Synchronized
    fun addFavorite(stats: SolveStats) {
        val ids = readFavoriteIds().toMutableSet()
        ids.add(getFavoriteId(stats))
        writeFavoriteIds(ids)
    }
    
    /**
     * 즐겨찾기 제거
     */
    @Synchronized
    fun removeFavorite(stats: SolveStats) {
        val ids = readFavoriteIds().toMutableSet()
        ids.remove(getFavoriteId(stats))
        writeFavoriteIds(ids)
    }
    
    /**
     * 즐겨찾기 토글 (추가/제거)
     */
    @Synchronized
    fun toggleFavorite(stats: SolveStats): Boolean {
        val ids = readFavoriteIds().toMutableSet()
        val id = getFavoriteId(stats)
        val isNowFavorite = if (ids.contains(id)) {
            ids.remove(id)
            false
        } else {
            ids.add(id)
            true
        }
        writeFavoriteIds(ids)
        return isNowFavorite
    }
    
    /**
     * 즐겨찾기 여부 확인
     */
    @Synchronized
    fun isFavorite(stats: SolveStats): Boolean {
        return readFavoriteIds().contains(getFavoriteId(stats))
    }
    
    /**
     * 즐겨찾기한 게임만 조회
     */
    @Synchronized
    fun readFavoriteStats(): List<SolveStats> {
        val favoriteIds = readFavoriteIds()
        return readAllStats().filter { favoriteIds.contains(getFavoriteId(it)) }
    }
    
    // ==================== 필터링 & 페이징 ====================
    
    enum class GameFilter {
        ALL,        // 전체
        FAVORITE,   // 즐겨찾기
        WIN,        // 성공한 게임
        LOSS        // 실패한 게임
    }
    
    enum class SortOrder {
        NEWEST_FIRST,   // 최신순
        OLDEST_FIRST,   // 오래된순
        MOST_MOVES,     // 이동 많은순
        LEAST_MOVES,    // 이동 적은순
        LONGEST_TIME,   // 시간 긴순
        SHORTEST_TIME   // 시간 짧은순
    }
    
    /**
     * 필터링 및 정렬된 게임 목록 조회
     */
    @Synchronized
    fun readFilteredStats(
        filter: GameFilter = GameFilter.ALL,
        sortOrder: SortOrder = SortOrder.NEWEST_FIRST
    ): List<SolveStats> {
        // 필터링
        val filtered = when (filter) {
            GameFilter.ALL -> readAllStats()
            GameFilter.FAVORITE -> readFavoriteStats()
            GameFilter.WIN -> readWinStats()
            GameFilter.LOSS -> readLossStats()
        }
        
        // 정렬
        return when (sortOrder) {
            SortOrder.NEWEST_FIRST -> filtered.sortedByDescending { it.startedAt }
            SortOrder.OLDEST_FIRST -> filtered.sortedBy { it.startedAt }
            SortOrder.MOST_MOVES -> filtered.sortedByDescending { it.moveCount }
            SortOrder.LEAST_MOVES -> filtered.sortedBy { it.moveCount }
            SortOrder.LONGEST_TIME -> filtered.sortedByDescending { it.durationMs }
            SortOrder.SHORTEST_TIME -> filtered.sortedBy { it.durationMs }
        }
    }
    
    /**
     * 페이징된 게임 목록 조회
     * @param page 페이지 번호 (0부터 시작)
     * @param pageSize 페이지당 항목 수
     * @param filter 필터 조건
     * @param sortOrder 정렬 순서
     * @return 페이징된 결과
     */
    @Synchronized
    fun readPagedStats(
        page: Int = 0,
        pageSize: Int = 20,
        filter: GameFilter = GameFilter.ALL,
        sortOrder: SortOrder = SortOrder.NEWEST_FIRST
    ): PagedResult<SolveStats> {
        val allFiltered = readFilteredStats(filter, sortOrder)
        val totalItems = allFiltered.size
        val totalPages = (totalItems + pageSize - 1) / pageSize
        
        val startIndex = page * pageSize
        val endIndex = minOf(startIndex + pageSize, totalItems)
        
        val items = if (startIndex < totalItems) {
            allFiltered.subList(startIndex, endIndex)
        } else {
            emptyList()
        }
        
        return PagedResult(
            items = items,
            page = page,
            pageSize = pageSize,
            totalItems = totalItems,
            totalPages = totalPages,
            hasNext = page < totalPages - 1,
            hasPrevious = page > 0
        )
    }
    
    /**
     * 페이징 결과 데이터 클래스
     */
    data class PagedResult<T>(
        val items: List<T>,
        val page: Int,
        val pageSize: Int,
        val totalItems: Int,
        val totalPages: Int,
        val hasNext: Boolean,
        val hasPrevious: Boolean
    )
}
