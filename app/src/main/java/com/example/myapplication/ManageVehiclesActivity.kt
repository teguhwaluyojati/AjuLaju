package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.Vehicle
import com.example.myapplication.data.VehicleAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class ManageVehiclesActivity : AppCompatActivity() {

    private val db by lazy { AppDatabase.getDatabase(this) }
    private lateinit var adapter: VehicleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_manage_vehicles)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewVehicles)
        val textEmpty = findViewById<TextView>(R.id.textEmptyVehicles)
        val fab = findViewById<FloatingActionButton>(R.id.fabAddVehicle)

        adapter = VehicleAdapter(
            onEdit = { vehicle ->
                val intent = Intent(this, AddVehicleActivity::class.java)
                intent.putExtra("VEHICLE_ID", vehicle.id)
                startActivity(intent)
            },
            onDelete = { vehicle ->
                showDeleteConfirmation(vehicle)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        db.vehicleDao().getAllVehicles().asLiveData().observe(this) { vehicles ->
            adapter.submitList(vehicles)
            textEmpty.visibility = if (vehicles.isEmpty()) View.VISIBLE else View.GONE
        }

        fab.setOnClickListener {
            startActivity(Intent(this, AddVehicleActivity::class.java))
        }
    }

    private fun showDeleteConfirmation(vehicle: Vehicle) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Kendaraan")
            .setMessage("Apakah Anda yakin ingin menghapus ${vehicle.name}?")
            .setPositiveButton("Hapus") { _, _ ->
                lifecycleScope.launch {
                    db.vehicleDao().delete(vehicle)
                    Toast.makeText(this@ManageVehiclesActivity, "Kendaraan dihapus", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
