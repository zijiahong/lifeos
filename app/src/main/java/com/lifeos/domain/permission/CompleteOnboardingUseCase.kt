package com.lifeos.domain.permission

import javax.inject.Inject

class CompleteOnboardingUseCase @Inject constructor(
    private val repository: PermissionRepository
) {
    operator fun invoke() = repository.markOnboardingCompleted()
}
