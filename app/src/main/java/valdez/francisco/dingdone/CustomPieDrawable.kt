package valdez.francisco.dingdone.graphics

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable

class CustomPieDrawable(
    val context: Context,
    private val slices: List<PieSlice>
) : Drawable() {

    private val paintSlice = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val paintHole = Paint().apply {
        style = Paint.Style.FILL
        color = Color.WHITE
        isAntiAlias = true
    }

    private val paintText = Paint().apply {
        color = Color.BLACK
        textSize = 40f
        isAntiAlias = true
    }

    private var oval = RectF()

    override fun draw(canvas: Canvas) {
        val width = bounds.width().toFloat()
        val height = bounds.height().toFloat()
        val minSide = minOf(width, height * 0.7f)
        val radius = minSide - 40

        oval = RectF(
            (width - radius) / 2,
            40f,
            (width + radius) / 2,
            40f + radius
        )

        var startAngle = 0f
        for (slice in slices) {
            paintSlice.color = slice.color
            canvas.drawArc(oval, startAngle, slice.angle, true, paintSlice)
            startAngle += slice.angle
        }

        val holeRadius = radius * 0.40f
        canvas.drawCircle(width / 2, 40f + radius / 2, holeRadius, paintHole)

        val legendStartY = 60f + radius
        val legendBoxSize = 40f
        var currentY = legendStartY

        for (slice in slices) {
            paintSlice.color = slice.color
            canvas.drawRect(40f, currentY, 40f + legendBoxSize, currentY + legendBoxSize, paintSlice)

            val labelWithCount = "${slice.label} ${slice.count} Tasks Completed"
            canvas.drawText(labelWithCount, 100f, currentY + legendBoxSize * 0.8f, paintText)
            currentY += legendBoxSize + 20f
        }
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    override fun getOpacity(): Int = PixelFormat.OPAQUE
}

data class PieSlice(
    val label: String,
    val angle: Float,
    val color: Int,
    val count: Int
)
