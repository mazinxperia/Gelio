package io.gelio.app.features.admin.kiosk

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SettingsOverscan
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.gelio.app.app.LocalAppContainer
import io.gelio.app.core.ui.ShowcaseBackground
import io.gelio.app.core.ui.ViewerTopBar

@Composable
fun KioskModeScreen(
    viewModel: KioskModeViewModel,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onClose: () -> Unit,
) {
    val appContainer = LocalAppContainer.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context.findActivity()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val runtimeState by viewModel.runtimeState.collectAsStateWithLifecycle()
    var pendingGrantTarget by remember { mutableStateOf<KioskGrantTarget?>(null) }

    val grantLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        viewModel.onGrantResult(pendingGrantTarget)
        pendingGrantTarget = null
    }

    DisposableEffect(lifecycleOwner, activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.endGrantSession()
                viewModel.refreshPermissionState()
                activity?.let { appContainer.kioskController.reapply(it) }
            }
        }
        val lifecycle = lifecycleOwner.lifecycle
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val permissionRows = listOf(
        PermissionRowModel(
            target = KioskGrantTarget.OVERLAY,
            icon = Icons.Rounded.Visibility,
            title = "Display over other apps",
            description = "Lets Gelio install invisible edge shields over the gesture and shade zones.",
            granted = runtimeState.permissionState.overlay,
        ),
        PermissionRowModel(
            target = KioskGrantTarget.USAGE,
            icon = Icons.Rounded.Security,
            title = "Usage access",
            description = "Lets the watchdog detect when another app steals the foreground.",
            granted = runtimeState.permissionState.usageAccess,
        ),
        PermissionRowModel(
            target = KioskGrantTarget.EXACT_ALARMS,
            icon = Icons.Rounded.Timer,
            title = "Exact alarms",
            description = "Schedules self-heal relaunch checks after ordinary service loss.",
            granted = runtimeState.permissionState.exactAlarms,
        ),
        PermissionRowModel(
            target = KioskGrantTarget.BATTERY,
            icon = Icons.Rounded.Shield,
            title = "Ignore battery optimizations",
            description = "Reduces OEM background killing of the kiosk watchdog service.",
            granted = runtimeState.permissionState.ignoreBatteryOptimizations,
        ),
        PermissionRowModel(
            target = KioskGrantTarget.ACCESSIBILITY,
            icon = Icons.Rounded.Lock,
            title = "Accessibility service",
            description = "Best-effort key filtering and fast SystemUI dismissal for power and shade surfaces.",
            granted = runtimeState.permissionState.accessibility,
        ),
        PermissionRowModel(
            target = KioskGrantTarget.HOME,
            icon = Icons.Rounded.Home,
            title = "Default launcher",
            description = "Makes HOME return to Gelio instead of leaving the kiosk shell.",
            granted = runtimeState.permissionState.defaultHome,
        ),
    )

    ShowcaseBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            ViewerTopBar(
                title = "Kiosk Mode",
                subtitle = "Aggressive single-app user-space lockdown",
                onBack = onBack,
                onHome = onHome,
                onClose = onClose,
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp, vertical = 20.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 1380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    StatusHeaderCard(
                        desiredEnabled = settings.kioskModeEnabled,
                        active = runtimeState.active,
                        maintenance = runtimeState.adminMaintenance || runtimeState.grantSession,
                    )

                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            Text(
                                text = "Required grants",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "All six grants must be green before kiosk can be enabled. Grant sessions temporarily relax the watchdog while you are in system settings.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            permissionRows.forEach { row ->
                                PermissionRow(
                                    model = row,
                                    onGrant = {
                                        val intent = viewModel.permissionIntent(row.target) ?: return@PermissionRow
                                        pendingGrantTarget = row.target
                                        viewModel.beginGrantSession()
                                        grantLauncher.launch(intent)
                                    },
                                )
                            }
                        }
                    }

                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Text(
                                text = "Kiosk control",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = if (runtimeState.permissionState.allGranted) {
                                    "Kiosk can now take over Home, immersive fullscreen, overlays, accessibility filtering, screen-off bounce-back, watchdog recovery, and reboot return."
                                } else {
                                    "Finish the grant checklist first. The toggle stays blocked until every required capability is available."
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                Button(
                                    onClick = { activity?.let { appContainer.kioskController.enable(it) } },
                                    enabled = activity != null && runtimeState.permissionState.allGranted && !runtimeState.active,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Enable kiosk")
                                }
                                OutlinedButton(
                                    onClick = { activity?.let { appContainer.kioskController.disable(it) } },
                                    enabled = activity != null && settings.kioskModeEnabled,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Disable kiosk")
                                }
                            }
                        }
                    }

                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Escape path",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Long-press the welcome logo, enter the existing admin PIN, open Settings, then disable Kiosk Mode here. Restored devices keep only the desired flag; kiosk becomes active again only after the required grants exist on that tablet.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusHeaderCard(
    desiredEnabled: Boolean,
    active: Boolean,
    maintenance: Boolean,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (active) "Kiosk mode is active" else if (desiredEnabled) "Kiosk requested, waiting for grants" else "Kiosk mode is off",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (maintenance) {
                        "Maintenance is open. Edge shields are relaxed until you leave admin or finish the grant flow."
                    } else {
                        "Runtime state updates live. No permission snapshot is stored in backup; only the desired enabled flag is."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusChip(
                label = when {
                    active -> "Active"
                    desiredEnabled -> "Repair grants"
                    else -> "Off"
                },
            )
        }
    }
}

@Composable
private fun PermissionRow(
    model: PermissionRowModel,
    onGrant: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                androidx.compose.material3.Icon(
                    imageVector = model.icon,
                    contentDescription = model.title,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(20.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = model.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = model.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusChip(label = if (model.granted) "Granted" else "Required")
            if (!model.granted) {
                Button(onClick = onGrant) {
                    Text("Grant")
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

private data class PermissionRowModel(
    val target: KioskGrantTarget,
    val icon: ImageVector,
    val title: String,
    val description: String,
    val granted: Boolean,
)

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
