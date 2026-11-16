package br.edu.unirv.lineup.ui.screens.lineup

import android.graphics.Paint
import android.text.TextPaint
import android.text.TextUtils
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.edu.unirv.lineup.data.model.Berth
import br.edu.unirv.lineup.data.model.ShipSchedule
import br.edu.unirv.lineup.data.model.TideTable
import br.edu.unirv.lineup.ui.theme.DangerZoneRed
import br.edu.unirv.lineup.ui.theme.HighTideGreen
import br.edu.unirv.lineup.ui.theme.OceanPrimary
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class DragMode { MOVE, RESIZE_START, RESIZE_END }

/** Formata o dia sem casas decimais quando inteiro (3.0 -> "3", 3.5 -> "3,5"). */
private fun formatDay(day: Double): String =
    if (day % 1.0 == 0.0) day.toInt().toString() else "%.1f".format(day).replace('.', ',')

/**
 * Linha do tempo de 30 dias de um berço (RF003).
 *
 * Cada navio é desenhado como uma embarcação sobre a linha d'água, com o nome e a
 * janela ("MV Atlantic Star 42 (Dia 3–8)") e as cargas de descarga/embarque acima do casco.
 *
 * Responsivo: a largura de cada dia é derivada da largura disponível (celular ≈ 9 dias
 * visíveis, tablet ≈ 16, telas largas = mês inteiro) e todas as medidas usam dp/sp, de modo
 * que a altura acompanha a escala de fonte do sistema.
 */
@Composable
fun BerthTimelineCanvas(
    berth: Berth,
    activeTide: TideTable?,
    onShipClick: (ShipSchedule) -> Unit,
    onMoveOrResize: (scheduleId: Long, newStart: Double, newEnd: Double) -> Boolean,
    onConflictWarning: (String) -> Unit,
    modifier: Modifier = Modifier,
    dayWidthOverride: Dp? = null
) {
  BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
    val density = LocalDensity.current
    val visibleDays = when {
        maxWidth < 600.dp -> 9
        maxWidth < 840.dp -> 16
        else -> 30
    }
    val dayWidthDp = dayWidthOverride ?: (maxWidth / visibleDays).coerceIn(30.dp, 72.dp)
    val dayWidthPx = with(density) { dayWidthDp.toPx() }
    val totalWidthDp = dayWidthDp * 30
    val totalWidthPx = dayWidthPx * 30

    // Métricas verticais (px) derivadas de dp/sp.
    val titleSizePx = with(density) { 10.sp.toPx() }
    val subSizePx = with(density) { 8.5.sp.toPx() }
    val axisSizePx = with(density) { 9.sp.toPx() }
    val dp1 = with(density) { 1.dp.toPx() }
    val tierH = titleSizePx * 1.25f + subSizePx * 1.3f + 2 * dp1
    val labelsTop = 4 * dp1
    val mastTop = labelsTop + 2 * tierH
    val mastH = 7 * dp1
    val cabinH = 8 * dp1
    val hullH = 12 * dp1
    val deckY = mastTop + mastH + cabinH
    val waterY = deckY + hullH - 3 * dp1
    val axisTop = deckY + hullH + 4 * dp1
    val canvasHeightPx = axisTop + axisSizePx + 8 * dp1
    val canvasHeightDp = with(density) { canvasHeightPx.toDp() }
    val handleZonePx = 20 * dp1

    // Estado local durante o arraste interativo
    var draggingShipId by remember { mutableStateOf<Long?>(null) }
    var dragStartDay by remember { mutableDoubleStateOf(0.0) }
    var dragEndDay by remember { mutableDoubleStateOf(0.0) }
    var isDragValid by remember { mutableStateOf(true) }

    val handleShipClick by rememberUpdatedState(onShipClick)
    val commitDrag by rememberUpdatedState(onMoveOrResize)
    val showWarning by rememberUpdatedState(onConflictWarning)

    val labelColor = MaterialTheme.colorScheme.onSurface
    val mutedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
    val cabinColor = MaterialTheme.colorScheme.surface
    val titlePaint = remember(titleSizePx, labelColor) {
        TextPaint().apply {
            color = labelColor.toArgb(); textSize = titleSizePx
            typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true; textAlign = Paint.Align.LEFT
        }
    }
    val subPaint = remember(subSizePx, mutedColor) {
        TextPaint().apply {
            color = mutedColor.toArgb(); textSize = subSizePx
            isAntiAlias = true; textAlign = Paint.Align.LEFT
        }
    }
    val axisPaint = remember(axisSizePx) {
        Paint().apply {
            textSize = axisSizePx; isAntiAlias = true; textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
    }
    val lowTideDays = activeTide?.lowTideDays ?: emptySet()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(canvasHeightDp)
            .horizontalScroll(rememberScrollState())
    ) {
        Canvas(
            modifier = Modifier
                .width(totalWidthDp)
                .fillMaxHeight()
                .pointerInput(berth.schedules, dayWidthPx, axisTop) {
                    detectTapGestures { offset ->
                        val day = offset.x / dayWidthPx + 1.0
                        if (offset.y <= axisTop) {
                            berth.schedules.firstOrNull { day >= it.startDay && day < it.endDay }
                                ?.let(handleShipClick)
                        }
                    }
                }
                .pointerInput(berth.schedules, activeTide, dayWidthPx, axisTop) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        if (down.position.y > axisTop) {
                            return@awaitEachGesture
                        }
                        val day = down.position.x / dayWidthPx + 1.0
                        val ship = berth.schedules.firstOrNull { day >= it.startDay && day < it.endDay }
                            ?: return@awaitEachGesture
                        val duration = ship.endDay - ship.startDay
                        // Zona de toque das bordas em dp (independe da densidade/zoom).
                        val handleWidth = minOf((handleZonePx / dayWidthPx).toDouble(), duration / 3)
                        val mode = when {
                            day - ship.startDay <= handleWidth -> DragMode.RESIZE_START
                            ship.endDay - day <= handleWidth -> DragMode.RESIZE_END
                            else -> DragMode.MOVE
                        }

                        // A rolagem horizontal fica disponível nos espaços sem navios.
                        val firstDrag = awaitHorizontalTouchSlopOrCancellation(down.id) { change, _ ->
                            change.consume()
                        } ?: return@awaitEachGesture

                        var distance = firstDrag.position.x - down.position.x
                        val minDuration = minOf(0.5, duration)
                        fun updatePreview() {
                            val delta = (distance / dayWidthPx * 2).roundToInt() / 2.0
                            dragStartDay = ship.startDay
                            dragEndDay = ship.endDay
                            when (mode) {
                                DragMode.MOVE -> {
                                    dragStartDay = (ship.startDay + delta).coerceIn(1.0, 30.0 - duration)
                                    dragEndDay = dragStartDay + duration
                                }
                                DragMode.RESIZE_START -> {
                                    dragStartDay = (ship.startDay + delta).coerceIn(1.0, ship.endDay - minDuration)
                                }
                                DragMode.RESIZE_END -> {
                                    dragEndDay = (ship.endDay + delta).coerceIn(ship.startDay + minDuration, 30.0)
                                }
                            }
                            isDragValid = !berth.hasOverlap(dragStartDay, dragEndDay, ship.id) &&
                                activeTide?.findFirstLowTideDayInRange(dragStartDay, dragEndDay) == null
                        }

                        draggingShipId = ship.id
                        try {
                            updatePreview()
                            val completed = horizontalDrag(firstDrag.id) { change ->
                                distance += change.positionChange().x
                                change.consume()
                                updatePreview()
                            }
                            if (completed && (dragStartDay != ship.startDay || dragEndDay != ship.endDay)) {
                                val lowDay = activeTide?.findFirstLowTideDayInRange(dragStartDay, dragEndDay)
                                when {
                                    berth.hasOverlap(dragStartDay, dragEndDay, ship.id) ->
                                        showWarning("RN001: Colisão com outro navio neste berço.")
                                    lowDay != null ->
                                        showWarning("RN002: Bloqueio de maré baixa no dia %02d.".format(lowDay))
                                    !commitDrag(ship.id, dragStartDay, dragEndDay) ->
                                        showWarning("Não foi possível atualizar a atracação.")
                                }
                            }
                        } finally {
                            draggingShipId = null
                        }
                    }
                }
        ) {
            val native = drawContext.canvas.nativeCanvas
            val lowTideArgb = DangerZoneRed.toArgb()
            val axisArgb = mutedColor.toArgb()

            // 1. Grade de 30 dias, sombreamento de maré baixa (RN002) e eixo de dias na base
            for (day in 1..30) {
                val x = (day - 1) * dayWidthPx
                val isLowTide = lowTideDays.contains(day)

                if (isLowTide) {
                    drawRect(
                        color = DangerZoneRed.copy(alpha = 0.14f),
                        topLeft = Offset(x, 0f),
                        size = Size(dayWidthPx, axisTop)
                    )
                    drawLine(
                        color = DangerZoneRed.copy(alpha = 0.4f),
                        start = Offset(x, 0f),
                        end = Offset(x, axisTop),
                        strokeWidth = 1.5f * dp1,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4 * dp1, 3 * dp1), 0f)
                    )
                }

                drawLine(
                    color = Color.Gray.copy(alpha = 0.18f),
                    start = Offset(x, 0f),
                    end = Offset(x, axisTop),
                    strokeWidth = dp1 * 0.5f
                )

                axisPaint.color = if (isLowTide) lowTideArgb else axisArgb
                native.drawText("%02d".format(day), x + dayWidthPx / 2f, size.height - 4 * dp1, axisPaint)
            }
            // Linha do eixo
            drawLine(
                color = Color.Gray.copy(alpha = 0.35f),
                start = Offset(0f, axisTop),
                end = Offset(totalWidthPx, axisTop),
                strokeWidth = dp1 * 0.75f
            )

            // 2. Navios: embarcação sobre a linha d'água + rótulo acima do casco
            val labelGap = 6 * dp1
            // Borda direita já ocupada em cada faixa de rótulo (0 = junto ao navio, 1 = acima)
            val tierRight = floatArrayOf(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY)

            berth.schedules.sortedBy { it.startDay }.forEach { ship ->
                val isBeingDragged = (ship.id == draggingShipId)
                val activeStart = if (isBeingDragged) dragStartDay else ship.startDay
                val activeEnd = if (isBeingDragged) dragEndDay else ship.endDay

                val leftX = (activeStart - 1.0).toFloat() * dayWidthPx + dp1
                val rightX = (activeEnd - 1.0).toFloat() * dayWidthPx - dp1
                val shipWidth = (rightX - leftX).coerceAtLeast(6 * dp1)
                val centerX = leftX + shipWidth / 2f

                val baseColorHex = ship.stripeColorHex.ifBlank { ShipSchedule.colorForStatus(ship.status) }
                val hullColor = try {
                    Color(android.graphics.Color.parseColor(baseColorHex))
                } catch (e: Exception) {
                    OceanPrimary
                }
                val effectiveColor = if (isBeingDragged) {
                    if (isDragValid) HighTideGreen else DangerZoneRed
                } else {
                    hullColor
                }

                // Casco: convés largo, fundo mais estreito (proa à direita mais alongada)
                val hullBottom = deckY + hullH
                val sternSlant = minOf(5 * dp1, shipWidth * 0.12f)
                val bowSlant = minOf(12 * dp1, shipWidth * 0.28f)
                val hullPath = Path().apply {
                    moveTo(leftX, deckY)
                    lineTo(leftX + shipWidth, deckY)
                    lineTo(leftX + shipWidth - bowSlant, hullBottom)
                    lineTo(leftX + sternSlant, hullBottom)
                    close()
                }
                drawPath(hullPath, color = effectiveColor.copy(alpha = if (isBeingDragged) 0.95f else 1f))
                drawPath(
                    hullPath,
                    color = labelColor.copy(alpha = 0.55f),
                    style = Stroke(width = if (isBeingDragged) 2f * dp1 else dp1)
                )

                // Superestrutura (cabine + janelas + mastro), proporcional ao comprimento
                val cabinW = minOf(shipWidth * 0.5f, 34 * dp1)
                if (cabinW >= 8 * dp1) {
                    val cabinLeft = centerX - cabinW / 2f
                    drawRoundRect(
                        color = cabinColor,
                        topLeft = Offset(cabinLeft, deckY - cabinH),
                        size = Size(cabinW, cabinH),
                        cornerRadius = CornerRadius(1.5f * dp1)
                    )
                    drawRoundRect(
                        color = labelColor.copy(alpha = 0.55f),
                        topLeft = Offset(cabinLeft, deckY - cabinH),
                        size = Size(cabinW, cabinH),
                        cornerRadius = CornerRadius(1.5f * dp1),
                        style = Stroke(width = dp1)
                    )
                    val windows = (cabinW / (7 * dp1)).toInt().coerceIn(1, 4)
                    for (w in 1..windows) {
                        drawCircle(
                            color = labelColor.copy(alpha = 0.6f),
                            radius = dp1,
                            center = Offset(cabinLeft + cabinW * w / (windows + 1), deckY - cabinH / 2f)
                        )
                    }
                    val mastColor = labelColor.copy(alpha = 0.6f)
                    drawLine(mastColor, Offset(centerX, deckY - cabinH), Offset(centerX, mastTop + dp1), strokeWidth = dp1)
                    drawLine(
                        mastColor,
                        Offset(centerX - 4 * dp1, mastTop + 3 * dp1),
                        Offset(centerX + 4 * dp1, mastTop + 3 * dp1),
                        strokeWidth = dp1
                    )
                }

                // Alças de redimensionamento (início e fim) no convés
                val handleY = deckY + hullH * 0.4f
                listOf(leftX + sternSlant / 2f + 2 * dp1, leftX + shipWidth - bowSlant / 2f - 2 * dp1).forEach { hx ->
                    drawCircle(color = Color.White, radius = 2.4f * dp1, center = Offset(hx, handleY))
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.35f),
                        radius = 2.4f * dp1,
                        center = Offset(hx, handleY),
                        style = Stroke(width = dp1 * 0.6f)
                    )
                }

                // Rótulo: "Nome (Dia a–b)" e "↓ descarga  ↑ embarque", centralizado sobre o navio
                val maxLabelW = maxOf(shipWidth, dayWidthPx * 4.5f).coerceAtMost(totalWidthPx)
                val title = TextUtils.ellipsize(
                    "${ship.shipName} (Dia ${formatDay(activeStart)}–${formatDay(activeEnd)})",
                    titlePaint, maxLabelW, TextUtils.TruncateAt.END
                ).toString()
                val subtitle = TextUtils.ellipsize(
                    "↓ ${ship.unloadingProduct}  ↑ ${ship.loadingProduct}",
                    subPaint, maxLabelW, TextUtils.TruncateAt.END
                ).toString()
                val titleW = titlePaint.measureText(title)
                val subW = subPaint.measureText(subtitle)
                val labelW = maxOf(titleW, subW)
                val labelLeft = (centerX - labelW / 2f).coerceIn(0f, (totalWidthPx - labelW).coerceAtLeast(0f))
                // Faixa junto ao navio por padrão; sobe uma faixa se colidir com o rótulo anterior
                val tier = if (labelLeft >= tierRight[0] + labelGap) 0 else 1
                tierRight[tier] = labelLeft + labelW
                val tierTop = labelsTop + (1 - tier) * tierH
                val labelCenter = labelLeft + labelW / 2f
                titlePaint.color = (if (isBeingDragged) effectiveColor else labelColor).toArgb()
                native.drawText(title, labelCenter - titleW / 2f, tierTop + titleSizePx, titlePaint)
                native.drawText(
                    subtitle,
                    labelCenter - subW / 2f,
                    tierTop + titleSizePx * 1.25f + subSizePx,
                    subPaint
                )
            }

            // 3. Curva senoidal da maré (linha d'água), desenhada sobre a base dos cascos
            val tidePath = Path()
            val amplitude = 2.2f * dp1
            val step = 3 * dp1
            var px = 0f
            while (px <= totalWidthPx) {
                val dayFrac = px / dayWidthPx
                val py = waterY + (sin(dayFrac * PI * 1.6) * amplitude).toFloat()
                if (px == 0f) tidePath.moveTo(px, py) else tidePath.lineTo(px, py)
                px += step
            }
            drawPath(
                path = tidePath,
                color = OceanPrimary,
                style = Stroke(width = 2f * dp1)
            )
        }
    }
  }
}
