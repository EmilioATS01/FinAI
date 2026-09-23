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