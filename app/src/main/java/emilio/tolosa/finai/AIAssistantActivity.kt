package emilio.tolosa.finai

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import emilio.tolosa.finai.databinding.ActivityAiAssistantBinding
import emilio.tolosa.finai.network.*
import emilio.tolosa.finai.ui.ChatAdapter
import emilio.tolosa.finai.viewmodel.FinanzasViewModel
import kotlinx.coroutines.launch

class AIAssistantActivity : AppCompatActivity() {
    private lateinit var b: ActivityAiAssistantBinding
    private val vm: FinanzasViewModel by viewModels()
    private val historial = mutableListOf<ChatMessage>()
    private val visibles = mutableListOf<String>()
    private lateinit var adapter: ChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityAiAssistantBinding.inflate(layoutInflater)
        setContentView(b.root)

        adapter = ChatAdapter()
        b.rvChat.layoutManager = LinearLayoutManager(this)
        b.rvChat.adapter = adapter

        historial.add(ChatMessage("system", """
            Eres Finai, un asistente de finanzas personales para un estudiante en México.
            Responde en español, breve y claro, con consejos prácticos. Usa pesos mexicanos.
            Estos son los datos actuales del usuario:
            ${vm.resumenParaIa()}
        """.trimIndent()))

        b.btnEnviar.setOnClickListener {
            val texto = b.etMensaje.text.toString().trim()
            if (texto.isEmpty()) return@setOnClickListener
            b.etMensaje.text.clear()
            agregar("Tú: $texto")
            historial.add(ChatMessage("user", texto))
            preguntar()
        }
    }

    private fun preguntar() = lifecycleScope.launch {
        b.progress.visibility = View.VISIBLE
        try {
            val r = ApiClient.openai.chat(ChatRequest(messages = historial))
            val respuesta = r.choices.first().message.content.toString()
            historial.add(ChatMessage("assistant", respuesta))
            agregar("Finai: $respuesta")
        } catch (e: Exception) {
            agregar("Error: no pude conectar (${e.message})")
        } finally {
            b.progress.visibility = View.GONE
        }
    }

    private fun agregar(t: String) {
        visibles.add(t)
        adapter.submit(visibles.toList())
        b.rvChat.scrollToPosition(visibles.size - 1)
    }
}
