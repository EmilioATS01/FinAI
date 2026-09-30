package emilio.tolosa.finai.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import emilio.tolosa.finai.R
import emilio.tolosa.finai.databinding.FragmentMovimientosBinding
import emilio.tolosa.finai.viewmodel.FinanzasViewModel
import kotlinx.coroutines.launch

class MovimientosFragment : Fragment(R.layout.fragment_movimientos) {
    private val vm: FinanzasViewModel by activityViewModels()

    override fun onViewCreated(view: View, s: Bundle?) {
        val b = FragmentMovimientosBinding.bind(view)

        val onItemClick: (emilio.tolosa.finai.data.Movimiento) -> Unit = { m ->
            NuevoMovimientoDialog(movimientoAEditar = m).show(childFragmentManager, "editar")
        }
        val onItemLongClick: (emilio.tolosa.finai.data.Movimiento) -> Unit = { m ->
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("¿Borrar ${m.titulo}?")
                .setPositiveButton("Borrar") { _, _ -> vm.borrarMovimiento(m) }
                .setNegativeButton("Cancelar", null).show()
        }

        val adapterIngresos = MovimientoAdapter(onClick = onItemClick, onLongClick = onItemLongClick)
        val adapterEgresos = MovimientoAdapter(onClick = onItemClick, onLongClick = onItemLongClick)

        b.rvIngresos.layoutManager = LinearLayoutManager(requireContext())
        b.rvIngresos.adapter = adapterIngresos

        b.rvEgresos.layoutManager = LinearLayoutManager(requireContext())
        b.rvEgresos.adapter = adapterEgresos

        b.btnNuevo.setOnClickListener { NuevoMovimientoDialog().show(childFragmentManager, "nuevo") }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.movimientos.collect { lista ->
                    val ingresos = lista.filter { it.esIngreso }
                    val egresos = lista.filter { !it.esIngreso }

                    adapterIngresos.submitList(ingresos)
                    adapterEgresos.submitList(egresos)

                    b.tvTituloIngresos.visibility = if (ingresos.isEmpty()) View.GONE else View.VISIBLE
                    b.rvIngresos.visibility = if (ingresos.isEmpty()) View.GONE else View.VISIBLE

                    b.tvTituloEgresos.visibility = if (egresos.isEmpty()) View.GONE else View.VISIBLE
                    b.rvEgresos.visibility = if (egresos.isEmpty()) View.GONE else View.VISIBLE

                    b.tvVacioMovimientos.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }
}