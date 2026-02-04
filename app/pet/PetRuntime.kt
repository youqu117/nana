package app.pet

import app.content.ContentPackManifest

class PetRuntime(
    val manifest: ContentPackManifest,
    val handles: PetTextureHandles,
    initialState: PetState = PetState()
) {
    var state: PetState = initialState
        private set

    fun tick(nowMs: Long) {
        state = state.tick(nowMs)
    }

    fun applyTap() {
        state = state.applyTap()
    }
}

data class PetTextureHandles(
    val normal: Int,
    val tongue: Int
)
