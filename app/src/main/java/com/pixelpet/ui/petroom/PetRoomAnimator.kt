package com.pixelpet.ui.petroom

import android.widget.FrameLayout
import com.pixelpet.pet.model.PetBehavior
import com.pixelpet.pet.view.PetView
import kotlin.math.sqrt
import kotlin.random.Random

class PetRoomAnimator {
    var isDriftEnabled = true
    var driftSpeed = 50

    private var targetX = 0f
    private var targetY = 0f
    private var startX = 0f
    private var startY = 0f
    private var moveStartTime = 0L
    private var moveDuration = 0L
    private var isMovingToTarget = false

    fun updateMovement(
        petView: PetView,
        roomContainer: FrameLayout?,
        isEnabled: Boolean,
        behavior: PetBehavior,
        now: Long
    ): Boolean {
        if (isEnabled) {
            isMovingToTarget = false
            petView.rotation = 0f
            return false
        }

        val shouldMove = (behavior == PetBehavior.WALK || behavior == PetBehavior.RUN) ||
                (isDriftEnabled && behavior != PetBehavior.SLEEP && behavior != PetBehavior.HELD)

        if (shouldMove) {
            if (!isMovingToTarget && roomContainer != null) {
                pickNewTarget(petView, roomContainer)
            }

            val elapsed = now - moveStartTime
            if (elapsed < moveDuration && moveDuration > 0) {
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
        return shouldMove
    }

    fun pickNewTarget(petView: PetView, container: FrameLayout) {
        val containerW = container.width
        val containerH = container.height
        val viewW = petView.width
        val viewH = petView.height

        if (containerW == 0 || viewW == 0) return

        val maxTransX = ((containerW - viewW) / 2f - 20f).coerceAtLeast(0f)
        val maxTransY = ((containerH - viewH) / 2f - 20f).coerceAtLeast(0f)

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
        val distance = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

        val speedFactor = if (isDriftEnabled && driftSpeed > 0) {
            10.0f - (driftSpeed / 100f * 9.0f)
        } else {
            4.0f
        }

        moveDuration = (distance * speedFactor).toLong().coerceAtLeast(1000L)
        isMovingToTarget = true

        if (dx > 0) {
            petView.setFacingDirection(1)
        } else if (dx < 0) {
            petView.setFacingDirection(-1)
        }
    }

    private fun easeInOutCubic(t: Float): Float {
        return if (t < 0.5f) 4 * t * t * t else 1 - Math.pow((-2 * t + 2).toDouble(), 3.0).toFloat() / 2
    }
}
