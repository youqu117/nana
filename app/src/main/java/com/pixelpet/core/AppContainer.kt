package com.pixelpet.core

import android.content.Context
import com.pixelpet.data.AppDatabase
import com.pixelpet.data.PetRepository

/**
 * 简单的依赖容器，替代各处自行 `AppDatabase.getDatabase + PetRepository(...)` 构造。
 *
 * 不引入 Hilt/Koin 等框架，保持零额外依赖；提供全局单例 Repository。
 * 所有页面统一通过 [get] 获取，避免 Fragment 强转 Activity 的反模式。
 */
object AppContainer {
    @Volatile
    private var repository: PetRepository? = null

    fun get(context: Context): PetRepository {
        return repository ?: synchronized(this) {
            repository ?: run {
                val db = AppDatabase.getDatabase(context.applicationContext)
                PetRepository(db.petDao(), db.settingsDao()).also { repository = it }
            }
        }
    }
}
