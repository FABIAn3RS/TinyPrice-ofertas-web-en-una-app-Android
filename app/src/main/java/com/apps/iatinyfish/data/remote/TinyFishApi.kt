package com.apps.iatinyfish.data.remote

import com.apps.iatinyfish.data.model.SearchRequest
import com.apps.iatinyfish.data.model.SearchResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface TinyFishApi {
    @POST("search-offers")
    suspend fun searchOffers(
        @Body request: SearchRequest,
        @Header("apikey") apiKey: String? = null,
        @Header("Authorization") authHeader: String? = null
    ): SearchResponse
}
