@file:OptIn(
    androidx.compose.animation.ExperimentalSharedTransitionApi::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package io.gelio.app.media.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Preview
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.gelio.app.core.theme.LocalNavAnimatedVisibilityScope
import io.gelio.app.core.theme.LocalSharedTransitionScope
import io.gelio.app.core.performance.LocalDevicePerformanceProfile
import io.gelio.app.core.ui.OptimizedAsyncImage
import io.gelio.app.core.ui.ShowcaseBackground
import io.gelio.app.core.ui.ViewerTopBar
import io.gelio.app.core.util.isRemoteUrl
import io.gelio.app.data.model.Brochure
import io.gelio.app.media.webview.WebViewerScreen
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun BrochureViewerScreen(
    brochure: Brochure?,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onClose: () -> Unit,
) {
    val pdfUri = brochure?.pdfUri.orEmpty()
    if (brochure == null) {
        ShowcaseBackground {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Brochure not found.",
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        }
        return
    }

    if (isRemoteUrl(pdfUri)) {
        WebViewerScreen(
            title = brochure.title,
            subtitle = "PDF Viewer",
            url = "https://drive.google.com/viewerng/viewer?embedded=true&url=${Uri.encode(pdfUri)}",
            onBack = onBack,
            onHome = onHome,
            onClose = onClose,
        )
        return
    }

    LocalPdfViewerScreen(
        brochure = brochure,
        onBack = onBack,
        onHome = onHome,
        onClose = onClose,
    )
}

@Composable
private fun LocalPdfViewerScreen(
    brochure: Brochure,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var documentState by remember(brochure.pdfUri) { mutableStateOf<PdfDocumentState?>(null) }
    var documentError by remember(brochure.pdfUri) { mutableStateOf<String?>(null) }
    
    val pagerState = rememberPagerState(pageCount = { documentState?.pageCount ?: 0 })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(brochure.pdfUri) {
        documentState = null
        documentError = null
        val openedDocument = withContext(Dispatchers.IO) {
            runCatching {
                openPdfDocument(context = context, value = brochure.pdfUri)
            }
        }
        openedDocument
            .onSuccess { document ->
                documentState = document
                if (document == null) {
                    documentError = "Unable to open this PDF from app storage."
                }
            }
            .onFailure { error ->
                documentError = error.message ?: "Unable to open this PDF from app storage."
            }
    }

    DisposableEffect(documentState) {
        val activeDocument = documentState
        onDispose {
            activeDocument?.close()
        }
    }

    // Bitmap rendering is now handled internally by the PdfSinglePage component

    ShowcaseBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            ViewerTopBar(
                title = brochure.title,
                subtitle = when {
                    documentState != null -> "Page ${pagerState.currentPage + 1} of ${documentState?.pageCount}"
                    documentError != null -> "PDF unavailable"
                    else -> "Opening PDF"
                },
                onBack = onBack,
                onHome = onHome,
                onClose = onClose,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrochureSidePanel(brochure = brochure)

                ElevatedCard(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    PdfPagerStage(
                        brochure = brochure,
                        documentState = documentState,
                        documentError = documentError,
                        pagerState = pagerState,
                    )
                }
            }

            documentState?.let { state ->
                PdfPageControls(
                    pageIndex = pagerState.currentPage,
                    pageCount = state.pageCount,
                    onPrevious = { 
                        coroutineScope.launch { 
                            pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0)) 
                        } 
                    },
                    onNext = { 
                        coroutineScope.launch { 
                            pagerState.animateScrollToPage((pagerState.currentPage + 1).coerceAtMost(state.pageCount - 1)) 
                        } 
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }

            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

@Composable
private fun BrochureSidePanel(
    brochure: Brochure,
) {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalNavAnimatedVisibilityScope.current
    val sharedBoundsSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Rect>()

    Surface(
        modifier = Modifier.widthIn(max = 280.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .let { baseModifier ->
                        if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                            with(sharedTransitionScope) {
                                baseModifier.sharedElement(
                                    sharedContentState = rememberSharedContentState(key = "brochure-cover-${brochure.id}"),
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    boundsTransform = { _, _ -> sharedBoundsSpec },
                                )
                            }
                        } else {
                            baseModifier
                        }
                    },
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                if (brochure.coverThumbnailUri.isNotBlank()) {
                    OptimizedAsyncImage(
                        model = brochure.coverThumbnailUri,
                        contentDescription = brochure.title,
                        maxWidth = 420.dp,
                        maxHeight = 620.dp,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.extraLarge),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Preview, contentDescription = null)
                    }
                }
            }
            Text(
                text = brochure.title,
                modifier = Modifier.let { baseModifier ->
                    if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                        with(sharedTransitionScope) {
                            baseModifier.sharedBounds(
                                sharedContentState = rememberSharedContentState(key = "brochure-title-${brochure.id}"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                enter = fadeIn(),
                                exit = fadeOut(),
                                boundsTransform = { _, _ -> sharedBoundsSpec },
                                resizeMode = androidx.compose.animation.SharedTransitionScope.ResizeMode.scaleToBounds(),
                            )
                        }
                    } else {
                        baseModifier
                    }
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "View-only brochure",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PdfPagerStage(
    brochure: Brochure,
    documentState: PdfDocumentState?,
    documentError: String?,
    pagerState: androidx.compose.foundation.pager.PagerState,
) {
    if (documentError != null) {
        Box(modifier = Modifier.fillMaxSize().padding(22.dp), contentAlignment = Alignment.Center) {
            Text(
                text = documentError,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    if (documentState == null) {
        Box(modifier = Modifier.fillMaxSize().padding(22.dp), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ContainedLoadingIndicator()
                Text(
                    text = "Opening PDF...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        return
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        pageSpacing = 16.dp,
    ) { page ->
        PdfSinglePage(
            brochure = brochure,
            documentState = documentState,
            pageIndex = page,
        )
    }
}

@Composable
private fun PdfSinglePage(
    brochure: Brochure,
    documentState: PdfDocumentState,
    pageIndex: Int,
) {
    val performanceProfile = LocalDevicePerformanceProfile.current
    val bitmapResult by produceState<Result<Bitmap>?>(
        initialValue = null,
        documentState,
        pageIndex,
        performanceProfile.pdfRenderMaxDimension
    ) {
        value = null
        value = withContext(Dispatchers.IO) {
            runCatching {
                documentState.render(
                    pageIndex = pageIndex,
                    maxRenderDimension = performanceProfile.pdfRenderMaxDimension,
                )
            }
        }
    }
    
    val bitmap = bitmapResult?.getOrNull()
    val renderError = bitmapResult?.exceptionOrNull()?.message

    DisposableEffect(bitmap) {
        onDispose {
            bitmap?.recycle()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(22.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            renderError != null -> {
                Text(
                    text = "Unable to render this page. $renderError",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            bitmap == null -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    ContainedLoadingIndicator()
                    Text(
                        text = "Rendering page...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = brochure.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }
}

@Composable
private fun PdfPageControls(
    pageIndex: Int,
    pageCount: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalIconButton(
                onClick = onPrevious,
                enabled = pageIndex > 0,
                shapes = IconButtonDefaults.shapes(),
            ) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous page")
            }
            Text(
                text = "Page ${pageIndex + 1} / $pageCount",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            FilledTonalIconButton(
                onClick = onNext,
                enabled = pageIndex < pageCount - 1,
                shapes = IconButtonDefaults.shapes(),
            ) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next page")
            }
        }
    }
}

private class PdfDocumentState(
    private val renderer: PdfRenderer,
    private val descriptor: ParcelFileDescriptor,
) {
    val pageCount: Int = renderer.pageCount
    private val renderLock = Any()
    private var closed = false

    fun render(
        pageIndex: Int,
        maxRenderDimension: Int,
    ): Bitmap {
        return synchronized(renderLock) {
            check(!closed) { "PDF document was closed." }
            check(pageCount > 0) { "PDF has no pages." }

            val safeIndex = pageIndex.coerceIn(0, pageCount - 1)
            val page = renderer.openPage(safeIndex)
            try {
                val scale = minOf(
                    maxRenderDimension.toFloat() / page.width.toFloat(),
                    maxRenderDimension.toFloat() / page.height.toFloat(),
                    2f,
                ).coerceAtLeast(0.1f)
                val bitmap = createBitmap(
                    (page.width * scale).roundToInt().coerceAtLeast(1),
                    (page.height * scale).roundToInt().coerceAtLeast(1),
                    Bitmap.Config.ARGB_8888,
                )
                bitmap.eraseColor(AndroidColor.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            } finally {
                page.close()
            }
        }
    }

    fun close() {
        synchronized(renderLock) {
            if (closed) return
            closed = true
            renderer.close()
            descriptor.close()
        }
    }
}

private fun openPdfDocument(
    context: Context,
    value: String,
): PdfDocumentState? {
    val descriptor = when {
        value.startsWith("content://", ignoreCase = true) ->
            context.contentResolver.openFileDescriptor(value.toUri(), "r")

        value.startsWith("file://", ignoreCase = true) -> {
            val path = value.toUri().path ?: return null
            ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY)
        }

        else -> ParcelFileDescriptor.open(File(value), ParcelFileDescriptor.MODE_READ_ONLY)
    } ?: return null

    return PdfDocumentState(
        renderer = PdfRenderer(descriptor),
        descriptor = descriptor,
    )
}
