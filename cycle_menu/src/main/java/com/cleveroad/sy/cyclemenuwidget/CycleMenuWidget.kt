package com.cleveroad.sy.cyclemenuwidget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.view.*
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.MenuRes
import androidx.appcompat.view.menu.MenuBuilder
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class CycleMenuWidget : ViewGroup {

    private var mState = STATE.CLOSED

    private var mCorner = CORNER.RIGHT_TOP

    private var mScalingType = RADIUS_SCALING_TYPE.AUTO

    private var mScrollType = SCROLL.ENDLESS

    private var mOnStateChangeListener: OnStateChangedListener? = null
    private var mStateSaveListener: StateSaveListener? = null

    private var mShouldOpen = false
    private var mShadowSize = 40f
    private var mPreLollipopAdditionalButtonsMargin = 0f
    private var mVariableShadowSize = 45f
    private var mOutCircleRadius = 0

    /**
     * Colors for the shadow gradient
     */
    private var mShadowStartColor: Int = 0
    private var mShadowMiddleColor: Int = 0
    private var mShadowEndColor: Int = 0

    /**
     * CycleMenuWidget background color
     */
    private var mBackgroundColor: Int = 0

    /**
     * Paint for background circle
     */
    private var mCirclePaint: Paint? = null
    /**
     * Paint ripple circles that appears when user touch central image button (in the corner).
     */
    private var mRipplePaint: Paint? = null
    /**
     * Color for the ripple effect of touching central image button (in the corner)
     */
    private var mRippleColor = DEFAULT_UNDEFINED_VALUE
    /**
     * Radius for drawing ripple effect
     */
    private var mRippleRadius: Int = 0

    /**
     * Paint shadow around background circle
     */
    private var mCornerShadowPaint: Paint? = null
    /**
     * Path for the shadow around the background circle
     */
    private var mCornerShadowPath: Path? = null

    /**
     * Minimal circle radius for the background
     */
    private var mCircleMinRadius = DEFAULT_UNDEFINED_VALUE
    /**
     * Radius for drawing background circle
     */
    private var mAnimationCircleRadius = 200

    /**
     * Position from the adapter of the current first item.
     */
    private var mCurrentPosition = RecyclerView.NO_POSITION

    /**
     * Angle in degrees from the layout manager to be saved if holder with cycleMenuWidget will be reused.
     */
    private var mCurrentAngleOffset = UNDEFINED_ANGLE_VALUE.toDouble()

    /**
     * Size of the one item element
     */
    private var mItemSize = -1
    /**
     * Size of inner menu recycler view
     */
    private var mRecyclerSize = -1

    /**
     * Image for the image button placed in the corner.
     */
    private var mCenterImage: ImageView? = null
    /**
     * Recycler view which contains items of the menu
     */
    private var mRecyclerView: TouchedRecyclerView? = null
    /**
     * Layout manager that place items in the circular way
     */
    private var mLayoutManager: CycleLayoutManager? = null

    /**
     * Background tint for the items
     */
    private var mItemsBackgroundTint: ColorStateList? = null

    /**
     * Radiuses to be used to calculate real radius of the items recyclerView
     */
    private var mAutoMinRadius = 0
    private var mAutoMaxRadius = 0
    private var mFixedRadius = 0

    /**
     * State of the layout manager and recyclerMenuAdapter
     */
    private var mInitialized = false

    /**
     * Adapter for recycler view which will be produce items for the circular menu
     */
    private var mAdapter: RecyclerMenuAdapter? = null

    /**
     * When widget is used in the recyclerView item. It need to be requested to relayout itself.
     * runnableRequestLayout is used for that reason
     */
    private val runnableRequestLayout = Runnable { mLayoutManager!!.requestLayout() }

    /**
     * Retrieve current position from the menu
     *
     * @return position of the first item
     */
    /**
     * Set current position of the menu to be first
     *
     * @param position - position of the first item
     */
    private var currentPosition: Int
        get() = mLayoutManager!!.currentPosition
        set(position) {
            if (position != RecyclerView.NO_POSITION) {
                mCurrentPosition = position
            }
        }

    /**
     * Retrieve current offset as an angle (degree) of the first item
     *
     * @return offset as an angle of the first angle
     */
    /**
     * Set current offset of the firstItem as an angle (in degrees)
     *
     * @param angle - offset that need to set for the first item (and next) in degrees
     */
    private var currentItemsAngleOffset: Double
        get() = mCurrentAngleOffset
        set(angle) {
            mCurrentAngleOffset = angle
            mLayoutManager!!.setAdditionalAngleOffset(angle)
        }

    /**
     * Specifies states of cycle menu widget. If mState is IN_OPEN_PROCESS or IN_CLOSE_PROCESS then clicks will not be handled.
     */
    enum class STATE {
        OPEN, CLOSED, IN_OPEN_PROCESS, IN_CLOSE_PROCESS
    }

    /**
     * Specifies corner which will be set to layout menu cycle
     */
    enum class CORNER private constructor(private val mInnerCorner: INNER_CORNER) {
        LEFT_TOP(INNER_CORNER.LEFT_TOP),
        RIGHT_TOP(INNER_CORNER.RIGHT_TOP),
        LEFT_BOTTOM(INNER_CORNER.LEFT_BOTTOM),
        RIGHT_BOTTOM(INNER_CORNER.RIGHT_BOTTOM);

        val isLeftSide: Boolean
            get() = mInnerCorner == INNER_CORNER.LEFT_TOP || mInnerCorner == INNER_CORNER.LEFT_BOTTOM

        val isUpSide: Boolean
            get() = mInnerCorner == INNER_CORNER.LEFT_TOP || mInnerCorner == INNER_CORNER.RIGHT_TOP

        val isBottomSide: Boolean
            get() = mInnerCorner == INNER_CORNER.LEFT_BOTTOM || mInnerCorner == INNER_CORNER.RIGHT_BOTTOM

        val isRightSide: Boolean
            get() = mInnerCorner == INNER_CORNER.RIGHT_TOP || mInnerCorner == INNER_CORNER.RIGHT_BOTTOM

        val value: Int
            get() = mInnerCorner.value

        /**
         * Specified for checking for side inside the parent enum
         */
        internal enum class INNER_CORNER private constructor(val value: Int) {
            LEFT_TOP(0), RIGHT_TOP(1), LEFT_BOTTOM(2), RIGHT_BOTTOM(3)

        }

        companion object {

            fun valueOf(value: Int): CORNER {
                when (value) {
                    0 -> return LEFT_TOP
                    2 -> return LEFT_BOTTOM
                    3 -> return RIGHT_BOTTOM
                    1 -> return RIGHT_TOP
                    else -> return RIGHT_TOP
                }
            }
        }
    }

    /**
     * Specified radius scaling type.
     * If AUTO then size can be increased to the max_auto_radius_size if there is a lot of items in menu
     * or decreased to the min_auto_radius_size if the count of items is little.
     * If FIXED then radius will be set exactly to the value specified in fixed_radius.
     * If fixed radius is bigger the available size of widget it will be decreased.
     */
    enum class RADIUS_SCALING_TYPE private constructor(val value: Int) {
        AUTO(0),
        FIXED(1);


        companion object {

            fun valueOf(value: Int): RADIUS_SCALING_TYPE {
                return if (value == 0) {
                    AUTO
                } else FIXED
            }
        }
    }

    /**
     * The scroll type specified if the scroll will be infinite (ENDLESS) or will have bounds (BASIC).
     * If scroll type is set to ENDLESS but there is no elements to scroll, the scrolling type will be changed to BASIC.
     */
    enum class SCROLL private constructor(val value: Int) {
        BASIC(0),
        ENDLESS(1);


        companion object {

            fun valueOf(value: Int): SCROLL {
                return if (value == 0) {
                    BASIC
                } else ENDLESS
            }
        }
    }

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : super(context, attrs, defStyle) {
        init(context, attrs)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes
    ) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        setWillNotDraw(false)
        val typedArrayValues = context.obtainStyledAttributes(attrs, R.styleable.CycleMenuWidget)
        mItemsBackgroundTint = typedArrayValues.getColorStateList(R.styleable.CycleMenuWidget_cm_item_background_tint)
        mCorner = CORNER.valueOf(typedArrayValues.getInt(R.styleable.CycleMenuWidget_cm_corner, CORNER.RIGHT_TOP.value))
        mAutoMinRadius = typedArrayValues.getDimensionPixelSize(
            R.styleable.CycleMenuWidget_cm_autoMinRadius,
            DEFAULT_UNDEFINED_VALUE
        )
        mAutoMaxRadius = typedArrayValues.getDimensionPixelSize(
            R.styleable.CycleMenuWidget_cm_autoMaxRadius,
            DEFAULT_UNDEFINED_VALUE
        )
        mFixedRadius =
            typedArrayValues.getDimensionPixelSize(R.styleable.CycleMenuWidget_cm_fixedRadius, DEFAULT_UNDEFINED_VALUE)
        mScalingType = RADIUS_SCALING_TYPE.valueOf(
            typedArrayValues.getInt(
                R.styleable.CycleMenuWidget_cm_radius_scale_type,
                RADIUS_SCALING_TYPE.AUTO.value
            )
        )
        mScrollType =
            SCROLL.valueOf(typedArrayValues.getInt(R.styleable.CycleMenuWidget_cm_scroll_type, SCROLL.BASIC.value))
        val cornerImageDrawable = typedArrayValues.getDrawable(R.styleable.CycleMenuWidget_cm_corner_image_src)
        mRippleColor = typedArrayValues.getColor(R.styleable.CycleMenuWidget_cm_ripple_color, DEFAULT_UNDEFINED_VALUE)
        setCollapsedRadius(
            typedArrayValues.getDimensionPixelSize(
                R.styleable.CycleMenuWidget_cm_collapsed_radius,
                DEFAULT_UNDEFINED_VALUE
            )
        )
        mBackgroundColor = typedArrayValues.getColor(R.styleable.CycleMenuWidget_cm_background, DEFAULT_UNDEFINED_VALUE)
        typedArrayValues.recycle()

        mCirclePaint = Paint()
        mCirclePaint!!.isAntiAlias = true
        mCirclePaint!!.color = Color.WHITE
        mCirclePaint!!.style = Paint.Style.FILL

        if (mRippleColor == DEFAULT_UNDEFINED_VALUE) {
            mRippleColor = ContextCompat.getColor(getContext(), R.color.cm_ripple_color)
        }
        mRipplePaint = Paint()
        mRipplePaint!!.isAntiAlias = true
        mRipplePaint!!.style = Paint.Style.FILL
        mRipplePaint!!.color = mRippleColor

        mCornerShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
        mCornerShadowPaint!!.style = Paint.Style.FILL

        mShadowStartColor = ContextCompat.getColor(getContext(), R.color.cm_shadow_start_color)
        mShadowMiddleColor = ContextCompat.getColor(getContext(), R.color.cm_shadow_mid_color)
        mShadowEndColor = ContextCompat.getColor(getContext(), R.color.cm_shadow_end_color)

        mShadowSize = resources.getDimensionPixelSize(R.dimen.cm_main_shadow_size).toFloat()
        mVariableShadowSize = mShadowSize * SHADOW_SIZE_MIN_COEFFICIENT
        mPreLollipopAdditionalButtonsMargin =
            getContext().resources.getDimensionPixelSize(R.dimen.cm_prelollipop_additional_margin).toFloat()
        if (mCircleMinRadius == DEFAULT_UNDEFINED_VALUE) {
            mCircleMinRadius = getContext().resources.getDimensionPixelSize(R.dimen.cm_circle_min_radius)
        }
        mAnimationCircleRadius = mCircleMinRadius

        mRecyclerView = TouchedRecyclerView(getContext())
        mRecyclerView!!.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        mLayoutManager = CycleLayoutManager(getContext(), mCorner)
        addView(mRecyclerView)

        mAdapter = RecyclerMenuAdapter()
        if (mItemsBackgroundTint != null) {
            mAdapter!!.setItemsBackgroundTint(mItemsBackgroundTint!!)
        }
        mRecyclerView!!.layoutManager = mLayoutManager
        mRecyclerView!!.adapter = mAdapter
        mCenterImage = ImageView(getContext())
        if (cornerImageDrawable != null) {
            mCenterImage!!.setImageDrawable(cornerImageDrawable)
        } else {
            mCenterImage!!.setImageResource(R.drawable.cm_ic_plus)
        }
        if (mBackgroundColor != DEFAULT_UNDEFINED_VALUE) {
            mCirclePaint!!.color = mBackgroundColor
        }
        mCenterImage!!.scaleType = ImageView.ScaleType.CENTER_INSIDE
        addView(mCenterImage)

        mCenterImage!!.setOnTouchListener(CenterImageTouchListener())
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        mRecyclerView!!.setHasItemsToScroll(mLayoutManager!!.isCountOfItemsAvailableToScroll)
        return super.onInterceptTouchEvent(ev)
    }

    /**
     * Set menu item click listener
     *
     * @param onMenuItemClickListener listener
     */
    fun setOnMenuItemClickListener(onMenuItemClickListener: OnMenuItemClickListener) {
        mAdapter!!.setOnMenuItemClickListener(onMenuItemClickListener)
    }

    /**
     * Add the menu item.
     *
     * @param item menu item to add
     */
    fun addMenuItem(item: CycleMenuItem) {
        checkNonNullParams(item, FIELD_NAME_FOR_EXCEPTION_ITEM)
        mInitialized = false
        mAdapter!!.addItem(item)
        mAdapter!!.notifyDataSetChanged()
    }

    /**
     * Set the menu items from the menu res.
     *
     * @param menuResId menu resource from which need to get menuItems and add to the cycleMenu
     */
    @SuppressLint("RestrictedApi")
    fun setMenuRes(@MenuRes menuResId: Int) {
        mInitialized = false
        val menu = MenuBuilder(context)
        val inflater = MenuInflater(context)
        inflater.inflate(menuResId, menu)
        setMenu(menu)
    }

    /**
     * Set the menu items from the Menu object
     *
     * @param menu menu object from which need to get menuItems and add to the cycleMenu
     */
    fun setMenu(menu: Menu) {
        checkNonNullParams(menu, FIELD_NAME_FOR_EXCEPTION_MENU)
        mInitialized = false
        for (i in 0 until menu.size()) {
            val menuItem = menu.getItem(i)
            val cycleMenuItem = CycleMenuItem(menuItem.icon, menuItem.itemId)
            mAdapter!!.addItem(cycleMenuItem)
        }
        mAdapter!!.notifyDataSetChanged()
    }


    /**
     * Add the menu items for the cycleMenu
     *
     * @param items Collection of the items to add
     */
    fun addMenuItems(items: Collection<CycleMenuItem>) {
        checkNonNullParams(items, FIELD_NAME_FOR_EXCEPTION_ITEMS)
        mInitialized = false
        mAdapter!!.addItems(items)
        mAdapter!!.notifyDataSetChanged()
    }

    /**
     * Set the menu items for the cycleMenu
     *
     * @param items Collection of the items to set
     */
    fun setMenuItems(items: Collection<CycleMenuItem>) {
        checkNonNullParams(items, FIELD_NAME_FOR_EXCEPTION_ITEMS)
        mInitialized = false
        mAdapter!!.setItems(items)
        mAdapter!!.notifyDataSetChanged()
    }

    /**
     * Set the scaling type which will be used to calculate radius for the cycle menu.
     *
     * @param corner - mCorner to set for the menu LEFT_TOP, RIGHT_TOP, LEFT_BOTTOM, RIGHT_BOTTOM
     */
    fun setCorner(corner: CORNER) {
        checkNonNullParams(corner, FIELD_NAME_FOR_EXCEPTION_CORNER)
        mInitialized = false
        mLayoutManager!!.setCorner(corner)
        mCorner = corner
    }

    /**
     * Set the scaling type which will be used to calculate radius for the cycle menu.
     *
     * @param scalingType type of scaling AUTO,FIXED
     */
    fun setScalingType(scalingType: RADIUS_SCALING_TYPE) {
        checkNonNullParams(scalingType, FIELD_NAME_FOR_EXCEPTION_SCALING_TYPE)
        mInitialized = false
        mScalingType = scalingType
    }

    /**
     * Set ripple color
     *
     * @param rippleColor color to set
     */
    fun setRippleColor(rippleColor: Int) {
        mRippleColor = rippleColor
        setRippleAlpha(Color.alpha(mRippleColor))
        mRipplePaint!!.color = mRippleColor
    }

    /**
     * Set image for corner image button
     *
     * @param cornerImageDrawable resource to drawable
     */
    fun setCornerImageDrawable(cornerImageDrawable: Drawable?) {
        mCenterImage!!.setImageDrawable(cornerImageDrawable)
    }

    /**
     * Set image for corner image button
     *
     * @param cornerImageDrawable resource to drawable
     */
    fun setCornerImagevisibility(visibility: Int) {
        mCenterImage!!.visibility = visibility
    }

    /**
     * Set image resource for corner image button
     *
     * @param drawableRes resource to drawable
     */
    fun setCornerImageResource(@DrawableRes drawableRes: Int) {
        mCenterImage!!.setImageResource(drawableRes)
    }

    /**
     * Set image bitmap for corner image button
     *
     * @param bitmap resource to drawable
     */
    fun setCornerImageBitmap(bitmap: Bitmap) {
        mCenterImage!!.setImageBitmap(bitmap)
    }

    /**
     * Applies a min radius radius for the menu. Will be used if scaling_type set to `RADIUS_SCALING_TYPE.AUTO`
     *
     * @param autoMinRadius min radius to set
     */
    fun setAutoMinRadius(autoMinRadius: Int) {
        mInitialized = false
        mAutoMinRadius = autoMinRadius
    }

    /**
     * Applies a max radius radius for the menu. Will be used if scaling_type set to `RADIUS_SCALING_TYPE.AUTO`
     *
     * @param autoMaxRadius max radius to set
     */
    fun setAutoMaxRadius(autoMaxRadius: Int) {
        mInitialized = false
        mAutoMaxRadius = autoMaxRadius
    }

    /**
     * Applies a fixed radius for the menu. Will be used if scaling_type set to `RADIUS_SCALING_TYPE.FIXED`
     *
     * @param fixedRadius - fixed radius to set
     */
    fun setFixedRadius(fixedRadius: Int) {
        mInitialized = false
        mFixedRadius = fixedRadius
    }

    /**
     * Applies a radius of the collapsed menu.
     *
     * @param collapsedRadius radius to set (in px)
     */
    @Throws(IllegalArgumentException::class)
    fun setCollapsedRadius(collapsedRadius: Int) {
        mInitialized = false
        if (mScalingType == RADIUS_SCALING_TYPE.FIXED && collapsedRadius < mFixedRadius || mScalingType == RADIUS_SCALING_TYPE.AUTO && collapsedRadius < mAutoMaxRadius) {
            mCircleMinRadius = collapsedRadius
        }
    }

    /**
     * Applies a color to the CycleMenuWidget background
     *
     * @param backgroundColor color to set
     */
    fun setBackground(backgroundColor: Int) {
        mBackgroundColor = backgroundColor
        mCirclePaint!!.color = mBackgroundColor
        invalidate()
    }

    /**
     * Applies a tint to the background drawable of the items in cycle menu. Does not modify the current tint
     * mode, which is [PorterDuff.Mode.SRC_IN] by default.
     *
     * @param itemsBackgroundTint the tint to apply, may be `null` to clear tint
     */
    fun setItemsBackgroundTint(itemsBackgroundTint: ColorStateList?) {
        mItemsBackgroundTint = itemsBackgroundTint
        mAdapter!!.setItemsBackgroundTint(itemsBackgroundTint!!)
        mAdapter!!.notifyDataSetChanged()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setupMargins()
        var width = View.MeasureSpec.getSize(widthMeasureSpec)
        var height = View.MeasureSpec.getSize(heightMeasureSpec)
        val parentWidth = (parent as ViewGroup).width
        val parentHeight = (parent as ViewGroup).height

        var newWidthMeasureSpec = widthMeasureSpec
        var newHeightMeasureSpec = heightMeasureSpec
        if (height == 0) {
            newHeightMeasureSpec = View.MeasureSpec.makeMeasureSpec(parentHeight, View.MeasureSpec.AT_MOST)
            height = View.MeasureSpec.getSize(newHeightMeasureSpec)
        }

        if (width == 0) {
            newWidthMeasureSpec = View.MeasureSpec.makeMeasureSpec(parentWidth, View.MeasureSpec.AT_MOST)
            width = View.MeasureSpec.getSize(newWidthMeasureSpec)
        }

        if (mItemSize <= 0) {
            val buttonItem =
                LayoutInflater.from(context).inflate(R.layout.cm_item_fab, this, false) as FloatingActionButton
            val buttonSpec = View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.AT_MOST)
            measureChild(buttonItem, buttonSpec, buttonSpec)
            val measuredItemWidth = buttonItem.measuredWidth
            val measuredItemHeight = buttonItem.measuredHeight
            mItemSize = if (measuredItemWidth > measuredItemHeight) measuredItemWidth else measuredItemHeight
            if (mItemSize > 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mItemSize = (mItemSize * 1.3).toInt()
                } else {
                    mItemSize = (mItemSize - mPreLollipopAdditionalButtonsMargin * 2 / 1.5f).toInt()
                }
            }
        }

        mRecyclerSize = ((if (width > height) height else width) - mShadowSize).toInt()
        val recyclerSizeMeasureSpec = View.MeasureSpec.makeMeasureSpec(mRecyclerSize, View.MeasureSpec.EXACTLY)

        if ((mScalingType == RADIUS_SCALING_TYPE.FIXED || mAutoMaxRadius > mRecyclerSize || mAutoMaxRadius < 0) && mRecyclerSize > 0) {
            mAutoMaxRadius = mRecyclerSize
        }
        if (mAutoMinRadius < mCircleMinRadius + mItemSize) {
            mAutoMinRadius = mCircleMinRadius + mItemSize
        }
        if (mAutoMinRadius > mAutoMaxRadius) {
            mAutoMinRadius = mAutoMaxRadius
        }
        if (mScalingType == RADIUS_SCALING_TYPE.AUTO) {
            mRecyclerSize = (mItemSize * mAdapter!!.realItemsCount * 4 / (Math.PI * 2)).toInt()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mRecyclerSize += mItemSize * 5 / 8
            } else {
                mRecyclerSize += mItemSize * 7 / 8
            }
            if (mRecyclerSize > mAutoMaxRadius) {
                mRecyclerSize = mAutoMaxRadius
            }

            if (mRecyclerSize < mAutoMinRadius) {
                mRecyclerSize = mAutoMinRadius
            }
        } else if (mRecyclerSize > 0) {
            if (mFixedRadius > mAutoMaxRadius) {
                mFixedRadius = mAutoMaxRadius
            }
            if (mFixedRadius < mAutoMinRadius) {
                mFixedRadius = mAutoMinRadius
            }
            mRecyclerSize = mFixedRadius
        }

        mOutCircleRadius = mRecyclerSize
        mRecyclerView!!.measure(recyclerSizeMeasureSpec, recyclerSizeMeasureSpec)

        val lCenterIconSize = Math.sqrt(mCircleMinRadius * mCircleMinRadius / 2.0).toInt()
        val centerImageMeasureWidthSpec = View.MeasureSpec.makeMeasureSpec(lCenterIconSize, View.MeasureSpec.EXACTLY)
        val centerImageMeasureHeightSpec = View.MeasureSpec.makeMeasureSpec(lCenterIconSize, View.MeasureSpec.EXACTLY)
        mCenterImage!!.measure(centerImageMeasureWidthSpec, centerImageMeasureHeightSpec)

        width = View.resolveSize(width, newWidthMeasureSpec)
        height = View.resolveSize(height, newHeightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val containerWidth = r - l
        var centerImageLeft = 0
        var centerImageTop = 0
        var centerImageRight = 0
        var centerImageBottom = 0
        var recyclerLeft = 0
        var recyclerTop = 0
        var recyclerRight = 0
        var recyclerBottom = 0

        if (mCorner.isUpSide) {
            centerImageBottom = mCenterImage!!.measuredHeight
            recyclerTop = t
            recyclerBottom = t + mRecyclerSize
        } else if (mCorner.isBottomSide) {
            centerImageTop = height - mCenterImage!!.measuredHeight
            centerImageBottom = height
            recyclerTop = b - mRecyclerSize
            recyclerBottom = b
        }
        if (mCorner.isLeftSide) {
            centerImageRight = mCenterImage!!.measuredWidth
            recyclerLeft = l
            recyclerRight = l + mRecyclerSize
        } else if (mCorner.isRightSide) {
            centerImageLeft = containerWidth - mCenterImage!!.measuredWidth
            centerImageRight = containerWidth
            recyclerLeft = r - mRecyclerSize
            recyclerRight = r
        }

        mCenterImage!!.layout(centerImageLeft, centerImageTop, centerImageRight, centerImageBottom)
        mRecyclerView!!.layout(recyclerLeft, recyclerTop, recyclerRight, recyclerBottom)
        mRecyclerView!!.translationX = width.toFloat()
        val countOfVisibleElements = (mRecyclerSize * Math.PI / 2 / mItemSize).toInt()
        if (!mInitialized && r > 0 && b > 0) {
            if (mAdapter!!.realItemsCount > countOfVisibleElements && mScrollType == SCROLL.ENDLESS) {
                mAdapter!!.setScrollType(SCROLL.ENDLESS)
                if (mCurrentPosition == RecyclerView.NO_POSITION) {
                    mCurrentPosition =
                        Integer.MAX_VALUE / 2 + (mAdapter!!.realItemsCount - Integer.MAX_VALUE / 2 % mAdapter!!.realItemsCount)
                }
            } else {
                mAdapter!!.setScrollType(SCROLL.BASIC)
            }
            if (mCurrentPosition != RecyclerView.NO_POSITION) {
                mLayoutManager!!.scrollToPosition(mCurrentPosition)
            }
            mLayoutManager!!.setAdditionalAngleOffset(mCurrentAngleOffset)
            mRecyclerView!!.post(runnableRequestLayout)
            mInitialized = true
        }
        if (mState == STATE.OPEN) {
            open(false)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val mainCircleRadius = mAnimationCircleRadius
        buildShadowCorners()

        val rippleRadius = if (mainCircleRadius < mRippleRadius) mainCircleRadius else mRippleRadius
        var circleCenterX = 0
        var circleCenterY = 0
        if (mCorner == CORNER.LEFT_TOP) {
            val canvasState = canvas.save()
            canvas.rotate(-90f, width.toFloat(), 0f)
            canvas.translate(0f, (-width).toFloat())
            canvas.drawPath(mCornerShadowPath!!, mCornerShadowPaint!!)
            canvas.restoreToCount(canvasState)
        } else if (mCorner == CORNER.RIGHT_TOP) {
            circleCenterX = canvas.width
            circleCenterY = 0
            canvas.drawPath(mCornerShadowPath!!, mCornerShadowPaint!!)
        } else if (mCorner == CORNER.LEFT_BOTTOM) {
            circleCenterX = 0
            circleCenterY = height
            val canvasState = canvas.save()
            canvas.rotate(-180f, width.toFloat(), 0f)
            canvas.translate(width.toFloat(), (-height).toFloat())
            canvas.drawPath(mCornerShadowPath!!, mCornerShadowPaint!!)
            canvas.restoreToCount(canvasState)
        } else if (mCorner == CORNER.RIGHT_BOTTOM) {
            circleCenterX = width
            circleCenterY = height
            val canvasState = canvas.save()
            canvas.rotate(90f, width.toFloat(), 0f)
            canvas.translate(height.toFloat(), 0f)
            canvas.drawPath(mCornerShadowPath!!, mCornerShadowPaint!!)
            canvas.restoreToCount(canvasState)
        }

        canvas.drawCircle(circleCenterX.toFloat(), circleCenterY.toFloat(), mainCircleRadius.toFloat(), mCirclePaint!!)
        canvas.drawCircle(
            circleCenterX.toFloat(),
            circleCenterY.toFloat(),
            rippleRadius.toFloat(),
            mRipplePaint!!
        )
    }

    /**
     * Build path for circular shadow.
     */
    private fun buildShadowCorners() {
        val mCornerRadius = mAnimationCircleRadius.toFloat()

        val innerBounds = RectF(width - mCornerRadius, -mCornerRadius, width + mCornerRadius, mCornerRadius)
        val outerBounds = RectF(innerBounds)
        outerBounds.inset(-mVariableShadowSize, -mVariableShadowSize)

        if (mCornerShadowPath == null) {
            mCornerShadowPath = Path()
        } else {
            mCornerShadowPath!!.reset()
        }
        mCornerShadowPath!!.fillType = Path.FillType.EVEN_ODD
        mCornerShadowPath!!.moveTo(width - mCornerRadius, 0f)

        mCornerShadowPath!!.rLineTo(-mVariableShadowSize, 0f)
        // outer arc
        mCornerShadowPath!!.arcTo(outerBounds, 180f, -90f, false)
        // inner arc
        mCornerShadowPath!!.arcTo(innerBounds, 90f, 90f, false)

        val shadowRadius = -outerBounds.top
        if (shadowRadius > 0f) {
            val startRatio = mCornerRadius / shadowRadius
            val midRatio = startRatio + (1f - startRatio) / 2f
            val gradient = RadialGradient(
                width.toFloat(), 0f, shadowRadius,
                intArrayOf(0, mShadowStartColor, mShadowMiddleColor, mShadowEndColor),
                floatArrayOf(0f, startRatio, midRatio, 1f),
                Shader.TileMode.CLAMP
            )
            mCornerShadowPaint!!.shader = gradient
        }
    }

    /**
     * Set scroll type for menu
     *
     * @param scrollType the scroll type BASIC, ENDLESS
     */
    fun setScrollType(scrollType: SCROLL) {
        checkNonNullParams(scrollType, FIELD_NAME_FOR_EXCEPTION_SCROLLING_TYPE)
        mScrollType = scrollType
    }

    /**
     * Set StateChangeListener to MenuWidget
     *
     * @param listener OnStateChangedListener
     */
    fun setStateChangeListener(listener: OnStateChangedListener?) {
        mOnStateChangeListener = listener
    }

    /**
     * Set mState save listener. this object widget will call to save position of the first item and offset in degrees.
     *
     * @param stateSaveListener - listener
     */
    fun setStateSaveListener(stateSaveListener: StateSaveListener) {
        mStateSaveListener = stateSaveListener
    }

    private fun setRippleAlpha(rippleAlpha: Int) {
        mRipplePaint!!.alpha = rippleAlpha
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mInitialized = false
        mLayoutManager!!.requestLayout()
        mAdapter!!.notifyDataSetChanged()
    }

    override fun onDetachedFromWindow() {
        mCurrentPosition = currentPosition
        mCurrentAngleOffset = mLayoutManager!!.currentItemsAngleOffset
        if (mStateSaveListener != null) {
            mStateSaveListener!!.saveState(mCurrentPosition, mCurrentAngleOffset)
        }
        if (mState == STATE.IN_CLOSE_PROCESS) {
            close(false)
        }
        if (mState == STATE.IN_OPEN_PROCESS) {
            mState = STATE.OPEN
            sendState()
            mAnimationCircleRadius = mOutCircleRadius
            scrollEnabled(true)
        }
        super.onDetachedFromWindow()
    }

    private fun sendState() {
        if (mOnStateChangeListener != null) {
            mOnStateChangeListener!!.onStateChanged(mState)
        }
    }

    private fun setupMargins() {
        val params = layoutParams as ViewGroup.MarginLayoutParams ?: return

        when (mCorner) {
            CycleMenuWidget.CORNER.LEFT_TOP -> {
                params.leftMargin = 0
                params.topMargin = 0
            }
            CycleMenuWidget.CORNER.LEFT_BOTTOM -> {
                params.leftMargin = 0
                params.bottomMargin = 0
            }
            CycleMenuWidget.CORNER.RIGHT_BOTTOM -> {
                params.rightMargin = 0
                params.bottomMargin = 0
            }
            CycleMenuWidget.CORNER.RIGHT_TOP -> {
                params.rightMargin = 0
                params.topMargin = 0
            }
        }
        layoutParams = params
    }

    /**
     * enable/disable of items scrolling.
     *
     * @param enabled - scroll enabling value
     */
    private fun scrollEnabled(enabled: Boolean) {
        mRecyclerView!!.setTouchEnabled(enabled)
        mLayoutManager!!.setScrollEnabled(enabled)
    }

    /**
     * Change menu mState open -> close, close -> open if don't doing open/close right now.
     */
    private fun changeMenuState() {
        if (mState == STATE.IN_OPEN_PROCESS || mState == STATE.IN_CLOSE_PROCESS) {
            return
        }
        if (mState == STATE.OPEN) {
            close(true)
            return
        }
        open(true)
    }

    /**
     * Open cycle menu.
     *
     * @param animated - indicate if need to open cycle menu with animation (true), immediately otherwise
     */
    fun open(animated: Boolean) {
        val centerCrossImageRotateAngle = -45
        if (animated) {
            scrollEnabled(false)
            mState = STATE.IN_OPEN_PROCESS
            sendState()
            mCenterImage!!.animate()
                .rotation(centerCrossImageRotateAngle.toFloat())
                .setInterpolator(OvershootInterpolator(2f))
                .setDuration(CENTER_IMAGE_ROTATE_DURATION.toLong())
                .start()

            val circleRadiusAnimator =
                ObjectAnimator.ofInt(this, CIRCLE_RADIUS_ANIMATOR_FIELD_NAME, mCircleMinRadius, mOutCircleRadius)
            circleRadiusAnimator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    mRecyclerView!!.translationX = 0f
                    mLayoutManager!!.rollInItemsWithAnimation(object : CycleLayoutManager.OnCompleteCallback {
                        override fun onComplete() {
                            mState = STATE.OPEN
                            sendState()
                            scrollEnabled(true)
                            if (mOnStateChangeListener != null) {
                                mOnStateChangeListener!!.onOpenComplete()
                            }
                        }
                    })
                }
            })
            val shadowAnimator = ObjectAnimator.ofFloat(
                this,
                SHADOW_SIZE_ANIMATOR_FIELD_NAME,
                mShadowSize * SHADOW_SIZE_MIN_COEFFICIENT,
                mShadowSize
            )

            val animatorSet = AnimatorSet()
            animatorSet.duration = REVEAL_ANIMATION_DURATION.toLong()
            animatorSet.playTogether(circleRadiusAnimator, shadowAnimator)
            animatorSet.start()
        } else {
            mVariableShadowSize = mShadowSize
            mCenterImage!!.rotation = centerCrossImageRotateAngle.toFloat()
            mAnimationCircleRadius = mOutCircleRadius
            mRecyclerView!!.translationX = 0f
            scrollEnabled(true)
            mState = STATE.OPEN
            sendState()
            invalidate()
        }
    }

    /**
     * Close cycle menu.
     *
     * @param animated - indicate if need to close cycle menu with animation (true), immediately otherwise
     */
    fun close(animated: Boolean) {
        if (animated) {
            scrollEnabled(false)
            mState = STATE.IN_CLOSE_PROCESS
            sendState()
            mCenterImage!!.animate()
                .rotation(0f)
                .setInterpolator(OvershootInterpolator(2f))
                .setDuration(CENTER_IMAGE_ROTATE_DURATION.toLong())
                .start()
            mLayoutManager!!.rollOutItemsWithAnimation(object : CycleLayoutManager.OnCompleteCallback {
                override fun onComplete() {
                    innerAnimatedClose()
                }
            })
        } else {
            scrollEnabled(true)
            mState = STATE.CLOSED
            sendState()
            mVariableShadowSize = mShadowSize * SHADOW_SIZE_MIN_COEFFICIENT
            mCenterImage!!.rotation = 0f
            mAnimationCircleRadius = mCircleMinRadius
            invalidate()
        }
    }

    private fun innerAnimatedClose() {
        val circleRadiusAnimator =
            ObjectAnimator.ofInt(this, CIRCLE_RADIUS_ANIMATOR_FIELD_NAME, mOutCircleRadius, mCircleMinRadius)
        circleRadiusAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                mState = STATE.CLOSED
                sendState()
                if (mOnStateChangeListener != null) {
                    mOnStateChangeListener!!.onCloseComplete()
                }
            }
        })
        val shadowAnimator = ObjectAnimator.ofFloat(
            this,
            SHADOW_SIZE_ANIMATOR_FIELD_NAME,
            mShadowSize,
            mShadowSize * SHADOW_SIZE_MIN_COEFFICIENT
        )

        val animatorSet = AnimatorSet()
        animatorSet.duration = REVEAL_ANIMATION_DURATION.toLong()
        animatorSet.playTogether(circleRadiusAnimator, shadowAnimator)
        animatorSet.start()
        mRecyclerView!!.translationX = width.toFloat()
    }

    private fun setRippleRadius(rippleRadius: Int) {
        mRippleRadius = rippleRadius
        if (mShouldOpen && rippleRadius >= mCircleMinRadius) {
            mShouldOpen = false
            changeMenuState()
        }
        invalidate()
    }

    private fun setVariableShadowSize(variableShadowSize: Float) {
        mVariableShadowSize = variableShadowSize
    }

    private fun setAnimationCircleRadius(animationCircleRadius: Int) {
        mAnimationCircleRadius = animationCircleRadius
        invalidate()
    }

    private inner class CenterImageTouchListener : View.OnTouchListener {
        private var wasOutside = false
        private val rect = Rect()
        private var mRippleSizeAnimator: ObjectAnimator? = null
        private var mRippleAlphaAnimator: ObjectAnimator? = null

        private fun cancelRippleAnimator() {
            if (mRippleSizeAnimator != null) {
                mRippleSizeAnimator!!.cancel()
            }
            if (mRippleAlphaAnimator != null) {
                mRippleAlphaAnimator!!.cancel()
            }
        }

        private fun startRippleSizeAnimator(fromRadius: Int, toRadius: Int) {
            mRippleSizeAnimator =
                ObjectAnimator.ofInt(this@CycleMenuWidget, RIPPLE_RADIUS_ANIMATOR_FIELD_NAME, fromRadius, toRadius)
                    .setDuration(RIPPLE_REVEAL_DURATION.toLong())
            mRippleSizeAnimator!!.start()
        }

        private fun startRippleAlphaAnimator(fromAlpha: Int, toAlpha: Int) {
            mRippleAlphaAnimator =
                ObjectAnimator.ofInt(this@CycleMenuWidget, RIPPLE_ALPHA_ANIMATOR_FIELD_NAME, fromAlpha, toAlpha)
                    .setDuration(RIPPLE_ALPHA_DURATION.toLong())
            mRippleAlphaAnimator!!.start()
        }

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            mShouldOpen = false
            if (mState == STATE.IN_OPEN_PROCESS || mState == STATE.IN_CLOSE_PROCESS) {
                return false
            }
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    cancelRippleAnimator()
                    rect.set(v.left, v.top, v.right, v.bottom)
                    wasOutside = false
                    setRippleAlpha(Color.alpha(mRippleColor))
                    startRippleSizeAnimator(0, mAnimationCircleRadius)
                }
                MotionEvent.ACTION_MOVE -> if (!rect.contains(v.left + event.x.toInt(), v.top + event.y.toInt())) {
                    wasOutside = true
                }
                MotionEvent.ACTION_CANCEL -> {
                    wasOutside = true
                    cancelRippleAnimator()
                    if (wasOutside) {
                        startRippleSizeAnimator(mAnimationCircleRadius, 0)
                    } else {
                        if (mAnimationCircleRadius == mRippleRadius) {
                            mRippleRadius = mOutCircleRadius
                            changeMenuState()
                        } else {
                            startRippleSizeAnimator(mRippleRadius, mOutCircleRadius)
                            if (mState == STATE.CLOSED) {
                                mShouldOpen = true
                            } else {
                                changeMenuState()
                            }
                        }
                    }
                    startRippleAlphaAnimator(Color.alpha(mRippleColor), 0)
                }
                MotionEvent.ACTION_UP -> {
                    cancelRippleAnimator()
                    if (wasOutside) {
                        startRippleSizeAnimator(mAnimationCircleRadius, 0)
                    } else {
                        if (mAnimationCircleRadius == mRippleRadius) {
                            mRippleRadius = mOutCircleRadius
                            changeMenuState()
                        } else {
                            startRippleSizeAnimator(mRippleRadius, mOutCircleRadius)
                            if (mState == STATE.CLOSED) {
                                mShouldOpen = true
                            } else {
                                changeMenuState()
                            }
                        }
                    }
                    startRippleAlphaAnimator(Color.alpha(mRippleColor), 0)
                }
            }
            return true
        }
    }

    private fun checkNonNullParams(param: Any?, paramName: String) {
        if (param == null) {
            throw IllegalArgumentException("Parameter \"$paramName\" can't be null.")
        }
    }

    companion object {

        const val UNDEFINED_ANGLE_VALUE = -1000
        private val CENTER_IMAGE_ROTATE_DURATION = 300
        private val REVEAL_ANIMATION_DURATION = 200
        private val RIPPLE_REVEAL_DURATION = 300
        private val RIPPLE_ALPHA_DURATION = 450

        private val RIPPLE_RADIUS_ANIMATOR_FIELD_NAME = "rippleRadius"
        private val RIPPLE_ALPHA_ANIMATOR_FIELD_NAME = "rippleAlpha"
        private val CIRCLE_RADIUS_ANIMATOR_FIELD_NAME = "animationCircleRadius"
        private val SHADOW_SIZE_ANIMATOR_FIELD_NAME = "variableShadowSize"

        private val FIELD_NAME_FOR_EXCEPTION_ITEM = "item"
        private val FIELD_NAME_FOR_EXCEPTION_MENU = "menu"
        private val FIELD_NAME_FOR_EXCEPTION_ITEMS = "items"
        private val FIELD_NAME_FOR_EXCEPTION_CORNER = "corner"
        private val FIELD_NAME_FOR_EXCEPTION_SCALING_TYPE = "scalingType"
        private val FIELD_NAME_FOR_EXCEPTION_SCROLLING_TYPE = "scrollingType"

        private val DEFAULT_UNDEFINED_VALUE = -1
        private val SHADOW_SIZE_MIN_COEFFICIENT = 0.25f
    }
}
