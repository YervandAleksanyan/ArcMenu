package com.cleveroad.sy.cyclemenuwidget

import android.view.View

/**
 * Listener for menu items clicks
 */
interface OnMenuItemClickListener {
    fun onMenuItemClick(view: View, itemPosition: Int)

    fun onMenuItemLongClick(view: View, itemPosition: Int)
}
