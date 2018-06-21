package com.uchia.patternview.anims

import android.annotation.TargetApi
import android.graphics.Canvas
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.util.StateSet
import android.view.View

/**
 * Created by uchia on 2018/6/21.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
internal class RippleClickAnimHelper : IClickAnim {

    private val normalState = StateSet.WILD_CARD
    private val clickState = intArrayOf(
            android.R.attr.state_enabled,
            android.R.attr.state_pressed)


    private var hostView : View

    constructor(view : View){
        hostView = view
    }

    var drawable: RippleDrawable? = null
    set(value) {
        field = value
        field?.state = normalState
        field?.callback = hostView
    }

    override fun setClickBounds(left: Int, top: Int, right: Int, bottom: Int){
//        drawable?.setHotspotBounds(left,top,right,bottom)
        drawable?.setBounds(left,top,right,bottom)
    }

    override fun setHotspot(x : Float,y : Float){
        drawable?.setHotspot(x,y)
    }

    override fun draw(canvas : Canvas){
        drawable?.draw(canvas)
    }

    override fun setClickState(result : Boolean) {
        if (result){
            drawable?.state = clickState
        } else {
            drawable?.state = normalState
        }
    }

}