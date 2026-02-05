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

class PetView(context: Context) : FrameLayout(context) {
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
        addView(imageView, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
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
            
            // Apply scale
            imageView.scaleX = manifest.defaultScale.toFloat()
            imageView.scaleY = manifest.defaultScale.toFloat()
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
        // Try direct path first
        try {
            return context.assets.open(path).use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            // Try appending .base64 (Development artifact support)
            try {
                val b64Path = "$path.base64"
                val b64String = context.assets.open(b64Path).bufferedReader().use { it.readText() }
                val bytes = Base64.decode(b64String, Base64.DEFAULT)
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
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
                return
            }
        }

        // 3. Fallback to Static Normal
        if (normalBitmap != null) imageView.setImageBitmap(normalBitmap)
        else imageView.setImageResource(normalRes)
        imageView.rotation = 0f
    }
}


