package org.eloqium.tts.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import org.eloqium.tts.R
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.eloqium.tts.service.EloqiumForegroundService
import org.eloqium.tts.service.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForegroundServiceSettingsScreen(
    settings: Settings,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var persistentNotification by remember { mutableStateOf(settings.persistentNotification) }

    fun checkNotificationsEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED &&
                    NotificationManagerCompat.from(context).areNotificationsEnabled()
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun checkBatteryExempt(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            pm?.isIgnoringBatteryOptimizations(context.packageName) == true
        } else {
            true
        }
    }

    var notificationsEnabled by remember { mutableStateOf(checkNotificationsEnabled()) }
    var batteryExempt by remember { mutableStateOf(checkBatteryExempt()) }

    // Re-check permissions and exemption state whenever the user returns to this screen
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsEnabled = checkNotificationsEnabled()
                batteryExempt = checkBatteryExempt()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notificationsEnabled = isGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun openNotificationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationsEnabled) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }

        // Open application notification settings
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val intent = Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)
                }
                context.startActivity(intent)
            } else {
                val intent = Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            }
        } catch (_: Throwable) {
            try {
                val intent = Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            } catch (_: Throwable) {
                // Ignore if settings cannot be opened
            }
        }
    }

    fun openBatterySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!batteryExempt) {
                try {
                    val intent = Intent(AndroidSettings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                    return
                } catch (_: Throwable) {
                    // OEM direct prompt unsupported, fall back to general battery settings
                }
            }

            try {
                val intent = Intent(AndroidSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                context.startActivity(intent)
            } catch (_: Throwable) {
                try {
                    val intent = Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                } catch (_: Throwable) {
                    // Safe fallback
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Eloqium foreground service",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { contentDescription = "Navigate up" }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Control 1: App notifications
            val notificationStatus = if (notificationsEnabled) "Allowed" else "Not allowed"
            SettingsClickableItem(
                title = "App notifications",
                subtitle = "$notificationStatus. Allow notifications for Eloqium's foreground service.",
                onClick = { openNotificationSettings() }
            )

            // Explanatory Section: About notifications
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "About notifications",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { heading() }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Android requires a visible ongoing notification while a foreground service is active to indicate that the application is running.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = DividerDefaults.color.copy(alpha = 0.5f)
            )

            // Control 2: Persistent notification switch (Default: ON)
            SettingsSwitchItem(
                title = "Show persistent notification",
                subtitle = "Keep a notification visible while the foreground service is running.",
                checked = persistentNotification,
                onCheckedChange = { isChecked ->
                    persistentNotification = isChecked
                    settings.persistentNotification = isChecked
                    EloqiumForegroundService.syncService(context, settings)
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = DividerDefaults.color.copy(alpha = 0.5f)
            )

            // Control 3: Battery optimization
            val batterySubtitle = if (batteryExempt) {
                "Unrestricted. Eloqium is allowed to run without battery restrictions."
            } else {
                "Optimized. Allow Eloqium to run without battery restrictions."
            }
            SettingsClickableItem(
                title = "Battery optimization",
                subtitle = batterySubtitle,
                onClick = { openBatterySettings() }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
