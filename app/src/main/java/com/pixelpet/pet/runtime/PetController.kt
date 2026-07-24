package com.pixelpet.pet.runtime

import com.pixelpet.audio.SoundManager
import com.pixelpet.pet.view.PetView

/**
 * 连接 [PetRuntime]（状态机）与 [PetView]（渲染）的控制器。
 *
 * OverlayWindowManager 通过本类驱动宠物的手势响应与每帧刷新；
 * 同时把 [PetRuntime] 产生的表情事件（Emote）映射为音效播放。
 *
 * PetDetailActivity 目前直接使用 PetRuntime，后续可统一走本类以保持行为一致。
 */
class PetController(
    private val petView: PetView,
    private val petRuntime: PetRuntime,
    private val soundManager: SoundManager? = null
) {
    /** 上次已消费的表情序列号，用于 [pollAndPlayEmotes]。 */
    private var lastSeenEmoteSeq = 0L

    /** 每帧调用：推进状态机、同步视图、处理新表情并播放对应音效。 */
    fun update() {
        petRuntime.tick(System.currentTimeMillis())
        petView.updateState(petRuntime.state)
        pollAndPlayEmotes()
    }

    fun onTap() {
        petRuntime.handleTap(System.currentTimeMillis())
        update()
        soundManager?.play("tap")
    }

    fun onDoubleTap() {
        petRuntime.handleDoubleTap(System.currentTimeMillis())
        update()
        soundManager?.play("double_tap")
    }

    fun onLongPress() {
        petRuntime.handleLongPressStart()
        update()
    }

    fun onRelease() {
        petRuntime.handleRelease()
        update()
        soundManager?.play("release")
    }

    /** 主动触发“喂食”音效与行为（供小屋/详情页调用）。 */
    fun onFeed() {
        petRuntime.handleFeed()
        update()
        soundManager?.play("feed")
    }

    /** 主动触发“播放音乐”音效与行为。 */
    fun onPlayMusic() {
        petRuntime.handlePlayMusic(System.currentTimeMillis())
        update()
        soundManager?.play("music")
    }

    /** 主动触发“唱歌”音效与行为。 */
    fun onSing() {
        petRuntime.handleSing(System.currentTimeMillis())
        update()
        soundManager?.play("sing")
    }

    /** 轮询 PetRuntime 产生的新表情事件，并驱动对应音效。 */
    private fun pollAndPlayEmotes() {
        while (true) {
            val event = petRuntime.pollEmote(lastSeenEmoteSeq) ?: break
            lastSeenEmoteSeq = event.seq
            soundManager?.playForEmote(event.type.name)
        }
    }
}
