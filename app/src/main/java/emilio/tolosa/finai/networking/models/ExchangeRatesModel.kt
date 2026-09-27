package emilio.tolosa.finai.networking.models

import androidx.lifecycle.MutableLiveData

class ExchangeRatesModel {
    val tasas = MutableLiveData<Map<String, Double>>()
}