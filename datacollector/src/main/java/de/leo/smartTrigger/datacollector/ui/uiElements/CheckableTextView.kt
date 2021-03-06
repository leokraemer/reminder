package de.leo.smartTrigger.datacollector.ui.uiElements

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.View.OnClickListener
import android.widget.Checkable
import android.widget.TextView
import android.content.res.TypedArray
import android.graphics.PorterDuff
import de.leo.smartTrigger.datacollector.R
import kotlinx.android.synthetic.main.activity_trigger_list.view.*
import org.jetbrains.anko.attr


/**
 * Created by Leo on 01.03.2018.
 */
class CheckableTextView : TextView, Checkable {

    private var mChecked = false

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) :
        super(context, attrs, defStyleAttr)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) :
        super(context, attrs, defStyleAttr, defStyleRes)

    init {
        super.setOnClickListener { _ -> toggle() }
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val drawableState = super.onCreateDrawableState(extraSpace + 1)
        if (isChecked)
            View.mergeDrawableStates(drawableState, CHECKED_STATE_SET)
        return drawableState
    }

    override fun setChecked(checked: Boolean) {
        if (mChecked != checked) {
            mChecked = checked
            refreshDrawableState()
        }
    }

    override fun isChecked(): Boolean = mChecked

    //store the elevation set in the xml
    private var defaultElevation = elevation

    override fun setEnabled(enabled: Boolean) {
        elevation = if (enabled) defaultElevation else 0F
        super.setEnabled(enabled)
    }

    override fun toggle() {
        isChecked = !mChecked
    }

    override fun setOnClickListener(l: View.OnClickListener?) {
        val onClickListener = OnClickListener { v ->
            toggle()
            l!!.onClick(v)
        }
        super.setOnClickListener(onClickListener)
    }

    companion object {
        private val CHECKED_STATE_SET = intArrayOf(android.R.attr.state_checked)
    }
}