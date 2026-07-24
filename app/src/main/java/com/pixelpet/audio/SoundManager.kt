package com.pixelpet.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.pixelpet.core.LogUtils

/**
 * 宠物音效管理器，基于 [SoundPool]，适合播放短交互音效。
 *
 * 每个宠物可独立配置一套声音（通过 manifest 的 `sounds` 字段），
 * 在素材加载时一次性预加载，交互时直接触发播放。
 *
 * 生命周期由 [OverlayPetUnit] 管理：创建时 load，销毁时 release。
 */
class SoundManager(context: Context) {

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    /** 声音名 -> SoundPool soundId（>0 表示已加载）。 */
    private val soundIds = mutableMapOf<String, Int>()

    /** 记录哪些 soundId 已经加载完成，避免未就绪时播放。 */
    private val loaded = mutableSetOf<Int>()

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) loaded.add(sampleId)
        }
    }

    /**
     * 预加载宠物配置的声音。
     * @param sounds map<事件名, 相对路径>，路径相对于宠物包根目录。
     */
    fun load(context: Context, sounds: Map<String, String>) {
        releaseAll()
        sounds.forEach { (name, path) ->
            val id = loadSingle(context, path)
            if (id > 0) soundIds[name] = id
        }
    }

    private fun loadSingle(context: Context, path: String): Int {
        // 1) 尝试 assets（效率最高，直接得到 AssetFileDescriptor）
        try {
            context.assets.openFd(path).use { afd ->
                return soundPool.load(afd, 1)
            }
        } catch (_: Exception) {
            // fall through
        }
        // 2) 尝试 filesDir（用户导入的宠物包）
        try {
            val file = java.io.File(context.filesDir, path)
            if (file.exists()) {
                return soundPool.load(file.absolutePath, 1)
            }
        } catch (_: Exception) {
            // fall through
        }
        LogUtils.w(TAG, "Sound not found: $path")
        return 0
    }

    /** 播放指定事件名的声音（若未加载或不存在则静默跳过）。 */
    fun play(event: String) {
        val soundId = soundIds[event] ?: return
        if (!loaded.contains(soundId)) return
        soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
    }

    /** 播放与 [com.pixelpet.pet.interaction.PetEmote] 对应的声音（按小写名称匹配）。 */
    fun playForEmote(emoteName: String) {
        play(emoteName.lowercase())
    }

    /** 释放所有已加载的声音并清空状态。 */
    fun releaseAll() {
        soundIds.values.forEach { soundPool.unload(it) }
        soundIds.clear()
        loaded.clear()
    }

    /** 彻底释放 SoundPool（通常在宠物销毁时调用一次）。 */
    fun release() {
        releaseAll()
        soundPool.release()
    }

    companion object {
        private const val TAG = "SoundManager"
    }
}
