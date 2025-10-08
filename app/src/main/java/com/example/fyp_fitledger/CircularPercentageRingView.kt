package com.example.fyp_fitledger

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.view.View
import com.example.fyp_fitledger.R // Ensure this import is correct for your project

class CircularPercentageRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var percentage: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 100f)
            invalidate() // Request a redraw when the percentage changes
        }

    private var ringColor: Int = Color.BLUE
    private var backgroundColor: Int = Color.LTGRAY
    private var ringWidth: Float = 20f
    private var startAngle: Float = -90f // Start at the top

    //Text inside the ring view
    private var centerText: String? = null
    private var centerTextSize = 40f
    private var centerTextColor = Color.BLACK

    //gradient color on ring
    private var useGradient = false
    private var gradientColors = intArrayOf(Color.RED, Color.GREEN)

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
        textSize = 40f // Adjust as needed
    }

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CircularPercentageRingView, // Create this styleable in attrs.xml
            defStyleAttr,
            0
        ).apply {
            try {
                percentage = getFloat(R.styleable.CircularPercentageRingView_percentage, 0f)
                ringColor = getColor(R.styleable.CircularPercentageRingView_ringColor, Color.BLUE)
                backgroundColor = getColor(R.styleable.CircularPercentageRingView_ringBackgroundColor, Color.LTGRAY)
                ringWidth = getDimension(R.styleable.CircularPercentageRingView_ringWidth, 20f)
                startAngle = getFloat(R.styleable.CircularPercentageRingView_startAngle, -90f)
            } finally {
                recycle()
            }
        }
        ringPaint.strokeWidth = ringWidth
        backgroundPaint.strokeWidth = ringWidth
        backgroundPaint.color = backgroundColor
        ringPaint.color = ringColor
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = 200 // Adjust as needed
        val desiredHeight = 200 // Adjust as needed

        val width = resolveSize(desiredWidth, widthMeasureSpec)
        val height = resolveSize(desiredHeight, heightMeasureSpec)

        val size = Math.min(width, height)
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = Math.min(width, height) / 2f - ringWidth / 2f

        val oval = RectF(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )

        if (useGradient) {
            val sweepGradient = SweepGradient(centerX, centerY, gradientColors, null)
            ringPaint.shader = sweepGradient
        } else {
            ringPaint.shader = null
            ringPaint.color = ringColor
        }

        // Draw the background ring
        canvas.drawArc(oval, 0f, 360f, false, backgroundPaint)

        // Calculate the sweep angle based on the percentage
        val sweepAngle = 360 * (percentage / 100f)

        // Draw the foreground ring (percentage)
        canvas.drawArc(oval, startAngle, sweepAngle, false, ringPaint)

        // Draw the center text if it's not null
        centerText?.let {
            val textLines = it.split("\n")
            val lineHeight = textPaint.fontSpacing
            val totalHeight = lineHeight * textLines.size
            for ((index, line) in textLines.withIndex()) {
                canvas.drawText(
                    line,
                    centerX,
                    centerY - totalHeight / 2 + lineHeight * index + lineHeight / 2 + 10f,
                    textPaint
                )
            }
        }
    }


    // Public methods to customize appearance (optional)
    fun setRingColor(color: Int) {
        ringColor = color
        ringPaint.color = color
        invalidate()
    }

    override fun setBackgroundColor(color: Int) {
        backgroundColor = color
        backgroundPaint.color = color
        invalidate()
    }

    fun setRingWidth(width: Float) {
        ringWidth = width
        ringPaint.strokeWidth = width
        backgroundPaint.strokeWidth = width
        invalidate()
    }

    fun setStartAngle(angle: Float) {
        startAngle = angle
        invalidate()
    }

    fun setCenterText(text: String) {
        centerText = text
        invalidate()
    }

    fun setCenterTextSize(size: Float) {
        centerTextSize = size
        textPaint.textSize = size
        invalidate()
    }

    fun setCenterTextColor(color: Int) {
        centerTextColor = color
        textPaint.color = color
        invalidate()
    }

    fun enableGradient(colors: IntArray) {
        gradientColors = colors
        useGradient = true
        invalidate()
    }

}