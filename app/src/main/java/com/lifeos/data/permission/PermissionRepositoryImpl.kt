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

    override fun checkUsageStatsPermission(): PermissionStatus {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return if (mode == AppOpsManager.MODE_ALLOWED) PermissionStatus.GRANTED else PermissionStatus.DENIED
    }

    override suspend fun checkHealthConnectAvailability(): Boolean {
        return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }

    override suspend fun checkHealthConnectPermissions(): Triple<PermissionStatus, PermissionStatus, PermissionStatus> {
        return try {
            val client = HealthConnectClient.getOrCreate(context)
            val granted = client.permissionController.getGrantedPermissions()
            Triple(
                if (HealthPermission.getReadPermission(StepsRecord::class) in granted) PermissionStatus.GRANTED else PermissionStatus.DENIED,
                if (HealthPermission.getReadPermission(HeartRateRecord::class) in granted) PermissionStatus.GRANTED else PermissionStatus.DENIED,
                if (HealthPermission.getReadPermission(SleepSessionRecord::class) in granted) PermissionStatus.GRANTED else PermissionStatus.DENIED
            )
        } catch (e: Exception) {
            Triple(PermissionStatus.DENIED, PermissionStatus.DENIED, PermissionStatus.DENIED)
        }
    }

    override fun isXiaomiDevice(): Boolean {
        return Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)
    }

    override fun checkBatteryOptimizationIgnored(): PermissionStatus {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return if (pm.isIgnoringBatteryOptimizations(context.packageName)) {
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
            Triple(PermissionStatus.NOT_DETERMINED, PermissionStatus.NOT_DETERMINED, PermissionStatus.NOT_DETERMINED)
        }
        return AllPermissionState(
            usageStats = checkUsageStatsPermission(),
            healthConnectAvailable = hcAvailable,
            healthSteps = hcPerms.first,
            healthHeartRate = hcPerms.second,
            healthSleep = hcPerms.third,
            isXiaomiDevice = isXiaomiDevice(),
            batteryOptIgnored = checkBatteryOptimizationIgnored(),
            onboardingCompleted = isOnboardingCompleted()
        )
    }

    companion object {
        private const val PREFS_NAME = "permission_prefs"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }
}
