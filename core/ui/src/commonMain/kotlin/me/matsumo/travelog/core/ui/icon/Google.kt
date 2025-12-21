package me.matsumo.zencall.core.ui.icon

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Filled.Google: ImageVector
    get() {
        if (icon != null) return icon!!

        icon = ImageVector.Builder(
            name = "googleIconLogoSvgrepoCom",
            defaultWidth = 800.dp,
            defaultHeight = 800.dp,
            viewportWidth = 262f,
            viewportHeight = 262f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF4285F4))
            ) {
                moveTo(258.878f, 133.451f)
                curveToRelative(0f, -10.734f, -0.871f, -18.567f, -2.756f, -26.69f)
                horizontalLineTo(133.55f)
                verticalLineToRelative(48.448f)
                horizontalLineToRelative(71.947f)
                curveToRelative(-1.45f, 12.04f, -9.283f, 30.172f, -26.69f, 42.356f)
                lineToRelative(-0.244f, 1.622f)
                lineToRelative(38.755f, 30.023f)
                lineToRelative(2.685f, 0.268f)
                curveToRelative(24.659f, -22.774f, 38.875f, -56.282f, 38.875f, -96.027f)
            }
            path(
                fill = SolidColor(Color(0xFF34A853))
            ) {
                moveTo(133.55f, 261.1f)
                curveToRelative(35.248f, 0f, 64.839f, -11.605f, 86.453f, -31.622f)
                lineToRelative(-41.196f, -31.913f)
                curveToRelative(-11.024f, 7.688f, -25.82f, 13.055f, -45.257f, 13.055f)
                curveToRelative(-34.523f, 0f, -63.824f, -22.773f, -74.269f, -54.25f)
                lineToRelative(-1.531f, 0.13f)
                lineToRelative(-40.298f, 31.187f)
                lineToRelative(-0.527f, 1.465f)
                curveTo(38.393f, 231.798f, 82.49f, 261.1f, 133.55f, 261.1f)
            }
            path(
                fill = SolidColor(Color(0xFFFBBC05))
            ) {
                moveTo(59.281f, 156.37f)
                curveToRelative(-2.756f, -8.123f, -4.351f, -16.827f, -4.351f, -25.82f)
                curveToRelative(0f, -8.994f, 1.595f, -17.697f, 4.206f, -25.82f)
                lineToRelative(-0.073f, -1.73f)
                lineTo(18.259999999999998f, 71.312f)
                lineToRelative(-1.335f, 0.635f)
                curveTo(8.077f, 89.644f, 3f, 109.517f, 3f, 130.55f)
                reflectiveCurveToRelative(5.077f, 40.905f, 13.925f, 58.602f)
                lineToRelative(42.356f, -32.782f)
            }
            path(
                fill = SolidColor(Color(0xFFEB4335))
            ) {
                moveTo(133.55f, 50.479f)
                curveToRelative(24.514f, 0f, 41.05f, 10.589f, 50.479f, 19.438f)
                lineToRelative(36.844f, -35.974f)
                curveTo(198.245f, 12.91f, 168.798f, 0f, 133.55f, 0f)
                curveTo(82.49f, 0f, 38.393f, 29.301f, 16.925f, 71.947f)
                lineToRelative(42.211f, 32.783f)
                curveToRelative(10.59f, -31.477f, 39.891f, -54.251f, 74.414f, -54.251f)
            }
        }.build()

        return icon!!
    }

private var icon: ImageVector? = null

