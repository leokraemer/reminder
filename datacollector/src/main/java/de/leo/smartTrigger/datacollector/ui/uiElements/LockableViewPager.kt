package de.leo.smartTrigger.datacollector.ui.uiElements

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.viewpager.widget.ViewPager


/**
 * Created by Leo on 14.03.2018.
 */
class LockableViewPager : ViewPager {

    var isPagingEnabled = true

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return this.isPagingEnabled && super.onTouchEvent(event)
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        return this.isPagingEnabled && super.onInterceptTouchEvent(event)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var tempHeightMeasureSpec = heightMeasureSpec

        var height = 0
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child.measure(widthMeasureSpec,
                          View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
            val h = child.measuredHeight
            if (h > height) height = h
        }

        if (height != 0) {
            tempHeightMeasureSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        }
        super.onMeasure(widthMeasureSpec, tempHeightMeasureSpec)
    }
}