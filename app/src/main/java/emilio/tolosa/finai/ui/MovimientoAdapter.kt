package emilio.tolosa.finai.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import emilio.tolosa.finai.R
import emilio.tolosa.finai.data.Movimiento
import emilio.tolosa.finai.databinding.ItemMovimientoBinding
import emilio.tolosa.finai.mx

class MovimientoAdapter(
    private val onClick: ((Movimiento) -> Unit)? = null,
    private val onLongClick: ((Movimiento) -> Unit)? = null
) : ListAdapter<Movimiento, MovimientoAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<Movimiento>() {
        override fun areItemsTheSame(a: Movimiento, b: Movimiento) = a.id == b.id
        override fun areContentsTheSame(a: Movimiento, b: Movimiento) = a == b
    }

    class VH(val b: ItemMovimientoBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemMovimientoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(h: VH, position: Int) {
        val m = getItem(position)
        h.b.tvTitulo.text = m.titulo
        h.b.tvDetalle.text = m.categoria
        val signo = if (m.esIngreso) "+" else "-"
        h.b.tvMonto.text = "$signo${m.monto.mx()}"
        val colorMonto = if (m.esIngreso) R.color.income else R.color.expense
        h.b.tvMonto.setTextColor(
            ContextCompat.getColor(h.b.root.context, colorMonto)
        )
        h.b.tvIcono.text = obtenerIcono(m.categoria, m.esIngreso)
        h.b.root.setOnClickListener { onClick?.invoke(m) }
        h.b.root.setOnLongClickListener { onLongClick?.invoke(m); true }
    }

    private fun obtenerIcono(categoria: String, esIngreso: Boolean): String {
        if (esIngreso) return "💼"
        return when (categoria.lowercase()) {
            "comida", "alimentación", "alimentacion", "supermercado" -> "🛒"
            "transporte" -> "🚗"
            "entretenimiento" -> "▣"
            "salud" -> "♡"
            "servicios" -> "⌂"
            "compras" -> "🛍"
            else -> "●"
        }
    }
}