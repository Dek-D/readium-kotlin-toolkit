/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

internal class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : AppCompatImageView(context, attrs, defStyle) {

    private val matrix = Matrix()
    private val savedMatrix = Matrix()
    private var mode = NONE
    private val startPoint = PointF()
    private val midPoint = PointF()
    private var minScale = 1f
    private val maxScale = 6f
    private val values = FloatArray(9)

    private val scaleDetector = ScaleGestureDetector(context, ScaleListener())
    private val gestureDetector = GestureDetector(context, DoubleTapListener())

    init {
        scaleType = ScaleType.MATRIX
        imageMatrix = matrix
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        fitImageToView()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                savedMatrix.set(matrix)
                startPoint.set(event.x, event.y)
                mode = DRAG
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (spacing(event) > 10f) {
                    savedMatrix.set(matrix)
                    midPoint(midPoint, event)
                    mode = ZOOM
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (mode == DRAG && !scaleDetector.isInProgress) {
                    matrix.set(savedMatrix)
                    matrix.postTranslate(event.x - startPoint.x, event.y - startPoint.y)
                    clampMatrix()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> mode = NONE
        }
        imageMatrix = matrix
        return true
    }

    private fun fitImageToView() {
        val d = drawable ?: return
        val dW = d.intrinsicWidth.toFloat()
        val dH = d.intrinsicHeight.toFloat()
        if (dW <= 0 || dH <= 0 || width <= 0 || height <= 0) return
        val scale = min(width / dW, height / dH)
        minScale = scale
        matrix.setScale(scale, scale)
        matrix.postTranslate((width - dW * scale) / 2f, (height - dH * scale) / 2f)
        imageMatrix = matrix
    }

    private fun clampMatrix() {
        matrix.getValues(values)
        val scale = values[Matrix.MSCALE_X]
        val d = drawable ?: return
        val dW = d.intrinsicWidth * scale
        val dH = d.intrinsicHeight * scale
        val vW = width.toFloat()
        val vH = height.toFloat()
        val tx = values[Matrix.MTRANS_X]
        val ty = values[Matrix.MTRANS_Y]
        values[Matrix.MTRANS_X] = if (dW <= vW) (vW - dW) / 2f else tx.coerceIn(vW - dW, 0f)
        values[Matrix.MTRANS_Y] = if (dH <= vH) (vH - dH) / 2f else ty.coerceIn(vH - dH, 0f)
        matrix.setValues(values)
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            matrix.getValues(values)
            val cur = values[Matrix.MSCALE_X]
            val factor = (detector.scaleFactor * cur).coerceIn(minScale, maxScale) / cur
            matrix.postScale(factor, factor, detector.focusX, detector.focusY)
            clampMatrix()
            imageMatrix = matrix
            return true
        }
    }

    private inner class DoubleTapListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            matrix.getValues(values)
            val cur = values[Matrix.MSCALE_X]
            val target = if (cur > minScale * 1.5f) minScale else min(minScale * 3f, maxScale)
            matrix.postScale(target / cur, target / cur, e.x, e.y)
            clampMatrix()
            imageMatrix = matrix
            return true
        }
    }

    private fun spacing(e: MotionEvent): Float {
        if (e.pointerCount < 2) return 0f
        val dx = e.getX(0) - e.getX(1)
        val dy = e.getY(0) - e.getY(1)
        return sqrt((dx.pow(2) + dy.pow(2)).toDouble()).toFloat()
    }

    private fun midPoint(p: PointF, e: MotionEvent) {
        p.set((e.getX(0) + e.getX(1)) / 2f, (e.getY(0) + e.getY(1)) / 2f)
    }

    companion object {
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
    }
}
