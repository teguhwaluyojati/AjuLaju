package com.example.myapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.TripAdapter
import com.example.myapplication.data.TripWithVehicle
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val db by lazy { AppDatabase.getDatabase(this) }
    private lateinit var adapter: TripAdapter

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            val vId = pendingVehicleId
            val vName = pendingVehicleName
            val eff = pendingEfficiency
            if (vId != null && vName != null && eff != null) {
                startLiveTracking(vId, vName, eff)
                pendingVehicleId = null
                pendingVehicleName = null
                pendingEfficiency = null
            }
        } else {
            Toast.makeText(this, "Izin lokasi dibutuhkan untuk fitur ini", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        androidx.core.view.WindowCompat.getInsetsController(window, findViewById(R.id.main))
            .isAppearanceLightStatusBars = true

        val appBar = findViewById<com.google.android.material.appbar.AppBarLayout>(R.id.appBarLayout)
        ViewCompat.setOnApplyWindowInsetsListener(appBar) { v, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(v.paddingLeft, statusBars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        val bottomBar = findViewById<View>(R.id.bottomBarContainer)
        val bottomNavBg = findViewById<View>(R.id.bottomNavBg)
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)
        ViewCompat.setOnApplyWindowInsetsListener(bottomBar) { _, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            val baseHeight = (56 * resources.displayMetrics.density).toInt()
            bottomNavBg.layoutParams = bottomNavBg.layoutParams.apply {
                height = baseHeight + bottomInset
            }
            bottomNav.setPadding(
                bottomNav.paddingLeft,
                bottomNav.paddingTop,
                bottomNav.paddingRight,
                bottomInset + (2 * resources.displayMetrics.density).toInt()
            )
            insets
        }

        setupBottomNav()
        setupMainContent()
        setupLiveTrackingUI()
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_placeholder -> {
                    findViewById<View>(R.id.fabAddTrip).performClick()
                    false
                }
                R.id.nav_vehicles -> {
                    startActivity(Intent(this, ManageVehiclesActivity::class.java))
                    true
                }
                R.id.nav_add_cost -> {
                    startActivity(Intent(this, AddCostActivity::class.java))
                    true
                }
                R.id.nav_costs -> {
                    startActivity(Intent(this, ManageCostsActivity::class.java))
                    true
                }
                R.id.nav_statistics -> {
                    startActivity(Intent(this, StatisticsActivity::class.java))
                    true
                }
                else -> false
            }
        }
        bottomNav.selectedItemId = R.id.nav_home
    }

    private fun setupMainContent() {
        adapter = TripAdapter(
            onClick = { trip ->
                val intent = Intent(this, TripDetailActivity::class.java)
                intent.putExtra("TRIP_ID", trip.id)
                startActivity(intent)
            },
            onEdit = { trip ->
                val intent = Intent(this, AddTripActivity::class.java)
                intent.putExtra("TRIP_ID", trip.id)
                startActivity(intent)
            },
            onDelete = { trip ->
                showDeleteConfirmation(trip)
            }
        )

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewTrips)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val fab = findViewById<FloatingActionButton>(R.id.fabAddTrip)
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 10) fab.hide()
                else if (dy < -10) fab.show()
            }
        })

        val textEmpty = findViewById<View>(R.id.textEmpty)
        val textSummaryDistance = findViewById<TextView>(R.id.textSummaryDistance)
        val textSummaryTrips = findViewById<TextView>(R.id.textSummaryTrips)
        val cardReminder = findViewById<View>(R.id.cardReminder)
        val textReminder = findViewById<TextView>(R.id.textReminder)

        db.tripDao().getAllTrips().asLiveData().observe(this) { trips ->
            adapter.submitList(trips)
            val isEmpty = trips.isEmpty()
            textEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
            recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
            val totalDistance = trips.sumOf { it.endKm - it.startKm }
            textSummaryDistance.text = String.format("%.0f", totalDistance)
            textSummaryTrips.text = trips.size.toString()
            
            // Trigger update bensin jika trips berubah
            refreshFuelIndicator()
        }

        db.vehicleDao().getAllVehicles().asLiveData().observe(this) { vehicles ->
            val overdue = vehicles.find { (it.currentOdometer - it.lastServiceOdo) >= it.serviceIntervalKm }
            if (overdue != null) {
                cardReminder.visibility = View.VISIBLE
                textReminder.text = "Waktunya Servis! ${overdue.name} sudah menempuh ${overdue.currentOdometer.toInt() - overdue.lastServiceOdo.toInt()} km sejak servis."
            } else {
                cardReminder.visibility = View.GONE
            }
            refreshFuelIndicator()
        }
        
        // Tambahkan observer untuk Biaya agar Dashboard update saat bensin dihapus
        db.costDao().getAllCosts().asLiveData().observe(this) {
            refreshFuelIndicator()
        }

        fab.setOnClickListener {
            lifecycleScope.launch {
                val vehicles = db.vehicleDao().getAllVehicles().first()
                if (vehicles.isEmpty()) {
                    Toast.makeText(this@MainActivity, "Tambah kendaraan dulu di tab Kendaraan", Toast.LENGTH_LONG).show()
                    return@launch
                }

                val options = arrayOf("Mulai Live Tracking", "Input Manual")
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Tambah Perjalanan")
                    .setItems(options) { _, which ->
                        if (which == 0) {
                            val vehicleNames = vehicles.map { "${it.name} (${it.plateNumber})" }.toTypedArray()
                            AlertDialog.Builder(this@MainActivity)
                                .setTitle("Pilih Kendaraan")
                                .setItems(vehicleNames) { _, vIndex ->
                                    val selected = vehicles[vIndex]
                                    checkPermissionsAndStart(selected.id, selected.name, selected.calculatedEfficiency ?: selected.manualEfficiency)
                                }.show()
                        } else {
                            startActivity(Intent(this@MainActivity, AddTripActivity::class.java))
                        }
                    }.show()
            }
        }

        findViewById<View>(R.id.btnQuickFuel).setOnClickListener {
            startActivity(Intent(this, AddCostActivity::class.java))
        }

        findViewById<View>(R.id.btnQuickService).setOnClickListener {
            startActivity(Intent(this, AddCostActivity::class.java))
        }
    }

    private fun refreshFuelIndicator() {
        // Logika sisa bensin dihapus atas permintaan user karena redundan dengan dashboard mobil
    }

    private fun setupLiveTrackingUI() {
        val fab = findViewById<FloatingActionButton>(R.id.fabAddTrip)

        LocationService.isTracking.observe(this) { tracking ->
            if (tracking) fab.hide() else fab.show()
        }
    }

    private fun checkPermissionsAndStart(vehicleId: Int, vehicleName: String, efficiency: Double) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            Toast.makeText(this, "Mohon izinkan 'Tampil di atas aplikasi lain' untuk fitur tracking", Toast.LENGTH_LONG).show()
            return
        }

        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        if (permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            startLiveTracking(vehicleId, vehicleName, efficiency)
        } else {
            // Kita simpan sementara untuk dipanggil setelah izin diberikan
            pendingVehicleId = vehicleId
            pendingVehicleName = vehicleName
            pendingEfficiency = efficiency
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private var pendingVehicleId: Int? = null
    private var pendingVehicleName: String? = null
    private var pendingEfficiency: Double? = null

    private fun startLiveTracking(vehicleId: Int, vehicleName: String, efficiency: Double) {
        val intent = Intent(this, LocationService::class.java).apply {
            putExtra("VEHICLE_ID", vehicleId)
            putExtra("VEHICLE_NAME", vehicleName)
            putExtra("VEHICLE_EFFICIENCY", efficiency)
        }
        startService(intent)
    }

    private fun showDeleteConfirmation(trip: com.example.myapplication.data.TripWithVehicle) {
        AlertDialog.Builder(this)
            .setTitle("Hapus")
            .setMessage("Yakin hapus trip ke ${trip.destination}?")
            .setPositiveButton("Hapus") { _, _ ->
                lifecycleScope.launch {
                    db.tripDao().deleteById(trip.id)
                    db.routePointDao().deleteByTripId(trip.id)
                    val latestOdo = db.tripDao().getMaxEndKmForVehicle(trip.vehicleId)
                    val newOdo = latestOdo ?: trip.startKm
                    db.vehicleDao().updateOdometerById(trip.vehicleId, newOdo)
                }
            }.setNegativeButton("Batal", null).show()
    }
}
