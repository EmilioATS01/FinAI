package emilio.tolosa.finai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import emilio.tolosa.finai.BuildConfig
import emilio.tolosa.finai.data.*
import emilio.tolosa.finai.network.*
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

    private val _tasas = MutableStateFlow<Map<String, Double>>(emptyMap())
    val tasas: StateFlow<Map<String, Double>> = _tasas

    fun agregarMovimiento(m: Movimiento) = viewModelScope.launch { dao.insertar(m) }
    fun borrarMovimiento(m: Movimiento) = viewModelScope.launch { dao.borrar(m) }
    fun agregarMeta(nombre: String, objetivo: Double) =
        viewModelScope.launch { dao.insertarMeta(Meta(nombre = nombre, objetivo = objetivo)) }
    fun abonarMeta(meta: Meta, monto: Double) =
        viewModelScope.launch { dao.actualizarMeta(meta.copy(ahorrado = meta.ahorrado + monto)) }
    fun guardarPresupuesto(cat: String, limite: Double) =
        viewModelScope.launch { dao.guardarPresupuesto(Presupuesto(cat, limite)) }

    fun cargarTasas(base: String) = viewModelScope.launch {
        try {
            val r = ApiClient.exchange.latest(BuildConfig.EXCHANGE_KEY, base)
            if (r.result == "success") _tasas.value = r.conversion_rates
        } catch (_: Exception) { }
    }

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

    fun importarDeBelvo(onResultado: (String) -> Unit) = viewModelScope.launch {
        try {
            val link = ApiClient.belvo.crearLink(
                LinkRequest(institution = "INSTITUCION_SANDBOX", username = "USUARIO_PRUEBA", password = "PASS_PRUEBA")
            )
            val txs = ApiClient.belvo.transacciones(
                TxRequest(link.id, date_from = "2026-08-01", date_to = "2026-09-23")
            )
            txs.forEach { t ->
                dao.insertar(
                    Movimiento(
                        titulo = t.description ?: "Movimiento bancario",
                        categoria = t.category ?: "Otros",
                        monto = kotlin.math.abs(t.amount),
                        esIngreso = t.type == "INFLOW",
                        origen = "belvo",
                        externalId = t.id
                    )
                )
            }
            onResultado("Se importaron ${txs.size} movimientos")
        } catch (e: Exception) {
            onResultado("Error con Belvo: ${e.message}")
        }
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