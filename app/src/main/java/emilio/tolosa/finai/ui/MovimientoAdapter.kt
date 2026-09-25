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