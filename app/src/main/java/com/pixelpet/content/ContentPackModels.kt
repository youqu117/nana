package com.pixelpet.content

/**
 * 内容包清单。
 *
 * [singEmote] 用于让特定宠物在唱歌时附带额外表情（如龙喷火），
 * 取值为 PetEmote 名称字符串（如 "fire"），为空表示无额外表情。
 * 这样避免在 PetRuntime 中硬编码 `if (manifest.id == "dragon")`。
 *
 * [sounds] 为事件名到音频文件路径的映射，例如 {"tap":"sounds/tap.ogg"}，
 * 由 [com.pixelpet.audio.SoundManager] 在加载宠物素材时统一预加载。
 */
data class ContentPackManifest(
    val id: String,
    val name: String,
    val version: Int,
    val preview: String,
    val staticNormal: String,
    val staticTongue: String,
    val idleSheet: String,
    val idleAnim: String,
    val defaultScale: Int,
    val hitbox: Hitbox,
    val anchors: Anchors,
    val singEmote: String? = null,
    val sounds: Map<String, String> = emptyMap()
)

data class Hitbox(
    val x: Int,
    val y: Int,
    val w: Int,
    val h: Int
)

data class Anchors(
    val rootX: Int,
    val rootY: Int,
    val headX: Int,
    val headY: Int,
    val faceX: Int,
    val faceY: Int
)


