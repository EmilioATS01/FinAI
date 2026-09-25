package emilio.tolosa.finai.ui

import android.app.Dialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import emilio.tolosa.finai.data.Movimiento
import emilio.tolosa.finai.databinding.DialogMovimientoBinding
import emilio.tolosa.finai.viewmodel.FinanzasViewModel

class NuevoMovimientoDialog(
    private val tituloInicial: String = "",
    private val montoInicial: Double? = null,
    private val categoriaInicial: String? = null,
    private val origen: String = "manual"
) : DialogFragment() {

    private val vm: FinanzasViewModel by activityViewModels()
    private val categorias = listOf("Comida", "Transporte", "Entretenimiento", "Compras", "Salud", "Ingreso", "Otros")

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val b = DialogMovimientoBinding.inflate(layoutInflater)
        b.spCategoria.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categorias)
        b.etTitulo.setText(tituloInicial)
        montoInicial?.let { b.etMonto.setText(it.toString()) }
        categoriaInicial?.let { b.spCategoria.setSelection(categorias.indexOf(it).coerceAtLeast(0)) }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Nuevo movimiento")
            .setView(b.root)
            .setPositiveButton("Guardar") { _, _ ->
                val monto = b.etMonto.text.toString().toDoubleOrNull()
                if (monto == null || b.etTitulo.text.isNullOrBlank()) {
                    Toast.makeText(context, "Datos inválidos", Toast.LENGTH_SHORT).show()
                } else {
                    vm.agregarMovimiento(
                        Movimiento(
                            titulo = b.etTitulo.text.toString(),
                            categoria = b.spCategoria.selectedItem.toString(),
                            monto = monto,
                            esIngreso = b.swIngreso.isChecked,
                            origen = origen
                        )
                    )
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()
    }
}