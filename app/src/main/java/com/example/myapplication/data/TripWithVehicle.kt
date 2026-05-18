package com.example.myapplication.data

data class TripWithVehicle(
    val id: Int,
    val date: Long,
    val vehicleId: Int,
    val vehicleName: String,
    val startKm: Double,
    val endKm: Double,
    val destination: String,
    val durationMillis: Long,
    val weightedDistance: Double?,
    val notes: String?
)

