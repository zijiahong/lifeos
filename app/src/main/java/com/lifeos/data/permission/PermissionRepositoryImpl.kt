package com.lifeos.data.permission

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.os.Process
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import com.lifeos.domain.permission.AllPermissionState
import com.lifeos.domain.permission.HealthConnectPermissions
import com.lifeos.domain.permission.PermissionRepository
import com.lifeos.domain.permission.PermissionStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PermissionRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    // Fix #9: obtain system services once in constructor rather than on every call
    private val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    override fun checkUsageStatsPermission(): PermissionStatus {
        val mode = appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return if (mode == AppOpsManager.MODE_ALLOWED) PermissionStatus.GRANTED else PermissionStatus.DENIED
    }

    // Fix #6: isAvailable() is the correct API in alpha11 (getSdkStatus introduced later)
    override fun checkHealthConnectAvailability(): Boolean {
        return HealthConnectClient.isAvailable(context)
    }

    // Fix #7: returns HealthConnectPermissions instead of Triple
    override suspend fun checkHealthConnectPermissions(): HealthConnectPermissions {
        return try {
            val client = HealthConnectClient.getOrCreate(context)
            val granted = client.permissionController.getGrantedPermissions()
            HealthConnectPermissions(
                steps = if (HealthPermission.getReadPermission(StepsRecord::class) in granted) PermissionStatus.GRANTED else PermissionStatus.DENIED,
                heartRate = if (HealthPermission.getReadPermission(HeartRateRecord::class) in granted) PermissionStatus.GRANTED else PermissionStatus.DENIED,
                sleep = if (HealthPermission.getReadPermission(SleepSessionRecord::class) in granted) PermissionStatus.GRANTED else PermissionStatus.DENIED
            )
        } catch (e: Exception) {
            HealthConnectPermissions(PermissionStatus.DENIED, PermissionStatus.DENIED, PermissionStatus.DENIED)
        }
    }

    override fun isXiaomiDevice(): Boolean {
        return Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)
    }

    override fun checkBatteryOptimizationIgnored(): PermissionStatus {
        return if (powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
            PermissionStatus.GRANTED
        } else {
            PermissionStatus.DENIED
        }
    }

    override fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    override fun markOnboardingCompleted() {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply()
    }

    override suspend fun getAllPermissionState(): AllPermissionState {
        val hcAvailable = checkHealthConnectAvailability()
        val hcPerms = if (hcAvailable) {
            checkHealthConnectPermissions()
        } else {
            HealthConnectPermissions(
                PermissionStatus.NOT_DETERMINED,
                PermissionStatus.NOT_DETERMINED,
                PermissionStatus.NOT_DETERMINED
            )
        }
        // Fix #8: onboardingCompleted not included — caller reads it directly
        return AllPermissionState(
            usageStats = checkUsageStatsPermission(),
            healthConnectAvailable = hcAvailable,
            healthSteps = hcPerms.steps,
            healthHeartRate = hcPerms.heartRate,
            healthSleep = hcPerms.sleep,
            isXiaomiDevice = isXiaomiDevice(),
            batteryOptIgnored = checkBatteryOptimizationIgnored()
        )
    }

    companion object {
        private const val PREFS_NAME = "permission_prefs"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }
}
