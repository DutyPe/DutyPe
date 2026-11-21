package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.models.PromoShowcase
import com.example.dutype.repositories.PromoShowcaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PromoShowcaseViewModel @Inject constructor(
    private val repository: PromoShowcaseRepository
) : ViewModel() {

    val showcases: StateFlow<List<PromoShowcase>> = repository.getShowcasesFlow()
        .map { list -> list.filter { it.active } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    suspend fun getShowcase(showcaseId: String): PromoShowcase? {
        return repository.getShowcase(showcaseId)
    }
}
