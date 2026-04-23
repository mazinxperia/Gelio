package io.gelio.app.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.gelio.app.app.LocalAdaptiveProfile
import io.gelio.app.app.LocalLayoutTokens
import io.gelio.app.app.ShowcaseAspectBucket

data class AdaptivePanePolicy(
    val primaryWeight: Float,
    val secondaryWeight: Float,
    val tertiaryWeight: Float,
)

@Composable
fun ShowcasePageFrame(
    modifier: Modifier = Modifier,
    maxWidth: Dp = LocalLayoutTokens.current.pageMaxWidth,
    contentPadding: PaddingValues? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = LocalLayoutTokens.current
    val adaptive = LocalAdaptiveProfile.current
    val resolvedPadding = contentPadding ?: PaddingValues(
        horizontal = adaptive.contentPaddingHorizontal,
        vertical = adaptive.contentPaddingVertical,
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(resolvedPadding),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .fillMaxWidth()
                .widthIn(max = maxWidth),
            content = content,
        )
    }
}

@Composable
fun ShowcaseCenteredContent(
    modifier: Modifier = Modifier,
    maxWidth: Dp = LocalLayoutTokens.current.contentMaxWidth,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = maxWidth),
            content = content,
        )
    }
}

@Composable
fun rememberAdaptivePanePolicy(
    paneCount: Int,
): AdaptivePanePolicy {
    val adaptive = LocalAdaptiveProfile.current
    return remember(adaptive, paneCount) {
        when (paneCount) {
            2 -> when {
                adaptive.aspectBucket == ShowcaseAspectBucket.CLASSIC_FOUR_THREE -> AdaptivePanePolicy(0.98f, 1.02f, 0f)
                adaptive.compactLandscape -> AdaptivePanePolicy(1f, 1f, 0f)
                else -> AdaptivePanePolicy(0.96f, 1.04f, 0f)
            }

            3 -> when {
                adaptive.compactLandscape -> AdaptivePanePolicy(1f, 1.05f, 1f)
                adaptive.aspectBucket == ShowcaseAspectBucket.CLASSIC_FOUR_THREE -> AdaptivePanePolicy(0.94f, 1.08f, 0.94f)
                else -> AdaptivePanePolicy(0.95f, 1.1f, 0.95f)
            }

            else -> AdaptivePanePolicy(1f, 1f, 1f)
        }
    }
}

@Composable
fun ShowcaseAdaptiveRow(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    val tokens = LocalLayoutTokens.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(tokens.panelGap),
        content = content,
    )
}
