package com.example.yervand.arcmenu

import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.leochuan.CircleLayoutManager
import com.leochuan.CircleLayoutManager.BOTTOM_RIGHT
import com.ogaclejapan.arclayout.ArcLayout
import kotlin.random.Random


class MainActivity : AppCompatActivity() {

    private lateinit var colorListRecyclerView: RecyclerView
    private lateinit var arcLayout: ArcLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initColorListRecyclerView()
        initColorListAdapter()
    }

    private fun initColorListRecyclerView() {
        colorListRecyclerView = findViewById(R.id.colors_rv)
        val manager = CircleLayoutManager.Builder(this)
            .setRadius(800)
            .setAngleInterval(10)
            .setGravity(BOTTOM_RIGHT)
            .setDistanceToBottom(800)
            .build()

        manager.moveSpeed = (5F * 0.005F)
        colorListRecyclerView.layoutManager = manager
        manager.infinite = true
    }

    private fun initColorListAdapter() {
        val adapter = ColorListAdapter(getColorsDataSet())
        colorListRecyclerView.adapter = adapter
    }

    private fun getColorsDataSet(): List<Int> {
        val list = ArrayList<Int>()
        repeat(50) {
            list += generateRandomColor()
        }

        return list
    }

    private fun generateRandomColor(): Int =
        Color.argb(255, Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))


}

fun convertPixelsToDp(px: Float): Float {
    val metrics = Resources.getSystem().getDisplayMetrics()
    val dp = px / (metrics.densityDpi / 160f)
    return Math.round(dp).toFloat()
}

fun convertDpToPixel(dp: Float): Float {
    val metrics = Resources.getSystem().getDisplayMetrics()
    val px = dp * (metrics.densityDpi / 160f)
    return Math.round(px).toFloat()
}



