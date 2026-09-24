package emilio.tolosa.finai.viewmodel

import android.app.Application
import androidx.datastore.preferences.protobuf.LazyStringArrayList.emptyList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import emilio.tolosa.finai.data.DataStoreManager
import emilio.tolosa.finai.data.Meta
import emilio.tolosa.finai.data.Movimiento
import emilio.tolosa.finai.data.MovimientoDatabase
import emilio.tolosa.finai.data.Presupuesto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.collections.emptyList

class PresupuestoUi(val categoria: String, val limite: Double, val gastado: Double) {
    val restante get() = limite - gastado
    val progreso get() = if (limite > 0) ((gastado / limite) * 100).toInt().coerceIn(0, 100) else 0
}

class FinanzasViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = MovimientoDatabase.get(app).dao()
    val store = DataStoreManager(app)

    private fun <T> Flow<T>.estado(inicial: T) =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), inicial)

    // ---- Datos de Room ----
    val movimientos = dao.todos().estado(emptyList())
    val ultimos = dao.ultimos(5).estado(emptyList())
    val ingresos = dao.totalIngresos().estado(0.0)
    val gastos = dao.totalGastos().estado(0.0)
    val balance = combine(ingresos, gastos) { i, g -> i - g }.estado(0.0)
    val metas = dao.metas().estado(emptyList())

    // Presupuesto por categoría: une límite (tabla presupuestos) con gasto real
    val presupuestos = combine(dao.presupuestos(), dao.gastosPorCategoria()) { pres, gastos ->
        pres.map { p ->
            PresupuestoUi(p.categoria, p.limite, gastos.find { it.categoria == p.categoria }?.total ?: 0.0)
        }
    }.estado(emptyList())

    // ---- Acciones ----
    fun agregarMovimiento(m: Movimiento) = viewModelScope.launch { dao.insertar(m) }
    fun borrarMovimiento(m: Movimiento) = viewModelScope.launch { dao.borrar(m) }
    fun agregarMeta(nombre: String, objetivo: Double) =
        viewModelScope.launch { dao.insertarMeta(Meta(nombre = nombre, objetivo = objetivo)) }
    fun abonarMeta(meta: Meta, monto: Double) =
        viewModelScope.launch { dao.actualizarMeta(meta.copy(ahorrado = meta.ahorrado + monto)) }
    fun guardarPresupuesto(cat: String, limite: Double) =
        viewModelScope.launch { dao.guardarPresupuesto(Presupuesto(cat, limite)) }

    // Datos de ejemplo la primera vez (útil para que se vea como tu diseño)
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