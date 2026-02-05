package app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pet_assets")
data class PetAssetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val previewPath: String,
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
    val adoptionTime: Long = System.currentTimeMillis(),
    val energy: Int = 80,
    val mood: Int = 0,
    val affection: Int = 0,
    val lastTickTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "global_settings")
data class GlobalSettingEntity(
    @PrimaryKey val key: String,
    val value: String
)
