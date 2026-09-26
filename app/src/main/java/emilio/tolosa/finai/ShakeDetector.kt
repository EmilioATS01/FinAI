package emilio.tolosa.finai

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.pow
import kotlin.math.sqrt

class ShakeDetector(private val onShake: () -> Unit) : SensorEventListener {
    private var ultimo = 0L

    override fun onSensorChanged(e: SensorEvent) {
        val g = sqrt(e.values[0].pow(2) + e.values[1].pow(2) + e.values[2].pow(2)) / SensorManager.GRAVITY_EARTH
        val ahora = System.currentTimeMillis()
        if (g > 2.5f && ahora - ultimo > 1000) {
            ultimo = ahora
            onShake()
        }
    }

    override fun onAccuracyChanged(s: Sensor?, a: Int) {}
}