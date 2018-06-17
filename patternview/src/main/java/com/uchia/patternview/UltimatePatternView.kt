package com.uchia.patternview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.os.Debug
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import com.uchia.patternview.rules.AbsPatternRule
import com.uchia.patternview.rules.GesturePattern
import com.uchia.patternview.rules.NumberPattern
import com.uchia.patternview.rules.enums.PatternType
import com.uchia.patternview.rules.enums.DisplayMode
import java.util.*

class UltimatePatternView : View ,IPatternView{

    enum class TouchEventHandleMode{
        ClickMode,
        GestureMode
    }

    private val hitFactor = 0.6f
    private val diameterFactor = 0.10f

    private var circleSize: Int = 0

    private var inputEnabled = true
    private val PROFILE_DRAWING = false
    private var drawingProfilingStarted = false

    private val invalidate = Rect()
    private lateinit var mPattern : ArrayList<Cell>

    var touchEventMode = TouchEventHandleMode.ClickMode

    var patternType = PatternType.Number
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

    var onPatternStartListener : OnPatternStartListener? = null
    var onPatternClearedListener : OnPatternClearedListener? = null
    var onPatternCellAddedListener : OnPatternCellAddedListener? = null
    var onPatternDetectedListener : OnPatternDetectedListener? = null
    var onPatternSelectedListener : OnPatternSelectedListener? = null

    override var squareWidth: Float = 0f
    override var squareHeight: Float = 0f

    override var gridColumns: Int = 0
    override var gridRows: Int = 0

    override var inProgressX = -1f
    override var inProgressY = -1f

    override var animatingPeriodStart: Long = 0

    override val hostContext: Context
        get() = context

    override var pathWidth: Float = 0f

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

            numberTextSize = typedArray.getDimension(
                    R.styleable.UltimatePatternView_upv_number_textSize,24f)

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
                    5) + 0f



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

//        squareWidth = Math.min(squareWidth, squareHeight)
//        squareHeight = Math.min(squareWidth, squareHeight)

        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        patternRule.getDrawProxy().draw(canvas,mPattern)
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!inputEnabled || !isEnabled) {
            return false
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                handleActionDown(event)
                return true
            }
            MotionEvent.ACTION_UP -> {
                handleActionUp()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                handleActionMove(event)
                return true
            }
            MotionEvent.ACTION_CANCEL -> {

                patternRule.patternInProgress = false
                resetPattern()
                notifyPatternCleared()

                if (PROFILE_DRAWING) {
                    if (drawingProfilingStarted) {
                        Debug.stopMethodTracing()
                        drawingProfilingStarted = false
                    }
                }
                return true
            }
            else -> {
            }
        }
        return false
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

        return if (patternRule.isDrawn(rowHit, columnHit)
                || patternRule.isInExcludeRow(rowHit)
                || patternRule.isInExcludeColumn(columnHit)
                || patternRule.isExcludeCell(rowHit,columnHit)) {
            null
        } else {
            patternRule.getCell(rowHit, columnHit)
        }
    }

    private fun detectAndAddHitByClickMode(x: Float, y: Float): Cell?{
        val cell = checkForNewHit(x, y)
        if (cell != null){
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

    private fun handleActionDown(event: MotionEvent){
        if (touchEventMode == TouchEventHandleMode.ClickMode){
            handleTouchDownByClickMode(event)
        } else{
            handleActionDownByTouchMode(event)
        }
    }


    private fun handleActionDownByTouchMode(event: MotionEvent) {
        resetPattern()
        val x = event.x
        val y = event.y
        val hitCell = detectAndAddHit(x, y)
        if (hitCell != null) {
            patternRule.patternInProgress = true
            patternRule.patternDisplayMode = DisplayMode.Correct
            notifyPatternStarted()
        } else {
            /*
             * Original source check for patternInProgress == true first before
			 * calling this block. But if we do that, there will be nothing
			 * happened when the user taps at empty area and releases the
			 * finger. We want the pattern to be reset and the message will be
			 * updated after the user did that.
			 */
            patternRule.patternInProgress = false
            notifyPatternCleared()
        }
        if (hitCell != null) {
            val startX = getCenterXForColumn(hitCell.column)
            val startY = getCenterYForRow(hitCell.row)

            val widthOffset = squareWidth / 2f
            val heightOffset = squareHeight / 2f

            invalidate((startX - widthOffset).toInt(),
                    (startY - heightOffset).toInt(),
                    (startX + widthOffset).toInt(), (startY + heightOffset).toInt())
        }
        inProgressX = x
        inProgressY = y
        if (PROFILE_DRAWING) {
            if (!drawingProfilingStarted) {
                Debug.startMethodTracing("LockPatternDrawing")
                drawingProfilingStarted = true
            }
        }
    }

    private fun handleTouchDownByClickMode(event: MotionEvent){
        val x = event.x
        val y = event.y
        resetPattern()
        patternRule.clearCellState()
        val hitCell = detectAndAddHitByClickMode(x, y)
        if (hitCell != null) {
            invalidateCellArea(hitCell)
            notifyPatternStarted()
        }
    }

    private fun invalidateCellArea(hitCell : Cell){
        val startX = getCenterXForColumn(hitCell.column)
        val startY = getCenterYForRow(hitCell.row)

        val widthOffset = squareWidth / 2f
        val heightOffset = squareHeight / 2f

        invalidate((startX - widthOffset).toInt(),
                (startY - heightOffset).toInt(),
                (startX + widthOffset).toInt(), (startY + heightOffset).toInt())
    }

    private fun handleActionMove(event: MotionEvent){
        if (touchEventMode == TouchEventHandleMode.ClickMode){
            handleActionMoveByClickMode(event)
        }else {
            handleActionMoveByTouchMode(event)
        }
    }

    private fun handleActionMoveByClickMode(event : MotionEvent){
        val historySize = event.historySize
        for (i in 0 .. historySize){
            val x = if (i < historySize) {
                event.getHistoricalX(i)
            } else {
                event.x
            }

            val y = if (i < historySize) {
                event.getHistoricalY(i)
            } else {
                event.y
            }

            if (mPattern.size > 0){
                val hitCell = mPattern[0]
                val startX = getCenterXForColumn(hitCell.column)
                val startY = getCenterYForRow(hitCell.row)

                hitCell.isSelected = patternRule.isInClickArea(hitCell,x,y)

               invalidateCellArea(hitCell)
            } else{
                var hitCell = detectAndAddHit(x, y)
                val patternSize = mPattern.size
                if (hitCell != null && patternSize == 0) {
                    hitCell.isSelected = true
                    notifyPatternStarted()
                    invalidateCellArea(hitCell)
                }
            }
        }
    }

    private fun handleActionMoveByTouchMode(event : MotionEvent) {
        // Handle all recent motion events so we don't skip any cells even when
        // the device
        // is busy...
        val historySize = event.historySize
        for (i in 0 until historySize + 1) {
            val x = if (i < historySize) {
                event.getHistoricalX(i)
            } else {
                event.x
            }

            val y = if (i < historySize) {
                event.getHistoricalY(i)
            } else {
                event.y
            }
            val patternSizePreHitDetect = mPattern.size
            var hitCell = detectAndAddHit(x, y)
            val patternSize = mPattern.size
            if (hitCell != null && patternSize == 1) {
                patternRule.patternInProgress = true
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

                if (patternRule.patternInProgress && patternSize > 0) {
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

    private fun handleActionUp(){
        if (touchEventMode == TouchEventHandleMode.ClickMode){
            handleActionUpByClickMode()
        } else {
            handleActionUpByTouchMode()
        }
    }

    private fun handleActionUpByTouchMode() {
        // report pattern detected
        if (!mPattern.isEmpty()) {
            patternRule.patternInProgress = false
            notifyPatternDetected()
            invalidate()
        }
        if (PROFILE_DRAWING) {
            if (drawingProfilingStarted) {
                Debug.stopMethodTracing()
                drawingProfilingStarted = false
            }
        }
    }

    private fun handleActionUpByClickMode(){
        if (!mPattern.isEmpty()){
            val hitCell = mPattern[0]
            Log.i("ClickMode","row : ${hitCell.row} col : ${hitCell.column}")
            hitCell.isSelected = false
            notifyPatternSelected()
            invalidateCellArea(hitCell)
        }
    }


    override fun getCenterXForColumn(column: Int): Float {
        return paddingLeft + column * squareWidth + squareWidth / 2f
    }

    override fun getCenterYForRow(row: Int): Float {
        return paddingTop + row * squareHeight + squareHeight / 2f
    }

    override fun getHostViewWidth(): Int = measuredWidth

    override fun getHostViewHeight(): Int = measuredHeight

    override var numberTextSize: Float = 0f

    fun addCellToPattern(newCell: Cell) {
        patternRule.draw(newCell, true)
        mPattern.add(newCell)
        notifyCellAdded()
    }

    fun resetPattern() {
        mPattern.clear()
        clearPatternDrawLookup()
        patternRule.patternDisplayMode = DisplayMode.Correct
        invalidate()
    }

    fun setDisplayMode(displayMode: DisplayMode) {
        patternRule.patternDisplayMode = displayMode
        if (displayMode == DisplayMode.Animate) {
            if (mPattern.size == 0) {
                throw IllegalStateException(
                        "you must have a pattern to " + "animate if you want to set the display mode to animate")
            }
            animatingPeriodStart = SystemClock.elapsedRealtime()
            val first = mPattern[0]
            inProgressX = getCenterXForColumn(first.column)
            inProgressY = getCenterYForRow(first.row)
            clearPatternDrawLookup()
        }
        invalidate()
    }

    override fun isHostInEditMode(): Boolean = isInEditMode

    private fun clearPatternDrawLookup() {
//        for (i in 0 until gridRows) {
//        for (j in 0 until gridColumns) {
        patternRule.clearDrawing()
//            }
//        }
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

    private fun notifyPatternSelected(){
        onPatternSelectedListener?.onPatternSelected()
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

    interface OnPatternSelectedListener{
        fun onPatternSelected()
    }

}