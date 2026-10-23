package com.apps.iatinyfish.data.repository

import android.util.Log
import com.apps.iatinyfish.BuildConfig
import com.apps.iatinyfish.data.model.Offer
import com.apps.iatinyfish.data.model.SearchRequest
import com.apps.iatinyfish.data.remote.TinyFishApi
import com.apps.iatinyfish.domain.repository.OfferRepository
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class OfferRepositoryImpl : OfferRepository {

    private val api: TinyFishApi by lazy {
        val baseUrl = BuildConfig.EDGE_RUNTIME_BASE_URL.let {
            if (it.endsWith("/")) it else "$it/"
        }
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TinyFishApi::class.java)
    }

    override suspend fun searchOffers(query: String): Result<List<Offer>> {
        if (!BuildConfig.USE_REMOTE_DEALS) {
            Log.d("OfferRepository", "USE_REMOTE_DEALS es false: Devolviendo ofertas ficticias locales para '$query'")
            val mockOffers = listOf(
                Offer(
                    id = "mock-1",
                    store = "TinyStore Demo",
                    productName = "Producto Demo para: $query",
                    price = "$99.99",
                    sourceUrl = "https://www.google.com/search?q=${query}",
                    summary = "Oferta ficticia local de prueba. Activa USE_REMOTE_DEALS para usar Supabase.",
                    tags = listOf("Demo Local", "Ficticio")
                ),
                Offer(
                    id = "mock-2",
                    store = "Tienda Ejemplo",
                    productName = "Edición Especial de $query",
                    price = "$149.50",
                    sourceUrl = "https://www.google.com/search?q=${query}+oferta",
                    summary = "Segunda oferta simulada para verificar la interfaz de usuario en modo local.",
                    tags = listOf("Oferta Especial", "Demo")
                )
            )
            return Result.success(mockOffers)
        }

        return try {
            Log.d("SupabaseNetwork", "USE_REMOTE_DEALS es true: Invocando API remota en ${BuildConfig.EDGE_RUNTIME_BASE_URL}")
            val anonKey = BuildConfig.SUPABASE_ANON_KEY
            val authHeader = if (anonKey.isNotBlank()) "Bearer $anonKey" else null
            val apiKeyHeader = if (anonKey.isNotBlank()) anonKey else null

            val response = api.searchOffers(
                request = SearchRequest(query = query),
                apiKey = apiKeyHeader,
                authHeader = authHeader
            )

            Log.d("ComparadorJSON", "Respuesta remota recibida con ${response.offers.size} ofertas")
            Result.success(response.offers)
        } catch (e: Exception) {
            Log.e("SupabaseNetwork", "Error en llamada remota: ${e.message}", e)
            Result.failure(e)
        }
    }
}
