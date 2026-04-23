package io.gelio.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
import io.gelio.app.data.local.entity.SectionEntity
import io.gelio.app.data.local.entity.ServiceEntity
import io.gelio.app.data.local.entity.ShowcaseVideoEntity
import io.gelio.app.data.local.entity.ReviewCardEntity
import io.gelio.app.data.local.entity.VirtualTourEntity
import io.gelio.app.data.local.entity.WorldMapPinEntity
import io.gelio.app.data.local.entity.WorldMapSectionEntity
import io.gelio.app.data.model.SectionType
import io.gelio.app.data.repository.CompanySeed
import io.gelio.app.data.repository.SectionSeed

@Database(
    entities = [
        CompanyEntity::class,
        SectionEntity::class,
        FeaturedProjectEntity::class,
        VirtualTourEntity::class,
        ShowcaseVideoEntity::class,
        BrochureEntity::class,
        DestinationEntity::class,
        ServiceEntity::class,
        GlobalLinkEntity::class,
        WorldMapSectionEntity::class,
        WorldMapPinEntity::class,
        ReviewCardEntity::class,
        ContentPageCardEntity::class,
        ArtGalleryHeroEntity::class,
        ArtGalleryCardEntity::class,
        RemoteImageAssetEntity::class,
    ],
    version = 9,
    exportSchema = true,
)
@TypeConverters(ShowcaseTypeConverters::class)
abstract class ShowcaseDatabase : RoomDatabase() {
    abstract fun showcaseDao(): ShowcaseDao

    companion object {
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `companies` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `paletteContextKey` TEXT NOT NULL,
                        `hidden` INTEGER NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sections` (
                        `id` TEXT NOT NULL,
                        `companyId` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `hidden` INTEGER NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `world_map_sections` (
                        `sectionId` TEXT NOT NULL,
                        `assetName` TEXT NOT NULL,
                        `viewportCenterX` REAL NOT NULL,
                        `viewportCenterY` REAL NOT NULL,
                        `zoomScale` REAL NOT NULL,
                        `highlightedCountryCodes` TEXT NOT NULL,
                        PRIMARY KEY(`sectionId`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `world_map_pins` (
                        `id` TEXT NOT NULL,
                        `sectionId` TEXT NOT NULL,
                        `xNorm` REAL NOT NULL,
                        `yNorm` REAL NOT NULL,
                        `label` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )

                listOf(
                    "featured_projects",
                    "virtual_tours",
                    "videos",
                    "brochures",
                    "destinations",
                    "services",
                    "global_links",
                ).forEach { table ->
                    db.execSQL("ALTER TABLE `$table` ADD COLUMN `sectionId` TEXT NOT NULL DEFAULT ''")
                }

                db.execSQL(
                    """
                    INSERT OR REPLACE INTO `companies` (`id`, `name`, `paletteContextKey`, `hidden`, `sortOrder`)
                    VALUES
                    ('${CompanySeed.DESIGN_ID}', 'Legacy Design', 'DESIGN', 0, 0),
                    ('${CompanySeed.TOURISM_ID}', 'Legacy Tourism', 'TOURISM', 0, 1)
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT OR REPLACE INTO `sections` (`id`, `companyId`, `type`, `title`, `hidden`, `sortOrder`)
                    VALUES
                    ('${SectionSeed.DESIGN_FEATURED_PROJECTS_ID}', '${CompanySeed.DESIGN_ID}', '${SectionType.IMAGE_GALLERY.storageKey}', 'Project Images', 0, 0),
                    ('${SectionSeed.DESIGN_VIRTUAL_TOURS_ID}', '${CompanySeed.DESIGN_ID}', '${SectionType.TOUR_360.storageKey}', 'Virtual Tours', 0, 1),
                    ('${SectionSeed.DESIGN_VIDEOS_ID}', '${CompanySeed.DESIGN_ID}', '${SectionType.YOUTUBE_VIDEOS.storageKey}', 'YouTube Videos', 0, 2),
                    ('${SectionSeed.DESIGN_BROCHURES_ID}', '${CompanySeed.DESIGN_ID}', '${SectionType.PDF_VIEWER.storageKey}', 'Brochures', 0, 3),
                    ('${SectionSeed.TOURISM_DESTINATIONS_ID}', '${CompanySeed.TOURISM_ID}', '${SectionType.DESTINATIONS.storageKey}', 'Destinations', 0, 0),
                    ('${SectionSeed.TOURISM_SERVICES_ID}', '${CompanySeed.TOURISM_ID}', '${SectionType.SERVICES.storageKey}', 'Services', 0, 1),
                    ('${SectionSeed.TOURISM_BROCHURES_ID}', '${CompanySeed.TOURISM_ID}', '${SectionType.PDF_VIEWER.storageKey}', 'Brochures', 0, 2)
                    """.trimIndent(),
                )

                db.execSQL("UPDATE `featured_projects` SET `sectionId` = '${SectionSeed.DESIGN_FEATURED_PROJECTS_ID}'")
                db.execSQL("UPDATE `virtual_tours` SET `sectionId` = '${SectionSeed.DESIGN_VIRTUAL_TOURS_ID}'")
                db.execSQL("UPDATE `videos` SET `sectionId` = '${SectionSeed.DESIGN_VIDEOS_ID}'")
                db.execSQL(
                    """
                    UPDATE `brochures`
                    SET `sectionId` = CASE
                        WHEN `brand` = 'DESIGN' THEN '${SectionSeed.DESIGN_BROCHURES_ID}'
                        ELSE '${SectionSeed.TOURISM_BROCHURES_ID}'
                    END
                    """.trimIndent(),
                )
                db.execSQL("UPDATE `destinations` SET `sectionId` = '${SectionSeed.TOURISM_DESTINATIONS_ID}'")
                db.execSQL("UPDATE `services` SET `sectionId` = '${SectionSeed.TOURISM_SERVICES_ID}'")
                db.execSQL(
                    """
                    UPDATE `global_links`
                    SET `sectionId` = CASE
                        WHEN `brand` = 'DESIGN' THEN '${SectionSeed.DESIGN_FEATURED_PROJECTS_ID}'
                        ELSE '${SectionSeed.TOURISM_BROCHURES_ID}'
                    END
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE `world_map_sections`
                    ADD COLUMN `subtitle` TEXT NOT NULL DEFAULT ''
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE `world_map_sections`
                    ADD COLUMN `countryLabel` TEXT NOT NULL DEFAULT ''
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    ALTER TABLE `world_map_sections`
                    ADD COLUMN `cityLabel` TEXT NOT NULL DEFAULT ''
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `review_cards` (
                        `id` TEXT NOT NULL,
                        `sectionId` TEXT NOT NULL,
                        `reviewerName` TEXT NOT NULL,
                        `sourceType` TEXT NOT NULL,
                        `subHeading` TEXT NOT NULL,
                        `comment` TEXT NOT NULL,
                        `rating` INTEGER NOT NULL,
                        `hidden` INTEGER NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `content_page_cards` (
                        `id` TEXT NOT NULL,
                        `sectionId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `bodyText` TEXT NOT NULL,
                        `imagePath` TEXT NOT NULL,
                        `hidden` INTEGER NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_6_7: Migration = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `art_gallery_heroes` (
                        `id` TEXT NOT NULL,
                        `sectionId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `hidden` INTEGER NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `art_gallery_cards` (
                        `id` TEXT NOT NULL,
                        `heroId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `bodyText` TEXT NOT NULL,
                        `imagePath` TEXT NOT NULL,
                        `hidden` INTEGER NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_7_8: Migration = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `remote_image_assets` (
                        `localPath` TEXT NOT NULL,
                        `provider` TEXT NOT NULL,
                        `providerAssetId` TEXT NOT NULL,
                        `sourcePageUrl` TEXT NOT NULL,
                        `downloadUrl` TEXT NOT NULL,
                        `photographerName` TEXT NOT NULL,
                        `photographerUrl` TEXT NOT NULL,
                        `importedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`localPath`)
                    )
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_8_9: Migration = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE `companies`
                    ADD COLUMN `logoPath` TEXT NOT NULL DEFAULT ''
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    ALTER TABLE `companies`
                    ADD COLUMN `brandSeedColor` TEXT NOT NULL DEFAULT '#6750A4'
                    """.trimIndent(),
                )
            }
        }
    }
}
