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
    private val movimientoAEditar: Movimiento? = null,
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

        val esEdicion = movimientoAEditar != null

        // Prellenar según si es edición o creación desde cámara/sensor
        if (esEdicion) {
            b.etTitulo.setText(movimientoAEditar!!.titulo)
            b.etMonto.setText(movimientoAEditar.monto.toString())
            b.spCategoria.setSelection(categorias.indexOf(movimientoAEditar.categoria).coerceAtLeast(0))
            b.swIngreso.isChecked = movimientoAEditar.esIngreso
        } else {
            b.etTitulo.setText(tituloInicial)
            montoInicial?.let { b.etMonto.setText(it.toString()) }
            categoriaInicial?.let { b.spCategoria.setSelection(categorias.indexOf(it).coerceAtLeast(0)) }
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (esEdicion) "Editar movimiento" else "Nuevo movimiento")
            .setView(b.root)
            .setPositiveButton(if (esEdicion) "Guardar cambios" else "Guardar") { _, _ ->
                val monto = b.etMonto.text.toString().toDoubleOrNull()
                if (monto == null || b.etTitulo.text.isNullOrBlank()) {
                    Toast.makeText(context, "Datos inválidos", Toast.LENGTH_SHORT).show()
                } else if (esEdicion) {
                    vm.actualizarMovimiento(
                        movimientoAEditar!!.copy(
                            titulo = b.etTitulo.text.toString(),
                            categoria = b.spCategoria.selectedItem.toString(),
                            monto = monto,
                            esIngreso = b.swIngreso.isChecked
                        )
                    )
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