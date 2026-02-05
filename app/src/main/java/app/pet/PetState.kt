package app.pet

data class PetState(
    val energy: Int = 80,
    val mood: Int = 0,
    val affection: Int = 0,
    val lastTickMs: Long = 0L
) {
    fun tick(nowMs: Long): PetState {
        if (lastTickMs == 0L) return copy(lastTickMs = nowMs)
        val elapsedSeconds = ((nowMs - lastTickMs) / 1000).coerceAtLeast(0)
        if (elapsedSeconds == 0L) return this
        val energyLoss = (elapsedSeconds / ENERGY_DECAY_SECONDS).toInt()
        val moodRecovery = (elapsedSeconds / MOOD_RECOVER_SECONDS).toInt()
        return copy(
            energy = (energy - energyLoss).coerceIn(0, 100),
            mood = (mood - moodRecovery).coerceIn(-100, 100),
            lastTickMs = nowMs
        )
    }

    fun applyTap(): PetState = copy(
        affection = (affection + 1).coerceIn(0, 100),
        mood = (mood + 1).coerceIn(-100, 100)
    )

    companion object {
        private const val ENERGY_DECAY_SECONDS = 600
        private const val MOOD_RECOVER_SECONDS = 900
    }
}
