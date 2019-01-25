package com.example.yervand.arcmenu

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ShapeDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView


class ColorListAdapter(private val colorList: List<Int>) : RecyclerView.Adapter<ColorListViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorListViewHolder =
        ColorListViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.color_cell, parent, false))

    override fun getItemCount(): Int = colorList.size


    override fun onBindViewHolder(holder: ColorListViewHolder, position: Int) {
        val color = colorList[position]
        val background = holder.colorView.background
        when (background) {
            is ShapeDrawable -> background.paint.color = color
            is GradientDrawable -> background.setColor(color)
            is ColorDrawable -> background.color = color
        }
    }
}