package com.cleveroad.sy.cyclemenuwidget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.core.view.MotionEventCompat
import androidx.recyclerview.widget.RecyclerView

/**
 * TouchedRecyclerView are used as RecycleView that allow to control touch interception.
 */
internal class TouchedRecyclerView : RecyclerView {

    private var mTouchEnabled = true
    private var mHasItemsToScroll = true
    private var lastX: Float = 0.toFloat()
    private var lastY: Float = 0.toFloat()
    private var mIsScrolling: Boolean = false

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {}

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {}

    fun setTouchEnabled(touchEnabled: Boolean) {
        mTouchEnabled = touchEnabled
    }

    fun setHasItemsToScroll(hasItemsToScroll: Boolean) {
        mHasItemsToScroll = hasItemsToScroll
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {

        if (mTouchEnabled) {
            return super.onInterceptTouchEvent(ev)
        }

        if (!mHasItemsToScroll) {
            return mTouchEnabled
        }
        //if we haven't enough elements to scroll need to intercept touch but do not intercepot scroll.

        val vc = ViewConfiguration.get(context)
        val slop = vc.scaledTouchSlop
        val action = MotionEventCompat.getActionMasked(ev)

        // Always handle the case of the touch gesture being complete.
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            // Release the scroll.
            mIsScrolling = false
            return false // Do not intercept touch event, let the child handle it
        }

        // If the user has dragged her finger horizontally more than
        // the touch slop, start the scroll
        // left as an exercise for the reader
        // Touch slop should be calculated using ViewConfiguration
        // constants.
        if (action == MotionEvent.ACTION_MOVE) {
            if (mIsScrolling) {
                // We're currently scrolling, so yes, intercept the
                // touch event!
                return true
            }
            val diff = Math.sqrt(((ev.x - lastX) * (ev.x - lastX) + (ev.y - lastY) * (ev.y - lastY)).toDouble())
            lastX = ev.x
            lastY = ev.y
            if (diff > slop) {
                // Start scrolling!
                mIsScrolling = true
                return true
            }
        }

        // In general, we don't want to intercept touch events. They should be
        // handled by the child view.
        return false

    }

    override fun onTouchEvent(e: MotionEvent): Boolean {

        if (mTouchEnabled && mHasItemsToScroll) {
            parent.requestDisallowInterceptTouchEvent(true)
            return super.onTouchEvent(e)
        }
        return mTouchEnabled
    }
}
