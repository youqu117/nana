package com.pixelpet.pet.interaction

enum class PetEmote {
    MUSIC,
    SLEEP,
    WAKE,
    GREET,
    SING,
    HAPPY,
    FIRE,
    THINK
}

data class PetEmoteEvent(
    val type: PetEmote,
    val seq: Long,
    val atMs: Long
)
