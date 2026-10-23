package com.apps.iatinyfish.domain.repository

import com.apps.iatinyfish.data.model.Offer

interface OfferRepository {
    suspend fun searchOffers(query: String): Result<List<Offer>>
}
