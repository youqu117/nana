package app.input

import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

class GestureHandler(
    private val onTap: () -> Unit,
    private val onDrag: (dx: Int, dy: Int) -> Unit,
    private val onDragEnd: () -> Unit
) : View.OnTouchListener {
    private var lastX = 0f
    private var lastY = 0f
    private var downX = 0f
    private var downY = 0f
    private var downTime = 0L
    private var isDragging = false

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                lastX = downX
                lastY = downY
                downTime = System.currentTimeMillis()
                isDragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - lastX).toInt()
                val dy = (event.rawY - lastY).toInt()
                if (abs(event.rawX - downX) > TAP_SLOP || abs(event.rawY - downY) > TAP_SLOP) {
                    isDragging = true
                }
                if (isDragging) {
                    onDrag(dx, dy)
                    lastX = event.rawX
                    lastY = event.rawY
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                val elapsed = System.currentTimeMillis() - downTime
                if (!isDragging && elapsed < TAP_TIMEOUT) {
                    onTap()
                } else {
                    onDragEnd()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                onDragEnd()
                return true
            }
        }
        return false
    }

    companion object {
        private const val TAP_SLOP = 8
        private const val TAP_TIMEOUT = 250L
    }
}
