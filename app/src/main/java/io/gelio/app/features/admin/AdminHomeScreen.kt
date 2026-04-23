package io.gelio.app.features.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DynamicFeed
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.interaction.MutableInteractionSource
import io.gelio.app.core.theme.expressivePressScale
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import io.gelio.app.features.admin.AdminStaggeredEntrance
import io.gelio.app.core.ui.ShowcaseBackground
import io.gelio.app.core.ui.ShowcaseCenteredContent
import io.gelio.app.core.ui.ShowcaseHero
import io.gelio.app.core.ui.ShowcaseHomeButton
import io.gelio.app.core.ui.ShowcasePageFrame
import io.gelio.app.app.LocalAdaptiveProfile
import io.gelio.app.app.LocalLayoutTokens

import io.gelio.app.app.LocalScreenConfig
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass

@Composable
fun AdminHomeScreen(
    onSettingsClick: () -> Unit,
    onBackupImportClick: () -> Unit,
    onClearDataClick: () -> Unit,
    onSectionsClick: () -> Unit,
    onHomeClick: () -> Unit,
) {
    val config = LocalScreenConfig.current
    val adaptive = LocalAdaptiveProfile.current
    val tokens = LocalLayoutTokens.current
    val isExpanded = config.widthClass == WindowWidthSizeClass.Expanded
    
    val cardHeight = tokens.cardHeight
    val gridSpacing = tokens.gridSpacing

    ShowcaseBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            ShowcaseHomeButton(
                onClick = onHomeClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = adaptive.homeButtonInsetTop, start = adaptive.homeButtonInsetStart),
            )

            ShowcasePageFrame(
                maxWidth = tokens.pageMaxWidth,
                contentPadding = PaddingValues(
                    start = tokens.margin,
                    top = adaptive.topChromeReserve,
                    end = tokens.margin,
                    bottom = tokens.margin,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(tokens.gridSpacing),
                ) {
                    ShowcaseHero(
                        eyebrow = "Admin Panel",
                        title = "Hidden controls for the tablet showcase.",
                        subtitle = "Theme settings, local content management, visibility control, and future-ready structure all stay grouped here without polluting the client-facing experience.",
                        maxWidth = tokens.contentMaxWidth * 0.72f,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                    ShowcaseCenteredContent(
                        modifier = Modifier.fillMaxWidth(),
                        maxWidth = if (isExpanded) 1400.dp else 1200.dp,
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(gridSpacing),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                            ) {
                                AdminStaggeredEntrance(index = 0, modifier = Modifier.weight(1f)) {
                                    AdminActionCard(
                                        title = "App Settings",
                                        subtitle = "Theme mode, accent tone, and idle timeout structure.",
                                        icon = Icons.Rounded.Settings,
                                        modifier = Modifier.height(cardHeight),
                                        onClick = onSettingsClick,
                                    )
                                }
                                AdminStaggeredEntrance(index = 1, modifier = Modifier.weight(1f)) {
                                    AdminActionCard(
                                        title = "Sections",
                                        subtitle = "Dynamic company tabs, media modules, maps, and ordering.",
                                        icon = Icons.Rounded.DynamicFeed,
                                        modifier = Modifier.height(cardHeight),
                                        onClick = onSectionsClick,
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                            ) {
                                AdminStaggeredEntrance(index = 2, modifier = Modifier.weight(1f)) {
                                    AdminActionCard(
                                        title = "Backup & Import",
                                        subtitle = "Create or restore a complete .kskm mirror of app content.",
                                        icon = Icons.Rounded.Backup,
                                        modifier = Modifier.height(cardHeight),
                                        onClick = onBackupImportClick,
                                    )
                                }
                                AdminStaggeredEntrance(index = 3, modifier = Modifier.weight(1f)) {
                                    AdminActionCard(
                                        title = "Clear Data",
                                        subtitle = "Factory reset local app data, media, settings, and cache.",
                                        icon = Icons.Rounded.DeleteForever,
                                        modifier = Modifier.height(cardHeight),
                                        onClick = onClearDataClick,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun AdminActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    ElevatedCard(
        modifier = modifier.expressivePressScale(interactionSource, pressedScale = 0.95f),
        onClick = onClick,
        interactionSource = interactionSource,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp, pressedElevation = 6.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ElevatedCard(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(14.dp)
                        .size(28.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
