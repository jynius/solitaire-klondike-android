package us.jyni.game.klondike.sync

import android.content.Context
import us.jyni.game.klondike.util.stats.SolveCodec
import us.jyni.game.klondike.util.stats.SolveStats
import us.jyni.game.klondike.util.sync.Ruleset
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
    
    // ==================== 데이터 내보내기/불러오기 ====================
    
    /**
     * 모든 통계 데이터를 JSON으로 내보내기 (즐겨찾기 포함)
     */
    @Synchronized
    fun exportToJson(): String {
        val allStats = readAllStats()
        val favoriteIds = readFavoriteIds()
        
        val data = mapOf(
            "version" to "1.0",
            "exportedAt" to System.currentTimeMillis(),
            "stats" to allStats.map { stats ->
                mapOf(
                    "dealId" to stats.dealId,
                    "seed" to stats.seed.toString(),
                    "rules" to mapOf(
                        "draw" to stats.rules.draw,
                        "redeals" to stats.rules.redeals,
                        "recycle" to stats.rules.recycle.name,
                        "allowFoundationToTableau" to stats.rules.allowFoundationToTableau
                    ),
                    "startedAt" to stats.startedAt,
                    "finishedAt" to stats.finishedAt,
                    "durationMs" to stats.durationMs,
                    "moveCount" to stats.moveCount,
                    "outcome" to stats.outcome,
                    "clientVersion" to stats.clientVersion,
                    "platform" to stats.platform,
                    "isFavorite" to favoriteIds.contains(getFavoriteId(stats))
                )
            }
        )
        
        return org.json.JSONObject(data).toString(2)
    }
    
    /**
     * JSON에서 통계 데이터 불러오기 (병합 모드)
     * @param jsonString JSON 문자열
     * @param clearExisting true면 기존 데이터 삭제 후 불러오기
     * @return 불러온 게임 수
     */
    @Synchronized
    fun importFromJson(jsonString: String, clearExisting: Boolean = false): Int {
        try {
            val jsonObj = org.json.JSONObject(jsonString)
            val statsArray = jsonObj.getJSONArray("stats")
            
            // 기존 데이터 삭제
            if (clearExisting) {
                filePending().writeText("")
                fileUploaded().writeText("")
                fileFavorites().writeText("")
            }
            
            val existingDealIds = readAllStats().map { it.dealId }.toSet()
            val newFavorites = mutableSetOf<String>()
            var importedCount = 0
            
            for (i in 0 until statsArray.length()) {
                val item = statsArray.getJSONObject(i)
                val dealId = item.getString("dealId")
                
                // 중복 체크 (병합 모드일 때만)
                if (!clearExisting && existingDealIds.contains(dealId)) {
                    // 즐겨찾기 정보만 업데이트
                    if (item.optBoolean("isFavorite", false)) {
                        newFavorites.add(dealId)
                    }
                    continue
                }
                
                // SolveStats 복원
                val rulesObj = item.getJSONObject("rules")
                val rules = Ruleset(
                    draw = rulesObj.getInt("draw"),
                    redeals = rulesObj.getInt("redeals"),
                    recycle = us.jyni.game.klondike.util.sync.RecycleOrder.valueOf(
                        rulesObj.getString("recycle")
                    ),
                    allowFoundationToTableau = rulesObj.getBoolean("allowFoundationToTableau")
                )
                
                val stats = SolveStats(
                    dealId = dealId,
                    seed = item.getString("seed").toULong(),
                    rules = rules,
                    startedAt = item.getLong("startedAt"),
                    finishedAt = item.optLong("finishedAt", 0L).takeIf { it > 0 },
                    durationMs = item.getLong("durationMs"),
                    moveCount = item.getInt("moveCount"),
                    outcome = item.optString("outcome", null),
                    clientVersion = item.optString("clientVersion", "unknown"),
                    platform = item.optString("platform", "android")
                )
                
                // uploaded 파일에 추가 (이미 완료된 게임이므로)
                fileUploaded().appendText(SolveCodec.encode(stats) + "\n")
                
                // 즐겨찾기 체크
                if (item.optBoolean("isFavorite", false)) {
                    newFavorites.add(dealId)
                }
                
                importedCount++
            }
            
            // 즐겨찾기 업데이트
            if (newFavorites.isNotEmpty()) {
                val currentFavorites = if (clearExisting) {
                    mutableSetOf()
                } else {
                    readFavoriteIds().toMutableSet()
                }
                currentFavorites.addAll(newFavorites)
                writeFavoriteIds(currentFavorites)
            }
            
            return importedCount
        } catch (e: Exception) {
            android.util.Log.e("JsonlFileRepository", "Failed to import data", e)
            throw e
        }
    }
}
