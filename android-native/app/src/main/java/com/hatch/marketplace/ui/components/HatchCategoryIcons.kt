package com.hatch.marketplace.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.hatch.marketplace.data.model.CategoryId

/**
 * Exact Lucide category icons matching `src/components/MarketBrowse.tsx`.
 */
object HatchCategoryIcons {

    fun forCategory(categoryId: CategoryId): ImageVector = when (categoryId) {
        CategoryId.PHONES -> Smartphone
        CategoryId.ELECTRONICS -> Laptop
        CategoryId.VEHICLES -> Car
        CategoryId.FURNITURE -> Sofa
        CategoryId.FASHION -> Shirt
        CategoryId.APPLIANCES -> Wrench
        CategoryId.SOLAR_ENERGY -> SunMedium
        CategoryId.REAL_ESTATE -> Building
        CategoryId.BOOKS_HOBBIES -> Gamepad2
    }

    val All: ImageVector by lazy {
        lucideIcon("HatchCatAll") {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12.586f, 2.586f)
                arcTo(2f, 2f, 0f, false, false, 11.172f, 2f)
                horizontalLineTo(4f)
                arcToRelative(2f, 2f, 0f, false, false, -2f, 2f)
                verticalLineToRelative(7.172f)
                arcToRelative(2f, 2f, 0f, false, false, 0.586f, 1.414f)
                lineToRelative(8.704f, 8.704f)
                arcToRelative(2.426f, 2.426f, 0f, false, false, 3.42f, 0f)
                lineToRelative(6.58f, -6.58f)
                arcToRelative(2.426f, 2.426f, 0f, false, false, 0f, -3.42f)
                close()
            }
            path(
                fill = SolidColor(Color.Black),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 7.5f)
                arcToRelative(0.5f, 0.5f, 0f, true, true, 1f, 0f)
                arcToRelative(0.5f, 0.5f, 0f, true, true, -1f, 0f)
                close()
            }
        }
    }

    val Smartphone: ImageVector by lazy {
        lucideIcon("HatchCatSmartphone") {
            strokePath {
                moveTo(7f, 2f)
                horizontalLineToRelative(10f)
                arcToRelative(2f, 2f, 0f, false, true, 2f, 2f)
                verticalLineToRelative(16f)
                arcToRelative(2f, 2f, 0f, false, true, -2f, 2f)
                horizontalLineToRelative(-10f)
                arcToRelative(2f, 2f, 0f, false, true, -2f, -2f)
                verticalLineToRelative(-16f)
                arcToRelative(2f, 2f, 0f, false, true, 2f, -2f)
                close()
            }
            strokePath {
                moveTo(12f, 18f)
                horizontalLineToRelative(0.01f)
            }
        }
    }

    val SunMedium: ImageVector by lazy {
        lucideIcon("HatchCatSunMedium") {
            strokePath {
                moveTo(8f, 12f)
                arcToRelative(4f, 4f, 0f, true, true, 8f, 0f)
                arcToRelative(4f, 4f, 0f, true, true, -8f, 0f)
                close()
                moveTo(12f, 3f); verticalLineToRelative(1f)
                moveTo(12f, 20f); verticalLineToRelative(1f)
                moveTo(3f, 12f); horizontalLineToRelative(1f)
                moveTo(20f, 12f); horizontalLineToRelative(1f)
                moveTo(18.364f, 5.636f); lineToRelative(-0.707f, 0.707f)
                moveTo(6.343f, 17.657f); lineToRelative(-0.707f, 0.707f)
                moveTo(5.636f, 5.636f); lineToRelative(0.707f, 0.707f)
                moveTo(17.657f, 17.657f); lineToRelative(0.707f, 0.707f)
            }
        }
    }

    val Laptop: ImageVector by lazy {
        lucideIcon("HatchCatLaptop") {
            strokePath {
                moveTo(18f, 5f)
                arcToRelative(2f, 2f, 0f, false, true, 2f, 2f)
                verticalLineToRelative(8.526f)
                arcToRelative(2f, 2f, 0f, false, false, 0.212f, 0.897f)
                lineToRelative(1.068f, 2.127f)
                arcToRelative(1f, 1f, 0f, false, true, -0.9f, 1.45f)
                horizontalLineTo(3.62f)
                arcToRelative(1f, 1f, 0f, false, true, -0.9f, -1.45f)
                lineToRelative(1.068f, -2.127f)
                arcTo(2f, 2f, 0f, false, false, 4f, 15.526f)
                verticalLineTo(7f)
                arcToRelative(2f, 2f, 0f, false, true, 2f, -2f)
                close()
            }
            strokePath {
                moveTo(20.054f, 15.987f)
                horizontalLineTo(3.946f)
            }
        }
    }

    val Car: ImageVector by lazy {
        lucideIcon("HatchCatCar") {
            strokePath {
                moveTo(19f, 17f)
                horizontalLineToRelative(2f)
                curveToRelative(0.6f, 0f, 1f, -0.4f, 1f, -1f)
                verticalLineToRelative(-3f)
                curveToRelative(0f, -0.9f, -0.7f, -1.7f, -1.5f, -1.9f)
                curveTo(18.7f, 10.6f, 16f, 10f, 16f, 10f)
                curveToRelative(0f, 0f, -1.3f, -1.4f, -2.2f, -2.3f)
                curveToRelative(-0.5f, -0.4f, -1.1f, -0.7f, -1.8f, -0.7f)
                horizontalLineTo(5f)
                curveToRelative(-0.6f, 0f, -1.1f, 0.4f, -1.4f, 0.9f)
                lineToRelative(-1.4f, 2.9f)
                arcTo(3.7f, 3.7f, 0f, false, false, 2f, 12f)
                verticalLineToRelative(4f)
                curveToRelative(0f, 0.6f, 0.4f, 1f, 1f, 1f)
                horizontalLineToRelative(2f)
            }
            strokePath {
                moveTo(5f, 17f)
                arcToRelative(2f, 2f, 0f, true, true, 4f, 0f)
                arcToRelative(2f, 2f, 0f, true, true, -4f, 0f)
                close()
            }
            strokePath {
                moveTo(9f, 17f)
                horizontalLineToRelative(6f)
            }
            strokePath {
                moveTo(15f, 17f)
                arcToRelative(2f, 2f, 0f, true, true, 4f, 0f)
                arcToRelative(2f, 2f, 0f, true, true, -4f, 0f)
                close()
            }
        }
    }

    val Shirt: ImageVector by lazy {
        lucideIcon("HatchCatShirt") {
            strokePath {
                moveTo(20.38f, 3.46f)
                lineTo(16f, 2f)
                arcToRelative(4f, 4f, 0f, false, true, -8f, 0f)
                lineTo(3.62f, 3.46f)
                arcToRelative(2f, 2f, 0f, false, false, -1.34f, 2.23f)
                lineToRelative(0.58f, 3.47f)
                arcToRelative(1f, 1f, 0f, false, false, 0.99f, 0.84f)
                horizontalLineTo(6f)
                verticalLineToRelative(10f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(8f)
                arcToRelative(2f, 2f, 0f, false, false, 2f, -2f)
                verticalLineTo(10f)
                horizontalLineToRelative(2.15f)
                arcToRelative(1f, 1f, 0f, false, false, 0.99f, -0.84f)
                lineToRelative(0.58f, -3.47f)
                arcToRelative(2f, 2f, 0f, false, false, -1.34f, -2.23f)
                close()
            }
        }
    }

    val Sofa: ImageVector by lazy {
        lucideIcon("HatchCatSofa") {
            strokePath {
                moveTo(20f, 9f)
                verticalLineTo(6f)
                arcToRelative(2f, 2f, 0f, false, false, -2f, -2f)
                horizontalLineTo(6f)
                arcToRelative(2f, 2f, 0f, false, false, -2f, 2f)
                verticalLineToRelative(3f)
            }
            strokePath {
                moveTo(2f, 16f)
                arcToRelative(2f, 2f, 0f, false, false, 2f, 2f)
                horizontalLineToRelative(16f)
                arcToRelative(2f, 2f, 0f, false, false, 2f, -2f)
                verticalLineToRelative(-5f)
                arcToRelative(2f, 2f, 0f, false, false, -4f, 0f)
                verticalLineToRelative(1.5f)
                arcToRelative(0.5f, 0.5f, 0f, false, true, -0.5f, 0.5f)
                horizontalLineToRelative(-11f)
                arcToRelative(0.5f, 0.5f, 0f, false, true, -0.5f, -0.5f)
                verticalLineTo(11f)
                arcToRelative(2f, 2f, 0f, false, false, -4f, 0f)
                close()
            }
            strokePath { moveTo(4f, 18f); verticalLineToRelative(2f) }
            strokePath { moveTo(20f, 18f); verticalLineToRelative(2f) }
            strokePath { moveTo(12f, 4f); verticalLineToRelative(9f) }
        }
    }

    val Wrench: ImageVector by lazy {
        lucideIcon("HatchCatWrench") {
            strokePath {
                moveTo(14.7f, 6.3f)
                arcToRelative(1f, 1f, 0f, false, false, 0f, 1.4f)
                lineToRelative(1.6f, 1.6f)
                arcToRelative(1f, 1f, 0f, false, false, 1.4f, 0f)
                lineToRelative(3.106f, -3.105f)
                curveToRelative(0.32f, -0.322f, 0.863f, -0.22f, 0.983f, 0.218f)
                arcToRelative(6f, 6f, 0f, false, true, -8.259f, 7.057f)
                lineToRelative(-7.91f, 7.91f)
                arcToRelative(1f, 1f, 0f, false, true, -2.999f, -3f)
                lineToRelative(7.91f, -7.91f)
                arcToRelative(6f, 6f, 0f, false, true, 7.057f, -8.259f)
                curveToRelative(0.438f, 0.12f, 0.54f, 0.662f, 0.219f, 0.984f)
                close()
            }
        }
    }

    val Gamepad2: ImageVector by lazy {
        lucideIcon("HatchCatGamepad2") {
            strokePath {
                moveTo(6f, 11f); lineTo(10f, 11f)
                moveTo(8f, 9f); lineTo(8f, 13f)
                moveTo(15f, 12f); lineTo(15.01f, 12f)
                moveTo(18f, 10f); lineTo(18.01f, 10f)
            }
            strokePath {
                moveTo(17.32f, 5f)
                horizontalLineTo(6.68f)
                arcToRelative(4f, 4f, 0f, false, false, -3.978f, 3.59f)
                curveToRelative(-0.006f, 0.052f, -0.01f, 0.101f, -0.017f, 0.152f)
                curveTo(2.604f, 9.416f, 2f, 14.456f, 2f, 16f)
                arcToRelative(3f, 3f, 0f, false, false, 3f, 3f)
                curveToRelative(1f, 0f, 1.5f, -0.5f, 2f, -1f)
                lineToRelative(1.414f, -1.414f)
                arcTo(2f, 2f, 0f, false, true, 9.828f, 16f)
                horizontalLineToRelative(4.344f)
                arcToRelative(2f, 2f, 0f, false, true, 1.414f, 0.586f)
                lineTo(17f, 18f)
                curveToRelative(0.5f, 0.5f, 1f, 1f, 2f, 1f)
                arcToRelative(3f, 3f, 0f, false, false, 3f, -3f)
                curveToRelative(0f, -1.545f, -0.604f, -6.584f, -0.685f, -7.258f)
                curveToRelative(-0.007f, -0.05f, -0.011f, -0.1f, -0.017f, -0.151f)
                arcTo(4f, 4f, 0f, false, false, 17.32f, 5f)
                close()
            }
        }
    }

    val Building: ImageVector by lazy {
        lucideIcon("HatchCatBuilding") {
            strokePath {
                moveTo(6f, 2f)
                horizontalLineToRelative(12f)
                arcToRelative(2f, 2f, 0f, false, true, 2f, 2f)
                verticalLineToRelative(16f)
                arcToRelative(2f, 2f, 0f, false, true, -2f, 2f)
                horizontalLineToRelative(-12f)
                arcToRelative(2f, 2f, 0f, false, true, -2f, -2f)
                verticalLineToRelative(-16f)
                arcToRelative(2f, 2f, 0f, false, true, 2f, -2f)
                close()
            }
            strokePath {
                moveTo(9f, 22f)
                verticalLineToRelative(-3f)
                arcToRelative(1f, 1f, 0f, false, true, 1f, -1f)
                horizontalLineToRelative(4f)
                arcToRelative(1f, 1f, 0f, false, true, 1f, 1f)
                verticalLineToRelative(3f)
            }
            strokePath {
                moveTo(8f, 6f); horizontalLineToRelative(0.01f)
                moveTo(12f, 6f); horizontalLineToRelative(0.01f)
                moveTo(16f, 6f); horizontalLineToRelative(0.01f)
                moveTo(8f, 10f); horizontalLineToRelative(0.01f)
                moveTo(12f, 10f); horizontalLineToRelative(0.01f)
                moveTo(16f, 10f); horizontalLineToRelative(0.01f)
                moveTo(8f, 14f); horizontalLineToRelative(0.01f)
                moveTo(12f, 14f); horizontalLineToRelative(0.01f)
                moveTo(16f, 14f); horizontalLineToRelative(0.01f)
            }
        }
    }

    private inline fun lucideIcon(
        name: String,
        block: ImageVector.Builder.() -> Unit
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply(block).build()

    private inline fun ImageVector.Builder.strokePath(
        block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit
    ) {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = block
        )
    }
}
