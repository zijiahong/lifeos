package com.lifeos.ui.screens.permission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.domain.permission.AllPermissionState
import com.lifeos.domain.permission.CompleteOnboardingUseCase
import com.lifeos.domain.permission.GetPermissionStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PermissionUiState {
    object Loading : PermissionUiState()
    data class Loaded(val state: AllPermissionState) : PermissionUiState()
}

@HiltViewModel
class PermissionViewModel @Inject constructor(
    private val getPermissionStateUseCase: GetPermissionStateUseCase,
    private val completeOnboardingUseCase: CompleteOnboardingUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<PermissionUiState>(PermissionUiState.Loading)
    val uiState: StateFlow<PermissionUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null

    init {
        refreshPermissions()
    }

    fun refreshPermissions() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            try {
                _uiState.value = PermissionUiState.Loaded(getPermissionStateUseCase())
            } catch (_: Exception) {
                // leave existing state unchanged; user can retry via ON_RESUME
            }
        }
    }

    fun completeOnboarding() {
        completeOnboardingUseCase()
    }
}
