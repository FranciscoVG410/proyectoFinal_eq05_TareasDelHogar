package valdez.francisco.dingdone.graphics

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import valdez.francisco.dingdone.PieSlice

class CustomPieDrawable(
    private val context: Context,
    private val slices: List<PieSlice>,
    private val isCompleted: Boolean // 👈 ahora si recibe el tipo
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

    private val legendTextSize = 42f
    private val legendBoxSize = 40f

    private var oval = RectF()

    override fun draw(canvas: Canvas) {
        val width = bounds.width().toFloat()
        val height = bounds.height().toFloat()
        val minSide = minOf(width, height * 0.7f)
        val radius = minSide - 40

        // Pastel
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

        // Hoyo central
        val holeRadius = radius * 0.40f
        canvas.drawCircle(width / 2, 40f + radius / 2, holeRadius, paintHole)

        // Leyendas
        paintText.textSize = legendTextSize
        var startY = radius + 80f

        for (slice in slices) {
            paintSlice.color = slice.color
            canvas.drawRect(40f, startY, 40f + legendBoxSize, startY + legendBoxSize, paintSlice)

            val typeText = if (isCompleted) "Tasks Completed" else "Unfinished Tasks"
            val label = "${slice.label}  ${slice.count} $typeText"

            canvas.drawText(label, 100f, startY + legendBoxSize * 0.8f, paintText)

            startY += legendBoxSize + 20f
        }
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
