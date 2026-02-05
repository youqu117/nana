package app.pet

import android.content.Context
import android.widget.FrameLayout
import android.widget.ImageView
import app.R

class PetView(context: Context) : FrameLayout(context) {
    private val imageView = ImageView(context)
    private val normalRes = R.drawable.normal
    private val tongueRes = R.drawable.tongue
    private var resetRunnable: Runnable? = null

    init {
        imageView.setImageResource(normalRes)
        addView(imageView, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
    }

    fun playTapReaction() {
        resetRunnable?.let { removeCallbacks(it) }
        imageView.setImageResource(tongueRes)
        val runnable = Runnable { imageView.setImageResource(normalRes) }
        resetRunnable = runnable
        postDelayed(runnable, 500L)
    }
}
