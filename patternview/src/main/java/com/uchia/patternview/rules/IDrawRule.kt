package com.uchia.patternview.rules

import android.graphics.Canvas

interface IDrawRule {

    fun draw(canvas : Canvas, leftX : Int, topY : Int, partOfPattern : Boolean)

}