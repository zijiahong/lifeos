package com.lifeos.data.permission

import android.app.AppOpsManager
import android.content.Context
import android.content.SharedPreferences
import android.os.PowerManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import com.lifeos.domain.permission.PermissionStatus

class PermissionRepositoryImplTest {

    private lateinit var context: Context
    private lateinit var appOpsManager: AppOpsManager
    private lateinit var powerManager: PowerManager
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var prefsEditor: SharedPreferences.Editor

    @Before
    fun setUp() {
        context = mock()
        appOpsManager = mock()
        powerManager = mock()
        sharedPrefs = mock()
        prefsEditor = mock()

        whenever(context.packageName).thenReturn("com.lifeos")
        // Fix #9: system services are now fetched in the constructor, so stubs must be set here
        whenever(context.getSystemService(Context.APP_OPS_SERVICE)).thenReturn(appOpsManager)
        whenever(context.getSystemService(Context.POWER_SERVICE)).thenReturn(powerManager)
        whenever(context.getSharedPreferences(any(), any())).thenReturn(sharedPrefs)
        whenever(sharedPrefs.edit()).thenReturn(prefsEditor)
        whenever(prefsEditor.putBoolean(any(), any())).thenReturn(prefsEditor)
    }

    private fun buildRepo() = PermissionRepositoryImpl(context)

    @Test
    fun `checkUsageStatsPermission returns GRANTED when AppOps MODE_ALLOWED`() {
        whenever(
            appOpsManager.checkOpNoThrow(
                eq(AppOpsManager.OPSTR_GET_USAGE_STATS),
                any(),
                eq("com.lifeos")
            )
        ).thenReturn(AppOpsManager.MODE_ALLOWED)

        val result = buildRepo().checkUsageStatsPermission()

        assertEquals(PermissionStatus.GRANTED, result)
    }

    @Test
    fun `checkUsageStatsPermission returns DENIED when AppOps MODE_IGNORED`() {
        whenever(
            appOpsManager.checkOpNoThrow(
                eq(AppOpsManager.OPSTR_GET_USAGE_STATS),
                any(),
                eq("com.lifeos")
            )
        ).thenReturn(AppOpsManager.MODE_IGNORED)

        val result = buildRepo().checkUsageStatsPermission()

        assertEquals(PermissionStatus.DENIED, result)
    }

    @Test
    fun `checkBatteryOptimizationIgnored returns GRANTED when battery optimization is ignored`() {
        whenever(powerManager.isIgnoringBatteryOptimizations("com.lifeos")).thenReturn(true)

        val result = buildRepo().checkBatteryOptimizationIgnored()

        assertEquals(PermissionStatus.GRANTED, result)
    }

    @Test
    fun `checkBatteryOptimizationIgnored returns DENIED when battery optimization is active`() {
        whenever(powerManager.isIgnoringBatteryOptimizations("com.lifeos")).thenReturn(false)

        val result = buildRepo().checkBatteryOptimizationIgnored()

        assertEquals(PermissionStatus.DENIED, result)
    }

    @Test
    fun `isOnboardingCompleted returns false by default`() {
        whenever(sharedPrefs.getBoolean(any(), eq(false))).thenReturn(false)

        val result = buildRepo().isOnboardingCompleted()

        assertFalse(result)
    }

    @Test
    fun `isOnboardingCompleted returns true when flag is set`() {
        whenever(sharedPrefs.getBoolean(any(), eq(false))).thenReturn(true)

        val result = buildRepo().isOnboardingCompleted()

        assertTrue(result)
    }

    @Test
    fun `markOnboardingCompleted persists flag via SharedPreferences`() {
        // Fix #1: previous last test had no assertions — replaced with this meaningful test
        buildRepo().markOnboardingCompleted()

        verify(prefsEditor).putBoolean("onboarding_completed", true)
        verify(prefsEditor).apply()
    }

    @Test
    fun `checkUsageStatsPermission returns DENIED for any non-ALLOWED mode`() {
        listOf(AppOpsManager.MODE_ERRORED, AppOpsManager.MODE_DEFAULT).forEach { mode ->
            whenever(
                appOpsManager.checkOpNoThrow(any(), any(), any())
            ).thenReturn(mode)

            val result = buildRepo().checkUsageStatsPermission()

            assertEquals("Expected DENIED for mode $mode", PermissionStatus.DENIED, result)
        }
    }
}
