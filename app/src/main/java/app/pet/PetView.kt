package app.pet

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.FrameLayout
import android.widget.ImageView
import app.content.ContentPackManifest
import com.pixelpet.R
import org.json.JSONObject
import java.io.InputStream

import android.util.AttributeSet

class PetView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val imageView = ImageView(context)
    private var normalBitmap: Bitmap? = null
    private var tongueBitmap: Bitmap? = null
    
    // Animation Support
    private var idleAnimation: AnimationSequence? = null
    
    // Fallback resources
    private val normalRes = R.drawable.normal
    private val tongueRes = R.drawable.tongue
    
    data class AnimationSequence(
        val frames: List<Bitmap>,
        val durations: List<Int>,
        val loop: Boolean,
        val totalDuration: Int
    )
    
    init {
        imageView.setImageResource(normalRes)
        imageView.adjustViewBounds = true
        imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
        imageView.setFilterBitmap(false)
        addView(imageView, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        clipChildren = false
        clipToPadding = false
    }

    // Base scale from manifest (defaults to 1.0 if not specified)
    private var baseScale = 1.0f
    
    // External display scale (e.g., from settings)
    private var displayScale = 1.0f
    
    // Facing direction (1 or -1)
    private var facingDirection = 1
    
    fun setDisplayScale(scale: Float) {
        this.displayScale = scale
        updateLayoutSize()
    }
    
    fun setFacingDirection(direction: Int) {
        this.facingDirection = direction
        // Apply flip using scaleX
        imageView.scaleX = if (direction > 0) 1f else -1f
    }

    private fun updateLayoutSize() {
        // Calculate total scale
        val totalScale = baseScale * displayScale
        
        // We want to scale the ImageView's LAYOUT params, not just visual scale
        // But ImageView inside FrameLayout needs explicit size to force FrameLayout size change
        // OR we just use scaleX/Y on the ImageView but ensure FrameLayout is large enough?
        // Better: Set the ImageView size explicitly based on drawable size * scale
        
        val drawable = imageView.drawable ?: return
        val w = drawable.intrinsicWidth
        val h = drawable.intrinsicHeight
        if (w <= 0 || h <= 0) return
        
        val targetW = (w * totalScale).toInt()
        val targetH = (h * totalScale).toInt()
        
        val lp = imageView.layoutParams
        lp.width = targetW
        lp.height = targetH
        imageView.layoutParams = lp
        
        // Reset scaleX/Y to 1 (except for direction flip)
        imageView.scaleX = if (facingDirection > 0) 1f else -1f
        imageView.scaleY = 1f
    }

    fun loadAssets(manifest: ContentPackManifest) {
        try {
            normalBitmap = loadBitmapFromAsset(manifest.staticNormal)
            tongueBitmap = loadBitmapFromAsset(manifest.staticTongue)
            
            // Load Idle Animation
            if (manifest.idleSheet.isNotEmpty() && manifest.idleAnim.isNotEmpty()) {
                val sheet = loadBitmapFromAsset(manifest.idleSheet)
                val animJsonStr = loadStringFromAsset(manifest.idleAnim)
                
                if (sheet != null && animJsonStr != null) {
                    idleAnimation = parseAnimation(sheet, animJsonStr)
                }
            }
            
            // Set base scale
            baseScale = manifest.defaultScale.toFloat()
            
            // Initial update
            // Set the image first so updateLayoutSize can read intrinsic dimensions
            if (normalBitmap != null) imageView.setImageBitmap(normalBitmap)
            else imageView.setImageResource(normalRes)
            
            updateLayoutSize()
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun parseAnimation(sheet: Bitmap, jsonString: String): AnimationSequence? {
        try {
            val json = JSONObject(jsonString)
            val loop = json.optBoolean("loop", true)
            val framesArray = json.getJSONArray("frames")
            
            val frames = mutableListOf<Bitmap>()
            val durations = mutableListOf<Int>()
            var totalDuration = 0
            
            for (i in 0 until framesArray.length()) {
                val frameObj = framesArray.getJSONObject(i)
                val x = frameObj.getInt("x")
                val y = frameObj.getInt("y")
                val w = frameObj.getInt("w")
                val h = frameObj.getInt("h")
                val duration = frameObj.getInt("duration")
                
                // Slice bitmap
                if (x + w <= sheet.width && y + h <= sheet.height) {
                    val frameBitmap = Bitmap.createBitmap(sheet, x, y, w, h)
                    frames.add(frameBitmap)
                    durations.add(duration)
                    totalDuration += duration
                }
            }
            
            return AnimationSequence(frames, durations, loop, totalDuration)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    private fun loadStringFromAsset(path: String): String? {
        try {
            return context.assets.open(path).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            return null
        }
    }
    
    private fun loadBitmapFromAsset(path: String): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inScaled = false
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        // Try direct path first
        try {
            return context.assets.open(path).use { BitmapFactory.decodeStream(it, null, options) }
        } catch (e: Exception) {
            // Try appending .base64 (Development artifact support)
            try {
                val b64Path = "$path.base64"
                val b64String = context.assets.open(b64Path).bufferedReader().use { it.readText() }
                val bytes = Base64.decode(b64String, Base64.DEFAULT)
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            } catch (e2: Exception) {
                // e2.printStackTrace()
            }
        }
        return null
    }

    fun updateState(state: PetState) {
        // 1. Behavior Overrides (Static High Priority)
        if (state.behavior == PetBehavior.CUTE) {
            if (tongueBitmap != null) imageView.setImageBitmap(tongueBitmap)
            else imageView.setImageResource(tongueRes)
            imageView.rotation = 0f
            // Update layout if bitmap changed dimensions
            // updateLayoutSize() // Optimization: Assume similar sizes for now
            return
        }
        
        if (state.behavior == PetBehavior.SPIN) {
            if (normalBitmap != null) imageView.setImageBitmap(normalBitmap)
            else imageView.setImageResource(normalRes)
            imageView.rotation = (System.currentTimeMillis() % 360).toFloat()
            return
        }

        // 2. Animations
        val anim = idleAnimation
        if (anim != null && (state.behavior == PetBehavior.IDLE || state.behavior == PetBehavior.WALK || state.behavior == PetBehavior.RUN)) {
            val now = System.currentTimeMillis()
            // Use behaviorStartTime to sync animation
            val elapsed = now - state.behaviorStartTime
            
            val timeInLoop = if (anim.loop) (elapsed % anim.totalDuration).toInt() else elapsed.toInt().coerceAtMost(anim.totalDuration)
            
            // Find frame
            var currentDur = 0
            var frameIndex = 0
            for (i in anim.durations.indices) {
                currentDur += anim.durations[i]
                if (timeInLoop < currentDur) {
                    frameIndex = i
                    break
                }
            }
            
            if (frameIndex < anim.frames.size) {
                imageView.setImageBitmap(anim.frames[frameIndex])
                imageView.rotation = 0f
                // In a perfect world we'd updateLayoutSize() here if frames differ in size
                return
            }
        }

        // 3. Fallback to Static Normal
        if (normalBitmap != null) imageView.setImageBitmap(normalBitmap)
        else imageView.setImageResource(normalRes)
        imageView.rotation = 0f
    }
}

