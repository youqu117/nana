package app.overlay

import android.content.Context
import android.graphics.Point
import android.os.Handler
import android.os.Looper
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import android.graphics.Rect
import android.view.View
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
    private val activePets = mutableMapOf<Long, OverlayPetUnit>()
    
    private var currentScale = 1.5f
    private var currentAlpha = 1.0f

    init {
        scope.launch {
            // Observe Settings
            launch {
                repository.getSetting("scale").collect { scaleStr ->
                    currentScale = scaleStr?.toFloatOrNull() ?: 1.5f
                    activePets.values.forEach { it.updateScale(currentScale) }
                }
            }
            
            launch {
                repository.getSetting("alpha").collect { alphaStr ->
                    currentAlpha = alphaStr?.toFloatOrNull() ?: 1.0f
                    activePets.values.forEach { it.updateAlpha(currentAlpha) }
                }
            }

            // Observe Enabled Pets
            repository.enabledInstances.collect { instances ->
                syncPets(instances)
            }
        }
    }

    private fun syncPets(instances: List<PetInstanceEntity>) {
        val activeIds = instances.map { it.instanceId }.toSet()
        
        // 1. Remove pets that are no longer enabled
        val toRemove = activePets.keys.filter { !activeIds.contains(it) }
        toRemove.forEach { id ->
            activePets[id]?.destroy()
            activePets.remove(id)
        }
        
        // 2. Add new pets or update existing ones
        instances.forEach { instance ->
            val existing = activePets[instance.instanceId]
            if (existing != null) {
                existing.updateEntity(instance)
            } else {
                val unit = OverlayPetUnit(context, windowManager, scope, instance, repository)
                unit.updateScale(currentScale)
                unit.updateAlpha(currentAlpha)
                unit.show()
                activePets[instance.instanceId] = unit
            }
        }
    }

    fun show() {
        // Logic handled by syncPets observing the repository
    }

    fun hide() {
        activePets.values.forEach { it.destroy() }
        activePets.clear()
    }
}

private class OverlayPetUnit(
    private val context: Context,
    private val windowManager: WindowManager,
    private val scope: CoroutineScope,
    private var currentEntity: PetInstanceEntity,
    private val repository: PetRepository
) {
    private val petView = PetView(context)
    private var petRuntime: PetRuntime? = null
    private var petController: PetController? = null
    
    private var isShowing = false
    private val handler = Handler(Looper.getMainLooper())
    
    private var moveDirectionX = 1
    
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
        x = 100 // Default random pos could be better
        y = 300
    }

    private val gestureHandler = GestureHandler(
        onTap = { petController?.onTap() },
        onDoubleTap = { petController?.onDoubleTap() },
        onLongPress = { petController?.onLongPress() },
        onDragStart = { petController?.onLongPress() },
        onDrag = { dx, dy -> moveBy(dx, dy) },
        onDragEnd = {
            petController?.onRelease()
            snapToEdge()
        }
    )

    init {
        petView.setOnTouchListener(gestureHandler)
        loadResources()
    }

    fun updateScale(scale: Float) {
        petView.setDisplayScale(scale)
        petView.setFacingDirection(moveDirectionX)
    }

    fun updateAlpha(alpha: Float) {
        layoutParams.alpha = alpha.coerceIn(0.1f, 1.0f)
        if (isShowing && petView.parent != null) {
            windowManager.updateViewLayout(petView, layoutParams)
        }
    }

    fun updateEntity(newInstance: PetInstanceEntity) {
        if (currentEntity.assetId != newInstance.assetId) {
            currentEntity = newInstance
            loadResources()
        } else {
            // Sync state logic if needed (e.g. forced mood change)
            currentEntity = newInstance
        }
    }

    private fun loadResources() {
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val manifest = AssetLoader.loadManifest(context, currentEntity.assetId)
            if (manifest != null) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    petView.loadAssets(manifest)
                    val rt = PetRuntime(manifest)
                    petRuntime = rt
                    petController = PetController(petView, rt)
                    
                    // Restore State
                    val loadedState = PetState(
                        energy = currentEntity.energy,
                        mood = currentEntity.mood,
                        affection = currentEntity.affection,
                        lastTickMs = currentEntity.lastTickTime
                    )
                    val caughtUpState = loadedState.tick(System.currentTimeMillis())
                    petRuntime?.restoreState(caughtUpState)
                }
            }
        }
    }

    fun show() {
        if (isShowing) return
        // Randomize initial position
        val screenSize = getScreenSize()
        layoutParams.x = kotlin.random.Random.nextInt(EDGE_PADDING_PX, screenSize.x - 200)
        layoutParams.y = kotlin.random.Random.nextInt(200, screenSize.y - 200)
        
        windowManager.addView(petView, layoutParams)
        isShowing = true
        startLoop()
    }

    fun destroy() {
        if (!isShowing) return
        saveState()
        stopLoop()
        if (petView.parent != null) {
            windowManager.removeView(petView)
        }
        isShowing = false
    }

    private fun saveState() {
        val s = petRuntime?.state ?: return
        val updated = currentEntity.copy(
            energy = s.energy,
            mood = s.mood,
            affection = s.affection,
            lastTickTime = s.lastTickMs
        )
        scope.launch {
            repository.updateInstance(updated)
        }
    }

    // --- Loop & Movement ---

    private var targetX = 0
    private var startX = 0
    private var moveStartTime = 0L
    private var moveDuration = 0L
    private var isMovingToTarget = false
    private var lastSaveTime = 0L

    private fun startLoop() {
        handler.post(loopRunnable)
    }

    private fun stopLoop() {
        handler.removeCallbacks(loopRunnable)
    }

    private fun easeInOutCubic(t: Float): Float {
        return if (t < 0.5f) 4 * t * t * t else 1 - Math.pow((-2 * t + 2).toDouble(), 3.0).toFloat() / 2
    }

    private fun pickNewTarget() {
        val screenSize = getScreenSize()
        val viewWidth = petView.width
        if (viewWidth == 0) return
        
        // Random target across screen
        val maxW = screenSize.x - viewWidth - EDGE_PADDING_PX
        val destX = kotlin.random.Random.nextInt(EDGE_PADDING_PX, max(EDGE_PADDING_PX + 1, maxW))
        
        targetX = destX
        startX = layoutParams.x
        moveStartTime = System.currentTimeMillis()
        
        val distance = kotlin.math.abs(targetX - startX)
        moveDuration = (distance * 4).toLong().coerceAtLeast(1500L) // Slower movement
        isMovingToTarget = true
        
        // Face direction
        val scale = kotlin.math.abs(petView.scaleY)
        if (targetX > startX) {
            moveDirectionX = 1
            petView.setFacingDirection(1)
        } else {
            moveDirectionX = -1
            petView.setFacingDirection(-1)
        }
    }

    private val loopRunnable = object : Runnable {
        override fun run() {
            if (!isShowing) return

            petController?.update()
            
            // Auto Save
            val now = System.currentTimeMillis()
            if (now - lastSaveTime > 30000) {
                lastSaveTime = now
                saveState()
            }
            
            val state = petRuntime?.state ?: run {
                handler.postDelayed(this, 33)
                return
            }
            
            val delay = if (state.behavior == PetBehavior.IDLE || state.behavior == PetBehavior.SLEEP) 83L else 33L
            
            if (state.behavior == PetBehavior.WALK || state.behavior == PetBehavior.RUN) {
                if (!isMovingToTarget) pickNewTarget()
                
                val elapsed = now - moveStartTime
                if (elapsed < moveDuration) {
                    val progress = elapsed.toFloat() / moveDuration
                    val eased = easeInOutCubic(progress)
                    val cx = startX + (targetX - startX) * eased
                    moveTo(cx.toInt(), layoutParams.y)
                } else {
                    isMovingToTarget = false
                }
            } else {
                isMovingToTarget = false
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
        // Optional: Desktop pets usually stay on floor, snapping to side might be annoying
        // but let's keep it if user drags close to edge? 
        // For now, let's DISABLE automatic snap to edge to allow free placement
    }

    private fun clampToScreen(x: Int, y: Int): Pair<Int, Int> {
        val screenSize = getScreenSize()
        val viewWidth = petView.width
        val viewHeight = petView.height
        if (viewWidth == 0) return Pair(x, y)
        
        val maxX = max(EDGE_PADDING_PX, screenSize.x - viewWidth - EDGE_PADDING_PX)
        val maxY = max(EDGE_PADDING_PX, screenSize.y - viewHeight - EDGE_PADDING_PX)
        return Pair(x.coerceIn(EDGE_PADDING_PX, maxX), y.coerceIn(EDGE_PADDING_PX, maxY))
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
        private const val EDGE_PADDING_PX = 10
    }
}
