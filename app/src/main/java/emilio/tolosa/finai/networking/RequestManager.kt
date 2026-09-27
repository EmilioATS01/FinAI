package emilio.tolosa.finai.networking

import android.content.Context
import android.util.Log
import com.android.volley.*
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import emilio.tolosa.finai.networking.models.TargetType

class RequestManager(private val context: Context) {
    private val TAG = this::class.java.simpleName

    fun request(target: TargetType, requestListener: RequestListener) {
        try {
            Log.i(TAG, "URL: ${target.url}")

            val queue = Volley.newRequestQueue(context)
            val request = object : JsonObjectRequest(
                target.method.rawValue, target.url, target.parameters,
                { response ->
                    try {
                        requestListener.onResponse(response.toString())
                    } catch (e: Exception) {
                        requestListener.onError("Internal error")
                    }
                },
                { error ->
                    val codigo = error.networkResponse?.statusCode
                    val cuerpo = error.networkResponse?.data?.let { String(it) }
                    Log.e(TAG, "RESPONSE ERROR: codigo=$codigo, cuerpo=$cuerpo, error=$error")
                    when (error) {
                        is NetworkError, is NoConnectionError -> requestListener.onError("Network Error")
                        is ServerError, is AuthFailureError, is ParseError -> requestListener.onError("Server Error ($codigo)")
                        is TimeoutError -> requestListener.onError("Time Out")
                        else -> requestListener.onError("Unknown error")
                    }
                }
            ) {
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>()
                    target.headers?.let { headers.putAll(it) }
                    return headers
                }
            }
            request.retryPolicy = DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
            queue.add(request)
        } catch (e: Exception) {
            Log.e(TAG, "CATCH: $e")
            requestListener.onError("Internal error")
        }
    }
}