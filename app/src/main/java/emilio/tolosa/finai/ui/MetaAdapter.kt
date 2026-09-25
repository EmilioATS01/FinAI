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
