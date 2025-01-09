/*
 * Module: r2-navigator-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readiumd.dekd.navigator.pager

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager
import org.readiumd.dekd.navigator.BuildConfig.DEBUG
import org.readiumd.dekd.shared.publication.Publication
import timber.log.Timber
import java.lang.Math.abs


class R2ViewPager : ViewPager {


    lateinit var type: Publication.TYPE

    private var mStartDragX = 0f
    private var mStartDragY = 0f
    var mOnSwipeOutListener: OnSwipeOutListener? = null

    private val SWIPE_THRESHOLD = 100

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun setCurrentItem(item: Int) {
        super.setCurrentItem(item, false)
    }

    fun setOnSwipeOutListener(listener: OnSwipeOutListener) {
        mOnSwipeOutListener = listener
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (DEBUG) Timber.d("ev.action ${ev.action}")
        if (type == Publication.TYPE.EPUB) {
            if (currentItem == adapter!!.count - 1) {
                when (ev.action) {
                    MotionEvent.ACTION_DOWN -> {
                        mStartDragX = ev.x
                        mStartDragY = ev.y
                    }
                    MotionEvent.ACTION_UP -> {
                        val deltaX = ev.x - mStartDragX
                        val deltaY = ev.y - mStartDragY
                        if (abs(deltaX) > SWIPE_THRESHOLD || abs(deltaY) > SWIPE_THRESHOLD) {
                            if (abs(deltaX) > abs(deltaY)) {
                                if (deltaX < 0) {
                                    // User has swiped left
                                    mOnSwipeOutListener?.onSwipeOutAtEnd()
                                } else if (deltaX > 0) {
                                    // User has swiped right
                                    mOnSwipeOutListener?.onSwipeOutAtStart()
                                }
                            } else {
                                if (deltaY < 0) {
                                    // User has swiped up
                                    if (currentItem == adapter!!.count - 1) {
                                        // User has swiped up and there are no more pages to show
                                        mOnSwipeOutListener?.onSwipeOutAtBottom()
                                    }
                                } else if (deltaY > 0) {
                                    // User has swiped down
                                    if (currentItem == 0) {
                                        // User has swiped down and there are no more pages to show
                                        mOnSwipeOutListener?.onSwipeOutAtTop()
                                    }
                                }
                            }
                        }
                    }
                }
                // Forward the touch event to the ViewPager
                return super.dispatchTouchEvent(ev)
            } else {
                mStartDragX = 0f
                mStartDragY = 0f
            }
            return super.dispatchTouchEvent(ev)
        }
        return try {
            // The super implementation sometimes triggers:
            // java.lang.IllegalArgumentException: pointerIndex out of range
            // i.e. https://stackoverflow.com/q/48496257/1474476
            return super.dispatchTouchEvent(ev)

        } catch (ex: IllegalArgumentException) {
            Timber.e(ex)
            false
        }
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (DEBUG) Timber.d("ev.action ${ev.action}")
        if (type == Publication.TYPE.EPUB) {
            when (ev.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    // prevent swipe from view pager directly
                    if (DEBUG) Timber.d("ACTION_DOWN")
                    return false
                }
            }
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
                    // prevent swipe from view pager directly
                    return false
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
        fun onSwipeOutAtBottom()
        fun onSwipeOutAtTop()
    }

}

