package com.uchia.patternapp

import android.graphics.Canvas
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.TextView
import com.uchia.patternview.rules.AbsPatternRule
import com.uchia.patternview.rules.IDrawRule

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<TextView>(R.id.text).text = printCell(4,3)
    }

    fun printCell(row: Int, col: Int) : String {

        val drawRule = object : IDrawRule {
            override fun draw(canvas: Canvas, leftX: Int, topY: Int, partOfPattern: Boolean) {
            }
        }

        val obj = object : AbsPatternRule(row, col, View(this)) {
            override fun needDrawLine(): Boolean = false

            override fun getDrawRule(): IDrawRule = drawRule

        }

        val sb = StringBuilder()
        for (r in 0 until row){

            for (c in 0 until col){
                val cell = obj.getCell(r,c)
                sb.append(cell.toString())
                sb.append("\t")
            }
            sb.append("\n")
        }

        return sb.toString()
    }

}
