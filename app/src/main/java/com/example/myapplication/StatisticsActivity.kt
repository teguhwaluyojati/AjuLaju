package com.example.myapplication

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.asLiveData
import com.example.myapplication.data.AppDatabase
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.color.MaterialColors
import java.util.Locale

class StatisticsActivity : AppCompatActivity() {

    private val db by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_statistics)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val pieChart = findViewById<PieChart>(R.id.pieChart)
        val barChart = findViewById<BarChart>(R.id.barChart)
        val textSummaryPerVehicle = findViewById<TextView>(R.id.textSummaryPerVehicle)

        val surface = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface, Color.WHITE)
        val onSurface = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, Color.BLACK)
        val onSurfaceVariant = MaterialColors.getColor(
            this,
            com.google.android.material.R.attr.colorOnSurfaceVariant,
            onSurface
        )
        val primary = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary, onSurface)
        val secondary = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSecondary, primary)
        val tertiary = MaterialColors.getColor(this, com.google.android.material.R.attr.colorTertiary, secondary)
        val primaryContainer = MaterialColors.getColor(
            this,
            com.google.android.material.R.attr.colorPrimaryContainer,
            primary
        )
        val secondaryContainer = MaterialColors.getColor(
            this,
            com.google.android.material.R.attr.colorSecondaryContainer,
            secondary
        )
        val tertiaryContainer = MaterialColors.getColor(
            this,
            com.google.android.material.R.attr.colorTertiaryContainer,
            tertiary
        )

        pieChart.setHoleColor(surface)
        pieChart.setEntryLabelColor(onSurface)
        pieChart.setEntryLabelTextSize(12f)
        pieChart.setCenterTextColor(onSurface)
        pieChart.legend.textColor = onSurface
        pieChart.setNoDataTextColor(onSurfaceVariant)

        barChart.legend.textColor = onSurface
        barChart.xAxis.textColor = onSurface
        barChart.axisLeft.textColor = onSurface
        barChart.axisRight.textColor = onSurface
        barChart.axisLeft.gridColor = onSurfaceVariant
        barChart.axisRight.gridColor = onSurfaceVariant
        barChart.xAxis.gridColor = onSurfaceVariant
        barChart.setNoDataTextColor(onSurfaceVariant)

        var latestTrips: List<com.example.myapplication.data.TripWithVehicle> = emptyList()
        var latestVehicles: List<com.example.myapplication.data.Vehicle> = emptyList()

        fun updateSummary() {
            if (latestTrips.isEmpty() || latestVehicles.isEmpty()) {
                textSummaryPerVehicle.text = "Belum ada data perjalanan."
                return
            }

            val grouped = latestTrips.groupBy { it.vehicleId }
            val lines = grouped.map { (vehicleId, trips) ->
                val distance = trips.sumOf { it.endKm - it.startKm }
                val weightedDistanceSum = trips.sumOf { it.weightedDistance ?: (it.endKm - it.startKm) }
                val vehicle = latestVehicles.find { it.id == vehicleId }
                val vehicleName = vehicle?.name ?: "-"
                val eff = vehicle?.calculatedEfficiency ?: vehicle?.manualEfficiency ?: 12.0
                val fuelUsed = if (eff > 0) weightedDistanceSum / eff else 0.0
                String.format(
                    Locale.getDefault(),
                    "• %s: %.1f km, %.2f L",
                    vehicleName,
                    distance,
                    fuelUsed
                )
            }

            textSummaryPerVehicle.text = lines.joinToString("\n")
        }

        // Observe Costs for Pie Chart
        db.costDao().getAllCosts().asLiveData().observe(this) { costs ->
            if (costs.isNullOrEmpty()) return@observe
            
            val typeMap = costs.groupBy { it.type }.mapValues { entry -> entry.value.sumOf { it.amount } }
            val entries = typeMap.map { PieEntry(it.value.toFloat(), it.key) }
            
            val dataSet = PieDataSet(entries, "Kategori Biaya")
            dataSet.colors = listOf(
                primary,
                secondary,
                tertiary,
                primaryContainer,
                secondaryContainer,
                tertiaryContainer
            )
            dataSet.valueTextColor = onSurface
            dataSet.valueTextSize = 14f
            
            pieChart.data = PieData(dataSet)
            pieChart.description.isEnabled = false
            pieChart.centerText = "Biaya"
            pieChart.animateY(1000)
            pieChart.invalidate()
        }

        // Observe Trips for Bar Chart
        db.tripDao().getAllTrips().asLiveData().observe(this) { trips ->
            if (trips.isNullOrEmpty()) return@observe
            
            // Ambil 7 trip terakhir
            val lastTrips = trips.take(7).reversed()
            val entries = lastTrips.mapIndexed { index, trip -> 
                BarEntry(index.toFloat(), (trip.endKm - trip.startKm).toFloat()) 
            }
            
            val dataSet = BarDataSet(entries, "Jarak (km)")
            dataSet.color = primary
            dataSet.valueTextColor = onSurface

            barChart.data = BarData(dataSet)
            barChart.description.isEnabled = false
            barChart.animateY(1000)
            barChart.invalidate()

            latestTrips = trips
            updateSummary()
        }

        db.vehicleDao().getAllVehicles().asLiveData().observe(this) { vehicles ->
            latestVehicles = vehicles
            updateSummary()
        }
    }
}
