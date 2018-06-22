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
        initNumberMode()
//        initGestureMode()
    }

    private fun initGestureMode(){
        patternView = findViewById(R.id.pattern_view)
        patternView.patternType = PatternType.Gesture

        patternView.onPatternDetectedListener = object : UltimatePatternView.OnPatternDetectedListener{
            override fun onPatternDetected(result: String) {
                Toast.makeText(
                        this@MainActivity,
                        "Click: $result",
                        Toast.LENGTH_SHORT).show()
            }

        }
    }

    private fun initNumberMode() {
        patternView = findViewById(R.id.pattern_view)
        patternView.patternType = PatternType.Number
        patternView.registerClickEventByCell(3, 2, object : Cell.ClickListener {
            override fun click(cell: Cell) {
                Toast.makeText(
                        this@MainActivity,
                        "hahah",
                        Toast.LENGTH_SHORT).show()
            }

        })

        patternView.deleteIcon = resources.getDrawable(R.drawable.ic_btn_delete_20dp)

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
