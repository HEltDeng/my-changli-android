package com.mychangli.app.overview.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mychangli.app.core.PlanetApplication
import com.mychangli.app.overview.data.OverviewRepository
import com.mychangli.app.overview.ui.model.OverviewUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OverviewViewModel : ViewModel() {
    private val repository by lazy { OverviewRepository(PlanetApplication.appContext) }
    private var loadJob: Job? = null
    private val _uiState = MutableStateFlow(OverviewUiState())
    val uiState: StateFlow<OverviewUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onResume() = load()

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = repository.loadLocalState()
        }
    }
}
