package com.cleveroad.sy.cyclemenuwidget

import android.graphics.drawable.Drawable

/**
 * Model class for menu items
 */
data class CycleMenuItem @JvmOverloads constructor(
    var icon: Drawable? = null,
    var id: Int = -1,
    var color: Int = -1
)
