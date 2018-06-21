package com.uchia.patternview.rules

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.uchia.patternview.Cell
import com.uchia.patternview.IPatternView
import java.util.ArrayList

class NumberPattern : AbsPatternRule {

    private val InvalidNumber = "-1"

    private val ClickAreaRatio = 0.3f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val debugPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val rect = Rect()

    private val positionToTextMap = mapOf(
            Pair(0, "1"), Pair(1, "2"), Pair(2, "3"),
            Pair(3, "4"), Pair(4, "5"), Pair(5, "6"),
            Pair(6, "7"), Pair(7, "8"), Pair(8, "9"),
            Pair(10, "0"), Pair(11, InvalidNumber)
    )


    private val numberToOffsetMap = HashMap<String, IntArray>(10)

    constructor(row: Int, col: Int, hostView: IPatternView)
            : super(row, col, hostView) {
        textPaint.color = Color.WHITE
        textPaint.textSize = hostView.numberTextSize

        selectedPaint.strokeWidth = 3f
        selectedPaint.color = Color.argb(51,255,255,255)
        selectedPaint.style = Paint.Style.FILL

        debugPaint.strokeWidth = 8f
        debugPaint.color = Color.RED


    }


    override fun isExcludeCell(row: Int, col: Int): Boolean {
        return (row == 3 && col == 0)
    }


    override fun getDrawProxy(): IDrawRule = drawProxy

    override fun isInClickArea(cell: Cell, x: Float, y: Float): Boolean {

        if (isInExcludeRow(cell.row)
                || isInExcludeColumn(cell.column)
                || isExcludeCell(cell.row, cell.column)) {
            return false
        }

        val index = cell.row * hostView.gridColumns


        val text = positionToTextMap[index]

        if (text == InvalidNumber) {
            return false
        }

        val horizontalArea = (ClickAreaRatio * hostView.squareWidth).toInt()
        val verticalArea = (ClickAreaRatio * hostView.squareHeight).toInt()
        val centerX = hostView.getCenterXForColumn(cell.column)
        var centerY = hostView.getCenterYForRow(cell.row)
        if ((centerX - horizontalArea) <= x && (x <= (centerX + horizontalArea))
                && (centerY - verticalArea) <= y && y <= (centerY + verticalArea)) {
            return true
        }

        return false
    }

    override fun getClickContent(cell: Cell): String? {

        if (isInExcludeRow(cell.row)
                || isInExcludeColumn(cell.column)
                || isExcludeCell(cell.row, cell.column)) {
            return null
        }

        val index = cell.row * hostView.gridColumns + cell.column
        return positionToTextMap[index]
    }


    private fun getNumberOffsetCached(number: String): IntArray {

        if (numberToOffsetMap.containsKey(number)) {
            return numberToOffsetMap[number]!!
        }

        textPaint.getTextBounds(number, 0, number.length, rect)
        var width = textPaint.measureText(number).toInt()
        val result = intArrayOf(width / 2 , rect.height() / 2)
        numberToOffsetMap[number] = result
        rect.setEmpty()
        return result
    }


    private val drawProxy = object : IDrawRule {


        override fun draw(canvas: Canvas, pattern: ArrayList<Cell>) {

//            hostView.clickAnimHelper?.draw(canvas)

            drawNumbers(canvas)
            if (pattern.size > 0){
                drawSelectedCircle(canvas,pattern[0])
            }
        }

        private fun drawNumbers(canvas: Canvas) {

            for (row in 0 until hostView.gridRows) {

                if (isInExcludeRow(row)) {
                    continue
                }

                for (col in 0 until hostView.gridColumns) {

                    if (isInExcludeColumn(col)) {
                        continue
                    }

                    if (isExcludeCell(row, col)) {
                        continue
                    }

                    val index = ((row * hostView.gridColumns) + col)
                    val text = positionToTextMap[index]

                    if (text == InvalidNumber) {
                        continue
                    }

                    val offsetXY = getNumberOffsetCached(text!!)
                    val centerX = hostView.getCenterXForColumn(col)
                    val centerY = hostView.getCenterYForRow(row)
                    canvas.drawText(
                            text,
                            centerX - offsetXY[0] ,
                            centerY + offsetXY[1],
                            textPaint)

//                    canvas.drawPoint(centerX,centerY,debugPaint)
                }

            }
        }

        private fun drawSelectedCircle(canvas : Canvas , cell : Cell){

            if (!cell.isSelected){
                return
            }

            val centerX = hostView.getCenterXForColumn(cell.column)
            val centerY = hostView.getCenterYForRow(cell.row)
            canvas.drawCircle(centerX,centerY,numberCircleRadius,selectedPaint)

        }


    }
}