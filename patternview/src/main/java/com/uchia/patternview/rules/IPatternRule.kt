package com.uchia.patternview.rules

import com.uchia.patternview.Cell
import com.uchia.patternview.rules.enums.DisplayMode

interface IPatternRule {

    var patternDisplayMode : DisplayMode

    var inStealthMode : Boolean
    var inErrorStealthMode : Boolean

    var patternInProgress : Boolean

    fun getRowCount(): Int

    fun getColumnCount(): Int

    fun getCell(row: Int, column: Int): Cell

    fun getSize(): Int

    fun clear()

    fun draw(t: Cell, drawn: Boolean)

    fun draw(row: Int, column: Int, drawn: Boolean)

    fun clearDrawing()

    fun isDrawn(row: Int, column: Int): Boolean

    fun isDrawn(t: Cell): Boolean

    fun getDrawProxy() : IDrawRule



}