package com.cleveroad.sy.cyclemenuwidget

import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.*

/**
 * Inner adapter for menu mItems.
 */
internal class RecyclerMenuAdapter : RecyclerView.Adapter<RecyclerMenuAdapter.ItemHolder>(), OnMenuItemClickListener {

    private val mItems: MutableList<CycleMenuItem>
    private var mItemsBackgroundTint: ColorStateList? = null
    private var defaultTintColorChanged = false
    private var mOnMenuItemClickListener: OnMenuItemClickListener? = null

    private var mScrollType: CycleMenuWidget.SCROLL = CycleMenuWidget.SCROLL.BASIC

    val realItemsCount: Int
        get() = mItems.size

    init {
        mItems = ArrayList()
    }

    /**
     * Set scroll type for menu
     *
     * @param scrollType the scroll type BASIC, ENDLESS
     */
    fun setScrollType(scrollType: CycleMenuWidget.SCROLL) {
        mScrollType = scrollType
    }

    /**
     * Set items Collection for the adapter
     *
     * @param items collections to be set to adapter
     */
    fun setItems(items: Collection<CycleMenuItem>) {
        mItems.clear()
        mItems.addAll(items)
    }

    /**
     * Set menu item click listener
     *
     * @param onMenuItemClickListener listener
     */
    fun setOnMenuItemClickListener(onMenuItemClickListener: OnMenuItemClickListener) {
        mOnMenuItemClickListener = onMenuItemClickListener
    }

    /**
     * Applies a tint to the background drawable of the items in cycle menu. Does not modify the current tint
     * mode, which is [PorterDuff.Mode.SRC_IN] by default.
     *
     * @param itemsBackgroundTint the tint to apply, may be `null` to clear tint
     */
    fun setItemsBackgroundTint(itemsBackgroundTint: ColorStateList) {
        defaultTintColorChanged = true
        mItemsBackgroundTint = itemsBackgroundTint
    }

    /**
     * Add items Collection to the adapter
     *
     * @param items collections that need to be added to adapter
     */
    fun addItems(items: Collection<CycleMenuItem>) {
        mItems.addAll(items)
    }

    /**
     * Add item to the adapter
     *
     * @param item that need to add to the adapter
     */
    fun addItem(item: CycleMenuItem) {
        mItems.add(item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.cm_item_fab, parent, false)
        if (defaultTintColorChanged) {

            (view as FloatingActionButton).backgroundTintList = mItemsBackgroundTint
        }
        return ItemHolder(view, this)
    }

    override fun onBindViewHolder(holder: ItemHolder, position: Int) {
        val button = holder.itemView as FloatingActionButton
        //        button.setImageDrawable(mItems.get(getRealPosition(position)).getIcon());
        //        button.setBackgroundDrawable(new ColorDrawable(getRealPosition(position)));
        button.supportBackgroundTintList =
            ColorStateList.valueOf(getRealPosition(mItems[getRealPosition(position)].color))
        //        holder.itemView.setId(mItems.get(getRealPosition(position)).getId());
    }

    override fun getItemCount(): Int {
        //if scrollType is ENDLESS then need to set infinite scrolling
        return if (mScrollType === CycleMenuWidget.SCROLL.ENDLESS) {
            Integer.MAX_VALUE
        } else mItems.size
    }

    /**
     * Return real position of item in adapter. Is used when scrollType = ENDLESS.
     *
     * @param position position form adapter.
     * @return int realPosition of the item in adapter
     */
    private fun getRealPosition(position: Int): Int {
        return position % mItems.size
    }

    override fun onMenuItemClick(view: View, itemPosition: Int) {
        if (mOnMenuItemClickListener != null) {
            mOnMenuItemClickListener!!.onMenuItemClick(view, getRealPosition(itemPosition))
        }
    }

    override fun onMenuItemLongClick(view: View, itemPosition: Int) {
        if (mOnMenuItemClickListener != null) {
            mOnMenuItemClickListener!!.onMenuItemLongClick(view, getRealPosition(itemPosition))
        }
    }

    internal class ItemHolder(itemView: View, private val mOnMenuItemClickListener: OnMenuItemClickListener) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        override fun onClick(view: View) {
            //Resend click to the outer menu item click listener with provided item position. if scrollType is ENDLESS need to getRealPosition from the position.
            mOnMenuItemClickListener.onMenuItemClick(view, adapterPosition)
        }

        override fun onLongClick(view: View): Boolean {
            mOnMenuItemClickListener.onMenuItemLongClick(view, adapterPosition)
            return true
        }
    }
}
