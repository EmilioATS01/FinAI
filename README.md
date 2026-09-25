# 💸 FINAI — README (guía rápida, paso a paso)

Ya tienes creados: `data/`, `network/` (interfaces vacías), `ui/` (fragments con su xml), `viewmodel/FinanzasViewModel.kt` y todas las Activities. Esta guía te da, **en orden**, el código exacto que va en cada archivo para que todo funcione.

> Ve tachando cada paso conforme lo termines. No saltes pasos: cada uno depende del anterior.

---

## Paso 1 — `build.gradle.kts` (módulo `app`)

Abre `app/build.gradle.kts` y asegúrate de tener esto:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" // ajusta si Android Studio te pide otra versión
}

import java.util.Properties
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

android {
    defaultConfig {
        buildConfigField("String", "OPENAI_KEY", "\"${localProps["OPENAI_KEY"] ?: ""}\"")
        buildConfigField("String", "EXCHANGE_KEY", "\"${localProps["EXCHANGE_KEY"] ?: ""}\"")
        buildConfigField("String", "BELVO_ID", "\"${localProps["BELVO_ID"] ?: ""}\"")
        buildConfigField("String", "BELVO_SECRET", "\"${localProps["BELVO_SECRET"] ?: ""}\"")
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("com.google.android.material:material:1.12.0")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
}
```

Dale clic a **"Sync Now"** arriba a la derecha cuando lo pegues.

## Paso 2 — `local.properties`

En la raíz del proyecto (junto a `settings.gradle.kts`), crea o edita `local.properties` y agrega tus llaves:

```properties
OPENAI_KEY=sk-xxxxxxxx
EXCHANGE_KEY=xxxxxxxx
BELVO_ID=xxxxxxxx
BELVO_SECRET=xxxxxxxx
```

Nunca subas este archivo a GitHub (ya viene ignorado por defecto).

## Paso 3 — `AndroidManifest.xml`

Verifica que tengas esto (agrega lo que falte):

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.CAMERA" />

<application ...>
    <activity android:name=".MainActivity" android:exported="true">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
    </activity>
    <activity android:name=".LoginActivity" />
    <activity android:name=".RegisterActivity" />
    <activity android:name=".HomeActivity" />
    <activity android:name=".AIAssistantActivity" />
    <activity android:name=".CameraActivity" />
    <activity android:name=".SensorsActivity" />
</application>
```

Quita cualquier `<activity>` de `MovimientosActivity`, `MetasActivity`, `PerfilActivity` si ya los borraste.

---

## Paso 4 — `Utils.kt` (raíz del paquete `emilio.tolosa.finai`)

Funciones que usarás en toda la app: hashear contraseñas y formatear dinero.

```kotlin
package emilio.tolosa.finai

import java.security.MessageDigest
import java.text.NumberFormat
import java.util.Locale

fun String.sha256(): String =
    MessageDigest.getInstance("SHA-256").digest(toByteArray())
        .joinToString("") { "%02x".format(it) }

fun Double.mx(): String =
    NumberFormat.getCurrencyInstance(Locale("es", "MX")).format(this)
```

---

## Paso 5 — Completar `data/Movimiento.kt`

Debe tener estas 3 tablas + 1 clase auxiliar (si ya tienes `Movimiento`, agrégale los campos que falten):

```kotlin
package emilio.tolosa.finai.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "movimientos",
    indices = [Index(value = ["externalId"], unique = true)]
)
data class Movimiento(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val titulo: String,
    val categoria: String,
    val monto: Double,
    val esIngreso: Boolean,
    val fecha: Long = System.currentTimeMillis(),
    val origen: String = "manual",
    val externalId: String? = null
)

@Entity(tableName = "metas")
data class Meta(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val objetivo: Double,
    val ahorrado: Double = 0.0
)

@Entity(tableName = "presupuestos")
data class Presupuesto(
    @PrimaryKey val categoria: String,
    val limite: Double
)

data class GastoCategoria(val categoria: String, val total: Double)
```

## Paso 6 — Completar `data/MovimientoDao.kt`

```kotlin
package emilio.tolosa.finai.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MovimientoDao {

    @Query("SELECT * FROM movimientos ORDER BY fecha DESC")
    fun todos(): Flow<List<Movimiento>>

    @Query("SELECT * FROM movimientos ORDER BY fecha DESC LIMIT :n")
    fun ultimos(n: Int): Flow<List<Movimiento>>

    @Query("SELECT COALESCE(SUM(monto),0) FROM movimientos WHERE esIngreso = 1")
    fun totalIngresos(): Flow<Double>

    @Query("SELECT COALESCE(SUM(monto),0) FROM movimientos WHERE esIngreso = 0")
    fun totalGastos(): Flow<Double>

    @Query("""SELECT categoria, SUM(monto) AS total FROM movimientos
              WHERE esIngreso = 0 GROUP BY categoria""")
    fun gastosPorCategoria(): Flow<List<GastoCategoria>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertar(m: Movimiento)

    @Delete
    suspend fun borrar(m: Movimiento)

    @Query("SELECT * FROM metas")
    fun metas(): Flow<List<Meta>>
    @Insert suspend fun insertarMeta(m: Meta)
    @Update suspend fun actualizarMeta(m: Meta)

    @Query("SELECT * FROM presupuestos")
    fun presupuestos(): Flow<List<Presupuesto>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarPresupuesto(p: Presupuesto)
}
```

## Paso 7 — Completar `data/MovimientoDatabase.kt`

```kotlin
package emilio.tolosa.finai.data

import android.content.Context
import androidx.room.*

@Database(
    entities = [Movimiento::class, Meta::class, Presupuesto::class],
    version = 1,
    exportSchema = false
)
abstract class MovimientoDatabase : RoomDatabase() {
    abstract fun dao(): MovimientoDao

    companion object {
        @Volatile private var INSTANCE: MovimientoDatabase? = null

        fun get(context: Context): MovimientoDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    MovimientoDatabase::class.java,
                    "finai.db"
                ).fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}
```

> ⚠️ Si ya habías corrido la app antes con otras tablas, **desinstala la app del emulador** antes de volver a correrla.

## Paso 8 — Completar `data/DataStoreManager.kt`

```kotlin
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

    suspend fun cerrarSesion() = context.dataStore.edit { it[Keys.SESION] = false }
    suspend fun guardarMoneda(m: String) = context.dataStore.edit { it[Keys.MONEDA] = m }
    suspend fun guardarPresupuesto(v: Double) = context.dataStore.edit { it[Keys.PRESUPUESTO] = v }
}
```

---

## Paso 9 — Las 3 APIs (`network/`)

### 9.1 `network/ExchangeApi.kt`

```kotlin
package emilio.tolosa.finai.network

import retrofit2.http.GET
import retrofit2.http.Path

interface ExchangeApi {
    @GET("v6/{key}/latest/{base}")
    suspend fun latest(@Path("key") key: String, @Path("base") base: String): ExchangeResponse
}

data class ExchangeResponse(
    val result: String,
    val base_code: String,
    val conversion_rates: Map<String, Double>
)
```

### 9.2 `network/OpenAiApi.kt`

```kotlin
package emilio.tolosa.finai.network

import retrofit2.http.Body
import retrofit2.http.POST

interface OpenAiApi {
    @POST("v1/chat/completions")
    suspend fun chat(@Body body: ChatRequest): ChatResponse
}

data class ChatMessage(val role: String, val content: Any)
data class ChatRequest(val model: String = "gpt-4o-mini", val messages: List<ChatMessage>)
data class ChatResponse(val choices: List<Choice>)
data class Choice(val message: ChatMessage)
```

### 9.3 `network/BelvoApi.kt`

```kotlin
package emilio.tolosa.finai.network

import retrofit2.http.Body
import retrofit2.http.POST

interface BelvoApi {
    @POST("api/links/")
    suspend fun crearLink(@Body body: LinkRequest): LinkResponse

    @POST("api/transactions/")
    suspend fun transacciones(@Body body: TxRequest): List<BelvoTx>
}

data class LinkRequest(
    val institution: String,
    val username: String,
    val password: String,
    val access_mode: String = "single"
)
data class LinkResponse(val id: String)
data class TxRequest(val link: String, val date_from: String, val date_to: String)
data class BelvoTx(
    val id: String,
    val amount: Double,
    val type: String,
    val description: String?,
    val category: String?,
    val value_date: String?
)
```

> Confirma los datos de "institution/username/password" de sandbox en la documentación oficial de Belvo (developers.belvo.com) antes de probarlo; cambian con el tiempo.

### 9.4 `network/ApiClient.kt`

```kotlin
package emilio.tolosa.finai.network

import emilio.tolosa.finai.BuildConfig
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private val log = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }

    private fun http(vararg extra: Interceptor) = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .apply { extra.forEach { addInterceptor(it) } }
        .addInterceptor(log)
        .build()

    private fun retrofit(base: String, client: OkHttpClient) = Retrofit.Builder()
        .baseUrl(base)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val exchange: ExchangeApi by lazy {
        retrofit("https://v6.exchangerate-api.com/", http()).create(ExchangeApi::class.java)
    }

    val openai: OpenAiApi by lazy {
        val auth = Interceptor { chain ->
            chain.proceed(chain.request().newBuilder()
                .header("Authorization", "Bearer ${BuildConfig.OPENAI_KEY}").build())
        }
        retrofit("https://api.openai.com/", http(auth)).create(OpenAiApi::class.java)
    }

    val belvo: BelvoApi by lazy {
        val cred = Credentials.basic(BuildConfig.BELVO_ID, BuildConfig.BELVO_SECRET)
        val auth = Interceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("Authorization", cred).build())
        }
        retrofit("https://sandbox.belvo.com/", http(auth)).create(BelvoApi::class.java)
    }
}
```

---

## Paso 10 — `viewmodel/FinanzasViewModel.kt` (completo)

```kotlin
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
```

---

## Paso 11 — Login / Registro / Router

### 11.1 `MainActivity.kt`

```kotlin
package emilio.tolosa.finai

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
            val activa = DataStoreManager(this@MainActivity).sesionActiva.first()
            val destino = if (activa) HomeActivity::class.java else LoginActivity::class.java
            startActivity(Intent(this@MainActivity, destino))
            finish()
        }
    }
}
```

### 11.2 `activity_login.xml` — IDs necesarios

Confirma que tu layout tenga: `etEmail`, `etPassword`, `btnLogin`, `tvRegistro`.

### 11.3 `LoginActivity.kt`

```kotlin
package emilio.tolosa.finai

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import emilio.tolosa.finai.data.DataStoreManager
import emilio.tolosa.finai.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var b: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)
        val store = DataStoreManager(this)

        b.btnLogin.setOnClickListener {
            val email = b.etEmail.text.toString().trim()
            val pass = b.etPassword.text.toString()
            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Completa los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                if (store.login(email, pass.sha256())) {
                    startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this@LoginActivity, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
                }
            }
        }
        b.tvRegistro.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
```

### 11.4 `activity_register.xml` — IDs necesarios

`etNombre`, `etEmail`, `etPassword`, `btnRegistrar`.

### 11.5 `RegisterActivity.kt`

```kotlin
package emilio.tolosa.finai

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import emilio.tolosa.finai.data.DataStoreManager
import emilio.tolosa.finai.databinding.ActivityRegisterBinding
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    private lateinit var b: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnRegistrar.setOnClickListener {
            val nombre = b.etNombre.text.toString().trim()
            val email = b.etEmail.text.toString().trim()
            val pass = b.etPassword.text.toString()

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches() || pass.length < 6 || nombre.isEmpty()) {
                Toast.makeText(this, "Revisa los datos (contraseña mínimo 6)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                DataStoreManager(this@RegisterActivity).registrar(nombre, email, pass.sha256())
                startActivity(Intent(this@RegisterActivity, HomeActivity::class.java))
                finishAffinity()
            }
        }
    }
}
```

---

## Paso 12 — `HomeActivity` (contenedor de fragments + barra inferior)

### 12.1 `res/menu/bottom_nav.xml` (créalo: clic derecho en `res` → New → Android Resource File → Resource type: Menu)

```xml
<menu xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:id="@+id/nav_inicio" android:icon="@drawable/ic_home" android:title="Inicio"/>
    <item android:id="@+id/nav_movimientos" android:icon="@drawable/ic_swap" android:title="Movimientos"/>
    <item android:id="@+id/nav_presupuesto" android:icon="@drawable/ic_pie" android:title="Presupuesto"/>
    <item android:id="@+id/nav_metas" android:icon="@drawable/ic_target" android:title="Metas"/>
    <item android:id="@+id/nav_perfil" android:icon="@drawable/ic_person" android:title="Perfil"/>
</menu>
```

Crea los íconos: clic derecho en `drawable` → New → Vector Asset → busca "home", "swap horiz", "pie chart", "flag", "person", "auto awesome" (para `ic_sparkle`).

### 12.2 `activity_home.xml`

```xml
<androidx.coordinatorlayout.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent" android:layout_height="match_parent">

    <FrameLayout
        android:id="@+id/contenedor"
        android:layout_width="match_parent" android:layout_height="match_parent"
        android:paddingBottom="64dp"/>

    <com.google.android.material.bottomnavigation.BottomNavigationView
        android:id="@+id/bottomNav"
        android:layout_width="match_parent" android:layout_height="wrap_content"
        android:layout_gravity="bottom"
        app:menu="@menu/bottom_nav"/>

    <com.google.android.material.floatingactionbutton.FloatingActionButton
        android:id="@+id/fabIa"
        android:layout_width="wrap_content" android:layout_height="wrap_content"
        android:layout_gravity="bottom|end"
        android:layout_marginEnd="16dp" android:layout_marginBottom="80dp"
        android:src="@drawable/ic_sparkle"/>
</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

### 12.3 `HomeActivity.kt`

```kotlin
package emilio.tolosa.finai

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import emilio.tolosa.finai.databinding.ActivityHomeBinding
import emilio.tolosa.finai.ui.*

class HomeActivity : AppCompatActivity() {
    private lateinit var b: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(b.root)

        if (savedInstanceState == null) mostrar(HomeFragment())

        b.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> mostrar(HomeFragment())
                R.id.nav_movimientos -> mostrar(MovimientosFragment())
                R.id.nav_presupuesto -> mostrar(PresupuestoFragment())
                R.id.nav_metas -> mostrar(MetasFragment())
                R.id.nav_perfil -> mostrar(PerfilFragment())
            }
            true
        }
        b.fabIa.setOnClickListener {
            startActivity(Intent(this, AIAssistantActivity::class.java))
        }
    }

    private fun mostrar(f: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.contenedor, f)
            .commit()
    }
}
```

---

## Paso 13 — Fragments (código `.kt` completo)

Como ya tienes los `.xml` creados, solo confirma que los IDs coincidan con los que usa este código (si tu XML usa otros nombres de ID, cambia el código o el XML para que coincidan — lo importante es que sean iguales en ambos lados).

### 13.1 `ui/MovimientoAdapter.kt`

IDs esperados en `item_movimiento.xml`: `tvTitulo`, `tvDetalle`, `tvMonto`.

```kotlin
package emilio.tolosa.finai.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import emilio.tolosa.finai.data.Movimiento
import emilio.tolosa.finai.databinding.ItemMovimientoBinding
import emilio.tolosa.finai.mx

class MovimientoAdapter(
    private val onLongClick: ((Movimiento) -> Unit)? = null
) : ListAdapter<Movimiento, MovimientoAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<Movimiento>() {
        override fun areItemsTheSame(a: Movimiento, b: Movimiento) = a.id == b.id
        override fun areContentsTheSame(a: Movimiento, b: Movimiento) = a == b
    }

    inner class VH(val b: ItemMovimientoBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemMovimientoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(h: VH, position: Int) {
        val m = getItem(position)
        h.b.tvTitulo.text = m.titulo
        h.b.tvDetalle.text = m.categoria
        val signo = if (m.esIngreso) "+" else "-"
        h.b.tvMonto.text = "$signo${m.monto.mx()}"
        h.b.tvMonto.setTextColor(
            if (m.esIngreso) Color.parseColor("#F5A623") else Color.parseColor("#E5484D")
        )
        h.b.root.setOnLongClickListener { onLongClick?.invoke(m); true }
    }
}
```

### 13.2 `ui/HomeFragment.kt`

IDs esperados en `fragment_home.xml`: `tvBalance`, `tvIngresos`, `tvGastos`, `tvBalanceUsd`, `rvUltimos`.

```kotlin
package emilio.tolosa.finai.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import emilio.tolosa.finai.R
import emilio.tolosa.finai.databinding.FragmentHomeBinding
import emilio.tolosa.finai.mx
import emilio.tolosa.finai.viewmodel.FinanzasViewModel
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {

    private val vm: FinanzasViewModel by activityViewModels()
    private var _b: FragmentHomeBinding? = null
    private val b get() = _b!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _b = FragmentHomeBinding.bind(view)

        val adapter = MovimientoAdapter()
        b.rvUltimos.layoutManager = LinearLayoutManager(requireContext())
        b.rvUltimos.adapter = adapter

        vm.sembrarDatosDemo()
        vm.cargarTasas("MXN")

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { vm.balance.collect { b.tvBalance.text = it.mx() } }
                launch { vm.ingresos.collect { b.tvIngresos.text = it.mx() } }
                launch { vm.gastos.collect { b.tvGastos.text = it.mx() } }
                launch { vm.ultimos.collect { adapter.submitList(it) } }
                launch {
                    vm.tasas.collect { tasas ->
                        val usd = tasas["USD"] ?: return@collect
                        b.tvBalanceUsd.text = "≈ USD ${"%.2f".format(vm.balance.value * usd)}"
                    }
                }
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
```

### 13.3 `ui/NuevoMovimientoDialog.kt`

IDs esperados en `dialog_movimiento.xml`: `etTitulo`, `etMonto`, `spCategoria`, `swIngreso`.

```kotlin
package emilio.tolosa.finai.ui

import android.app.Dialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import emilio.tolosa.finai.data.Movimiento
import emilio.tolosa.finai.databinding.DialogMovimientoBinding
import emilio.tolosa.finai.viewmodel.FinanzasViewModel

class NuevoMovimientoDialog(
    private val tituloInicial: String = "",
    private val montoInicial: Double? = null,
    private val categoriaInicial: String? = null,
    private val origen: String = "manual"
) : DialogFragment() {

    private val vm: FinanzasViewModel by activityViewModels()
    private val categorias = listOf("Comida", "Transporte", "Entretenimiento", "Compras", "Salud", "Ingreso", "Otros")

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val b = DialogMovimientoBinding.inflate(layoutInflater)
        b.spCategoria.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categorias)
        b.etTitulo.setText(tituloInicial)
        montoInicial?.let { b.etMonto.setText(it.toString()) }
        categoriaInicial?.let { b.spCategoria.setSelection(categorias.indexOf(it).coerceAtLeast(0)) }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Nuevo movimiento")
            .setView(b.root)
            .setPositiveButton("Guardar") { _, _ ->
                val monto = b.etMonto.text.toString().toDoubleOrNull()
                if (monto == null || b.etTitulo.text.isNullOrBlank()) {
                    Toast.makeText(context, "Datos inválidos", Toast.LENGTH_SHORT).show()
                } else {
                    vm.agregarMovimiento(
                        Movimiento(
                            titulo = b.etTitulo.text.toString(),
                            categoria = b.spCategoria.selectedItem.toString(),
                            monto = monto,
                            esIngreso = b.swIngreso.isChecked,
                            origen = origen
                        )
                    )
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()
    }
}
```

### 13.4 `ui/MovimientosFragment.kt`

IDs esperados en `fragment_movimientos.xml`: `rvMovimientos`, `btnNuevo`.

```kotlin
package emilio.tolosa.finai.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import emilio.tolosa.finai.R
import emilio.tolosa.finai.databinding.FragmentMovimientosBinding
import emilio.tolosa.finai.viewmodel.FinanzasViewModel
import kotlinx.coroutines.launch

class MovimientosFragment : Fragment(R.layout.fragment_movimientos) {
    private val vm: FinanzasViewModel by activityViewModels()

    override fun onViewCreated(view: View, s: Bundle?) {
        val b = FragmentMovimientosBinding.bind(view)

        val adapter = MovimientoAdapter { m ->
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("¿Borrar ${m.titulo}?")
                .setPositiveButton("Borrar") { _, _ -> vm.borrarMovimiento(m) }
                .setNegativeButton("Cancelar", null).show()
        }
        b.rvMovimientos.layoutManager = LinearLayoutManager(requireContext())
        b.rvMovimientos.adapter = adapter

        b.btnNuevo.setOnClickListener { NuevoMovimientoDialog().show(childFragmentManager, "nuevo") }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.movimientos.collect { adapter.submitList(it) }
            }
        }
    }
}
```

### 13.5 `ui/PresupuestoAdapter.kt`

IDs esperados en `item_presupuesto.xml`: `tvCategoria`, `tvLimite`, `progressBar`, `tvUso`, `tvRestan`.

```kotlin
package emilio.tolosa.finai.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import emilio.tolosa.finai.databinding.ItemPresupuestoBinding
import emilio.tolosa.finai.mx
import emilio.tolosa.finai.viewmodel.PresupuestoUi

class PresupuestoAdapter : ListAdapter<PresupuestoUi, PresupuestoAdapter.VH>(Diff) {
    object Diff : DiffUtil.ItemCallback<PresupuestoUi>() {
        override fun areItemsTheSame(a: PresupuestoUi, b: PresupuestoUi) = a.categoria == b.categoria
        override fun areContentsTheSame(a: PresupuestoUi, b: PresupuestoUi) =
            a.limite == b.limite && a.gastado == b.gastado
    }
    inner class VH(val b: ItemPresupuestoBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(ItemPresupuestoBinding.inflate(LayoutInflater.from(p.context), p, false))

    override fun onBindViewHolder(h: VH, i: Int) {
        val p = getItem(i)
        h.b.tvCategoria.text = p.categoria
        h.b.tvLimite.text = p.limite.mx()
        h.b.progressBar.progress = p.progreso
        h.b.tvUso.text = "Uso: ${p.progreso}%"
        h.b.tvRestan.text = "Restan ${p.restante.mx()}"
        val color = if (p.progreso >= 90) "#E5484D" else "#4F5BFF"
        h.b.progressBar.setIndicatorColor(Color.parseColor(color))
    }
}
```

### 13.6 `ui/PresupuestoFragment.kt`

IDs esperados en `fragment_presupuesto.xml`: `tvPresupuestoMensual`, `rvPresupuestos`, `tvDisponible`, `btnEditar`.

```kotlin
package emilio.tolosa.finai.ui

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import emilio.tolosa.finai.R
import emilio.tolosa.finai.databinding.FragmentPresupuestoBinding
import emilio.tolosa.finai.mx
import emilio.tolosa.finai.viewmodel.FinanzasViewModel
import kotlinx.coroutines.launch

class PresupuestoFragment : Fragment(R.layout.fragment_presupuesto) {
    private val vm: FinanzasViewModel by activityViewModels()

    override fun onViewCreated(view: View, s: Bundle?) {
        val b = FragmentPresupuestoBinding.bind(view)
        val adapter = PresupuestoAdapter()
        b.rvPresupuestos.layoutManager = LinearLayoutManager(requireContext())
        b.rvPresupuestos.adapter = adapter

        b.btnEditar.setOnClickListener {
            val etCat = EditText(requireContext()).apply { hint = "Categoría" }
            val etLim = EditText(requireContext()).apply { hint = "Límite"; inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL }
            val layout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(40, 20, 40, 20)
                addView(etCat); addView(etLim)
            }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Editar presupuesto")
                .setView(layout)
                .setPositiveButton("Guardar") { _, _ ->
                    val cat = etCat.text.toString().trim()
                    val lim = etLim.text.toString().toDoubleOrNull()
                    if (cat.isEmpty() || lim == null) {
                        Toast.makeText(requireContext(), "Datos inválidos", Toast.LENGTH_SHORT).show()
                    } else vm.guardarPresupuesto(cat, lim)
                }
                .setNegativeButton("Cancelar", null).show()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { vm.presupuestos.collect { lista ->
                    adapter.submitList(lista)
                    b.tvDisponible.text = lista.sumOf { it.restante }.mx()
                } }
                launch { vm.store.presupuestoMensual.collect { b.tvPresupuestoMensual.text = it.mx() } }
            }
        }
    }
}
```

### 13.7 Adaptador de Metas: `ui/MetaAdapter.kt`

IDs esperados en `item_meta.xml`: `tvNombre`, `tvPorcentaje`, `progressMeta`, `tvAhorrado`, `tvObjetivo`, `btnAbonar`.

```kotlin
package emilio.tolosa.finai.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import emilio.tolosa.finai.data.Meta
import emilio.tolosa.finai.databinding.ItemMetaBinding
import emilio.tolosa.finai.mx

class MetaAdapter(
    private val onAbonar: (Meta) -> Unit
) : ListAdapter<Meta, MetaAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<Meta>() {
        override fun areItemsTheSame(a: Meta, b: Meta) = a.id == b.id
        override fun areContentsTheSame(a: Meta, b: Meta) = a == b
    }
    inner class VH(val b: ItemMetaBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(ItemMetaBinding.inflate(LayoutInflater.from(p.context), p, false))

    override fun onBindViewHolder(h: VH, i: Int) {
        val m = getItem(i)
        val pct = ((m.ahorrado / m.objetivo) * 100).toInt().coerceIn(0, 100)
        h.b.tvNombre.text = m.nombre
        h.b.tvPorcentaje.text = "$pct%"
        h.b.progressMeta.progress = pct
        h.b.tvAhorrado.text = "Ahorrado: ${m.ahorrado.mx()}"
        h.b.tvObjetivo.text = "Meta: ${m.objetivo.mx()}"
        h.b.btnAbonar.setOnClickListener { onAbonar(m) }
    }
}
```

### 13.8 `ui/MetasFragment.kt`

IDs esperados en `fragment_metas.xml`: `rvMetas`, `btnNuevaMeta`.

```kotlin
package emilio.tolosa.finai.ui

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import emilio.tolosa.finai.R
import emilio.tolosa.finai.databinding.FragmentMetasBinding
import emilio.tolosa.finai.viewmodel.FinanzasViewModel
import kotlinx.coroutines.launch

class MetasFragment : Fragment(R.layout.fragment_metas) {
    private val vm: FinanzasViewModel by activityViewModels()

    override fun onViewCreated(view: View, s: Bundle?) {
        val b = FragmentMetasBinding.bind(view)

        val adapter = MetaAdapter { meta ->
            val et = EditText(requireContext()).apply { hint = "Monto a abonar" }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Abonar a ${meta.nombre}")
                .setView(LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL; setPadding(40, 20, 40, 20); addView(et)
                })
                .setPositiveButton("Abonar") { _, _ ->
                    et.text.toString().toDoubleOrNull()?.let { vm.abonarMeta(meta, it) }
                        ?: Toast.makeText(requireContext(), "Monto inválido", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancelar", null).show()
        }
        b.rvMetas.layoutManager = LinearLayoutManager(requireContext())
        b.rvMetas.adapter = adapter

        b.btnNuevaMeta.setOnClickListener {
            val etNombre = EditText(requireContext()).apply { hint = "Nombre de la meta" }
            val etObjetivo = EditText(requireContext()).apply { hint = "Objetivo ($)" }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Nueva meta")
                .setView(LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL; setPadding(40, 20, 40, 20)
                    addView(etNombre); addView(etObjetivo)
                })
                .setPositiveButton("Crear") { _, _ ->
                    val obj = etObjetivo.text.toString().toDoubleOrNull()
                    if (etNombre.text.isNullOrBlank() || obj == null) {
                        Toast.makeText(requireContext(), "Datos inválidos", Toast.LENGTH_SHORT).show()
                    } else vm.agregarMeta(etNombre.text.toString(), obj)
                }
                .setNegativeButton("Cancelar", null).show()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.metas.collect { adapter.submitList(it) }
            }
        }
    }
}
```

### 13.9 `ui/PerfilFragment.kt`

IDs esperados en `fragment_perfil.xml`: `tvNombre`, `tvEmail`, `spMoneda`, `btnCerrarSesion`.

```kotlin
package emilio.tolosa.finai.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import emilio.tolosa.finai.LoginActivity
import emilio.tolosa.finai.R
import emilio.tolosa.finai.databinding.FragmentPerfilBinding
import emilio.tolosa.finai.viewmodel.FinanzasViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PerfilFragment : Fragment(R.layout.fragment_perfil) {
    private val vm: FinanzasViewModel by activityViewModels()

    override fun onViewCreated(view: View, s: Bundle?) {
        val b = FragmentPerfilBinding.bind(view)

        viewLifecycleOwner.lifecycleScope.launch {
            b.tvNombre.text = vm.store.nombre.first()
            b.tvEmail.text = vm.store.email.first()
        }

        val monedas = listOf("MXN", "USD", "EUR")
        b.spMoneda.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, monedas)
        b.spMoneda.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                viewLifecycleOwner.lifecycleScope.launch { vm.store.guardarMoneda(monedas[pos]) }
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        b.btnCerrarSesion.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                vm.store.cerrarSesion()
                startActivity(Intent(requireContext(), LoginActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }
}
```

---

## Paso 14 — Asistente de IA (`AIAssistantActivity`)

IDs necesarios en `activity_ai_assistant.xml`: `rvChat` (RecyclerView), `etMensaje` (EditText), `btnEnviar` (Button), `progress` (ProgressBar).

### 14.1 `ui/ChatAdapter.kt` (simple, lista de textos)

```kotlin
package emilio.tolosa.finai.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter : RecyclerView.Adapter<ChatAdapter.VH>() {
    private val mensajes = mutableListOf<String>()

    inner class VH(val tv: TextView) : RecyclerView.ViewHolder(tv)

    fun submit(lista: List<String>) {
        mensajes.clear(); mensajes.addAll(lista)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val tv = TextView(parent.context).apply { setPadding(24, 16, 24, 16); textSize = 15f }
        return VH(tv)
    }
    override fun onBindViewHolder(holder: VH, position: Int) { holder.tv.text = mensajes[position] }
    override fun getItemCount() = mensajes.size
}
```

### 14.2 `AIAssistantActivity.kt`

```kotlin
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
```

---

## Paso 15 — Cámara (`CameraActivity`)

IDs necesarios en `activity_camera.xml`: `btnFoto` (Button).

```kotlin
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
```

---

## Paso 16 — Sensor de agitar (`SensorsActivity` o dentro de `HomeActivity`)

### 16.1 `ShakeDetector.kt` (raíz del paquete)

```kotlin
package emilio.tolosa.finai

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.pow
import kotlin.math.sqrt

class ShakeDetector(private val onShake: () -> Unit) : SensorEventListener {
    private var ultimo = 0L
    override fun onSensorChanged(e: SensorEvent) {
        val g = sqrt(e.values[0].pow(2) + e.values[1].pow(2) + e.values[2].pow(2)) / SensorManager.GRAVITY_EARTH
        val ahora = System.currentTimeMillis()
        if (g > 2.5f && ahora - ultimo > 1000) { ultimo = ahora; onShake() }
    }
    override fun onAccuracyChanged(s: Sensor?, a: Int) {}
}
```

### 16.2 Agrégalo a `HomeActivity.kt` (dentro de la misma clase del Paso 12.3)

```kotlin
import android.content.Context.SENSOR_SERVICE
import android.hardware.Sensor
import android.hardware.SensorManager
import emilio.tolosa.finai.ui.NuevoMovimientoDialog

// dentro de la clase HomeActivity, agrega:
private lateinit var sm: SensorManager
private val shake = ShakeDetector {
    NuevoMovimientoDialog(origen = "sensor").show(supportFragmentManager, "shake")
}

override fun onResume() {
    super.onResume()
    sm = getSystemService(SENSOR_SERVICE) as SensorManager
    sm.registerListener(shake, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI)
}
override fun onPause() { super.onPause(); sm.unregisterListener(shake) }
```

En el emulador, simula el sensor desde los **Extended Controls (⋮) → Virtual sensors → Accelerometer**.

---

## Paso 17 — Colores (`res/values/colors.xml`)

```xml
<resources>
    <color name="primary">#4F5BFF</color>
    <color name="bg">#F5F3EE</color>
    <color name="card">#FFFFFF</color>
    <color name="income">#22A06B</color>
    <color name="expense">#E5484D</color>
    <color name="warning">#F5A623</color>
    <color name="text_secondary">#6B7280</color>
</resources>
```

---

## ✅ Orden para probar que todo funcione

1. Compila (Sync + Build) sin tocar ninguna Activity todavía → debe compilar sin errores.
2. Corre la app → debe ir a Login (o Home si ya había sesión).
3. Regístrate → debe entrar a Home y ver la barra inferior con 5 pestañas.
4. En Inicio, deben aparecer los 3 movimientos de ejemplo (`sembrarDatosDemo`).
5. Ve a Movimientos → "+ Nuevo movimiento" → agrega uno → revisa que aparezca en la lista y en Inicio.
6. Ve a Presupuesto → "Editar presupuesto" → agrega una categoría con límite → revisa la barra de progreso.
7. Ve a Metas → "+ Nueva meta" → agrega una → "Abonar" → revisa que suba el porcentaje.
8. Ve a Perfil → cambia moneda → cierra sesión → debe regresar a Login.
9. Toca el botón flotante (✨) → abre el Asistente de IA → escribe algo → debe responder (aquí es donde se prueba tu llave de OpenAI).
10. Prueba Cámara y Sensores al final, son extras.

---

## 🛠️ Errores comunes

| Error | Causa | Solución |
|---|---|---|
| `Unresolved reference: FragmentHomeBinding` | ViewBinding apagado | Revisa `viewBinding = true` en Paso 1 y sincroniza |
| `BuildConfig.OPENAI_KEY` no existe | Falta `buildConfig = true` | Revisa Paso 1 |
| Se cierra la app al entrar a una pestaña | El ID del código no existe en tu XML | Revisa que los nombres de ID coincidan exactamente (mayúsculas incluidas) |
| Room: no compila / "Cannot find implementation" | Falta plugin KSP o versión no coincide | Revisa Paso 1 y la versión de Kotlin de tu proyecto |
| 401 en OpenAI | Llave mal copiada o sin saldo | Revisa `local.properties` |
| Pantalla en blanco en un Fragment | El `.kt` no apunta a su layout (`R.layout.fragment_x`) | Revisa el constructor `Fragment(R.layout....)` |

¡Éxito con Finai! 🚀

