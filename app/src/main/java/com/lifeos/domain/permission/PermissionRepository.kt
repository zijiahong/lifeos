package com.lifeos.domain.permission

enum class HealthConnectStatus {
    AVAILABLE,
    NOT_INSTALLED,
    UPDATE_REQUIRED
}

data class HealthConnectPermissions(
    val steps: PermissionStatus,
    val heartRate: PermissionStatus,
    val sleep: PermissionStatus
)

data class AllPermissionState(
    val usageStats: PermissionStatus,
    val healthConnectStatus: HealthConnectStatus,
    val healthSteps: PermissionStatus,
    val healthHeartRate: PermissionStatus,
    val healthSleep: PermissionStatus,
    val isXiaomiDevice: Boolean,
    val batteryOptIgnored: PermissionStatus
)

interface PermissionRepository {
    fun checkUsageStatsPermission(): PermissionStatus
    fun checkHealthConnectStatus(): HealthConnectStatus
    suspend fun checkHealthConnectPermissions(): HealthConnectPermissions
    fun isXiaomiDevice(): Boolean
    fun checkBatteryOptimizationIgnored(): PermissionStatus
    fun isOnboardingCompleted(): Boolean
    fun markOnboardingCompleted()
    suspend fun getAllPermissionState(): AllPermissionState
}
