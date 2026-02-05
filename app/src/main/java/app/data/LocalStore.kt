package app.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- Entities ---

@Entity(tableName = "pet_assets")
data class PetAssetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val previewPath: String, // Path to preview image in assets
    val defaultScale: Int,
    val width: Int,
    val height: Int
)

@Entity(tableName = "pet_instances")
data class PetInstanceEntity(
    @PrimaryKey(autoGenerate = true) val instanceId: Long = 0,
    val assetId: String,
    val name: String,
    val scale: Float = 1.0f,
    val alpha: Float = 1.0f,
    val isEnabled: Boolean = true,
    val x: Int = 0,
    val y: Int = 0,
    val zOrder: Int = 0,
    // New MVP Stats
    val adoptionTime: Long = System.currentTimeMillis(),
    val energy: Int = 80,
    val mood: Int = 0,
    val affection: Int = 0,
    // Offline Calculation
    val lastTickTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "global_settings")
data class GlobalSettingEntity(
    @PrimaryKey val key: String,
    val value: String
)

// --- DAOs ---

@Dao
interface PetDao {
    // Assets
    @Query("SELECT * FROM pet_assets")
    fun getAllAssets(): Flow<List<PetAssetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: PetAssetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAssets(assets: List<PetAssetEntity>)

    @Query("DELETE FROM pet_assets")
    suspend fun clearAssets()

    // Instances
    @Query("SELECT * FROM pet_instances")
    fun getAllInstances(): Flow<List<PetInstanceEntity>>

    @Query("SELECT * FROM pet_instances WHERE isEnabled = 1")
    fun getEnabledInstances(): Flow<List<PetInstanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstance(instance: PetInstanceEntity): Long

    @Update
    suspend fun updateInstance(instance: PetInstanceEntity)

    @Delete
    suspend fun deleteInstance(instance: PetInstanceEntity)

    @Query("SELECT * FROM pet_instances WHERE instanceId = :id")
    suspend fun getInstanceById(id: Long): PetInstanceEntity?
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM global_settings")
    fun getAllSettings(): Flow<List<GlobalSettingEntity>>

    @Query("SELECT value FROM global_settings WHERE `key` = :key")
    fun getSetting(key: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: GlobalSettingEntity)
}

// --- Database ---

@Database(entities = [PetAssetEntity::class, PetInstanceEntity::class, GlobalSettingEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun petDao(): PetDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pixel_pet_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// --- Repository ---

class PetRepository(private val petDao: PetDao, private val settingsDao: SettingsDao) {

    // Pet Assets
    val allAssets: Flow<List<PetAssetEntity>> = petDao.getAllAssets()

    suspend fun insertAsset(asset: PetAssetEntity) {
        petDao.insertAsset(asset)
    }

    suspend fun insertAllAssets(assets: List<PetAssetEntity>) {
        petDao.insertAllAssets(assets)
    }

    suspend fun clearAssets() {
        petDao.clearAssets()
    }

    // Pet Instances
    val allInstances: Flow<List<PetInstanceEntity>> = petDao.getAllInstances()
    val enabledInstances: Flow<List<PetInstanceEntity>> = petDao.getEnabledInstances()

    suspend fun addInstance(assetId: String, name: String): Long {
        val instance = PetInstanceEntity(
            assetId = assetId,
            name = name,
            x = 100, // Default position
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

    // Global Settings
    val allSettings: Flow<List<GlobalSettingEntity>> = settingsDao.getAllSettings()

    fun getSetting(key: String): Flow<String?> {
        return settingsDao.getSetting(key)
    }

    suspend fun setSetting(key: String, value: String) {
        settingsDao.setSetting(GlobalSettingEntity(key, value))
    }
}