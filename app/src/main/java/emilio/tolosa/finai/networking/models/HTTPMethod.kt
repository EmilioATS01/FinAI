package emilio.tolosa.finai.networking.models

import com.android.volley.Request.Method

enum class HTTPMethod(val rawValue: Int) {
    GET(Method.GET),
    POST(Method.POST),
    PUT(Method.PUT),
    DELETE(Method.DELETE)
}