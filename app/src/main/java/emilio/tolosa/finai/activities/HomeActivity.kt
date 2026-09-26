package emilio.tolosa.finai.activities

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import emilio.tolosa.finai.R
import emilio.tolosa.finai.ShakeDetector
import emilio.tolosa.finai.databinding.ActivityHomeBinding
import emilio.tolosa.finai.ui.HomeFragment
import emilio.tolosa.finai.ui.MetasFragment
import emilio.tolosa.finai.ui.MovimientosFragment
import emilio.tolosa.finai.ui.NuevoMovimientoDialog
import emilio.tolosa.finai.ui.PerfilFragment
import emilio.tolosa.finai.ui.PresupuestoFragment

class HomeActivity : AppCompatActivity() {
    private lateinit var b: ActivityHomeBinding
    private lateinit var sm: SensorManager
    private val shake = ShakeDetector {
        NuevoMovimientoDialog(origen = "sensor").show(supportFragmentManager, "shake")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(b.root)

        if (savedInstanceState == null) mostrar(HomeFragment())

        b.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> mostrar(HomeFragment())
                R.id.nav_movimientos -> mostrar(MovimientosFragment())
                R.id.nav_presupuesto -> mostrar(PresupuestoFragment())
                R.id.nav_metas -> mostrar(MetasFragment())
                R.id.nav_perfil -> mostrar(PerfilFragment())
            }
            true
        }
        b.fabIa.setOnClickListener {
            startActivity(Intent(this, AIAssistantActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        sm = getSystemService(SENSOR_SERVICE) as SensorManager
        sm.registerListener(shake, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI)
    }

    override fun onPause() {
        super.onPause()
        sm.unregisterListener(shake)
    }

    private fun mostrar(f: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.contenedor, f)
            .commit()
    }
}