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
import emilio.tolosa.finai.viewmodel.ExchangeViewModel
import emilio.tolosa.finai.viewmodel.FinanzasViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment(R.layout.fragment_home) {

    private val vm: FinanzasViewModel by activityViewModels()

    private var _b: FragmentHomeBinding? = null
    private val b get() = _b!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _b = FragmentHomeBinding.bind(view)

        // Mostrar el mes actual automáticamente
        val formatoMes = SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("es-MX"))
        val mesActual = formatoMes.format(Date()).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.forLanguageTag("es-MX")) else it.toString()
        }

        b.tvMesActual.text = mesActual
        b.tvActualizacion.text = "Hoy"

        // Adapter exclusivo para los movimientos del Inicio
        val adapter = HomeMovimientoAdapter()

        b.rvUltimos.layoutManager = LinearLayoutManager(requireContext())
        b.rvUltimos.adapter = adapter

        // Conversión del balance de MXN a USD
        val exchangeVm = ExchangeViewModel(requireContext())

        exchangeVm.fetchTasas(
            emilio.tolosa.finai.BuildConfig.EXCHANGE_KEY,
            "MXN"
        )

        exchangeVm.tasas.observe(viewLifecycleOwner) { tasas ->

            val usd = tasas["USD"] ?: return@observe

            b.tvBalanceUsd.text =
                "≈ USD ${"%.2f".format(vm.balance.value * usd)}"
        }

        // Observar los datos financieros
        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                // Balance
                launch {
                    vm.balance.collect {
                        b.tvBalance.text = it.mx()
                    }
                }

                // Ingresos
                launch {
                    vm.ingresos.collect {
                        b.tvIngresos.text = it.mx()
                    }
                }

                // Gastos
                launch {
                    vm.gastos.collect {
                        b.tvGastos.text = it.mx()
                    }
                }

                // Últimos movimientos
                launch {
                    vm.ultimos.collect {
                        adapter.submitList(it)
                    }
                }

                // Ahorro del mes
                launch {

                    combine(
                        vm.ingresos,
                        vm.gastos
                    ) { ingresos, gastos ->

                        Pair(ingresos, gastos)

                    }.collect { (ingresos, gastos) ->

                        val ahorro = ingresos - gastos

                        val porcentaje =
                            if (ingresos > 0) {

                                ((ahorro / ingresos) * 100)
                                    .toInt()
                                    .coerceIn(0, 100)

                            } else {
                                0
                            }

                        b.tvAhorroMes.text = ahorro.mx()

                        b.tvPorcentajeAhorro.text =
                            "$porcentaje%"

                        b.progressAhorro.progress =
                            porcentaje
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _b = null
    }
}