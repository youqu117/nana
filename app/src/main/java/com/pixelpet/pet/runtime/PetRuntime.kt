package com.pixelpet.pet.runtime

import com.pixelpet.content.ContentPackManifest
import com.pixelpet.pet.interaction.PetEmote
import com.pixelpet.pet.interaction.PetEmoteEvent
import com.pixelpet.pet.model.PetBehavior
import com.pixelpet.pet.model.PetState
import kotlin.random.Random

class PetRuntime(
    val manifest: ContentPackManifest,
    initialState: PetState = PetState()
) {
    var state: PetState = initialState
        private set

    private var lastInteractionTime = 0L
    private var lastAutoActionTime = 0L
    private var lastEmotionTime = 0L
    private var lastBehavior: PetBehavior = initialState.behavior

    private var emoteSeq = 0L
    private var lastEmote: PetEmoteEvent? = null

    fun pollEmote(lastSeenSeq: Long): PetEmoteEvent? {
        val event = lastEmote ?: return null
        return if (event.seq > lastSeenSeq) event else null
    }

    private fun emitEmote(type: PetEmote, nowMs: Long) {
        emoteSeq += 1
        lastEmote = PetEmoteEvent(type, emoteSeq, nowMs)
    }

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

        // 3. Autonomous behaviors (sing/greet/sleep/happy)
        val idleEnough = state.behavior == PetBehavior.IDLE && (nowMs - state.behaviorStartTime) > 2500
        if (idleEnough && nowMs - lastAutoActionTime > 8000) {
            val moodFactor = state.mood.coerceIn(0, 100) / 100f

            // Weighted random based on mood/energy
            val roll = Random.nextFloat()
            val singChance = 0.08f + moodFactor * 0.10f
            val greetChance = 0.06f + moodFactor * 0.08f
            val sleepChance = if (state.energy < 25) 0.18f else 0.04f
            val happyChance = 0.05f + moodFactor * 0.12f
            val thinkChance = if (state.mood < 35) 0.08f else 0.03f

            if (roll < sleepChance) {
                state = state.withBehavior(PetBehavior.SLEEP)
                lastAutoActionTime = nowMs
            } else if (roll < sleepChance + singChance) {
                state = state.applySing().withBehavior(PetBehavior.CUTE)
                emitEmote(PetEmote.SING, nowMs)
                lastAutoActionTime = nowMs
            } else if (roll < sleepChance + singChance + greetChance) {
                state = state.applyGreet().withBehavior(PetBehavior.CUTE)
                emitEmote(PetEmote.GREET, nowMs)
                lastAutoActionTime = nowMs
            } else if (roll < sleepChance + singChance + greetChance + happyChance) {
                state = state.applyHappyBurst().withBehavior(PetBehavior.CUTE)
                emitEmote(PetEmote.HAPPY, nowMs)
                lastAutoActionTime = nowMs
            } else if (roll < sleepChance + singChance + greetChance + happyChance + thinkChance) {
                emitEmote(PetEmote.THINK, nowMs)
                lastAutoActionTime = nowMs
            }
        }

        // 4. Natural emotion drift (make it feel alive)
        if (nowMs - lastEmotionTime > 60000) {
            lastEmotionTime = nowMs
            if (state.energy > 70 && state.mood < 90) {
                state = state.copy(mood = (state.mood + 2).coerceIn(0, 100))
            } else if (state.energy < 25 && state.mood > 5) {
                state = state.copy(mood = (state.mood - 2).coerceIn(0, 100))
            }
        }

        // 5. Emit emotes on behavior change (sleep/wake)
        if (state.behavior != lastBehavior) {
            if (state.behavior == PetBehavior.SLEEP) {
                emitEmote(PetEmote.SLEEP, nowMs)
            } else if (lastBehavior == PetBehavior.SLEEP) {
                emitEmote(PetEmote.WAKE, nowMs)
            }
            lastBehavior = state.behavior
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
        lastBehavior = newState.behavior
    }

    fun handlePlayMusic(nowMs: Long) {
        if (nowMs - lastInteractionTime < 1000) return
        lastInteractionTime = nowMs
        state = state.applyMusic()
        state = state.withBehavior(PetBehavior.CUTE)
        emitEmote(PetEmote.MUSIC, nowMs)
    }

    fun handleSleepToggle() {
        state = state.applySleepToggle()
    }

    fun handleGreet(nowMs: Long) {
        if (nowMs - lastInteractionTime < 600) return
        lastInteractionTime = nowMs
        state = state.applyGreet()
        state = state.withBehavior(PetBehavior.CUTE)
        emitEmote(PetEmote.GREET, nowMs)
    }

    fun handleSing(nowMs: Long) {
        if (nowMs - lastInteractionTime < 1000) return
        lastInteractionTime = nowMs
        state = state.applySing()
        state = state.withBehavior(PetBehavior.CUTE)
        emitEmote(PetEmote.SING, nowMs)
        if (manifest.id == "dragon") {
            emitEmote(PetEmote.FIRE, nowMs)
        }
    }

    fun handleHappyBurst(nowMs: Long) {
        if (nowMs - lastInteractionTime < 1200) return
        lastInteractionTime = nowMs
        state = state.applyHappyBurst()
        state = state.withBehavior(PetBehavior.CUTE)
        emitEmote(PetEmote.HAPPY, nowMs)
    }
}


