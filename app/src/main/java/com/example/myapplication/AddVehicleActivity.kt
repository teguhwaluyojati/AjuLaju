package com.example.myapplication

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.Vehicle
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.util.Locale

class AddVehicleActivity : AppCompatActivity() {

    private var vehicleId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_vehicle)

        vehicleId = intent.getIntExtra("VEHICLE_ID", -1)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        if (vehicleId != -1) {
            supportActionBar?.title = "Edit Kendaraan"
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val editName = findViewById<TextInputEditText>(R.id.editVehicleName)
        val editPlate = findViewById<TextInputEditText>(R.id.editPlateNumber)
        val editOdo = findViewById<TextInputEditText>(R.id.editInitialOdo)
        val editServiceInterval = findViewById<TextInputEditText>(R.id.editServiceInterval)
        val editFuelEff = findViewById<TextInputEditText>(R.id.editFuelEfficiency)
        val editTankCapacity = findViewById<TextInputEditText>(R.id.editTankCapacity)
        val spinnerFuel = findViewById<AutoCompleteTextView>(R.id.spinnerFuelType)
        val btnSave = findViewById<MaterialButton>(R.id.btnSaveVehicle)

        if (vehicleId != -1) {
            btnSave.text = "Update Kendaraan"
        }

        // Setup Fuel Type Spinner
        val fuelTypes = arrayOf("Pertalite", "Pertamax", "Pertamax Turbo", "Dexlite", "Pertamina Dex", "Listrik")
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, fuelTypes)
        spinnerFuel.setAdapter(adapter)

        // Load data if editing
        if (vehicleId != -1) {
            lifecycleScope.launch {
                val vehicle = AppDatabase.getDatabase(this@AddVehicleActivity).vehicleDao().getVehicleById(vehicleId)
                vehicle?.let {
                    editName.setText(it.name)
                    editPlate.setText(it.plateNumber)
                    editOdo.setText(it.currentOdometer.toString())
                    editServiceInterval.setText(it.serviceIntervalKm.toInt().toString())
                    
                    // Gunakan calculatedEfficiency jika ada, jika tidak gunakan manualEfficiency
                    val displayEff = it.calculatedEfficiency ?: it.manualEfficiency
                    editFuelEff.setText(String.format(Locale.US, "%.1f", displayEff))
                    editTankCapacity.setText(it.tankCapacity.toString())

                    spinnerFuel.setText(it.fuelType, false)
                }
            }
        }

        btnSave.setOnClickListener {
            val name = editName.text.toString()
            val plate = editPlate.text.toString()
            val fuel = spinnerFuel.text.toString()
            val odo = editOdo.text.toString().toDoubleOrNull() ?: 0.0
            val interval = editServiceInterval.text.toString().toDoubleOrNull() ?: 5000.0
            val eff = editFuelEff.text.toString().toDoubleOrNull() ?: 12.0
            val capacity = editTankCapacity.text.toString().toDoubleOrNull() ?: 40.0

            if (name.isBlank() || plate.isBlank() || fuel.isBlank()) {
                Toast.makeText(this, "Mohon isi semua data", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val db = AppDatabase.getDatabase(this@AddVehicleActivity)
                val existing = if (vehicleId != -1) db.vehicleDao().getVehicleById(vehicleId) else null
                
                val vehicle = Vehicle(
                    id = if (vehicleId == -1) 0 else vehicleId,
                    name = name,
                    plateNumber = plate,
                    fuelType = fuel,
                    currentOdometer = odo,
                    serviceIntervalKm = interval,
                    manualEfficiency = eff,
                    tankCapacity = capacity,
                    lastServiceOdo = existing?.lastServiceOdo ?: 0.0,
                    calculatedEfficiency = existing?.calculatedEfficiency
                )

                if (vehicleId == -1) {
                    db.vehicleDao().insert(vehicle)
                } else {
                    db.vehicleDao().update(vehicle)
                }
                Toast.makeText(this@AddVehicleActivity, "Data kendaraan berhasil disimpan", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
