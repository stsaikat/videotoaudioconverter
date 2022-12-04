package com.simplerapps.phonic.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.simplerapps.phonic.LogD
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class VolumeProgressBarView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private var listener: OnVolumeChangedListener? = null
    fun setVolumeListener(listener: OnVolumeChangedListener) {
        this.listener = listener
    }

    var maxValue: Int = 500
    var currentValue: Int = 100
        set(value) {
            field = value
            tempCurrent = value
            listener?.onVolumeChange(value)
        }
    private var tempCurrent = 100

    private val linePaint = Paint().apply {
        strokeWidth = 10f
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    override fun onDraw(canvas: Canvas?) {
        linePaint.strokeWidth = max(1f, width / 105f)
        canvas?.let { mCanvas ->
            //mCanvas.drawColor(Color.TRANSPARENT)
            val startY = height / 6f
            val endY = 5 * height / 6f
            for (i in 2 until width - 2*linePaint.strokeWidth.toInt() step 5 * linePaint.strokeWidth.toInt()) {
                val x = ((i + tranX + width) % width).toFloat()
                linePaint.alpha = (255 * sin(x / width * Math.PI)).toInt()
                mCanvas.drawLine(
                    x, startY, x, endY, linePaint
                )
            }
        }
    }

    private var prevX = 0f
    private var tranX: Int = 0

    private fun invokeChange() {
        if (tempCurrent == 0 && tranX < 0) return
        if (tempCurrent == maxValue && tranX > 0) return

        postInvalidate()

        var vol = (currentValue + tranX / 20)
        vol = max(0,vol)
        vol = min(vol, maxValue)

        if (vol != tempCurrent) {
            tempCurrent = vol
            listener?.onVolumeChange(tempCurrent)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let {
            when (it.action) {
                MotionEvent.ACTION_DOWN -> {
                    prevX = it.x
                    tranX = 0
                    tempCurrent = currentValue
                }
                MotionEvent.ACTION_MOVE -> {
                    val tx = (it.x - prevX).toInt()
                    if (tx != tranX) {
                        tranX = tx
                        invokeChange()
                    }
                }
                MotionEvent.ACTION_UP -> {
                    currentValue = tempCurrent
                }
            }
        }
        return true
    }

    interface OnVolumeChangedListener {
        fun onVolumeChange(volume: Int)
    }
}