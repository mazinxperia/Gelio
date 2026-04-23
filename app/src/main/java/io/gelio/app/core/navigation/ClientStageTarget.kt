package io.gelio.app.core.navigation

import io.gelio.app.core.theme.BrandPaletteContext

sealed interface ClientStageTarget {
    data object Welcome : ClientStageTarget
    data object BrandSelection : ClientStageTarget
    data class Company(val companyId: String, val sectionId: String? = null) : ClientStageTarget
}

val ClientStageTarget.paletteContext: BrandPaletteContext
    get() = when (this) {
        is ClientStageTarget.Company -> BrandPaletteContext.NEUTRAL
        ClientStageTarget.BrandSelection,
        ClientStageTarget.Welcome -> BrandPaletteContext.NEUTRAL
    }
