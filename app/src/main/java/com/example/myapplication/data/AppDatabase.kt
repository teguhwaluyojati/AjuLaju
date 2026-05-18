package com.example.myapplication.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Trip::class, Vehicle::class, Cost::class, RoutePoint::class], version = 9, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun tripDao(): TripDao
    abstract fun vehicleDao(): VehicleDao
    abstract fun costDao(): CostDao
    abstract fun routePointDao(): RoutePointDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `trips_new` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`date` INTEGER NOT NULL, " +
                        "`vehicleId` INTEGER NOT NULL DEFAULT 0, " +
                        "`startKm` REAL NOT NULL, " +
                        "`endKm` REAL NOT NULL, " +
                        "`destination` TEXT NOT NULL, " +
                        "`durationMillis` INTEGER NOT NULL, " +
                        "`weightedDistance` REAL, " +
                        "`notes` TEXT)"
                )
                db.execSQL(
                    "INSERT INTO `trips_new` (id, date, vehicleId, startKm, endKm, destination, durationMillis, weightedDistance, notes) " +
                        "SELECT t.id, t.date, COALESCE(v.id, 0), t.startKm, t.endKm, t.destination, t.durationMillis, t.weightedDistance, t.notes " +
                        "FROM trips t LEFT JOIN vehicles v ON v.name = t.vehicleName"
                )
                db.execSQL("DROP TABLE trips")
                db.execSQL("ALTER TABLE trips_new RENAME TO trips")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `costs_new` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`date` INTEGER NOT NULL, " +
                        "`vehicleId` INTEGER NOT NULL DEFAULT 0, " +
                        "`type` TEXT NOT NULL, " +
                        "`amount` REAL NOT NULL, " +
                        "`quantity` REAL, " +
                        "`odometer` REAL, " +
                        "`isFullTank` INTEGER NOT NULL, " +
                        "`notes` TEXT)"
                )
                db.execSQL(
                    "INSERT INTO `costs_new` (id, date, vehicleId, type, amount, quantity, odometer, isFullTank, notes) " +
                        "SELECT c.id, c.date, COALESCE(v.id, 0), c.type, c.amount, c.quantity, c.odometer, c.isFullTank, c.notes " +
                        "FROM costs c LEFT JOIN vehicles v ON v.name = c.vehicleName"
                )
                db.execSQL("DROP TABLE costs")
                db.execSQL("ALTER TABLE costs_new RENAME TO costs")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vehicle_tracker_db"
                )
                    .addMigrations(MIGRATION_7_8)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
