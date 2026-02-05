package app.input

import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.abs

class GestureHandler(
    private val onTap: () -> Unit,
    private val onDragStart: () -> Unit,
    private val onDrag: (dx: Int, dy: Int) -> Unit,
    private val onDragEnd: () -> Unit
) : View.OnTouchListener {
    private var touchSlop = 0
    private var lastX = 0f
    private var lastY = 0f
    private var downX = 0f
    private var downY = 0f
    private var downTime = 0L
    private var isDragging = false

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        if (touchSlop == 0) {
            touchSlop = ViewConfiguration.get(view.context).scaledTouchSlop
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                lastX = downX
                lastY = downY
                downTime = event.eventTime
                isDragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - lastX).toInt()
                val dy = (event.rawY - lastY).toInt()
                if (!isDragging && (abs(event.rawX - downX) > touchSlop || abs(event.rawY - downY) > touchSlop)) {
                    isDragging = true
                    onDragStart()
                }
                if (isDragging) {
                    onDrag(dx, dy)
                    lastX = event.rawX
                    lastY = event.rawY
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                val elapsed = event.eventTime - downTime
                if (!isDragging && elapsed < TAP_TIMEOUT) {
                    onTap()
                } else if (isDragging) {
                    onDragEnd()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    onDragEnd()
                }
                return true
            }
        }
        return false
    }

    companion object {
        private const val TAP_TIMEOUT = 250L
    }
}
