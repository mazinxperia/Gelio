package io.gelio.app.core.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> clientStageFastSpatialSpec(): FiniteAnimationSpec<T> =
    MaterialTheme.motionScheme.fastSpatialSpec()

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> clientStageDefaultSpatialSpec(): FiniteAnimationSpec<T> =
    MaterialTheme.motionScheme.defaultSpatialSpec()

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> clientStageSlowSpatialSpec(): FiniteAnimationSpec<T> =
    MaterialTheme.motionScheme.slowSpatialSpec()

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> clientStageFastEffectsSpec(): FiniteAnimationSpec<T> =
    MaterialTheme.motionScheme.fastEffectsSpec()

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> clientStageDefaultEffectsSpec(): FiniteAnimationSpec<T> =
    MaterialTheme.motionScheme.defaultEffectsSpec()

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> clientStageSlowEffectsSpec(): FiniteAnimationSpec<T> =
    MaterialTheme.motionScheme.slowEffectsSpec()

