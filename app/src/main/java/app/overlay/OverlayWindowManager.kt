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
import app.pet.PetRuntime
import app.pet.PetBehavior
import app.pet.PetState
import app.content.ContentPackManifest
import app.content.Hitbox
import app.content.Anchors
import app.data.PetRepository
import app.data.PetInstanceEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlin.random.Random
import android.animation.ValueAnimator

class OverlayWindowManager(
    private val context: Context,
    private val repository: PetRepository,
    private val scope: CoroutineScope
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    
    // Dummy Manifest for MVP
    private val dummyManifest = ContentPackManifest(
        id = "shiba_default",
        name = "Shiba",
        version = 1,
        preview = "",
        staticNormal = "pets/dog_shiba/static/pet_normal.png",
        staticTongue = "pets/dog_shiba/static/pet_tongue.png",
        idleSheet = "",
        idleAnim = "",
        defaultScale = 3,
        hitbox = Hitbox(0,0,32,30),
        anchors = Anchors(0,0,0,0,0,0)
    )

    private val petView = PetView(context)
    private val petRuntime = PetRuntime(dummyManifest)
    private val petController = PetController(petView, petRuntime)
    
    private var isShowing = false
    private val handler = Handler(Looper.getMainLooper())
    
    private var moveDirectionX = 1
    private var moveSpeed = 2
    
    private var currentEntity: PetInstanceEntity? = null
    private var lastSaveTime = 0L

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
        onTap = { petController.onTap() },
        onDoubleTap = { petController.onDoubleTap() },
        onLongPress = { petController.onLongPress() },
        onDragStart = { 
            petController.onLongPress() // Treat drag start as held
        },
        onDrag = { dx, dy -> 
            moveBy(dx, dy) 
        },
        onDragEnd = {
            petController.onRelease()
            snapToEdge()
        }
    )

    init {
        petView.setOnTouchListener(gestureHandler)
        
        scope.launch {
            repository.enabledInstances.collect { instances ->
                if (instances.isNotEmpty()) {
                    val newEntity = instances.first()
                    // If switching pets or first load, restore state
                    if (currentEntity?.instanceId != newEntity.instanceId) {
                         val loadedState = PetState(
                            energy = newEntity.energy,
                            mood = newEntity.mood,
                            affection = newEntity.affection,
                            lastTickMs = System.currentTimeMillis()
                        )
                        petRuntime.restoreState(loadedState)
                    }
                    currentEntity = newEntity
                }
            }
        }
    }

    fun show() {
        if (isShowing) return
        windowManager.addView(petView, layoutParams)
        isShowing = true
        startLoop()
    }

    fun hide() {
        if (!isShowing) return
        saveState()
        stopLoop()
        if (petView.parent != null) {
            windowManager.removeView(petView)
        }
        isShowing = false
    }
    
    private fun saveState() {
        val entity = currentEntity ?: return
        val s = petRuntime.state
        val updated = entity.copy(
            energy = s.energy,
            mood = s.mood,
            affection = s.affection
        )
        scope.launch {
            repository.updateInstance(updated)
        }
    }

    private val loopRunnable = object : Runnable {
        override fun run() {
            if (!isShowing) return

            petController.update()
            
            // Save state periodically (every 30s)
            val now = System.currentTimeMillis()
            if (now - lastSaveTime > 30000) {
                lastSaveTime = now
                saveState()
            }
            
            // Handle Movement based on Behavior
            val state = petRuntime.state
            if (state.behavior == PetBehavior.WALK) {
                // Bounce off edges
                val screenSize = getScreenSize()
                val nextX = layoutParams.x + (moveDirectionX * moveSpeed)
                
                if (nextX <= EDGE_PADDING_PX || nextX + petView.width >= screenSize.x - EDGE_PADDING_PX) {
                    moveDirectionX *= -1
                    // Flip view?
                    // petView.scaleX = moveDirectionX.toFloat() 
                }
                
                moveBy(moveDirectionX * moveSpeed, 0)
            } else if (state.behavior == PetBehavior.RUN) {
                 // Faster movement
                 val screenSize = getScreenSize()
                 val speed = moveSpeed * 3
                 val nextX = layoutParams.x + (moveDirectionX * speed)
                 
                 if (nextX <= EDGE_PADDING_PX || nextX + petView.width >= screenSize.x - EDGE_PADDING_PX) {
                     moveDirectionX *= -1
                 }
                 moveBy(moveDirectionX * speed, 0)
            }

            handler.postDelayed(this, 33) // ~30 FPS
        }
    }

    private fun startLoop() {
        handler.post(loopRunnable)
    }

    private fun stopLoop() {
        handler.removeCallbacks(loopRunnable)
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
            autoMoveRunnable = null
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
