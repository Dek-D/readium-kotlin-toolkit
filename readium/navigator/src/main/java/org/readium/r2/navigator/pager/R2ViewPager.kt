/*
 * Module: r2-navigator-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator.pager

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager
import org.readium.r2.navigator.BuildConfig.DEBUG
import org.readium.r2.shared.publication.Publication
import timber.log.Timber


class R2ViewPager : ViewPager {


    lateinit var type: Publication.TYPE

    var mStartDragX = 0f
    var mOnSwipeOutListener: OnSwipeOutListener? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun setCurrentItem(item: Int) {
        super.setCurrentItem(item, false)
    }

    fun setOnSwipeOutListener(listener: OnSwipeOutListener) {
        mOnSwipeOutListener = listener
    }

    private fun onSwipeOutAtStart() {
        if (mOnSwipeOutListener != null) {
            mOnSwipeOutListener!!.onSwipeOutAtStart()
        }
    }

    private fun onSwipeOutAtEnd() {
        if (mOnSwipeOutListener != null) {
            mOnSwipeOutListener!!.onSwipeOutAtEnd()
        }
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (DEBUG) Timber.d("ev.action ${ev.action}")
        if (type == Publication.TYPE.EPUB) {
            if (currentItem == adapter!!.count - 1) {
                val x: Float = ev.x
                when (ev.action and MotionEvent.ACTION_MASK) {
                    MotionEvent.ACTION_DOWN -> mStartDragX = x
                    MotionEvent.ACTION_MOVE -> {}
                    MotionEvent.ACTION_UP -> if (x < mStartDragX) {
                        onSwipeOutAtEnd()
                    } else {
                        mStartDragX = 0f
                    }
                }
            } else {
                mStartDragX = 0f
            }
            return super.onTouchEvent(ev)
        }

        return try {
            // The super implementation sometimes triggers:
            // java.lang.IllegalArgumentException: pointerIndex out of range
            // i.e. https://stackoverflow.com/q/48496257/1474476
            return super.onTouchEvent(ev)

        } catch (ex: IllegalArgumentException) {
            Timber.e(ex)
            false
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (type == Publication.TYPE.EPUB) {
            when (ev.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    mStartDragX = ev.x
                    // prevent swipe from view pager directly
                    return super.onInterceptTouchEvent(ev)
                }
            }
        }

        return try {
            // The super implementation sometimes triggers:
            // java.lang.IllegalArgumentException: pointerIndex out of range
            // i.e. https://stackoverflow.com/q/48496257/1474476
            super.onInterceptTouchEvent(ev)
        } catch (ex: IllegalArgumentException) {
            Timber.e(ex)
            false
        }
    }

    interface OnSwipeOutListener {
        fun onSwipeOutAtStart()
        fun onSwipeOutAtEnd()
    }

}
