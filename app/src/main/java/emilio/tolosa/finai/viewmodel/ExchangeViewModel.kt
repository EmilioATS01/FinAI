package emilio.tolosa.finai.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import emilio.tolosa.finai.networking.APIConstants
import emilio.tolosa.finai.networking.RequestListener
import emilio.tolosa.finai.networking.RequestManager
import emilio.tolosa.finai.networking.models.AnahuacAPI
import emilio.tolosa.finai.networking.models.Encoding
import emilio.tolosa.finai.networking.models.ExchangeRatesModel
import emilio.tolosa.finai.networking.models.HTTPMethod

class ExchangeVolleyResponse(
    val result: String,
    val conversion_rates: Map<String, Double>
)

class ExchangeViewModel(context: Context) : ViewModel() {
    private val TAG = "ExchangeViewModel"
    private val model = ExchangeRatesModel()
    private val requestManager = RequestManager(context)
    private val gson = Gson()

    val tasas: LiveData<Map<String, Double>>
        get() = model.tasas

    fun fetchTasas(apiKey: String, base: String) {
        val url = "${APIConstants.MAIN_SERVER}$apiKey/${APIConstants.EndPoints.LATEST_RATES}/$base"
        val target = AnahuacAPI(url, HTTPMethod.GET, Encoding.URL, null)

        requestManager.request(target, object : RequestListener {
            override fun onResponse(response: String) {
                try {
                    val parsed = gson.fromJson(response, ExchangeVolleyResponse::class.java)
                    if (parsed.result == "success") model.tasas.postValue(parsed.conversion_rates)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing: ${e.message}")
                }
            }
            override fun onError(error: String) {
                Log.e(TAG, "Error: $error")
            }
        })
    }
}