package emilio.tolosa.finai.network

import retrofit2.http.Body
import retrofit2.http.POST

interface BelvoApi {
    @POST("api/links/")
    suspend fun crearLink(@Body body: LinkRequest): LinkResponse

    @POST("api/transactions/")
    suspend fun transacciones(@Body body: TxRequest): List<BelvoTx>
}

data class LinkRequest(
    val institution: String,
    val username: String,
    val password: String,
    val access_mode: String = "single"
)
data class LinkResponse(val id: String)
data class TxRequest(val link: String, val date_from: String, val date_to: String)
data class BelvoTx(
    val id: String,
    val amount: Double,
    val type: String,
    val description: String?,
    val category: String?,
    val value_date: String?
)
