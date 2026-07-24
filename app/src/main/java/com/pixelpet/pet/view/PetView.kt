package com.pixelpet.pet.view

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import com.pixelpet.content.ContentPackManifest
import com.pixelpet.core.LogUtils
import com.pixelpet.pet.model.PetBehavior
import com.pixelpet.pet.model.PetState
import com.pixelpet.util.BitmapUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PetView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val imageView = ImageView(context)
    private val sleepBubble = TextView(context)

    // Assets
    private var staticBitmap: Bitmap? = null
    private var staticAltBitmap: Bitmap? = null

    // State
    private var baseScale = 1.0f
    private var displayScale = 1.0f
    private var facingDirection = 1
    private var usingAltForm = false
    private var altFormUntilMs = 0L

    private var sleepAnimator: AnimatorSet? = null

    /**
     * View 自身的作用域：[loadAssets] 的协程在此运行，
     * 在 [onDetachedFromWindow] 时统一取消，避免 Activity/Service 销毁后协程仍在后台解码造成泄漏。
     */
    private val viewScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var loadJob: Job? = null

    init {
        imageView.adjustViewBounds = true
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        lp.gravity = android.view.Gravity.CENTER
        addView(imageView, lp)

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
        // 取消上一次未完成的加载，避免快速连续切换素材时旧协程覆盖新状态。
        loadJob?.cancel()
        loadJob = viewScope.launch {
            try {
                val staticPath = if (manifest.staticNormal.isNotBlank()) manifest.staticNormal else manifest.preview
                val altPath = manifest.staticTongue.takeIf { it.isNotBlank() }
                val targetW = manifest.hitbox.w.takeIf { it > 0 }
                val targetH = manifest.hitbox.h.takeIf { it > 0 }

                val staticBmp = withContext(Dispatchers.IO) {
                    BitmapUtils.decodeSampledFromPack(context, staticPath, targetW, targetH)
                }
                val altBmp = altPath?.let {
                    withContext(Dispatchers.IO) {
                        BitmapUtils.decodeSampledFromPack(context, it, targetW, targetH)
                    }
                }

                staticBitmap = staticBmp
                staticAltBitmap = altBmp
                usingAltForm = false
                altFormUntilMs = 0L
                applyCurrentFormBitmap()
                baseScale = manifest.defaultScale.toFloat()
                updateLayoutSize()
            } catch (e: Exception) {
                LogUtils.e(TAG, "loadAssets failed for ${manifest.id}", e)
            }
        }
    }

    fun updateState(state: PetState) {
        if (state.behavior == PetBehavior.SLEEP) {
            sleepBubble.visibility = VISIBLE
            startSleepAnimation()
        } else {
            sleepBubble.visibility = GONE
            stopSleepAnimation()
        }

        // 临时变身到期自动还原。
        if (usingAltForm && altFormUntilMs > 0L && System.currentTimeMillis() >= altFormUntilMs) {
            usingAltForm = false
            altFormUntilMs = 0L
            applyCurrentFormBitmap()
        }
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

        imageView.layoutParams.width = (w * totalScale).toInt()
        imageView.layoutParams.height = (h * totalScale).toInt()
        imageView.requestLayout()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopSleepAnimation()
        viewScope.cancel()
    }

    companion object {
        private const val TAG = "PetView"
    }
}
