package com.myproject.kenyaair.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.myproject.kenyaair.data.AirRepository
import com.myproject.kenyaair.data.net.LatestResult
import com.myproject.kenyaair.data.net.Location
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface UiState {
    object Idle : UiState
    object Loading : UiState
    data class Error(val message: String) : UiState
}

class AirViewModel(private val repo: AirRepository) : ViewModel() {
    private val _stations = MutableStateFlow<List<Location>>(emptyList())
    val stations = _stations.asStateFlow()

    private val _latest = MutableStateFlow<List<LatestResult>>(emptyList())
    val latest = _latest.asStateFlow()

    private val _state = MutableStateFlow<UiState>(UiState.Idle)
    val state = _state.asStateFlow()

    var selected: Location? = null
        private set

    fun loadStations() = viewModelScope.launch {
        _state.value = UiState.Loading
        try {
            _stations.value = repo.kenyaStations()
            _state.value = UiState.Idle
        } catch (e: Exception) {
            _state.value = UiState.Error(e.localizedMessage ?: "Failed to load stations")
        }
    }

    fun openStation(location: Location) {
        selected = location
        loadLatest(location.id)
    }

    private fun loadLatest(id: Int) = viewModelScope.launch {
        _state.value = UiState.Loading
        try {
            _latest.value = repo.latestFor(id)
            _state.value = UiState.Idle
        } catch (e: Exception) {
            _state.value = UiState.Error(e.localizedMessage ?: "Failed to load latest")
        }
    }
}

@Suppress("UNCHECKED_CAST")
class AirVMFactory(private val repo: AirRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = AirViewModel(repo) as T
}
