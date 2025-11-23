package com.example.dogregistration.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDog(dog: DogProfile): Long

    @Update
    suspend fun updateDog(dog: DogProfile)

    @Query("DELETE FROM dog_profiles WHERE id = :id")
    suspend fun deleteDogById(id: Long): Int

    @Query("SELECT * FROM dog_profiles")
    suspend fun getAllDogProfiles(): List<DogProfile>

    @Query("SELECT * FROM dog_profiles ORDER BY id DESC")
    fun getAllDogProfilesFlow(): Flow<List<DogProfile>>

    @Query("SELECT * FROM dog_profiles WHERE id = :id LIMIT 1")
    suspend fun getDogById(id: Long): DogProfile?

    @Query("SELECT COUNT(*) FROM dog_profiles")
    suspend fun count(): Int
}
