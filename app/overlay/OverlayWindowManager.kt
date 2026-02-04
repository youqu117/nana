package app.overlay

import android.content.Context
import android.graphics.Point
import android.os.Handler
import android.os.Looper
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import android.graphics.Rect
import kotlin.math.max
import kotlin.math.min
import app.input.GestureHandler
import app.pet.PetController
import app.pet.PetView
import kotlin.random.Random
import android.animation.ValueAnimator

class OverlayWindowManager(private val context: Context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val petView = PetView(context)
    private val petController = PetController(petView)
    private var isShowing = false
    private var isDragging = false
    private val handler = Handler(Looper.getMainLooper())
    private var autoMoveRunnable: Runnable? = null
    private var autoMoveAnimator: ValueAnimator? = null

    private val layoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        },
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        android.graphics.PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = 0
        y = 200
    }

    private val gestureHandler = GestureHandler(
        onTap = { petController.playTapFeedback() },
        onDragStart = { pauseAutoMove() },
        onDrag = { dx, dy -> moveBy(dx, dy) },
        onDragEnd = {
            snapToEdge()
            resumeAutoMove()
        }
    )

    init {
        petView.setOnTouchListener(gestureHandler)
    }

    fun show() {
        if (isShowing) return
        windowManager.addView(petView, layoutParams)
        isShowing = true
        petView.post { startAutoMove() }
    }

    fun hide() {
        if (!isShowing) return
        stopAutoMove()
        if (petView.parent != null) {
            windowManager.removeView(petView)
        }
        isShowing = false
    }

    fun moveTo(x: Int, y: Int) {
        if (!isShowing || petView.parent == null) return
        val clamped = clampToScreen(x, y)
        layoutParams.x = clamped.first
        layoutParams.y = clamped.second
        windowManager.updateViewLayout(petView, layoutParams)
    }

    fun moveBy(dx: Int, dy: Int) {
        moveTo(layoutParams.x + dx, layoutParams.y + dy)
    }

    fun snapToEdge() {
        val screenSize = getScreenSize()
        val viewWidth = petView.width
        if (viewWidth == 0) return
        val maxX = max(EDGE_PADDING_PX, screenSize.x - viewWidth - EDGE_PADDING_PX)
        val targetX = if (layoutParams.x < screenSize.x / 2) {
            EDGE_PADDING_PX
        } else {
            maxX
        }
        moveTo(targetX, layoutParams.y)
    }

    private fun startAutoMove() {
        if (!isShowing) return
        scheduleNextAutoMove()
    }

    private fun stopAutoMove() {
        autoMoveRunnable?.let { handler.removeCallbacks(it) }
        autoMoveRunnable = null
        autoMoveAnimator?.cancel()
        autoMoveAnimator = null
    }

    private fun pauseAutoMove() {
        isDragging = true
        stopAutoMove()
    }

    private fun resumeAutoMove() {
        isDragging = false
        scheduleNextAutoMove()
    }

    private fun scheduleNextAutoMove() {
        if (!isShowing || isDragging) return
        if (autoMoveRunnable != null) return
        val delayMs = Random.nextLong(AUTO_MOVE_DELAY_MIN_MS, AUTO_MOVE_DELAY_MAX_MS)
        val runnable = Runnable {
            if (!isShowing || isDragging) return@Runnable
            performAutoMove()
            scheduleNextAutoMove()
        }
        autoMoveRunnable = runnable
        handler.postDelayed(runnable, delayMs)
    }

    private fun performAutoMove() {
        val screenSize = getScreenSize()
        val viewWidth = petView.width
        val viewHeight = petView.height
        if (viewWidth == 0 || viewHeight == 0) {
            autoMoveRunnable = null
            scheduleNextAutoMove()
            return
        }

        val startX = layoutParams.x
        val startY = layoutParams.y
        val edgeBand = EDGE_BAND_PX
        val maxX = max(EDGE_PADDING_PX, screenSize.x - viewWidth - EDGE_PADDING_PX)
        val maxY = max(EDGE_PADDING_PX, screenSize.y - viewHeight - EDGE_PADDING_PX)
        val snapLeft = Random.nextBoolean()
        val targetX = if (snapLeft) {
            EDGE_PADDING_PX + Random.nextInt(0, min(edgeBand, maxX - EDGE_PADDING_PX) + 1)
        } else {
            max(EDGE_PADDING_PX, maxX - edgeBand) + Random.nextInt(0, min(edgeBand, maxX - EDGE_PADDING_PX) + 1)
        }
        val targetY = Random.nextInt(EDGE_PADDING_PX, maxY + 1)

        autoMoveAnimator?.cancel()
        autoMoveAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = AUTO_MOVE_ANIM_MS
            addUpdateListener { animation ->
                val fraction = animation.animatedFraction
                val newX = startX + ((targetX - startX) * fraction).toInt()
                val newY = startY + ((targetY - startY) * fraction).toInt()
                moveTo(newX, newY)
            }
            doOnEndOrCancel { autoMoveAnimator = null }
        }
        autoMoveAnimator?.start()
    }

    private fun clampToScreen(x: Int, y: Int): Pair<Int, Int> {
        val screenSize = getScreenSize()
        val viewWidth = petView.width
        val viewHeight = petView.height
        if (viewWidth == 0 || viewHeight == 0) {
            return Pair(x, y)
        }
        val maxX = max(EDGE_PADDING_PX, screenSize.x - viewWidth - EDGE_PADDING_PX)
        val maxY = max(EDGE_PADDING_PX, screenSize.y - viewHeight - EDGE_PADDING_PX)
        return Pair(
            x.coerceIn(EDGE_PADDING_PX, maxX),
            y.coerceIn(EDGE_PADDING_PX, maxY)
        )
    }

    private fun getScreenSize(): Point {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds: Rect = windowManager.currentWindowMetrics.bounds
            Point(bounds.width(), bounds.height())
        } else {
            @Suppress("DEPRECATION")
            Point().also { windowManager.defaultDisplay.getSize(it) }
        }
    }

    companion object {
        private const val EDGE_PADDING_PX = 12
        private const val EDGE_BAND_PX = 120
        private const val AUTO_MOVE_ANIM_MS = 600L
        private const val AUTO_MOVE_DELAY_MIN_MS = 3000L
        private const val AUTO_MOVE_DELAY_MAX_MS = 6000L
    }
}

private fun ValueAnimator.doOnEndOrCancel(block: () -> Unit) {
    addListener(object : android.animation.Animator.AnimatorListener {
        override fun onAnimationStart(animation: android.animation.Animator) {}
        override fun onAnimationEnd(animation: android.animation.Animator) = block()
        override fun onAnimationCancel(animation: android.animation.Animator) = block()
        override fun onAnimationRepeat(animation: android.animation.Animator) {}
    })
}
