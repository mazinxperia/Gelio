package io.gelio.app.app

enum class StartupWarmupStage(
    val label: String,
) {
    Seed("Preparing data"),
    LoadCompanies("Loading companies"),
    LoadSections("Loading sections"),
    WarmSectionPayloads("Warming content"),
    WarmLightweightMedia("Preparing previews"),
    WarmNetworkEnrichments("Resolving online previews"),
    Finalize("Finalizing"),
    Failed("Startup failed"),
}

data class StartupWarmupState(
    val isReady: Boolean = false,
    val stage: StartupWarmupStage = StartupWarmupStage.Seed,
    val completedTasks: Int = 0,
    val totalTasks: Int = 1,
    val message: String = "Preparing data",
    val errorMessage: String? = null,
)
