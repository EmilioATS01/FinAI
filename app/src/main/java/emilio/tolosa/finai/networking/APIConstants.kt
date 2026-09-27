package emilio.tolosa.finai.networking

object APIConstants {
    const val EXCHANGE_SERVER = "https://v6.exchangerate-api.com/v6/"
    const val OPENAI_SERVER = "https://api.openai.com/v1/chat/completions"
    object EndPoints {
        const val LATEST_RATES = "latest"
    }

    // dejamos MAIN_SERVER apuntando a ExchangeRate para no romper el ViewModel de arriba
    const val MAIN_SERVER = EXCHANGE_SERVER
}