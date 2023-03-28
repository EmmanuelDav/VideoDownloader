package com.cyberIyke.allvideodowloader.views.cardstack

import android.annotation.TargetApi
import android.content.Context
import android.database.Observable
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.view.View.OnClickListener
import android.widget.OverScroller
import com.cyberIyke.allvideodowloader.R
import com.cyberIyke.allvideodowloader.browser.BrowserWindow
import com.cyberIyke.allvideodowloader.views.cardstack.*

class CardStackView : ViewGroup, ScrollDelegate {
    var totalLength = 0
        private set
    var overlapGaps = 0
    var overlapGapsCollapse = 0
    var numBottomShow = 0
    private var mStackAdapter: StackAdapter<BrowserWindow>? = null
    private val mObserver: ViewDataObserver = ViewDataObserver()
    private var mSelectPosition = DEFAULT_SELECT_POSITION
    var showHeight = 0
        private set
    private var mViewHolders: MutableList<ViewHolder>? = null
    private var mAnimatorAdapter: AnimatorAdapter? = null
    private var mDuration = 0
    private var mScroller: OverScroller? = null
    private var mLastMotionY = 0
    private var mIsBeingDragged = false
    private var mVelocityTracker: VelocityTracker? = null
    private var mTouchSlop = 0
    private var mMinimumVelocity = 0
    private var mMaximumVelocity = 0
    private var mActivePointerId = INVALID_POINTER
    private val mScrollOffset = IntArray(2)
    private var mNestedYOffset = 0
    private var mScrollEnable = true
    private var mScrollDelegate: ScrollDelegate? = null
    var itemExpendListener: ItemExpendListener? = null

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs, defStyleAttr, 0)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context, attrs, defStyleAttr, defStyleRes)
    }

    private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        val array = context.obtainStyledAttributes(
            attrs,
            R.styleable.CardStackView,
            defStyleAttr,
            defStyleRes
        )
        overlapGaps =
            array.getDimensionPixelSize(R.styleable.CardStackView_stackOverlapGaps, dp2px(20))
        overlapGapsCollapse = array.getDimensionPixelSize(
            R.styleable.CardStackView_stackOverlapGapsCollapse,
            dp2px(20)
        )
        duration = array.getInt(
            R.styleable.CardStackView_stackDuration,
            AnimatorAdapter.ANIMATION_DURATION
        )
        setAnimationType(array.getInt(R.styleable.CardStackView_stackAnimationType, UP_DOWN_STACK))
        numBottomShow = array.getInt(R.styleable.CardStackView_stackNumBottomShow, 3)
        array.recycle()
        mViewHolders = ArrayList()
        initScroller()
    }

    private fun initScroller() {
        mScroller = OverScroller(context)
        isFocusable = true
        descendantFocusability = FOCUS_AFTER_DESCENDANTS
        val configuration = ViewConfiguration.get(context)
        mTouchSlop = configuration.scaledTouchSlop
        mMinimumVelocity = configuration.scaledMinimumFlingVelocity
        mMaximumVelocity = configuration.scaledMaximumFlingVelocity
    }

    private fun dp2px(value: Int): Int {
        val scale = context.resources.displayMetrics.density
        return (value * scale + 0.5f).toInt()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        checkContentHeightByParent()
        measureChild(widthMeasureSpec, heightMeasureSpec)
    }

    private fun checkContentHeightByParent() {
        val parentView = parent as View
        showHeight = parentView.measuredHeight - parentView.paddingTop - parentView.paddingBottom
    }

    private fun measureChild(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var maxWidth = 0
        totalLength = 0
        totalLength += paddingTop + paddingBottom
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0)
            val totalLength = totalLength
            val lp = child.layoutParams as LayoutParams
            if (lp.mHeaderHeight == -1) lp.mHeaderHeight = child.measuredHeight
            val childHeight = lp.mHeaderHeight
            this.totalLength = Math.max(
                totalLength, totalLength + childHeight + lp.topMargin +
                        lp.bottomMargin
            )
            this.totalLength -= overlapGaps * 2
            val margin = lp.leftMargin + lp.rightMargin
            val measuredWidth = child.measuredWidth + margin
            maxWidth = Math.max(maxWidth, measuredWidth)
        }
        totalLength += overlapGaps * 2
        var heightSize = totalLength
        heightSize = Math.max(heightSize, showHeight)
        val heightSizeAndState = resolveSizeAndState(heightSize, heightMeasureSpec, 0)
        setMeasuredDimension(
            resolveSizeAndState(maxWidth, widthMeasureSpec, 0),
            heightSizeAndState
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        layoutChild()
    }

    private fun layoutChild() {
        var childTop = paddingTop
        val childLeft = paddingLeft
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val childWidth = child.measuredWidth
            val childHeight = child.measuredHeight
            val lp = child.layoutParams as LayoutParams
            childTop += lp.topMargin
            if (i != 0) {
                childTop -= overlapGaps * 2
                child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight)
            } else {
                child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight)
            }
            childTop += lp.mHeaderHeight
        }
    }

    fun updateSelectPosition(selectPosition: Int) {
        post { doCardClickAnimation(mViewHolders!![selectPosition], selectPosition) }
    }

    fun clearSelectPosition() {
        updateSelectPosition(mSelectPosition)
    }

    fun clearScrollYAndTranslation() {
        if (mSelectPosition != DEFAULT_SELECT_POSITION) {
            clearSelectPosition()
        }
        if (mScrollDelegate != null) mScrollDelegate!!.viewScrollX=(0)
        requestLayout()
    }

    fun setAdapter(stackAdapter: StackAdapter<BrowserWindow>) {
        mStackAdapter = stackAdapter
        mStackAdapter!!.registerObserver(mObserver)
        refreshView()
    }

    fun setAnimationType(type: Int) {
        val animatorAdapter: AnimatorAdapter
        when (type) {
            ALL_DOWN -> animatorAdapter = AllMoveDownAnimatorAdapter(this)
            UP_DOWN -> animatorAdapter = UpDownAnimatorAdapter(this)
            else -> animatorAdapter = UpDownStackAnimatorAdapter(this)
        }
        setAnimatorAdapter(animatorAdapter)
    }

    fun setAnimatorAdapter(animatorAdapter: AnimatorAdapter?) {
        clearScrollYAndTranslation()
        mAnimatorAdapter = animatorAdapter
        if (mAnimatorAdapter is UpDownStackAnimatorAdapter) {
            mScrollDelegate = StackScrollDelegateImpl(this)
        } else {
            mScrollDelegate = this
        }
    }

    private fun refreshView() {
        removeAllViews()
        mViewHolders!!.clear()
        for (i in 0 until mStackAdapter!!.itemCount) {
            val holder = getViewHolder(i)
            holder!!.position = i
            holder.onItemExpand(i == mSelectPosition)
            addView(holder.itemView)
            setClickAnimator(holder, i)
            mStackAdapter!!.bindViewHolder(holder, i)
        }
        requestLayout()
    }

    fun getViewHolder(i: Int): ViewHolder? {
        if (i == DEFAULT_SELECT_POSITION) return null
        val viewHolder: ViewHolder
        if (mViewHolders!!.size <= i || mViewHolders!![i].mItemViewType != mStackAdapter!!.getItemViewType(
                i
            )
        ) {
            viewHolder = mStackAdapter!!.createView(this, mStackAdapter!!.getItemViewType(i))!!
            mViewHolders!!.add(viewHolder)
        } else {
            viewHolder = mViewHolders!![i]
        }
        return viewHolder
    }

    private fun setClickAnimator(holder: ViewHolder?, position: Int) {
        setOnClickListener(OnClickListener {
            if (mSelectPosition == DEFAULT_SELECT_POSITION) return@OnClickListener
            performItemClick(mViewHolders!![mSelectPosition])
        })
        holder!!.itemView.setOnClickListener { performItemClick(holder) }
    }

    operator fun next() {
        if (mSelectPosition == DEFAULT_SELECT_POSITION || mSelectPosition == mViewHolders!!.size - 1) return
        performItemClick(mViewHolders!![mSelectPosition + 1])
    }

    fun pre() {
        if (mSelectPosition == DEFAULT_SELECT_POSITION || mSelectPosition == 0) return
        performItemClick(mViewHolders!![mSelectPosition - 1])
    }

    val isExpending: Boolean
        get() = mSelectPosition != DEFAULT_SELECT_POSITION

    fun performItemClick(viewHolder: ViewHolder?) {
        doCardClickAnimation(viewHolder, viewHolder!!.position)
    }

    private fun doCardClickAnimation(viewHolder: ViewHolder?, position: Int) {
        checkContentHeightByParent()
        if (viewHolder != null) {
            mAnimatorAdapter?.itemClick(viewHolder, position)
        }
    }

    private fun initOrResetVelocityTracker() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain()
        } else {
            mVelocityTracker!!.clear()
        }
    }

    private fun initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain()
        }
    }

    private fun recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker!!.recycle()
            mVelocityTracker = null
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.action
        if (action == MotionEvent.ACTION_MOVE && mIsBeingDragged) {
            return true
        }
        if (viewScrollY == 0 && !canScrollVertically(1)) {
            return false
        }
        when (action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_MOVE -> {
                val activePointerId = mActivePointerId
                if (activePointerId == INVALID_POINTER) {
                }
                val pointerIndex = ev.findPointerIndex(activePointerId)
                if (pointerIndex == -1) {
                    Log.e(
                        TAG, "Invalid pointerId=" + activePointerId
                                + " in onInterceptTouchEvent"
                    )
                }
                val y = ev.getY(pointerIndex).toInt()
                val yDiff = Math.abs(y - mLastMotionY)
                if (yDiff > mTouchSlop) {
                    mIsBeingDragged = true
                    mLastMotionY = y
                    initVelocityTrackerIfNotExists()
                    mVelocityTracker!!.addMovement(ev)
                    mNestedYOffset = 0
                    val parent = parent
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
            }
            MotionEvent.ACTION_DOWN -> {
                val y = ev.y.toInt()
                mLastMotionY = y
                mActivePointerId = ev.getPointerId(0)
                initOrResetVelocityTracker()
                mVelocityTracker!!.addMovement(ev)
                mIsBeingDragged = !mScroller!!.isFinished
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                mIsBeingDragged = false
                mActivePointerId = INVALID_POINTER
                recycleVelocityTracker()
                if (mScroller!!.springBack(viewScrollX, viewScrollY, 0, 0, 0, scrollRange)) {
                    postInvalidate()
                }
            }
            MotionEvent.ACTION_POINTER_UP -> onSecondaryPointerUp(ev)
        }
        if (!mScrollEnable) {
            mIsBeingDragged = false
        }
        return mIsBeingDragged
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!mIsBeingDragged) {
            super.onTouchEvent(ev)
        }
        if (!mScrollEnable) {
            return true
        }
        initVelocityTrackerIfNotExists()
        val vtev = MotionEvent.obtain(ev)
        val actionMasked = ev.actionMasked
        if (actionMasked == MotionEvent.ACTION_DOWN) {
            mNestedYOffset = 0
        }
        vtev.offsetLocation(0f, mNestedYOffset.toFloat())
        when (actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (childCount == 0) {
                    return false
                }
                if (!mScroller!!.isFinished.also { mIsBeingDragged = it }) {
                    val parent = parent
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
                if (!mScroller!!.isFinished) {
                    mScroller!!.abortAnimation()
                }
                mLastMotionY = ev.y.toInt()
                mActivePointerId = ev.getPointerId(0)
            }
            MotionEvent.ACTION_MOVE -> {
                val activePointerIndex = ev.findPointerIndex(mActivePointerId)
                if (activePointerIndex == -1) {
                    Log.e(
                        TAG,
                        "Invalid pointerId=$mActivePointerId in onTouchEvent"
                    )
                }
                val y = ev.getY(activePointerIndex).toInt()
                var deltaY = mLastMotionY - y
                if (!mIsBeingDragged && Math.abs(deltaY) > mTouchSlop) {
                    val parent = parent
                    parent?.requestDisallowInterceptTouchEvent(true)
                    mIsBeingDragged = true
                    if (deltaY > 0) {
                        deltaY -= mTouchSlop
                    } else {
                        deltaY += mTouchSlop
                    }
                }
                if (mIsBeingDragged) {
                    mLastMotionY = y - mScrollOffset[1]
                    val range = scrollRange
                    if (mScrollDelegate is StackScrollDelegateImpl) {
                        mScrollDelegate!!.scrollViewTo(0, deltaY + mScrollDelegate!!.viewScrollX)
                    } else {
                        if (overScrollBy(
                                0, deltaY, 0, viewScrollY,
                                0, range, 0, 0, true
                            )
                        ) {
                            mVelocityTracker!!.clear()
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                if (mIsBeingDragged) {
                    val velocityTracker = mVelocityTracker
                    velocityTracker!!.computeCurrentVelocity(1000, mMaximumVelocity.toFloat())
                    val initialVelocity = velocityTracker.getYVelocity(mActivePointerId).toInt()
                    if (childCount > 0) {
                        if (Math.abs(initialVelocity) > mMinimumVelocity) {
                            fling(-initialVelocity)
                        } else {
                            if (mScroller!!.springBack(
                                    viewScrollX, mScrollDelegate!!.viewScrollY, 0, 0, 0,
                                    scrollRange
                                )
                            ) {
                                postInvalidate()
                            }
                        }
                        mActivePointerId = INVALID_POINTER
                    }
                }
                endDrag()
            }
            MotionEvent.ACTION_CANCEL -> {
                if (mIsBeingDragged && childCount > 0) {
                    if (mScroller!!.springBack(
                            viewScrollX, mScrollDelegate!!.viewScrollY, 0, 0, 0,
                            scrollRange
                        )
                    ) {
                        postInvalidate()
                    }
                    mActivePointerId = INVALID_POINTER
                }
                endDrag()
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                val index = ev.actionIndex
                mLastMotionY = ev.getY(index).toInt()
                mActivePointerId = ev.getPointerId(index)
            }
            MotionEvent.ACTION_POINTER_UP -> {
                onSecondaryPointerUp(ev)
                mLastMotionY = ev.getY(ev.findPointerIndex(mActivePointerId)).toInt()
            }
        }
        if (mVelocityTracker != null) {
            mVelocityTracker!!.addMovement(vtev)
        }
        vtev.recycle()
        return true
    }

    private fun onSecondaryPointerUp(ev: MotionEvent) {
        val pointerIndex = ev.action and MotionEvent.ACTION_POINTER_INDEX_MASK shr
                MotionEvent.ACTION_POINTER_INDEX_SHIFT
        val pointerId = ev.getPointerId(pointerIndex)
        if (pointerId == mActivePointerId) {
            val newPointerIndex = if (pointerIndex == 0) 1 else 0
            mLastMotionY = ev.getY(newPointerIndex).toInt()
            mActivePointerId = ev.getPointerId(newPointerIndex)
            if (mVelocityTracker != null) {
                mVelocityTracker!!.clear()
            }
        }
    }

    private val scrollRange: Int
        private get() {
            var scrollRange = 0
            if (childCount > 0) {
                scrollRange = Math.max(
                    0,
                    totalLength - showHeight
                )
            }
            return scrollRange
        }

    override fun computeVerticalScrollRange(): Int {
        val count = childCount
        val contentHeight = showHeight
        if (count == 0) {
            return contentHeight
        }
        var scrollRange = totalLength
        val scrollY: Int = mScrollDelegate!!.viewScrollY
        val overscrollBottom = Math.max(0, scrollRange - contentHeight)
        if (scrollY < 0) {
            scrollRange -= scrollY
        } else if (scrollY > overscrollBottom) {
            scrollRange += scrollY - overscrollBottom
        }
        return scrollRange
    }

    override fun onOverScrolled(
        scrollX: Int, scrollY: Int,
        clampedX: Boolean, clampedY: Boolean
    ) {
        if (!mScroller!!.isFinished) {
            val oldX: Int = mScrollDelegate!!.viewScrollX
            val oldY: Int = mScrollDelegate!!.viewScrollY
            mScrollDelegate!!.viewScrollX = (scrollX)
            mScrollDelegate!!.viewScrollY = (scrollY)
            onScrollChanged(
                mScrollDelegate!!.viewScrollX,
                mScrollDelegate!!.viewScrollY,
                oldX,
                oldY
            )
            if (clampedY) {
                mScroller!!.springBack(
                    mScrollDelegate!!.viewScrollX, mScrollDelegate!!.viewScrollY, 0, 0, 0,
                    scrollRange
                )
            }
        } else {
            super.scrollTo(scrollX, scrollY)
        }
    }

    override fun computeVerticalScrollOffset(): Int {
        return Math.max(0, super.computeVerticalScrollOffset())
    }

    override fun computeScroll() {
        if (mScroller!!.computeScrollOffset()) {
            mScrollDelegate!!.scrollViewTo(0, mScroller!!.currY)
            postInvalidate()
        }
    }

    fun fling(velocityY: Int) {
        if (childCount > 0) {
            val height = showHeight
            val bottom = totalLength
            mScroller!!.fling(
                mScrollDelegate!!.viewScrollX,
                mScrollDelegate!!.viewScrollY,
                0,
                velocityY,
                0,
                0,
                0,
                Math.max(0, bottom - height),
                0,
                0
            )
            postInvalidate()
        }
    }

    override fun scrollTo(x: Int, y: Int) {
        var x = x
        var y = y
        if (childCount > 0) {
            x = clamp(x, width - paddingRight - paddingLeft, width)
            y = clamp(y, showHeight, totalLength)
            if (x != mScrollDelegate!!.viewScrollX || y != mScrollDelegate!!.viewScrollY) {
                super.scrollTo(x, y)
            }
        }
    }

    override var viewScrollX: Int
        get() = scrollX
        set(x) {
            scrollX = x
        }

    override fun scrollViewTo(x: Int, y: Int) {
        scrollTo(x, y)
    }

    override var viewScrollY: Int
        get() = scrollY
        set(y) {
            scrollY = y
        }

    private fun endDrag() {
        mIsBeingDragged = false
        recycleVelocityTracker()
    }

    override fun generateLayoutParams(attrs: AttributeSet): ViewGroup.LayoutParams {
        return LayoutParams(context, attrs)
    }

    override fun generateDefaultLayoutParams(): ViewGroup.LayoutParams {
        return LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun generateLayoutParams(p: ViewGroup.LayoutParams): ViewGroup.LayoutParams {
        return LayoutParams(p)
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams): Boolean {
        return p is LayoutParams
    }

    class LayoutParams : MarginLayoutParams {
        var mHeaderHeight = 0

        constructor(c: Context, attrs: AttributeSet?) : super(c, attrs) {
            val array = c.obtainStyledAttributes(attrs, R.styleable.CardStackView)
            mHeaderHeight =
                array.getDimensionPixelSize(R.styleable.CardStackView_stackHeaderHeight, -1)
        }

        constructor(width: Int, height: Int) : super(width, height) {}
        constructor(source: ViewGroup.LayoutParams?) : super(source) {}
    }

    abstract class Adapter<VH : ViewHolder?> {
        private val mObservable = AdapterDataObservable()
        fun createView(parent: ViewGroup?, viewType: Int): VH {
            val holder = onCreateView(parent, viewType)
            holder!!.mItemViewType = viewType
            return holder
        }

        protected abstract fun onCreateView(parent: ViewGroup?, viewType: Int): VH
        fun bindViewHolder(holder: VH, position: Int) {
            onBindViewHolder(holder, position)
        }

        protected abstract fun onBindViewHolder(holder: VH, position: Int)
        abstract val itemCount: Int

        fun getItemViewType(position: Int): Int {
            return 0
        }

        fun notifyDataSetChanged() {
            mObservable.notifyChanged()
        }

        fun registerObserver(observer: AdapterDataObserver) {
            mObservable.registerObserver(observer)
        }
    }

    abstract class ViewHolder(var itemView: View) {
        var mItemViewType = INVALID_TYPE
        var position = 0

        val context: Context
            get() = itemView.context

        abstract fun onItemExpand(b: Boolean)
        fun onAnimationStateChange(state: Int, willBeSelect: Boolean) {}
    }

    class AdapterDataObservable :
        Observable<AdapterDataObserver?>() {
        fun hasObservers(): Boolean {
            return !mObservers.isEmpty()
        }

        fun notifyChanged() {
            for (i in mObservers.indices.reversed()) {
                mObservers[i]!!.onChanged()
            }
        }
    }

    abstract class AdapterDataObserver {
        open fun onChanged() {}
    }

    private inner class ViewDataObserver : AdapterDataObserver() {
        override fun onChanged() {
            refreshView()
        }
    }

    var selectPosition: Int
        get() = mSelectPosition
        set(selectPosition) {
            mSelectPosition = selectPosition
            itemExpendListener!!.onItemExpend(mSelectPosition != DEFAULT_SELECT_POSITION)
        }

    fun setScrollEnable(scrollEnable: Boolean) {
        mScrollEnable = scrollEnable
    }

    var duration: Int
        get() = if (mAnimatorAdapter != null) mDuration else 0
        set(duration) {
            mDuration = duration
        }
    val scrollDelegate: ScrollDelegate?
        get() = mScrollDelegate

    interface ItemExpendListener {
        fun onItemExpend(expend: Boolean)
    }

    companion object {
        private const val INVALID_POINTER = -1
        const val INVALID_TYPE = -1
        const val ANIMATION_STATE_START = 0
        const val ANIMATION_STATE_END = 1
        const val ANIMATION_STATE_CANCEL = 2
        private const val TAG = "CardStackView"
        const val ALL_DOWN = 0
        const val UP_DOWN = 1
        const val UP_DOWN_STACK = 2
        const val DEFAULT_SELECT_POSITION = -1
        private fun clamp(n: Int, my: Int, child: Int): Int {
            if (my >= child || n < 0) {
                return 0
            }
            return if (my + n > child) {
                child - my
            } else n
        }
    }
}