package com.cleveroad.sy.cyclemenuwidget

/**
 * Callback on changed stet in cycle menu.
 */
interface OnStateChangedListener {

    fun onStateChanged(state: CycleMenuWidget.STATE)

    fun onOpenComplete()

    fun onCloseComplete()

}
