package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "costs")
data class Cost(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: Long,
    val vehicleId: Int,
    val type: String, // "Bensin", "Servis", "Perbaikan", "Lainnya"
    val amount: Double,
    val quantity: Double? = null, // Liter
    val odometer: Double? = null,
    val isFullTank: Boolean = false, // Flag untuk metode Full-to-Full
    val notes: String? = null
)
