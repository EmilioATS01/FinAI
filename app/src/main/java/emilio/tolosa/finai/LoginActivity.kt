package emilio.tolosa.finai

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import emilio.tolosa.finai.data.DataStoreManager
import emilio.tolosa.finai.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Conectamos LoginActivity con activity_login.xml
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // DataStore se encargará de revisar los datos guardados del usuario
        val store = DataStoreManager(this)

        // Botón "Iniciar sesión"
        binding.btnLogin.setOnClickListener {

            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            // Validar que los campos no estén vacíos
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(
                    this,
                    getString(R.string.error_empty_fields),
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            // Consultar DataStore
            lifecycleScope.launch {

                val loginCorrecto = store.login(
                    email,
                    password.sha256()
                )

                if (loginCorrecto) {

                    val intent = Intent(
                        this@LoginActivity,
                        HomeActivity::class.java
                    )

                    startActivity(intent)
                    finish()

                } else {

                    Toast.makeText(
                        this@LoginActivity,
                        getString(R.string.error_invalid_credentials),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // Ir a la pantalla de registro
        binding.tvRegistro.setOnClickListener {

            val intent = Intent(
                this,
                RegisterActivity::class.java
            )

            startActivity(intent)
        }
    }
}