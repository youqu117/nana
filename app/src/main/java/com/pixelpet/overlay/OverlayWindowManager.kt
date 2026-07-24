package com.pixelpet.overlay

import android.content.Context
import android.graphics.Point
import android.os.Handler
import android.os.Looper
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import android.graphics.Rect
import android.view.View
import com.pixelpet.data.PetInstanceEntity
import com.pixelpet.data.PetRepository
import com.pixelpet.input.GestureHandler
import com.pixelpet.audio.SoundManager
import com.pixelpet.content.AssetLoader
import com.pixelpet.pet.model.PetBehavior
import com.pixelpet.pet.runtime.PetController
import com.pixelpet.pet.runtime.PetRuntime
import com.pixelpet.pet.model.PetState
import com.pixelpet.pet.view.PetView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

class OverlayWindowManager(
    private val context: Context,
    private val repository: PetRepository,
    private val scope: CoroutineScope
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val activePets = mutableMapOf<Long, OverlayPetUnit>()
    
    private var currentScale = 1.0f
    private var currentAlpha = 1.0f
    private var isVerticalMoveEnabled = false
    private var isDriftEnabled = false
    private var driftSpeed = 50

    init {
        scope.launch {
            // Observe Settings
            launch {
                repository.getSetting("scale").collect { scaleStr ->
                    currentScale = scaleStr?.toFloatOrNull() ?: 1.0f
                    activePets.values.forEach { it.updateScale(currentScale) }
                }
            }
            
            launch {
                repository.getSetting("alpha").collect { alphaStr ->
                    currentAlpha = alphaStr?.toFloatOrNull() ?: 1.0f
                    activePets.values.forEach { it.updateAlpha(currentAlpha) }
                }
            }

            launch {
                repository.getSetting("vertical_move").collect { valStr ->
                    isVerticalMoveEnabled = valStr?.toBoolean() ?: false
                    activePets.values.forEach { it.updateVerticalMove(isVerticalMoveEnabled) }
                }
            }

            launch {
                repository.getSetting("drift_enabled").collect { valStr ->
                    isDriftEnabled = valStr?.toBoolean() ?: true
                    activePets.values.forEach { it.updateDrift(isDriftEnabled, driftSpeed) }
                }
            }
            
            launch {
                repository.getSetting("drift_speed").collect { valStr ->
                    driftSpeed = valStr?.toIntOrNull() ?: 50
                    activePets.values.forEach { it.updateDrift(isDriftEnabled, driftSpeed) }
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
                unit.updateVerticalMove(isVerticalMoveEnabled)
                unit.updateDrift(isDriftEnabled, driftSpeed)
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
    private var soundManager: SoundManager? = null
    
    private var isShowing = false
    private val handler = Handler(Looper.getMainLooper())
    
    private var moveDirectionX = 1
    private var isVerticalMoveEnabled = false
    private var isDriftEnabled = false
    private var driftSpeed = 50
    
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
        x = 100 
        y = 300
    }

    private val gestureHandler = GestureHandler(
        onTap = {
            petController?.onTap()
            if (currentEntity.assetId == "dragon") {
                petView.toggleAlternateForm(durationMs = 2600L)
            }
            animateTap()
        },
        onDoubleTap = { petController?.onDoubleTap() },
        onLongPress = { petController?.onLongPress() },
        onDragStart = { petController?.onLongPress() },
        onDrag = { dx, dy -> moveBy(dx, dy) },
        onDragEnd = {
            petController?.onRelease()
            snapToEdge()
        }
    )

    private var globalScale = 1.0f

    init {
        petView.setOnTouchListener(gestureHandler)
        loadResources()
    }

    fun updateScale(scale: Float) {
        globalScale = scale
        applyScale()
    }

    private fun applyScale() {
        val finalScale = globalScale * currentEntity.scale
        petView.setDisplayScale(finalScale)
        petView.setFacingDirection(moveDirectionX)
        if (isShowing && petView.parent != null) {
            windowManager.updateViewLayout(petView, layoutParams)
        }
    }

    fun updateAlpha(alpha: Float) {
        layoutParams.alpha = alpha.coerceIn(0.1f, 1.0f)
        if (isShowing && petView.parent != null) {
            windowManager.updateViewLayout(petView, layoutParams)
        }
    }
    
    fun updateVerticalMove(enabled: Boolean) {
        isVerticalMoveEnabled = enabled
    }

    fun updateDrift(enabled: Boolean, speed: Int) {
        isDriftEnabled = enabled
        driftSpeed = speed
    }

    fun updateEntity(newInstance: PetInstanceEntity) {
        val scaleChanged = currentEntity.scale != newInstance.scale
        
        if (currentEntity.assetId != newInstance.assetId) {
            currentEntity = newInstance
            loadResources()
        } else {
            currentEntity = newInstance
        }
        
        if (scaleChanged) {
            applyScale()
        }
    }

    private fun loadResources() {
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val manifest = AssetLoader.loadManifest(context, currentEntity.assetId)
            if (manifest != null) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    petView.loadAssets(manifest)

                    // 释放旧宠物的声音资源，再创建新的。
                    soundManager?.release()
                    val sm = SoundManager(context)
                    soundManager = sm
                    if (manifest.sounds.isNotEmpty()) {
                        sm.load(context, manifest.sounds)
                    }

                    val rt = PetRuntime(manifest)
                    petRuntime = rt
                    petController = PetController(petView, rt, sm)

                    // Restore State
                    val loadedState = PetState(
                        energy = currentEntity.energy,
                        mood = currentEntity.mood,
                        hunger = currentEntity.hunger,
                        affection = currentEntity.affection,
                        lastTickMs = currentEntity.lastTickTime
                    )
                    // Catch up logic: Assume resting if loading from disk
                    val caughtUpState = loadedState.tick(System.currentTimeMillis(), isResting = true)
                    petRuntime?.restoreState(caughtUpState)
                }
            }
        }
    }

    fun show() {
        if (isShowing) return
        val screenSize = getScreenSize()

        val minX = EDGE_PADDING_PX
        val maxX = max(minX + 1, screenSize.x - 200)
        val minY = 200
        val maxY = max(minY + 1, screenSize.y - 200)

        // Prefer persisted position so room/overlay location stay in sync.
        val persistedX = currentEntity.x
        val persistedY = currentEntity.y
        if (persistedX > 0f && persistedY > 0f) {
            layoutParams.x = persistedX.toInt()
            layoutParams.y = persistedY.toInt()
            val clamped = clampToScreen(layoutParams.x, layoutParams.y)
            layoutParams.x = clamped.first
            layoutParams.y = clamped.second
        } else {
            layoutParams.x = kotlin.random.Random.nextInt(minX, maxX)
            layoutParams.y = kotlin.random.Random.nextInt(minY, maxY)
        }
        
        windowManager.addView(petView, layoutParams)
        isShowing = true
        startLoop()
    }

    fun destroy() {
        if (!isShowing) return
        // 销毁路径同步落盘，避免 service scope 被取消后异步保存丢失。
        saveStateBlocking()
        stopLoop()
        soundManager?.release()
        soundManager = null
        if (petView.parent != null) {
            windowManager.removeView(petView)
        }
        isShowing = false
    }

    private fun saveState() {
        val updated = buildPersistedEntity() ?: return
        scope.launch {
            repository.updatePetState(updated)
        }
    }

    /**
     * 同步保存宠物状态。仅在 service 销毁等"协程作用域即将取消"的场景使用，
     * 保证状态不丢失。单条 Room update 通常 <50ms，不会造成 ANR。
     */
    private fun saveStateBlocking() {
        val updated = buildPersistedEntity() ?: return
        kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
            repository.updatePetState(updated)
        }
    }

    private fun buildPersistedEntity(): PetInstanceEntity? {
        val s = petRuntime?.state ?: return null
        return currentEntity.copy(
            energy = s.energy,
            mood = s.mood,
            hunger = s.hunger,
            affection = s.affection,
            lastTickTime = System.currentTimeMillis(),
            x = layoutParams.x,
            y = layoutParams.y
        )
    }

    // --- Loop & Movement ---

    private var targetX = 0f
    private var targetY = 0f
    private var startX = 0f
    private var startY = 0f
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
        val viewHeight = petView.height
        if (viewWidth == 0) return
        
        // Random target
        val maxW = screenSize.x - viewWidth - EDGE_PADDING_PX
        val destX = kotlin.random.Random.nextInt(EDGE_PADDING_PX, max(EDGE_PADDING_PX + 1, maxW))

        targetX = destX.toFloat()
        startX = layoutParams.x.toFloat()
        
        if (isVerticalMoveEnabled || isDriftEnabled) {
            val maxH = screenSize.y - viewHeight - EDGE_PADDING_PX
            val destY = kotlin.random.Random.nextInt(EDGE_PADDING_PX, max(EDGE_PADDING_PX + 1, maxH))
            targetY = destY.toFloat()
        } else {
            targetY = layoutParams.y.toFloat()
        }
        startY = layoutParams.y.toFloat()
        
        moveStartTime = System.currentTimeMillis()
        
        val dx = targetX - startX
        val dy = targetY - startY
        val distance = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        
        // Calculate duration based on speed
        // Base duration: 1500ms
        // Speed factor: 0-100. 0 = very slow (multiplier 5), 100 = fast (multiplier 0.5)
        val speedFactor = if (isDriftEnabled && driftSpeed > 0) {
            // Mapping driftSpeed (0-100) to multiplier
            // 0 -> 10.0 (very slow)
            // 50 -> 4.0 (normal)
            // 100 -> 1.0 (fast)
            10.0f - (driftSpeed / 100f * 9.0f)
        } else {
            4.0f // Default walk speed
        }
        
        moveDuration = (distance * speedFactor).toLong().coerceAtLeast(1000L)
        isMovingToTarget = true
        
        // Rotation & Facing
        if (isVerticalMoveEnabled && kotlin.math.abs(dy) > kotlin.math.abs(dx)) {
            // Vertical dominant
            petView.rotation = if (dy > 0) 90f else -90f
            // Maintain facing for consistency or could flip based on subtle x movement
        } else {
            petView.rotation = 0f
            if (dx > 0) {
                moveDirectionX = 1
                petView.setFacingDirection(1)
            } else if (dx < 0) {
                moveDirectionX = -1
                petView.setFacingDirection(-1)
            }
        }
    }

    private val loopRunnable = object : Runnable {
        override fun run() {
            if (!isShowing) return

            petController?.update()
            
            val now = System.currentTimeMillis()
            if (now - lastSaveTime > 30000) {
                lastSaveTime = now
                saveState()
            }
            
            val state = petRuntime?.state ?: run {
                handler.postDelayed(this, 33)
                return
            }
            
            val delay = if (state.behavior == PetBehavior.IDLE || state.behavior == PetBehavior.SLEEP) 60L else 24L
            
            // Check if we should move:
            // 1. If explicit WALK/RUN behavior
            // 2. OR if drift is enabled and not interacting (could be IDLE)
            // Note: Don't drift if sleeping
            val shouldMove = (state.behavior == PetBehavior.WALK || state.behavior == PetBehavior.RUN) ||
                             (isDriftEnabled && state.behavior != PetBehavior.SLEEP && state.behavior != PetBehavior.HELD)
            
            if (shouldMove) {
                if (!isMovingToTarget) pickNewTarget()
                
                val elapsed = now - moveStartTime
                if (elapsed < moveDuration) {
                    val progress = elapsed.toFloat() / moveDuration
                    val eased = easeInOutCubic(progress)
                    val cx = startX + (targetX - startX) * eased
                    val cy = startY + (targetY - startY) * eased
                    moveTo(cx.roundToInt(), cy.roundToInt())
                } else {
                    isMovingToTarget = false
                    petView.rotation = 0f // Reset rotation when stopped
                }
            } else {
                isMovingToTarget = false
                petView.rotation = 0f
            }

            handler.postDelayed(this, delay)
        }
    }

    private fun animateTap() {
        petView.animate().cancel()
        petView.animate()
            .scaleX(1.08f)
            .scaleY(1.08f)
            .setDuration(90)
            .withEndAction {
                petView.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
            }
            .start()
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
        if (!isShowing || petView.parent == null) return
        val screenSize = getScreenSize()
        val viewWidth = petView.width
        if (viewWidth <= 0) {
            saveState()
            return
        }

        val midX = screenSize.x / 2
        val targetX = if (layoutParams.x + viewWidth / 2 < midX) {
            EDGE_PADDING_PX
        } else {
            max(EDGE_PADDING_PX, screenSize.x - viewWidth - EDGE_PADDING_PX)
        }
        val clamped = clampToScreen(targetX, layoutParams.y)
        layoutParams.x = clamped.first
        layoutParams.y = clamped.second
        windowManager.updateViewLayout(petView, layoutParams)
        saveState()
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

