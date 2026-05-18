package com.example.myapplication

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.*
import com.google.android.material.button.MaterialButton

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    
    private var windowManager: WindowManager? = null
    private var floatingView: View? = null

    companion object {
        const val CHANNEL_ID = "TripTrackingChannel"
        const val NOTIFICATION_ID = 123
        
        val liveDistance = MutableLiveData<Float>(0f)
        val liveAdjustedDistance = MutableLiveData<Float>(0f)
        val liveDurationMillis = MutableLiveData<Long>(0L)
        val liveSpeedKmH = MutableLiveData<Int>(0)
        val selectedVehicleEfficiency = MutableLiveData<Double>(12.0)
        val isTracking = MutableLiveData<Boolean>(false)
        val isPaused = MutableLiveData<Boolean>(false)
        
        private var lastLocation: Location? = null
        private var totalDistance = 0f
        private var weightedDistance = 0f
        private var accumulatedTimeMillis = 0L
        private var lastStartTimeTick = 0L
        private var selectedVehicleId: Int? = null
        private var selectedVehicleName: String? = null
        private val routePoints = mutableListOf<LiveRoutePoint>()

        data class LiveRoutePoint(
            val latitude: Double,
            val longitude: Double,
            val timestamp: Long
        )

        fun resetTracking() {
            totalDistance = 0f
            weightedDistance = 0f
            lastLocation = null
            accumulatedTimeMillis = 0L
            lastStartTimeTick = 0L
            selectedVehicleId = null
            selectedVehicleName = null
            liveDistance.postValue(0f)
            liveAdjustedDistance.postValue(0f)
            liveDurationMillis.postValue(0L)
            liveSpeedKmH.postValue(0)
            selectedVehicleEfficiency.postValue(12.0)
            isTracking.postValue(false)
            isPaused.postValue(false)
        }

        fun getRoutePointsSnapshot(): List<LiveRoutePoint> = routePoints.toList()

        fun clearRoutePoints() {
            routePoints.clear()
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateDistanceMeters(5f)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (isPaused.value == true) return

                for (location in locationResult.locations) {
                    lastLocation?.let {
                        val distance = it.distanceTo(location)
                        totalDistance += distance
                        
                        // Logika non-linier berdasarkan kecepatan
                        val speedKmH = location.speed * 3.6
                        val multiplier = when {
                            speedKmH < 5 -> 2.0f   // Sangat macet/idle
                            speedKmH < 15 -> 1.6f  // Macet
                            speedKmH < 40 -> 1.2f  // Padat
                            speedKmH < 80 -> 1.0f  // Optimal
                            else -> 1.15f          // Kecepatan tinggi (hambatan angin)
                        }
                        weightedDistance += distance * multiplier
                        
                        liveDistance.postValue(totalDistance / 1000f)
                        liveAdjustedDistance.postValue(weightedDistance / 1000f)
                        liveSpeedKmH.postValue(speedKmH.toInt())

                        if (distance >= 3f) {
                            routePoints.add(
                                LiveRoutePoint(location.latitude, location.longitude, System.currentTimeMillis())
                            )
                        }
                    } ?: run {
                        routePoints.add(
                            LiveRoutePoint(location.latitude, location.longitude, System.currentTimeMillis())
                        )
                    }
                    lastLocation = location
                }
                val currentDuration = accumulatedTimeMillis + (SystemClock.elapsedRealtime() - lastStartTimeTick)
                liveDurationMillis.postValue(currentDuration)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val vehicleId = intent?.getIntExtra("VEHICLE_ID", 0) ?: 0
        val vehicleName = intent?.getStringExtra("VEHICLE_NAME")
        val efficiency = intent?.getDoubleExtra("VEHICLE_EFFICIENCY", 12.0) ?: 12.0
        
        selectedVehicleId = if (vehicleId == 0) null else vehicleId
        selectedVehicleName = vehicleName
        selectedVehicleEfficiency.postValue(efficiency)

        createNotificationChannel()
        val content = if (vehicleName != null) "Merekam perjalanan $vehicleName..." else "Sedang merekam perjalanan..."
        val notification = createNotification(content)
        startForeground(NOTIFICATION_ID, notification)
        
        lastStartTimeTick = SystemClock.elapsedRealtime()
        accumulatedTimeMillis = 0L
        routePoints.clear()
        isTracking.postValue(true)
        isPaused.postValue(false)
        startLocationUpdates()
        
        showFloatingWindow()
        
        return START_STICKY
    }

    private fun showFloatingWindow() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        // Buat Context dengan Theme yang benar agar MaterialCardView tidak crash
        val contextThemeWrapper = android.view.ContextThemeWrapper(this, R.style.Theme_MyApplication)
        val inflater = LayoutInflater.from(contextThemeWrapper)
        floatingView = inflater.inflate(R.layout.layout_floating_tracking, null)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 200

        val textDist = floatingView?.findViewById<TextView>(R.id.textDistance)
        val textSpeed = floatingView?.findViewById<TextView>(R.id.textSpeed)
        val textFuel = floatingView?.findViewById<TextView>(R.id.textFuel)
        val btnPause = floatingView?.findViewById<MaterialButton>(R.id.btnPause)
        val btnFinish = floatingView?.findViewById<MaterialButton>(R.id.btnFinish)
        val btnToggle = floatingView?.findViewById<ImageView>(R.id.btnToggle)
        val layoutExpanded = floatingView?.findViewById<View>(R.id.layoutExpanded)
        val layoutConfirm = floatingView?.findViewById<View>(R.id.layoutConfirmFinish)
        val btnConfirmYes = floatingView?.findViewById<MaterialButton>(R.id.btnConfirmYes)
        val btnConfirmNo = floatingView?.findViewById<MaterialButton>(R.id.btnConfirmNo)

        liveDistance.observeForever { dist ->
            textDist?.text = String.format("%.2f km", dist)
        }
        
        liveSpeedKmH.observeForever { speed ->
            textSpeed?.text = speed.toString()
        }
        
        liveAdjustedDistance.observeForever { adjDist ->
            val eff = selectedVehicleEfficiency.value ?: 12.0
            val estimatedFuel = adjDist / eff
            textFuel?.text = String.format("%.2f", estimatedFuel)
        }

        isPaused.observeForever { paused ->
            btnPause?.text = if (paused) "Resume" else "Pause"
            btnPause?.setIconResource(if (paused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause)
        }

        btnPause?.setOnClickListener {
            val currentlyPaused = isPaused.value ?: false
            if (currentlyPaused) {
                // Resume
                lastStartTimeTick = SystemClock.elapsedRealtime()
                isPaused.postValue(false)
            } else {
                // Pause
                accumulatedTimeMillis += (SystemClock.elapsedRealtime() - lastStartTimeTick)
                isPaused.postValue(true)
                liveSpeedKmH.postValue(0)
            }
        }

        var isExpanded = false
        btnToggle?.setOnClickListener {
            isExpanded = !isExpanded
            layoutExpanded?.visibility = if (isExpanded) View.VISIBLE else View.GONE
            btnToggle.animate().rotation(if (isExpanded) 0f else 180f).start()
            
            // Sembunyikan konfirmasi jika sedang di-collapse
            if (!isExpanded) {
                layoutConfirm?.visibility = View.GONE
                btnFinish?.visibility = View.VISIBLE
            }
        }

        btnFinish?.setOnClickListener {
            // Munculkan konfirmasi
            btnFinish.visibility = View.GONE
            layoutConfirm?.visibility = View.VISIBLE
        }

        btnConfirmNo?.setOnClickListener {
            // Batalkan konfirmasi
            layoutConfirm?.visibility = View.GONE
            btnFinish?.visibility = View.VISIBLE
        }

        btnConfirmYes?.setOnClickListener {
            val totalDuration = if (isPaused.value == true) {
                accumulatedTimeMillis
            } else {
                accumulatedTimeMillis + (SystemClock.elapsedRealtime() - lastStartTimeTick)
            }

            val intent = Intent(this, AddTripActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("LIVE_DISTANCE", totalDistance.toDouble() / 1000.0)
                putExtra("LIVE_ADJUSTED_DISTANCE", weightedDistance.toDouble() / 1000.0)
                putExtra("LIVE_DURATION", totalDuration)
                putExtra("SELECTED_VEHICLE_ID", selectedVehicleId ?: 0)
                putExtra("SELECTED_VEHICLE_NAME", selectedVehicleName)
            }
            startActivity(intent)
            stopSelf()
            resetTracking()
        }

        // Drag logic
        floatingView?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isMoving = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isMoving = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val diffX = (event.rawX - initialTouchX).toInt()
                        val diffY = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(diffX) > 5 || Math.abs(diffY) > 5) {
                            params.x = initialX + diffX
                            params.y = initialY + diffY
                            windowManager?.updateViewLayout(floatingView, params)
                            isMoving = true
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isMoving) {
                            v.performClick()
                        }
                        return true
                    }
                }
                return false
            }
        })

        try {
            windowManager?.addView(floatingView, params)
        } catch (e: Exception) {
            // Permission not granted
        }
    }

    private fun hideFloatingWindow() {
        if (floatingView != null) {
            windowManager?.removeView(floatingView)
            floatingView = null
        }
    }

    private fun startLocationUpdates() {
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (unlikely: SecurityException) {
            isTracking.postValue(false)
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        isTracking.postValue(false)
    }

    override fun onDestroy() {
        stopLocationUpdates()
        hideFloatingWindow()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID, "Trip Tracking Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AjuLaju Tracker")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()
    }
}
