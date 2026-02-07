package com.pixelpet.ui.petroom

import android.os.Handler
import android.os.Looper
import android.os.Bundle
import android.net.Uri
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.widget.ImageView
import android.widget.ScrollView
import android.view.Gravity
import android.view.MotionEvent
import android.graphics.BitmapFactory
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import android.view.ViewGroup
import com.pixelpet.ui.main.MainActivity
import kotlin.math.roundToInt
import kotlin.math.max
import kotlin.math.min
import android.widget.GridLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.pixelpet.content.AssetLoader
import com.pixelpet.data.AppDatabase
import com.pixelpet.data.PetRepository
import com.pixelpet.data.PetInstanceEntity
import com.pixelpet.pet.model.PetBehavior
import com.pixelpet.pet.interaction.PetEmote
import com.pixelpet.pet.interaction.PetEmoteEvent
import com.pixelpet.pet.level.LevelSystem
import com.pixelpet.pet.runtime.PetRuntime
import com.pixelpet.pet.view.PetView
import com.pixelpet.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.random.Random

class PetDetailActivity : AppCompatActivity() {
    private data class RoomDecorItem(
        val id: String,
        val path: String,
        var nx: Float,
        var ny: Float,
        var scale: Float = 1.0f,
        var alpha: Float = 1.0f
    )
    private data class DecorEntry(
        val label: String,
        val resId: Int? = null,
        val path: String? = null
    )

    private lateinit var repository: PetRepository
    private var currentInstanceId: Long = -1L
    private var petRuntime: PetRuntime? = null
    private var isInteractingWithSeek = false
    private var currentInstance: PetInstanceEntity? = null
    private var currentAssetId: String? = null
    private var lastEmoteSeq: Long = 0L
    private var lastMusicSource: String = "violin"
    private lateinit var petViewRef: PetView
    private lateinit var roomContainer: FrameLayout
    private lateinit var decorLayer: FrameLayout
    private lateinit var consoleContent: View
    private lateinit var consoleAdvanced: View
    private lateinit var statusContent: View
    private lateinit var btnToggleConsoleRef: ImageView
    private lateinit var btnToggleStatusRef: ImageView
    private lateinit var btnConsoleMoreRef: Button
    private val decorItems = mutableListOf<RoomDecorItem>()
    private var decorBoundInstanceId: Long = -1L
    private var isConsoleCollapsed = false
    private var isStatusCollapsed = false
    private var isConsoleAdvancedVisible = false
    private var birthdayDialog: Dialog? = null
    private var lastStateSignature = 0
    private var lastStateUpdateMs = 0L
    private var lastOverlaySyncMs = 0L
    private var lastOverlayX = Float.NaN
    private var lastOverlayY = Float.NaN
    private var lastPersistMs = 0L
    private var lastFxAnchorX = 0f
    private var lastFxAnchorY = 0f
    private var lastInstanceEnabled: Boolean? = null
    private var suppressRoomSyncUntilMs = 0L
    private val autoHideRunnable = Runnable {
        setConsoleVisible(false)
        setStatusVisible(false)
    }

    private val decorImagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            addRoomDecor(uri)
        }
    }

    // Animation & Movement
    private val handler = Handler(Looper.getMainLooper())
    private var globalScale = 1.0f
    private var isDriftEnabled = true // Default to true for the room so it feels alive
    private var driftSpeed = 50
    
    private var targetX = 0f
    private var targetY = 0f
    private var startX = 0f
    private var startY = 0f
    private var moveStartTime = 0L
    private var moveDuration = 0L
    private var isMovingToTarget = false

    private val loopRunnable = object : Runnable {
        override fun run() {
            val delay = updateLoop()
            handler.postDelayed(this, delay)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pet_detail)

        currentInstanceId = intent.getLongExtra(EXTRA_PET_INSTANCE_ID, -1L)
        if (currentInstanceId < 0) {
            finish()
            return
        }

        try {
            val db = AppDatabase.getDatabase(this)
            repository = PetRepository(db.petDao(), db.settingsDao())
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Database Error: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupToolbar()
        setupViews()
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        startAnimationLoop()
        pokeUi()
    }

    override fun onPause() {
        super.onPause()
        stopAnimationLoop()
    }

    private fun startAnimationLoop() {
        stopAnimationLoop()
        handler.post(loopRunnable)
    }

    private fun stopAnimationLoop() {
        handler.removeCallbacks(loopRunnable)
    }

    private fun updateLoop(): Long {
        val now = System.currentTimeMillis()
        petRuntime?.tick(now)

        val petView = petViewRef
        val state = petRuntime?.state ?: return 70L

        val signature = ((state.behavior.ordinal + 1) * 1000000) +
            (state.energy * 10000) + (state.mood * 100) + state.hunger
        if (signature != lastStateSignature || now - lastStateUpdateMs > 300L) {
            petView.updateState(state)
            lastStateSignature = signature
            lastStateUpdateMs = now
        }
        petRuntime?.pollEmote(lastEmoteSeq)?.let { event ->
            lastEmoteSeq = event.seq
            handleEmote(event)
        }

        if (currentInstance?.isEnabled == true) {
            // Keep room avatar aligned with current overlay position when summoned, but throttle.
            val instance = currentInstance
            if (instance != null && now - lastOverlaySyncMs > 300L && now >= suppressRoomSyncUntilMs) {
                val ix = instance.x.toFloat()
                val iy = instance.y.toFloat()
                val dx = if (lastOverlayX.isNaN()) Float.MAX_VALUE else kotlin.math.abs(ix - lastOverlayX)
                val dy = if (lastOverlayY.isNaN()) Float.MAX_VALUE else kotlin.math.abs(iy - lastOverlayY)
                if (dx > 1f || dy > 1f) {
                    syncRoomPositionFromOverlay(instance)
                    lastOverlayX = ix
                    lastOverlayY = iy
                }
                lastOverlaySyncMs = now
            }
            updateFxAnchor()
            persistStateIfNeeded(now)
            return 60L
        }

        // Movement Logic
        val shouldMove = (state.behavior == PetBehavior.WALK || state.behavior == PetBehavior.RUN) ||
                         (isDriftEnabled && state.behavior != PetBehavior.SLEEP && state.behavior != PetBehavior.HELD)

        if (shouldMove) {
            if (!isMovingToTarget) pickNewTarget(petView)
            
            val elapsed = now - moveStartTime
            if (elapsed < moveDuration) {
                val progress = elapsed.toFloat() / moveDuration
                val eased = easeInOutCubic(progress)
                val cx = startX + (targetX - startX) * eased
                val cy = startY + (targetY - startY) * eased
                
                petView.translationX = cx
                petView.translationY = cy
            } else {
                isMovingToTarget = false
                petView.rotation = 0f
            }
        } else {
            isMovingToTarget = false
            petView.rotation = 0f
        }
        persistStateIfNeeded(now)
        updateFxAnchor()
        return if (shouldMove) 24L else 60L
    }

    private fun easeInOutCubic(t: Float): Float {
        return if (t < 0.5f) 4 * t * t * t else 1 - Math.pow((-2 * t + 2).toDouble(), 3.0).toFloat() / 2
    }

    private fun pickNewTarget(petView: PetView) {
        val container = findViewById<FrameLayout>(R.id.layoutRoomContainer) ?: return
        val containerW = container.width
        val containerH = container.height
        val viewW = petView.width
        val viewH = petView.height
        
        if (containerW == 0 || viewW == 0) return

        // If pet view is larger than the room container (common on some phones/assets),
        // movement range can become negative and crash Random range generation.
        // Clamp the available translation range to non-negative values.
        val maxTransX = ((containerW - viewW) / 2f - 20f).coerceAtLeast(0f)
        val maxTransY = ((containerH - viewH) / 2f - 20f).coerceAtLeast(0f)

        // Target
        targetX = if (maxTransX > 0f) {
            Random.nextDouble((-maxTransX).toDouble(), maxTransX.toDouble()).toFloat()
        } else {
            0f
        }
        startX = petView.translationX

        if (isDriftEnabled) {
             targetY = if (maxTransY > 0f) {
                 Random.nextDouble((-maxTransY).toDouble(), maxTransY.toDouble()).toFloat()
             } else {
                 0f
             }
        } else {
             targetY = maxTransY
        }
        startY = petView.translationY

        moveStartTime = System.currentTimeMillis()
        
        val dx = targetX - startX
        val dy = targetY - startY
        val distance = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

        val speedFactor = if (isDriftEnabled && driftSpeed > 0) {
             10.0f - (driftSpeed / 100f * 9.0f)
        } else {
             4.0f 
        }
        
        moveDuration = (distance * speedFactor).toLong().coerceAtLeast(1000L)
        isMovingToTarget = true
        
        // Facing
        if (dx > 0) {
            petView.setFacingDirection(1)
        } else if (dx < 0) {
            petView.setFacingDirection(-1)
        }
    }

    private fun setupObservers() {
        observePet()
        
        lifecycleScope.launch {
            repository.getSetting("scale").collectLatest {
                globalScale = it?.toFloatOrNull() ?: 1.0f
                updatePetScale()
            }
        }
        lifecycleScope.launch {
            repository.getSetting("drift_enabled").collectLatest {
                isDriftEnabled = it?.toBoolean() ?: true
            }
        }
        lifecycleScope.launch {
            repository.getSetting("drift_speed").collectLatest {
                driftSpeed = it?.toIntOrNull() ?: 50
            }
        }
    }

    private fun updatePetScale() {
        val petView = findViewById<PetView>(R.id.petView) ?: return
        val seekBar = findViewById<SeekBar>(R.id.seekInstanceScale)
        val progress = seekBar.progress
        val instanceScale = 0.5f + (progress / 100f)
        val finalScale = instanceScale * globalScale
        petView.setDisplayScale(finalScale)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupViews() {
        petViewRef = findViewById(R.id.petView)
        roomContainer = findViewById(R.id.layoutRoomContainer)
        decorLayer = findViewById(R.id.layoutDecorLayer)
        consoleContent = findViewById(R.id.layoutConsoleContent)
        consoleAdvanced = findViewById(R.id.layoutConsoleAdvanced)
        statusContent = findViewById(R.id.layoutStatusContent)
        val petView = petViewRef
        val btnToggle = findViewById<Button>(R.id.btnToggleSummon)
        val btnFeed = findViewById<Button>(R.id.btnRoomFeed)
        val btnRename = findViewById<Button>(R.id.btnRoomRename)
        val btnDelete = findViewById<Button>(R.id.btnRoomDelete)
        val btnDecorAdd = findViewById<Button>(R.id.btnDecorAdd)
        val btnDecorClear = findViewById<Button>(R.id.btnDecorClear)
        btnConsoleMoreRef = findViewById(R.id.btnConsoleMore)
        btnToggleConsoleRef = findViewById(R.id.btnToggleConsole)
        btnToggleStatusRef = findViewById(R.id.btnToggleStatus)
        val btnViolin = findViewById<Button>(R.id.btnViolin)
        val btnPiano = findViewById<Button>(R.id.btnPiano)
        val btnSleep = findViewById<Button>(R.id.btnSleep)
        val btnGreet = findViewById<Button>(R.id.btnGreet)
        val btnSing = findViewById<Button>(R.id.btnSing)

        setButtonTopIcon(btnFeed, R.drawable.fx_food_color, sizeDp = 18)
        setButtonTopIcon(btnRename, R.drawable.ic_rename, sizeDp = 16, tintColorRes = R.color.ui_text_mute)
        setButtonTopIcon(btnDelete, R.drawable.ic_delete, sizeDp = 16, tintColorRes = R.color.ui_rose)
        setButtonTopIcon(btnViolin, R.drawable.fx_violin_color, sizeDp = 18)
        setButtonTopIcon(btnPiano, R.drawable.fx_piano_color, sizeDp = 18)
        setButtonTopIcon(btnSleep, R.drawable.fx_sleep_color, sizeDp = 18)
        setButtonTopIcon(btnGreet, R.drawable.fx_heart_color, sizeDp = 18)
        setButtonTopIcon(btnSing, R.drawable.fx_music_note_color, sizeDp = 18)
        setButtonTopIcon(btnDecorAdd, R.drawable.fx_star_color, sizeDp = 18)
        setButtonTopIcon(btnDecorClear, R.drawable.ic_delete, sizeDp = 16, tintColorRes = R.color.ui_rose)
        setButtonTopIcon(btnToggle, R.drawable.ic_pixel_add, sizeDp = 16, tintColorRes = R.color.ui_text_title)
        
        // Scale Control
        val seekScale = findViewById<SeekBar>(R.id.seekInstanceScale)
        val textScaleValue = findViewById<TextView>(R.id.textInstanceScaleValue)
        
        seekScale.max = 150 // 0.5x to 2.0x
        seekScale.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val scale = 0.5f + (progress / 100f)
                    textScaleValue.text = String.format("%.1fx", scale)
                    updatePetScale()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) { isInteractingWithSeek = true }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isInteractingWithSeek = false
                seekBar?.let {
                    val scale = 0.5f + (it.progress / 100f)
                    saveScale(scale)
                }
            }
        })

        // Pet Interaction
        petView.setOnClickListener {
            petRuntime?.handleTap(System.currentTimeMillis())
            petView.animate().cancel()
            petView.animate().scaleX(1.08f).scaleY(1.08f).setDuration(90).withEndAction {
                petView.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
            }.start()
            if (Random.nextFloat() < 0.3f) { // 30% chance
                spawnHeart()
            }
            pushStateNow()
            pokeUi()
        }

        lifecycleScope.launch {
            val instance = repository.getInstanceById(currentInstanceId) ?: return@launch
            updateUi(instance)
            loadPetView(petView, instance.assetId)
        }

        btnToggle.setOnClickListener { toggleSummon() }
        btnFeed.setOnClickListener { feed() }
        btnRename.setOnClickListener { rename() }
        btnDelete.setOnClickListener { delete() }
        btnDecorAdd.setOnClickListener { showDecorAddMenu() }
        btnDecorClear.setOnClickListener { clearRoomDecor() }
        btnToggleConsoleRef.setOnClickListener { openSettings() }
        btnConsoleMoreRef.setOnClickListener { toggleConsoleAdvanced() }
        btnViolin.setOnClickListener { playMusic("violin") }
        btnPiano.setOnClickListener { playMusic("piano") }
        btnSleep.setOnClickListener { toggleSleep() }
        btnGreet.setOnClickListener { greet() }
        btnSing.setOnClickListener { sing() }

        setConsoleVisible(true)
        setStatusVisible(true)
        setConsoleAdvancedVisible(false)
        pokeUi()
    }

    private fun observePet() {
        lifecycleScope.launch {
            repository.allInstances.collectLatest { instances ->
                val instance = instances.find { it.instanceId == currentInstanceId } ?: return@collectLatest
                withContext(Dispatchers.Main) { updateUi(instance) }
            }
        }
    }

    private fun updateUi(instance: com.pixelpet.data.PetInstanceEntity) {
        currentInstance = instance
        val levelInfo = LevelSystem.fromPet(instance)
        supportActionBar?.title = "LV.${levelInfo.level} ${instance.name} 的小屋"
        findViewById<TextView>(R.id.textPetName).text = instance.name
        findViewById<ProgressBar>(R.id.progressEnergy).progress = instance.energy.coerceIn(0, 100)
        findViewById<ProgressBar>(R.id.progressMood).progress = instance.mood.coerceIn(0, 100)
        findViewById<ProgressBar>(R.id.progressHunger).progress = instance.hunger.coerceIn(0, 100)
        findViewById<TextView>(R.id.textLevelMini).text = "LV.${levelInfo.level} ${levelInfo.title}"
        findViewById<ProgressBar>(R.id.progressLevelXp).progress = levelInfo.progressPercent
        findViewById<TextView>(R.id.textXpMini).text = "经验 ${levelInfo.progressPercent}%"

        if (decorBoundInstanceId != instance.instanceId) {
            decorBoundInstanceId = instance.instanceId
            loadRoomDecorForInstance(instance.instanceId)
        }

        if (!isInteractingWithSeek) {
            val progress = ((instance.scale - 0.5f) * 100).toInt().coerceIn(0, 150)
            findViewById<SeekBar>(R.id.seekInstanceScale).progress = progress
            findViewById<TextView>(R.id.textInstanceScaleValue).text = String.format("%.1fx", instance.scale)
            updatePetScale()
        }

        val btnToggle = findViewById<Button>(R.id.btnToggleSummon)
        val petView = petViewRef

        if (instance.isEnabled) {
            btnToggle.text = getString(R.string.action_go_home)
            btnToggle.setBackgroundResource(R.drawable.bg_btn_primary)
            btnToggle.setTextColor(getColor(android.R.color.white))
            petView.visibility = View.VISIBLE
            syncRoomPositionFromOverlay(instance)
        } else {
            btnToggle.text = getString(R.string.action_summon)
            btnToggle.setBackgroundResource(R.drawable.bg_btn_dim)
            btnToggle.setTextColor(getColor(R.color.ui_text_main))
            petView.visibility = View.VISIBLE
            if (lastInstanceEnabled == true) {
                petView.translationX = 0f
                petView.translationY = 0f
            }
        }
        lastInstanceEnabled = instance.isEnabled
        pokeUi()
    }

    private fun loadPetView(petView: PetView, assetId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val manifest = AssetLoader.loadManifest(this@PetDetailActivity, assetId) ?: return@launch
            withContext(Dispatchers.Main) {
                petView.loadAssets(manifest)
                petRuntime = PetRuntime(manifest)
                currentAssetId = manifest.id
                petRuntime?.state?.let { petView.updateState(it) }

                // Easter Egg for Qi Qi
                try {
                    val crown = findViewById<ImageView>(R.id.imgEasterEggCrown)
                    if (crown != null) {
                        if (assetId == "qi_qi") {
                            try {
                                crown.setImageResource(R.drawable.ic_easter_egg_crown)
                                crown.visibility = View.VISIBLE
                                crown.setOnClickListener {
                                    triggerBirthdayEasterEgg(crown)
                                }
                            } catch (e: Exception) {
                                // Defensive fallback: avoid crashing the room if crown drawable fails.
                                crown.visibility = View.GONE
                                crown.setOnClickListener(null)
                            }
                        } else {
                            crown.visibility = View.GONE
                            crown.setOnClickListener(null)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun triggerBirthdayEasterEgg(view: View) {
        // Animation
        view.animate()
            .rotationBy(360f)
            .scaleX(1.5f)
            .scaleY(1.5f)
            .setInterpolator(android.view.animation.OvershootInterpolator())
            .setDuration(800)
            .withEndAction {
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setInterpolator(android.view.animation.BounceInterpolator())
                    .setDuration(500)
                    .start()

                showBirthdayBlessing()

                val burst = listOf(
                    R.drawable.fx_balloon_color,
                    R.drawable.fx_party_color,
                    R.drawable.fx_confetti_color,
                    R.drawable.fx_gift_color,
                    R.drawable.fx_cake_color,
                    R.drawable.fx_star_color,
                    R.drawable.fx_heart_color,
                    R.drawable.fx_spark_color
                )
                repeat(14) { i ->
                    handler.postDelayed({
                        spawnIconListBurst(burst, repeatCount = 1, sizeDp = if (i % 3 == 0) 34 else 26)
                    }, (i * 110L))
                }
                repeat(10) {
                    handler.postDelayed({ spawnHeart() }, (it * 130L))
                }
                spawnCelebrationAroundCrown(view)
            }
            .start()
    }

    private fun showBirthdayBlessing() {
        birthdayDialog?.dismiss()
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_birthday_blessing)
        dialog.setCancelable(true)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        val overlay = dialog.findViewById<View>(R.id.birthdayOverlay)
        val close = dialog.findViewById<Button>(R.id.btnBirthdayClose)
        overlay.setOnClickListener { dialog.dismiss() }
        close.setOnClickListener { dialog.dismiss() }

        birthdayDialog = dialog
        dialog.show()
    }

    private fun toggleSummon() {
        lifecycleScope.launch {
            val instance = repository.getInstanceById(currentInstanceId) ?: return@launch
            repository.toggleInstance(instance)
        }
    }

    private fun feed() {
        suppressRoomSyncUntilMs = System.currentTimeMillis() + 900L
        lifecycleScope.launch {
            val instance = repository.getInstanceById(currentInstanceId) ?: return@launch
            
            if (instance.hunger >= 100 && instance.mood >= 100) {
                 Toast.makeText(this@PetDetailActivity, "${instance.name} 已经吃饱喝足了!", Toast.LENGTH_SHORT).show()
                 return@launch
            }
            
            val newHunger = (instance.hunger + 10).coerceAtMost(100)
            val newMood = (instance.mood + 5).coerceAtMost(100)
            repository.updateInstance(instance.copy(hunger = newHunger, mood = newMood))
            
            // Interaction effect
            petRuntime?.handleFeed()
            spawnHeart()
            spawnIconListBurst(
                drawableSet = listOf(R.drawable.fx_food_color, R.drawable.fx_spark_color, R.drawable.fx_heart_color),
                repeatCount = 4,
                sizeDp = 24
            )
            pushStateNow()
            pokeUi()
        }
    }

    private fun rename() {
        suppressRoomSyncUntilMs = System.currentTimeMillis() + 900L
        lifecycleScope.launch {
            val instance = repository.getInstanceById(currentInstanceId) ?: return@launch
            val editText = EditText(this@PetDetailActivity).apply {
                setText(instance.name)
                setSelection(instance.name.length)
                hint = getString(R.string.hint_enter_name)
            }
            val container = LinearLayout(this@PetDetailActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(50, 40, 50, 40)
                addView(editText)
            }
            AlertDialog.Builder(this@PetDetailActivity)
                .setTitle(getString(R.string.action_rename))
                .setView(container)
                .setPositiveButton(getString(R.string.action_save)) { _, _ ->
                    val newName = editText.text.toString().trim()
                    if (newName.isNotEmpty()) {
                        lifecycleScope.launch {
                            repository.updateInstance(instance.copy(name = newName))
                        }
                    }
                }
                .setNegativeButton(getString(R.string.action_cancel), null)
                .show()
        }
    }
    
    private fun delete() {
        suppressRoomSyncUntilMs = System.currentTimeMillis() + 900L
        lifecycleScope.launch {
             val instance = repository.getInstanceById(currentInstanceId) ?: return@launch
             AlertDialog.Builder(this@PetDetailActivity)
                .setTitle("Delete Pet?")
                .setMessage("Are you sure you want to delete ${instance.name}?")
                .setPositiveButton("Delete") { _, _ ->
                    lifecycleScope.launch {
                        repository.deleteInstance(instance)
                        deleteDecorFiles()
                        repository.setSetting(roomDecorKey(instance.instanceId), "[]")
                        finish()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun saveScale(scale: Float) {
        lifecycleScope.launch {
            val instance = repository.getInstanceById(currentInstanceId) ?: return@launch
            repository.updateInstance(instance.copy(scale = scale))
        }
    }

    private fun spawnHeart() {
        val container = roomContainer
        val anchor = getFxAnchor()
        val sizePx = ((26f * getFxScaleFactor()).coerceIn(18f, 54f) * resources.displayMetrics.density).roundToInt()

        val heart = android.widget.ImageView(this).apply {
            setImageResource(R.drawable.fx_heart_color)
            layoutParams = FrameLayout.LayoutParams(sizePx, sizePx).apply {
                gravity = android.view.Gravity.TOP or android.view.Gravity.START
                leftMargin = clampInRange(anchor.first.roundToInt() - sizePx / 2, 0, max(0, container.width - sizePx))
                topMargin = clampInRange(anchor.second.roundToInt() - sizePx / 2, 0, max(0, container.height - sizePx))
            }
        }
        
        container.addView(heart)
        
        heart.animate()
            .translationY(-100f)
            .alpha(0f)
            .setDuration(1000)
            .withEndAction {
                container.removeView(heart)
            }
            .start()
    }

    private fun playMusic(source: String) {
        suppressRoomSyncUntilMs = System.currentTimeMillis() + 900L
        lastMusicSource = source
        petRuntime?.handlePlayMusic(System.currentTimeMillis())
        // Visual FX is driven by PetRuntime emote events to avoid duplicate effects.
        pushStateNow()
        pokeUi()
    }

    private fun toggleSleep() {
        suppressRoomSyncUntilMs = System.currentTimeMillis() + 900L
        petRuntime?.handleSleepToggle()
        val behavior = petRuntime?.state?.behavior
        if (behavior == PetBehavior.SLEEP) {
            Toast.makeText(this, "已进入睡觉模式", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "宠物醒来了", Toast.LENGTH_SHORT).show()
        }
        pushStateNow()
        pokeUi()
    }

    private fun greet() {
        suppressRoomSyncUntilMs = System.currentTimeMillis() + 900L
        petRuntime?.handleGreet(System.currentTimeMillis())
        pushStateNow()
        pokeUi()
    }

    private fun sing() {
        suppressRoomSyncUntilMs = System.currentTimeMillis() + 900L
        petRuntime?.handleSing(System.currentTimeMillis())
        pushStateNow()
        pokeUi()
    }

    private fun spawnIconBurst(drawableRes: Int, repeatCount: Int, sizeDp: Int = 28) {
        val container = roomContainer
        val anchor = getFxAnchor()
        val scaledDp = (sizeDp * getFxScaleFactor()).coerceIn(16f, 56f)
        val sizePx = (scaledDp * resources.displayMetrics.density).roundToInt()

        repeat(repeatCount) { index ->
            handler.postDelayed({
                val iv = ImageView(this).apply {
                    setImageResource(drawableRes)
                    alpha = 0.95f
                }
                val params = FrameLayout.LayoutParams(sizePx, sizePx).apply {
                    gravity = Gravity.TOP or Gravity.START
                    leftMargin = clampInRange((anchor.first + Random.nextInt(-42, 42)).roundToInt(), 0, max(0, container.width - sizePx))
                    topMargin = clampInRange((anchor.second + Random.nextInt(-28, 28)).roundToInt(), 0, max(0, container.height - sizePx))
                }
                container.addView(iv, params)

                iv.animate()
                    .translationY((-90 - Random.nextInt(40)).toFloat())
                    .alpha(0f)
                    .setDuration(900)
                    .withEndAction { container.removeView(iv) }
                    .start()
            }, (index * 120L))
        }
    }

    private fun spawnNoteSequence(noteRes: List<Int>, instrumentRes: Int?, steps: Int) {
        val sequence = mutableListOf<Int>()
        for (i in 0 until steps) {
            val pick = if (instrumentRes != null && i % 3 == 2) instrumentRes else noteRes[i % noteRes.size]
            sequence.add(pick)
        }
        sequence.forEachIndexed { index, res ->
            handler.postDelayed({ spawnIconBurst(res, repeatCount = 1, sizeDp = 26) }, (index * 140L))
        }
    }

    private fun spawnFireBurst() {
        spawnIconListBurst(
            drawableSet = listOf(R.drawable.fx_fire_color, R.drawable.fx_spark_color, R.drawable.fx_exclaim_color),
            repeatCount = 7,
            sizeDp = 30
        )
    }

    private fun handleEmote(event: PetEmoteEvent) {
        val state = petRuntime?.state
        val moodBoost = ((state?.mood ?: 50) / 35)
        val levelBoost = (currentInstance?.let { LevelSystem.fromPet(it).level } ?: 1) / 4
        val baseCount = 3 + moodBoost + levelBoost
        val count = baseCount.coerceIn(3, 9)

        when (event.type) {
            PetEmote.MUSIC -> {
                val instrument = if (currentAssetId == "qi_qi") {
                    R.drawable.fx_flute_color
                } else if (lastMusicSource == "piano") {
                    R.drawable.fx_piano_color
                } else {
                    R.drawable.fx_violin_color
                }
                spawnNoteSequence(
                    noteRes = listOf(R.drawable.fx_music_note_color, R.drawable.fx_music_notes_color, R.drawable.fx_spark_color),
                    instrumentRes = instrument,
                    steps = count + 2
                )
            }
            PetEmote.SING -> {
                val extra = if (currentAssetId == "qi_qi") R.drawable.fx_flute_color else null
                spawnNoteSequence(
                    noteRes = listOf(R.drawable.fx_music_notes_color, R.drawable.fx_music_note_color, R.drawable.fx_spark_color),
                    instrumentRes = extra,
                    steps = count + 1
                )
            }
            PetEmote.GREET -> {
                spawnIconListBurst(
                    drawableSet = listOf(R.drawable.fx_wave_color, R.drawable.fx_smile_color, R.drawable.fx_spark_color),
                    repeatCount = count,
                    sizeDp = 25
                )
            }
            PetEmote.HAPPY -> {
                spawnHeart()
                spawnIconListBurst(
                    drawableSet = listOf(R.drawable.fx_heart_color, R.drawable.fx_spark_color, R.drawable.fx_smile_color),
                    repeatCount = count,
                    sizeDp = 24
                )
            }
            PetEmote.SLEEP -> {
                spawnIconListBurst(
                    drawableSet = listOf(R.drawable.fx_sleep_color, R.drawable.fx_spark_color),
                    repeatCount = count,
                    sizeDp = 26
                )
            }
            PetEmote.WAKE -> {
                spawnIconListBurst(
                    drawableSet = listOf(R.drawable.fx_sun_color, R.drawable.fx_exclaim_color, R.drawable.fx_spark_color),
                    repeatCount = 3,
                    sizeDp = 24
                )
            }
            PetEmote.FIRE -> {
                spawnFireBurst()
            }
            PetEmote.THINK -> {
                spawnIconListBurst(
                    drawableSet = listOf(R.drawable.fx_think_color, R.drawable.fx_spark_color),
                    repeatCount = 3,
                    sizeDp = 24
                )
            }
        }
    }

    private fun spawnIconListBurst(drawableSet: List<Int>, repeatCount: Int, sizeDp: Int = 28) {
        if (drawableSet.isEmpty()) return
        repeat(repeatCount) {
            spawnIconBurst(drawableSet.random(), repeatCount = 1, sizeDp = sizeDp)
        }
    }

    private fun setButtonTopIcon(button: Button, drawableRes: Int, sizeDp: Int = 20, tintColorRes: Int? = null) {
        val d = getDrawable(drawableRes) ?: return
        val px = (sizeDp * resources.displayMetrics.density).roundToInt()
        d.setBounds(0, 0, px, px)
        tintColorRes?.let { d.setTint(getColor(it)) }
        button.setCompoundDrawables(null, d, null, null)
        button.compoundDrawablePadding = (2 * resources.displayMetrics.density).roundToInt()
        button.gravity = Gravity.CENTER
        button.textAlignment = View.TEXT_ALIGNMENT_CENTER
    }

    private fun getFxScaleFactor(): Float {
        val seekBar = findViewById<SeekBar>(R.id.seekInstanceScale)
        val instanceScale = 0.5f + (seekBar.progress / 100f)
        return (instanceScale * globalScale).coerceIn(0.6f, 2.2f)
    }

    private fun getFxAnchor(): Pair<Float, Float> {
        val petView = petViewRef
        if (petView.width <= 0 || petView.height <= 0) {
            val room = roomContainer
            if (lastFxAnchorX > 0f || lastFxAnchorY > 0f) {
                return Pair(lastFxAnchorX, lastFxAnchorY)
            }
            return Pair(room.width * 0.5f, room.height * 0.5f)
        }
        return Pair(lastFxAnchorX, lastFxAnchorY)
    }

    private fun updateFxAnchor() {
        val petView = petViewRef
        if (petView.width <= 0 || petView.height <= 0) return
        lastFxAnchorX = petView.x + petView.width * 0.5f
        lastFxAnchorY = petView.y + petView.height * 0.35f
    }

    private fun clampInRange(value: Int, minValue: Int, maxValue: Int): Int {
        return min(max(value, minValue), maxValue)
    }

    private fun syncRoomPositionFromOverlay(instance: PetInstanceEntity) {
        roomContainer.post {
            val display = resources.displayMetrics
            val screenW = display.widthPixels.toFloat().coerceAtLeast(1f)
            val screenH = display.heightPixels.toFloat().coerceAtLeast(1f)
            val roomW = roomContainer.width.toFloat().coerceAtLeast(1f)
            val roomH = roomContainer.height.toFloat().coerceAtLeast(1f)
            val normalizedX = (instance.x / screenW).coerceIn(0f, 1f)
            val normalizedY = (instance.y / screenH).coerceIn(0f, 1f)
            val targetX = (normalizedX - 0.5f) * roomW * 0.7f
            val targetY = (normalizedY - 0.5f) * roomH * 0.55f
            petViewRef.translationX = targetX
            petViewRef.translationY = targetY
        }
    }

    private fun toggleConsolePanel() {
        setConsoleVisible(isConsoleCollapsed)
        pokeUi()
    }

    private fun toggleStatusPanel() {
        setStatusVisible(isStatusCollapsed)
        pokeUi()
    }

    private fun setConsoleVisible(show: Boolean) {
        isConsoleCollapsed = !show
        if (!show) {
            if (consoleContent.visibility != View.GONE) {
                consoleContent.animate().alpha(0f).setDuration(120).withEndAction {
                    consoleContent.visibility = View.GONE
                }.start()
            }
            setConsoleAdvancedVisible(false)
        } else {
            if (consoleContent.visibility != View.VISIBLE || consoleContent.alpha < 1f) {
                consoleContent.visibility = View.VISIBLE
                consoleContent.alpha = 0f
                consoleContent.animate().alpha(1f).setDuration(140).start()
            }
        }
    }

    private fun openSettings() {
        val intent = android.content.Intent(this, MainActivity::class.java)
        intent.putExtra(MainActivity.EXTRA_OPEN_TAB, MainActivity.TAB_SETTINGS)
        startActivity(intent)
    }

    private fun toggleConsoleAdvanced() {
        setConsoleAdvancedVisible(!isConsoleAdvancedVisible)
        pokeUi()
    }

    private fun setConsoleAdvancedVisible(show: Boolean) {
        isConsoleAdvancedVisible = show
        if (show) {
            consoleAdvanced.visibility = View.VISIBLE
            btnConsoleMoreRef.text = "收起"
        } else {
            consoleAdvanced.visibility = View.GONE
            btnConsoleMoreRef.text = "更多"
        }
    }

    private fun setStatusVisible(show: Boolean) {
        isStatusCollapsed = !show
        if (!show) {
            if (statusContent.visibility != View.GONE) {
                statusContent.animate().alpha(0f).setDuration(120).withEndAction {
                    statusContent.visibility = View.GONE
                }.start()
            }
        } else {
            if (statusContent.visibility != View.VISIBLE || statusContent.alpha < 1f) {
                statusContent.visibility = View.VISIBLE
                statusContent.alpha = 0f
                statusContent.animate().alpha(1f).setDuration(140).start()
            }
        }
    }

    private fun pokeUi() {
        setConsoleVisible(true)
        setStatusVisible(true)
    }

    private fun pushStateNow() {
        val now = System.currentTimeMillis()
        persistState(now, force = true)
    }

    private fun persistStateIfNeeded(now: Long) {
        persistState(now, force = false)
    }

    private fun persistState(now: Long, force: Boolean) {
        if (!force && now - lastPersistMs < 15000L) return
        val instance = currentInstance ?: return
        val state = petRuntime?.state ?: return
        lastPersistMs = now
        lifecycleScope.launch {
            repository.updateInstance(
                instance.copy(
                    energy = state.energy,
                    mood = state.mood,
                    hunger = state.hunger,
                    affection = state.affection,
                    lastTickTime = now
                )
            )
        }
    }

    private fun showDecorAddMenu() {
        val options = arrayOf("上传图片素材", "从内置素材选择")
        AlertDialog.Builder(this)
            .setTitle("添加小屋装饰")
            .setItems(options) { _, which ->
                if (which == 0) {
                    decorImagePicker.launch("image/*")
                } else {
                    showBuiltInDecorPicker()
                }
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    private fun showBuiltInDecorPicker() {
        lifecycleScope.launch {
            val builtIn = builtInDecorEntries()
            val custom = loadDecorLibrary()
                .mapIndexed { idx, path -> DecorEntry(label = "自定义素材 ${idx + 1}", path = path) }
            val dialogView = layoutInflater.inflate(R.layout.dialog_decor_picker, null)
            val root = dialogView.findViewById<LinearLayout>(R.id.decorContent)

            if (custom.isNotEmpty()) {
                root.addView(makeSectionTitle("素材库"))
                root.addView(makeDecorGrid(custom) { entry ->
                    entry.path?.let { addCustomDecor(it) }
                })
            }

            root.addView(makeSectionTitle("内置素材"))
            root.addView(makeDecorGrid(builtIn) { entry ->
                entry.resId?.let { addBuiltInDecor(it) }
            })

            AlertDialog.Builder(this@PetDetailActivity)
                .setView(dialogView as View)
                .setNegativeButton(getString(R.string.action_cancel), null)
                .show()
        }
    }

    private fun addRoomDecor(uri: Uri) {
        val instanceId = currentInstance?.instanceId ?: currentInstanceId
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val savedPath = copyDecorToInternal(uri) ?: return@launch
                addToDecorLibrary(savedPath)
                val id = UUID.randomUUID().toString()
                val item = RoomDecorItem(
                    id = id,
                    path = savedPath,
                    nx = Random.nextFloat().coerceIn(0.15f, 0.85f),
                    ny = Random.nextFloat().coerceIn(0.15f, 0.85f),
                    scale = Random.nextDouble(0.8, 1.25).toFloat(),
                    alpha = 0.95f
                )
                decorItems.add(item)
                saveRoomDecorForInstance(instanceId)
                withContext(Dispatchers.Main) {
                    renderRoomDecor()
                    Toast.makeText(this@PetDetailActivity, "已添加小屋装饰", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PetDetailActivity, "装饰导入失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun addCustomDecor(path: String) {
        val instanceId = currentInstance?.instanceId ?: currentInstanceId
        lifecycleScope.launch(Dispatchers.IO) {
            val item = RoomDecorItem(
                id = UUID.randomUUID().toString(),
                path = path,
                nx = Random.nextDouble(0.18, 0.84).toFloat(),
                ny = Random.nextDouble(0.16, 0.86).toFloat(),
                scale = Random.nextDouble(0.85, 1.25).toFloat(),
                alpha = 0.96f
            )
            decorItems.add(item)
            saveRoomDecorForInstance(instanceId)
            withContext(Dispatchers.Main) {
                renderRoomDecor()
                Toast.makeText(this@PetDetailActivity, "已添加素材库装饰", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addBuiltInDecor(drawableRes: Int) {
        val instanceId = currentInstance?.instanceId ?: currentInstanceId
        lifecycleScope.launch(Dispatchers.IO) {
            val item = RoomDecorItem(
                id = UUID.randomUUID().toString(),
                path = "res://${resources.getResourceEntryName(drawableRes)}",
                nx = Random.nextDouble(0.18, 0.84).toFloat(),
                ny = Random.nextDouble(0.16, 0.86).toFloat(),
                scale = Random.nextDouble(0.85, 1.25).toFloat(),
                alpha = 0.96f
            )
            decorItems.add(item)
            saveRoomDecorForInstance(instanceId)
            withContext(Dispatchers.Main) {
                renderRoomDecor()
                Toast.makeText(this@PetDetailActivity, "已添加内置装饰", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearRoomDecor() {
        val instanceId = currentInstance?.instanceId ?: currentInstanceId
        AlertDialog.Builder(this)
            .setTitle("清空小屋装饰")
            .setMessage("只清空当前宠物小屋的装饰，不影响其他宠物。")
            .setPositiveButton(getString(R.string.action_delete)) { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    decorItems.clear()
                    saveRoomDecorForInstance(instanceId)
                    withContext(Dispatchers.Main) {
                        renderRoomDecor()
                        Toast.makeText(this@PetDetailActivity, "已清空当前小屋装饰", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    private fun loadRoomDecorForInstance(instanceId: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            val key = roomDecorKey(instanceId)
            val value = repository.getSetting(key).firstOrNull()
            decorItems.clear()
            parseDecorJson(value).forEach { decorItems.add(it) }
            if (decorItems.isEmpty()) {
                decorItems.addAll(defaultDecorTemplate())
                saveRoomDecorForInstance(instanceId)
            }
            withContext(Dispatchers.Main) { renderRoomDecor() }
        }
    }

    private suspend fun saveRoomDecorForInstance(instanceId: Long) {
        val key = roomDecorKey(instanceId)
        repository.setSetting(key, decorToJson(decorItems))
    }

    private fun renderRoomDecor() {
        decorLayer.removeAllViews()
        val w = decorLayer.width
        val h = decorLayer.height
        if (w <= 0 || h <= 0) {
            decorLayer.post { renderRoomDecor() }
            return
        }
        decorItems.forEach { item ->
            val resId = resolveDecorResId(item.path)
            if (resId == null && !File(item.path).exists()) return@forEach
            val basePx = (64 * resources.displayMetrics.density * item.scale).roundToInt().coerceIn(40, 140)
            val iv = ImageView(this).apply {
                if (resId != null) {
                    setImageResource(resId)
                } else {
                    setImageURI(Uri.fromFile(File(item.path)))
                }
                alpha = item.alpha.coerceIn(0.5f, 1f)
                layoutParams = FrameLayout.LayoutParams(basePx, basePx).apply {
                    gravity = Gravity.TOP or Gravity.START
                    leftMargin = (item.nx * (w - basePx)).roundToInt().coerceIn(0, max(0, w - basePx))
                    topMargin = (item.ny * (h - basePx)).roundToInt().coerceIn(0, max(0, h - basePx))
                }
                // Drag to arrange in this room only.
                setOnTouchListener(object : View.OnTouchListener {
                    private var downX = 0f
                    private var downY = 0f
                    private var startL = 0
                    private var startT = 0
                    override fun onTouch(v: View, event: MotionEvent): Boolean {
                        val lp = v.layoutParams as FrameLayout.LayoutParams
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN -> {
                                downX = event.rawX
                                downY = event.rawY
                                startL = lp.leftMargin
                                startT = lp.topMargin
                                v.parent?.requestDisallowInterceptTouchEvent(true)
                                v.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                                return true
                            }
                            MotionEvent.ACTION_MOVE -> {
                                val nx = (startL + (event.rawX - downX)).roundToInt()
                                val ny = (startT + (event.rawY - downY)).roundToInt()
                                val clampedL = nx.coerceIn(0, max(0, w - basePx))
                                val clampedT = ny.coerceIn(0, max(0, h - basePx))
                                v.translationX = (clampedL - startL).toFloat()
                                v.translationY = (clampedT - startT).toFloat()
                                return true
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                val finalL = (startL + v.translationX).roundToInt().coerceIn(0, max(0, w - basePx))
                                val finalT = (startT + v.translationY).roundToInt().coerceIn(0, max(0, h - basePx))
                                v.translationX = 0f
                                v.translationY = 0f
                                lp.leftMargin = finalL
                                lp.topMargin = finalT
                                v.layoutParams = lp
                                v.setLayerType(View.LAYER_TYPE_NONE, null)
                                item.nx = if (w - basePx > 0) finalL.toFloat() / (w - basePx).toFloat() else 0.5f
                                item.ny = if (h - basePx > 0) finalT.toFloat() / (h - basePx).toFloat() else 0.5f
                                lifecycleScope.launch(Dispatchers.IO) {
                                    val id = currentInstance?.instanceId ?: currentInstanceId
                                    saveRoomDecorForInstance(id)
                                }
                                return true
                            }
                        }
                        return false
                    }
                })
                setOnLongClickListener {
                    decorItems.removeAll { it.id == item.id }
                    lifecycleScope.launch(Dispatchers.IO) {
                        val id = currentInstance?.instanceId ?: currentInstanceId
                        saveRoomDecorForInstance(id)
                        withContext(Dispatchers.Main) { renderRoomDecor() }
                    }
                    Toast.makeText(context, "已删除该装饰", Toast.LENGTH_SHORT).show()
                    true
                }
            }
            decorLayer.addView(iv)
        }
    }

    private fun parseDecorJson(raw: String?): List<RoomDecorItem> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val path = o.optString("path")
                    if (path.isBlank()) continue
                    add(
                        RoomDecorItem(
                            id = o.optString("id", UUID.randomUUID().toString()),
                            path = path,
                            nx = o.optDouble("nx", 0.5).toFloat(),
                            ny = o.optDouble("ny", 0.5).toFloat(),
                            scale = o.optDouble("scale", 1.0).toFloat(),
                            alpha = o.optDouble("alpha", 1.0).toFloat()
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun decorToJson(items: List<RoomDecorItem>): String {
        val arr = JSONArray()
        items.forEach { item ->
            val o = JSONObject()
            o.put("id", item.id)
            o.put("path", item.path)
            o.put("nx", item.nx.toDouble())
            o.put("ny", item.ny.toDouble())
            o.put("scale", item.scale.toDouble())
            o.put("alpha", item.alpha.toDouble())
            arr.put(o)
        }
        return arr.toString()
    }

    private fun copyDecorToInternal(uri: Uri): String? {
        val dir = File(filesDir, "room_decor/library")
        if (!dir.exists()) dir.mkdirs()
        val outFile = File(dir, "decor_${System.currentTimeMillis()}_${UUID.randomUUID()}.png")
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(outFile).use { output ->
                input.copyTo(output)
            }
        } ?: return null
        return outFile.absolutePath
    }

    private fun deleteDecorFiles() {
        // No-op: decor library assets are shared; avoid deleting user library files.
    }

    private fun roomDecorKey(instanceId: Long): String = "room_decor_$instanceId"

    private fun resolveDecorResId(path: String): Int? {
        if (!path.startsWith("res://")) return null
        val name = path.removePrefix("res://").trim()
        val resId = resources.getIdentifier(name, "drawable", packageName)
        return if (resId != 0) resId else null
    }

    private fun builtInDecorEntries(): List<DecorEntry> {
        return listOf(
            DecorEntry("气球", resId = R.drawable.fx_balloon_color),
            DecorEntry("礼物", resId = R.drawable.fx_gift_color),
            DecorEntry("蛋糕", resId = R.drawable.fx_cake_color),
            DecorEntry("星光", resId = R.drawable.fx_star_color),
            DecorEntry("彩带", resId = R.drawable.fx_confetti_color),
            DecorEntry("派对喷花", resId = R.drawable.fx_party_color),
            DecorEntry("爱心", resId = R.drawable.fx_heart_color),
            DecorEntry("音乐符号", resId = R.drawable.fx_music_notes_color),
            DecorEntry("火焰", resId = R.drawable.fx_fire_color),
            DecorEntry("睡觉 Zzz", resId = R.drawable.fx_sleep_color),
            DecorEntry("微笑", resId = R.drawable.fx_smile_color),
            DecorEntry("思考", resId = R.drawable.fx_think_color),
            DecorEntry("惊叹", resId = R.drawable.fx_exclaim_color),
            DecorEntry("波纹", resId = R.drawable.fx_wave_color),
            DecorEntry("阳光", resId = R.drawable.fx_sun_color),
            DecorEntry("食物", resId = R.drawable.fx_food_color),
            DecorEntry("像素砖 1", resId = R.drawable.decor_kenney_tile_0),
            DecorEntry("像素砖 2", resId = R.drawable.decor_kenney_tile_1),
            DecorEntry("像素砖 3", resId = R.drawable.decor_kenney_tile_2),
            DecorEntry("像素砖 4", resId = R.drawable.decor_kenney_tile_3),
            DecorEntry("像素砖 5", resId = R.drawable.decor_kenney_tile_4),
            DecorEntry("像素砖 6", resId = R.drawable.decor_kenney_tile_5),
            DecorEntry("像素砖 7", resId = R.drawable.decor_kenney_tile_6),
            DecorEntry("像素砖 8", resId = R.drawable.decor_kenney_tile_7),
            DecorEntry("像素砖 9", resId = R.drawable.decor_kenney_tile_8),
            DecorEntry("像素砖 10", resId = R.drawable.decor_kenney_tile_9),
            DecorEntry("像素砖 11", resId = R.drawable.decor_kenney_tile_10),
            DecorEntry("像素砖 12", resId = R.drawable.decor_kenney_tile_11)
        )
    }

    private fun defaultDecorTemplate(): List<RoomDecorItem> {
        val picks = listOf(
            RoomDecorItem(UUID.randomUUID().toString(), "res://fx_star_color", 0.08f, 0.09f, 0.95f, 0.9f),
            RoomDecorItem(UUID.randomUUID().toString(), "res://fx_balloon_color", 0.82f, 0.10f, 1.10f, 0.95f),
            RoomDecorItem(UUID.randomUUID().toString(), "res://fx_music_note_color", 0.16f, 0.32f, 0.90f, 0.9f),
            RoomDecorItem(UUID.randomUUID().toString(), "res://fx_heart_color", 0.74f, 0.34f, 0.88f, 0.9f),
            RoomDecorItem(UUID.randomUUID().toString(), "res://fx_spark_color", 0.52f, 0.14f, 0.82f, 0.9f),
            RoomDecorItem(UUID.randomUUID().toString(), "res://fx_gift_color", 0.10f, 0.70f, 1.02f, 0.95f),
            RoomDecorItem(UUID.randomUUID().toString(), "res://fx_cake_color", 0.80f, 0.72f, 1.02f, 0.95f)
        )
        return picks
    }

    private suspend fun loadDecorLibrary(): List<String> {
        val raw = repository.getSetting("decor_library").firstOrNull()
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            val originalCount = arr.length()
            val cleaned = buildList {
                for (i in 0 until originalCount) {
                    val path = arr.optString(i)
                    if (path.isNotBlank() && File(path).exists()) add(path)
                }
            }
            if (cleaned.isNotEmpty() && cleaned.size < originalCount) {
                val out = JSONArray()
                cleaned.forEach { out.put(it) }
                repository.setSetting("decor_library", out.toString())
            }
            cleaned
        }.getOrDefault(emptyList())
    }

    private suspend fun addToDecorLibrary(path: String) {
        if (!File(path).exists()) return
        val existing = loadDecorLibrary().toMutableList()
        if (!existing.contains(path)) {
            existing.add(0, path)
            val arr = JSONArray()
            existing.take(80).forEach { arr.put(it) }
            repository.setSetting("decor_library", arr.toString())
        }
    }

    private fun makeSectionTitle(text: String): TextView {
        return TextView(this).apply {
            setText(text)
            setTextColor(getColor(R.color.ui_text_title))
            textSize = 14f
            setPadding(4.dp(), 8.dp(), 4.dp(), 6.dp())
        }
    }

    private fun makeDecorGrid(entries: List<DecorEntry>, onPick: (DecorEntry) -> Unit): View {
        val grid = GridLayout(this).apply {
            columnCount = 3
            rowCount = ((entries.size + 2) / 3)
            useDefaultMargins = true
        }
        entries.forEach { entry ->
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(10.dp(), 10.dp(), 10.dp(), 10.dp())
                setBackgroundResource(R.drawable.bg_control_card)
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                }
            }
            val iv = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(64.dp(), 64.dp())
                scaleType = ImageView.ScaleType.FIT_CENTER
                loadDecorPreview(entry, this)
            }
            val tv = TextView(this).apply {
                text = entry.label
                textSize = 11f
                setTextColor(getColor(R.color.ui_text_mute))
                setPadding(0, 6.dp(), 0, 0)
                maxLines = 1
            }
            item.addView(iv)
            item.addView(tv)
            item.setOnClickListener { onPick(entry) }
            grid.addView(item)
        }
        return grid
    }

    private fun loadDecorPreview(entry: DecorEntry, iv: ImageView) {
        if (entry.resId != null) {
            iv.setImageResource(entry.resId)
            return
        }
        val path = entry.path ?: return
        val file = File(path)
        if (!file.exists()) {
            iv.setImageResource(R.drawable.fx_star_color)
            return
        }
        val opts = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(path, opts)
        val target = 96
        var sample = 1
        while (opts.outWidth / sample > target || opts.outHeight / sample > target) {
            sample *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sample.coerceAtLeast(1)
        }
        val bmp = BitmapFactory.decodeFile(path, decodeOpts)
        if (bmp != null) {
            iv.setImageBitmap(bmp)
        } else {
            iv.setImageResource(R.drawable.fx_star_color)
        }
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).roundToInt()

    private fun spawnCelebrationAroundCrown(crownView: View) {
        val baseX = crownView.x + crownView.width / 2f
        val baseY = crownView.y + crownView.height / 2f
        val burst = listOf(
            R.drawable.fx_balloon_color,
            R.drawable.fx_party_color,
            R.drawable.fx_confetti_color,
            R.drawable.fx_star_color,
            R.drawable.fx_heart_color
        )
        repeat(16) { index ->
            handler.postDelayed({
                val res = burst.random()
                val iv = ImageView(this).apply {
                    setImageResource(res)
                    alpha = 0.95f
                }
                val sizePx = ((24 + Random.nextInt(16)) * resources.displayMetrics.density).roundToInt()
                val params = FrameLayout.LayoutParams(sizePx, sizePx).apply {
                    gravity = Gravity.TOP or Gravity.START
                    leftMargin = clampInRange((baseX + Random.nextInt(-80, 80)).roundToInt(), 0, max(0, roomContainer.width - sizePx))
                    topMargin = clampInRange((baseY + Random.nextInt(-80, 80)).roundToInt(), 0, max(0, roomContainer.height - sizePx))
                }
                roomContainer.addView(iv, params)
                iv.animate()
                    .translationY((-110 - Random.nextInt(80)).toFloat())
                    .alpha(0f)
                    .setDuration(1200)
                    .withEndAction { roomContainer.removeView(iv) }
                    .start()
            }, (index * 90L))
        }
    }
    
    companion object {
        const val EXTRA_PET_INSTANCE_ID = "extra_pet_instance_id"
    }
}

