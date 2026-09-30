package emilio.tolosa.finai.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import emilio.tolosa.finai.R
import emilio.tolosa.finai.data.Movimiento
import emilio.tolosa.finai.databinding.ItemMovimientoHomeBinding
import emilio.tolosa.finai.mx
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeMovimientoAdapter :
    ListAdapter<Movimiento, HomeMovimientoAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<Movimiento>() {

        override fun areItemsTheSame(
            oldItem: Movimiento,
            newItem: Movimiento
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: Movimiento,
            newItem: Movimiento
        ): Boolean {
            return oldItem == newItem
        }
    }

    class VH(
        val binding: ItemMovimientoHomeBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): VH {
        val binding = ItemMovimientoHomeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(
        holder: VH,
        position: Int
    ) {
        val movimiento = getItem(position)
        val binding = holder.binding

        // Nombre del movimiento
        binding.tvTitulo.text = movimiento.titulo

        // Categoría + fecha
        val fechaTexto = obtenerFechaTexto(movimiento.fecha)
        binding.tvDetalle.text = "${movimiento.categoria} · $fechaTexto"

        // Signo del movimiento
        val signo = if (movimiento.esIngreso) "+" else "-"
        binding.tvMonto.text = "$signo${movimiento.monto.mx()}"

        // Colores oficiales FinAI
        val colorMonto = if (movimiento.esIngreso) R.color.income else R.color.expense
        binding.tvMonto.setTextColor(
            ContextCompat.getColor(binding.root.context, colorMonto)
        )

        // Icono según la categoría
        binding.tvIcono.text = obtenerIcono(movimiento)
    }

    private fun obtenerIcono(movimiento: Movimiento): String {

        if (movimiento.esIngreso) {
            return "💼"
        }

        return when (movimiento.categoria.lowercase()) {

            "comida",
            "alimentación",
            "alimentacion",
            "supermercado" -> "🛒"

            "transporte" -> "🚗"

            "entretenimiento" -> "▣"

            "salud" -> "♡"

            "servicios" -> "⌂"

            "compras" -> "🛍"

            else -> "●"
        }
    }

    private fun obtenerFechaTexto(fecha: Long): String {

        val hoy = Calendar.getInstance()

        val movimiento = Calendar.getInstance().apply {
            timeInMillis = fecha
        }

        // Si es hoy
        if (
            hoy[Calendar.YEAR] == movimiento[Calendar.YEAR] &&
            hoy[Calendar.DAY_OF_YEAR] == movimiento[Calendar.DAY_OF_YEAR]
        ) {
            return "Hoy"
        }

        // Si fue ayer
        hoy.add(Calendar.DAY_OF_YEAR, -1)

        if (
            hoy[Calendar.YEAR] == movimiento[Calendar.YEAR] &&
            hoy[Calendar.DAY_OF_YEAR] == movimiento[Calendar.DAY_OF_YEAR]
        ) {
            return "Ayer"
        }

        // Cualquier otra fecha
        val formato = SimpleDateFormat(
            "dd MMM",
            Locale.forLanguageTag("es-MX")
        )

        return formato.format(Date(fecha))
    }
}