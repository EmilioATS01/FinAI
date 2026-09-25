package emilio.tolosa.finai.network

import retrofit2.http.Body
import retrofit2.http.POST

interface OpenAiApi {
    @POST("v1/chat/completions")
    suspend fun chat(@Body body: ChatRequest): ChatResponse
}

data class ChatMessage(val role: String, val content: Any)
data class ChatRequest(val model: String = "gpt-4o-mini", val messages: List<ChatMessage>)
data class ChatResponse(val choices: List<Choice>)
data class Choice(val message: ChatMessage)