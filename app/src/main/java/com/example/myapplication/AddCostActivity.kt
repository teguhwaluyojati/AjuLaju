package com.example.myapplication

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.Cost
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddCostActivity : AppCompatActivity() {

    private var selectedDate: Long = System.currentTimeMillis()
    private var editingCostId: Int = 0
    private var selectedVehicleId: Int = 0
    private var pendingVehicleId: Int? = null
    private var selectedVehicleFuelType: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_cost)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val editVehicle = findViewById<AutoCompleteTextView>(R.id.editVehicle)
        val spinnerCostType = findViewById<AutoCompleteTextView>(R.id.spinnerCostType)
        val editAmount = findViewById<TextInputEditText>(R.id.editAmount)
        val editQuantity = findViewById<TextInputEditText>(R.id.editQuantity)
        val layoutFuelExtra = findViewById<LinearLayout>(R.id.layoutFuelExtra)
        val checkFullTank = findViewById<MaterialCheckBox>(R.id.checkFullTank)
        val editOdo = findViewById<TextInputEditText>(R.id.editOdo)
        val editDate = findViewById<TextInputEditText>(R.id.editDate)
        val editNotes = findViewById<TextInputEditText>(R.id.editNotes)
        val btnSave = findViewById<MaterialButton>(R.id.btnSaveCost)

        val costTypes = arrayOf("Bensin", "Servis", "Perbaikan", "Lainnya")
        spinnerCostType.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, costTypes))
        spinnerCostType.setOnItemClickListener { _, _, position, _ ->
            layoutFuelExtra.visibility = if (costTypes[position] == "Bensin") View.VISIBLE else View.GONE
            calculateLiters()
        }

        AppDatabase.getDatabase(this).vehicleDao().getAllVehicles().asLiveData().observe(this) { vehicles ->
            val names = vehicles.map { it.name }
            editVehicle.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, names))
            editVehicle.setOnItemClickListener { _, _, pos, _ ->
                vehicles.getOrNull(pos)?.let {
                    selectedVehicleId = it.id
                    selectedVehicleFuelType = it.fuelType
                    editOdo.setText(it.currentOdometer.toString())
                    calculateLiters()
                }
            }

            pendingVehicleId?.let { pendingId ->
                val matched = vehicles.find { it.id == pendingId }
                if (matched != null) {
                    selectedVehicleId = matched.id
                    selectedVehicleFuelType = matched.fuelType
                    editVehicle.setText(matched.name, false)
                    calculateLiters()
                }
                pendingVehicleId = null
            }
        }

        editingCostId = intent.getIntExtra(EXTRA_COST_ID, 0)

        updateDateLabel(editDate)
        editDate.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker().setSelection(selectedDate).build()
            datePicker.addOnPositiveButtonClickListener { selectedDate = it; updateDateLabel(editDate) }
            datePicker.show(supportFragmentManager, "DATE")
        }

        if (editingCostId != 0) {
            lifecycleScope.launch {
                val existing = AppDatabase.getDatabase(this@AddCostActivity).costDao().getCostById(editingCostId)
                if (existing != null) {
                    selectedDate = existing.date
                    updateDateLabel(editDate)
                    pendingVehicleId = existing.vehicleId
                    spinnerCostType.setText(existing.type, false)
                    editAmount.setText(existing.amount.toString())
                    editQuantity.setText(existing.quantity?.toString() ?: "")
                    editOdo.setText(existing.odometer?.toString() ?: "")
                    checkFullTank.isChecked = existing.isFullTank
                    editNotes.setText(existing.notes ?: "")
                    layoutFuelExtra.visibility = if (existing.type == "Bensin") View.VISIBLE else View.GONE
                }
            }
        }

        editAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                calculateLiters()
            }
        })

        spinnerCostType.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                calculateLiters()
            }
        })

        btnSave.setOnClickListener {
            val vName = editVehicle.text.toString()
            val type = spinnerCostType.text.toString()
            val amount = editAmount.text.toString().toDoubleOrNull()
            val quantity = editQuantity.text.toString().toDoubleOrNull()
            val odo = editOdo.text.toString().toDoubleOrNull()
            val isFull = checkFullTank.isChecked
            val notesValue = editNotes.text.toString()

            if (vName.isBlank() || type.isBlank() || amount == null) {
                Toast.makeText(this, "Mohon lengkapi data", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val db = AppDatabase.getDatabase(this@AddCostActivity)

                var finalVehicleId = selectedVehicleId
                if (finalVehicleId == 0) {
                    val vehicles = db.vehicleDao().getAllVehicles().first()
                    val matched = vehicles.find { it.name.trim().equals(vName.trim(), ignoreCase = true) }
                    if (matched != null) finalVehicleId = matched.id
                }

                if (finalVehicleId == 0) {
                    Toast.makeText(this@AddCostActivity, "Pilih kendaraan dari daftar", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val costToSave = Cost(
                    id = editingCostId,
                    date = selectedDate,
                    vehicleId = finalVehicleId,
                    type = type.trim(),
                    amount = amount,
                    quantity = quantity,
                    odometer = odo,
                    isFullTank = isFull,
                    notes = if (notesValue.isBlank()) null else notesValue
                )

                if (editingCostId == 0) {
                    db.costDao().insert(costToSave)
                } else {
                    db.costDao().update(costToSave)
                }

                val vehicle = db.vehicleDao().getVehicleById(finalVehicleId)
                if (vehicle != null) {
                    var updatedVehicle = vehicle
                    if (odo != null) {
                        updatedVehicle = updatedVehicle.copy(currentOdometer = maxOf(updatedVehicle.currentOdometer, odo))
                    }

                    val isBensinType = type.trim().equals("Bensin", ignoreCase = true)
                    if (isBensinType && isFull) {
                        if (odo != null && quantity != null && quantity > 0) {
                            val lastFullTank = db.costDao().getLastFullTank(finalVehicleId, odo)
                            if (lastFullTank != null && lastFullTank.odometer != null) {
                                val kmDiff = odo - lastFullTank.odometer
                                if (kmDiff > 0) {
                                    val efficiency = kmDiff / quantity
                                    updatedVehicle = updatedVehicle.copy(calculatedEfficiency = efficiency)
                                    Toast.makeText(applicationContext, "Berhasil! Efisiensi ${updatedVehicle.name}: ${String.format("%.1f", efficiency)} km/L", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(applicationContext, "Gagal hitung: Odometer harus > ${lastFullTank.odometer} km", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(applicationContext, "Info: Butuh 1 data Full Tank lama (KM < $odo) sebagai pembanding", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(applicationContext, "Gagal: Angka Odometer atau Liter tidak valid", Toast.LENGTH_LONG).show()
                        }
                    }

                    if (type.trim().equals("Servis", ignoreCase = true) && odo != null) {
                        updatedVehicle = updatedVehicle.copy(lastServiceOdo = odo)
                    }
                    db.vehicleDao().update(updatedVehicle)
                }

                val finishMsg = if (editingCostId == 0) "Data tersimpan" else "Data diperbarui"
                Toast.makeText(this@AddCostActivity, finishMsg, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun calculateLiters() {
        val editAmount = findViewById<TextInputEditText>(R.id.editAmount)
        val editQuantity = findViewById<TextInputEditText>(R.id.editQuantity)
        val spinnerCostType = findViewById<AutoCompleteTextView>(R.id.spinnerCostType)

        val typeText = spinnerCostType.text.toString()
        if (typeText.trim().equals("Bensin", ignoreCase = true)) {
            val amount = editAmount.text.toString().toDoubleOrNull() ?: 0.0
            if (amount > 0) {
                val fuelType = selectedVehicleFuelType?.lowercase()?.trim() ?: "pertalite"
                val pricePerLiter = when {
                    fuelType.contains("pertalite") -> 10000.0
                    fuelType.contains("pertamax turbo") -> 14400.0
                    fuelType.contains("pertamax") -> 13200.0
                    fuelType.contains("dexlite") -> 14550.0
                    fuelType.contains("pertamina dex") -> 15100.0
                    else -> 10000.0
                }
                val liters = amount / pricePerLiter
                // Gunakan Locale.US agar decimal separator selalu TITIK (.)
                editQuantity.setText(String.format(Locale.US, "%.2f", liters))
            }
        }
    }

    private fun updateDateLabel(editText: TextInputEditText) {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        editText.setText(sdf.format(Date(selectedDate)))
    }

    companion object {
        const val EXTRA_COST_ID = "extra_cost_id"
    }
}
