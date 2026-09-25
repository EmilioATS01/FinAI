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

        val adapter = MovimientoAdapter { m ->
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("¿Borrar ${m.titulo}?")
                .setPositiveButton("Borrar") { _, _ -> vm.borrarMovimiento(m) }
                .setNegativeButton("Cancelar", null).show()
        }
        b.rvMovimientos.layoutManager = LinearLayoutManager(requireContext())
        b.rvMovimientos.adapter = adapter

        b.btnNuevo.setOnClickListener { NuevoMovimientoDialog().show(childFragmentManager, "nuevo") }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.movimientos.collect { adapter.submitList(it) }
            }
        }
    }
}