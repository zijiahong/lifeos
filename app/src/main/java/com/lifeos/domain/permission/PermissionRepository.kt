package com.lifeos.domain.permission

// Fix #7: named data class instead of Triple for readability
data class HealthConnectPermissions(
    val steps: PermissionStatus,
    val heartRate: PermissionStatus,
    val sleep: PermissionStatus
)

// Fix #8: removed onboardingCompleted — it is not a permission state
data class AllPermissionState(
    val usageStats: PermissionStatus,
    val healthConnectAvailable: Boolean,
    val healthSteps: PermissionStatus,
    val healthHeartRate: PermissionStatus,
    val healthSleep: PermissionStatus,
    val isXiaomiDevice: Boolean,
    val batteryOptIgnored: PermissionStatus
)

interface PermissionRepository {
    fun checkUsageStatsPermission(): PermissionStatus
    // Fix #6: getSdkStatus is synchronous — no suspend needed
    fun checkHealthConnectAvailability(): Boolean
    suspend fun checkHealthConnectPermissions(): HealthConnectPermissions
    fun isXiaomiDevice(): Boolean
    fun checkBatteryOptimizationIgnored(): PermissionStatus
    fun isOnboardingCompleted(): Boolean
    fun markOnboardingCompleted()
    suspend fun getAllPermissionState(): AllPermissionState
}
