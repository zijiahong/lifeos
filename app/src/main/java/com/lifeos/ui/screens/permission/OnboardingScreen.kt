package com.lifeos.ui.screens.permission

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.lifeos.domain.permission.AllPermissionState
import com.lifeos.domain.permission.PermissionStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: PermissionViewModel = hiltViewModel(),
    onOnboardingComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val healthPermissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class)
    )

    val requestHealthPermissions = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) {
        viewModel.refreshPermissions()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("完成设置") },
                actions = {
                    TextButton(onClick = {
                        viewModel.completeOnboarding()
                        onOnboardingComplete()
                    }) {
                        Text("跳过")
                    }
                }
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is PermissionUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is PermissionUiState.Loaded -> {
                OnboardingContent(
                    state = state.state,
                    modifier = Modifier.padding(innerPadding),
                    onRequestHealthPermissions = {
                        requestHealthPermissions.launch(healthPermissions)
                    },
                    onOpenUsageStats = {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    },
                    onOpenBatteryOptimization = {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    },
                    onOpenAutoStart = {
                        try {
                            val intent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                                setClassName(
                                    "com.miui.securitycenter",
                                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                                )
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(fallback)
                        }
                    },
                    onComplete = {
                        viewModel.completeOnboarding()
                        onOnboardingComplete()
                    }
                )
            }
        }
    }
}

@Composable
private fun OnboardingContent(
    state: AllPermissionState,
    modifier: Modifier = Modifier,
    onRequestHealthPermissions: () -> Unit,
    onOpenUsageStats: () -> Unit,
    onOpenBatteryOptimization: () -> Unit,
    onOpenAutoStart: () -> Unit,
    onComplete: () -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "授权以下权限，让 LifeOS 正常运行",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        item {
            PermissionCard(
                icon = Icons.Default.QueryStats,
                title = "应用使用时间",
                description = "读取屏幕时长和各应用使用时长（需在系统设置中手动开启）",
                status = state.usageStats,
                actionLabel = if (state.usageStats == PermissionStatus.GRANTED) null else "去开启",
                onAction = onOpenUsageStats
            )
        }

        item {
            if (state.healthConnectAvailable) {
                val hcAllGranted = state.healthSteps == PermissionStatus.GRANTED &&
                    state.healthHeartRate == PermissionStatus.GRANTED &&
                    state.healthSleep == PermissionStatus.GRANTED
                PermissionCard(
                    icon = Icons.Default.Favorite,
                    title = "Health Connect 健康数据",
                    description = "读取步数、心率和睡眠数据",
                    status = if (hcAllGranted) PermissionStatus.GRANTED else PermissionStatus.DENIED,
                    actionLabel = if (hcAllGranted) null else "授权",
                    onAction = onRequestHealthPermissions
                )
            } else {
                PermissionCard(
                    icon = Icons.Default.Favorite,
                    title = "Health Connect 健康数据",
                    description = "设备不支持 Health Connect 或需要更新",
                    status = PermissionStatus.NOT_DETERMINED,
                    actionLabel = null,
                    onAction = {}
                )
            }
        }

        item {
            PermissionCard(
                icon = Icons.Default.Battery5Bar,
                title = "电池优化白名单",
                description = "将 LifeOS 加入电池优化白名单，防止后台任务被系统杀死",
                status = state.batteryOptIgnored,
                actionLabel = if (state.batteryOptIgnored == PermissionStatus.GRANTED) null else "去设置",
                onAction = onOpenBatteryOptimization
            )
        }

        if (state.isXiaomiDevice) {
            item {
                PermissionCard(
                    icon = Icons.Default.PhoneAndroid,
                    title = "自启动（HyperOS）",
                    description = "允许 LifeOS 自启动，确保 WorkManager 后台任务正常运行",
                    status = PermissionStatus.NOT_DETERMINED,
                    actionLabel = "去开启",
                    onAction = onOpenAutoStart,
                    note = "系统无法自动检测此权限状态，请手动确认"
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("完成设置")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    status: PermissionStatus,
    actionLabel: String?,
    onAction: () -> Unit,
    note: String? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                StatusIcon(status = status)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (note != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = note,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            if (actionLabel != null) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onAction,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun StatusIcon(status: PermissionStatus) {
    when (status) {
        PermissionStatus.GRANTED -> Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "已授权",
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(20.dp)
        )
        PermissionStatus.DENIED -> Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "未授权",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp)
        )
        PermissionStatus.NOT_DETERMINED -> Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "未知",
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp)
        )
    }
}
