package io.gelio.app.data.backup

import android.content.Context
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.OpenableColumns
import android.provider.MediaStore
import androidx.core.net.toUri
import io.gelio.app.core.util.generateImageThumbnailIfSupported
import io.gelio.app.core.util.mapPreviewFile
import io.gelio.app.data.local.dao.ShowcaseDao
import io.gelio.app.data.local.entity.ArtGalleryCardEntity
import io.gelio.app.data.local.entity.ArtGalleryHeroEntity
import io.gelio.app.data.local.entity.BrochureEntity
import io.gelio.app.data.local.entity.CompanyEntity
import io.gelio.app.data.local.entity.ContentPageCardEntity
import io.gelio.app.data.local.entity.DestinationEntity
import io.gelio.app.data.local.entity.FeaturedProjectEntity
import io.gelio.app.data.local.entity.GlobalLinkEntity
import io.gelio.app.data.local.entity.RemoteImageAssetEntity
import io.gelio.app.data.local.entity.ReviewCardEntity
import io.gelio.app.data.local.entity.SectionEntity
import io.gelio.app.data.local.entity.ServiceEntity
import io.gelio.app.data.local.entity.ShowcaseVideoEntity
import io.gelio.app.data.local.entity.VirtualTourEntity
import io.gelio.app.data.local.entity.WorldMapPinEntity
import io.gelio.app.data.local.entity.WorldMapSectionEntity
import io.gelio.app.data.model.AppColorMode
import io.gelio.app.data.model.AppSettings
import io.gelio.app.data.model.Brand
import io.gelio.app.data.model.CuratedPalette
import io.gelio.app.data.model.ThemeMode
import io.gelio.app.data.model.SectionType
import io.gelio.app.data.repository.SettingsRepository
import io.gelio.app.features.map.MapAssetPaths
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class BackupRepository(
    private val context: Context,
    private val dao: ShowcaseDao,
    private val settingsRepository: SettingsRepository,
) {
    suspend fun inspectBackup(
        inputUri: Uri,
        onProgress: (BackupProgress) -> Unit,
    ): BackupInspection = withContext(Dispatchers.IO) {
        val (displayName, sizeBytes) = readDocumentMeta(inputUri)
        onProgress(
            BackupProgress(
                phase = BackupPhase.Inspect,
                message = "Preparing inspection for $displayName.",
                totalBytes = sizeBytes,
                progressFraction = 0.06f,
            ),
        )
        val input = requireNotNull(context.contentResolver.openInputStream(inputUri)) {
            "Unable to open selected backup file."
        }
        BufferedInputStream(input).use { buffered ->
            onProgress(
                BackupProgress(
                    phase = BackupPhase.Inspect,
                    message = "Reading backup header.",
                    totalBytes = sizeBytes,
                    progressFraction = 0.18f,
                ),
            )
            buffered.mark(ENCRYPTED_MAGIC.size + 8)
            val header = ByteArray(ENCRYPTED_MAGIC.size)
            val read = buffered.read(header)
            buffered.reset()
            if (read == ENCRYPTED_MAGIC.size && header.contentEquals(ENCRYPTED_MAGIC)) {
                onProgress(
                    BackupProgress(
                        phase = BackupPhase.Inspect,
                        message = "Password detected. Enter password to continue.",
                        totalBytes = sizeBytes,
                        progressFraction = 1f,
                    ),
                )
                return@withContext BackupInspection(
                    displayName = displayName,
                    sizeBytes = sizeBytes,
                    passwordProtected = true,
                )
            }
            onProgress(
                BackupProgress(
                    phase = BackupPhase.Inspect,
                    message = "No password detected. Reading backup manifest.",
                    totalBytes = sizeBytes,
                    progressFraction = 0.32f,
                ),
            )
            return@withContext inspectZipPayload(
                input = buffered,
                displayName = displayName,
                sizeBytes = sizeBytes,
                onProgress = onProgress,
            )
        }
    }

    suspend fun exportBackup(
        request: BackupExportRequest,
        onProgress: (BackupProgress) -> Unit,
    ): BackupSummary = withContext(Dispatchers.IO) {
        val workDir = File(context.cacheDir, "kskm_export").apply {
            deleteRecursively()
            mkdirs()
        }
        val zipFile = File(workDir, "payload.zip")

        onProgress(BackupProgress(BackupPhase.Scan, "Scanning app settings and content."))
        repairLegacyMediaPaths(onProgress)
        val snapshot = sanitizeSnapshotForExport(readSnapshot(), onProgress)
        val mediaEntries = buildMediaEntries(snapshot)
        val totalBytes = mediaEntries.sumOf { it.sourceFile.length() }
        logMediaScanSummary(snapshot, totalBytes, mediaEntries.size, onProgress)
        val encrypted = !request.password.isNullOrBlank()
        if (encrypted) {
            requireUsableSpace(
                root = workDir,
                requiredBytes = totalBytes + backupSpacePadding(totalBytes),
                label = "export staging cache",
            )
        }
        requireUsableSpace(
            root = downloadsDirectory(),
            requiredBytes = totalBytes + backupSpacePadding(totalBytes),
            label = "Downloads backup output",
        )
        onProgress(
            BackupProgress(
                phase = BackupPhase.Package,
                message = "Packaging ${mediaEntries.size} media files.",
                totalFiles = mediaEntries.size,
                totalBytes = totalBytes,
            ),
        )

        val output = requireNotNull(context.contentResolver.openOutputStream(request.outputUri)) {
            "Unable to open export destination."
        }
        val verification = if (encrypted) {
            writeZipPayload(
                zipFile = zipFile,
                snapshot = snapshot,
                mediaEntries = mediaEntries,
                compressionLevel = request.compressionLevel.coerceIn(0, 9),
                encrypted = true,
                onProgress = onProgress,
            )

            onProgress(BackupProgress(BackupPhase.Verify, "Verifying .kskm archive payload.", totalFiles = mediaEntries.size, totalBytes = totalBytes))
            val verifiedPayload = verifyZipPayload(zipFile) { verifiedFiles, totalFiles, verifiedBytes, totalBytes ->
                onProgress(
                    BackupProgress(
                        phase = BackupPhase.Verify,
                        message = "Verified $verifiedFiles of $totalFiles archive files.",
                        completedFiles = verifiedFiles,
                        totalFiles = totalFiles,
                        completedBytes = verifiedBytes,
                        totalBytes = totalBytes,
                    ),
                )
            }
            output.use { destination ->
                encryptZipPayload(zipFile, destination, request.password)
            }
            verifiedPayload
        } else {
            output.use { destination ->
                writeZipPayload(
                    output = destination,
                    snapshot = snapshot,
                    mediaEntries = mediaEntries,
                    compressionLevel = request.compressionLevel.coerceIn(0, 9),
                    encrypted = false,
                    onProgress = onProgress,
                )
            }
            onProgress(BackupProgress(BackupPhase.Verify, "Verifying written .kskm archive.", totalFiles = mediaEntries.size, totalBytes = totalBytes))
            requireNotNull(context.contentResolver.openInputStream(request.outputUri)) {
                "Unable to verify exported backup file."
            }.use { input ->
                verifyZipPayload(input) { verifiedFiles, totalFiles, verifiedBytes, totalBytes ->
                    onProgress(
                        BackupProgress(
                            phase = BackupPhase.Verify,
                            message = "Verified $verifiedFiles of $totalFiles archive files.",
                            completedFiles = verifiedFiles,
                            totalFiles = totalFiles,
                            completedBytes = verifiedBytes,
                            totalBytes = totalBytes,
                        ),
                    )
                }
            }
        }

        workDir.deleteRecursively()
        onProgress(BackupProgress(BackupPhase.Complete, "Backup exported and verified.", completedFiles = verification.fileCount, totalFiles = verification.fileCount, completedBytes = verification.totalBytes, totalBytes = verification.totalBytes))
        BackupSummary(verification.fileCount, verification.totalBytes, snapshot.contentCounts())
    }

    suspend fun exportBackupToDownloads(
        password: String?,
        compressionLevel: Int,
        onProgress: (BackupProgress) -> Unit,
    ): BackupSummary = withContext(Dispatchers.IO) {
        val fileName = nextDownloadsBackupName()
        val uri = createDownloadsBackupUri(fileName)
        runCatching {
            exportBackup(
                request = BackupExportRequest(
                    outputUri = uri,
                    password = password,
                    compressionLevel = compressionLevel,
                ),
                onProgress = onProgress,
            )
        }.fold(
            onSuccess = { summary ->
                markDownloadsBackupComplete(uri)
                summary.copy(outputLabel = "Downloads/$fileName")
            },
            onFailure = { error ->
                context.contentResolver.delete(uri, null, null)
                throw error
            },
        )
    }

    suspend fun importBackup(
        request: BackupImportRequest,
        onProgress: (BackupProgress) -> Unit,
    ): BackupSummary = withContext(Dispatchers.IO) {
        val workDir = File(context.cacheDir, "kskm_import").apply {
            deleteRecursively()
            mkdirs()
        }
        val zipFile = File(workDir, "payload.zip")

        onProgress(BackupProgress(BackupPhase.Verify, "Reading .kskm backup file."))
        val selectedBackupSize = request.inputUri.contentLength()
        if (selectedBackupSize > 0L) {
            requireUsableSpace(
                root = workDir,
                requiredBytes = selectedBackupSize + backupSpacePadding(selectedBackupSize),
                label = "import staging cache",
            )
        }
        copyOrDecryptBackupToZip(
            inputUri = request.inputUri,
            zipFile = zipFile,
            password = request.password,
        )

        onProgress(BackupProgress(BackupPhase.Verify, "Verifying backup before import."))
        val verification = verifyZipPayload(zipFile) { verifiedFiles, totalFiles, verifiedBytes, totalBytes ->
            onProgress(
                BackupProgress(
                    phase = BackupPhase.Verify,
                    message = "Verified $verifiedFiles of $totalFiles backup files.",
                    completedFiles = verifiedFiles,
                    totalFiles = totalFiles,
                    completedBytes = verifiedBytes,
                    totalBytes = totalBytes,
                ),
            )
        }
        val importRoot = File(context.filesDir, "showcase_media/imported/${UUID.randomUUID()}").apply { mkdirs() }
        requireUsableSpace(
            root = importRoot,
            requiredBytes = verification.totalBytes + backupSpacePadding(verification.totalBytes),
            label = "app-private imported media",
        )
        val mediaPathMap = copyVerifiedMediaFromZip(zipFile, verification, importRoot, onProgress)

        onProgress(
            BackupProgress(
                BackupPhase.Import,
                "Replacing app content after verification.",
                completedFiles = verification.fileCount,
                totalFiles = verification.fileCount,
                completedBytes = verification.totalBytes,
                totalBytes = verification.totalBytes,
            ),
        )
        val deviceLocalSettings = settingsRepository.settings.first()
        val importedSnapshot = parseSnapshotFromZip(zipFile, mediaPathMap)
        val imported = importedSnapshot.copy(
            settings = importedSnapshot.settings.withDeviceLocalOnlySettings(deviceLocalSettings),
        )
        dao.replaceAllContent(
            companies = imported.companies,
            sections = imported.sections,
            featuredProjects = imported.featuredProjects,
            virtualTours = imported.virtualTours,
            videos = imported.videos,
            brochures = imported.brochures,
            destinations = imported.destinations,
            services = imported.services,
            globalLinks = imported.globalLinks,
            worldMapSections = imported.worldMapSections,
            worldMapPins = imported.worldMapPins,
            reviewCards = imported.reviewCards,
            contentPageCards = imported.contentPageCards,
            artGalleryHeroes = imported.artGalleryHeroes,
            artGalleryCards = imported.artGalleryCards,
            remoteImageAssets = imported.remoteImageAssets,
        )
        settingsRepository.replaceSettings(imported.settings)
        restoreMapPreviews(imported.worldMapSections, mediaPathMap)
        removeOldMediaExcept(importRoot)
        workDir.deleteRecursively()
        onProgress(BackupProgress(BackupPhase.Complete, "Import verified and applied.", completedFiles = verification.fileCount, totalFiles = verification.fileCount, completedBytes = verification.totalBytes, totalBytes = verification.totalBytes))
        BackupSummary(verification.fileCount, verification.totalBytes, imported.contentCounts())
    }

    private suspend fun repairLegacyMediaPaths(onProgress: (BackupProgress) -> Unit) {
        onProgress(BackupProgress(BackupPhase.Repair, "Repairing older external media paths if needed."))
        dao.upsertFeaturedProjects(
            dao.getAllFeaturedProjectsSnapshot().map { project ->
                project.copy(
                    galleryImages = project.galleryImages.mapIndexed { index, path ->
                        repairMediaPath(path, "design/projects/gallery/${project.safeId()}", ".jpg", "project ${project.projectName} gallery image ${index + 1}", onProgress)
                    },
                    thumbnailUri = repairMediaPath(project.thumbnailUri, "design/projects/thumbnails", ".jpg", "project ${project.projectName} thumbnail", onProgress),
                )
            },
        )
        dao.upsertVirtualTours(
            dao.getAllVirtualToursSnapshot().map { tour ->
                tour.copy(thumbnailUri = repairMediaPath(tour.thumbnailUri, "design/tours/thumbnails", ".jpg", "tour ${tour.projectName} thumbnail", onProgress))
            },
        )
        dao.upsertVideos(
            dao.getAllVideosSnapshot().map { video ->
                video.copy(thumbnailUri = video.thumbnailUri?.let { repairMediaPath(it, "design/videos/thumbnails", ".jpg", "video ${video.title} thumbnail", onProgress) })
            },
        )
        dao.upsertBrochures(
            (dao.getAllBrochuresSnapshot(Brand.DESIGN.name) + dao.getAllBrochuresSnapshot(Brand.TOURISM.name)).map { brochure ->
                brochure.copy(
                    pdfUri = repairMediaPath(brochure.pdfUri, "brochures/pdfs", ".pdf", "brochure ${brochure.title} PDF", onProgress),
                    coverThumbnailUri = repairMediaPath(brochure.coverThumbnailUri, "brochures/covers", ".jpg", "brochure ${brochure.title} cover", onProgress),
                )
            },
        )
        dao.upsertDestinations(
            dao.getAllDestinationsSnapshot().map { destination ->
                destination.copy(imageUri = repairMediaPath(destination.imageUri, "tourism/destinations", ".jpg", "destination ${destination.destinationName} image", onProgress))
            },
        )
        dao.upsertServices(
            dao.getAllServicesSnapshot().map { service ->
                service.copy(imageUri = repairMediaPath(service.imageUri, "tourism/services", ".jpg", "service ${service.serviceTitle} image", onProgress))
            },
        )
    }

    private fun repairMediaPath(
        path: String,
        folderName: String,
        fallbackExtension: String,
        label: String,
        onProgress: (BackupProgress) -> Unit,
    ): String {
        if (path.isBlank() || path.isRemoteUrl() || path.startsWith("media/")) return path
        if (path.startsWith(context.filesDir.absolutePath) && File(path).exists()) return path
        if (path.startsWith("content://")) {
            return runCatching {
                copyUriToAppStorageBlocking(path.toUri(), folderName, fallbackExtension)
            }.getOrElse { cause ->
                onProgress(BackupProgress(BackupPhase.Repair, "Warning: $label is missing; backup will use a fallback or skip it."))
                path
            }
        }
        val file = when {
            path.startsWith("file://") -> File(path.toUri().path.orEmpty())
            else -> File(path)
        }
        if (!file.exists()) {
            onProgress(BackupProgress(BackupPhase.Repair, "Warning: $label is missing; backup will use a fallback or skip it."))
            return path
        }
        return copyFileToAppStorage(file, folderName, fallbackExtension)
    }

    private fun copyUriToAppStorageBlocking(uri: Uri, folderName: String, fallbackExtension: String): String {
        val destination = createDestinationFile(folderName, uri.lastPathSegment?.substringAfterLast('.'), fallbackExtension)
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Unable to read $uri" }
            destination.outputStream().use { output -> input.copyTo(output) }
        }
        generateImageThumbnailIfSupported(context, destination)
        return destination.absolutePath
    }

    private fun copyFileToAppStorage(file: File, folderName: String, fallbackExtension: String): String {
        val destination = createDestinationFile(folderName, file.extension.takeIf { it.isNotBlank() }, fallbackExtension)
        file.inputStream().use { input -> destination.outputStream().use { output -> input.copyTo(output) } }
        generateImageThumbnailIfSupported(context, destination)
        return destination.absolutePath
    }

    private fun createDestinationFile(folderName: String, extension: String?, fallbackExtension: String): File {
        val suffix = extension?.takeIf { it.isNotBlank() && it.length <= 8 }?.let { ".$it" } ?: fallbackExtension
        val folder = File(context.filesDir, "showcase_media/$folderName").apply { mkdirs() }
        return File(folder, "${UUID.randomUUID()}$suffix")
    }

    private suspend fun readSnapshot(): BackupSnapshot {
        val snapshot = BackupSnapshot(
            settings = settingsRepository.settings.first(),
            companies = dao.getAllCompaniesSnapshot(),
            sections = dao.getAllSectionsSnapshot(),
            featuredProjects = dao.getAllFeaturedProjectsSnapshot(),
            virtualTours = dao.getAllVirtualToursSnapshot(),
            videos = dao.getAllVideosSnapshot(),
            brochures = dao.getAllBrochuresSnapshot(Brand.DESIGN.name) + dao.getAllBrochuresSnapshot(Brand.TOURISM.name),
            destinations = dao.getAllDestinationsSnapshot(),
            services = dao.getAllServicesSnapshot(),
            globalLinks = dao.getAllGlobalLinksSnapshot(),
            worldMapSections = dao.getAllWorldMapSectionsSnapshot(),
            worldMapPins = dao.getAllWorldMapPinsSnapshot(),
            reviewCards = dao.getAllReviewCardsSnapshot(),
            contentPageCards = dao.getAllContentPageCardsSnapshot(),
            artGalleryHeroes = dao.getAllArtGalleryHeroesSnapshot(),
            artGalleryCards = dao.getAllArtGalleryCardsSnapshot(),
            remoteImageAssets = dao.getAllRemoteImageAssetsSnapshot(),
        )
        return snapshot.copy(
            remoteImageAssets = snapshot.remoteImageAssets.filter { it.localPath in snapshot.referencedLocalMediaPaths() },
        )
    }

    private fun sanitizeSnapshotForExport(
        snapshot: BackupSnapshot,
        onProgress: (BackupProgress) -> Unit,
    ): BackupSnapshot {
        fun warn(message: String) {
            onProgress(BackupProgress(BackupPhase.Repair, "Warning: $message"))
        }

        val projects = snapshot.featuredProjects.mapNotNull { project ->
            val gallery = project.galleryImages.filter { path ->
                path.isUsableBackupReference().also { usable ->
                    if (!usable) warn("Removed missing gallery image from ${project.projectName}.")
                }
            }
            val thumbnail = when {
                project.thumbnailUri.isUsableBackupReference() -> project.thumbnailUri
                gallery.isNotEmpty() -> {
                    warn("Used first gallery image as fallback thumbnail for ${project.projectName}.")
                    gallery.first()
                }
                else -> ""
            }
            if (gallery.isEmpty() && thumbnail.isBlank()) {
                warn("Skipped ${project.projectName}; no readable project media remains.")
                null
            } else {
                project.copy(galleryImages = gallery, thumbnailUri = thumbnail)
            }
        }

        val tours = snapshot.virtualTours.map { tour ->
            if (tour.thumbnailUri.isUsableBackupReference()) {
                tour
            } else {
                warn("Kept ${tour.projectName} tour link but removed missing thumbnail.")
                tour.copy(thumbnailUri = "")
            }
        }

        val videos = snapshot.videos.map { video ->
            val thumbnail = video.thumbnailUri
            if (thumbnail.isNullOrBlank() || thumbnail.isUsableBackupReference()) {
                video
            } else {
                warn("Kept ${video.title} YouTube link but removed missing custom thumbnail.")
                video.copy(thumbnailUri = null)
            }
        }

        val brochures = snapshot.brochures.mapNotNull { brochure ->
            if (!brochure.pdfUri.isUsableBackupReference()) {
                warn("Skipped brochure ${brochure.title}; PDF is missing and cannot be recreated.")
                null
            } else if (!brochure.coverThumbnailUri.isUsableBackupReference()) {
                warn("Kept brochure ${brochure.title} but removed missing cover thumbnail.")
                brochure.copy(coverThumbnailUri = "")
            } else {
                brochure
            }
        }

        val destinations = snapshot.destinations.mapNotNull { destination ->
            if (destination.imageUri.isUsableBackupReference()) {
                destination
            } else {
                warn("Skipped tourism destination ${destination.destinationName}; image is missing.")
                null
            }
        }

        val services = snapshot.services.mapNotNull { service ->
            if (service.imageUri.isUsableBackupReference()) {
                service
            } else {
                warn("Skipped tourism service ${service.serviceTitle}; image is missing.")
                null
            }
        }

        val contentPageCards = snapshot.contentPageCards.mapNotNull { card ->
            if (card.imagePath.isUsableBackupReference()) {
                card
            } else {
                warn("Skipped content page card ${card.title}; image is missing.")
                null
            }
        }

        val artGalleryCards = snapshot.artGalleryCards.mapNotNull { card ->
            if (card.imagePath.isUsableBackupReference()) {
                card
            } else {
                warn("Skipped art gallery card ${card.title}; image is missing.")
                null
            }
        }
        val keptHeroIds = artGalleryCards.mapTo(mutableSetOf()) { it.heroId }
        val artGalleryHeroes = snapshot.artGalleryHeroes.filter { it.id in keptHeroIds }

        return snapshot.copy(
            featuredProjects = projects,
            virtualTours = tours,
            videos = videos,
            brochures = brochures,
            destinations = destinations,
            services = services,
            contentPageCards = contentPageCards,
            artGalleryHeroes = artGalleryHeroes,
            artGalleryCards = artGalleryCards,
        )
    }

    private fun logMediaScanSummary(
        snapshot: BackupSnapshot,
        totalBytes: Long,
        totalFiles: Int,
        onProgress: (BackupProgress) -> Unit,
    ) {
        val projectImages = snapshot.featuredProjects.sumOf { it.galleryImages.size }
        onProgress(
            BackupProgress(
                phase = BackupPhase.Scan,
                message = "Media scan: $totalFiles files, ${totalBytes.formatBytes()}, $projectImages project images.",
                totalFiles = totalFiles,
                totalBytes = totalBytes,
            ),
        )
    }

    private fun buildMediaEntries(snapshot: BackupSnapshot): List<MediaExportEntry> {
        val entries = mutableListOf<MediaExportEntry>()
        snapshot.featuredProjects.forEach { project ->
            entries += mediaEntry(
                ownerType = "featured_project",
                ownerId = project.id,
                field = "thumbnailUri",
                sourcePath = project.thumbnailUri,
                archivePath = "media/design/projects/thumbnails/${project.safeId()}_%s${project.thumbnailUri.safeExtension(".jpg")}",
            )
            project.galleryImages.forEachIndexed { index, path ->
                entries += mediaEntry(
                    ownerType = "featured_project",
                    ownerId = project.id,
                    field = "galleryImages[$index]",
                    sourcePath = path,
                    archivePath = "media/design/projects/gallery/${project.safeId()}/${(index + 1).toString().padStart(4, '0')}_%s${path.safeExtension(".jpg")}",
                )
            }
        }
        snapshot.virtualTours.forEach { tour ->
            entries += mediaEntry("virtual_tour", tour.id, "thumbnailUri", tour.thumbnailUri, "media/design/tours/thumbnails/${tour.safeId()}_%s${tour.thumbnailUri.safeExtension(".jpg")}")
        }
        snapshot.videos.forEach { video ->
            video.thumbnailUri?.takeIf { it.isNotBlank() && !it.isRemoteUrl() }?.let { thumbnail ->
                entries += mediaEntry("video", video.id, "thumbnailUri", thumbnail, "media/design/videos/thumbnails/${video.safeId()}_%s${thumbnail.safeExtension(".jpg")}")
            }
        }
        snapshot.brochures.forEach { brochure ->
            entries += mediaEntry("brochure", brochure.id, "pdfUri", brochure.pdfUri, "media/brochures/pdfs/${brochure.safeId()}_%s${brochure.pdfUri.safeExtension(".pdf")}")
            entries += mediaEntry("brochure", brochure.id, "coverThumbnailUri", brochure.coverThumbnailUri, "media/brochures/covers/${brochure.safeId()}_%s${brochure.coverThumbnailUri.safeExtension(".jpg")}")
        }
        snapshot.destinations.forEach { destination ->
            entries += mediaEntry("destination", destination.id, "imageUri", destination.imageUri, "media/tourism/destinations/${destination.safeId()}_%s${destination.imageUri.safeExtension(".jpg")}")
        }
        snapshot.services.forEach { service ->
            entries += mediaEntry("service", service.id, "imageUri", service.imageUri, "media/tourism/services/${service.safeId()}_%s${service.imageUri.safeExtension(".jpg")}")
        }
        snapshot.worldMapSections.forEach { section ->
            val previewFile = mapPreviewFile(context, section.sectionId)
            if (previewFile.exists()) {
                entries += mediaEntry(
                    ownerType = "world_map_section",
                    ownerId = section.sectionId,
                    field = "previewImage",
                    sourcePath = previewFile.absolutePath,
                    archivePath = mapPreviewArchivePath(section.sectionId),
                )
            }
        }
        snapshot.contentPageCards.forEach { card ->
            entries += mediaEntry(
                ownerType = "content_page_card",
                ownerId = card.id,
                field = "imagePath",
                sourcePath = card.imagePath,
                archivePath = "media/content/pages/${card.safeId()}_%s${card.imagePath.safeExtension(".jpg")}",
            )
        }
        snapshot.artGalleryCards.forEach { card ->
            entries += mediaEntry(
                ownerType = "art_gallery_card",
                ownerId = card.id,
                field = "imagePath",
                sourcePath = card.imagePath,
                archivePath = "media/art_gallery/cards/${card.safeId()}_%s${card.imagePath.safeExtension(".jpg")}",
            )
        }
        return entries.filter { it.sourceFile.exists() }
    }

    private fun mediaEntry(ownerType: String, ownerId: String, field: String, sourcePath: String, archivePath: String): MediaExportEntry {
        if (sourcePath.isBlank() || sourcePath.isRemoteUrl()) {
            return MediaExportEntry(ownerType, ownerId, field, File(""), "")
        }
        val sourceFile = File(sourcePath)
        if (!sourceFile.exists()) error("Missing media for $ownerType $ownerId $field: $sourcePath")
        val sha = sourceFile.sha256()
        return MediaExportEntry(ownerType, ownerId, field, sourceFile, archivePath.format(sha.take(8)), sha)
    }

    private fun writeZipPayload(
        zipFile: File,
        snapshot: BackupSnapshot,
        mediaEntries: List<MediaExportEntry>,
        compressionLevel: Int,
        encrypted: Boolean,
        onProgress: (BackupProgress) -> Unit,
    ) {
        BufferedOutputStream(FileOutputStream(zipFile)).use { output ->
            writeZipPayload(
                output = output,
                snapshot = snapshot,
                mediaEntries = mediaEntries,
                compressionLevel = compressionLevel,
                encrypted = encrypted,
                onProgress = onProgress,
            )
        }
    }

    private fun writeZipPayload(
        output: OutputStream,
        snapshot: BackupSnapshot,
        mediaEntries: List<MediaExportEntry>,
        compressionLevel: Int,
        encrypted: Boolean,
        onProgress: (BackupProgress) -> Unit,
    ) {
        val mediaBySource = mediaEntries.associateBy { it.sourceFile.absolutePath }
        val contentSnapshot = snapshot.withArchivePaths(mediaBySource)
        val totalBytes = mediaEntries.sumOf { it.sourceFile.length() }
        val verifier = JSONObject()
            .put("algorithm", "SHA-256")
            .put("fileCount", mediaEntries.size)
            .put("totalBytes", totalBytes)
            .put("files", JSONArray().also { array ->
                mediaEntries.forEach { entry ->
                    array.put(
                        JSONObject()
                            .put("ownerType", entry.ownerType)
                            .put("ownerId", entry.ownerId)
                            .put("field", entry.field)
                            .put("archivePath", entry.archivePath)
                            .put("originalPathType", if (entry.sourceFile.absolutePath.startsWith(context.filesDir.absolutePath)) "app_private" else "external_repaired")
                            .put("bytes", entry.sourceFile.length())
                            .put("sha256", entry.sha256),
                    )
                }
            })
        val manifest = JSONObject()
            .put("format", FORMAT)
            .put("extension", ".kskm")
            .put("formatVersion", FORMAT_VERSION)
            .put("appId", context.packageName)
            .put("appVersionName", runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "1.0")
            .put("createdAt", OffsetDateTime.now(ZoneId.systemDefault()).toString())
            .put("encrypted", encrypted)
            .put("contentCounts", contentSnapshot.contentCounts().toJsonObject())
            .put("media", JSONObject().put("fileCount", mediaEntries.size).put("totalBytes", totalBytes))

        ZipOutputStream(BufferedOutputStream(output)).use { zip ->
            zip.setLevel(compressionLevel)
            zip.putJson("manifest.json", manifest)
            zip.putJson("verifier.json", verifier)
            zip.putJson("settings/app_settings.json", contentSnapshot.settings.toJson())
            zip.putJson("content/companies.json", contentSnapshot.companies.toCompaniesJson())
            zip.putJson("content/sections.json", contentSnapshot.sections.toSectionsJson())
            zip.putJson("content/featured_projects.json", contentSnapshot.featuredProjects.toFeaturedProjectsJson())
            zip.putJson("content/virtual_tours.json", contentSnapshot.virtualTours.toVirtualToursJson())
            zip.putJson("content/videos.json", contentSnapshot.videos.toVideosJson())
            zip.putJson("content/brochures.json", contentSnapshot.brochures.toBrochuresJson())
            zip.putJson("content/tourism_destinations.json", contentSnapshot.destinations.toDestinationsJson())
            zip.putJson("content/tourism_services.json", contentSnapshot.services.toServicesJson())
            zip.putJson("content/global_links.json", contentSnapshot.globalLinks.toGlobalLinksJson())
            zip.putJson("content/world_map_sections.json", contentSnapshot.worldMapSections.toWorldMapSectionsJson())
            zip.putJson("content/world_map_pins.json", contentSnapshot.worldMapPins.toWorldMapPinsJson())
            zip.putJson("content/review_cards.json", contentSnapshot.reviewCards.toReviewCardsJson())
            zip.putJson("content/content_page_cards.json", contentSnapshot.contentPageCards.toContentPageCardsJson())
            zip.putJson("content/art_gallery_heroes.json", contentSnapshot.artGalleryHeroes.toArtGalleryHeroesJson())
            zip.putJson("content/art_gallery_cards.json", contentSnapshot.artGalleryCards.toArtGalleryCardsJson())
            zip.putJson("content/remote_image_assets.json", contentSnapshot.remoteImageAssets.toRemoteImageAssetsJson())

            var completedFiles = 0
            var completedBytes = 0L
            mediaEntries.forEach { entry ->
                zip.putNextEntry(ZipEntry(entry.archivePath))
                FileInputStream(entry.sourceFile).use { input -> input.copyTo(zip) }
                zip.closeEntry()
                completedFiles += 1
                completedBytes += entry.sourceFile.length()
                onProgress(
                    BackupProgress(
                        phase = BackupPhase.Package,
                        message = "Packed $completedFiles of ${mediaEntries.size} media files.",
                        completedFiles = completedFiles,
                        totalFiles = mediaEntries.size,
                        completedBytes = completedBytes,
                        totalBytes = totalBytes,
                    ),
                )
            }
        }
    }

    private fun readDocumentMeta(inputUri: Uri): Pair<String, Long> {
        context.contentResolver.query(
            inputUri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val name = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                val size = if (sizeIndex >= 0) cursor.getLong(sizeIndex).coerceAtLeast(0L) else 0L
                return name to size
            }
        }
        return inputUri.lastPathSegment.orEmpty().ifBlank { "selected-backup.kskm" } to 0L
    }

    private fun inspectZipPayload(
        input: InputStream,
        displayName: String,
        sizeBytes: Long,
        onProgress: (BackupProgress) -> Unit,
    ): BackupInspection {
        ZipInputStream(BufferedInputStream(input)).use { zip ->
            var manifest: JSONObject? = null
            var verifier: JSONObject? = null
            while (true) {
                val entry = zip.nextEntry ?: break
                when (entry.name) {
                    "manifest.json" -> {
                        manifest = JSONObject(zip.readCurrentEntryText())
                        onProgress(
                            BackupProgress(
                                phase = BackupPhase.Inspect,
                                message = "Manifest loaded.",
                                totalBytes = sizeBytes,
                                progressFraction = 0.6f,
                            ),
                        )
                    }
                    "verifier.json" -> {
                        verifier = JSONObject(zip.readCurrentEntryText())
                        val fileCount = verifier?.optInt("fileCount") ?: 0
                        val mediaBytes = verifier?.optLong("totalBytes") ?: 0L
                        onProgress(
                            BackupProgress(
                                phase = BackupPhase.Inspect,
                                message = if (fileCount > 0) {
                                    "Verifier metadata loaded for $fileCount files."
                                } else {
                                    "Verifier metadata loaded."
                                },
                                completedFiles = 0,
                                totalFiles = fileCount,
                                completedBytes = 0L,
                                totalBytes = mediaBytes.takeIf { it > 0L } ?: sizeBytes,
                                progressFraction = 0.86f,
                            ),
                        )
                    }
                    else -> zip.drainCurrentEntry()
                }
                zip.closeEntry()
                val resolvedManifest = manifest
                val hasEnoughManifestData = resolvedManifest
                    ?.optJSONObject("media")
                    ?.let { mediaInfo ->
                        mediaInfo.has("fileCount") || mediaInfo.has("totalBytes")
                    } == true
                if (manifest != null && (verifier != null || hasEnoughManifestData)) {
                    break
                }
            }
            val resolvedManifest = requireNotNull(manifest) { "Selected file is missing manifest.json." }
            require(resolvedManifest.getString("format") == FORMAT) { "This is not a Gelio backup." }
            require(resolvedManifest.getInt("formatVersion") == FORMAT_VERSION) { "Unsupported backup format version." }
            val contentCountsJson = resolvedManifest.optJSONObject("contentCounts")
            val contentCounts = buildMap {
                contentCountsJson?.keys()?.forEach { key ->
                    put(key, contentCountsJson.optInt(key))
                }
            }
            val mediaInfo = resolvedManifest.optJSONObject("media")
            val fileCount = verifier?.optInt("fileCount") ?: mediaInfo?.optInt("fileCount")
            val mediaBytes = verifier?.optLong("totalBytes") ?: mediaInfo?.optLong("totalBytes")
            onProgress(
                BackupProgress(
                    phase = BackupPhase.Inspect,
                    message = "Inspection complete. Ready to import.",
                    completedFiles = 0,
                    totalFiles = fileCount ?: 0,
                    completedBytes = 0L,
                    totalBytes = mediaBytes ?: sizeBytes,
                    progressFraction = 1f,
                ),
            )
            return BackupInspection(
                displayName = displayName,
                sizeBytes = sizeBytes,
                passwordProtected = false,
                fileCount = fileCount,
                mediaBytes = mediaBytes,
                contentCounts = contentCounts,
            )
        }
    }

    private fun verifyZipPayload(
        zipFile: File,
        onVerifiedEntry: ((verifiedFiles: Int, totalFiles: Int, verifiedBytes: Long, totalBytes: Long) -> Unit)? = null,
    ): BackupVerification {
        FileInputStream(zipFile).use { return verifyZipPayload(it, onVerifiedEntry) }
    }

    private fun verifyZipPayload(
        input: InputStream,
        onVerifiedEntry: ((verifiedFiles: Int, totalFiles: Int, verifiedBytes: Long, totalBytes: Long) -> Unit)? = null,
    ): BackupVerification {
        ZipInputStream(BufferedInputStream(input)).use { zip ->
            var manifestValidated = false
            var verifierFiles: JSONArray? = null
            var expectedFileCount = 0
            var expectedTotalBytes = 0L
            val expectedMedia = linkedMapOf<String, JSONObject>()
            var verifiedFileCount = 0
            var verifiedTotalBytes = 0L

            while (true) {
                val entry = zip.nextEntry ?: break
                when (entry.name) {
                    "manifest.json" -> {
                        val manifest = JSONObject(zip.readCurrentEntryText())
                        require(manifest.getString("format") == FORMAT) { "This is not a Gelio backup." }
                        require(manifest.getInt("formatVersion") == FORMAT_VERSION) { "Unsupported backup format version." }
                        manifestValidated = true
                    }

                    "verifier.json" -> {
                        val verifier = JSONObject(zip.readCurrentEntryText())
                        val files = verifier.getJSONArray("files")
                        verifierFiles = files
                        expectedFileCount = verifier.getInt("fileCount")
                        expectedTotalBytes = verifier.getLong("totalBytes")
                        repeat(files.length()) { index ->
                            val item = files.getJSONObject(index)
                            expectedMedia[item.getString("archivePath")] = item
                        }
                    }

                    else -> {
                        val media = expectedMedia[entry.name]
                        if (media == null) {
                            zip.drainCurrentEntry()
                        } else {
                            val sha = MessageDigest.getInstance("SHA-256")
                            var bytes = 0L
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            while (true) {
                                val read = zip.read(buffer)
                                if (read == -1) break
                                bytes += read
                                sha.update(buffer, 0, read)
                            }
                            require(bytes == media.getLong("bytes")) { "Byte count mismatch for ${entry.name}" }
                            require(sha.digest().toHex() == media.getString("sha256")) { "Checksum mismatch for ${entry.name}" }
                            verifiedFileCount += 1
                            verifiedTotalBytes += bytes
                            onVerifiedEntry?.invoke(verifiedFileCount, expectedFileCount, verifiedTotalBytes, expectedTotalBytes)
                        }
                    }
                }
                zip.closeEntry()
            }

            require(manifestValidated) { "Missing backup manifest." }
            val files = requireNotNull(verifierFiles) { "Missing backup verifier." }
            require(verifiedFileCount == expectedFileCount) { "Verifier file count mismatch." }
            require(verifiedTotalBytes == expectedTotalBytes) { "Verifier byte count mismatch." }
            return BackupVerification(fileCount = files.length(), totalBytes = verifiedTotalBytes, filesJson = files)
        }
    }

    private fun copyVerifiedMediaFromZip(
        zipFile: File,
        verifier: BackupVerification,
        importRoot: File,
        onProgress: (BackupProgress) -> Unit,
    ): Map<String, String> {
        val pathMap = mutableMapOf<String, String>()
        ZipFile(zipFile).use { zip ->
            var completedFiles = 0
            var completedBytes = 0L
            repeat(verifier.filesJson.length()) { index ->
                val item = verifier.filesJson.getJSONObject(index)
                val archivePath = item.getString("archivePath")
                val target = File(importRoot, archivePath).also { it.parentFile?.mkdirs() }
                zip.getInputStream(zip.getEntry(archivePath)).use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
                require(target.length() == item.getLong("bytes")) { "Imported byte count mismatch for $archivePath" }
                require(target.sha256() == item.getString("sha256")) { "Imported checksum mismatch for $archivePath" }
                generateImageThumbnailIfSupported(context, target)
                pathMap[archivePath] = target.absolutePath
                completedFiles += 1
                completedBytes += target.length()
                onProgress(
                    BackupProgress(
                        phase = BackupPhase.Import,
                        message = "Imported $completedFiles of ${verifier.fileCount} media files.",
                        completedFiles = completedFiles,
                        totalFiles = verifier.fileCount,
                        completedBytes = completedBytes,
                        totalBytes = verifier.totalBytes,
                    ),
                )
            }
        }
        return pathMap
    }

    private fun restoreMapPreviews(
        sections: List<WorldMapSectionEntity>,
        mediaPathMap: Map<String, String>,
    ) {
        sections.forEach { section ->
            val importedPreviewPath = mediaPathMap[mapPreviewArchivePath(section.sectionId)]
            val destination = mapPreviewFile(context, section.sectionId)
            if (importedPreviewPath == null) {
                if (destination.exists()) destination.delete()
                return@forEach
            }
            val source = File(importedPreviewPath)
            if (!source.exists()) return@forEach
            destination.parentFile?.mkdirs()
            source.copyTo(destination, overwrite = true)
        }
    }

    private fun parseSnapshotFromZip(zipFile: File, mediaPathMap: Map<String, String>): BackupSnapshot =
        ZipFile(zipFile).use { zip ->
            val companies = zip.readOptionalJsonArray("content/companies.json")?.toCompanies().orEmpty()
            val sections = zip.readOptionalJsonArray("content/sections.json")?.toSections().orEmpty()

            BackupSnapshot(
                settings = zip.readJsonObject("settings/app_settings.json").toAppSettings(),
                companies = companies,
                sections = sections,
                featuredProjects = zip.readJsonArray("content/featured_projects.json").toFeaturedProjects(mediaPathMap),
                virtualTours = zip.readJsonArray("content/virtual_tours.json").toVirtualTours(mediaPathMap),
                videos = zip.readJsonArray("content/videos.json").toVideos(mediaPathMap),
                brochures = zip.readJsonArray("content/brochures.json").toBrochures(mediaPathMap),
                destinations = zip.readJsonArray("content/tourism_destinations.json").toDestinations(mediaPathMap),
                services = zip.readJsonArray("content/tourism_services.json").toServices(mediaPathMap),
                globalLinks = zip.readJsonArray("content/global_links.json").toGlobalLinks(),
                worldMapSections = zip.readOptionalJsonArray("content/world_map_sections.json")?.toWorldMapSections().orEmpty(),
                worldMapPins = zip.readOptionalJsonArray("content/world_map_pins.json")?.toWorldMapPins().orEmpty(),
                reviewCards = zip.readOptionalJsonArray("content/review_cards.json")?.toReviewCards().orEmpty(),
                contentPageCards = zip.readOptionalJsonArray("content/content_page_cards.json")?.toContentPageCards(mediaPathMap).orEmpty(),
                artGalleryHeroes = zip.readOptionalJsonArray("content/art_gallery_heroes.json")?.toArtGalleryHeroes().orEmpty(),
                artGalleryCards = zip.readOptionalJsonArray("content/art_gallery_cards.json")?.toArtGalleryCards(mediaPathMap).orEmpty(),
                remoteImageAssets = zip.readOptionalJsonArray("content/remote_image_assets.json")?.toRemoteImageAssets(mediaPathMap).orEmpty(),
            )
        }.withLegacySectionsFilled()

    private fun encryptZipPayload(zipFile: File, destination: OutputStream, password: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(128, iv))
        }
        DataOutputStream(BufferedOutputStream(destination)).use { output ->
            output.write(ENCRYPTED_MAGIC)
            output.writeInt(salt.size)
            output.write(salt)
            output.writeInt(iv.size)
            output.write(iv)
            CipherOutputStream(output, cipher).use { encrypted ->
                FileInputStream(zipFile).use { input -> input.copyTo(encrypted) }
            }
        }
    }

    private fun copyOrDecryptBackupToZip(
        inputUri: Uri,
        zipFile: File,
        password: String?,
    ) {
        val input = requireNotNull(context.contentResolver.openInputStream(inputUri)) {
            "Unable to open selected backup file."
        }
        BufferedInputStream(input).use { buffered ->
            buffered.mark(ENCRYPTED_MAGIC.size + 4)
            val header = ByteArray(ENCRYPTED_MAGIC.size)
            val read = buffered.read(header)
            buffered.reset()
            if (read == ENCRYPTED_MAGIC.size && header.contentEquals(ENCRYPTED_MAGIC)) {
                val resolvedPassword = password?.takeIf { it.isNotBlank() }
                    ?: error("This backup is password protected. Enter the password and try again.")
                decryptZipPayload(buffered, zipFile, resolvedPassword)
            } else {
                FileOutputStream(zipFile).use { output -> buffered.copyTo(output) }
            }
        }
    }

    private fun decryptZipPayload(source: InputStream, zipFile: File, password: String) {
        DataInputStream(BufferedInputStream(source)).use { input ->
            val magic = ByteArray(ENCRYPTED_MAGIC.size)
            input.readFully(magic)
            require(magic.contentEquals(ENCRYPTED_MAGIC)) { "Invalid encrypted backup header." }
            val salt = ByteArray(input.readInt()).also { input.readFully(it) }
            val iv = ByteArray(input.readInt()).also { input.readFully(it) }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.DECRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(128, iv))
            }
            CipherInputStream(input, cipher).use { decrypted ->
                FileOutputStream(zipFile).use { output -> decrypted.copyTo(output) }
            }
        }
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, 120_000, 256)
        val encoded = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return SecretKeySpec(encoded, "AES")
    }

    private fun nextDownloadsBackupName(): String {
        val stamp = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmm").format(OffsetDateTime.now(ZoneId.systemDefault()))
        val base = "gelio-$stamp"
        for (index in 0..99) {
            val candidate = if (index == 0) "$base.kskm" else "$base ($index).kskm"
            if (!downloadsFileExists(candidate)) return candidate
        }
        return "$base-${UUID.randomUUID().toString().take(8)}.kskm"
    }

    private fun downloadsFileExists(displayName: String): Boolean {
        val collection = downloadsCollectionUri()
        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
        context.contentResolver.query(
            collection,
            projection,
            "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
            arrayOf(displayName),
            null,
        )?.use { cursor ->
            return cursor.moveToFirst()
        }
        return false
    }

    private fun createDownloadsBackupUri(displayName: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        return context.contentResolver.insert(downloadsCollectionUri(), values)
            ?: error("Unable to create backup file in Downloads.")
    }

    private fun markDownloadsBackupComplete(uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver.update(
                uri,
                ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                null,
                null,
            )
        }
    }

    private fun downloadsCollectionUri(): Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Files.getContentUri("external")
        }

    private fun downloadsDirectory(): File =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

    private fun Uri.contentLength(): Long {
        context.contentResolver.query(this, arrayOf(OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (index >= 0) return cursor.getLong(index).coerceAtLeast(0L)
                }
            }
        return 0L
    }

    private fun requireUsableSpace(root: File, requiredBytes: Long, label: String) {
        if (requiredBytes <= 0L) return
        root.mkdirs()
        val usableBytes = runCatching {
            val storageManager = context.getSystemService(StorageManager::class.java)
            val uuid = storageManager?.getUuidForPath(root)
            if (storageManager != null && uuid != null) storageManager.getAllocatableBytes(uuid) else root.usableSpace
        }.getOrDefault(root.usableSpace)
        require(usableBytes >= requiredBytes) {
            "Not enough free space for $label. Need about ${requiredBytes.formatBytes()}, available ${usableBytes.formatBytes()}."
        }
    }

    private fun backupSpacePadding(bytes: Long): Long =
        maxOf(256L * 1024L * 1024L, bytes / 10L)

    private fun removeOldMediaExcept(activeImportRoot: File) {
        val mediaRoot = File(context.filesDir, "showcase_media")
        val activePath = activeImportRoot.canonicalPath
        mediaRoot.listFiles()?.forEach { child ->
            if (child.name != "imported") {
                child.deleteRecursively()
            } else {
                child.listFiles()?.forEach { importedBackup ->
                    if (importedBackup.canonicalPath != activePath) {
                        importedBackup.deleteRecursively()
                    }
                }
            }
        }
    }

    private companion object {
        const val FORMAT = "gelio-backup"
        const val FORMAT_VERSION = 2
        val ENCRYPTED_MAGIC = "KSKM1ENC\n".toByteArray()
    }
  }

private fun mapPreviewArchivePath(sectionId: String): String =
    "media/maps/previews/${sectionId.safeName()}.png"

private data class BackupSnapshot(
    val settings: AppSettings,
    val companies: List<CompanyEntity>,
    val sections: List<SectionEntity>,
    val featuredProjects: List<FeaturedProjectEntity>,
    val virtualTours: List<VirtualTourEntity>,
    val videos: List<ShowcaseVideoEntity>,
    val brochures: List<BrochureEntity>,
    val destinations: List<DestinationEntity>,
    val services: List<ServiceEntity>,
    val globalLinks: List<GlobalLinkEntity>,
    val worldMapSections: List<WorldMapSectionEntity>,
    val worldMapPins: List<WorldMapPinEntity>,
    val reviewCards: List<ReviewCardEntity>,
    val contentPageCards: List<ContentPageCardEntity>,
    val artGalleryHeroes: List<ArtGalleryHeroEntity>,
    val artGalleryCards: List<ArtGalleryCardEntity>,
    val remoteImageAssets: List<RemoteImageAssetEntity>,
) {
    fun contentCounts(): Map<String, Int> =
        linkedMapOf(
            "companies" to companies.size,
            "sections" to sections.size,
            "featuredProjects" to featuredProjects.size,
            "virtualTours" to virtualTours.size,
            "videos" to videos.size,
            "brochures" to brochures.size,
            "destinations" to destinations.size,
            "services" to services.size,
            "globalLinks" to globalLinks.size,
            "worldMapSections" to worldMapSections.size,
            "worldMapPins" to worldMapPins.size,
            "reviewCards" to reviewCards.size,
            "contentPageCards" to contentPageCards.size,
            "artGalleryHeroes" to artGalleryHeroes.size,
            "artGalleryCards" to artGalleryCards.size,
            "remoteImageAssets" to remoteImageAssets.size,
        )

    fun withArchivePaths(mediaBySourcePath: Map<String, MediaExportEntry>): BackupSnapshot =
        copy(
            featuredProjects = featuredProjects.map { project ->
                project.copy(
                    galleryImages = project.galleryImages.map { mediaBySourcePath[it]?.archivePath ?: it },
                    thumbnailUri = mediaBySourcePath[project.thumbnailUri]?.archivePath ?: project.thumbnailUri,
                )
            },
            virtualTours = virtualTours.map { tour ->
                tour.copy(thumbnailUri = mediaBySourcePath[tour.thumbnailUri]?.archivePath ?: tour.thumbnailUri)
            },
            videos = videos.map { video ->
                video.copy(thumbnailUri = video.thumbnailUri?.let { mediaBySourcePath[it]?.archivePath ?: it })
            },
            brochures = brochures.map { brochure ->
                brochure.copy(
                    pdfUri = mediaBySourcePath[brochure.pdfUri]?.archivePath ?: brochure.pdfUri,
                    coverThumbnailUri = mediaBySourcePath[brochure.coverThumbnailUri]?.archivePath ?: brochure.coverThumbnailUri,
                )
            },
            destinations = destinations.map { destination ->
                destination.copy(imageUri = mediaBySourcePath[destination.imageUri]?.archivePath ?: destination.imageUri)
            },
            services = services.map { service ->
                service.copy(imageUri = mediaBySourcePath[service.imageUri]?.archivePath ?: service.imageUri)
            },
            contentPageCards = contentPageCards.map { card ->
                card.copy(imagePath = mediaBySourcePath[card.imagePath]?.archivePath ?: card.imagePath)
            },
            artGalleryCards = artGalleryCards.map { card ->
                card.copy(imagePath = mediaBySourcePath[card.imagePath]?.archivePath ?: card.imagePath)
            },
            remoteImageAssets = remoteImageAssets.map { asset ->
                asset.copy(localPath = mediaBySourcePath[asset.localPath]?.archivePath ?: asset.localPath)
            },
        )

    fun referencedLocalMediaPaths(): Set<String> =
        buildSet {
            featuredProjects.forEach { project ->
                addAll(project.galleryImages.filter(String::isNotBlank))
                project.thumbnailUri.takeIf(String::isNotBlank)?.let(::add)
            }
            virtualTours.forEach { tour ->
                tour.thumbnailUri.takeIf(String::isNotBlank)?.let(::add)
            }
            videos.forEach { video ->
                video.thumbnailUri?.takeIf(String::isNotBlank)?.let(::add)
            }
            brochures.forEach { brochure ->
                brochure.pdfUri.takeIf(String::isNotBlank)?.let(::add)
                brochure.coverThumbnailUri.takeIf(String::isNotBlank)?.let(::add)
            }
            destinations.forEach { destination ->
                destination.imageUri.takeIf(String::isNotBlank)?.let(::add)
            }
            services.forEach { service ->
                service.imageUri.takeIf(String::isNotBlank)?.let(::add)
            }
            contentPageCards.forEach { card ->
                card.imagePath.takeIf(String::isNotBlank)?.let(::add)
            }
            artGalleryCards.forEach { card ->
                card.imagePath.takeIf(String::isNotBlank)?.let(::add)
            }
        }
}

private data class MediaExportEntry(
    val ownerType: String,
    val ownerId: String,
    val field: String,
    val sourceFile: File,
    val archivePath: String,
    val sha256: String = "",
)

private data class BackupVerification(
    val fileCount: Int,
    val totalBytes: Long,
    val filesJson: JSONArray,
)

private fun BackupSnapshot.withLegacySectionsFilled(): BackupSnapshot {
    val knownSectionIds = sections.mapTo(mutableSetOf()) { it.id }
    val filteredArtGalleryHeroes = artGalleryHeroes.filter { it.sectionId in knownSectionIds }
    val knownHeroIds = filteredArtGalleryHeroes.mapTo(mutableSetOf()) { it.id }
    return copy(
        featuredProjects = featuredProjects.filter { it.sectionId in knownSectionIds },
        virtualTours = virtualTours.filter { it.sectionId in knownSectionIds },
        videos = videos.filter { it.sectionId in knownSectionIds },
        brochures = brochures.filter { it.sectionId in knownSectionIds },
        destinations = destinations.filter { it.sectionId in knownSectionIds },
        services = services.filter { it.sectionId in knownSectionIds },
        globalLinks = globalLinks.filter { it.sectionId in knownSectionIds },
        worldMapSections = worldMapSections.filter { it.sectionId in knownSectionIds },
        worldMapPins = worldMapPins.filter { it.sectionId in knownSectionIds },
        reviewCards = reviewCards.filter { it.sectionId in knownSectionIds },
        contentPageCards = contentPageCards.filter { it.sectionId in knownSectionIds },
        artGalleryHeroes = filteredArtGalleryHeroes,
        artGalleryCards = artGalleryCards.filter { it.heroId in knownHeroIds },
    )
}

private fun ZipOutputStream.putJson(path: String, json: JSONObject) = putText(path, json.toString(2))
private fun ZipOutputStream.putJson(path: String, json: JSONArray) = putText(path, json.toString(2))

private fun ZipOutputStream.putText(path: String, text: String) {
    putNextEntry(ZipEntry(path))
    write(text.toByteArray(Charsets.UTF_8))
    closeEntry()
}

private fun ZipFile.readJsonObject(path: String): JSONObject = JSONObject(readText(path))
private fun ZipFile.readJsonArray(path: String): JSONArray = JSONArray(readText(path))
private fun ZipFile.readOptionalJsonArray(path: String): JSONArray? = getEntry(path)?.let { JSONArray(readText(path)) }

private fun ZipFile.readText(path: String): String {
    val entry = getEntry(path) ?: error("Missing $path in backup.")
    return getInputStream(entry).bufferedReader().use { it.readText() }
}

private fun ZipInputStream.readCurrentEntryText(): String =
    buildString {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = this@readCurrentEntryText.read(buffer)
            if (read == -1) break
            append(String(buffer, 0, read, Charsets.UTF_8))
        }
    }

private fun ZipInputStream.drainCurrentEntry() {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    while (read(buffer) != -1) {
        // Drain entry to advance the stream.
    }
}

private fun File.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().toHex()
}

private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

private fun String.isRemoteUrl(): Boolean =
    startsWith("http://", ignoreCase = true) || startsWith("https://", ignoreCase = true)

private fun String.isUsableBackupReference(): Boolean =
    isBlank() || isRemoteUrl() || startsWith("media/") || (!startsWith("content://") && File(removePrefix("file://")).exists())

private fun String.safeExtension(fallback: String): String {
    val clean = substringBefore('?').substringAfterLast('.', missingDelimiterValue = "")
    return clean.takeIf { it.isNotBlank() && it.length <= 8 }?.let { ".$it" } ?: fallback
}

private fun String.safeName(): String =
    replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { UUID.randomUUID().toString() }

private fun Long.formatBytes(): String {
    if (this < 1024) return "$this B"
    val kb = this / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    return "%.1f GB".format(mb / 1024.0)
}

private fun FeaturedProjectEntity.safeId(): String = id.safeName()
private fun VirtualTourEntity.safeId(): String = id.safeName()
private fun ShowcaseVideoEntity.safeId(): String = id.safeName()
private fun BrochureEntity.safeId(): String = id.safeName()
private fun DestinationEntity.safeId(): String = id.safeName()
private fun ServiceEntity.safeId(): String = id.safeName()
private fun ContentPageCardEntity.safeId(): String = id.safeName()
private fun ArtGalleryCardEntity.safeId(): String = id.safeName()

private fun AppSettings.toJson(): JSONObject =
    JSONObject()
        .put("themeMode", themeMode.name)
        .put("colorMode", colorMode.name)
        .put("curatedPalette", curatedPalette.name)
        .put("neutralBaseColor", neutralBaseColor)
        .put("idleTimeoutMinutes", idleTimeoutMinutes)
        .put("idleHeroTitle", idleHeroTitle)
        .put("idleHeroCaption", idleHeroCaption)
        .put("homescreenLogoPath", homescreenLogoPath)

private fun JSONObject.toAppSettings(): AppSettings =
    AppSettings(
        themeMode = ThemeMode.valueOf(getString("themeMode")),
        colorMode = AppColorMode.valueOf(getString("colorMode")),
        curatedPalette = CuratedPalette.valueOf(getString("curatedPalette")),
        neutralBaseColor = optString("neutralBaseColor", AppSettings().neutralBaseColor),
        idleTimeoutMinutes = getInt("idleTimeoutMinutes"),
        adminPin = optString("adminPin", "0000").takeIf { it.length == 4 && it.all(Char::isDigit) } ?: "0000",
        idleHeroTitle = optString("idleHeroTitle", "Hero Text"),
        idleHeroCaption = optString("idleHeroCaption", "enter the caption here"),
        kioskModeEnabled = optBoolean("kioskModeEnabled", false),
        pexelsApiKey = optString("pexelsApiKey", ""),
        homescreenLogoPath = optString("homescreenLogoPath", ""),
    )

private fun AppSettings.withDeviceLocalOnlySettings(deviceLocal: AppSettings): AppSettings =
    copy(
        adminPin = AppSettings().adminPin,
        kioskModeEnabled = deviceLocal.kioskModeEnabled,
        pexelsApiKey = deviceLocal.pexelsApiKey,
    )

private fun List<CompanyEntity>.toCompaniesJson(): JSONArray =
    JSONArray().also { array ->
        forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("name", item.name)
                    .put("paletteContextKey", item.paletteContextKey)
                    .put("logoPath", item.logoPath)
                    .put("brandSeedColor", item.brandSeedColor)
                    .put("hidden", item.hidden)
                    .put("sortOrder", item.sortOrder),
            )
        }
    }

private fun JSONArray.toCompanies(): List<CompanyEntity> =
    objects().map {
        CompanyEntity(
            id = it.getString("id"),
            name = it.getString("name"),
            paletteContextKey = it.optString("paletteContextKey", "NEUTRAL"),
            logoPath = it.optString("logoPath", ""),
            brandSeedColor = it.optString("brandSeedColor", "#8D4B68"),
            hidden = it.optBoolean("hidden", false),
            sortOrder = it.optInt("sortOrder", 0),
        )
    }

private fun List<SectionEntity>.toSectionsJson(): JSONArray =
    JSONArray().also { array ->
        forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("companyId", item.companyId)
                    .put("type", item.type)
                    .put("title", item.title)
                    .put("hidden", item.hidden)
                    .put("sortOrder", item.sortOrder),
            )
        }
    }

private fun JSONArray.toSections(): List<SectionEntity> =
    objects().map {
        SectionEntity(
            id = it.getString("id"),
            companyId = it.getString("companyId"),
            type = it.optString("type", SectionType.IMAGE_GALLERY.storageKey),
            title = it.getString("title"),
            hidden = it.optBoolean("hidden", false),
            sortOrder = it.optInt("sortOrder", 0),
        )
    }

private fun List<WorldMapSectionEntity>.toWorldMapSectionsJson(): JSONArray =
    JSONArray().also { array ->
        forEach { item ->
            array.put(
                JSONObject()
                    .put("sectionId", item.sectionId)
                    .put("assetName", item.assetName)
                    .put("subtitle", item.subtitle)
                    .put("countryLabel", item.countryLabel)
                    .put("cityLabel", item.cityLabel)
                    .put("viewportCenterX", item.viewportCenterX)
                    .put("viewportCenterY", item.viewportCenterY)
                    .put("zoomScale", item.zoomScale)
                    .put("highlightedCountryCodes", JSONArray(item.highlightedCountryCodes)),
            )
        }
    }

private fun JSONArray.toWorldMapSections(): List<WorldMapSectionEntity> =
    objects().map {
        WorldMapSectionEntity(
            sectionId = it.getString("sectionId"),
            assetName = it.optString("assetName", MapAssetPaths.WORLD),
            subtitle = it.optString("subtitle", ""),
            countryLabel = it.optString("countryLabel", ""),
            cityLabel = it.optString("cityLabel", ""),
            viewportCenterX = it.optDouble("viewportCenterX", 0.5).toFloat(),
            viewportCenterY = it.optDouble("viewportCenterY", 0.5).toFloat(),
            zoomScale = it.optDouble("zoomScale", 1.0).toFloat(),
            highlightedCountryCodes = it.optJSONArray("highlightedCountryCodes")?.strings().orEmpty(),
        )
    }

private fun List<WorldMapPinEntity>.toWorldMapPinsJson(): JSONArray =
    JSONArray().also { array ->
        forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("sectionId", item.sectionId)
                    .put("xNorm", item.xNorm)
                    .put("yNorm", item.yNorm)
                    .put("label", item.label),
            )
        }
    }

private fun JSONArray.toWorldMapPins(): List<WorldMapPinEntity> =
    objects().map {
        WorldMapPinEntity(
            id = it.getString("id"),
            sectionId = it.getString("sectionId"),
            xNorm = it.optDouble("xNorm", 0.5).toFloat(),
            yNorm = it.optDouble("yNorm", 0.5).toFloat(),
            label = it.optString("label"),
        )
    }

private fun List<ReviewCardEntity>.toReviewCardsJson(): JSONArray =
    JSONArray().also { array ->
        forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("sectionId", item.sectionId)
                    .put("reviewerName", item.reviewerName)
                    .put("sourceType", item.sourceType)
                    .put("subHeading", item.subHeading)
                    .put("comment", item.comment)
                    .put("rating", item.rating)
                    .put("hidden", item.hidden)
                    .put("sortOrder", item.sortOrder)
                    .put("createdAt", item.createdAt)
                    .put("updatedAt", item.updatedAt),
            )
        }
    }

private fun JSONArray.toReviewCards(): List<ReviewCardEntity> =
    objects().map {
        ReviewCardEntity(
            id = it.getString("id"),
            sectionId = it.optString("sectionId"),
            reviewerName = it.optString("reviewerName", ""),
            sourceType = it.optString("sourceType", "generic"),
            subHeading = it.optString("subHeading", ""),
            comment = it.optString("comment", ""),
            rating = it.optInt("rating", 5).coerceIn(1, 5),
            hidden = it.optBoolean("hidden", false),
            sortOrder = it.optInt("sortOrder", 0),
            createdAt = it.optLong("createdAt", 0L),
            updatedAt = it.optLong("updatedAt", 0L),
        )
    }

private fun List<ContentPageCardEntity>.toContentPageCardsJson(): JSONArray =
    JSONArray().also { array ->
        forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("sectionId", item.sectionId)
                    .put("title", item.title)
                    .put("bodyText", item.bodyText)
                    .put("imagePath", item.imagePath)
                    .put("hidden", item.hidden)
                    .put("sortOrder", item.sortOrder)
                    .put("createdAt", item.createdAt)
                    .put("updatedAt", item.updatedAt),
            )
        }
    }

private fun JSONArray.toContentPageCards(pathMap: Map<String, String>): List<ContentPageCardEntity> =
    objects().map {
        val imagePath = it.optString("imagePath", "")
        ContentPageCardEntity(
            id = it.getString("id"),
            sectionId = it.optString("sectionId"),
            title = it.optString("title", ""),
            bodyText = it.optString("bodyText", ""),
            imagePath = pathMap[imagePath] ?: imagePath,
            hidden = it.optBoolean("hidden", false),
            sortOrder = it.optInt("sortOrder", 0),
            createdAt = it.optLong("createdAt", 0L),
            updatedAt = it.optLong("updatedAt", 0L),
        )
    }

private fun List<ArtGalleryHeroEntity>.toArtGalleryHeroesJson(): JSONArray =
    JSONArray().also { array ->
        forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("sectionId", item.sectionId)
                    .put("title", item.title)
                    .put("description", item.description)
                    .put("hidden", item.hidden)
                    .put("sortOrder", item.sortOrder)
                    .put("createdAt", item.createdAt)
                    .put("updatedAt", item.updatedAt),
            )
        }
    }

private fun JSONArray.toArtGalleryHeroes(): List<ArtGalleryHeroEntity> =
    objects().map {
        ArtGalleryHeroEntity(
            id = it.getString("id"),
            sectionId = it.optString("sectionId"),
            title = it.optString("title", ""),
            description = it.optString("description", ""),
            hidden = it.optBoolean("hidden", false),
            sortOrder = it.optInt("sortOrder", 0),
            createdAt = it.optLong("createdAt", 0L),
            updatedAt = it.optLong("updatedAt", 0L),
        )
    }

private fun List<ArtGalleryCardEntity>.toArtGalleryCardsJson(): JSONArray =
    JSONArray().also { array ->
        forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("heroId", item.heroId)
                    .put("title", item.title)
                    .put("bodyText", item.bodyText)
                    .put("imagePath", item.imagePath)
                    .put("hidden", item.hidden)
                    .put("sortOrder", item.sortOrder)
                    .put("createdAt", item.createdAt)
                    .put("updatedAt", item.updatedAt),
            )
        }
    }

private fun JSONArray.toArtGalleryCards(pathMap: Map<String, String>): List<ArtGalleryCardEntity> =
    objects().map {
        val imagePath = it.optString("imagePath", "")
        ArtGalleryCardEntity(
            id = it.getString("id"),
            heroId = it.optString("heroId"),
            title = it.optString("title", ""),
            bodyText = it.optString("bodyText", ""),
            imagePath = pathMap[imagePath] ?: imagePath,
            hidden = it.optBoolean("hidden", false),
            sortOrder = it.optInt("sortOrder", 0),
            createdAt = it.optLong("createdAt", 0L),
            updatedAt = it.optLong("updatedAt", 0L),
        )
    }

private fun List<RemoteImageAssetEntity>.toRemoteImageAssetsJson(): JSONArray =
    JSONArray().also { array ->
        forEach { item ->
            array.put(
                JSONObject()
                    .put("localPath", item.localPath)
                    .put("provider", item.provider)
                    .put("providerAssetId", item.providerAssetId)
                    .put("sourcePageUrl", item.sourcePageUrl)
                    .put("downloadUrl", item.downloadUrl)
                    .put("photographerName", item.photographerName)
                    .put("photographerUrl", item.photographerUrl)
                    .put("importedAt", item.importedAt),
            )
        }
    }

private fun JSONArray.toRemoteImageAssets(pathMap: Map<String, String>): List<RemoteImageAssetEntity> =
    objects().map {
        val localPath = it.optString("localPath", "")
        RemoteImageAssetEntity(
            localPath = pathMap[localPath] ?: localPath,
            provider = it.optString("provider", "pexels"),
            providerAssetId = it.optString("providerAssetId", ""),
            sourcePageUrl = it.optString("sourcePageUrl", ""),
            downloadUrl = it.optString("downloadUrl", ""),
            photographerName = it.optString("photographerName", ""),
            photographerUrl = it.optString("photographerUrl", ""),
            importedAt = it.optLong("importedAt", 0L),
        )
    }.filter { it.localPath.isNotBlank() }

private fun List<FeaturedProjectEntity>.toFeaturedProjectsJson(): JSONArray =
    JSONArray().also { array ->
        forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("sectionId", item.sectionId)
                    .put("projectName", item.projectName)
                    .put("galleryImages", JSONArray(item.galleryImages))
                    .put("thumbnailUri", item.thumbnailUri)
                    .put("featured", item.featured)
                    .put("hidden", item.hidden)
                    .put("sortOrder", item.sortOrder),
            )
        }
    }

private fun JSONArray.toFeaturedProjects(pathMap: Map<String, String>): List<FeaturedProjectEntity> =
    objects().map {
        FeaturedProjectEntity(
            id = it.getString("id"),
            sectionId = it.optString("sectionId"),
            projectName = it.getString("projectName"),
            galleryImages = it.getJSONArray("galleryImages").strings().map { path -> pathMap[path] ?: path },
            thumbnailUri = pathMap[it.getString("thumbnailUri")] ?: it.getString("thumbnailUri"),
            featured = it.getBoolean("featured"),
            hidden = it.getBoolean("hidden"),
            sortOrder = it.getInt("sortOrder"),
        )
    }

private fun List<VirtualTourEntity>.toVirtualToursJson(): JSONArray =
    JSONArray().also { array ->
        forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("sectionId", item.sectionId)
                    .put("projectName", item.projectName)
                    .put("embedUrl", item.embedUrl)
                    .put("thumbnailUri", item.thumbnailUri)
                    .put("description", item.description)
                    .put("hidden", item.hidden)
                    .put("sortOrder", item.sortOrder),
            )
        }
    }

private fun JSONArray.toVirtualTours(pathMap: Map<String, String>): List<VirtualTourEntity> =
    objects().map {
        VirtualTourEntity(
            id = it.getString("id"),
            sectionId = it.optString("sectionId"),
            projectName = it.getString("projectName"),
            embedUrl = it.getString("embedUrl"),
            thumbnailUri = pathMap[it.getString("thumbnailUri")] ?: it.getString("thumbnailUri"),
            description = it.getString("description"),
            hidden = it.getBoolean("hidden"),
            sortOrder = it.getInt("sortOrder"),
        )
    }

private fun List<ShowcaseVideoEntity>.toVideosJson(): JSONArray =
    JSONArray().also { array ->
        forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("sectionId", item.sectionId)
                    .put("title", item.title)
                    .put("youtubeLink", item.youtubeLink)
                    .put("description", item.description)
                    .put("thumbnailUri", item.thumbnailUri ?: JSONObject.NULL)
                    .put("hidden", item.hidden)
                    .put("sortOrder", item.sortOrder),
            )
        }
    }

private fun JSONArray.toVideos(pathMap: Map<String, String>): List<ShowcaseVideoEntity> =
    objects().map {
        val thumbnail = it.optString("thumbnailUri").takeIf { value -> value.isNotBlank() && value != "null" }
        ShowcaseVideoEntity(
            id = it.getString("id"),
            sectionId = it.optString("sectionId"),
            title = it.getString("title"),
            youtubeLink = it.getString("youtubeLink"),
            description = it.getString("description"),
            thumbnailUri = thumbnail?.let { path -> pathMap[path] ?: path },
            hidden = it.getBoolean("hidden"),
            sortOrder = it.getInt("sortOrder"),
        )
    }

private fun List<BrochureEntity>.toBrochuresJson(): JSONArray =
    JSONArray().also { array ->
        forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("sectionId", item.sectionId)
                    .put("brand", item.brand)
                    .put("title", item.title)
                    .put("pdfUri", item.pdfUri)
                    .put("coverThumbnailUri", item.coverThumbnailUri)
                    .put("hidden", item.hidden)
                    .put("sortOrder", item.sortOrder),
            )
        }
    }

private fun JSONArray.toBrochures(pathMap: Map<String, String>): List<BrochureEntity> =
    objects().map {
        BrochureEntity(
            id = it.getString("id"),
            sectionId = it.optString("sectionId"),
            brand = it.optString("brand", Brand.GENERIC.name),
            title = it.getString("title"),
            pdfUri = pathMap[it.getString("pdfUri")] ?: it.getString("pdfUri"),
            coverThumbnailUri = pathMap[it.getString("coverThumbnailUri")] ?: it.getString("coverThumbnailUri"),
            hidden = it.getBoolean("hidden"),
            sortOrder = it.getInt("sortOrder"),
        )
    }

private fun List<DestinationEntity>.toDestinationsJson(): JSONArray =
    JSONArray().also { array ->
        forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("sectionId", item.sectionId)
                    .put("destinationName", item.destinationName)
                    .put("imageUri", item.imageUri)
                    .put("subtitle", item.subtitle)
                    .put("hidden", item.hidden)
                    .put("sortOrder", item.sortOrder),
            )
        }
    }

private fun JSONArray.toDestinations(pathMap: Map<String, String>): List<DestinationEntity> =
    objects().map {
        DestinationEntity(
            id = it.getString("id"),
            sectionId = it.optString("sectionId"),
            destinationName = it.getString("destinationName"),
            imageUri = pathMap[it.getString("imageUri")] ?: it.getString("imageUri"),
            subtitle = it.getString("subtitle"),
            hidden = it.getBoolean("hidden"),
            sortOrder = it.getInt("sortOrder"),
        )
    }

private fun List<ServiceEntity>.toServicesJson(): JSONArray =
    JSONArray().also { array ->
        forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("sectionId", item.sectionId)
                    .put("serviceTitle", item.serviceTitle)
                    .put("imageUri", item.imageUri)
                    .put("description", item.description)
                    .put("hidden", item.hidden)
                    .put("sortOrder", item.sortOrder),
            )
        }
    }

private fun JSONArray.toServices(pathMap: Map<String, String>): List<ServiceEntity> =
    objects().map {
        ServiceEntity(
            id = it.getString("id"),
            sectionId = it.optString("sectionId"),
            serviceTitle = it.getString("serviceTitle"),
            imageUri = pathMap[it.getString("imageUri")] ?: it.getString("imageUri"),
            description = it.getString("description"),
            hidden = it.getBoolean("hidden"),
            sortOrder = it.getInt("sortOrder"),
        )
    }

private fun List<GlobalLinkEntity>.toGlobalLinksJson(): JSONArray =
    JSONArray().also { array ->
        forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("sectionId", item.sectionId)
                    .put("brand", item.brand)
                    .put("label", item.label)
                    .put("url", item.url),
            )
        }
    }

private fun JSONArray.toGlobalLinks(): List<GlobalLinkEntity> =
    objects().map {
        GlobalLinkEntity(
            id = it.getString("id"),
            sectionId = it.optString("sectionId"),
            brand = it.optString("brand", Brand.GENERIC.name),
            label = it.getString("label"),
            url = it.getString("url"),
        )
    }

private fun JSONArray.objects(): List<JSONObject> =
    List(length()) { index -> getJSONObject(index) }

private fun JSONArray.strings(): List<String> =
    List(length()) { index -> getString(index) }

private fun Map<String, Int>.toJsonObject(): JSONObject =
    JSONObject().also { json ->
        forEach { (key, value) -> json.put(key, value) }
    }
