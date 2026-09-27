package emilio.tolosa.finai.activities

import android.Manifest
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import emilio.tolosa.finai.BuildConfig
import emilio.tolosa.finai.databinding.ActivityCameraBinding
import emilio.tolosa.finai.networking.APIConstants
import emilio.tolosa.finai.networking.RequestListener
import emilio.tolosa.finai.networking.RequestManager
import emilio.tolosa.finai.networking.models.AnahuacAPI
import emilio.tolosa.finai.networking.models.Encoding
import emilio.tolosa.finai.networking.models.HTTPMethod
import emilio.tolosa.finai.ui.NuevoMovimientoDialog
import emilio.tolosa.finai.viewmodel.FinanzasViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

data class TicketIa(val titulo: String, val monto: Double, val categoria: String)

class CameraActivity : AppCompatActivity() {
    private lateinit var b: ActivityCameraBinding
    private val vm: FinanzasViewModel by viewModels()
    private lateinit var requestManager: RequestManager

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
        requestManager = RequestManager(this)

        b.btnVolver.setOnClickListener { finish() }
        b.btnFoto.setOnClickListener { pedirPermiso.launch(Manifest.permission.CAMERA) }
    }

    private fun analizar(bmp: Bitmap) {
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
        val base64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)

        // El contenido del mensaje es un arreglo: un bloque de texto + un bloque de imagen
        val contenido = JSONArray().apply {
            put(JSONObject().apply {
                put("type", "text")
                put("text", """Lee este ticket. Responde SOLO un JSON sin texto extra:
                    {"titulo":"comercio","monto":0.0,"categoria":"Comida|Transporte|Entretenimiento|Compras|Salud|Otros"}""")
            })
            put(JSONObject().apply {
                put("type", "image_url")
                put("image_url", JSONObject().apply {
                    put("url", "data:image/jpeg;base64,$base64")
                })
            })
        }

        val mensajes = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "user")
                put("content", contenido)
            })
        }

        val body = JSONObject().apply {
            put("model", "gpt-4o-mini")
            put("messages", mensajes)
        }

        val target = AnahuacAPI(
            url = APIConstants.OPENAI_SERVER,
            method = HTTPMethod.POST,
            encoding = Encoding.JSON,
            parameters = body,
            headers = mapOf(
                "Authorization" to "Bearer ${BuildConfig.OPENAI_KEY}",
                "Content-Type" to "application/json"
            )
        )

        requestManager.request(target, object : RequestListener {
            override fun onResponse(response: String) {
                try {
                    val json = JSONObject(response)
                    val texto = json.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                        .replace("```json", "").replace("```", "").trim()

                    val t = Gson().fromJson(texto, TicketIa::class.java)
                    runOnUiThread {
                        NuevoMovimientoDialog(tituloInicial = t.titulo, montoInicial = t.monto, categoriaInicial = t.categoria, origen = "camara")
                            .show(supportFragmentManager, "ticket")
                    }
                } catch (e: Exception) {
                    Log.e("CameraActivity", "Error parsing: ${e.message}")
                    runOnUiThread {
                        Toast.makeText(this@CameraActivity, "No pude leer el ticket", Toast.LENGTH_LONG).show()
                    }
                }
            }

            override fun onError(error: String) {
                runOnUiThread {
                    Toast.makeText(this@CameraActivity, "Error: $error", Toast.LENGTH_LONG).show()
                }
            }
        })
    }
}