package com.example.myapplication.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RoutePointDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(points: List<RoutePoint>)

    @Query("SELECT * FROM route_points WHERE tripId = :tripId ORDER BY timestamp ASC")
    suspend fun getByTripId(tripId: Int): List<RoutePoint>

    @Query("DELETE FROM route_points WHERE tripId = :tripId")
    suspend fun deleteByTripId(tripId: Int)
}

