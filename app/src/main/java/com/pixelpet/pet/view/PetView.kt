package com.pixelpet.pet.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.animation.LinearInterpolator
import com.pixelpet.content.AssetLoader
import com.pixelpet.content.ContentPackManifest
import com.pixelpet.pet.model.PetBehavior
import com.pixelpet.pet.model.PetState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

class PetView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val imageView = ImageView(context)
    private val sleepBubble = TextView(context)
    
    // Assets
    private var idleSheet: Bitmap? = null
    private var staticBitmap: Bitmap? = null
    private var staticAltBitmap: Bitmap? = null
    
    // State
    private var baseScale = 1.0f
    private var displayScale = 1.0f
    private var facingDirection = 1
    private var usingAltForm = false
    private var altFormUntilMs = 0L
    
    // Animation
    private var currentFrameIndex = 0
    private var lastFrameTime = 0L
    private var frameDuration = 200L // Default 200ms
    private var frameCount = 1
    private var sleepAnimator: AnimatorSet? = null

    init {
        // Image View Setup
        imageView.adjustViewBounds = true
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        lp.gravity = android.view.Gravity.CENTER
        addView(imageView, lp)

        // Sleep Bubble Setup
        sleepBubble.text = "ZzZ"
        sleepBubble.textSize = 26f
        sleepBubble.setTextColor(0xFF5D4037.toInt())
        sleepBubble.setShadowLayer(6f, 0f, 2f, 0x33000000)
        sleepBubble.visibility = GONE
        val lpSleep = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        lpSleep.gravity = android.view.Gravity.TOP or android.view.Gravity.END
        lpSleep.topMargin = (8 * context.resources.displayMetrics.density).toInt()
        lpSleep.marginEnd = (4 * context.resources.displayMetrics.density).toInt()
        addView(sleepBubble, lpSleep)

        clipChildren = false
        clipToPadding = false
    }

    fun loadAssets(manifest: ContentPackManifest) {
        // Simple loading logic. In a real app, use AssetLoader or Glide/Coil.
        // Assuming manifests provide paths relative to assets/
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Load Static
                val staticPath = if (manifest.staticNormal.isNotBlank()) {
                    manifest.staticNormal
                } else {
                    manifest.preview
                }
                val altPath = manifest.staticTongue.takeIf { it.isNotBlank() }
                val targetW = manifest.hitbox.w.takeIf { it > 0 }
                val targetH = manifest.hitbox.h.takeIf { it > 0 }
                val staticBmp = loadBitmapFromAssets(staticPath, targetW, targetH)
                val altBmp = altPath?.let { loadBitmapFromAssets(it, targetW, targetH) }
                
                // Load Idle Sheet (if exists)
                // For simplicity, we might just use static for now unless we parse the sheet
                // But the user wants "ZZZ" and hearts, which are overlays.
                
                withContext(Dispatchers.Main) {
                    staticBitmap = staticBmp
                    staticAltBitmap = altBmp
                    usingAltForm = false
                    altFormUntilMs = 0L
                    applyCurrentFormBitmap()
                    baseScale = manifest.defaultScale.toFloat()
                    updateLayoutSize()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun loadBitmapFromAssets(path: String, targetW: Int?, targetH: Int?): Bitmap? {
        return try {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            AssetLoader.openStream(context, path).use { BitmapFactory.decodeStream(it, null, boundsOptions) }

            val sourceW = boundsOptions.outWidth
            val sourceH = boundsOptions.outHeight
            if (sourceW <= 0 || sourceH <= 0) return null

            val desiredW = (targetW ?: sourceW).coerceIn(1, MAX_BITMAP_DIM_PX)
            val desiredH = (targetH ?: sourceH).coerceIn(1, MAX_BITMAP_DIM_PX)
            val sampleSize = calculateInSampleSize(sourceW, sourceH, desiredW, desiredH)

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inScaled = false
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val decoded = AssetLoader.openStream(context, path).use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            } ?: return null

            if (decoded.width <= desiredW && decoded.height <= desiredH) {
                decoded
            } else {
                val ratio = min(desiredW.toFloat() / decoded.width, desiredH.toFloat() / decoded.height)
                val scaledW = max(1, (decoded.width * ratio).toInt())
                val scaledH = max(1, (decoded.height * ratio).toInt())
                val scaled = Bitmap.createScaledBitmap(decoded, scaledW, scaledH, false)
                if (scaled !== decoded) decoded.recycle()
                scaled
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateInSampleSize(sourceW: Int, sourceH: Int, reqW: Int, reqH: Int): Int {
        var sampleSize = 1
        var halfW = sourceW / 2
        var halfH = sourceH / 2
        while (halfW / sampleSize >= reqW && halfH / sampleSize >= reqH) {
            sampleSize *= 2
        }
        return sampleSize.coerceAtLeast(1)
    }

    fun updateState(state: PetState) {
        // Sleep Bubble
        if (state.behavior == PetBehavior.SLEEP) {
            sleepBubble.visibility = VISIBLE
            startSleepAnimation()
        } else {
            sleepBubble.visibility = GONE
            stopSleepAnimation()
        }

        // Auto revert temporary transformed form.
        if (usingAltForm && altFormUntilMs > 0L && System.currentTimeMillis() >= altFormUntilMs) {
            usingAltForm = false
            altFormUntilMs = 0L
            applyCurrentFormBitmap()
        }

        // Update Animation Frame if needed
        // For now, we keep it simple with static image + flipping
    }

    private fun startSleepAnimation() {
        if (sleepAnimator != null) return
        sleepBubble.alpha = 0.7f
        sleepBubble.translationY = 0f
        sleepBubble.scaleX = 1f
        sleepBubble.scaleY = 1f

        val floatUp = ObjectAnimator.ofFloat(sleepBubble, "translationY", 0f, -18f).apply {
            duration = 1600L
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = LinearInterpolator()
        }
        val fade = ObjectAnimator.ofFloat(sleepBubble, "alpha", 0.5f, 1f).apply {
            duration = 1600L
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = LinearInterpolator()
        }
        val pulseX = ObjectAnimator.ofFloat(sleepBubble, "scaleX", 0.95f, 1.05f).apply {
            duration = 1200L
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = LinearInterpolator()
        }
        val pulseY = ObjectAnimator.ofFloat(sleepBubble, "scaleY", 0.95f, 1.05f).apply {
            duration = 1200L
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = LinearInterpolator()
        }
        sleepAnimator = AnimatorSet().apply {
            playTogether(floatUp, fade, pulseX, pulseY)
            start()
        }
    }

    private fun stopSleepAnimation() {
        sleepAnimator?.cancel()
        sleepAnimator = null
        sleepBubble.translationY = 0f
        sleepBubble.alpha = 1f
        sleepBubble.scaleX = 1f
        sleepBubble.scaleY = 1f
    }

    fun toggleAlternateForm(durationMs: Long = 2200L) {
        if (staticAltBitmap == null) return
        usingAltForm = !usingAltForm
        altFormUntilMs = if (usingAltForm && durationMs > 0L) System.currentTimeMillis() + durationMs else 0L
        applyCurrentFormBitmap()
    }

    private fun applyCurrentFormBitmap() {
        val bitmap = if (usingAltForm && staticAltBitmap != null) staticAltBitmap else staticBitmap
        imageView.setImageBitmap(bitmap)
        updateLayoutSize()
    }

    fun setDisplayScale(scale: Float) {
        if (displayScale != scale) {
            displayScale = scale
            updateLayoutSize()
        }
    }

    fun setFacingDirection(direction: Int) {
        if (facingDirection != direction) {
            facingDirection = direction
            imageView.scaleX = if (direction > 0) 1f else -1f
        }
    }

    private fun updateLayoutSize() {
        val density = context.resources.displayMetrics.density
        val totalScale = baseScale * displayScale * density
        
        val drawable = imageView.drawable ?: return
        val w = drawable.intrinsicWidth
        val h = drawable.intrinsicHeight
        if (w <= 0 || h <= 0) return
        
        val targetW = (w * totalScale).toInt()
        val targetH = (h * totalScale).toInt()
        
        imageView.layoutParams.width = targetW
        imageView.layoutParams.height = targetH
        imageView.requestLayout()
    }

    companion object {
        private const val MAX_BITMAP_DIM_PX = 512
    }
}

