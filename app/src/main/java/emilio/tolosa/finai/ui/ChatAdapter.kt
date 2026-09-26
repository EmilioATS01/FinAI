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