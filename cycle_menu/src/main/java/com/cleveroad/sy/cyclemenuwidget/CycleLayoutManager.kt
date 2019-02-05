package com.cleveroad.sy.cyclemenuwidget

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.util.SparseArray
import android.view.View
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import androidx.recyclerview.widget.RecyclerView
import com.cleveroad.sy.cyclemenuwidget.CycleMenuWidget.CORNER

class CycleLayoutManager(context: Context, corner: CORNER) : RecyclerView.LayoutManager() {

    /**
     * Half of the margin from item side. Is used to calculate item scroll possibility.
     */
    private var mHalfAdditionalMargin = 0

    /**
     * Can disable/enable scrolling. Can be set via setter. Is used in @CycleMenuWidget class
     */
    private var mScrollEnabled = false

    /**
     * Specifies corner which will be set to layout menu cycle
     */
    private var mCurrentCorner = CORNER.RIGHT_TOP

    /**
     * Used to prevent scrolling when measureChildWithMargins called
     */
    private var mCanScroll = true

    /**
     * Calculated margin of each item. Used to calculate animation shift in rollInAnimation.
     */
    private var mMarginAngle: Double = 0.toDouble()

    /**
     * Additional margin for items in preLollipop device.
     * In preLollipop device FloatingActionButton has additional margins from the sides.
     */
    private var mPreLollipopAdditionalButtonsMargin = 0f

    /**
     * View cache that is used to recycler and remove all not used views in fill method.
     */
    private val mViewCache = SparseArray<View>()
    /**
     * Angles of each item view. Used in internalScroll method.
     */
    private val mViewAngles: SparseArray<Double>
    /**
     * Angle that view item has per own diameter.
     */
    private var mAnglePerItem = -1.0
    /**
     * Calculated radius of the cycle menu.
     */
    private var radius = 10
    /**
     * Used to indicate if are there available amount of items for scrolling.
     */
    private var mScrollIsAvailableDueToChildrenCount: Boolean? = null
    /**
     * Predefined position of the first element
     */
    private var mScrollToPosition = RecyclerView.NO_POSITION
    /**
     * Predefined angle shift in degrees of the first element.
     */
    private var mAdditionalAngleOffset = CycleMenuWidget.UNDEFINED_ANGLE_VALUE.toDouble()

    /**
     * Getting anchor view for the filling.
     * The first partially visible item
     */
    private val anchorView: View?
        get() {
            var anchorView: View?
            val childCount = childCount
            if (childCount == 0) {
                return null
            }
            var anchorViewPosition = 0
            if (mCurrentCorner.isLeftSide) {
                do {
                    anchorView = getChildAt(anchorViewPosition)
                    anchorViewPosition++
                } while (anchorView!!.right < 0 && anchorViewPosition < childCount)
            } else {
                do {
                    anchorView = getChildAt(anchorViewPosition)
                    anchorViewPosition++
                } while (anchorView!!.left > width && anchorViewPosition < childCount)
            }

            return anchorView
        }

    /**
     * Get current position of the first item from adapter.
     */
    internal val currentPosition: Int
        get() = if (childCount > 0) {
            getPosition(getChildAt(0)!!)
        } else RecyclerView.NO_POSITION

    /**
     * Getting shift angle of the items in degree.
     */
    internal val currentItemsAngleOffset: Double
        get() = if (childCount > 0) {
            90 - mViewAngles.get(getPosition(getChildAt(0)!!))
        } else 0.0

    /**
     * @return true if count pof item availabkle to scroll
     */
    val isCountOfItemsAvailableToScroll: Boolean
        get() = mScrollIsAvailableDueToChildrenCount == null || mScrollIsAvailableDueToChildrenCount!!

    init {
        mCurrentCorner = corner
        mPreLollipopAdditionalButtonsMargin =
            context.resources.getDimensionPixelSize(R.dimen.cm_prelollipop_additional_margin).toFloat()
        mViewAngles = SparseArray()
    }

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(
            RecyclerView.LayoutParams.MATCH_PARENT,
            RecyclerView.LayoutParams.MATCH_PARENT
        )
    }

    internal fun setCorner(currentCorner: CORNER) {
        mCurrentCorner = currentCorner
    }

    internal fun setScrollEnabled(scrollEnabled: Boolean) {
        mScrollEnabled = scrollEnabled
    }

    override fun canScrollVertically(): Boolean {
        return mCanScroll && mScrollEnabled && (mScrollIsAvailableDueToChildrenCount == null || mScrollIsAvailableDueToChildrenCount!!)
    }

    override fun canScrollHorizontally(): Boolean {
        return mCanScroll && mScrollEnabled && (mScrollIsAvailableDueToChildrenCount == null || mScrollIsAvailableDueToChildrenCount!!)
    }

    override fun scrollHorizontallyBy(dx: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?): Int {
        mScrollToPosition = RecyclerView.NO_POSITION
        return if (mScrollEnabled) {
            internalScrollBy(
                if (mCurrentCorner == CORNER.RIGHT_TOP || mCurrentCorner == CORNER.LEFT_BOTTOM) dx else -dx,
                recycler
            )
        } else 0
    }

    override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?): Int {
        mScrollToPosition = RecyclerView.NO_POSITION
        return if (mScrollEnabled) {
            internalScrollBy(dy, recycler)
        } else 0
    }

    private fun internalScrollBy(dScroll: Int, recycler: RecyclerView.Recycler?): Int {
        val childCount = childCount
        if (childCount == 0) {
            return 0
        }
        mScrollIsAvailableDueToChildrenCount = true
        val delta: Int
        //need to use upToDown calculation if the menu has bottom orientation.
        if (mCurrentCorner.isBottomSide) {
            delta = checkEndsReached(-dScroll)
        } else {
            delta = checkEndsReached(dScroll)
        }

        val radius = radius
        //Length of the circle of the menu.
        val circleLength = 2.0 * Math.PI * radius.toDouble()
        //Approximately calculated angle that menu need to be scrolled on
        val angleToRotate = 360.0 * delta / circleLength

        for (indexOfView in 0 until childCount) {
            val view = getChildAt(indexOfView)
            val viewPosition = getPosition(view!!)
            //Save new angle of the view item
            mViewAngles.put(viewPosition, angleToRotate + mViewAngles.get(viewPosition))

            //current position of the view item
            val viewCenterX = view.right - view.width / 2.0
            val viewCenterY = view.top + view.height / 2.0

            //new position for the view item
            var newCenterX = radius * Math.cos(mViewAngles.get(viewPosition) * Math.PI / 180)
            var newCenterY = radius * Math.sin(mViewAngles.get(viewPosition) * Math.PI / 180)

            if (mCurrentCorner == CORNER.RIGHT_TOP) {
                newCenterX = width - newCenterX
            } else if (mCurrentCorner == CORNER.LEFT_BOTTOM) {
                newCenterY = height - newCenterY
            } else if (mCurrentCorner == CORNER.RIGHT_BOTTOM) {
                newCenterX = width - newCenterX
                newCenterY = height - newCenterY
            }

            val dx = Math.round(newCenterX - viewCenterX).toInt()
            val dy = Math.round(newCenterY - viewCenterY).toInt()

            view.offsetTopAndBottom(dy)
            view.offsetLeftAndRight(dx)
        }
        //refill items after scroll
        fill(recycler)
        //need to use upToDown calculation if the menu has bottom orientation.
        return if (mCurrentCorner.isBottomSide) {
            delta
        } else -delta
    }

    /**
     * Method to check if the end is reached with scrolling
     *
     * @param dy value to scroll
     * @return available value to scroll.
     */
    private fun checkEndsReached(dy: Int): Int {
        val childCount = childCount
        val itemCount = itemCount
        if (childCount == 0) {
            return 0
        }

        var delta = 0
        val firstChildView = getChildAt(0)
        val lastChildView = getChildAt(childCount - 1)

        if (dy < 0) { //scroll to bottom if menu corner is top side, to up if menu corner is bottom side
            if (getPosition(lastChildView!!) < itemCount - 1) { //if last item not reached
                delta = dy
            } else { //if last item reached
                if (mCurrentCorner.isBottomSide) { //scroll from bottom to up
                    val viewBottom = getDecoratedBottom(lastChildView)
                    val parentBottom = height
                    delta = Math.max(parentBottom - mHalfAdditionalMargin - viewBottom, dy)
                } else { //scroll from up to down
                    val viewTop = getDecoratedTop(lastChildView)
                    delta = Math.max(viewTop - mHalfAdditionalMargin, dy)
                }
            }
        } else if (dy > 0) { //scroll to up if menu corner is top side, to bottom if menu corner is bottom side
            if (getPosition(firstChildView!!) > 0) { //if first item not reached
                delta = dy
            } else {
                //if first item reached
                if (mCurrentCorner.isLeftSide) {
                    val viewLeft = getDecoratedLeft(firstChildView)
                    val parentLeft = 0
                    delta = Math.min(parentLeft - viewLeft + mHalfAdditionalMargin, dy)
                } else {
                    val viewRight = getDecoratedRight(firstChildView)
                    val parentRight = width
                    delta = Math.min(viewRight + mHalfAdditionalMargin - parentRight, dy)
                }
            }
        }
        return -delta
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        mAnglePerItem = -1.0
        detachAndScrapAttachedViews(recycler!!)
        if (width > 0 && height > 0 && width < 10000 && height < 10000) {
            fill(recycler)
        }
    }

    fun fill(recycler: RecyclerView.Recycler?) {
        val anchorView = anchorView
        mViewCache.clear()

        run {
            var i = 0
            val cnt = childCount
            while (i < cnt) {
                val view = getChildAt(i)
                val pos = getPosition(view!!)
                mViewCache.put(pos, view)
                i++
            }
        }

        for (i in 0 until mViewCache.size()) {
            detachView(mViewCache.valueAt(i))
        }
        fillUp(anchorView, recycler)
        fillDown(anchorView, recycler)

        for (i in 0 until mViewCache.size()) {
            recycler!!.recycleView(mViewCache.valueAt(i))
        }
    }

    /**
     * fill to up items from the anchor item
     *
     * @param anchorView
     * @param recycler
     */
    private fun fillUp(anchorView: View?, recycler: RecyclerView.Recycler?) {
        val anchorPos: Int
        if (anchorView != null) {
            anchorPos = getPosition(anchorView)
        } else {
            return
        }

        var pos = anchorPos - 1
        if (mScrollToPosition != RecyclerView.NO_POSITION) {
            pos = mScrollToPosition - 1
        }
        var canFillUp: Boolean
        val radius = radius
        var angle: Double
        if (mCurrentCorner.isLeftSide) {
            canFillUp = anchorView.left > 0
        } else {
            canFillUp = anchorView.right < width
        }
        angle = mViewAngles.get(anchorPos) + mAnglePerItem
        //Can be used View.MeasureSpec.AT_MOST because items is floating action buttons
        val widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.AT_MOST)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.AT_MOST)

        var top: Int
        var bottom: Int
        var right: Int
        var left: Int

        while (canFillUp && pos >= 0) {
            var view: View? = mViewCache.get(pos)

            if (view == null) {
                mViewAngles.put(pos, angle)
                view = recycler!!.getViewForPosition(pos)
                addView(view, 0)
                measureChildWithMargins(view, widthSpec, heightSpec)
                val decoratedMeasuredWidth = getDecoratedMeasuredWidth(view)
                val decoratedMeasuredHeight = getDecoratedMeasuredHeight(view)

                //position of the new item
                val xDistance = (radius * Math.cos(angle * Math.PI / 180)).toInt()
                val yDistance = (radius * Math.sin(angle * Math.PI / 180)).toInt()

                left = xDistance - decoratedMeasuredWidth / 2
                right = xDistance + decoratedMeasuredWidth / 2
                top = yDistance - decoratedMeasuredHeight / 2
                bottom = yDistance + decoratedMeasuredHeight / 2

                //changes for each corners except left_top
                if (mCurrentCorner == CORNER.RIGHT_TOP) {
                    left = width - xDistance - decoratedMeasuredWidth / 2
                    right = width - xDistance + decoratedMeasuredWidth / 2
                } else if (mCurrentCorner == CORNER.LEFT_BOTTOM) {
                    top = height - yDistance - decoratedMeasuredHeight / 2
                    bottom = height - yDistance + decoratedMeasuredHeight / 2
                } else if (mCurrentCorner == CORNER.RIGHT_BOTTOM) {
                    left = width - xDistance - decoratedMeasuredWidth / 2
                    right = width - xDistance + decoratedMeasuredWidth / 2
                    top = height - yDistance - decoratedMeasuredHeight / 2
                    bottom = height - yDistance + decoratedMeasuredHeight / 2
                }

                layoutDecorated(view, left, top, right, bottom)

            } else {
                attachView(view)
                mViewCache.remove(pos)
                left = view.left
                right = view.right
            }
            pos--
            //Check if top not reached
            if (mCurrentCorner.isLeftSide) {
                canFillUp = left > 0
            } else if (mCurrentCorner.isRightSide) {
                canFillUp = right < width
            }
            angle += mAnglePerItem

        }
    }

    /**
     * fill to up down from the anchor item
     *
     * @param anchorView
     * @param recycler
     */
    private fun fillDown(anchorView: View?, recycler: RecyclerView.Recycler?) {

        var anchorPos = 0
        if (anchorView != null) {
            anchorPos = getPosition(anchorView)
        }
        var pos = anchorPos
        if (mScrollToPosition != RecyclerView.NO_POSITION) {
            pos = mScrollToPosition
        }
        var canFillDown = true
        val itemCount = itemCount
        //Can be used View.MeasureSpec.AT_MOST because items is floating action buttons
        val widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.AT_MOST)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.AT_MOST)

        var angle = 90.0
        if (anchorView != null) {
            angle = mViewAngles.get(pos)
        }
        var left: Int
        var top: Int
        var right: Int
        var bottom: Int
        while (canFillDown && pos < itemCount) {
            var view: View? = mViewCache.get(pos)
            if (view == null) {
                view = recycler!!.getViewForPosition(pos)
                addView(view)
                measureChildWithMargins(view, widthSpec, heightSpec)
                val decoratedMeasuredWidth = getDecoratedMeasuredWidth(view)
                val decoratedMeasuredHeight = getDecoratedMeasuredHeight(view)
                if (mAnglePerItem < 0) { //if not initialized
                    //calculate and set radius of the menu
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        radius = (if (width > height) height else width) - decoratedMeasuredHeight * 4 / 5
                    } else {
                        radius = (if (width > height) height else width) - decoratedMeasuredHeight / 2
                    }
                    //Calculate margins between the items.
                    val circleLength = 2.0 * Math.PI * radius.toDouble()
                    val anglePerLength: Double
                    val anglePerLengthWithMargins: Double
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        anglePerLength = 360.0 * decoratedMeasuredHeight / circleLength
                        anglePerLengthWithMargins = anglePerLength * SCALING_COEFFICIENT
                        mMarginAngle = (anglePerLengthWithMargins - anglePerLength) / 2.0
                        mHalfAdditionalMargin =
                            ((decoratedMeasuredHeight * SCALING_COEFFICIENT - decoratedMeasuredHeight) / 2.0).toInt()
                    } else {
                        //In preLollipop android floatingActionButton has additional margin.
                        anglePerLength =
                            360.0 * (decoratedMeasuredHeight - mPreLollipopAdditionalButtonsMargin * 2) / circleLength
                        anglePerLengthWithMargins =
                            360.0 * (decoratedMeasuredHeight - mPreLollipopAdditionalButtonsMargin * 2 / 1.5f) / circleLength
                        mMarginAngle = (anglePerLengthWithMargins - anglePerLength) / 2.0
                    }
                    if (mAdditionalAngleOffset < -999) {
                        angle -= anglePerLengthWithMargins / 2.0
                    } else {
                        angle -= mAdditionalAngleOffset
                    }
                    mAnglePerItem = anglePerLengthWithMargins
                }
                mViewAngles.put(pos, angle)
                val xDistance = (radius * Math.cos(angle * Math.PI / 180)).toInt()
                val yDistance = (radius * Math.sin(angle * Math.PI / 180)).toInt()

                left = xDistance - decoratedMeasuredWidth / 2
                right = xDistance + decoratedMeasuredWidth / 2
                top = yDistance - decoratedMeasuredHeight / 2
                bottom = yDistance + decoratedMeasuredHeight / 2
                if (mCurrentCorner == CORNER.RIGHT_TOP) {
                    left = width - xDistance - decoratedMeasuredWidth / 2
                    right = width - xDistance + decoratedMeasuredWidth / 2
                } else if (mCurrentCorner == CORNER.LEFT_BOTTOM) {
                    top = height - yDistance - decoratedMeasuredHeight / 2
                    bottom = height - yDistance + decoratedMeasuredHeight / 2
                } else if (mCurrentCorner == CORNER.RIGHT_BOTTOM) {
                    left = width - xDistance - decoratedMeasuredWidth / 2
                    right = width - xDistance + decoratedMeasuredWidth / 2
                    top = height - yDistance - decoratedMeasuredHeight / 2
                    bottom = height - yDistance + decoratedMeasuredHeight / 2
                }

                layoutDecorated(view, left, top, right, bottom)
            } else {

                attachView(view)
                mViewCache.remove(pos)
                top = view.top
                bottom = view.bottom
            }

            if (mCurrentCorner.isUpSide) {
                canFillDown = top > 0
            } else {
                canFillDown = bottom < height
            }
            pos++
            if (pos == itemCount && mScrollIsAvailableDueToChildrenCount == null) {
                mScrollIsAvailableDueToChildrenCount = !canFillDown
            }
            angle -= mAnglePerItem
        }
    }

    override fun measureChildWithMargins(child: View, widthSpec: Int, heightSpec: Int) {
        // change a value to "false "temporary while measuring
        mCanScroll = false

        val decorRect = Rect()
        calculateItemDecorationsForChild(child, decorRect)
        val lp = child.layoutParams as RecyclerView.LayoutParams

        val lWidthSpec =
            updateSpecWithExtra(widthSpec, lp.leftMargin + decorRect.left, lp.rightMargin + decorRect.right)
        val lHeightSpec =
            updateSpecWithExtra(heightSpec, lp.topMargin + decorRect.top, lp.bottomMargin + decorRect.bottom)
        child.measure(lWidthSpec, lHeightSpec)

        // return a value to "true" because we do actually can scroll in both ways
        mCanScroll = true
    }

    private fun updateSpecWithExtra(spec: Int, startInset: Int, endInset: Int): Int {
        if (startInset == 0 && endInset == 0) {
            return spec
        }
        val mode = View.MeasureSpec.getMode(spec)
        return if (mode == View.MeasureSpec.AT_MOST || mode == View.MeasureSpec.EXACTLY) {
            View.MeasureSpec.makeMeasureSpec(
                View.MeasureSpec.getSize(spec) - startInset - endInset, mode
            )
        } else spec
    }

    /**
     * Set shift angle of the items in degree.
     */
    internal fun setAdditionalAngleOffset(additionalAngleOffset: Double) {
        mAdditionalAngleOffset = additionalAngleOffset
    }


    internal fun rollInItemsWithAnimation(callback: OnCompleteCallback) {
        val childCount = childCount
        if (childCount == 0) {
            callback.onComplete()
            return
        }
        var overshootCoefficient = 6
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            overshootCoefficient = 6
        }
        val duration = 300

        val startOffset = duration / childCount

        for (i in 0 until childCount) {
            val view = getChildAt(i)
            var animationRotateAnchorX = 0f
            var animationRotateAnchorY = 0f
            var startDegree = 100f
            var overshootDegree = ((i + overshootCoefficient).toDouble() * mMarginAngle * 2.0).toFloat()
            if (mCurrentCorner == CORNER.LEFT_TOP) {
                startDegree = -100f
                animationRotateAnchorX = (-view!!.left).toFloat()
                animationRotateAnchorY = (-view.top).toFloat()
            } else if (mCurrentCorner == CORNER.RIGHT_TOP) {
                startDegree = 100f
                overshootDegree = -overshootDegree
                animationRotateAnchorX = (width - view!!.left).toFloat()
                animationRotateAnchorY = (-view.top).toFloat()
            } else if (mCurrentCorner == CORNER.LEFT_BOTTOM) {
                startDegree = 100f
                overshootDegree = -overshootDegree
                animationRotateAnchorX = (-view!!.left).toFloat()
                animationRotateAnchorY = (height - view.top).toFloat()
            } else if (mCurrentCorner == CORNER.RIGHT_BOTTOM) {
                startDegree = -100f
                animationRotateAnchorX = (width - view!!.left).toFloat()
                animationRotateAnchorY = (height - view.top).toFloat()
            }

            val rotateAnimation = RotateAnimation(
                startDegree,
                overshootDegree,
                Animation.ABSOLUTE,
                animationRotateAnchorX,
                Animation.ABSOLUTE,
                animationRotateAnchorY
            )
            rotateAnimation.duration = duration.toLong()
            rotateAnimation.startOffset = (startOffset * i / 2).toLong()
            rotateAnimation.fillBefore = true
            rotateAnimation.interpolator = DecelerateInterpolator()

            val backRotateAnimation = RotateAnimation(
                overshootDegree,
                0f,
                Animation.ABSOLUTE,
                animationRotateAnchorX,
                Animation.ABSOLUTE,
                animationRotateAnchorY
            )
            backRotateAnimation.duration = ((i + overshootCoefficient) * startOffset / 2).toLong()
            backRotateAnimation.fillBefore = true
            backRotateAnimation.interpolator = LinearInterpolator()

            backRotateAnimation.startOffset = ((getChildCount() - i - 1) * startOffset / 2).toLong()
            rotateAnimation.setAnimationListener(object : AnimationListenerAdapter() {
                override fun onAnimationEnd(animation: Animation) {
                    if (i == childCount - 1) {
                        backRotateAnimation.setAnimationListener(object : AnimationListenerAdapter() {

                            override fun onAnimationEnd(animation: Animation) {
                                callback.onComplete()
                            }
                        })
                    }
                    view!!.startAnimation(backRotateAnimation)
                }
            })
            view!!.startAnimation(rotateAnimation)
        }
    }

    internal fun rollOutItemsWithAnimation(callback: OnCompleteCallback) {
        if (childCount == 0) {
            callback.onComplete()
            return
        }
        val duration = 300

        val startOffset = 50
        for (i in childCount - 1 downTo 0) {

            val view = getChildAt(i)
            view!!.clearAnimation()
            var animationRotateAnchorX = 0f
            var animationRotateAnchorY = 0f
            var overshootDegree = 0f
            if (mCurrentCorner == CORNER.LEFT_TOP) {
                overshootDegree = -100f
                animationRotateAnchorX = (-view.left).toFloat()
                animationRotateAnchorY = (-view.top).toFloat()
            } else if (mCurrentCorner == CORNER.RIGHT_TOP) {
                overshootDegree = 100f
                animationRotateAnchorX = (width - view.left).toFloat()
                animationRotateAnchorY = (-view.top).toFloat()
            } else if (mCurrentCorner == CORNER.LEFT_BOTTOM) {
                overshootDegree = 100f
                animationRotateAnchorX = (-view.left).toFloat()
                animationRotateAnchorY = (height - view.top).toFloat()
            } else if (mCurrentCorner == CORNER.RIGHT_BOTTOM) {
                overshootDegree = -100f
                animationRotateAnchorX = (width - view.left).toFloat()
                animationRotateAnchorY = (height - view.top).toFloat()
            }

            val rotateAnimation = RotateAnimation(
                0f,
                overshootDegree,
                Animation.ABSOLUTE,
                animationRotateAnchorX,
                Animation.ABSOLUTE,
                animationRotateAnchorY
            )
            rotateAnimation.duration = duration.toLong()
            rotateAnimation.startOffset = (startOffset * (childCount - i - 1)).toLong()
            rotateAnimation.fillAfter = true
            rotateAnimation.interpolator = DecelerateInterpolator()

            if (i == 0) {
                rotateAnimation.setAnimationListener(object : AnimationListenerAdapter() {
                    override fun onAnimationEnd(animation: Animation) {
                        callback.onComplete()
                    }
                })
            }
            view.startAnimation(rotateAnimation)
        }

    }

    override fun scrollToPosition(position: Int) {
        mScrollToPosition = position
        requestLayout()
    }

    internal interface OnCompleteCallback {

        fun onComplete()

    }

    companion object {

        /**
         * Scaling coefficient that is used for increasing spaces between two items in lollipop
         */
        private val SCALING_COEFFICIENT = 1.3
    }
}
