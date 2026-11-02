package one.crinklecod.cc.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.crinklecod.cc.ui.theme.CottonCandyPink
import one.crinklecod.cc.ui.theme.SleepyRose

/**
 * Broken parts of the Razr's main display, in landscape with the camera on the left.
 * Tune these after checking the calibration outlines on the real screen.
 */
object DeadZones {
    /** The fold: a vertical strip centred on the screen. */
    val FoldWidth = 160.dp
    /** Strip along the top edge. */
    val TopStrip = 40.dp
    /** Roughly quarter-circle dead patch in the top-right corner. */
    val CornerRadius = 130.dp
    /**
     * How far the mascot/health column may reach past the left half into the fold strip.
     * FoldWidth is generous; on the real panel ~3/4" (~120 dp) beside it is usable.
     */
    val SanctuaryOverhang = 100.dp
}

/** Dashed pink outlines over each dead zone. Draw-only, so touches pass straight through. */
@Composable
fun DeadZoneCalibrationOverlay(modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = SleepyRose, fontSize = 11.sp, fontWeight = FontWeight.Black)
    Canvas(modifier.fillMaxSize()) {
        val fill = CottonCandyPink.copy(alpha = 0.28f)
        val dashed = Stroke(
            width = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))
        )
        val fold = DeadZones.FoldWidth.toPx()
        val top = DeadZones.TopStrip.toPx()
        val r = DeadZones.CornerRadius.toPx()

        val foldRect = Rect(Offset(size.width / 2f - fold / 2f, 0f), Size(fold, size.height))
        val topRect = Rect(Offset.Zero, Size(size.width, top))
        listOf(foldRect, topRect).forEach { rect ->
            drawRect(fill, rect.topLeft, rect.size)
            drawRect(SleepyRose, rect.topLeft, rect.size, style = dashed)
        }
        val corner = Path().apply {
            moveTo(size.width, 0f)
            lineTo(size.width - r, 0f)
            arcTo(Rect(Offset(size.width - r, -r), Size(2 * r, 2 * r)), 180f, -90f, false)
            close()
        }
        drawPath(corner, fill)
        drawPath(corner, SleepyRose, style = dashed)

        drawText(measurer, "fold · ${DeadZones.FoldWidth.value.toInt()}dp", Offset(foldRect.left + 8.dp.toPx(), size.height / 2f), labelStyle)
        drawText(measurer, "top strip · ${DeadZones.TopStrip.value.toInt()}dp", Offset(12.dp.toPx(), top + 4.dp.toPx()), labelStyle)
        drawText(measurer, "corner · ${DeadZones.CornerRadius.value.toInt()}dp", Offset(size.width - r, r + 4.dp.toPx()), labelStyle)
    }
}
