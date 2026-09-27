package emilio.tolosa.finai.ui

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import emilio.tolosa.finai.R
import emilio.tolosa.finai.databinding.FragmentPresupuestoBinding
import emilio.tolosa.finai.mx
import emilio.tolosa.finai.viewmodel.FinanzasViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PresupuestoFragment : Fragment(R.layout.fragment_presupuesto) {
    private val vm: FinanzasViewModel by activityViewModels()

    override fun onViewCreated(view: View, s: Bundle?) {
        val b = FragmentPresupuestoBinding.bind(view)
        val adapter = PresupuestoAdapter()
        b.rvPresupuestos.layoutManager = LinearLayoutManager(requireContext())
        b.rvPresupuestos.adapter = adapter

        // Editar límites por categoría
        b.btnEditar.setOnClickListener {
            val etCat = EditText(requireContext()).apply { hint = "Categoría" }
            val etLim = EditText(requireContext()).apply {
                hint = "Límite"
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            }
            val layout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(40, 20, 40, 20)
                addView(etCat); addView(etLim)
            }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Editar presupuesto por categoría")
                .setView(layout)
                .setPositiveButton("Guardar") { _, _ ->
                    val cat = etCat.text.toString().trim()
                    val lim = etLim.text.toString().toDoubleOrNull()
                    if (cat.isEmpty() || lim == null) {
                        Toast.makeText(requireContext(), "Datos inválidos", Toast.LENGTH_SHORT).show()
                    } else vm.guardarPresupuesto(cat, lim)
                }
                .setNegativeButton("Cancelar", null).show()
        }

        // Editar el presupuesto mensual general (la tarjeta de arriba)
        b.cardPresupuestoMensual.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val actual = vm.store.presupuestoMensual.first()
                val etMonto = EditText(requireContext()).apply {
                    hint = "Presupuesto mensual"
                    inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                    setText(actual.toString())
                }
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Editar presupuesto mensual")
                    .setView(LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(40, 20, 40, 20)
                        addView(etMonto)
                    })
                    .setPositiveButton("Guardar") { _, _ ->
                        val nuevoMonto = etMonto.text.toString().toDoubleOrNull()
                        if (nuevoMonto == null || nuevoMonto <= 0) {
                            Toast.makeText(requireContext(), "Ingresa un monto válido", Toast.LENGTH_SHORT).show()
                        } else {
                            viewLifecycleOwner.lifecycleScope.launch { vm.store.guardarPresupuesto(nuevoMonto) }
                        }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    vm.presupuestos.collect { lista ->
                        adapter.submitList(lista)
                        b.tvDisponible.text = lista.sumOf { it.restante }.mx()
                        b.pieChart.setDatos(lista.map { it.categoria to it.gastado })
                    }
                }
                launch {
                    vm.store.presupuestoMensual.collect { b.tvPresupuestoMensual.text = it.mx() }
                }
            }
        }
    }
}