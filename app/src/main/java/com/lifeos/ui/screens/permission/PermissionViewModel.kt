package com.lifeos.ui.screens.permission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.domain.permission.AllPermissionState
import com.lifeos.domain.permission.PermissionRepository
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
    private val permissionRepository: PermissionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PermissionUiState>(PermissionUiState.Loading)
    val uiState: StateFlow<PermissionUiState> = _uiState.asStateFlow()

    // Fix #2: track the active job so concurrent calls cancel the stale one
    private var refreshJob: Job? = null

    init {
        refreshPermissions()
    }

    fun refreshPermissions() {
        // Fix #2 + #4: cancelling the previous job prevents both race conditions
        // and the double-IPC on first launch (ON_RESUME cancels the init coroutine
        // before it starts, so only one network/IPC call is made)
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val state = permissionRepository.getAllPermissionState()
            _uiState.value = PermissionUiState.Loaded(state)
        }
    }

    fun completeOnboarding() {
        permissionRepository.markOnboardingCompleted()
    }
}
