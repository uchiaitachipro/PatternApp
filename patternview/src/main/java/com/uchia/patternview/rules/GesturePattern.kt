package com.uchia.patternview.rules

import android.graphics.*
import android.os.SystemClock
import android.util.Log
import com.uchia.patternview.Cell
import com.uchia.patternview.IPatternView
import com.uchia.patternview.R
import com.uchia.patternview.rules.enums.DisplayMode
import java.util.*

class GesturePattern : AbsPatternRule {

    private val MILLIS_PER_CIRCLE_ANIMATING = 700

    private val diameterFactor = 0.10f

    private var bitmapBtnDefault: Bitmap? = null
    private var bitmapBtnTouched: Bitmap? = null
    private var bitmapCircleDefault: Bitmap? = null
    private var bitmapCircleSelected: Bitmap? = null
    private var bitmapCircleRed: Bitmap? = null

    private var bitmapWidth: Int = 0
    private var bitmapHeight: Int = 0

    private val currentPath = Path()

    private val circleMatrix = Matrix()


    constructor(row : Int, col :Int, hostView : IPatternView) : super(row,col,hostView){
        bitmapBtnDefault = drawableToBitmap(R.drawable.mk_hider_pattern_untouched)
        bitmapCircleDefault = bitmapBtnDefault

        bitmapBtnTouched = drawableToBitmap(R.drawable.mk_hider_pattern_touched)
        bitmapCircleSelected = bitmapBtnTouched
        bitmapCircleRed = drawableToBitmap(R.drawable.mk_hider_pattern_red)
        computeBitmapSize()

        pathPaint.isDither = true
        pathPaint.style = Paint.Style.STROKE
        pathPaint.strokeJoin = Paint.Join.ROUND
        pathPaint.strokeCap = Paint.Cap.ROUND
    }

    override fun getDrawProxy(): IDrawRule = drawProxy


    private fun computeBitmapSize() {
        // bitmaps have the size of the largest bitmap in this group
        val bitmaps = arrayOf(bitmapBtnDefault, bitmapCircleSelected, bitmapCircleRed)
        if (hostView.isHostInEditMode()) {
            bitmapWidth = 50
            bitmapHeight = 50
            return
        }

        for (bitmap in bitmaps) {
            bitmapWidth = Math.max(bitmapWidth, bitmap!!.width)
            bitmapHeight = Math.max(bitmapHeight, bitmap!!.height)
        }
    }


    private var drawProxy = object : IDrawRule{


        override fun draw(canvas: Canvas, pattern : ArrayList<Cell>) {

            val count = pattern.size

            if (patternDisplayMode == DisplayMode.Animate) {

                val oneCycle = (count + 1) * MILLIS_PER_CIRCLE_ANIMATING
                val spotInCycle = (SystemClock.elapsedRealtime() - hostView.animatingPeriodStart)
                        .toInt() % oneCycle
                val numCircles = spotInCycle / MILLIS_PER_CIRCLE_ANIMATING

                clearDrawing()

                for (i in 0 until numCircles) {
                    val cell = pattern[i]
                    draw(cell, true)
                }

                val needToUpdateInProgressPoint = numCircles > 0 && numCircles < count

                if (needToUpdateInProgressPoint) {
                    val percentageOfNextCircle = (spotInCycle % MILLIS_PER_CIRCLE_ANIMATING)
                            .toFloat() / MILLIS_PER_CIRCLE_ANIMATING

                    val currentCell = pattern[numCircles - 1]
                    val centerX = hostView.getCenterXForColumn(currentCell.column)
                    val centerY = hostView.getCenterYForRow(currentCell.row)

                    val nextCell = pattern[numCircles]
                    val dx = percentageOfNextCircle *
                            (hostView.getCenterXForColumn(nextCell.column) - centerX)
                    val dy = percentageOfNextCircle *
                            (hostView.getCenterYForRow(nextCell.row) - centerY)
                    hostView.inProgressX = centerX + dx
                    hostView.inProgressY = centerY + dy
                }
                hostView.invalidate()
            }

            val squareWidth = hostView.squareWidth
            val squareHeight = hostView.squareHeight

            val radius = if (hostView.pathWidth > 0) {
                hostView.pathWidth
            } else {
                squareWidth * diameterFactor * 0.5f
            }
            pathPaint.strokeWidth = radius

            val currentPath = currentPath
            currentPath.rewind()

            val paddingTop = hostView.getPaddingTop()
            val paddingLeft = hostView.getPaddingLeft()

            for (i in 0 until hostView.gridRows) {
                val topY = paddingTop + i * squareHeight
                for (j in 0 until hostView.gridColumns) {
                    val leftX = paddingLeft + j * squareWidth
                    drawCircle(canvas, leftX.toInt(), topY.toInt(), isDrawn(i, j))
                }
            }

            val drawPath = !inStealthMode &&
                    patternDisplayMode == DisplayMode.Correct ||
                    !inErrorStealthMode &&
                    patternDisplayMode == DisplayMode.Wrong


            // draw the arrows associated with the path (unless the user is in
            // progress, and
            // we are in stealth mode)
            val oldFlagCircle = circlePaint.flags and Paint.FILTER_BITMAP_FLAG != 0
            val oldFlagDot = dotPaint.flags and Paint.FILTER_BITMAP_FLAG != 0
            circlePaint.isFilterBitmap = true
            dotPaint.isFilterBitmap = true

            if (drawPath) {
                var anyCircles = false
                for (i in 0 until count) {
                    val cell = pattern[i]

                    // only draw the part of the pattern stored in
                    // the lookup table (this is only different in the case
                    // of animation).
                    if (!isDrawn(cell)) {
                        break
                    }
                    anyCircles = true

                    val centerX = hostView.getCenterXForColumn(cell.column)
                    val centerY = hostView.getCenterYForRow(cell.row)
                    Log.i("onDraw","column: ${cell.column} row: ${cell.row} centerX: $centerX centerY: $centerY")
                    if (i == 0) {
                        currentPath.moveTo(centerX, centerY)
                    } else {
                        currentPath.lineTo(centerX, centerY)
                    }
                }

                // add last in progress section
                if ((patternInProgress || patternDisplayMode == DisplayMode.Animate)
                        && anyCircles && count > 1) {
                    currentPath.lineTo(hostView.inProgressX, hostView.inProgressY)
                }
                canvas.drawPath(currentPath, pathPaint)
            }

            circlePaint.isFilterBitmap = oldFlagCircle // restore default flag
            dotPaint.isFilterBitmap = oldFlagDot // restore default flag

        }

        private fun drawCircle(canvas: Canvas, leftX: Int, topY: Int, partOfPattern: Boolean){
            var outerCircle: Bitmap?

            if (!partOfPattern
                    || inStealthMode && patternDisplayMode == DisplayMode.Correct
                    || inErrorStealthMode && patternDisplayMode == DisplayMode.Wrong) {
                // unselected circle
                outerCircle = bitmapCircleDefault
            } else if (patternInProgress) {
                // user is in middle of drawing a pattern
                outerCircle = bitmapCircleSelected
            } else if (patternDisplayMode == DisplayMode.Wrong) {
                // the pattern is wrong
                outerCircle = bitmapCircleRed
            } else if (patternDisplayMode == DisplayMode.Correct
                    || patternDisplayMode == DisplayMode.Animate) {
                outerCircle = bitmapCircleSelected
            } else {
                throw IllegalStateException("unknown display mode $patternDisplayMode")
            }

            val width = bitmapWidth
            val height = bitmapHeight

            val squareWidth = hostView.squareWidth
            val squareHeight = hostView.squareHeight

            val offsetX = ((squareWidth - width) / 2f).toInt()
            val offsetY = ((squareHeight - height) / 2f).toInt()

            circleMatrix.setTranslate((leftX + offsetX + 0f), (topY + offsetY + 0f))

            canvas.drawBitmap(outerCircle, circleMatrix, circlePaint)
        }

    }


}