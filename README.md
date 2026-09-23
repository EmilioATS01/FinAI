# 💸 FINAI — Guía paso a paso (Android Studio + Kotlin)

App de finanzas personales con **Activities + Fragments**, **sin backend**, usando **OpenAI**, **Belvo** y **ExchangeRate**.

> Paquete del proyecto: `emilio.tolosa.finai`
> Nota: las versiones de librerías cambian seguido. Usa las que Android Studio te sugiera si alguna ya está desactualizada.

---

## 0. Visión general

### ¿Qué vamos a construir?

Según tus diseños:

| Pantalla | Qué hace | Tipo |
|---|---|---|
| Login / Registro | Iniciar sesión y crear cuenta (local) | Activity |
| Inicio | Balance, ingresos, gastos, ahorro y últimos movimientos | Fragment |
| Movimientos | Lista de ingresos/egresos + botón "Nuevo movimiento" | Fragment |
| Presupuesto | Presupuesto mensual y barras por categoría | Fragment |
| Metas | Metas de ahorro con porcentaje | Fragment |
| Perfil | Datos del usuario, moneda, cerrar sesión | Fragment |
| Asistente IA | Chat con OpenAI que conoce tus finanzas | Activity |
| Cámara | Foto de un ticket → la IA extrae el gasto | Activity |
| Sensores | Agitar el celular para agregar movimiento | Activity |

### Arquitectura (simple, ideal para un proyecto escolar)

```
┌──────────────────────────────────────────────┐
│  UI: Activities + Fragments (XML + ViewBinding)│
└───────────────┬──────────────────────────────┘
                │ observa (StateFlow)
┌───────────────▼──────────────┐
│ FinanzasViewModel            │  ← lógica y cálculos
└───────┬──────────────┬───────┘
        │              │
┌───────▼──────┐  ┌────▼────────────────────────┐
│ Room (SQLite)│  │ APIs con Retrofit            │
│ + DataStore  │  │ OpenAI · Belvo · ExchangeRate│
└──────────────┘  └──────────────────────────────┘
```

### ¿Por qué esta estructura?

- **Room**: base de datos local. Como no hay backend, aquí viven tus movimientos, metas y presupuestos.
- **DataStore**: guarda datos pequeños (sesión iniciada, nombre, moneda). Reemplaza a SharedPreferences.
- **ViewModel**: guarda el estado de la pantalla y sobrevive a rotaciones. Los fragments comparten **un solo** ViewModel.
- **Retrofit**: librería para llamar APIs REST de forma sencilla.
- **Fragments**: cada pestaña de la barra inferior es un Fragment dentro de **una sola** Activity (`HomeActivity`). Así no se recrea toda la pantalla al cambiar de pestaña.

---

## 1. Reorganizar el proyecto

Hoy tienes `MovimientosActivity`, `MetasActivity` y `PerfilActivity` como Activities. Según tu diseño, todas comparten la barra inferior, así que conviene que sean **Fragments**.

### Estructura objetivo

```
emilio.tolosa.finai
├── data/
│   ├── DataStoreManager.kt
│   ├── Movimiento.kt          (Movimiento, Meta, Presupuesto)
│   ├── MovimientoDao.kt
│   └── MovimientoDatabase.kt
├── network/                   ← NUEVO
│   ├── ApiClient.kt
│   ├── OpenAiApi.kt
│   ├── BelvoApi.kt
│   └── ExchangeApi.kt
├── ui/                        ← NUEVO
│   ├── HomeFragment.kt
│   ├── MovimientosFragment.kt
│   ├── PresupuestoFragment.kt
│   ├── MetasFragment.kt
│   ├── PerfilFragment.kt
│   ├── MovimientoAdapter.kt
│   └── NuevoMovimientoDialog.kt
├── viewmodel/FinanzasViewModel.kt
├── LoginActivity.kt
├── RegisterActivity.kt
├── HomeActivity.kt            ← contenedor de fragments + barra inferior
├── AIAssistantActivity.kt
├── CameraActivity.kt
└── SensorsActivity.kt
```

### Pasos

1. Crea los paquetes `network` y `ui` (clic derecho en el paquete → New → Package).
2. Crea los Fragments con: clic derecho → New → Fragment → Fragment (Blank).
3. Copia el contenido de `activity_movimientos.xml`, `activity_metas.xml`, `activity_perfil.xml` a `fragment_movimientos.xml`, `fragment_metas.xml`, `fragment_perfil.xml` (quita la barra inferior de esos layouts: ahora vive en `activity_home.xml`).
4. Cuando todo funcione, borra `MovimientosActivity`, `MetasActivity`, `PerfilActivity` y sus layouts viejos (y su declaración en el Manifest).
5. `MainActivity` puede quedarse como pantalla de arranque que decide si ir a Login o Home (ver sección 5).

---

## 2. Dependencias y permisos

### 2.1 `app/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // KSP: procesa las anotaciones de Room. La versión debe coincidir con tu versión de Kotlin
    id("com.google.devtools.ksp") version "TU_VERSION_KSP"
}

import java.util.Properties

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

android {
    defaultConfig {
        // Las llaves NO van escritas en el código. Se leen de local.properties
        buildConfigField("String", "OPENAI_KEY", "\"${localProps["OPENAI_KEY"] ?: ""}\"")
        buildConfigField("String", "EXCHANGE_KEY", "\"${localProps["EXCHANGE_KEY"] ?: ""}\"")
        buildConfigField("String", "BELVO_ID", "\"${localProps["BELVO_ID"] ?: ""}\"")
        buildConfigField("String", "BELVO_SECRET", "\"${localProps["BELVO_SECRET"] ?: ""}\"")
    }
    buildFeatures {
        viewBinding = true   // acceder a las vistas sin findViewById
        buildConfig = true   // permite usar BuildConfig.OPENAI_KEY
    }
}

dependencies {
    // Room: base de datos local
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // DataStore: preferencias/sesión
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // ViewModel + corrutinas en el ciclo de vida
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.fragment:fragment-ktx:1.8.5")

    // UI Material (BottomNavigation, tarjetas, barras de progreso)
    implementation("com.google.android.material:material:1.12.0")

    // Retrofit: llamadas HTTP a las APIs
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
}
```

### 2.2 Llaves de las APIs → `local.properties`

Este archivo está en la raíz del proyecto y Android Studio ya lo ignora en Git.

```properties
OPENAI_KEY=sk-xxxxxxxx
EXCHANGE_KEY=xxxxxxxx
BELVO_ID=xxxxxxxx
BELVO_SECRET=xxxxxxxx
```

> ⚠️ **Sin backend, las llaves viajan dentro del APK** y alguien podría extraerlas. Para un proyecto escolar es aceptable si:
> - Nunca subes `local.properties` a GitHub.
> - En OpenAI pones un **límite de gasto** bajo (por ejemplo, 5 USD).
> - Belvo lo usas solo en **sandbox** (datos falsos).
>
> En un proyecto real, las llaves van en un servidor. Menciónalo en tu presentación: demuestra que entiendes la limitación.

### 2.3 `AndroidManifest.xml`

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

- `INTERNET`: sin esto ninguna API funciona.
- `CAMERA`: para tomar fotos de tickets.

---

## 3. Base de datos local (Room)

### ¿Para qué sirve?

Guarda tus movimientos, metas y presupuestos aunque cierres la app. Tiene 3 piezas:
- **Entity**: una tabla (clase de Kotlin).
- **DAO**: las consultas SQL.
- **Database**: une todo y crea la base.

### 3.1 `Movimiento.kt` (entidades)

```kotlin
package emilio.tolosa.finai.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "movimientos",
    indices = [Index(value = ["externalId"], unique = true)] // evita duplicar transacciones de Belvo
)
data class Movimiento(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val titulo: String,            // "Netflix"
    val categoria: String,         // "Entretenimiento"
    val monto: Double,             // siempre positivo
    val esIngreso: Boolean,        // true = ingreso, false = gasto
    val fecha: Long = System.currentTimeMillis(),
    val origen: String = "manual", // manual | camara | belvo | sensor
    val externalId: String? = null // id de Belvo (si viene de allá)
)

@Entity(tableName = "metas")
data class Meta(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,     // "Laptop"
    val objetivo: Double,   // 25000
    val ahorrado: Double = 0.0
)

@Entity(tableName = "presupuestos")
data class Presupuesto(
    @PrimaryKey val categoria: String, // "Comida"
    val limite: Double                 // 2800
)

// Resultado de una consulta con SUM (no es tabla)
data class GastoCategoria(val categoria: String, val total: Double)
```

### 3.2 `MovimientoDao.kt`

```kotlin
package emilio.tolosa.finai.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MovimientoDao {

    // Flow = se actualiza solo cada vez que cambia la tabla
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

    // IGNORE: si ya existe el externalId, no lo inserta otra vez
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertar(m: Movimiento)

    @Delete
    suspend fun borrar(m: Movimiento)

    // Metas
    @Query("SELECT * FROM metas")
    fun metas(): Flow<List<Meta>>
    @Insert suspend fun insertarMeta(m: Meta)
    @Update suspend fun actualizarMeta(m: Meta)

    // Presupuestos
    @Query("SELECT * FROM presupuestos")
    fun presupuestos(): Flow<List<Presupuesto>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarPresupuesto(p: Presupuesto)
}
```

### 3.3 `MovimientoDatabase.kt`

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

        // Singleton: una sola instancia en toda la app
        fun get(context: Context): MovimientoDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    MovimientoDatabase::class.java,
                    "finai.db"
                )
                    .fallbackToDestructiveMigration() // si cambias tablas, recrea (solo para desarrollo)
                    .build().also { INSTANCE = it }
            }
    }
}
```

> Si ya tenías una versión anterior instalada en el emulador y cambias las tablas, **desinstala la app** o sube `version`.

---

## 4. DataStore (sesión y preferencias)

### ¿Para qué sirve?

Recordar quién inició sesión, su nombre y su moneda preferida sin usar una base de datos completa.

### `DataStoreManager.kt`

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

## 5. Login y Registro

### ¿Para qué sirve?

Sin backend no hay servidor que valide usuarios. La cuenta se guarda **localmente** en el teléfono. Es una simulación válida para un proyecto escolar (dilo así en tu exposición).

### 5.1 Función para hashear la contraseña (nunca guardes texto plano)

Crea `Utils.kt` en el paquete raíz:

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

### 5.2 `MainActivity` como "router" (sin pantalla)

```kotlin
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

### 5.3 `LoginActivity`

Los IDs de `activity_login.xml` que usaremos: `etEmail`, `etPassword`, `btnLogin`, `tvRegistro`.

```kotlin
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

### 5.4 `RegisterActivity`

```kotlin
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
```

> Los botones "Google" y "GitHub" de tu diseño: déjalos como visuales y muestra un Toast "Próximamente". Implementarlos requiere Firebase/OAuth y no es necesario.

---

## 6. `HomeActivity`: contenedor de Fragments

### ¿Para qué sirve?

Es la "carcasa": muestra la barra inferior y cambia el Fragment según la pestaña.

### 6.1 Menú: `res/menu/bottom_nav.xml`

```xml
<menu xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:id="@+id/nav_inicio" android:icon="@drawable/ic_home" android:title="Inicio"/>
    <item android:id="@+id/nav_movimientos" android:icon="@drawable/ic_swap" android:title="Movimientos"/>
    <item android:id="@+id/nav_presupuesto" android:icon="@drawable/ic_pie" android:title="Presupuesto"/>
    <item android:id="@+id/nav_metas" android:icon="@drawable/ic_target" android:title="Metas"/>
    <item android:id="@+id/nav_perfil" android:icon="@drawable/ic_person" android:title="Perfil"/>
</menu>
```

Los íconos: clic derecho en `drawable` → New → Vector Asset → busca "home", "swap horiz", "pie chart", "flag", "person".

### 6.2 `activity_home.xml`

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

    <!-- Botón de IA flotante -->
    <com.google.android.material.floatingactionbutton.FloatingActionButton
        android:id="@+id/fabIa"
        android:layout_width="wrap_content" android:layout_height="wrap_content"
        android:layout_gravity="bottom|end"
        android:layout_marginEnd="16dp" android:layout_marginBottom="80dp"
        android:src="@drawable/ic_sparkle"/>
</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

### 6.3 `HomeActivity.kt`

```kotlin
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

## 7. `FinanzasViewModel`

### ¿Para qué sirve?

Concentra **todos los cálculos** (balance, progreso de presupuestos, etc.). Los Fragments solo muestran datos. Como se crea con `activityViewModels()`, **todos los fragments comparten la misma instancia** y ven los mismos datos.

```kotlin
package emilio.tolosa.finai.viewmodel

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
```

---

## 8. Fragment de Inicio (`HomeFragment`)

### ¿Qué muestra?

La tarjeta azul con el balance, las tarjetas de Ingresos y Gastos, el ahorro del mes y los últimos movimientos.

### 8.1 IDs sugeridos en `fragment_home.xml`

`tvBalance`, `tvIngresos`, `tvGastos`, `tvBalanceUsd`, `progressAhorro`, `rvUltimos`.

### 8.2 Adaptador de la lista: `MovimientoAdapter.kt`

**¿Para qué sirve?** RecyclerView necesita un adaptador que convierta cada `Movimiento` en una fila visual. `ListAdapter` + `DiffUtil` calcula automáticamente qué filas cambiaron.

Layout `item_movimiento.xml` con IDs: `tvTitulo`, `tvDetalle`, `tvMonto`.

```kotlin
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

### 8.3 `HomeFragment.kt`

```kotlin
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
        vm.cargarTasas("MXN")   // API ExchangeRate (sección 12)

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

**Puntos clave a explicar:**
- `repeatOnLifecycle(STARTED)`: solo escucha cambios cuando el Fragment está visible (ahorra batería).
- `_b = null` en `onDestroyView`: evita fugas de memoria (los Fragments viven más que sus vistas).

---

## 9. Movimientos + "Nuevo movimiento"

### 9.1 `MovimientosFragment`

Layout: `rvMovimientos` (RecyclerView) y `btnNuevo` (botón "+ Nuevo movimiento").

```kotlin
class MovimientosFragment : Fragment(R.layout.fragment_movimientos) {
    private val vm: FinanzasViewModel by activityViewModels()

    override fun onViewCreated(view: View, s: Bundle?) {
        val b = FragmentMovimientosBinding.bind(view)

        // Mantener pulsado = borrar
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

> Tu diseño separa "INGRESOS" y "EGRESOS" con encabezados. Para lograrlo más adelante, usa dos `RecyclerView` o un adaptador con dos tipos de vista (`getItemViewType`). Empieza con una lista simple y luego mejoras.

### 9.2 `NuevoMovimientoDialog.kt`

**¿Para qué sirve?** Formulario emergente para capturar título, monto, categoría y tipo. Es reutilizable: también lo abrirá el sensor (agitar) y la cámara.

`dialog_movimiento.xml`: `etTitulo`, `etMonto`, `spCategoria` (Spinner), `swIngreso` (Switch).

```kotlin
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

---

## 10. Presupuesto

### ¿Para qué sirve?

Define cuánto puedes gastar por categoría y muestra cuánto llevas (barras de progreso). Cruza dos tablas: `presupuestos` (límite) y `movimientos` (gasto real). Ese cruce ya lo hace el ViewModel.

### 10.1 Layout

- `tvPresupuestoMensual` (el "$10,000").
- `rvPresupuestos` con `item_presupuesto.xml`: `tvCategoria`, `tvLimite`, `progressBar` (`LinearProgressIndicator`), `tvUso`, `tvRestan`.
- `tvDisponible` y `btnEditar`.

### 10.2 Adaptador

```kotlin
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
        // Rojo si te pasaste del 90%
        val color = if (p.progreso >= 90) "#E5484D" else "#4F5BFF"
        h.b.progressBar.setIndicatorColor(Color.parseColor(color))
    }
}
```

### 10.3 Fragment

```kotlin
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch { vm.presupuestos.collect { lista ->
            adapter.submitList(lista)
            val totalRestante = lista.sumOf { it.restante }
            b.tvDisponible.text = totalRestante.mx()
        } }
        launch { vm.store.presupuestoMensual.collect { b.tvPresupuestoMensual.text = it.mx() } }
    }
}

// "Editar presupuesto": un diálogo con categoría + límite que llama a vm.guardarPresupuesto(cat, limite)
```

---

## 11. Metas y Perfil

### 11.1 Metas

**¿Para qué sirve?** Objetivos de ahorro (laptop, viaje, fondo de emergencia). El porcentaje = ahorrado / objetivo.

`item_meta.xml`: `tvNombre`, `tvPorcentaje`, `progressMeta`, `tvAhorrado`, `tvObjetivo`, `btnAbonar`.

```kotlin
override fun onBindViewHolder(h: VH, i: Int) {
    val m = getItem(i)
    val pct = ((m.ahorrado / m.objetivo) * 100).toInt().coerceIn(0, 100)
    h.b.tvNombre.text = m.nombre
    h.b.tvPorcentaje.text = "$pct%"
    h.b.progressMeta.progress = pct
    h.b.tvAhorrado.text = "Ahorrado: ${m.ahorrado.mx()}"
    h.b.tvObjetivo.text = "Meta: ${m.objetivo.mx()}"
    h.b.btnAbonar.setOnClickListener { onAbonar(m) }   // abre diálogo → vm.abonarMeta(m, monto)
}
```

El botón "+ Nueva meta" abre un diálogo (nombre + objetivo) que llama a `vm.agregarMeta(...)`.

### 11.2 Perfil

**¿Para qué sirve?** Muestra datos del usuario, permite cambiar moneda y cerrar sesión.

```kotlin
class PerfilFragment : Fragment(R.layout.fragment_perfil) {
    private val vm: FinanzasViewModel by activityViewModels()

    override fun onViewCreated(view: View, s: Bundle?) {
        val b = FragmentPerfilBinding.bind(view)

        viewLifecycleOwner.lifecycleScope.launch {
            b.tvNombre.text = vm.store.nombre.first()
            b.tvEmail.text = vm.store.email.first()
        }

        // Moneda preferida (la usará ExchangeRate)
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

## 12. Las tres APIs

### Resumen: ¿qué hace cada una en Finai?

| API | Para qué la usamos | Requiere |
|---|---|---|
| **ExchangeRate** | Mostrar tu balance en otras monedas (USD, EUR) | API key gratuita |
| **OpenAI** | Asistente financiero (chat) + leer tickets con la cámara | API key con saldo |
| **Belvo** | Importar movimientos bancarios (datos de prueba en sandbox) | Cuenta de desarrollador |

### 12.1 Base común: `network/ApiClient.kt`

Un solo archivo que crea los clientes Retrofit.

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
        .readTimeout(60, TimeUnit.SECONDS)   // OpenAI puede tardar
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
        // Belvo usa autenticación "Basic" con secretId:secretPassword
        val cred = Credentials.basic(BuildConfig.BELVO_ID, BuildConfig.BELVO_SECRET)
        val auth = Interceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("Authorization", cred).build())
        }
        retrofit("https://sandbox.belvo.com/", http(auth)).create(BelvoApi::class.java)
    }
}
```

### 12.2 ExchangeRate (tipo de cambio)

**Paso 1.** Crea cuenta en https://www.exchangerate-api.com y copia tu API key a `local.properties` (`EXCHANGE_KEY`).

**Paso 2.** Interfaz:

```kotlin
interface ExchangeApi {
    @GET("v6/{key}/latest/{base}")
    suspend fun latest(@Path("key") key: String, @Path("base") base: String): ExchangeResponse
}

data class ExchangeResponse(
    val result: String,
    val base_code: String,
    val conversion_rates: Map<String, Double>  // {"USD":0.054,"EUR":0.05,...}
)
```

**Paso 3.** En `FinanzasViewModel` agrega:

```kotlin
private val _tasas = MutableStateFlow<Map<String, Double>>(emptyMap())
val tasas: StateFlow<Map<String, Double>> = _tasas

fun cargarTasas(base: String) = viewModelScope.launch {
    try {
        val r = ApiClient.exchange.latest(BuildConfig.EXCHANGE_KEY, base)
        if (r.result == "success") _tasas.value = r.conversion_rates
    } catch (e: Exception) {
        // Sin internet o error: la app sigue funcionando sin conversión
        Log.e("Exchange", "Error: ${e.message}")
    }
}
```

**Cómo lo explicas:** "Consulto cuánto vale 1 MXN en otras monedas y multiplico mi balance para mostrar el equivalente."

> El plan gratuito tiene límite de peticiones mensuales. Llama a `cargarTasas` una vez al abrir Inicio, no en cada cambio.

### 12.3 OpenAI (asistente financiero)

**Paso 1.** Crea una API key en https://platform.openai.com, agrega saldo y pon un límite de gasto. Guarda la key en `local.properties`.

**Paso 2.** Interfaz y modelos:

```kotlin
interface OpenAiApi {
    @POST("v1/chat/completions")
    suspend fun chat(@Body body: ChatRequest): ChatResponse
}

// content es Any porque puede ser un texto o una lista (texto + imagen)
data class ChatMessage(val role: String, val content: Any)
data class ChatRequest(val model: String = "gpt-4o-mini", val messages: List<ChatMessage>)
data class ChatResponse(val choices: List<Choice>)
data class Choice(val message: ChatMessage)
```

`gpt-4o-mini` es económico y soporta imágenes. Verifica en la documentación de OpenAI qué modelos hay disponibles cuando lo hagas.

**Paso 3.** Añade a `FinanzasViewModel` un resumen de tus finanzas para dárselo a la IA como contexto:

```kotlin
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
```

**Paso 4.** `AIAssistantActivity.kt` (layout: `rvChat`, `etMensaje`, `btnEnviar`, `progress`):

```kotlin
class AIAssistantActivity : AppCompatActivity() {
    private lateinit var b: ActivityAiAssistantBinding
    private val vm: FinanzasViewModel by viewModels()
    private val historial = mutableListOf<ChatMessage>()
    private val visibles = mutableListOf<String>()   // lo que se muestra en pantalla

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityAiAssistantBinding.inflate(layoutInflater)
        setContentView(b.root)

        // "system" = las reglas del asistente. Nunca las ve el usuario.
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
        // Sencillo: usa un ArrayAdapter en un ListView o tu propio RecyclerView
        (b.rvChat.adapter as? ChatAdapter)?.submit(visibles.toList())
        b.rvChat.scrollToPosition(visibles.size - 1)
    }
}
```

> `historial` se manda completo en cada petición porque la API **no tiene memoria**: tú le pasas toda la conversación.

Ideas de preguntas para la demo: "¿En qué gasté más este mes?", "¿Cómo puedo ahorrar para mi laptop?", "Arma un plan para reducir mis gastos en comida".

### 12.4 Belvo (movimientos bancarios)

**¿Para qué sirve?** Belvo es un servicio de *open banking* para Latinoamérica: se conecta a bancos y devuelve cuentas y transacciones. Con esto Finai importaría tus movimientos reales automáticamente.

> ⚠️ **Limitación importante:** el flujo oficial de producción (widget de Belvo) requiere un backend para generar tokens. Por eso, para tu proyecto sin backend, se usa el **sandbox** con credenciales bancarias **falsas** que Belvo publica en su documentación. Confirma los endpoints y credenciales de prueba actuales en https://developers.belvo.com, porque pueden cambiar.

**Paso 1.** Crea una cuenta en Belvo (dashboard de desarrollador), entra al entorno **sandbox** y crea las llaves (`secretId` y `secretPassword`). Guárdalas en `local.properties`.

**Paso 2.** Interfaz:

```kotlin
interface BelvoApi {
    // 1) Crear un "link" = la conexión con un banco de prueba
    @POST("api/links/")
    suspend fun crearLink(@Body body: LinkRequest): LinkResponse

    // 2) Pedir transacciones de ese link
    @POST("api/transactions/")
    suspend fun transacciones(@Body body: TxRequest): List<BelvoTx>
}

data class LinkRequest(
    val institution: String,        // institución de sandbox (ver docs de Belvo)
    val username: String,           // usuario de prueba (ver docs)
    val password: String,           // contraseña de prueba (ver docs)
    val access_mode: String = "single"
)
data class LinkResponse(val id: String)

data class TxRequest(val link: String, val date_from: String, val date_to: String) // "2026-09-01"

data class BelvoTx(
    val id: String,
    val amount: Double,
    val type: String,           // "INFLOW" (ingreso) | "OUTFLOW" (gasto)
    val description: String?,
    val category: String?,
    val value_date: String?
)
```

Si al probar la respuesta viene con otra estructura (por ejemplo paginada con `results`), ajusta el modelo mirando el log de Retrofit: es normal.

**Paso 3.** En el ViewModel:

```kotlin
fun importarDeBelvo(onResultado: (String) -> Unit) = viewModelScope.launch {
    try {
        // Credenciales de prueba: cópialas de la documentación de sandbox de Belvo
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
                    externalId = t.id      // evita duplicados si importas dos veces
                )
            )
        }
        onResultado("Se importaron ${txs.size} movimientos")
    } catch (e: Exception) {
        onResultado("Error con Belvo: ${e.message}")
    }
}
```

**Paso 4.** Un botón "Importar del banco" en Movimientos o Perfil que llame a `vm.importarDeBelvo { msg -> Toast... }`.

**Cómo lo explicas:** "Belvo actúa como puente con el banco. En producción se usaría un backend por seguridad; aquí uso sandbox con datos ficticios."

---

## 13. Cámara y Sensores (extras que suman puntos)

### 13.1 `CameraActivity`: foto de ticket → gasto automático

**Flujo:** tomas foto → se codifica en Base64 → OpenAI la lee y devuelve JSON → se abre el diálogo prellenado.

**Permiso en tiempo de ejecución + captura (simple):**

```kotlin
class CameraActivity : AppCompatActivity() {
    private lateinit var b: ActivityCameraBinding
    private val vm: FinanzasViewModel by viewModels()

    // Devuelve una miniatura (Bitmap). Suficiente para tickets legibles y sin FileProvider.
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
data class TicketIa(val titulo: String, val monto: Double, val categoria: String)
```

Nota: `NuevoMovimientoDialog` usa `activityViewModels()`, así que funciona dentro de una Activity con `supportFragmentManager` (la Activity es su dueña).

### 13.2 `SensorsActivity` / sensor: agitar para agregar un gasto

**¿Para qué sirve el acelerómetro?** Detecta movimiento. Al agitar el teléfono abrimos el diálogo de "Nuevo movimiento" (registro rápido).

```kotlin
class ShakeDetector(private val onShake: () -> Unit) : SensorEventListener {
    private var ultimo = 0L
    override fun onSensorChanged(e: SensorEvent) {
        val g = sqrt(e.values[0].pow(2) + e.values[1].pow(2) + e.values[2].pow(2)) / SensorManager.GRAVITY_EARTH
        val ahora = System.currentTimeMillis()
        if (g > 2.5f && ahora - ultimo > 1000) { ultimo = ahora; onShake() }
    }
    override fun onAccuracyChanged(s: Sensor?, a: Int) {}
}

// En HomeActivity:
private lateinit var sm: SensorManager
private val shake = ShakeDetector {
    NuevoMovimientoDialog(origen = "sensor").show(supportFragmentManager, "shake")
}
override fun onResume() {
    super.onResume()
    sm = getSystemService(SENSOR_SERVICE) as SensorManager
    sm.registerListener(shake, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI)
}
override fun onPause() { super.onPause(); sm.unregisterListener(shake) }  // importante: ahorra batería
```

En el emulador, los sensores se simulan en **Extended Controls (…) → Virtual sensors**.

`SensorsActivity` puede quedarse como pantalla demo que muestre valores en vivo del acelerómetro/luz para la rúbrica.

---

## 14. Estilos y diseño

Colores tomados de tus mockups (ajústalos a gusto), en `res/values/colors.xml`:

```xml
<color name="primary">#4F5BFF</color>       <!-- botones e íconos activos -->
<color name="bg">#F5F3EE</color>            <!-- fondo crema del login -->
<color name="card">#FFFFFF</color>
<color name="income">#22A06B</color>
<color name="expense">#E5484D</color>
<color name="warning">#F5A623</color>
<color name="text_secondary">#6B7280</color>
```

- Tarjetas: `bg_card.xml` con `<corners android:radius="20dp"/>` y fondo blanco (ya lo tienes).
- Tarjeta azul del balance: un `<gradient>` de `#3B6BFF` a `#5B8DFF` con radio de 24dp.
- Barras: `com.google.android.material.progressindicator.LinearProgressIndicator` con `app:trackCornerRadius="8dp"`.

---

## 15. Orden de trabajo recomendado

| Etapa | Qué hacer | Resultado que verás |
|---|---|---|
| 1 | Dependencias, Manifest, `local.properties` | Proyecto compila |
| 2 | Room + DataStore + ViewModel | Datos guardados |
| 3 | Login / Registro / MainActivity router | Puedes entrar y salir |
| 4 | `HomeActivity` + barra inferior + fragments vacíos | Navegas entre pestañas |
| 5 | Inicio + Movimientos + diálogo | Agregas y ves movimientos |
| 6 | Presupuesto + Metas + Perfil | App completa localmente |
| 7 | ExchangeRate | Balance en USD |
| 8 | OpenAI (chat) | Asistente funcionando |
| 9 | Belvo sandbox | Importas transacciones |
| 10 | Cámara + sensor | Extras |
| 11 | Pulir diseño y probar | Listo para entregar |

**Regla de oro:** que cada etapa funcione antes de pasar a la siguiente. Prueba las APIs solas, con un botón temporal, antes de integrarlas.

---

## 16. Errores comunes y soluciones

| Problema | Causa probable | Solución |
|---|---|---|
| `Unresolved reference: ActivityHomeBinding` | ViewBinding no activo | `viewBinding = true` + Sync Gradle |
| `BuildConfig.OPENAI_KEY` no existe | Falta `buildConfig = true` | Agrégalo y sincroniza |
| Room: "Cannot find implementation" | Falta plugin KSP | Revisa plugin y versión de KSP |
| App se cierra al abrir | Activity sin declarar en el Manifest | Añade el `<activity>` |
| Error 401 en OpenAI | Key mal copiada / sin saldo | Revisa `local.properties` y saldo |
| Error 429 en OpenAI | Límite de peticiones o sin crédito | Espera o recarga crédito |
| `UnknownHostException` | Sin internet o falta permiso `INTERNET` | Revisa Manifest y conexión del emulador |
| Lista vacía | Olvidaste `submitList` o el `LayoutManager` | Asigna `layoutManager` en el RecyclerView |
| Crash al volver a un Fragment | Uso de `_b` tras `onDestroyView` | Usa `_b = null` y `viewLifecycleOwner` |
| Cambié las tablas y falla | Base vieja instalada | Desinstala la app o sube la `version` |

---

## 17. Puntos para tu exposición

1. **Arquitectura**: UI → ViewModel → Room/APIs.
2. **Sin backend**: base local (Room), sesión local (DataStore), APIs directas.
3. **Seguridad honesta**: llaves en `local.properties`; en producción irían en un servidor. Belvo solo en sandbox.
4. **Cada API con su función**: ExchangeRate (conversión), OpenAI (asistente + lectura de tickets), Belvo (importación bancaria).
5. **Extras técnicos**: cámara con permiso en runtime, acelerómetro, corrutinas y Flow (datos reactivos).
6. **Mejoras futuras**: backend propio, biometría, gráficas por mes, exportar a PDF/CSV.

---

¡Éxito con Finai! 🚀
