package emilio.tolosa.finai

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import emilio.tolosa.finai.data.DataStoreManager
import emilio.tolosa.finai.databinding.ActivityRegisterBinding
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Conectar RegisterActivity con activity_register.xml
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Botón para registrar al usuario
        binding.btnRegistrar.setOnClickListener {

            val nombre = binding.etNombre.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            // Validar nombre, correo y contraseña
            if (
                nombre.isEmpty() ||
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() ||
                password.length < 6
            ) {

                Toast.makeText(
                    this,
                    getString(R.string.error_register_data),
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            // Guardar usuario localmente
            lifecycleScope.launch {

                DataStoreManager(this@RegisterActivity).registrar(
                    nombre,
                    email,
                    password.sha256()
                )

                // Ir directamente al Home después del registro
                startActivity(
                    Intent(
                        this@RegisterActivity,
                        HomeActivity::class.java
                    )
                )

                finishAffinity()
            }
        }

        // Regresar al Login
        binding.tvLogin.setOnClickListener {

            finish()
        }

        // Google solamente visual por ahora
        binding.btnGoogle.setOnClickListener {

            Toast.makeText(
                this,
                getString(R.string.coming_soon),
                Toast.LENGTH_SHORT
            ).show()
        }

        // GitHub solamente visual por ahora
        binding.btnGithub.setOnClickListener {

            Toast.makeText(
                this,
                getString(R.string.coming_soon),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}