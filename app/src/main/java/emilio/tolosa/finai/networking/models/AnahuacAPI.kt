package emilio.tolosa.finai.networking.models

import org.json.JSONObject

open class AnahuacAPI(
    override var url: String,
    override var method: HTTPMethod,
    override var encoding: Encoding,
    override var parameters: JSONObject?,
    override var headers: Map<String, String>? = null
) : TargetType