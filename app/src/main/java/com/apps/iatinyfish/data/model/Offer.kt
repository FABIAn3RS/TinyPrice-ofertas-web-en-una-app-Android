package com.apps.iatinyfish.data.model

import com.google.gson.annotations.SerializedName

data class Offer(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("store")
    val store: String = "Tienda",
    @SerializedName("productName")
    val productName: String = "",
    @SerializedName("price")
    val price: String = "Ver en fuente",
    @SerializedName("sourceUrl")
    val sourceUrl: String = "",
    @SerializedName("summary")
    val summary: String = "",
    @SerializedName("tags")
    val tags: List<String> = emptyList()
)

data class SearchRequest(
    @SerializedName("query")
    val query: String = ""
)

data class SearchResponse(
    @SerializedName("query")
    val query: String = "",
    @SerializedName("offers")
    val offers: List<Offer> = emptyList(),
    @SerializedName("searchedAt")
    val searchedAt: String? = null
)
