package io.gelio.app.features.brandselect

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.gelio.app.R
import io.gelio.app.app.LocalAdaptiveProfile
import io.gelio.app.core.theme.expressivePressScale
import io.gelio.app.core.ui.ShowcaseHomeButton

@Composable
fun BrandSelectionScreen(
    onDesignClick: () -> Unit,
    onTourismClick: () -> Unit,
    onHomeClick: () -> Unit,
) {
    val adaptive = LocalAdaptiveProfile.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        ShowcaseHomeButton(
            onClick = onHomeClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = adaptive.homeButtonInsetTop, start = adaptive.homeButtonInsetStart),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(adaptive.gutter * 3f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandLogoButton(
                drawableRes = R.drawable.gelio_logo,
                contentDescription = "Gelio",
                onClick = onDesignClick,
                logoSize = adaptive.brandLogoSize,
            )
            BrandLogoButton(
                drawableRes = R.drawable.gelio_logo,
                contentDescription = "Gelio",
                onClick = onTourismClick,
                logoSize = adaptive.brandLogoSize,
            )
        }
    }
}

@Composable
private fun BrandLogoButton(
    drawableRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    logoSize: androidx.compose.ui.unit.Dp,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Image(
        painter = painterResource(drawableRes),
        contentDescription = contentDescription,
        modifier = Modifier
            .size(logoSize)
            .expressivePressScale(interactionSource, pressedScale = 0.98f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    )
}
