package com.example.ecodexandroid

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object TrefleClient {

    // Igual que en la web pero completo:
    private const val BASE_URL = "https://trefle.io/api/v1/"

    // 👇 Pega aquí el MISMO token que tienes en tu .env de la web
    // VITE_TREFLE_TOKEN=...
    private const val TREFLE_TOKEN = "usr-Oa4OGBvqoYN096VjQg4S8-CVLLyh5AAkIN0rB_2fV_k" 
    //                       ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    //         Sustituye por tu token real si es distinto

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val newUrl = original.url.newBuilder()
                .addQueryParameter("token", TREFLE_TOKEN)
                .build()

            val newRequest = original.newBuilder()
                .url(newUrl)
                .build()

            chain.proceed(newRequest)
        }
        .build()

    val api: TrefleApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TrefleApi::class.java)
    }
}
