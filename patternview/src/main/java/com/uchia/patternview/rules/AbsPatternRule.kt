package com.uchia.patternview.rules

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.support.annotation.DrawableRes
import android.support.v4.content.ContextCompat
import android.view.View
import com.uchia.patternview.Cell
import com.uchia.patternview.IPatternView
import com.uchia.patternview.UltimatePatternView
import com.uchia.patternview.extensions.CellUtils
import com.uchia.patternview.rules.enums.DisplayMode
import java.util.*

abstract class AbsPatternRule : IPatternRule {

    private var patternDrawLookup : Array<BooleanArray>
    private var cells : Array<Array<Cell>>
    private val rows : Int
    private val columns : Int
    private val size : Int

    protected val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    protected val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    protected val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    protected val hostView : IPatternView

    protected var context: Context

    var pathColor: Int = 0
        get() = field
        set(value) {
            field = value
            pathPaint.color = field
            hostView.invalidate()
        }

    var circleColor: Int = 0
        set(value) {
            field = value
            circlePaint.colorFilter = PorterDuffColorFilter(
                    circleColor,
                    PorterDuff.Mode.MULTIPLY)
            hostView.invalidate()
        }

    var dotColor: Int = 0
        set(value) {
            field = value
            dotPaint.colorFilter = PorterDuffColorFilter(dotColor, PorterDuff.Mode.MULTIPLY)
            hostView.invalidate()
        }

    override var patternDisplayMode = DisplayMode.Correct

    override var inStealthMode: Boolean = false

    override var inErrorStealthMode: Boolean = false

    override var patternInProgress: Boolean = false

    override var numberCircleRadius: Float = 0f

    override var numberCircleStroke: Float = 0f

    constructor(rows: Int, columns: Int, hostView : IPatternView) {

        CellUtils.checkRange(rows, columns)

        this.rows = rows
        this.columns = columns
        this.size = rows * columns
        this.hostView = hostView
        this.context = hostView.hostContext

        patternDrawLookup = Array(rows) { rowIndex ->
            BooleanArray(columns) { colIndex ->
                false
            }
        }

        cells = Array(rows) { rowIndex ->
            Array(columns) { colIndex ->
                Cell(rowIndex, colIndex)
            }
        }

    }

    override fun getRowCount(): Int = rows

    override fun getColumnCount(): Int = columns

    override fun getCell(row: Int, column: Int): Cell = cells[row][column]

    override fun getSize(): Int = size

    override fun clear() {
        cells = Array(0) { _ ->
            Array(0) { _ ->
                Cell(-1, -1)
            }
        }
    }

    override fun draw(cell: Cell, drawn: Boolean) {
        patternDrawLookup[cell.row][cell.column] = drawn
    }

    override fun draw(row: Int, column: Int, drawn: Boolean) {
        patternDrawLookup[row][column] = drawn
    }

    override fun clearDrawing() {
        for (r in 0 until rows){
           Arrays.fill(patternDrawLookup[r],false)
        }
    }

    override fun clearCellState() {
        cells.forEach {
            it.forEach { cell ->
                cell.isSelected = false
            }
        }
    }

    override fun isDrawn(row: Int, column: Int): Boolean = patternDrawLookup[row][column]

    override fun isDrawn(cell: Cell): Boolean = patternDrawLookup[cell.row][cell.column]

    override fun isExcludeCell(row: Int,col : Int): Boolean = false

    override fun isInExcludeColumn(column: Int): Boolean = false

    override fun isInExcludeRow(row: Int): Boolean = false

    override fun isInClickArea(cell : Cell,x : Float, y : Float): Boolean = false

    override fun getClickContent(cell: Cell): String? = null

    private fun getBitmapFor(resId: Int): Bitmap {
        return BitmapFactory.decodeResource(context.resources, resId)
    }

    fun drawableToBitmap(@DrawableRes resId: Int): Bitmap? {
        return drawableToBitmap(ContextCompat.getDrawable(context, resId)!!)
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        val width = drawable.intrinsicWidth
        val height = drawable.intrinsicHeight
        if (width <= 0 || height <= 0) {
            return null
        }
        val config = if (drawable.opacity != PixelFormat.OPAQUE)
            Bitmap.Config.ARGB_8888
        else
            Bitmap.Config.RGB_565// 取 drawable 的颜色格式
        val bitmap = Bitmap.createBitmap(width, height, config)// 建立对应 bitmap
        val canvas = Canvas(bitmap) // 建立对应 bitmap 的画布
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)      // 把 drawable 内容画到画布中
        return bitmap
    }

}