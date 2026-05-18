package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val plateNumber: String,
    val fuelType: String,
    val currentOdometer: Double = 0.0,
    val lastServiceOdo: Double = 0.0,
    val serviceIntervalKm: Double = 5000.0,
    val manualEfficiency: Double = 12.0, // Angka manual dari user/pabrikan
    val calculatedEfficiency: Double? = null, // Hasil hitungan sistem Full-to-Full
    val tankCapacity: Double = 40.0 // Kapasitas tangki bensin dalam Liter
)
