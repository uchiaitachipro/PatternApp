package com.uchia.patternview

import android.content.Context
import android.graphics.drawable.Drawable
import com.uchia.patternview.anims.IClickAnim

/**
 * Created by uchia on 2018/6/16.
 */
interface IPatternView {

    var gridColumns: Int
    var gridRows: Int

    var numberTextSize : Float
    var pathWidth : Float
    var squareWidth : Float
    var squareHeight : Float
    var inProgressX : Float
    var inProgressY : Float
    var leftRealPadding : Int
    var topRealPadding : Int
    var rightRealPadding : Int
    var bottomRealPadding : Int

    var animatingPeriodStart : Long

    val hostContext : Context

    var deleteIcon : Drawable?

//    var clickAnimHelper : IClickAnim?

    fun getPaddingTop() : Int

    fun getPaddingLeft() : Int

    fun getHostViewWidth() : Int

    fun getHostViewHeight() : Int

    fun isHostInEditMode() : Boolean

    fun getCenterXForColumn(column: Int) : Float

    fun getCenterYForRow(row: Int) : Float

    fun invalidate()

}