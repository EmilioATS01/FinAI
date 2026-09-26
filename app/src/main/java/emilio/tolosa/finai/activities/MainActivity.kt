package emilio.tolosa.finai.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import emilio.tolosa.finai.data.DataStoreManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {

            val activa =
                DataStoreManager(this@MainActivity)
                    .sesionActiva
                    .first()

            val destino =
                if (activa) {
                    HomeActivity::class.java
                } else {
                    LoginActivity::class.java
                }

            startActivity(
                Intent(this@MainActivity, destino)
            )

            finish()
        }
    }
}