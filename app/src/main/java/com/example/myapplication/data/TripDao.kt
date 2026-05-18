package com.example.myapplication.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trip: Trip): Long

    @Update
    suspend fun update(trip: Trip)

    @Delete
    suspend fun delete(trip: Trip)

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT trips.*, COALESCE(vehicles.name, '-') AS vehicleName FROM trips LEFT JOIN vehicles ON trips.vehicleId = vehicles.id ORDER BY trips.date DESC")
    fun getAllTrips(): Flow<List<TripWithVehicle>>

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun getTripById(id: Int): Trip?

    @Query("SELECT MAX(endKm) FROM trips WHERE vehicleId = :vehicleId")
    suspend fun getMaxEndKmForVehicle(vehicleId: Int): Double?

    @Query("SELECT * FROM trips WHERE vehicleId = :vId ORDER BY date DESC")
    fun getAllTripsByVehicle(vId: Int): Flow<List<Trip>>
}
