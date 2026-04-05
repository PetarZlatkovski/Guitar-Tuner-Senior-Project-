package com.example.myapplication

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.toColorInt
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin


class TuningMeterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var cents = 0f
    private var active = false

    private val arcStartAngle = 180f
    private val arcSweepAngle = 180f

    private val arcPaint     = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tickPaint    = Paint(Paint.ANTI_ALIAS_FLAG)
    private val needlePaint  = Paint(Paint.ANTI_ALIAS_FLAG)
    private val centreDotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint   = Paint(Paint.ANTI_ALIAS_FLAG)
    private val arcRect      = RectF()
    private val needlePath   = Path()

    init {
        arcPaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            color = Color.LTGRAY
        }
        tickPaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = Color.DKGRAY
        }
        needlePaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = 5f
            strokeCap = Paint.Cap.ROUND
        }
        centreDotPaint.apply {
            style = Paint.Style.FILL
            color = Color.DKGRAY
        }
        labelPaint.apply {
            textSize = 28f
            textAlign = Paint.Align.CENTER
            color = Color.DKGRAY
        }
    }

    fun setCents(newCents: Float?) {
        active = newCents != null
        cents  = newCents?.coerceIn(-50f, 50f) ?: 0f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()


        val cx = w / 2f
        val cy = h * 0.9f
        val radius = (w * 0.38f).coerceAtMost(cy * 0.95f)

        // Arc bounding box
        arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius)
        canvas.drawArc(arcRect, arcStartAngle, arcSweepAngle, false, arcPaint)

        // Tick marks at -50, -25, 0, +25, +50 cents
        drawTicks(canvas, cx, cy, radius)

        // Labels
        canvas.drawText("♭", cx - radius * 0.85f, cy + 36f, labelPaint)
        canvas.drawText("♯", cx + radius * 0.85f, cy + 36f, labelPaint)

        // Centre dot (pivot point)
        canvas.drawCircle(cx, cy, 10f, centreDotPaint)

        // Needle
        val needleAngle = centsToAngle(cents)
        val needleLength = radius * 0.88f
        val radians = Math.toRadians(needleAngle.toDouble())
        val tipX = (cx + needleLength * cos(radians)).toFloat()
        val tipY = (cy + needleLength * sin(radians)).toFloat()

        needlePaint.color = when {
            !active                        -> Color.LTGRAY
            abs(cents) < 5f           -> "#4CAF50".toColorInt()  // green — in tune
            abs(cents) < 20f          -> "#FF9800".toColorInt()  // amber — close
            else                           -> "#F44336".toColorInt()  // red — out of tune
        }

        needlePath.reset()
        needlePath.moveTo(cx, cy)
        needlePath.lineTo(tipX, tipY)
        canvas.drawPath(needlePath, needlePaint)
    }

    private fun drawTicks(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        // Five ticks: -50, -25, 0, +25, +50 cents
        val tickCents = floatArrayOf(-50f, -25f, 0f, 25f, 50f)
        for (c in tickCents) {
            val angle   = Math.toRadians(centsToAngle(c).toDouble())
            val innerR  = radius * (if (c == 0f) 0.75f else 0.85f)   // centre tick is longer
            val outerR  = radius * 1.02f
            val x1 = (cx + innerR * cos(angle)).toFloat()
            val y1 = (cy + innerR * sin(angle)).toFloat()
            val x2 = (cx + outerR * cos(angle)).toFloat()
            val y2 = (cy + outerR * sin(angle)).toFloat()
            tickPaint.strokeWidth = if (c == 0f) 5f else 2f
            canvas.drawLine(x1, y1, x2, y2, tickPaint)
        }
    }


    private fun centsToAngle(cents: Float): Float {
        return 180f + (cents + 50f) * (180f / 100f)
    }
}