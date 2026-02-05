package app.pet

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.FrameLayout
import android.widget.ImageView
import app.content.ContentPackManifest
import com.pixelpet.R
import java.io.InputStream

class PetView(context: Context) : FrameLayout(context) {
    private val imageView = ImageView(context)
    private var normalBitmap: Bitmap? = null
    private var tongueBitmap: Bitmap? = null
    
    // Fallback resources
    private val normalRes = R.drawable.normal
    private val tongueRes = R.drawable.tongue
    
    init {
        imageView.setImageResource(normalRes)
        addView(imageView, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
    }

    fun loadAssets(manifest: ContentPackManifest) {
        try {
            normalBitmap = loadBitmapFromAsset(manifest.staticNormal)
            tongueBitmap = loadBitmapFromAsset(manifest.staticTongue)
            
            // Apply scale
            imageView.scaleX = manifest.defaultScale.toFloat()
            imageView.scaleY = manifest.defaultScale.toFloat()
        } catch (e: Exception) {
            e.printStackTrace()
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
        // MVP: Simple resource switching
        // In future: Play animations from assets
        when (state.behavior) {
            PetBehavior.CUTE -> {
                if (tongueBitmap != null) imageView.setImageBitmap(tongueBitmap)
                else imageView.setImageResource(tongueRes)
            }
            PetBehavior.SPIN -> {
                 if (normalBitmap != null) imageView.setImageBitmap(normalBitmap)
                 else imageView.setImageResource(normalRes)
                 
                 imageView.rotation = (System.currentTimeMillis() % 360).toFloat()
            }
            else -> {
                if (normalBitmap != null) imageView.setImageBitmap(normalBitmap)
                else imageView.setImageResource(normalRes)
                
                imageView.rotation = 0f
            }
        }
    }
}


