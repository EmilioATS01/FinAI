package emilio.tolosa.finai.network

import retrofit2.http.GET
import retrofit2.http.Path

interface ExchangeApi {
    @GET("v6/{key}/latest/{base}")
    suspend fun latest(@Path("key") key: String, @Path("base") base: String): ExchangeResponse
}

data class ExchangeResponse(
    val result: String,
    val base_code: String,
    val conversion_rates: Map<String, Double>
)
