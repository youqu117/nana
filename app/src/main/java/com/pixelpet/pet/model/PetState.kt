package com.pixelpet.pet.model

enum class PetBehavior {
    IDLE,
    WALK,
    RUN,
    SPIN,
    CUTE,
    SLEEP,
    HELD,
    DOCKED,
    HIDDEN
}

data class PetState(
    val energy: Int = 80,
    val mood: Int = 0,
    val hunger: Int = 80,
    val affection: Int = 0,
    val lastTickMs: Long = 0L,
    
    // Behavior State
    val behavior: PetBehavior = PetBehavior.IDLE,
    val behaviorStartTime: Long = System.currentTimeMillis()
) {
    fun tick(nowMs: Long, isResting: Boolean = false): PetState {
        if (lastTickMs == 0L) return copy(lastTickMs = nowMs)
        
        val elapsed = nowMs - lastTickMs
        // Check for 1 minute interval (60,000 ms) for finer granularity
        val minutesPassed = elapsed / (1000 * 60)
        
        if (minutesPassed < 1) return this 
        
        var newEnergy: Int
        var newMood = mood
        var newHunger: Int
        var nextBehavior = behavior // Default to current behavior
        
        val intervals = minutesPassed.toInt()

        if (isResting) {
            // Resting (Home): Recover Energy, Slow Hunger Decay
            // +1 Energy per 2 mins
            newEnergy = (energy + (intervals / 2)).coerceIn(0, 100)
            // -1 Hunger per 20 mins
            newHunger = (hunger - (intervals / 20)).coerceIn(0, 100)
            // Mood stable or slight increase
        } else {
            // Active: Consume Energy, Hunger Decay
            // -1 Energy per 5 mins
            if (behavior == PetBehavior.SLEEP) {
                newEnergy = (energy + (intervals / 5)).coerceIn(0, 100)
            } else {
                newEnergy = (energy - (intervals / 5)).coerceIn(0, 100)
            }
            
            // -1 Hunger per 10 mins
            newHunger = (hunger - (intervals / 10)).coerceIn(0, 100)
            
            // Auto-Sleep if Energy is critically low (< 20)
            if (newEnergy < 20 && behavior != PetBehavior.SLEEP && behavior != PetBehavior.HELD) {
                 nextBehavior = PetBehavior.SLEEP
            } else if (newEnergy > 80 && behavior == PetBehavior.SLEEP) {
                 // Wake up if recovered enough
                 nextBehavior = PetBehavior.IDLE
            }
            
            // Mood Decay if ignored or needs neglected
            // If Hunger < 30 or Energy < 20, Mood -1 per 5 mins
            if (newHunger < 30 || newEnergy < 20) {
                newMood = (mood - (intervals / 5)).coerceAtLeast(0)
            }
        }
        
        return copy(
            energy = newEnergy,
            mood = newMood,
            hunger = newHunger,
            behavior = if (!isResting && behavior != PetBehavior.HELD) nextBehavior else behavior,
            lastTickMs = nowMs
        )
    }

    fun applyTap(): PetState {
        // Mood + 5, Energy -1
        return copy(
            mood = (mood + 5).coerceIn(0, 100),
            energy = (energy - 1).coerceIn(0, 100),
            affection = (affection + 1).coerceIn(0, 100)
        )
    }
    
    fun applyFeed(): PetState {
        // Hunger + 20, Mood + 5
        return copy(
            hunger = (hunger + 20).coerceIn(0, 100),
            mood = (mood + 5).coerceIn(0, 100)
        )
    }

    fun applyDoubleTap(): PetState {
        return copy(
            mood = (mood + 10).coerceIn(0, 100)
        )
    }

    fun applyMusic(): PetState {
        return copy(
            mood = (mood + 8).coerceIn(0, 100),
            affection = (affection + 2).coerceIn(0, 100),
            energy = (energy - 1).coerceIn(0, 100)
        )
    }

    fun applySleepToggle(): PetState {
        return if (behavior == PetBehavior.SLEEP) {
            withBehavior(PetBehavior.IDLE)
        } else {
            withBehavior(PetBehavior.SLEEP)
        }
    }

    fun applyGreet(): PetState {
        return copy(
            mood = (mood + 4).coerceIn(0, 100),
            affection = (affection + 2).coerceIn(0, 100)
        )
    }

    fun applySing(): PetState {
        return copy(
            mood = (mood + 6).coerceIn(0, 100),
            affection = (affection + 1).coerceIn(0, 100),
            energy = (energy - 2).coerceIn(0, 100)
        )
    }

    fun applyHappyBurst(): PetState {
        return copy(
            mood = (mood + 10).coerceIn(0, 100),
            affection = (affection + 3).coerceIn(0, 100)
        )
    }

    fun withBehavior(newBehavior: PetBehavior): PetState {
        return copy(
            behavior = newBehavior,
            behaviorStartTime = System.currentTimeMillis()
        )
    }
}

