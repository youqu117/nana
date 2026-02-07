package com.pixelpet.input

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

class GestureHandler(
    private val onTap: () -> Unit,
    private val onDoubleTap: () -> Unit,
    private val onLongPress: () -> Unit,
    private val onDragStart: () -> Unit,
    private val onDrag: (dx: Int, dy: Int) -> Unit,
    private val onDragEnd: () -> Unit
) : View.OnTouchListener {

    private lateinit var gestureDetector: GestureDetector
    private var isDragging = false
    private var lastRawX = 0f
    private var lastRawY = 0f
    private var initialX = 0f
    private var initialY = 0f

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        if (!::gestureDetector.isInitialized) {
            gestureDetector = GestureDetector(view.context, GestureListener())
        }
        
        // Handle Dragging manually as GestureDetector doesn't handle raw movement efficiently for overlays
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastRawX = event.rawX
                lastRawY = event.rawY
                initialX = event.rawX
                initialY = event.rawY
                isDragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - lastRawX).toInt()
                val dy = (event.rawY - lastRawY).toInt()
                
                // Threshold for drag start
                if (!isDragging) {
                    val dist = abs(event.rawX - initialX) + abs(event.rawY - initialY)
                    if (dist > 10) { // Simple slop
                        isDragging = true
                        onDragStart()
                    }
                }
                
                if (isDragging) {
                    onDrag(dx, dy)
                    lastRawX = event.rawX
                    lastRawY = event.rawY
                    return true // Consume drag
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    onDragEnd()
                    isDragging = false
                    return true
                }
            }
        }

        return gestureDetector.onTouchEvent(event)
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            onTap()
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            onDoubleTap()
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            if (!isDragging) {
                onLongPress()
            }
        }
        
        override fun onDown(e: MotionEvent): Boolean = true
    }
}

