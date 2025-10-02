package us.jyni.game

import us.jyni.game.klondike.util.sync.LayoutId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutContractTest {
    @Test
    fun initialLayoutSerializationContract() {
        // 가상의 초기 배치를 구성: 7열, 각 열의 맨 위는 앞면(u), 나머지는 뒷면(d)
        val tableau = List(7) { col ->
            val size = col + 1
            List(size) { idx ->
                val face = if (idx == size - 1) 'u' else 'd'
                // 예시 카드 표기(수트/랭크는 단순 문자열)
                "S:${idx + 1}:$face"
            }
        }
        val foundation = List(4) { emptyList<String>() }
        val stock = listOf("H:10:d", "D:J:d") // 예: 맨 위가 마지막 요소라는 계약을 가정
        val waste = emptyList<String>()

        val json = LayoutId.canonicalJson(0, tableau, foundation, stock, waste)
        // 필드 순서/공백 검사(간접): 키 존재 순서 확인
        val expectedPrefix = "{\"lv\":0,\"tableau\":"
        assertTrue(json.startsWith(expectedPrefix))
        assertTrue(json.contains("\"foundation\":["))
        assertTrue(json.contains("\"stock\":["))
        assertTrue(json.endsWith("]}"))

        val id = LayoutId.generate(tableau, foundation, stock, waste, lv = 0)
        assertTrue(id.startsWith("L1_"))

        // 동일 레이아웃 → 동일 ID
        val id2 = LayoutId.generate(tableau, foundation, stock, waste, lv = 0)
        assertEquals(id, id2)
    }
}
