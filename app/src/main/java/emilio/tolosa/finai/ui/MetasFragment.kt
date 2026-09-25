package emilio.tolosa.finai.ui

import android.os.Bundle
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
import emilio.tolosa.finai.databinding.FragmentMetasBinding
import emilio.tolosa.finai.viewmodel.FinanzasViewModel
import kotlinx.coroutines.launch

class MetasFragment : Fragment(R.layout.fragment_metas) {
    private val vm: FinanzasViewModel by activityViewModels()

    override fun onViewCreated(view: View, s: Bundle?) {
        val b = FragmentMetasBinding.bind(view)

        val adapter = MetaAdapter { meta ->
            val et = EditText(requireContext()).apply { hint = "Monto a abonar" }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Abonar a ${meta.nombre}")
                .setView(LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL; setPadding(40, 20, 40, 20); addView(et)
                })
                .setPositiveButton("Abonar") { _, _ ->
                    et.text.toString().toDoubleOrNull()?.let { vm.abonarMeta(meta, it) }
                        ?: Toast.makeText(requireContext(), "Monto inválido", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancelar", null).show()
        }
        b.rvMetas.layoutManager = LinearLayoutManager(requireContext())
        b.rvMetas.adapter = adapter

        b.btnNuevaMeta.setOnClickListener {
            val etNombre = EditText(requireContext()).apply { hint = "Nombre de la meta" }
            val etObjetivo = EditText(requireContext()).apply { hint = "Objetivo ($)" }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Nueva meta")
                .setView(LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL; setPadding(40, 20, 40, 20)
                    addView(etNombre); addView(etObjetivo)
                })
                .setPositiveButton("Crear") { _, _ ->
                    val obj = etObjetivo.text.toString().toDoubleOrNull()
                    if (etNombre.text.isNullOrBlank() || obj == null) {
                        Toast.makeText(requireContext(), "Datos inválidos", Toast.LENGTH_SHORT).show()
                    } else vm.agregarMeta(etNombre.text.toString(), obj)
                }
                .setNegativeButton("Cancelar", null).show()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.metas.collect { adapter.submitList(it) }
            }
        }
    }
}