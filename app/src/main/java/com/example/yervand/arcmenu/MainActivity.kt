package com.example.yervand.arcmenu

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cleveroad.sy.cyclemenuwidget.CycleMenuItem
import com.cleveroad.sy.cyclemenuwidget.CycleMenuWidget
import kotlin.random.Random


class MainActivity : AppCompatActivity() {

    private lateinit var cycleWidget: CycleMenuWidget
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initCycleMenuWidget()
    }


    private fun initCycleMenuWidget() {
        cycleWidget = findViewById(R.id.itemCycleMenuWidget)
        cycleWidget.setMenuItems(getColorsDataSet())
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




