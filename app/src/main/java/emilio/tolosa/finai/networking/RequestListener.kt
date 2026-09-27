package emilio.tolosa.finai.networking

interface RequestListener {
    fun onResponse(response: String)
    fun onError(error: String)
}