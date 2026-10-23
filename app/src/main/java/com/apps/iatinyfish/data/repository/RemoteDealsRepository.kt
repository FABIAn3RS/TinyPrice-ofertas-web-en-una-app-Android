package com.apps.iatinyfish.data.repository

import com.apps.iatinyfish.BuildConfig
import com.apps.iatinyfish.data.model.ProductOffer
import com.apps.iatinyfish.data.remote.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import retrofit2.Response

class RemoteDealsRepository(
    private val api: TinyPriceApi = TinyPriceApiFactory.create(),
    private val gson: Gson = Gson(),
) : DealsRepository {
    override val isPreviewData = false

    override suspend fun search(query: String): List<ProductOffer> =
        withContext(Dispatchers.IO) {
            try {
                val anonKey = BuildConfig.SUPABASE_ANON_KEY
                val authHeader = if (anonKey.isNotBlank()) "Bearer $anonKey" else null
                val apiKeyHeader = if (anonKey.isNotBlank()) anonKey else null

                val response = api.searchProducts(
                    request = SearchProductsRequest(query),
                    apiKey = apiKeyHeader,
                    authHeader = authHeader
                )
                if (!response.isSuccessful) {
                    throw RemoteSearchException(response.errorMessage())
                }
                response.body()?.offers.orEmpty().map { it.toProductOffer() }
            } catch (error: RemoteSearchException) {
                throw error
            } catch (_: SocketTimeoutException) {
                throw RemoteSearchException(
                    "La búsqueda está tardando más de lo esperado. Inténtalo de nuevo en unos segundos.",
                )
            } catch (_: Exception) {
                throw RemoteSearchException(
                    "No se pudo conectar con la búsqueda en vivo. Revisa tu conexión e inténtalo de nuevo.",
                )
            }
        }

    private fun Response<*>.errorMessage(): String = runCatching {
        errorBody()?.charStream()?.use { reader ->
            gson.fromJson(reader, ApiErrorResponse::class.java)?.error
        }
    }.getOrNull().orEmpty().ifBlank {
        "No fue posible hacer la búsqueda en vivo. Inténtalo de nuevo."
    }
}

class RemoteSearchException(message: String) : Exception(message)

object DealsRepositoryProvider {
    fun create(): DealsRepository =
        if (BuildConfig.USE_REMOTE_DEALS) RemoteDealsRepository()
        else PreviewDealsRepository
}
