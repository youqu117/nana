package app.pet

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
    val affection: Int = 0,
    val lastTickMs: Long = 0L,
    
    // Behavior State
    val behavior: PetBehavior = PetBehavior.IDLE,
    val behaviorStartTime: Long = System.currentTimeMillis()
) {
    fun tick(nowMs: Long): PetState {
        if (lastTickMs == 0L) return copy(lastTickMs = nowMs)
        
        val elapsed = nowMs - lastTickMs
        // Check for 10 minute interval (600,000 ms)
        val minutesPassed = elapsed / (1000 * 60)
        
        if (minutesPassed < 1) return this // Optimization: don't update too often
        
        // Energy Logic
        // Decay 1 every 10 mins if not sleeping
        // Recover 3 every 10 mins if sleeping
        val tenMinIntervals = minutesPassed / 10
        var newEnergy = energy
        var newMood = mood
        
        if (tenMinIntervals >= 1) {
            val change = if (behavior == PetBehavior.SLEEP) 3 else -1
            newEnergy = (energy + (change * tenMinIntervals)).toInt().coerceIn(0, 100)
        }

        // Mood Logic (based on Energy)
        // Energy > 60: +1 per 30 mins
        // Energy < 20: -2 per 30 mins
        val thirtyMinIntervals = minutesPassed / 30
        if (thirtyMinIntervals >= 1) {
            if (newEnergy > 60) {
                newMood = (newMood + (1 * thirtyMinIntervals)).toInt().coerceAtMost(100) // Cap at +100
            } else if (newEnergy < 20) {
                newMood = (newMood - (2 * thirtyMinIntervals)).toInt().coerceAtLeast(-100) // Min -100
            }
        }
        
        return copy(
            energy = newEnergy,
            mood = newMood,
            lastTickMs = nowMs
        )
    }

    fun withBehavior(newBehavior: PetBehavior): PetState {
        if (behavior == newBehavior) return this
        return copy(
            behavior = newBehavior,
            behaviorStartTime = System.currentTimeMillis()
        )
    }

    fun applyTap(): PetState = copy(
        affection = (affection + 1).coerceIn(0, 100),
        mood = (mood + 2).coerceIn(-100, 100)
    )
    
    fun applyDoubleTap(): PetState = copy(
        mood = (mood + 5).coerceIn(-100, 100)
    )
}

