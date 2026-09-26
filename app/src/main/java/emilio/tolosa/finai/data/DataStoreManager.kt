package emilio.tolosa.finai.data


import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.*

private val Context.dataStore by preferencesDataStore(name = "finai_prefs")

class DataStoreManager(private val context: Context) {

    private object Keys {
        val NOMBRE = stringPreferencesKey("nombre")
        val EMAIL = stringPreferencesKey("email")
        val PASS = stringPreferencesKey("pass_hash")
        val SESION = booleanPreferencesKey("sesion")
        val MONEDA = stringPreferencesKey("moneda")
        val PRESUPUESTO = doublePreferencesKey("presupuesto_mensual")
    }

    val sesionActiva: Flow<Boolean> = context.dataStore.data.map { it[Keys.SESION] ?: false }
    val nombre: Flow<String> = context.dataStore.data.map { it[Keys.NOMBRE] ?: "" }
    val email: Flow<String> = context.dataStore.data.map { it[Keys.EMAIL] ?: "" }
    val moneda: Flow<String> = context.dataStore.data.map { it[Keys.MONEDA] ?: "MXN" }
    val presupuestoMensual: Flow<Double> =
        context.dataStore.data.map { it[Keys.PRESUPUESTO] ?: 10000.0 }

    suspend fun registrar(nombre: String, email: String, passHash: String) {
        context.dataStore.edit {
            it[Keys.NOMBRE] = nombre
            it[Keys.EMAIL] = email
            it[Keys.PASS] = passHash
            it[Keys.SESION] = true
        }
    }

    suspend fun login(email: String, passHash: String): Boolean {
        val p = context.dataStore.data.first()
        val ok = p[Keys.EMAIL] == email && p[Keys.PASS] == passHash
        if (ok) context.dataStore.edit { it[Keys.SESION] = true }
        return ok
    }

    suspend fun activarSesion() = context.dataStore.edit { it[Keys.SESION] = true }

    suspend fun cerrarSesion() = context.dataStore.edit { it[Keys.SESION] = false }
    suspend fun guardarMoneda(m: String) = context.dataStore.edit { it[Keys.MONEDA] = m }
    suspend fun guardarPresupuesto(v: Double) = context.dataStore.edit { it[Keys.PRESUPUESTO] = v }
}
