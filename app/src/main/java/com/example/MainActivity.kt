package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.AllReservesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel

private const val REQUEST_CODE_POST_NOTIFICATIONS = 1001

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            MyApplicationTheme(darkTheme = uiState.isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SamadApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun SamadApp(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val context = LocalContext.current

    // Request notification permission on Android 13+ using a 16-bit safe request code
    // compatible with FragmentActivity (used by BiometricPrompt).
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) {
                    val activity = context as? android.app.Activity
                    if (activity != null) {
                        ActivityCompat.requestPermissions(
                            activity,
                            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                            REQUEST_CODE_POST_NOTIFICATIONS
                        )
                    }
                }
            } catch (_: Throwable) {
            }
        }
    }

    val initialStartDestination = remember { if (uiState.isLoggedIn) "home" else "login" }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Security Hardening: Apply FLAG_SECURE on login and settings to prevent screenshot/screen record leaks
    // Do NOT apply on home / QR screens so barcodes remain screenshot- and brightness-friendly
    LaunchedEffect(currentRoute) {
        try {
            val window = (context as? android.app.Activity)?.window
            if (currentRoute == "login" || currentRoute == "settings") {
                window?.setFlags(
                    android.view.WindowManager.LayoutParams.FLAG_SECURE,
                    android.view.WindowManager.LayoutParams.FLAG_SECURE
                )
            } else {
                window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
            }
        } catch (_: Throwable) {
        }
    }

    val showBottomBar = currentRoute in listOf("home", "settings")

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = androidx.compose.ui.unit.Dp(8f),
                        modifier = Modifier.testTag("main_bottom_nav_bar")
                    ) {
                        NavigationBarItem(
                            selected = currentRoute == "home",
                            onClick = {
                                if (currentRoute != "home") {
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.QrCode2,
                                    contentDescription = "میزکار و کارت"
                                )
                            },
                            label = { Text("میزکار") },
                            modifier = Modifier.testTag("nav_item_home")
                        )

                        NavigationBarItem(
                            selected = currentRoute == "settings",
                            onClick = {
                                if (currentRoute != "settings") {
                                    navController.navigate("settings") {
                                        launchSingleTop = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = "تنظیمات"
                                )
                            },
                            label = { Text("تنظیمات") },
                            modifier = Modifier.testTag("nav_item_settings")
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = initialStartDestination,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                composable("login") {
                    LoginScreen(
                        viewModel = viewModel,
                        uiState = uiState,
                        onLoginSuccess = {
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    )
                }

                composable("home") {
                    HomeScreen(
                        viewModel = viewModel,
                        uiState = uiState,
                        onNavigateToSettings = { navController.navigate("settings") },
                        onNavigateToAllReserves = { navController.navigate("all_reserves") }
                    )
                }

                composable("settings") {
                    SettingsScreen(
                        viewModel = viewModel,
                        uiState = uiState,
                        onNavigateBack = { navController.popBackStack() },
                        onLogoutDone = {
                            navController.navigate("login") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }

                composable("all_reserves") {
                    AllReservesScreen(
                        uiState = uiState,
                        onNavigateBack = { navController.popBackStack() },
                        onSelectReserve = { date, mealTypeId ->
                            viewModel.selectDate(date)
                            viewModel.selectMealType(mealTypeId)
                        }
                    )
                }
            }
        }
    }
}
