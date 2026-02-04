package app.pet

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

class PetView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var sprite: Bitmap? = null
    private var showTapFeedback = false

    fun setSprite(bitmap: Bitmap) {
        sprite = bitmap
        requestLayout()
        invalidate()
    }

    fun triggerTapFeedback() {
        showTapFeedback = true
        invalidate()
        postDelayed({
            showTapFeedback = false
            invalidate()
        }, 500L)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val spriteWidth = sprite?.width ?: 160
        val spriteHeight = sprite?.height ?: 160
        setMeasuredDimension(spriteWidth, spriteHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bitmap = sprite
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
        } else {
            paint.color = if (showTapFeedback) Color.MAGENTA else Color.CYAN
            canvas.drawCircle(width / 2f, height / 2f, width.coerceAtMost(height) / 2f, paint)
        }
    }
}
