package emilio.tolosa.finai.network

import emilio.tolosa.finai.BuildConfig
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private val log = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }

    private fun http(vararg extra: Interceptor) = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .apply { extra.forEach { addInterceptor(it) } }
        .addInterceptor(log)
        .build()

    private fun retrofit(base: String, client: OkHttpClient) = Retrofit.Builder()
        .baseUrl(base)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val exchange: ExchangeApi by lazy {
        retrofit("https://v6.exchangerate-api.com/", http()).create(ExchangeApi::class.java)
    }

    val openai: OpenAiApi by lazy {
        val auth = Interceptor { chain ->
            chain.proceed(chain.request().newBuilder()
                .header("Authorization", "Bearer ${BuildConfig.OPENAI_KEY}").build())
        }
        retrofit("https://api.openai.com/", http(auth)).create(OpenAiApi::class.java)
    }

    val belvo: BelvoApi by lazy {
        val cred = Credentials.basic(BuildConfig.BELVO_ID, BuildConfig.BELVO_SECRET)
        val auth = Interceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("Authorization", cred).build())
        }
        retrofit("https://sandbox.belvo.com/", http(auth)).create(BelvoApi::class.java)
    }
}