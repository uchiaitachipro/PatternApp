package com.uchia.patternapp

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.uchia.patternview.Cell
import com.uchia.patternview.UltimatePatternView
import com.uchia.patternview.rules.enums.PatternType

class MainActivity : AppCompatActivity() {

    lateinit var patternView: UltimatePatternView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        registerBackwardEvent()
    }

    private fun registerBackwardEvent() {
        patternView = findViewById(R.id.pattern_view)
        patternView.patternType = PatternType.Number
        patternView.registerClickEventByCell(3, 2, object : Cell.ClickListener {
            override fun click(cell: Cell) {
                Toast.makeText(this@MainActivity, "hahah", Toast.LENGTH_SHORT).show()
            }

        })

        patternView.onPatternSelectedListener = object : UltimatePatternView.OnPatternSelectedListener {
            override fun onPatternSelected(content: String?) {
                content?.let {

                    Toast.makeText(
                            this@MainActivity,
                            "Click: $content",
                            Toast.LENGTH_SHORT).show()

                }
            }

        }

    }

}
