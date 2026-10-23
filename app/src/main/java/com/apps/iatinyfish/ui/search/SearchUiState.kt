package com.apps.iatinyfish.ui.search

import com.apps.iatinyfish.data.model.ProductOffer

sealed interface SearchUiState {
    data object Initial : SearchUiState
    data object Loading : SearchUiState
    data class Success(val offers: List<ProductOffer>, val isPreview: Boolean = false) : SearchUiState
    data class Error(val message: String) : SearchUiState
}
