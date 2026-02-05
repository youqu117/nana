package app.data

import kotlinx.coroutines.flow.Flow

class PetRepository(private val petDao: PetDao, private val settingsDao: SettingsDao) {
    val allAssets: Flow<List<PetAssetEntity>> = petDao.getAllAssets()
    val allInstances: Flow<List<PetInstanceEntity>> = petDao.getAllInstances()
    val enabledInstances: Flow<List<PetInstanceEntity>> = petDao.getEnabledInstances()
    val allSettings: Flow<List<GlobalSettingEntity>> = settingsDao.getAllSettings()

    suspend fun insertAsset(asset: PetAssetEntity) {
        petDao.insertAsset(asset)
    }

    suspend fun insertAllAssets(assets: List<PetAssetEntity>) {
        petDao.insertAllAssets(assets)
    }

    suspend fun clearAssets() {
        petDao.clearAssets()
    }

    suspend fun addInstance(assetId: String, name: String): Long {
        val instance = PetInstanceEntity(
            assetId = assetId,
            name = name,
            x = 100,
            y = 200
        )
        return petDao.insertInstance(instance)
    }

    suspend fun updateInstance(instance: PetInstanceEntity) {
        petDao.updateInstance(instance)
    }

    suspend fun deleteInstance(instance: PetInstanceEntity) {
        petDao.deleteInstance(instance)
    }

    suspend fun toggleInstance(instance: PetInstanceEntity) {
        petDao.updateInstance(instance.copy(isEnabled = !instance.isEnabled))
    }

    suspend fun getInstanceById(id: Long): PetInstanceEntity? {
        return petDao.getInstanceById(id)
    }

    fun getSetting(key: String): Flow<String?> {
        return settingsDao.getSetting(key)
    }

    suspend fun setSetting(key: String, value: String) {
        settingsDao.setSetting(GlobalSettingEntity(key, value))
    }
}
