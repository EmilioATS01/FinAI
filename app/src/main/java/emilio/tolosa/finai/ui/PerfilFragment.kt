package emilio.tolosa.finai.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import emilio.tolosa.finai.activities.LoginActivity
import emilio.tolosa.finai.R
import emilio.tolosa.finai.databinding.FragmentPerfilBinding
import emilio.tolosa.finai.viewmodel.FinanzasViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PerfilFragment : Fragment(R.layout.fragment_perfil) {
    private val vm: FinanzasViewModel by activityViewModels()

    override fun onViewCreated(view: View, s: Bundle?) {
        val b = FragmentPerfilBinding.bind(view)

        viewLifecycleOwner.lifecycleScope.launch {
            b.tvNombre.text = vm.store.nombre.first()
            b.tvEmail.text = vm.store.email.first()
        }

        val monedas = listOf("MXN", "USD", "EUR")
        b.spMoneda.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, monedas)
        b.spMoneda.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                viewLifecycleOwner.lifecycleScope.launch { vm.store.guardarMoneda(monedas[pos]) }
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        b.btnCerrarSesion.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                vm.store.cerrarSesion()
                startActivity(Intent(requireContext(), LoginActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }
}