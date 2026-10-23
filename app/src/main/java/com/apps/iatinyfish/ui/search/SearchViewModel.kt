package com.apps.iatinyfish.ui.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apps.iatinyfish.data.repository.DealsRepository
import com.apps.iatinyfish.data.repository.DealsRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel(
    private val repository: DealsRepository = DealsRepositoryProvider.create()
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Initial)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        searchOffers("")
    }

    fun updateQuery(query: String) {
        _searchQuery.value = query
    }

    fun searchOffers(query: String) {
        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            try {
                Log.d("SupabaseNetwork", "Buscando ofertas para: '$query'")
                val offers = repository.search(query)
                Log.d("ComparadorJSON", "Ofertas recibidas: ${offers.size}")
                _uiState.value = SearchUiState.Success(
                    offers = offers,
                    isPreview = repository.isPreviewData
                )
            } catch (e: Exception) {
                Log.e("SupabaseNetwork", "Error buscando ofertas: ${e.message}", e)
                _uiState.value = SearchUiState.Error(
                    e.localizedMessage ?: "Ocurrió un error inesperado."
                )
            }
        }
    }
}
