package emilio.tolosa.finai.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import emilio.tolosa.finai.R
import emilio.tolosa.finai.databinding.FragmentHomeBinding
import emilio.tolosa.finai.mx
import emilio.tolosa.finai.viewmodel.FinanzasViewModel
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {

    private val vm: FinanzasViewModel by activityViewModels()
    private var _b: FragmentHomeBinding? = null
    private val b get() = _b!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _b = FragmentHomeBinding.bind(view)

        val adapter = MovimientoAdapter()
        b.rvUltimos.layoutManager = LinearLayoutManager(requireContext())
        b.rvUltimos.adapter = adapter

        vm.sembrarDatosDemo()
        vm.cargarTasas("MXN")

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { vm.balance.collect { b.tvBalance.text = it.mx() } }
                launch { vm.ingresos.collect { b.tvIngresos.text = it.mx() } }
                launch { vm.gastos.collect { b.tvGastos.text = it.mx() } }
                launch { vm.ultimos.collect { adapter.submitList(it) } }
                launch {
                    vm.tasas.collect { tasas ->
                        val usd = tasas["USD"] ?: return@collect
                        b.tvBalanceUsd.text = "≈ USD ${"%.2f".format(vm.balance.value * usd)}"
                    }
                }
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}