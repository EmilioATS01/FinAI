package emilio.tolosa.finai

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import emilio.tolosa.finai.databinding.ActivityHomeBinding
import emilio.tolosa.finai.ui.HomeFragment
import emilio.tolosa.finai.ui.MetasFragment
import emilio.tolosa.finai.ui.MovimientosFragment
import emilio.tolosa.finai.ui.PerfilFragment
import emilio.tolosa.finai.ui.PresupuestoFragment

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Mostrar Inicio la primera vez que se abre HomeActivity
        if (savedInstanceState == null) {
            mostrar(HomeFragment())
        }

        // Detectar qué opción toca el usuario en la barra inferior
        binding.bottomNav.setOnItemSelectedListener { item ->

            when (item.itemId) {

                R.id.nav_inicio -> {
                    mostrar(HomeFragment())
                }

                R.id.nav_movimientos -> {
                    mostrar(MovimientosFragment())
                }

                R.id.nav_presupuesto -> {
                    mostrar(PresupuestoFragment())
                }

                R.id.nav_metas -> {
                    mostrar(MetasFragment())
                }

                R.id.nav_perfil -> {
                    mostrar(PerfilFragment())
                }
            }

            true
        }

        // Botón flotante para abrir el asistente de IA
        binding.fabIa.setOnClickListener {

            val intent = Intent(
                this,
                AIAssistantActivity::class.java
            )

            startActivity(intent)
        }
    }

    private fun mostrar(fragment: Fragment) {

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.contenedor, fragment)
            .commit()
    }
}