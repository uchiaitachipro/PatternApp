package com.uchia.patternview.extensions

internal object CellUtils {

    fun checkRange(row: Int, column: Int) {
        if (row < 0) {
            throw IllegalArgumentException("row must be in range 0-" + (row - 1))
        }
        if (column < 0) {
            throw IllegalArgumentException("column must be in range 0-" + (row - 1))
        }
    }
}