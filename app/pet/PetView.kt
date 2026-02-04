package app.pet

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.FrameLayout
import android.widget.ImageView

class PetView(context: Context) : FrameLayout(context) {
    private val imageView = ImageView(context)
    private val normalBitmap: Bitmap?
    private val tongueBitmap: Bitmap?

    init {
        normalBitmap = loadBitmap("normal.png")
        tongueBitmap = loadBitmap("tongue.png")

        imageView.setImageBitmap(normalBitmap)
        addView(imageView, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
    }

    fun playTapReaction() {
        if (tongueBitmap == null || normalBitmap == null) return
        imageView.setImageBitmap(tongueBitmap)
        postDelayed({
            imageView.setImageBitmap(normalBitmap)
        }, 500L)
    }

    private fun loadBitmap(assetName: String): Bitmap? {
        return try {
            context.assets.open(assetName).use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (error: Exception) {
            null
        }
    }
}
