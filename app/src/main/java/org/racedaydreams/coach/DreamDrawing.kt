package org.racedaydreams.coach

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import org.racedaydreams.domain.support.DreamRoutes
import java.util.Locale

/** Designed offline overview. The retained course/frame and equivalent section stay authoritative. */
@Composable fun DreamDrawing(route: DreamRoutes.Route, start: Double, finish: Double) {
    val dark = isSystemInDarkTheme()
    val map = rememberDreamBasemap(route.id())
    val prepared = rememberDreamCoursePaths(route, start, finish)
    val paper = if (dark) Color(0xff142C39) else Color(0xffF6F5EC)
    val paperEnd = if (dark) Color(0xff1A333F) else Color(0xffF0F4EA)
    val green = if (dark) Color(0xff28483F) else Color(0xffDCEACF)
    val water = if (dark) Color(0xff2D5973) else Color(0xffA7D9ED)
    val road = if (dark) Color(0xff66808C).copy(alpha = .32f) else Color.White.copy(alpha = .70f)
    val remaining = if (dark) Color(0xff839AA6) else Color(0xff7D909C)
    val halo = if (dark) Color(0xff1B3543) else Color.White
    val accent = if (dark) Color(0xff80BFFF) else Color(0xff087BFF)
    val ink = if (dark) Color(0xffECF5FF) else Color(0xff173C61)
    val labelPaper = if (dark) Color(0xff213E50) else Color(0xffFCFEFF)
    val labelBorder = if (dark) Color(0xff7999B2) else Color(0xffA7BDD0)
    val startColour = if (dark) Color(0xff57D4AB) else Color(0xff059467)
    val finishColour = if (dark) Color(0xffFF8FA7) else Color(0xffE95076)
    val shownStart = prepared?.startMetres ?: start
    val shownFinish = prepared?.finishMetres ?: finish
    val startLabel = String.format(Locale.UK, "%.2f", shownStart.coerceIn(0.0, 42195.0) / 1000)
    val finishLabel = String.format(Locale.UK, "%.2f", shownFinish.coerceIn(0.0, 42195.0) / 1000)
    val exactSectionReady = prepared?.let { it.startMetres == start && it.finishMetres == finish } == true
    val landmarkSummary = prepared?.base?.landmarks?.takeIf { it.isNotEmpty() }
        ?.joinToString { it.label() }?.let {
            (if (route.id().contains("-geo-")) "Geographic landmarks: " else "Stylised city context: ") + it + ". "
        }.orEmpty()

    // The frozen source extracts cover this exact 5:4 course-plus-landmark viewport.
    // Outer poster/card owns its clipping and padding; this canvas joins the city hero.
    Canvas(Modifier.fillMaxWidth().aspectRatio(1.25f).background(paper).semantics {
        stateDescription = if (map != null && exactSectionReady) "Offline city context ready" else "Course overview"
        contentDescription = "Dream Route " + route.name() +
            ". Full marathon course in muted grey; strong blue marks the equivalent section from " +
            startLabel + " to " + finishLabel + " km. Start and Finish pins mark the full course, not your physical location. " +
            (if (finish > start) "A hollow blue ring marks your section start; a checked blue ring marks its finish. "
                else "A hollow blue ring marks the start of your equivalent section. ") +
            "Five kilometre course markers. " + landmarkSummary + "Offline geographic context; not navigation."
    }) {
        if (size.minDimension <= 0f) return@Canvas
        drawRect(Brush.verticalGradient(listOf(paper, paperEnd)))
        val data = prepared ?: return@Canvas
        val base = data.base
        val bounds = base.bounds
        val scale = minOf(size.width / (bounds.maxX - bounds.minX), size.height / (bounds.maxY - bounds.minY))
        val origin = Offset(((size.width - (bounds.maxX - bounds.minX) * scale) / 2).toFloat(),
            ((size.height - (bounds.maxY - bounds.minY) * scale) / 2).toFloat())
        fun p(vertex: DreamRoutes.Vertex) = origin + Offset(
            ((vertex.x() - bounds.minX) * scale).toFloat(), ((vertex.y() - bounds.minY) * scale).toFloat())
        fun inCourseFrame(block: DrawScope.() -> Unit) = withTransform({
            translate(origin.x - (bounds.minX * scale).toFloat(), origin.y - (bounds.minY * scale).toFloat())
            scale(scale.toFloat(), scale.toFloat(), Offset.Zero)
        }, block)
        fun line(width: Float) = Stroke(width / scale.toFloat(), cap = StrokeCap.Round, join = StrokeJoin.Round)

        clipRect {
            inCourseFrame {
                val context = map
                if (context != null) {
                    drawPath(context.green, green)
                    drawPath(context.water, water)
                    drawPath(context.roads, road, style = line(.70.dp.toPx()))
                } else {
                    // Legacy drawings remain in their own frame. No new geography is invented.
                    base.rivers.forEach { drawPath(it, water, style = line(16.dp.toPx())) }
                }
                drawPath(base.course, halo.copy(alpha = .90f), style = line(5.2.dp.toPx()))
                drawPath(base.course, remaining.copy(alpha = .84f), style = line(2.5.dp.toPx()))
            }

            val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = ink.toArgb()
                textSize = 9.5.dp.toPx()
                typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
            }
            val contextPaint = android.graphics.Paint(textPaint).apply {
                color = (if (dark) Color(0xffBCD1DE) else Color(0xff4D697D)).toArgb()
                textSize = 9.dp.toPx()
                typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
            }
            val occupied = mutableListOf<Rect>()
            val sectionStart = p(data.sectionStart)
            val sectionFinish = p(data.sectionFinish)
            val cueRadius = 9.dp.toPx()
            occupied.add(Rect(sectionStart - Offset(cueRadius, cueRadius), Size(cueRadius * 2, cueRadius * 2)))
            occupied.add(Rect(sectionFinish - Offset(cueRadius, cueRadius), Size(cueRadius * 2, cueRadius * 2)))

            // Reserve the actual blue corridor when placing pills. This is visual-only
            // collision work over the retained section; it never changes coordinates.
            val protectedHighlight = data.sectionVertices.zipWithNext { a, b ->
                val first = p(a); val second = p(b)
                Rect(minOf(first.x, second.x), minOf(first.y, second.y),
                    maxOf(first.x, second.x), maxOf(first.y, second.y)).inflate(4.dp.toPx())
            }
            fun label(text: String, anchor: Offset, preferredSide: Int = 0, required: Boolean = false, context: Boolean = false) {
                val paint = if (context) contextPaint else textPaint
                val horizontal = 5.dp.toPx()
                val vertical = 2.5.dp.toPx()
                val metrics = paint.fontMetrics
                val width = paint.measureText(text) + horizontal * 2
                val height = metrics.descent - metrics.ascent + vertical * 2
                val gap = if (required) 10.dp.toPx() else 7.dp.toPx()
                val right = listOf(Offset(anchor.x + gap, anchor.y - height - gap),
                    Offset(anchor.x + gap, anchor.y + gap))
                val left = listOf(Offset(anchor.x - width - gap, anchor.y - height - gap),
                    Offset(anchor.x - width - gap, anchor.y + gap))
                val candidates = (if (preferredSide < 0) left + right else right + left) + listOf(
                    Offset(anchor.x - width / 2, anchor.y - height - gap * 2),
                    Offset(anchor.x - width / 2, anchor.y + gap * 2),
                    Offset(anchor.x + gap * 2, anchor.y - height / 2),
                    Offset(anchor.x - width - gap * 2, anchor.y - height / 2),
                    Offset(anchor.x + gap * 3, anchor.y - height - gap * 3),
                    Offset(anchor.x - width - gap * 3, anchor.y - height - gap * 3))
                val edgeCandidates = if (required) listOf(
                    Offset(8.dp.toPx(), 8.dp.toPx()), Offset(size.width - width - 8.dp.toPx(), 8.dp.toPx()),
                    Offset(8.dp.toPx(), size.height - height - 8.dp.toPx()),
                    Offset(size.width - width - 8.dp.toPx(), size.height - height - 8.dp.toPx())) else emptyList()
                val rect = (candidates + edgeCandidates).map { Rect(it, Size(width, height)) }.firstOrNull { r ->
                    r.left >= 3.dp.toPx() && r.top >= 3.dp.toPx() &&
                        r.right <= size.width - 3.dp.toPx() && r.bottom <= size.height - 3.dp.toPx() &&
                        occupied.none { it.overlaps(r.inflate(2.dp.toPx())) } &&
                        protectedHighlight.none { it.overlaps(r) }
                } ?: return
                occupied.add(rect)
                val nearest = Offset(anchor.x.coerceIn(rect.left, rect.right), anchor.y.coerceIn(rect.top, rect.bottom))
                if ((nearest - anchor).getDistance() > 2.dp.toPx())
                    drawLine(labelBorder.copy(alpha = .65f), anchor, nearest, .8.dp.toPx())
                drawRoundRect(labelPaper.copy(alpha = if (context) .85f else 1f), rect.topLeft, rect.size,
                    CornerRadius(if (context) 3.dp.toPx() else 6.dp.toPx()))
                if (!context) drawRoundRect(labelBorder, rect.topLeft, rect.size, CornerRadius(6.dp.toPx()), style = Stroke(.7.dp.toPx()))
                drawContext.canvas.nativeCanvas.drawText(text, rect.left + horizontal,
                    rect.top + vertical - metrics.ascent, paint)
            }

            val courseStart = p(base.start)
            val courseFinish = p(base.finish)
            val closeEndpoints = (courseFinish - courseStart).getDistance() < 28.dp.toPx()
            fun pin(anchor: Offset, colour: Color, shift: Float, finishPin: Boolean): Offset {
                val head = anchor + Offset(shift, -14.dp.toPx())
                val radius = 6.dp.toPx()
                drawLine(halo, anchor, head, 4.dp.toPx(), StrokeCap.Round)
                drawLine(colour, anchor, head, 2.dp.toPx(), StrokeCap.Round)
                drawCircle(halo, radius + 2.dp.toPx(), head)
                drawCircle(colour, radius, head)
                if (finishPin) {
                    val square = 2.5.dp.toPx()
                    for (row in 0..1) for (column in 0..1) {
                        if ((row + column) % 2 == 0) drawRect(Color.White,
                            head + Offset((column - 1) * square, (row - 1) * square), Size(square, square))
                    }
                } else drawCircle(Color.White, 2.2.dp.toPx(), head)
                occupied.add(Rect(head - Offset(radius + 2.dp.toPx(), radius + 2.dp.toPx()),
                    Size((radius + 2.dp.toPx()) * 2, (radius + 2.dp.toPx()) * 2)))
                return head
            }
            val startHead = pin(courseStart, startColour, if (closeEndpoints) -10.dp.toPx() else 0f, false)
            val finishHead = pin(courseFinish, finishColour, if (closeEndpoints) 10.dp.toPx() else 0f, true)
            label("Start", startHead, preferredSide = -1, required = true)
            label("Finish", finishHead, preferredSide = 1, required = true)

            for (marker in base.kilometres) {
                val point = p(marker.position)
                drawCircle(halo, 3.dp.toPx(), point)
                drawCircle(remaining, 1.7.dp.toPx(), point)
                label(marker.kilometre.toString() + "K", point)
            }

            // Sourced landmark positions restore geographic identity without icon
            // clutter. They yield to the course pins, kilometre pills and blue section.
            for (landmark in base.landmarks) {
                label(landmark.label(), p(DreamRoutes.Vertex(landmark.x(), landmark.y())), context = true)
            }

            // The equivalent section is the strongest map information and sits above
            // context/markers. Label placement explicitly avoids its blue corridor.
            inCourseFrame {
                drawPath(data.highlighted, accent.copy(alpha = .16f), style = line(13.dp.toPx()))
                drawPath(data.highlighted, halo, style = line(8.2.dp.toPx()))
                drawPath(data.highlighted, accent, style = line(5.8.dp.toPx()))
            }
            drawCircle(halo, 7.dp.toPx(), sectionStart)
            drawCircle(accent, 5.2.dp.toPx(), sectionStart)
            drawCircle(halo, 2.4.dp.toPx(), sectionStart)
            if (finish > start) {
                drawCircle(halo, 7.dp.toPx(), sectionFinish)
                drawCircle(accent, 5.5.dp.toPx(), sectionFinish)
                val check = 1.7.dp.toPx()
                drawLine(halo, sectionFinish + Offset(-check * 1.5f, 0f),
                    sectionFinish + Offset(-check * .2f, check * 1.2f), 1.7.dp.toPx(), StrokeCap.Round)
                drawLine(halo, sectionFinish + Offset(-check * .2f, check * 1.2f),
                    sectionFinish + Offset(check * 1.7f, -check * 1.2f), 1.7.dp.toPx(), StrokeCap.Round)
            }
        }
    }
}
