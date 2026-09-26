package emilio.tolosa.finai

import android.Manifest
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import emilio.tolosa.finai.databinding.ActivityCameraBinding
import emilio.tolosa.finai.network.*
import emilio.tolosa.finai.ui.NuevoMovimientoDialog
import emilio.tolosa.finai.viewmodel.FinanzasViewModel
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

data class TicketIa(val titulo: String, val monto: Double, val categoria: String)

class CameraActivity : AppCompatActivity() {
    private lateinit var b: ActivityCameraBinding
    private val vm: FinanzasViewModel by viewModels()

    private val tomarFoto = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        if (bmp != null) analizar(bmp)
    }
    private val pedirPermiso = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) tomarFoto.launch(null) else Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.btnFoto.setOnClickListener { pedirPermiso.launch(Manifest.permission.CAMERA) }
    }

    private fun analizar(bmp: Bitmap) = lifecycleScope.launch {
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
        val base64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)

        val contenido = listOf(
            mapOf("type" to "text", "text" to
                    """Lee este ticket. Responde SOLO un JSON sin texto extra:
                   {"titulo":"comercio","monto":0.0,"categoria":"Comida|Transporte|Entretenimiento|Compras|Salud|Otros"}"""),
            mapOf("type" to "image_url", "image_url" to mapOf("url" to "data:image/jpeg;base64,$base64"))
        )
        try {
            val r = ApiClient.openai.chat(ChatRequest(messages = listOf(ChatMessage("user", contenido))))
            val json = r.choices.first().message.content.toString()
                .replace("```json", "").replace("```", "").trim()
            val t = Gson().fromJson(json, TicketIa::class.java)
            NuevoMovimientoDialog(t.titulo, t.monto, t.categoria, "camara")
                .show(supportFragmentManager, "ticket")
        } catch (e: Exception) {
            Toast.makeText(this@CameraActivity, "No pude leer el ticket", Toast.LENGTH_LONG).show()
        }
    }
}