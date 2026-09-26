package emilio.tolosa.finai.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.lifecycle.lifecycleScope
import emilio.tolosa.finai.R
import emilio.tolosa.finai.biometrics.BiometricAuthListener
import emilio.tolosa.finai.biometrics.BiometricUtil
import emilio.tolosa.finai.data.DataStoreManager
import emilio.tolosa.finai.databinding.ActivityLoginBinding
import emilio.tolosa.finai.sha256
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity(), BiometricAuthListener {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var store: DataStoreManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Conectamos LoginActivity con activity_login.xml
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // DataStore se encargará de revisar los datos guardados del usuario
        store = DataStoreManager(this)

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

        // Botón "Entrar con huella"
        binding.btnBiometrico.setOnClickListener {
            lifecycleScope.launch {
                val emailGuardado = store.email.first()

                if (emailGuardado.isEmpty()) {
                    // Todavía no hay ninguna cuenta registrada en este dispositivo
                    Toast.makeText(
                        this@LoginActivity,
                        "Primero regístrate una vez con tu correo y contraseña",
                        Toast.LENGTH_LONG
                    ).show()
                } else if (!BiometricUtil.isBiometricReady(this@LoginActivity)) {
                    Toast.makeText(
                        this@LoginActivity,
                        "No hay huella configurada en este dispositivo",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    BiometricUtil.showBiometricPrompt(
                        title = "Finai",
                        subtitle = "Confirma tu identidad",
                        description = "Usa tu huella para entrar",
                        activity = this@LoginActivity,
                        listener = this@LoginActivity
                    )
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

    // --- Callbacks de BiometricAuthListener ---

    override fun onBiometricAuthenticationSuccess(result: BiometricPrompt.AuthenticationResult) {
        lifecycleScope.launch {
            store.activarSesion()
            startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
            finish()
        }
    }

    override fun onBiometricAuthenticationError(errorCode: Int, errorMessage: String) {
        Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
    }
}