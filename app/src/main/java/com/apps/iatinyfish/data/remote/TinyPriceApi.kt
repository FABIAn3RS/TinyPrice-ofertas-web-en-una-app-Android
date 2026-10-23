package com.apps.iatinyfish.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface TinyPriceApi {
    @POST("search-offers")
    suspend fun searchProducts(
        @Body request: SearchProductsRequest,
        @Header("apikey") apiKey: String? = null,
        @Header("Authorization") authHeader: String? = null
    ): Response<SearchProductsResponse>
}
