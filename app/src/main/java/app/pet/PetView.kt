package app.pet

import android.content.Context
import android.widget.FrameLayout
import android.widget.ImageView
import com.pixelpet.R

class PetView(context: Context) : FrameLayout(context) {
    private val imageView = ImageView(context)
    private val normalRes = R.drawable.normal
    private val tongueRes = R.drawable.tongue
    
    init {
        imageView.setImageResource(normalRes)
        addView(imageView, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
    }

    fun updateState(state: PetState) {
        // MVP: Simple resource switching
        // In future: Play animations from assets
        when (state.behavior) {
            PetBehavior.CUTE -> imageView.setImageResource(tongueRes)
            PetBehavior.SPIN -> {
                 imageView.setImageResource(normalRes)
                 imageView.rotation = (System.currentTimeMillis() % 360).toFloat()
            }
            else -> {
                imageView.setImageResource(normalRes)
                imageView.rotation = 0f
            }
        }
        
        // MVP: Update alpha/scale if needed
        // alpha = state.alpha
    }
}

