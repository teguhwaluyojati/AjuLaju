package com.example.myapplication

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.data.AppDatabase
import com.google.android.material.color.MaterialColors
import org.osmdroid.config.Configuration
import kotlinx.coroutines.launch
import android.preference.PreferenceManager
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class TripDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_trip_detail)

        val tripId = intent.getIntExtra("TRIP_ID", -1)
        if (tripId == -1) { finish(); return }

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val textDest = findViewById<TextView>(R.id.detailDestination)
        val textDate = findViewById<TextView>(R.id.detailDate)
        val textDist = findViewById<TextView>(R.id.detailDistance)
        val textDur = findViewById<TextView>(R.id.detailDuration)
        val textVeh = findViewById<TextView>(R.id.detailVehicle)
        val textEff = findViewById<TextView>(R.id.detailEfficiency)
        val textFuelUsed = findViewById<TextView>(R.id.detailFuelUsed)
        val textNotes = findViewById<TextView>(R.id.detailNotes)
        val cardNotes = findViewById<View>(R.id.cardDetailNotes)
        val cardRoute = findViewById<View>(R.id.cardRoute)
        val textNoRoute = findViewById<TextView>(R.id.textNoRoute)
        val mapView = findViewById<MapView>(R.id.mapViewRoute)

        Configuration.getInstance().load(
            applicationContext,
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )
        Configuration.getInstance().userAgentValue = applicationContext.packageName
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setUseDataConnection(true)
        mapView.setMultiTouchControls(true)

        val primaryColor = MaterialColors.getColor(
            this,
            com.google.android.material.R.attr.colorPrimary,
            0xFF2196F3.toInt()
        )

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@TripDetailActivity)
            val trip = db.tripDao().getTripById(tripId)
            
            trip?.let { t ->
                textDest.text = t.destination
                val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy • HH:mm", Locale("in", "ID"))
                textDate.text = sdf.format(Date(t.date))
                
                val distance = t.endKm - t.startKm
                textDist.text = String.format(Locale.getDefault(), "%.1f km", distance)
                
                if (t.durationMillis > 0) {
                    val hours = TimeUnit.MILLISECONDS.toHours(t.durationMillis)
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(t.durationMillis) % 60
                    val seconds = TimeUnit.MILLISECONDS.toSeconds(t.durationMillis) % 60
                    textDur.text = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
                } else {
                    textDur.text = "--:--:--"
                }

                val vehicle = db.vehicleDao().getVehicleById(t.vehicleId)
                textVeh.text = vehicle?.name ?: "-"

                // Ambil Efisiensi BBM (km/L)
                val fuelEff = vehicle?.calculatedEfficiency ?: vehicle?.manualEfficiency ?: 12.0

                textEff.text = String.format(Locale.getDefault(), "%.1f km/L", fuelEff)
                
                // Hitung total bensin yang habis (Gunakan weightedDistance jika ada dan valid)
                val calculationDistance = if (t.weightedDistance != null && t.weightedDistance > 0) t.weightedDistance else distance
                val estimatedFuel = calculationDistance / fuelEff
                textFuelUsed.text = String.format(Locale.getDefault(), "%.2f L", estimatedFuel)

                if (!t.notes.isNullOrBlank()) {
                    textNotes.text = t.notes
                    cardNotes.visibility = View.VISIBLE
                } else {
                    cardNotes.visibility = View.GONE
                }

                val routePoints = db.routePointDao().getByTripId(tripId)
                if (routePoints.isEmpty()) {
                    textNoRoute.visibility = View.VISIBLE
                    mapView.visibility = View.GONE
                } else {
                    textNoRoute.visibility = View.GONE
                    mapView.visibility = View.VISIBLE

                    val geoPoints = routePoints.map { GeoPoint(it.latitude, it.longitude) }
                    val polyline = Polyline().apply {
                        outlinePaint.color = primaryColor
                        outlinePaint.strokeWidth = 8f
                        setPoints(geoPoints)
                    }
                    mapView.overlays.clear()
                    mapView.overlays.add(polyline)

                    val startMarker = Marker(mapView).apply {
                        position = geoPoints.first()
                        icon = androidx.core.content.ContextCompat.getDrawable(this@TripDetailActivity, R.drawable.ic_flag_start)
                        setAnchor(0.2f, 0.9f)
                        title = "Start"
                    }
                    val endMarker = Marker(mapView).apply {
                        position = geoPoints.last()
                        icon = androidx.core.content.ContextCompat.getDrawable(this@TripDetailActivity, R.drawable.ic_flag_finish)
                        setAnchor(0.2f, 0.9f)
                        title = "Finish"
                    }
                    mapView.overlays.add(startMarker)
                    mapView.overlays.add(endMarker)

                    val bbox = BoundingBox.fromGeoPoints(geoPoints)
                    mapView.zoomToBoundingBox(bbox, true, 64)
                    mapView.invalidate()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        findViewById<MapView>(R.id.mapViewRoute).onResume()
    }

    override fun onPause() {
        findViewById<MapView>(R.id.mapViewRoute).onPause()
        super.onPause()
    }
}
