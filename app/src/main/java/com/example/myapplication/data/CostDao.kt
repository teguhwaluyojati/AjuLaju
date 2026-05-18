package com.example.myapplication.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CostDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cost: Cost)

    @Update
    suspend fun update(cost: Cost)

    @Delete
    suspend fun delete(cost: Cost)

    @Query("DELETE FROM costs WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT * FROM costs ORDER BY date DESC")
    fun getAllCosts(): Flow<List<Cost>>

    @Query("SELECT costs.*, COALESCE(vehicles.name, '-') AS vehicleName FROM costs LEFT JOIN vehicles ON costs.vehicleId = vehicles.id ORDER BY costs.date DESC")
    fun getAllCostsWithVehicle(): Flow<List<CostWithVehicle>>

    @Query("SELECT * FROM costs WHERE id = :id LIMIT 1")
    suspend fun getCostById(id: Int): Cost?

    @Query("SELECT SUM(amount) FROM costs")
    fun getTotalAmount(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM costs WHERE vehicleId = :vehicleId")
    fun getTotalAmountByVehicle(vehicleId: Int): Flow<Double?>

    @Query("SELECT SUM(quantity) FROM costs WHERE vehicleId = :vId AND (type LIKE 'Bensin' OR type LIKE 'bensin')")
    fun getTotalFuelQuantityByVehicle(vId: Int): Flow<Double?>

    @Query("SELECT * FROM costs WHERE vehicleId = :vId AND (type LIKE 'Bensin' OR type LIKE 'bensin') AND isFullTank = 1 AND odometer < :currentOdo ORDER BY odometer DESC LIMIT 1")
    suspend fun getLastFullTank(vId: Int, currentOdo: Double): Cost?
}
