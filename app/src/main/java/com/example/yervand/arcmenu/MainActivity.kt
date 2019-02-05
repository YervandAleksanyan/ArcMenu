package com.example.yervand.arcmenu

import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cleveroad.sy.cyclemenuwidget.CycleMenuItem
import com.cleveroad.sy.cyclemenuwidget.CycleMenuWidget
import com.kapil.circularlayoutmanager.CircularLayoutManager
import com.ogaclejapan.arclayout.ArcLayout
import kotlin.random.Random


class MainActivity : AppCompatActivity() {

    //    private lateinit var colorListRecyclerView: RecyclerView
    private lateinit var cycleWidget: CycleMenuWidget
    private lateinit var arcLayout: ArcLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initColorListRecyclerView()
        initColorListAdapter()
    }


    private fun initColorListRecyclerView() {
//        colorListRecyclerView = findViewById(R.id.colors_rv)
        cycleWidget = findViewById(R.id.itemCycleMenuWidget)
//        val menu  = MenuInflater(this).inflate(R.menu.menu,menu)
//        cycleWidget.setMenu()
//        cycleWidget.setMenuItems(getColorsDataSet())
        cycleWidget.setMenuRes(R.menu.menu)
//        val manager = com.leochuan.other.CircleLayoutManager(this)
//        manager.radius = 600
//        manager.setDegreeRangeWillShow(270, 0)
//        manager.contentOffsetY = 300
//        manager.intervalAngle = 100
        val manager = CircularLayoutManager(this, 200, 0)
//        colorListRecyclerView.layoutManager = manager
    }

    private fun initColorListAdapter() {
//        val adapter = ColorListAdapter(getColorsDataSet())
//        colorListRecyclerView.adapter = adapter
    }

    private fun getColorsDataSet(): List<CycleMenuItem> {
        val list = ArrayList<CycleMenuItem>()
        repeat(50) {
            list += CycleMenuItem(generateRandomColor())
        }

        return list
    }

    private fun generateRandomColor(): Int =
        Color.argb(255, Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))


}

fun convertPixelsToDp(px: Float): Float {
    val metrics = Resources.getSystem().displayMetrics
    val dp = px / (metrics.densityDpi / 160f)
    return Math.round(dp).toFloat()
}

fun convertDpToPixel(dp: Float): Float {
    val metrics = Resources.getSystem().displayMetrics
    val px = dp * (metrics.densityDpi / 160f)
    return Math.round(px).toFloat()
}



