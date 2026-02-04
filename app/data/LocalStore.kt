package app.data

import android.content.Context
import org.json.JSONObject
import java.io.File

class LocalStore(private val context: Context) {
    fun loadState(): PetSaveState? {
        val file = saveFile()
        if (!file.exists()) return null
        val json = JSONObject(file.readText())
        if (json.optInt(KEY_VERSION) != CURRENT_VERSION) return null
        return PetSaveState(
            energy = json.optInt(KEY_ENERGY, DEFAULT_ENERGY),
            mood = json.optInt(KEY_MOOD, DEFAULT_MOOD),
            affection = json.optInt(KEY_AFFECTION, DEFAULT_AFFECTION),
            adoptedAt = json.optLong(KEY_ADOPTED_AT, 0L),
            posX = json.optInt(KEY_POS_X, 0),
            posY = json.optInt(KEY_POS_Y, 0),
            skinId = json.optString(KEY_SKIN_ID, "")
        )
    }

    fun saveState(state: PetSaveState) {
        val json = JSONObject()
            .put(KEY_VERSION, CURRENT_VERSION)
            .put(KEY_ENERGY, state.energy)
            .put(KEY_MOOD, state.mood)
            .put(KEY_AFFECTION, state.affection)
            .put(KEY_ADOPTED_AT, state.adoptedAt)
            .put(KEY_POS_X, state.posX)
            .put(KEY_POS_Y, state.posY)
            .put(KEY_SKIN_ID, state.skinId)
        saveFile().writeText(json.toString())
    }

    private fun saveFile(): File = File(context.filesDir, FILE_NAME)

    companion object {
        private const val FILE_NAME = "pet_state.json"
        private const val CURRENT_VERSION = 1
        private const val DEFAULT_ENERGY = 80
        private const val DEFAULT_MOOD = 0
        private const val DEFAULT_AFFECTION = 0
        private const val KEY_VERSION = "version"
        private const val KEY_ENERGY = "energy"
        private const val KEY_MOOD = "mood"
        private const val KEY_AFFECTION = "affection"
        private const val KEY_ADOPTED_AT = "adoptedAt"
        private const val KEY_POS_X = "posX"
        private const val KEY_POS_Y = "posY"
        private const val KEY_SKIN_ID = "skinId"
    }
}

data class PetSaveState(
    val energy: Int,
    val mood: Int,
    val affection: Int,
    val adoptedAt: Long,
    val posX: Int,
    val posY: Int,
    val skinId: String
)
