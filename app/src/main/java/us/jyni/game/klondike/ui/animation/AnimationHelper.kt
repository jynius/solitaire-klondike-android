package us.jyni.game.klondike.ui.animation

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.children
import us.jyni.game.klondike.ui.components.CardView
import us.jyni.game.klondike.model.Card
import android.view.animation.AccelerateDecelerateInterpolator

class AnimationHelper(
    private val context: Context,
    private val contentView: ViewGroup
) {
    /**
     * 카드를 시작 위치에서 끝 위치로 애니메이션하며 이동
     */
    fun animateCard(
        card: Card,
        startView: View,
        endView: View,
        duration: Long = 600,
        onComplete: () -> Unit = {}
    ) {
        // 1. 정확한 좌표 계산 (부모 기준)
        val startX = startView.getRelativeX(contentView)
        val startY = startView.getRelativeY(contentView)
        val endX = endView.getRelativeX(contentView)
        val endY = endView.getRelativeY(contentView)

        // 2. 크기 명시적 계산
        val cardWidth = if (startView.width > 0) startView.width else (60 * context.resources.displayMetrics.density).toInt()
        val cardHeight = if (startView.height > 0) startView.height else (84 * context.resources.displayMetrics.density).toInt()

        // 3. TrailView 생성 및 '사전 설정'
        val trailView = CardView(context).apply {
            setCard(card) // 내부에서 invalidate() 호출됨 확인
            elevation = 500f // 다른 모든 요소보다 위에 표시
            
            // 중요: addView 하기 전에 위치와 크기를 미리 설정합니다.
            this.x = startX
            this.y = startY
            this.alpha = 1f 
        }

        // 4. LayoutParams 설정
        val params = FrameLayout.LayoutParams(cardWidth, cardHeight).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        // 5. 부모 뷰에 추가 (이 시점에 이미 x, y는 설정되어 있음)
        contentView.addView(trailView, params)

        // 6. 즉시 애니메이션 시작 (post 없이 실행)
        trailView.animate()
            .x(endX)
            .y(endY)
            .setDuration(duration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                contentView.removeView(trailView)
                onComplete()
            }
            .start()
    }
}

/**
 * contentView 기준 상대 X 좌표 계산
 */
fun View.getRelativeX(contentView: ViewGroup): Float {
    val contentLocation = IntArray(2)
    val viewLocation = IntArray(2)
    
    contentView.getLocationInWindow(contentLocation)
    this.getLocationInWindow(viewLocation)
    
    return (viewLocation[0] - contentLocation[0]).toFloat()
}

/**
 * contentView 기준 상대 Y 좌표 계산
 */
fun View.getRelativeY(contentView: ViewGroup): Float {
    val contentLocation = IntArray(2)
    val viewLocation = IntArray(2)
    
    contentView.getLocationInWindow(contentLocation)
    this.getLocationInWindow(viewLocation)
    
    return (viewLocation[1] - contentLocation[1]).toFloat()
}
