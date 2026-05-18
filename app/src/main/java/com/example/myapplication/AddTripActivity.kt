package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.RoutePoint
import com.example.myapplication.data.Trip
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddTripActivity : AppCompatActivity() {

    private var selectedDate: Long = System.currentTimeMillis()
    private var tripId: Int = -1
    private var liveDistance: Double = 0.0
    private var liveAdjustedDistance: Double = 0.0
    private var liveDuration: Long = 0
    private var didSave = false
    private var selectedVehicleId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_trip)

        tripId = intent.getIntExtra("TRIP_ID", -1)
        liveDistance = intent.getDoubleExtra("LIVE_DISTANCE", 0.0)
        liveAdjustedDistance = intent.getDoubleExtra("LIVE_ADJUSTED_DISTANCE", liveDistance)
        liveDuration = intent.getLongExtra("LIVE_DURATION", 0)
        val preSelectedVehicleId = intent.getIntExtra("SELECTED_VEHICLE_ID", 0)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        
        if (tripId != -1) {
            supportActionBar?.title = "Edit Perjalanan"
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val editDestination = findViewById<TextInputEditText>(R.id.editDestination)
        val editVehicle = findViewById<AutoCompleteTextView>(R.id.editVehicle)
        val layoutVehicle = findViewById<TextInputLayout>(R.id.layoutVehicle)
        val editStartKm = findViewById<TextInputEditText>(R.id.editStartKm)
        val editEndKm = findViewById<TextInputEditText>(R.id.editEndKm)
        val editDate = findViewById<TextInputEditText>(R.id.editDate)
        val editNotes = findViewById<TextInputEditText>(R.id.editNotes)
        val btnSave = findViewById<MaterialButton>(R.id.btnSave)

        if (tripId != -1) {
            btnSave.text = "Update Perjalanan"
        }

        // Setup Vehicle Dropdown
        AppDatabase.getDatabase(this).vehicleDao().getAllVehicles().asLiveData().observe(this) { vehicles ->
            if (vehicles.isEmpty()) {
                layoutVehicle.helperText = "Belum ada kendaraan. Klik untuk tambah."
                editVehicle.setOnClickListener {
                    startActivity(Intent(this, AddVehicleActivity::class.java))
                }
            } else {
                layoutVehicle.helperText = null
                val names = vehicles.map { it.name }
                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, names)
                editVehicle.setAdapter(adapter)
                
                if (tripId != -1) {
                    loadTripData(editDestination, editVehicle, editStartKm, editEndKm, editDate, editNotes, vehicles)
                } else if (preSelectedVehicleId != 0) {
                    // Set kendaraan dan KUNCI agar tidak bisa diubah lagi
                    val vehicle = vehicles.find { it.id == preSelectedVehicleId }
                    vehicle?.let {
                        selectedVehicleId = it.id
                        editVehicle.setText(it.name, false)
                        layoutVehicle.isEnabled = false
                        layoutVehicle.helperText = "Kendaraan otomatis terpilih dari Live Tracking"
                        val startVal = it.currentOdometer
                        editStartKm.setText(startVal.toString())
                        if (liveDistance > 0) {
                            val roundedEndKm = Math.round((startVal + liveDistance) * 10.0) / 10.0
                            editEndKm.setText(roundedEndKm.toString())
                        }
                    }
                    // Langsung arahkan fokus ke Tujuan agar cepat
                    editDestination.requestFocus()
                }

                editVehicle.setOnItemClickListener { _, _, position, _ ->
                    val selectedName = editVehicle.adapter.getItem(position).toString()
                    val vehicle = vehicles.find { it.name == selectedName }
                    vehicle?.let {
                        selectedVehicleId = it.id
                        val startVal = it.currentOdometer
                        editStartKm.setText(startVal.toString())
                        
                        if (liveDistance > 0) {
                            val roundedEndKm = Math.round((startVal + liveDistance) * 10.0) / 10.0
                            editEndKm.setText(roundedEndKm.toString())
                        }
                        editEndKm.requestFocus()
                    }
                }
            }
        }

        // Setup Date Picker
        updateDateLabel(editDate)
        editDate.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Pilih Tanggal")
                .setSelection(selectedDate)
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                selectedDate = selection
                updateDateLabel(editDate)
            }
            datePicker.show(supportFragmentManager, "DATE_PICKER")
        }

        btnSave.setOnClickListener {
            val dest = editDestination.text.toString()
            val vehicleName = editVehicle.text.toString()
            val startKm = editStartKm.text.toString().toDoubleOrNull()
            val endKm = editEndKm.text.toString().toDoubleOrNull()
            val notes = editNotes.text.toString()

            if (dest.isBlank() || vehicleName.isBlank() || startKm == null || endKm == null || selectedVehicleId == 0) {
                Toast.makeText(this, "Mohon isi semua data yang wajib", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (endKm < startKm) {
                Toast.makeText(this, "KM Akhir tidak boleh lebih kecil dari KM Awal", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val trip = Trip(
                id = if (tripId == -1) 0 else tripId,
                date = selectedDate,
                vehicleId = selectedVehicleId,
                startKm = startKm,
                endKm = endKm,
                destination = dest,
                durationMillis = if (tripId == -1) liveDuration else 0,
                weightedDistance = if (tripId == -1) liveAdjustedDistance else null,
                notes = if (notes.isBlank()) null else notes
            )

            lifecycleScope.launch {
                val db = AppDatabase.getDatabase(this@AddTripActivity)
                if (tripId == -1) {
                    val insertedId = db.tripDao().insert(trip).toInt()
                    val routeSnapshot = LocationService.getRoutePointsSnapshot()
                    if (routeSnapshot.isNotEmpty()) {
                        val routeEntities = routeSnapshot.map { point ->
                            RoutePoint(
                                tripId = insertedId,
                                latitude = point.latitude,
                                longitude = point.longitude,
                                timestamp = point.timestamp
                            )
                        }
                        db.routePointDao().insertAll(routeEntities)
                        LocationService.clearRoutePoints()
                    }
                } else {
                    // Jika edit, kita ambil durationMillis yang lama agar tidak hilang
                    val existing = db.tripDao().getTripById(tripId)
                    db.tripDao().update(trip.copy(durationMillis = existing?.durationMillis ?: 0))
                }
                
                db.vehicleDao().updateOdometerById(selectedVehicleId, endKm)

                Toast.makeText(this@AddTripActivity, "Data berhasil disimpan", Toast.LENGTH_SHORT).show()
                didSave = true
                finish()
            }
        }
    }

    override fun onDestroy() {
        if (!didSave) {
            LocationService.clearRoutePoints()
        }
        super.onDestroy()
    }

    private fun loadTripData(
        dest: TextInputEditText,
        veh: AutoCompleteTextView,
        start: TextInputEditText,
        end: TextInputEditText,
        date: TextInputEditText,
        notes: TextInputEditText,
        vehicles: List<com.example.myapplication.data.Vehicle>
    ) {
        lifecycleScope.launch {
            val trip = AppDatabase.getDatabase(this@AddTripActivity).tripDao().getTripById(tripId)
            trip?.let {
                dest.setText(it.destination)
                selectedVehicleId = it.vehicleId
                vehicles.find { v -> v.id == it.vehicleId }?.let { v ->
                    veh.setText(v.name, false)
                }
                start.setText(it.startKm.toString())
                end.setText(it.endKm.toString())
                selectedDate = it.date
                updateDateLabel(date)
                notes.setText(it.notes ?: "")
            }
        }
    }

    private fun updateDateLabel(editText: TextInputEditText) {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        editText.setText(sdf.format(Date(selectedDate)))
    }
}
