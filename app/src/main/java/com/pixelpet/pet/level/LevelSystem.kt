package com.pixelpet.pet.level

import com.pixelpet.data.PetInstanceEntity

object LevelSystem {
    data class LevelInfo(
        val level: Int,
        val xp: Int,
        val currentLevelXp: Int,
        val nextLevelXp: Int,
        val title: String
    ) {
        val progressPercent: Int
            get() {
                val span = (nextLevelXp - currentLevelXp).coerceAtLeast(1)
                return ((xp - currentLevelXp) * 100 / span).coerceIn(0, 100)
            }
    }

    private const val XP_PER_LEVEL = 140
    private const val MAX_LEVEL = 50

    fun fromPet(instance: PetInstanceEntity, nowMs: Long = System.currentTimeMillis()): LevelInfo {
        val ageMinutes = ((nowMs - instance.adoptionTime).coerceAtLeast(0L) / (60L * 1000L)).toInt()
        // XP design:
        // - time-based slow growth (1 xp per 30 min)
        // - affection is the strongest long-term signal
        // - mood/energy/hunger reflect daily care quality
        val timeXp = ageMinutes / 30
        val xpRaw = (instance.affection * 12) +
            (instance.mood * 4) +
            (instance.energy * 3) +
            (instance.hunger * 2) +
            timeXp
        val xp = if (ageMinutes == 0 && instance.affection == 0 && instance.mood == 0) {
            0
        } else {
            xpRaw.coerceAtLeast(0)
        }

        val computedLevel = (xp / XP_PER_LEVEL) + 1
        val level = computedLevel.coerceIn(1, MAX_LEVEL)
        val currentLevelXp = (level - 1) * XP_PER_LEVEL
        val nextLevelXp = if (level >= MAX_LEVEL) currentLevelXp + 1 else level * XP_PER_LEVEL

        return LevelInfo(
            level = level,
            xp = xp,
            currentLevelXp = currentLevelXp,
            nextLevelXp = nextLevelXp,
            title = levelTitle(level)
        )
    }

    private fun levelTitle(level: Int): String {
        return when {
            level >= 40 -> "传奇守护者"
            level >= 25 -> "资深训练师"
            level >= 15 -> "冒险伙伴"
            level >= 8 -> "可信搭档"
            else -> "新手饲养员"
        }
    }
}
