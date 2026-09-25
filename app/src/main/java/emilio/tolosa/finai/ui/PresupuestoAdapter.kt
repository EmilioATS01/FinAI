package emilio.tolosa.finai.ui

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemPresupuestoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(h: VH, i: Int) {
        val p = getItem(i)
        h.b.tvCategoria.text = p.categoria
        h.b.tvLimite.text = p.limite.mx()
        h.b.progressBar.progress = p.progreso
        h.b.tvUso.text = "Uso: ${p.progreso}%"
        h.b.tvRestan.text = "Restan ${p.restante.mx()}"
    }
}