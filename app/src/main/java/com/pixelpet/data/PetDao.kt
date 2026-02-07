package com.pixelpet.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PetDao {
    @Query("SELECT * FROM pet_assets")
    fun getAllAssets(): Flow<List<PetAssetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: PetAssetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAssets(assets: List<PetAssetEntity>)

    @Query("DELETE FROM pet_assets")
    suspend fun clearAssets()

    @Query("SELECT * FROM pet_instances")
    fun getAllInstances(): Flow<List<PetInstanceEntity>>

    @Query("SELECT * FROM pet_instances WHERE isEnabled = 1")
    fun getEnabledInstances(): Flow<List<PetInstanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstance(instance: PetInstanceEntity): Long

    @Update
    suspend fun updateInstance(instance: PetInstanceEntity)

    @Query("UPDATE pet_instances SET energy = :energy, mood = :mood, hunger = :hunger, affection = :affection, lastTickTime = :lastTickTime, x = :x, y = :y WHERE instanceId = :id")
    suspend fun updatePetState(id: Long, energy: Int, mood: Int, hunger: Int, affection: Int, lastTickTime: Long, x: Int, y: Int)

    @Delete
    suspend fun deleteInstance(instance: PetInstanceEntity)

    @Query("SELECT * FROM pet_instances WHERE instanceId = :id")
    suspend fun getInstanceById(id: Long): PetInstanceEntity?

    @Query("SELECT COUNT(*) FROM pet_instances")
    suspend fun getInstanceCount(): Int
}

