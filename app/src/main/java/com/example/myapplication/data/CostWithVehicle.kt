package com.example.myapplication.data

data class CostWithVehicle(
    val id: Int,
    val date: Long,
    val vehicleId: Int,
    val vehicleName: String,
    val type: String,
    val amount: Double,
    val quantity: Double?,
    val odometer: Double?,
    val isFullTank: Boolean,
    val notes: String?
)

