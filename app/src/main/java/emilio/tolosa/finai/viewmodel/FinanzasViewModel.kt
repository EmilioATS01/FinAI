package emilio.tolosa.finai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import emilio.tolosa.finai.BuildConfig
import emilio.tolosa.finai.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PresupuestoUi(val categoria: String, val limite: Double, val gastado: Double) {
    val restante get() = limite - gastado
    val progreso get() = if (limite > 0) ((gastado / limite) * 100).toInt().coerceIn(0, 100) else 0
}

class FinanzasViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = MovimientoDatabase.get(app).dao()
    val store = DataStoreManager(app)

    private fun <T> Flow<T>.estado(inicial: T) =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), inicial)

    val movimientos = dao.todos().estado(emptyList())
    val ultimos = dao.ultimos(5).estado(emptyList())
    val ingresos = dao.totalIngresos().estado(0.0)
    val gastos = dao.totalGastos().estado(0.0)
    val balance = combine(ingresos, gastos) { i, g -> i - g }.estado(0.0)
    val metas = dao.metas().estado(emptyList())

    val presupuestos = combine(dao.presupuestos(), dao.gastosPorCategoria()) { pres, gastos ->
        pres.map { p ->
            PresupuestoUi(p.categoria, p.limite, gastos.find { it.categoria == p.categoria }?.total ?: 0.0)
        }
    }.estado(emptyList())

    

    fun agregarMovimiento(m: Movimiento) = viewModelScope.launch { dao.insertar(m) }
    fun borrarMovimiento(m: Movimiento) = viewModelScope.launch { dao.borrar(m) }

    fun actualizarMovimiento(m: Movimiento) = viewModelScope.launch { dao.actualizar(m) }
    fun agregarMeta(nombre: String, objetivo: Double) =
        viewModelScope.launch { dao.insertarMeta(Meta(nombre = nombre, objetivo = objetivo)) }
    fun abonarMeta(meta: Meta, monto: Double) =
        viewModelScope.launch { dao.actualizarMeta(meta.copy(ahorrado = meta.ahorrado + monto)) }
    fun guardarPresupuesto(cat: String, limite: Double) =
        viewModelScope.launch { dao.guardarPresupuesto(Presupuesto(cat, limite)) }



    fun resumenParaIa(): String {
        val movs = movimientos.value.take(15).joinToString("\n") {
            "- ${it.titulo} (${it.categoria}): ${if (it.esIngreso) "+" else "-"}${it.monto}"
        }
        val pres = presupuestos.value.joinToString("\n") {
            "- ${it.categoria}: límite ${it.limite}, gastado ${it.gastado}"
        }
        return """
            Balance: ${balance.value}. Ingresos: ${ingresos.value}. Gastos: ${gastos.value}.
            Últimos movimientos:
            $movs
            Presupuestos:
            $pres
        """.trimIndent()
    }

    fun importarDeBelvo(context: android.content.Context, onResultado: (String) -> Unit) {
        val requestManager = emilio.tolosa.finai.networking.RequestManager(context)
        val cred = android.util.Base64.encodeToString(
            "${BuildConfig.BELVO_ID}:${BuildConfig.BELVO_SECRET}".toByteArray(), android.util.Base64.NO_WRAP
        )
        val headers = mapOf("Authorization" to "Basic $cred", "Content-Type" to "application/json")

        val bodyLink = org.json.JSONObject().apply {
            put("institution", "tatooine_mx_fiscal")
            put("username", "PMO010101000")
            put("password", "business")
            put("access_mode", "single")
            put("fetch_resources", org.json.JSONArray(listOf("INVOICES")))
        }
        val targetLink = emilio.tolosa.finai.networking.models.AnahuacAPI(
            "https://sandbox.belvo.com/api/links/",
            emilio.tolosa.finai.networking.models.HTTPMethod.POST,
            emilio.tolosa.finai.networking.models.Encoding.JSON,
            bodyLink, headers
        )

        requestManager.request(targetLink, object : emilio.tolosa.finai.networking.RequestListener {
            override fun onResponse(response: String) {
                val linkId = org.json.JSONObject(response).getString("id")

                // Esperamos unos segundos: Belvo procesa las facturas en segundo plano
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    val urlInvoices = "https://sandbox.belvo.com/api/invoices/?link=$linkId"
                    val targetInvoices = emilio.tolosa.finai.networking.models.AnahuacAPI(
                        urlInvoices,
                        emilio.tolosa.finai.networking.models.HTTPMethod.GET,
                        emilio.tolosa.finai.networking.models.Encoding.URL,
                        null, headers
                    )

                    requestManager.request(targetInvoices, object : emilio.tolosa.finai.networking.RequestListener {
                        override fun onResponse(response: String) {
                            android.util.Log.i("Belvo", "RAW INVOICES: $response")
                            viewModelScope.launch {
                                try {
                                    val json = org.json.JSONObject(response)
                                    val results = json.optJSONArray("results") ?: org.json.JSONArray()

                                    if (results.length() == 0) {
                                        onResultado("Aún no hay facturas listas, intenta de nuevo en unos segundos")
                                        return@launch
                                    }

                                    val ingresos = mutableListOf<org.json.JSONObject>()
                                    val gastos = mutableListOf<org.json.JSONObject>()

                                    for (i in 0 until results.length()) {
                                        val inv = results.getJSONObject(i)
                                        val esIngreso = inv.optString("type") == "INFLOW" || inv.optString("invoice_type") == "Ingreso"
                                        if (esIngreso && ingresos.size < 2) ingresos.add(inv)
                                        else if (!esIngreso && gastos.size < 3) gastos.add(inv)
                                    }

                                    (ingresos + gastos).forEach { inv ->
                                        dao.insertar(
                                            Movimiento(
                                                titulo = inv.optString("sender_name").takeIf { it.isNotBlank() && it != "null" } ?: "Factura SAT",
                                                categoria = inv.optString("invoice_type", "Otros"),
                                                monto = inv.optDouble("total_amount", 0.0).coerceAtMost(2000.0),
                                                esIngreso = inv.optString("type") == "INFLOW" || inv.optString("invoice_type") == "Ingreso",
                                                origen = "belvo",
                                                externalId = inv.optString("id")
                                            )
                                        )
                                    }
                                    onResultado("Se importaron ${results.length()} facturas")
                                } catch (e: Exception) {
                                    onResultado("Error leyendo facturas: ${e.message}")
                                }
                            }
                        }
                        override fun onError(error: String) = onResultado("Error trayendo facturas: $error")
                    })
                }, 8000) // espera 4 segundos antes de pedir las facturas
            }
            override fun onError(error: String) = onResultado("Error creando link: $error")
        })
    }

    fun sembrarDatosDemo() = viewModelScope.launch {
        if (movimientos.value.isEmpty()) {
            dao.insertar(Movimiento(titulo = "Nómina", categoria = "Ingreso", monto = 25000.0, esIngreso = true))
            dao.insertar(Movimiento(titulo = "Supermercado", categoria = "Comida", monto = 1200.0, esIngreso = false))
            dao.insertar(Movimiento(titulo = "Netflix", categoria = "Entretenimiento", monto = 299.0, esIngreso = false))
            dao.guardarPresupuesto(Presupuesto("Comida", 2800.0))
            dao.guardarPresupuesto(Presupuesto("Transporte", 1500.0))
            dao.guardarPresupuesto(Presupuesto("Entretenimiento", 700.0))
        }
    }
}