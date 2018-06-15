package com.uchia.patternview

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import com.uchia.patternview.rules.AbsPatternRule
import com.uchia.patternview.rules.GesturePattern
import com.uchia.patternview.rules.NumberPattern
import com.uchia.patternview.rules.PatternType
import java.util.*

class UltimatePatternView : View {

    private val hitFactor = 0.6f
    private val diameterFactor = 0.10f

    private var circleSize: Int = 0

    private var pathWidth: Int = 0
    private var gridColumns: Int = 0
    private var gridRows: Int = 0

    private var squareWidth: Float = 0f
    private var squareHeight: Float = 0f

    private var inProgressX = -1f
    private var inProgressY = -1f

    private var patternInProgress = false

    private val invalidate = Rect()
    private lateinit var mPattern: ArrayList<Cell>


    var patternType = PatternType.Gesture
    lateinit var patternRule: AbsPatternRule

    var enableHapticFeedback = true

    var pathColor: Int
        get() = patternRule.pathColor
        set(value) {
            patternRule.pathColor = value
        }

    var circleColor: Int
        get() = patternRule.circleColor
        set(value) {
            patternRule.circleColor = value
        }

    var dotColor: Int
        get() = patternRule.dotColor
        set(value) {
            patternRule.dotColor = value
        }

    var onPatternStartListener: OnPatternStartListener? = null
    var onPatternClearedListener: OnPatternClearedListener? = null
    var onPatternCellAddedListener: OnPatternCellAddedListener? = null
    var onPatternDetectedListener: OnPatternDetectedListener? = null

    constructor(context: Context)
            : this(context, null)

    constructor(context: Context, attrs: AttributeSet?)
            : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int)
            : super(context, attrs, defStyle) {
        init(context, attrs)


    }

    private fun init(context: Context, attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.UltimatePatternView)
        try {

            gridColumns = typedArray.getInt(
                    R.styleable.UltimatePatternView_upv_gridColumns,
                    3)
            gridRows = typedArray.getInt(R.styleable.UltimatePatternView_upv_gridRows, 3)

            patternRule = initPattern(gridRows, gridColumns)

            circleSize = typedArray.getDimensionPixelSize(
                    R.styleable.UltimatePatternView_upv_circleSize,
                    20)
            circleColor = typedArray.getColor(
                    R.styleable.UltimatePatternView_upv_circleColor,
                    Color.BLACK)
            dotColor = typedArray.getColor(
                    R.styleable.UltimatePatternView_upv_dotColor,
                    Color.BLACK)
            pathColor = typedArray.getColor(
                    R.styleable.UltimatePatternView_upv_pathColor,
                    Color.BLACK)
            pathWidth = typedArray.getDimensionPixelOffset(
                    R.styleable.UltimatePatternView_upv_pathWidth,
                    5)

        } finally {
            typedArray.recycle()
        }

        mPattern = ArrayList(patternRule.getSize())

    }

    private fun initPattern(row: Int, col: Int): AbsPatternRule {
        return when (patternType) {
            PatternType.Gesture -> GesturePattern(row, col, this)
            PatternType.Number -> NumberPattern(row, col, this)
        }
    }

    //todo: support al_most mode
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var width = View.MeasureSpec.getSize(widthMeasureSpec)
        var height = View.MeasureSpec.getSize(heightMeasureSpec)
        val widthMode = View.MeasureSpec.getMode(widthMeasureSpec)
        if (widthMode == View.MeasureSpec.AT_MOST) {
            width = gridColumns * circleSize
            squareWidth = circleSize.toFloat()
        } else {
            squareWidth = width / gridColumns.toFloat()
        }
        val heightMode = View.MeasureSpec.getMode(heightMeasureSpec)
        if (heightMode == View.MeasureSpec.AT_MOST) {
            height = gridRows * circleSize
            squareHeight = circleSize.toFloat()
        } else {
            squareHeight = height / gridRows.toFloat()
        }

        squareWidth = Math.min(squareWidth, squareHeight)
        squareHeight = Math.min(squareWidth, squareHeight)
        setMeasuredDimension(width, height)
    }



    private fun getColumnHit(x: Float): Int {
        val squareWidth = this.squareWidth
        val hitSize = squareWidth * hitFactor

        val offset = paddingLeft + (squareWidth - hitSize) / 2f
        for (i in 0 until gridColumns) {

            val hitLeft = offset + squareWidth * i
            if (x >= hitLeft && x <= hitLeft + hitSize) {
                return i
            }
        }
        return -1
    }

    private fun getRowHit(y: Float): Int {

        val squareHeight = this.squareHeight
        val hitSize = squareHeight * hitFactor

        val offset = paddingTop + (squareHeight - hitSize) / 2f
        for (i in 0 until gridRows) {

            val hitTop = offset + squareHeight * i
            if (y >= hitTop && y <= hitTop + hitSize) {
                return i
            }
        }
        return -1
    }

    private fun checkForNewHit(x: Float, y: Float): Cell? {

        val rowHit = getRowHit(y)
        if (rowHit < 0) {
            return null
        }
        val columnHit = getColumnHit(x)
        if (columnHit < 0) {
            return null
        }

        return if (patternRule.isDrawn(rowHit, columnHit)) {
            null
        } else {
            patternRule.getCell(rowHit, columnHit)
        }
    }

    private fun detectAndAddHit(x: Float, y: Float): Cell? {
        val cell = checkForNewHit(x, y)
        if (cell != null) {

            // check for gaps in existing pattern
            // Cell fillInGapCelal = null;
            val newCells = ArrayList<Cell>()
            if (!mPattern.isEmpty()) {
                val lastCell = mPattern[mPattern.size - 1]
                val dRow = cell.row - lastCell.row
                val dCol = cell.column - lastCell.column
                val rsign = if (dRow > 0) 1 else -1
                val csign = if (dCol > 0) 1 else -1

                if (dRow == 0) {
                    for (i in 1 until Math.abs(dCol)) {
                        newCells.add(Cell(lastCell.row, lastCell.column + i * csign))
                    }
                } else if (dCol == 0) {
                    for (i in 1 until Math.abs(dRow)) {
                        newCells.add(Cell(lastCell.row + i * rsign,
                                lastCell.column))
                    }
                } else if (Math.abs(dCol) == Math.abs(dRow)) {
                    for (i in 1 until Math.abs(dRow)) {
                        newCells.add(Cell(lastCell.row + i * rsign,
                                lastCell.column + i * csign))
                    }
                }

            }
            for (fillInGapCell in newCells) {
                if (fillInGapCell != null && !patternRule.isDrawn(fillInGapCell)) {
                    addCellToPattern(fillInGapCell)
                }
            }
            addCellToPattern(cell)
            if (enableHapticFeedback) {
                performHapticFeedback(
                        HapticFeedbackConstants.VIRTUAL_KEY,
                        HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING or
                                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
            }
            return cell
        }
        return null
    }

    private fun handleActionMove(event: MotionEvent) {
        // Handle all recent motion events so we don't skip any cells even when
        // the device
        // is busy...
        val historySize = event.historySize
        for (i in 0 until historySize + 1) {
            val x = if (i < historySize) {
                event.getHistoricalX(i)
            }
            else{
                event.x
            }

            val y = if (i < historySize){
                event.getHistoricalY(i)
            }
            else{
                event.y
            }
            val patternSizePreHitDetect = mPattern.size
            var hitCell = detectAndAddHit(x, y)
            val patternSize = mPattern.size
            if (hitCell != null && patternSize == 1) {
                patternInProgress = true
                notifyPatternStarted()
            }
            // note current x and y for rubber banding of in progress patterns
            val dx = Math.abs(x - inProgressX)
            val dy = Math.abs(y - inProgressY)
            if (dx + dy > squareWidth * 0.01f) {
                var oldX = inProgressX
                var oldY = inProgressY

                inProgressX = x
                inProgressY = y

                if (patternInProgress && patternSize > 0) {
                    val pattern = mPattern
                    val radius = squareWidth * diameterFactor * 0.5f

                    val lastCell = pattern[patternSize - 1]

                    var startX = getCenterXForColumn(lastCell.column)
                    var startY = getCenterYForRow(lastCell.row)

                    var left: Float
                    var top: Float
                    var right: Float
                    var bottom: Float

                    val invalidateRect = invalidate

                    if (startX < x) {
                        left = startX
                        right = x
                    } else {
                        left = x
                        right = startX
                    }

                    if (startY < y) {
                        top = startY
                        bottom = y
                    } else {
                        top = y
                        bottom = startY
                    }

                    // Invalidate between the pattern's last cell and the
                    // current location
                    invalidateRect.set((left - radius).toInt(),
                            (top - radius).toInt(), (right + radius).toInt(),
                            (bottom + radius).toInt())

                    if (startX < oldX) {
                        left = startX
                        right = oldX
                    } else {
                        left = oldX
                        right = startX
                    }

                    if (startY < oldY) {
                        top = startY
                        bottom = oldY
                    } else {
                        top = oldY
                        bottom = startY
                    }

                    // Invalidate between the pattern's last cell and the
                    // previous location
                    invalidateRect.union((left - radius).toInt(),
                            (top - radius).toInt(), (right + radius).toInt(),
                            (bottom + radius).toInt())

                    // Invalidate between the pattern's new cell and the
                    // pattern's previous cell
                    if (hitCell != null) {
                        startX = getCenterXForColumn(hitCell.column)
                        startY = getCenterYForRow(hitCell.row)

                        if (patternSize >= 2) {
                            // (re-using hitcell for old cell)
                            hitCell = pattern[patternSize - 1
                                    - (patternSize - patternSizePreHitDetect)]
                            oldX = getCenterXForColumn(hitCell.column)
                            oldY = getCenterYForRow(hitCell.row)

                            if (startX < oldX) {
                                left = startX
                                right = oldX
                            } else {
                                left = oldX
                                right = startX
                            }

                            if (startY < oldY) {
                                top = startY
                                bottom = oldY
                            } else {
                                top = oldY
                                bottom = startY
                            }
                        } else {
                            right = startX
                            left = right
                            bottom = startY
                            top = bottom
                        }

                        val widthOffset = squareWidth / 2f
                        val heightOffset = squareHeight / 2f

                        invalidateRect.set((left - widthOffset).toInt(),
                                (top - heightOffset).toInt(),
                                (right + widthOffset).toInt(),
                                (bottom + heightOffset).toInt())
                    }

                    invalidate(invalidateRect)
                } else {
                    invalidate()
                }
            }
        }
        invalidate()
    }


    private fun getCenterXForColumn(column: Int): Float {
        return paddingLeft + column * squareWidth + squareWidth / 2f
    }

    private fun getCenterYForRow(row: Int): Float {
        return paddingTop + row * squareHeight + squareHeight / 2f
    }

    private fun addCellToPattern(newCell: Cell) {
        patternRule.draw(newCell, true)
        mPattern.add(newCell)
        notifyCellAdded()
    }


    private fun notifyCellAdded() {
        onPatternCellAddedListener?.onPatternCellAdded()
    }

    private fun notifyPatternStarted() {
        onPatternStartListener?.onPatternStart()
    }

    private fun notifyPatternDetected() {
        onPatternDetectedListener?.onPatternDetected()

    }

    private fun notifyPatternCleared() {
        onPatternClearedListener?.onPatternCleared()
    }

    interface OnPatternStartListener {
        fun onPatternStart()
    }

    interface OnPatternClearedListener {
        fun onPatternCleared()
    }

    interface OnPatternCellAddedListener {
        fun onPatternCellAdded()
    }

    interface OnPatternDetectedListener {
        fun onPatternDetected()
    }

}