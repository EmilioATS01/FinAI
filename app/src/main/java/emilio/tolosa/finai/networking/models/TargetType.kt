package emilio.tolosa.finai.networking.models

import org.json.JSONObject

interface TargetType {
    var url: String
    var method: HTTPMethod
    var encoding: Encoding
    var parameters: JSONObject?
    var headers: Map<String, String>?
}