package com.apexroute.audio.routing

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager

class ProximitySensorManager(
    context: Context,
    private val onProximityChanged: (Boolean) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val proximitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private var wakeLock: PowerManager.WakeLock? = null
    private var isListening = false

    init {
        @Suppress("DEPRECATION")
        wakeLock = powerManager.newWakeLock(
            PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
            "ApexRoute::ProximityWakelock"
        )
    }

    fun startListening() {
        if (isListening || proximitySensor == null) return
        sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL)
        isListening = true
        try {
            wakeLock?.acquire(10 * 60 * 1000L) // 10 minutes max
        } catch (_: Exception) {
        }
    }

    fun stopListening() {
        if (!isListening) return
        try {
            sensorManager.unregisterListener(this)
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {
        }
        isListening = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_PROXIMITY) {
            val distance = event.values[0]
            val maxRange = proximitySensor?.maximumRange ?: 5.0f
            val isNear = distance < maxRange && distance < 5.0f
            onProximityChanged(isNear)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
}
