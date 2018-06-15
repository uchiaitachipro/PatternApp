package com.uchia.patternview.rules

import android.view.View

class GesturePattern(row : Int, col :Int, hostView : View)
    : AbsPatternRule(row,col,hostView) {

    override fun needDrawLine(): Boolean = true

    override fun getDrawRule(): IDrawRule {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }



}