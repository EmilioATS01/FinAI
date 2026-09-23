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

    @Query(
        """SELECT categoria, SUM(monto) AS total FROM movimientos
              WHERE esIngreso = 0 GROUP BY categoria"""
    )
    fun gastosPorCategoria(): Flow<List<GastoCategoria>>

    // IGNORE: si ya existe el externalId, no lo inserta otra vez
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertar(m: Movimiento)

    @Delete
    suspend fun borrar(m: Movimiento)

    // Metas
    @Query("SELECT * FROM metas")
    fun metas(): Flow<List<Meta>>
    @Insert
    suspend fun insertarMeta(m: Meta)
    @Update
    suspend fun actualizarMeta(m: Meta)

    // Presupuestos
    @Query("SELECT * FROM presupuestos")
    fun presupuestos(): Flow<List<Presupuesto>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarPresupuesto(p: Presupuesto)
}