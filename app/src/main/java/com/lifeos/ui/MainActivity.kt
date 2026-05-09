package com.lifeos.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lifeos.domain.permission.PermissionRepository
import com.lifeos.ui.navigation.AppNavigation
import com.lifeos.ui.theme.LifeOsTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var permissionRepository: PermissionRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val startOnboarding = !permissionRepository.isOnboardingCompleted()
        setContent {
            LifeOsTheme {
                AppNavigation(startOnboarding = startOnboarding)
            }
        }
    }
}
