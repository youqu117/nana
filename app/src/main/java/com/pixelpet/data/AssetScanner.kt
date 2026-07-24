package com.pixelpet.data

import android.content.Context
import com.pixelpet.content.AssetLoader
import com.pixelpet.core.LogUtils
import com.pixelpet.core.SettingKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

object AssetScanner {
    private const val TAG = "AssetScanner"

    /** 内置默认宠物，首次启动时自动领养一只，避免新用户面对空状态。 */
    private const val DEFAULT_PET_ID = "qi_qi"

    suspend fun scanAndPopulate(context: Context, repository: PetRepository) {
        withContext(Dispatchers.IO) {
            try {
                val assetPets = context.assets.list("pets")?.toList() ?: emptyList()
                val localPetsDir = java.io.File(context.filesDir, "pets")
                val localPets = if (localPetsDir.exists()) {
                    localPetsDir.list()?.toList() ?: emptyList()
                } else {
                    emptyList()
                }

                // assets 优先于本地同名目录，去重时保留前者。
                val petsList = (assetPets + localPets).distinct()

                val assets = petsList.mapNotNull { folderName ->
                    val manifest = AssetLoader.loadManifest(context, folderName) ?: return@mapNotNull null
                    val hasPreview = AssetLoader.assetExists(context, manifest.preview)
                    val hasNormal = AssetLoader.assetExists(context, manifest.staticNormal)
                    val hasIdleSheet = manifest.idleSheet.isBlank() ||
                        AssetLoader.assetExists(context, manifest.idleSheet)
                    if (!hasPreview || !hasNormal || !hasIdleSheet) {
                        LogUtils.w(
                            TAG,
                            "Skipping ${manifest.id}: missing assets " +
                                "(preview=$hasPreview, normal=$hasNormal, idleSheet=$hasIdleSheet)"
                        )
                        return@mapNotNull null
                    }
                    PetAssetEntity(
                        id = manifest.id,
                        name = manifest.name,
                        previewPath = manifest.preview,
                        defaultScale = manifest.defaultScale,
                        width = manifest.hitbox.w,
                        height = manifest.hitbox.h
                    )
                }

                if (assets.isNotEmpty()) {
                    repository.clearAssets()
                    repository.insertAllAssets(assets)

                    // 首次启动自动领养默认宠物。
                    val hasDefault = repository.getSetting(SettingKeys.DEFAULT_PET_ADOPTED)
                        .firstOrNull()?.toBoolean() ?: false
                    if (!hasDefault && repository.getInstanceCount() == 0) {
                        assets.firstOrNull { it.id == DEFAULT_PET_ID }?.let { defaultPet ->
                            repository.addInstance(defaultPet.id, defaultPet.name)
                            repository.setSetting(SettingKeys.DEFAULT_PET_ADOPTED, "true")
                        }
                    }
                }
            } catch (e: Exception) {
                LogUtils.e(TAG, "scanAndPopulate failed", e)
            }
        }
    }
}
