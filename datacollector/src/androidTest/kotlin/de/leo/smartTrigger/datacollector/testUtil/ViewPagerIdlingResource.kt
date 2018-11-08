package de.leo.smartTrigger.datacollector.testUtil

import androidx.test.espresso.IdlingResource
import androidx.test.espresso.IdlingResource.ResourceCallback
import androidx.viewpager.widget.ViewPager


class ViewPagerIdlingResource(viewPager: ViewPager, private val mName: String) : IdlingResource {

    private var mIdle = true // Default to idle since we can't query the scroll state.

    private var mResourceCallback: ResourceCallback? = null

    init {
        viewPager.addOnPageChangeListener(ViewPagerListener())
    }

    override fun getName(): String {
        return mName
    }

    override fun isIdleNow(): Boolean {
        return mIdle
    }

    override fun registerIdleTransitionCallback(resourceCallback: ResourceCallback) {
        mResourceCallback = resourceCallback
    }

    private inner class ViewPagerListener : ViewPager.SimpleOnPageChangeListener() {

        override fun onPageScrollStateChanged(state: Int) {
            mIdle = (state == ViewPager.SCROLL_STATE_IDLE
                // Treat dragging as idle, or Espresso will block itself when swiping.
                || state == ViewPager.SCROLL_STATE_DRAGGING)
            if (mIdle && mResourceCallback != null) {
                mResourceCallback!!.onTransitionToIdle()
            }
        }
    }
}