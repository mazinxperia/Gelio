package io.gelio.app.data.backup

import android.net.Uri

enum class BackupPhase {
    Idle,
    Scan,
    Repair,
    Package,
    Inspect,
    Verify,
    Import,
    Complete,
    Error,
}

data class BackupProgress(
    val phase: BackupPhase,
    val message: String,
    val completedFiles: Int = 0,
    val totalFiles: Int = 0,
    val completedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val progressFraction: Float? = null,
)

data class BackupSummary(
    val fileCount: Int,
    val totalBytes: Long,
    val contentCounts: Map<String, Int>,
    val outputLabel: String? = null,
)

data class BackupInspection(
    val displayName: String,
    val sizeBytes: Long,
    val passwordProtected: Boolean,
    val fileCount: Int? = null,
    val mediaBytes: Long? = null,
    val contentCounts: Map<String, Int> = emptyMap(),
)

data class BackupExportRequest(
    val outputUri: Uri,
    val password: String?,
    val compressionLevel: Int,
)

data class BackupImportRequest(
    val inputUri: Uri,
    val password: String?,
)
