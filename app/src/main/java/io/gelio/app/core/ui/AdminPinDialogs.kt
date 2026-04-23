@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.gelio.app.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LockReset
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.gelio.app.core.theme.expressivePressScale
import kotlinx.coroutines.delay

@Composable
fun AdminPinGateDialog(
    expectedPin: String,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    onResetApp: () -> Unit,
) {
    var digits by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showReset by remember { mutableStateOf(false) }
    var verifying by remember { mutableStateOf(false) }

    LaunchedEffect(digits, expectedPin) {
        if (digits.length != 4 || verifying) return@LaunchedEffect
        verifying = true
        delay(110)
        if (digits == expectedPin) {
            onSuccess()
        } else {
            errorMessage = "Wrong PIN. Use reset only if you want to wipe the app."
            showReset = true
            digits = ""
        }
        verifying = false
    }

    AdminPinDialogScaffold(
        title = "Admin PIN",
        subtitle = "Enter the 4-digit admin PIN. It auto-submits on the fourth digit.",
        digits = digits,
        supportingText = errorMessage ?: "Default PIN is 0000 until you change it in app settings.",
        tone = if (errorMessage == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
        onDismiss = onDismiss,
        onDigit = { if (!verifying && digits.length < 4) digits += it },
        onBackspace = {
            if (!verifying && digits.isNotEmpty()) {
                digits = digits.dropLast(1)
                errorMessage = null
            }
        },
        footer = {
            AnimatedVisibility(
                visible = showReset,
                enter = fadeIn(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()),
                exit = fadeOut(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "Reset Password deletes all app data and restores the PIN to 0000.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                    OutlinedButton(
                        onClick = onResetApp,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.LockReset, contentDescription = null)
                        Text(
                            text = "Reset Password",
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        },
    )
}

@Composable
fun AdminPinChangeDialog(
    onDismiss: () -> Unit,
    onSavePin: (String) -> Unit,
) {
    var digits by remember { mutableStateOf("") }

    LaunchedEffect(digits) {
        if (digits.length == 4) {
            delay(90)
            onSavePin(digits)
        }
    }

    AdminPinDialogScaffold(
        title = "Change Admin PIN",
        subtitle = "Enter the new 4-digit PIN. It saves immediately on the fourth digit.",
        digits = digits,
        supportingText = "Numeric PIN only.",
        tone = MaterialTheme.colorScheme.onSurfaceVariant,
        onDismiss = onDismiss,
        onDigit = { if (digits.length < 4) digits += it },
        onBackspace = { if (digits.isNotEmpty()) digits = digits.dropLast(1) },
        footer = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Close")
            }
        },
    )
}

@Composable
private fun AdminPinDialogScaffold(
    title: String,
    subtitle: String,
    digits: String,
    supportingText: String,
    tone: Color,
    onDismiss: () -> Unit,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    footer: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x66000000))
                .padding(28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.widthIn(max = 520.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 12.dp,
                shadowElevation = 22.dp,
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    PinSlots(digits)
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = tone,
                        textAlign = TextAlign.Center,
                    )
                    PinPad(
                        onDigit = onDigit,
                        onBackspace = onBackspace,
                    )
                    footer()
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close admin panel",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
}

@Composable
private fun PinSlots(
    digits: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(4) { index ->
            val filled = index < digits.length
            Surface(
                modifier = Modifier.size(width = 58.dp, height = 70.dp),
                shape = MaterialTheme.shapes.large,
                color = if (filled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (filled) "•" else "",
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (filled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PinPad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
        ).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                row.forEach { digit ->
                    PinPadButton(label = digit, onClick = { onDigit(digit) })
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PinPadSpacer()
            PinPadButton(label = "0", onClick = { onDigit("0") })
            PinPadIconButton(
                onClick = onBackspace,
                icon = Icons.AutoMirrored.Rounded.Backspace,
                contentDescription = "Backspace",
            )
        }
    }
}

@Composable
private fun PinPadButton(
    label: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier
            .size(92.dp)
            .expressivePressScale(interactionSource, pressedScale = 0.965f),
        shape = CircleShape,
        interactionSource = interactionSource,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun PinPadIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = Modifier
            .size(92.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .expressivePressScale(interactionSource, pressedScale = 0.965f),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun PinPadSpacer() {
    Box(modifier = Modifier.size(92.dp))
}
