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