package com.lifeos.domain.permission

data class AllPermissionState(
    val usageStats: PermissionStatus,
    val healthConnectAvailable: Boolean,
    val healthSteps: PermissionStatus,
    val healthHeartRate: PermissionStatus,
    val healthSleep: PermissionStatus,
    val isXiaomiDevice: Boolean,
    val batteryOptIgnored: PermissionStatus,
    val onboardingCompleted: Boolean
)

interface PermissionRepository {
    fun checkUsageStatsPermission(): PermissionStatus
    suspend fun checkHealthConnectAvailability(): Boolean
    suspend fun checkHealthConnectPermissions(): Triple<PermissionStatus, PermissionStatus, PermissionStatus>
    fun isXiaomiDevice(): Boolean
    fun checkBatteryOptimizationIgnored(): PermissionStatus
    fun isOnboardingCompleted(): Boolean
    fun markOnboardingCompleted()
    suspend fun getAllPermissionState(): AllPermissionState
}
