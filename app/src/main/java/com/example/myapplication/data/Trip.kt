package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: Long,
    val vehicleId: Int,
    val startKm: Double,
    val endKm: Double,
    val destination: String,
    val durationMillis: Long = 0, // Tambahan untuk waktu tempuh
    val weightedDistance: Double? = null, // Jarak yang disesuaikan beban mesin
    val notes: String? = null
)
