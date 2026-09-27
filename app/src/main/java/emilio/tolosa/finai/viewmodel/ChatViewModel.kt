package emilio.tolosa.finai.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import emilio.tolosa.finai.networking.APIConstants
import emilio.tolosa.finai.networking.RequestListener
import emilio.tolosa.finai.networking.RequestManager
import emilio.tolosa.finai.networking.models.AnahuacAPI
import emilio.tolosa.finai.networking.models.ChatModel
import emilio.tolosa.finai.networking.models.Encoding
import emilio.tolosa.finai.networking.models.HTTPMethod
import org.json.JSONArray
import org.json.JSONObject

class ChatViewModel(private val context: Context) : ViewModel() {
    private val TAG = "ChatViewModel"
    private val model = ChatModel()
    private val requestManager = RequestManager(context)

    val respuesta: LiveData<String> get() = model.respuesta
    val error: LiveData<String> get() = model.error

    // historial: lista de pares (role, content) -> ej. ("user", "Hola")
    fun enviarMensaje(apiKey: String, historial: List<Pair<String, String>>) {
        val mensajes = JSONArray()
        historial.forEach { (role, content) ->
            mensajes.put(JSONObject().apply {
                put("role", role)
                put("content", content)
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
                "Authorization" to "Bearer $apiKey",
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
                    model.respuesta.postValue(texto)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing: ${e.message}")
                    model.error.postValue("No pude leer la respuesta")
                }
            }
            override fun onError(error: String) {
                model.error.postValue(error)
            }
        })
    }
}