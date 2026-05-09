package com.lifeos.domain.permission

import javax.inject.Inject

class GetPermissionStateUseCase @Inject constructor(
    private val repository: PermissionRepository
) {
    suspend operator fun invoke(): AllPermissionState = repository.getAllPermissionState()
}
