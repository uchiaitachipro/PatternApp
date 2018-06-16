package com.uchia.patternview.rules

import android.graphics.Canvas
import com.uchia.patternview.Cell
import java.util.ArrayList

interface IDrawRule {

    fun draw(canvas: Canvas, pattern : ArrayList<Cell>)

}