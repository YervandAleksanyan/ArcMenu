package com.example.yervand.arcmenu

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.cleveroad.sy.cyclemenuwidget.CycleMenuItem
import com.cleveroad.sy.cyclemenuwidget.CycleMenuWidget
import com.cleveroad.sy.cyclemenuwidget.OnStateChangedListener
import kotlin.random.Random


class MainActivity : AppCompatActivity() {

    private lateinit var cycleWidget: CycleMenuWidget
    private lateinit var cycleWidgetSecond: CycleMenuWidget
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initCycleMenuWidget()
    }


    private fun initCycleMenuWidget() {
        cycleWidget = findViewById(R.id.itemCycleMenuWidget)
        cycleWidgetSecond = findViewById(R.id.itemCycleMenuWidgetSecond)
        cycleWidget.setMenuItems(getColorsDataSet())
        cycleWidgetSecond.setMenuItems(getColorsDataSet())
        cycleWidgetSecond.visibility = View.INVISIBLE
        cycleWidget.setStateChangeListener(object : OnStateChangedListener {
            override fun onStateChanged(state: CycleMenuWidget.STATE) {
                when (state) {
                    CycleMenuWidget.STATE.IN_OPEN_PROCESS -> {
                        cycleWidgetSecond.visibility = View.VISIBLE
                        cycleWidgetSecond.open(true)
                    }

                    CycleMenuWidget.STATE.IN_CLOSE_PROCESS -> {

                    }
                }
            }

            override fun onOpenComplete() {
//                cycleWidgetSecond.open(true)
//                cycleWidgetSecond.visibility = View.VISIBLE

            }

            override fun onCloseComplete() {
                Log.i("tag", "ada")
            }
        })

        cycleWidgetSecond.setStateChangeListener(object : OnStateChangedListener {
            override fun onStateChanged(state: CycleMenuWidget.STATE) {
                when (state) {
                    CycleMenuWidget.STATE.IN_CLOSE_PROCESS -> {
                        cycleWidget.close(true)
                        cycleWidgetSecond.visibility = View.INVISIBLE
                    }
                }
            }

            override fun onOpenComplete() {
                Log.i("tag", "ada")
            }

            override fun onCloseComplete() {
                Log.i("tag", "ada")
            }
        })
    }

    private fun getColorsDataSet(): List<CycleMenuItem> {
        val list = ArrayList<CycleMenuItem>()
        repeat(50) {
            list += CycleMenuItem(null, generateRandomColor())
        }

        return list
    }

    private fun generateRandomColor(): Int =
        Color.argb(255, Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))

}




