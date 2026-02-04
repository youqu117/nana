package app.overlay

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import app.input.GestureHandler
import app.pet.PetController
import app.pet.PetView

class OverlayWindowManager(private val context: Context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val petView = PetView(context)
    private val petController = PetController(petView)
    private var isShowing = false

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
        onDrag = { dx, dy -> updatePosition(dx, dy) },
        onDragEnd = { snapToEdge() }
    )

    init {
        petView.setOnTouchListener(gestureHandler)
    }

    fun show() {
        if (isShowing) return
        windowManager.addView(petView, layoutParams)
        isShowing = true
    }

    fun hide() {
        if (!isShowing) return
        windowManager.removeView(petView)
        isShowing = false
    }

    private fun updatePosition(dx: Int, dy: Int) {
        layoutParams.x += dx
        layoutParams.y += dy
        windowManager.updateViewLayout(petView, layoutParams)
    }

    private fun snapToEdge() {
        val screenSize = Point()
        windowManager.defaultDisplay.getSize(screenSize)
        val viewWidth = petView.width
        val maxX = screenSize.x - viewWidth
        layoutParams.x = if (layoutParams.x < screenSize.x / 2) 0 else maxX
        windowManager.updateViewLayout(petView, layoutParams)
    }
}
