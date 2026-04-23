@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.gelio.app.core.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset

@Composable
fun Modifier.expressivePressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.94f,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "expressivePressScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

data class ShowcaseNavTransitions(
    val forwardEnter: (String?, String?) -> EnterTransition,
    val forwardExit: (String?, String?) -> ExitTransition,
    val backEnter: (String?, String?) -> EnterTransition,
    val backExit: (String?, String?) -> ExitTransition,
)

@Composable
fun rememberShowcaseNavTransitions(): ShowcaseNavTransitions {
    val defaultSpatial = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    val slowSpatial = MaterialTheme.motionScheme.slowSpatialSpec<IntOffset>()
    val defaultEffects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val fastEffects = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    val slowEffects = MaterialTheme.motionScheme.slowEffectsSpec<Float>()

    return remember(defaultSpatial, slowSpatial, defaultEffects, fastEffects, slowEffects) {
        ShowcaseNavTransitions(
            forwardEnter = { initialRoute, targetRoute ->
                if (isViewerTransition(initialRoute, targetRoute)) {
                    fadeIn(animationSpec = defaultEffects) + scaleIn(initialScale = 0.92f, animationSpec = defaultEffects)
                } else {
                    val spatial = if (isShowroomEntryTransition(initialRoute, targetRoute)) slowSpatial else defaultSpatial
                    val effects = if (isShowroomEntryTransition(initialRoute, targetRoute)) slowEffects else defaultEffects
                    slideInHorizontally(
                        animationSpec = spatial,
                        initialOffsetX = { fullWidth -> fullWidth / 4 },
                    ) + fadeIn(animationSpec = effects) + scaleIn(initialScale = 0.92f, animationSpec = effects)
                }
            },
            forwardExit = { initialRoute, targetRoute ->
                if (isViewerTransition(initialRoute, targetRoute)) {
                    fadeOut(animationSpec = fastEffects) + scaleOut(targetScale = 0.92f, animationSpec = fastEffects)
                } else {
                    val spatial = if (isShowroomEntryTransition(initialRoute, targetRoute)) slowSpatial else defaultSpatial
                    val effects = if (isShowroomEntryTransition(initialRoute, targetRoute)) slowEffects else fastEffects
                    slideOutHorizontally(
                        animationSpec = spatial,
                        targetOffsetX = { fullWidth -> -fullWidth / 4 },
                    ) + fadeOut(animationSpec = effects) + scaleOut(targetScale = 0.92f, animationSpec = effects)
                }
            },
            backEnter = { initialRoute, targetRoute ->
                if (isViewerTransition(initialRoute, targetRoute)) {
                    fadeIn(animationSpec = defaultEffects) + scaleIn(initialScale = 0.92f, animationSpec = defaultEffects)
                } else {
                    val spatial = if (isShowroomEntryTransition(targetRoute, initialRoute)) slowSpatial else defaultSpatial
                    val effects = if (isShowroomEntryTransition(targetRoute, initialRoute)) slowEffects else defaultEffects
                    slideInHorizontally(
                        animationSpec = spatial,
                        initialOffsetX = { fullWidth -> -fullWidth / 4 },
                    ) + fadeIn(animationSpec = effects) + scaleIn(initialScale = 0.92f, animationSpec = effects)
                }
            },
            backExit = { initialRoute, targetRoute ->
                if (isViewerTransition(initialRoute, targetRoute)) {
                    fadeOut(animationSpec = fastEffects) + scaleOut(targetScale = 0.92f, animationSpec = fastEffects)
                } else {
                    val spatial = if (isShowroomEntryTransition(targetRoute, initialRoute)) slowSpatial else defaultSpatial
                    val effects = if (isShowroomEntryTransition(targetRoute, initialRoute)) slowEffects else fastEffects
                    slideOutHorizontally(
                        animationSpec = spatial,
                        targetOffsetX = { fullWidth -> fullWidth / 4 },
                    ) + fadeOut(animationSpec = effects) + scaleOut(targetScale = 0.92f, animationSpec = effects)
                }
            },
        )
    }
}

fun <T> directionalTabTransitionSpec(
    spatial: FiniteAnimationSpec<IntOffset>,
    effects: FiniteAnimationSpec<Float>,
    indexOf: (T) -> Int,
): AnimatedContentTransitionScope<T>.() -> ContentTransform =
    {
        val forward = indexOf(targetState) > indexOf(initialState)
        val direction = if (forward) 1 else -1
        (
            slideInHorizontally(
                animationSpec = spatial,
                initialOffsetX = { fullWidth -> fullWidth * direction },
            ) + fadeIn(animationSpec = effects)
            ) togetherWith (
            slideOutHorizontally(
                animationSpec = spatial,
                targetOffsetX = { fullWidth -> -fullWidth * direction },
            ) + fadeOut(animationSpec = effects)
            ) using SizeTransform(clip = false) { _, _ -> snap() }
}

private fun isViewerTransition(initialRoute: String?, targetRoute: String?): Boolean =
    isViewerRoute(initialRoute) || isViewerRoute(targetRoute)

private fun isViewerRoute(route: String?): Boolean =
    route?.startsWith("viewer/") == true || route?.startsWith("gallery/") == true

private fun isShowroomEntryTransition(initialRoute: String?, targetRoute: String?): Boolean {
    return when {
        initialRoute == "welcome" && targetRoute == "brand_selection" -> true
        initialRoute == "brand_selection" && targetRoute?.startsWith("design/") == true -> true
        initialRoute == "brand_selection" && targetRoute?.startsWith("tourism/") == true -> true
        else -> false
    }
}
