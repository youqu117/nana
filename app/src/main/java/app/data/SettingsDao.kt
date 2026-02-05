package app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM global_settings")
    fun getAllSettings(): Flow<List<GlobalSettingEntity>>

    @Query("SELECT value FROM global_settings WHERE `key` = :key")
    fun getSetting(key: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: GlobalSettingEntity)
}
