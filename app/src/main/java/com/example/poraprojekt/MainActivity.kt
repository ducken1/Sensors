package com.example.poraprojekt

import android.Manifest
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.poraprojekt.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val MIN_DISTANCE_CHANGE_FOR_UPDATES: Float = 10f // 10 meters
    private val MIN_TIME_BW_UPDATES: Long = 1000 * 60 * 1 // 1 minute

    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometerSensor: Sensor
    private lateinit var gyroscopeSensor: Sensor
    private lateinit var magneticFieldSensor: Sensor

    private val sensorInfoMap = mutableMapOf<String, SensorInfo>()

    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1


    private var capturingAccelerometerData = false
    private var capturingGyroscopeData = false
    private var capturingMagneticFieldData = false


    private val handler = Handler()

    private var lastKnownLocation: Location? = null

    private val genericSensorEventListener = object : SensorEventListener {
        private var lastData = ""

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GYROSCOPE, Sensor.TYPE_MAGNETIC_FIELD -> {
                    handleSensorEvent(event)
                }
                // Add more cases for other sensor types if needed
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun handleSensorEvent(event: SensorEvent) {
            val sensorType = when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> "Accelerometer"
                Sensor.TYPE_GYROSCOPE -> "Gyroscope"
                Sensor.TYPE_MAGNETIC_FIELD -> "MagneticField"
                else -> return
            }

            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val sensorData = "X: $x | Y: $y | Z: $z"
            handleSensorData(sensorData, sensorType, event.timestamp)
        }

//        @RequiresApi(Build.VERSION_CODES.O)
//        private fun handleAccelerometerEvent(event: SensorEvent) {
//            // Process accelerometer data here
//            val x = event.values[0]
//            val y = event.values[1]
//            val z = event.values[2]
//
//            val sensorData = "X: $x | Y: $y | Z: $z"
//            handleSensorData(sensorData, "Accelerometer", event.timestamp)
//        }
//
//        @RequiresApi(Build.VERSION_CODES.O)
//        private fun handleGyroscopeEvent(event: SensorEvent) {
//            // Process gyroscope data here
//            val x = event.values[0]
//            val y = event.values[1]
//            val z = event.values[2]
//
//            val sensorData = "X: $x | Y: $y | Z: $z"
//            handleSensorData(sensorData, "Gyroscope", event.timestamp)
//        }
//
//        @RequiresApi(Build.VERSION_CODES.O)
//        private fun handleMagneticFieldEvent(event: SensorEvent) {
//            // Process magnetic field data here
//            val x = event.values[0]
//            val y = event.values[1]
//            val z = event.values[2]
//
//            val sensorData = "X: $x | Y: $y | Z: $z"
//            handleSensorData(sensorData, "MagneticField", event.timestamp)
//        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun handleSensorData(sensorData: String, sensorType: String, timestamp: Long) {
            val absoluteTime = System.currentTimeMillis()
            val instant = Instant.ofEpochMilli(absoluteTime)
            val zoneId = ZoneId.systemDefault()
            val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val absoluteTime2 = LocalDateTime.ofInstant(instant, zoneId).format(dateTimeFormatter)

            handler.postDelayed({
                if (sensorData != lastData) {
                    var captureTimeMillis = System.currentTimeMillis() - absoluteTime
                    captureTimeMillis /= 1000

                    when (sensorType) {
                        "Accelerometer" -> {
                            binding.absoluteTimeTextViewAccelerometer.text = "Time: $absoluteTime2"
                            binding.accelerometerDataTextView.text = "Data: $sensorData"
                            binding.startStopAccelerometerButton.text = "$captureTimeMillis"
                            binding.locationTextViewAccelerometer.text = getLocationName(
                                lastKnownLocation?.latitude ?: 0.0,
                                lastKnownLocation?.longitude ?: 0.0
                            )
                        }

                        "Gyroscope" -> {
                            binding.absoluteTimeTextViewGyroscope.text = "Time: $absoluteTime2"
                            binding.gyroscopeDataTextView.text = "Data: $sensorData"
                            binding.startStopGyroscopeButton.text = "$captureTimeMillis"
                            binding.locationTextViewGyroscope.text = getLocationName(
                                lastKnownLocation?.latitude ?: 0.0,
                                lastKnownLocation?.longitude ?: 0.0
                            )
                        }

                        "MagneticField" -> {
                            binding.absoluteTimeTextViewMagnet.text = "Time: $absoluteTime2"
                            binding.magneticFieldDataTextView.text = "Data: $sensorData"
                            binding.startStopMagneticFieldButton.text = "$captureTimeMillis"
                            binding.locationTextViewMagnet.text = getLocationName(
                                lastKnownLocation?.latitude ?: 0.0,
                                lastKnownLocation?.longitude ?: 0.0
                            )
                        }
                    }
                    requestLocationUpdates { location ->
                        lastKnownLocation = location
                        val locationName = getLocationName(location.latitude, location.longitude)
                        saveSensorDataToFirestore(
                            sensorType,
                            sensorData,
                            absoluteTime2,
                            locationName,
                            timestamp
                        )
                    }
                    lastData = sensorData
                }
            }, getCaptureInterval(sensorType))
        }
    }


    private fun getCaptureInterval(sensorType: String): Long {
        val intervalValue: Long
        val selectedUnitPosition: Int

        when (sensorType) {
            "Accelerometer" -> {
                intervalValue =
                    binding.intervalEditTextAccelerometer.text.toString().toLongOrNull() ?: 0
                selectedUnitPosition = binding.timeUnitSpinnerAccelerometer.selectedItemPosition
            }

            "Gyroscope" -> {
                intervalValue =
                    binding.intervalEditTextGyroscope.text.toString().toLongOrNull() ?: 0
                selectedUnitPosition = binding.timeUnitSpinnerGyroscope.selectedItemPosition
            }

            "MagneticField" -> {
                intervalValue =
                    binding.intervalEditTextMagneticField.text.toString().toLongOrNull() ?: 0
                selectedUnitPosition = binding.timeUnitSpinnerMagneticField.selectedItemPosition
            }

            else -> {
                intervalValue = 0
                selectedUnitPosition = 0
            }
        }

        return when (selectedUnitPosition) {
            0 -> intervalValue * 1000  // Seconds
            1 -> intervalValue * 60 * 1000  // Minutes
            2 -> intervalValue * 60 * 60 * 1000  // Hours
            else -> 0
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)!!
        magneticFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)!!

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        binding.historyButton.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }

        binding.startStopAccelerometerButton.setOnClickListener {
            toggleSensorDataCapture(
                "Accelerometer",
                genericSensorEventListener,
                accelerometerSensor,
                it
            )
        }
        binding.startStopGyroscopeButton.setOnClickListener {
            toggleSensorDataCapture(
                "Gyroscope",
                genericSensorEventListener,
                gyroscopeSensor,
                it
            )
        }
        binding.startStopMagneticFieldButton.setOnClickListener {
            toggleSensorDataCapture(
                "MagneticField",
                genericSensorEventListener,
                magneticFieldSensor,
                it
            )
        }

        val timeUnitAdapterAccelerometer = ArrayAdapter.createFromResource(
            this,
            R.array.time_units,
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.timeUnitSpinnerAccelerometer.adapter = timeUnitAdapterAccelerometer

        val timeUnitAdapterGyroscope = ArrayAdapter.createFromResource(
            this,
            R.array.time_units,
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.timeUnitSpinnerGyroscope.adapter = timeUnitAdapterGyroscope

        val timeUnitAdapterMagneticField = ArrayAdapter.createFromResource(
            this,
            R.array.time_units,
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.timeUnitSpinnerMagneticField.adapter = timeUnitAdapterMagneticField
    }

    private fun saveSensorDataToFirestore(
        sensorType: String,
        sensorData: String,
        absoluteTime: String,
        location: String,
        casTrajanja: Long
    ) {
        // Implement Firestore saving logic here
        // For demonstration purposes, let's assume you have a Firestore collection named "Readings"

        val firestore = FirebaseFirestore.getInstance()
        val readingsCollection = firestore.collection("Readings")

        val data = hashMapOf(
            "sensorType" to sensorType,
            "sensorData" to sensorData,
            "absoluteTime" to absoluteTime,
            "location" to location,
            "casTrajanja" to casTrajanja
        )

        readingsCollection.add(data)
            .addOnSuccessListener { documentReference ->
                Log.d("FireStore", "DocumentSnapshot added with ID: ${documentReference.id}")
                // Handle success if needed
            }
            .addOnFailureListener { e ->
                Log.w("FireStore", "Error adding document", e)
                // Handle failure if needed
            }
    }


    private fun requestLocationUpdates(callback: (Location) -> Unit) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Request permissions
            callback.invoke(Location("")) // You can handle this case accordingly
            return
        }

        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                Log.d(
                    "Location",
                    "Latitude: ${location.latitude}, Longitude: ${location.longitude}"
                )
                callback.invoke(location)
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

            override fun onProviderEnabled(provider: String) {}

            override fun onProviderDisabled(provider: String) {}
        }

        // Use whichever provider you prefer (NETWORK_PROVIDER or GPS_PROVIDER)
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            MIN_TIME_BW_UPDATES,
            MIN_DISTANCE_CHANGE_FOR_UPDATES,
            locationListener
        )
    }

    private fun getLocationName(latitude: Double, longitude: Double): String {
        val geocoder = Geocoder(this, Locale.getDefault())
        val addresses = geocoder.getFromLocation(latitude, longitude, 1)

        return if (!addresses.isNullOrEmpty()) {
            addresses[0].getAddressLine(0)
        } else {
            "Unknown Location"
        }
    }


    private fun toggleSensorDataCapture(
        sensorType: String,
        sensorEventListener: SensorEventListener,
        sensor: Sensor,
        button: View
    ) {
        val sensorInfo = sensorInfoMap.getOrPut(sensorType) { SensorInfo() }
        sensorInfo.capturingFlag = !sensorInfo.capturingFlag

        val buttonBackgroundColor: Int
        val buttonTextColor: Int

        if (sensorInfo.capturingFlag) {
            buttonBackgroundColor = R.color.green
            buttonTextColor = R.color.black
            (button as? TextView)?.apply {
                button.setBackgroundColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        buttonBackgroundColor
                    )
                )
                button.setTextColor(ContextCompat.getColor(this@MainActivity, buttonTextColor))
            }
            sensorManager.unregisterListener(sensorEventListener)
            getCaptureInterval(sensorType)

            sensorInfo.handler = Handler()

            sensorManager.registerListener(
                sensorEventListener,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        } else {
            buttonBackgroundColor = R.color.red
            buttonTextColor = R.color.black
            (button as? TextView)?.apply {
                button.setBackgroundColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        buttonBackgroundColor
                    )
                )
                button.setTextColor(ContextCompat.getColor(this@MainActivity, buttonTextColor))
            }
            sensorInfo.handler?.removeCallbacksAndMessages(null)
            sensorManager.unregisterListener(sensorEventListener)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationManager.removeUpdates(locationListener)
        sensorManager.unregisterListener(genericSensorEventListener)

    }

    private class SensorInfo {
        var capturingFlag: Boolean = false
        var handler: Handler? = null
    }
}