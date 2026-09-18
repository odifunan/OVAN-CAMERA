package com.ovan.camera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class CameraOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        alpha = 120
    }
    private val focusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        alpha = 220
    }
    private var showGrid = true
    private var focusX = -1f
    private var focusY = -1f
    private var focusUntil = 0L

    fun setGridVisible(visible: Boolean) {
        showGrid = visible
        invalidate()
    }

    fun showFocus(x: Float, y: Float) {
        focusX = x
        focusY = y
        focusUntil = System.currentTimeMillis() + 900L
        invalidate()
        postDelayed({ invalidate() }, 920L)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (showGrid) {
            val thirdW = width / 3f
            val thirdH = height / 3f
            canvas.drawLine(thirdW, 0f, thirdW, height.toFloat(), gridPaint)
            canvas.drawLine(thirdW * 2, 0f, thirdW * 2, height.toFloat(), gridPaint)
            canvas.drawLine(0f, thirdH, width.toFloat(), thirdH, gridPaint)
            canvas.drawLine(0f, thirdH * 2, width.toFloat(), thirdH * 2, gridPaint)
        }
        if (focusX >= 0f && System.currentTimeMillis() < focusUntil) {
            val r = 48f
            canvas.drawRect(RectF(focusX - r, focusY - r, focusX + r, focusY + r), focusPaint)
        }
    }
}
