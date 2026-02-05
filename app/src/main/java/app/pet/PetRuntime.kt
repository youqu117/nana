package app.pet

import app.content.ContentPackManifest
import kotlin.random.Random

class PetRuntime(
    val manifest: ContentPackManifest,
    initialState: PetState = PetState()
) {
    var state: PetState = initialState
        private set

    private var lastInteractionTime = 0L

    fun tick(nowMs: Long) {
        // 1. Update stats (decay)
        state = state.tick(nowMs)

        // 2. Handle Behavior Transitions
        val elapsed = nowMs - state.behaviorStartTime
        
        when (state.behavior) {
            PetBehavior.CUTE -> {
                if (elapsed > 1200) { // 1.2s max for Cute
                    state = state.withBehavior(PetBehavior.IDLE)
                }
            }
            PetBehavior.SPIN, PetBehavior.RUN -> {
                if (elapsed > 1500) { // 1.5s max for Spin/Run
                    state = state.withBehavior(PetBehavior.IDLE)
                }
            }
            PetBehavior.HELD -> {
                // Stay in Held until released
            }
            PetBehavior.WALK -> {
                if (elapsed > 3000) { // Walk for 3s then idle
                    state = state.withBehavior(PetBehavior.IDLE)
                }
            }
            PetBehavior.IDLE -> {
                // Randomly walk
                if (elapsed > 5000 && Random.nextFloat() < 0.3f) {
                     state = state.withBehavior(PetBehavior.WALK)
                }
                // Check for sleep if inactive (simulated)
            }
            else -> {}
        }
    }

    fun handleTap(nowMs: Long) {
        if (nowMs - lastInteractionTime < 800) return // Cooldown
        lastInteractionTime = nowMs
        
        state = state.applyTap()
        state = state.withBehavior(PetBehavior.CUTE)
    }

    fun handleFeed() {
        state = state.applyFeed()
        state = state.withBehavior(PetBehavior.CUTE) // React to feeding
    }

    fun handleDoubleTap(nowMs: Long) {
        if (nowMs - lastInteractionTime < 2000) return // Cooldown
        lastInteractionTime = nowMs
        
        state = state.applyDoubleTap()
        // Randomly choose Spin or Run
        val next = if (Random.nextBoolean()) PetBehavior.SPIN else PetBehavior.RUN
        state = state.withBehavior(next)
    }

    fun handleLongPressStart() {
        state = state.withBehavior(PetBehavior.HELD)
    }

    fun handleRelease() {
        if (state.behavior == PetBehavior.HELD) {
            state = state.withBehavior(PetBehavior.IDLE) // Or Land/Fall
        }
    }

    fun restoreState(newState: PetState) {
        state = newState
    }
}

