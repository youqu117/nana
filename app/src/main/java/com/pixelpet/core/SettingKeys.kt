package com.pixelpet.core

/**
 * 集中管理所有持久化设置的 key，避免字符串散落在各处导致拼写不一致。
 */
object SettingKeys {
    // 桌宠外观与行为
    const val SCALE = "scale"
    const val ALPHA = "alpha"
    const val VERTICAL_MOVE = "vertical_move"
    const val DRIFT_ENABLED = "drift_enabled"
    const val DRIFT_SPEED = "drift_speed"

    // 业务标记
    const val DEFAULT_PET_ADOPTED = "default_pet_adopted"
    const val PERMISSION_PROMPTED_ONCE = "permission_prompted_once"
}
