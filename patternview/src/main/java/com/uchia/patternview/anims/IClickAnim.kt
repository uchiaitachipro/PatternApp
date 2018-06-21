package com.uchia.patternview.anims

import android.graphics.Canvas

/**
 * Created by uchia on 2018/6/21.
 */
interface IClickAnim {

    fun setClickBounds(left: Int, top: Int, right: Int, bottom: Int)
    fun setHotspot(x : Float,y : Float)
    fun draw(canvas : Canvas)

    fun setClickState(state : Boolean)

}