package app.overlay

import android.content.Context
import android.graphics.Point
import android.os.Handler
import android.os.Looper
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import android.graphics.Rect
import app.data.PetInstanceEntity
import app.data.PetRepository
import app.input.GestureHandler
import app.pet.PetBehavior
import app.pet.PetController
import app.pet.PetRuntime
import app.pet.PetState
import app.pet.PetView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.max

import app.content.AssetLoader

class OverlayWindowManager(
    private val context: Context,
    private val repository: PetRepository,
    private val scope: CoroutineScope
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    
    private val petView = PetView(context)
    
    // Runtime & Controller (Mutable to support switching pets)
    private var petRuntime: PetRuntime? = null
    private var petController: PetController? = null
    
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
        onTap = { petController?.onTap() },
        onDoubleTap = { petController?.onDoubleTap() },
        onLongPress = { petController?.onLongPress() },
        onDragStart = { 
            petController?.onLongPress() // Treat drag start as held
        },
        onDrag = { dx, dy -> 
            moveBy(dx, dy) 
        },
        onDragEnd = {
            petController?.onRelease()
            snapToEdge()
        }
    )

    init {
        petView.setOnTouchListener(gestureHandler)
        
        scope.launch {
            // Observe Global Settings
            launch {
                repository.getSetting("scale").collect { scaleStr ->
                    val scale = scaleStr?.toFloatOrNull() ?: 1.5f
                    petView.scaleX = scale
                    petView.scaleY = scale
                }
            }
            
            launch {
                repository.getSetting("alpha").collect { alphaStr ->
                    val alpha = alphaStr?.toFloatOrNull() ?: 1.0f
                    layoutParams.alpha = alpha.coerceIn(0.1f, 1.0f)
                    if (isShowing && petView.parent != null) {
                        windowManager.updateViewLayout(petView, layoutParams)
                    }
                }
            }

            repository.enabledInstances.collect { instances ->
                if (instances.isNotEmpty()) {
                    val newEntity = instances.first()
                    
                    // 1. Load Assets / Runtime if asset changed
                    if (currentEntity?.assetId != newEntity.assetId || petRuntime == null) {
                        // Move heavy I/O to IO dispatcher to avoid ANR
                        launch(kotlinx.coroutines.Dispatchers.IO) {
                            val manifest = AssetLoader.loadManifest(context, newEntity.assetId)
                            if (manifest != null) {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    petView.loadAssets(manifest)
                                    val rt = PetRuntime(manifest)
                                    petRuntime = rt
                                    petController = PetController(petView, rt)
                                    
                                    // 2. Restore State if instance changed (nested to ensure runtime is ready)
                                    if (currentEntity?.instanceId != newEntity.instanceId) {
                                         val loadedState = PetState(
                                            energy = newEntity.energy,
                                            mood = newEntity.mood,
                                            affection = newEntity.affection,
                                            lastTickMs = newEntity.lastTickTime
                                        )
                                        val caughtUpState = loadedState.tick(System.currentTimeMillis())
                                        petRuntime?.restoreState(caughtUpState)
                                    }
                                    currentEntity = newEntity
                                }
                            }
                        }
                    } else if (currentEntity?.instanceId != newEntity.instanceId) {
                         // Only state restore if asset is same
                         val loadedState = PetState(
                            energy = newEntity.energy,
                            mood = newEntity.mood,
                            affection = newEntity.affection,
                            lastTickMs = newEntity.lastTickTime
                        )
                        val caughtUpState = loadedState.tick(System.currentTimeMillis())
                        petRuntime?.restoreState(caughtUpState)
                        currentEntity = newEntity
                    }
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
        val s = petRuntime?.state ?: return
        val updated = entity.copy(
            energy = s.energy,
            mood = s.mood,
            affection = s.affection,
            lastTickTime = s.lastTickMs
        )
        scope.launch {
            repository.updateInstance(updated)
        }
    }

    private fun startLoop() {
        handler.post(loopRunnable)
    }

    private fun stopLoop() {
        handler.removeCallbacks(loopRunnable)
    }

    // --- Movement Logic ---
    
    // Smooth easing function: ease-in-out cubic
    private fun easeInOutCubic(t: Float): Float {
        return if (t < 0.5f) 4 * t * t * t else 1 - Math.pow((-2 * t + 2).toDouble(), 3.0).toFloat() / 2
    }

    private var targetX = 0
    private var startX = 0
    private var moveStartTime = 0L
    private var moveDuration = 0L
    private var isMovingToTarget = false

    private fun pickNewTarget() {
        val screenSize = getScreenSize()
        val viewWidth = petView.width
        if (viewWidth == 0) return
        
        // Target Sampling: Edge band (0~120px from edges)
        val edgeBand = 120
        val isLeft = kotlin.random.Random.nextBoolean()
        
        val randomOffset = kotlin.random.Random.nextInt(0, edgeBand)
        val destX = if (isLeft) {
            EDGE_PADDING_PX + randomOffset
        } else {
            screenSize.x - viewWidth - EDGE_PADDING_PX - randomOffset
        }
        
        targetX = destX.coerceIn(EDGE_PADDING_PX, screenSize.x - viewWidth - EDGE_PADDING_PX)
        startX = layoutParams.x
        moveStartTime = System.currentTimeMillis()
        
        // Duration based on distance (speed = ~300px/s)
        val distance = kotlin.math.abs(targetX - startX)
        moveDuration = (distance * 3).toLong().coerceAtLeast(1000L) // Min 1s
        isMovingToTarget = true
        
        // Face direction
        if (targetX > startX) {
            moveDirectionX = 1
            petView.scaleX = -petView.scaleY // Flip (assuming sprite faces left by default)
        } else {
            moveDirectionX = -1
            petView.scaleX = petView.scaleY
        }
    }

    private val loopRunnable = object : Runnable {
        override fun run() {
            if (!isShowing) return

            petController?.update()
            
            // Save state periodically (every 30s)
            val now = System.currentTimeMillis()
            if (now - lastSaveTime > 30000) {
                lastSaveTime = now
                saveState()
            }
            
            // Handle Movement based on Behavior
            val state = petRuntime?.state ?: run {
                handler.postDelayed(this, 33)
                return
            }
            
            // Frame Rate Throttling
            val delay = if (state.behavior == PetBehavior.IDLE || state.behavior == PetBehavior.SLEEP) {
                83L // ~12 FPS for Idle/Sleep
            } else {
                33L // ~30 FPS for Active
            }
            
            if (state.behavior == PetBehavior.WALK || state.behavior == PetBehavior.RUN) {
                if (!isMovingToTarget) {
                     pickNewTarget()
                }
                
                val elapsedMove = now - moveStartTime
                if (elapsedMove < moveDuration) {
                    val progress = elapsedMove.toFloat() / moveDuration
                    val easedProgress = easeInOutCubic(progress)
                    val currentX = startX + (targetX - startX) * easedProgress
                    moveTo(currentX.toInt(), layoutParams.y)
                } else {
                    // Reached target
                    isMovingToTarget = false
                    // Optional: pause or switch to idle handled by Runtime
                }
            } else {
                isMovingToTarget = false // Reset if behavior changes
            }

            handler.postDelayed(this, delay) 
        }
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
    }
}

