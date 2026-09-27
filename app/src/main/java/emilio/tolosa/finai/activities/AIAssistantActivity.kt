package emilio.tolosa.finai.activities

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import emilio.tolosa.finai.BuildConfig
import emilio.tolosa.finai.databinding.ActivityAiAssistantBinding
import emilio.tolosa.finai.ui.ChatAdapter
import emilio.tolosa.finai.viewmodel.ChatViewModel
import emilio.tolosa.finai.viewmodel.FinanzasViewModel



class AIAssistantActivity : AppCompatActivity() {
    private lateinit var b: ActivityAiAssistantBinding
    private val vm: FinanzasViewModel by viewModels()
    private lateinit var chatVm: ChatViewModel

    private val historial = mutableListOf<Pair<String, String>>()
    private val visibles = mutableListOf<String>()
    private lateinit var adapter: ChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityAiAssistantBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.btnVolver.setOnClickListener { finish() }

        chatVm = ChatViewModel(this)

        adapter = ChatAdapter()
        b.rvChat.layoutManager = LinearLayoutManager(this)
        b.rvChat.adapter = adapter

        historial.add("system" to """
            Eres Finai, un asistente de finanzas personales para un estudiante en México.
            Responde en español, breve y claro, con consejos prácticos. Usa pesos mexicanos.
            Estos son los datos actuales del usuario:
            ${vm.resumenParaIa()}
        """.trimIndent())

        chatVm.respuesta.observe(this) { texto ->
            b.progress.visibility = View.GONE
            historial.add("assistant" to texto)
            agregar("Finai: $texto")
        }
        chatVm.error.observe(this) { err ->
            b.progress.visibility = View.GONE
            agregar("Error: $err")
        }

        b.btnEnviar.setOnClickListener {
            val texto = b.etMensaje.text.toString().trim()
            if (texto.isEmpty()) return@setOnClickListener
            b.etMensaje.text.clear()
            agregar("Tú: $texto")
            historial.add("user" to texto)

            b.progress.visibility = View.VISIBLE
            chatVm.enviarMensaje(BuildConfig.OPENAI_KEY, historial)
        }
    }

    private fun agregar(t: String) {
        visibles.add(t)
        adapter.submit(visibles.toList())
        b.rvChat.scrollToPosition(visibles.size - 1)
    }
}