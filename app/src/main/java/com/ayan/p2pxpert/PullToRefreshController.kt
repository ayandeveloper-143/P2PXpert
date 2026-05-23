package com.ayan.p2pxpert

import android.animation.ValueAnimator
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import androidx.core.widget.NestedScrollView
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlin.math.min
import android.view.animation.AlphaAnimation
import android.widget.ImageView
import androidx.core.animation.doOnEnd

class PullToRefreshHelper {

    interface OnRefreshListener {
        fun onRefresh()
        fun onRefreshComplete()
    }

    companion object {
        fun setupPullToRefresh(
            nestedScrollView: NestedScrollView,
            refreshHeader: LinearLayout,
            circularProgress: CircularProgressIndicator,
            onRefreshListener: OnRefreshListener? = null
        ): PullToRefreshController {
            return PullToRefreshController(nestedScrollView, refreshHeader, circularProgress, onRefreshListener)
        }
    }
}

class PullToRefreshController(
    private val nestedScrollView: NestedScrollView,
    private val refreshHeader: LinearLayout,
    private val circularProgress: CircularProgressIndicator,
    private val onRefreshListener: PullToRefreshHelper.OnRefreshListener?
) {

    private val checkIcon: ImageView = refreshHeader.findViewById(R.id.checkIcon)

    private var isRefreshing = false
    private var pullDownY = 0f
    private val refreshThreshold = 150f
    private var lastProgressValue = 0
    private var isBeingDragged = false

    init {
        initViews()
        setupPullToRefresh()
    }

    private fun initViews() {
        refreshHeader.translationY = -refreshHeader.height.toFloat()
        nestedScrollView.translationY = 0f
        refreshHeader.visibility = View.VISIBLE
        circularProgress.apply {
            isIndeterminate = false
            progress = 0
        }
    }

    private fun setupPullToRefresh() {
        nestedScrollView.setOnTouchListener { _, event ->
            if (isRefreshing) return@setOnTouchListener true

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (nestedScrollView.scrollY == 0 && !isRefreshing) {
                        pullDownY = event.y
                        isBeingDragged = false
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    if (!isRefreshing && nestedScrollView.scrollY == 0 && pullDownY > 0) {
                        val pullDistance = event.y - pullDownY
                        if (pullDistance > 0) {
                            isBeingDragged = true
                            val resistance = 0.6f
                            val adjustedDistance = pullDistance * resistance

                            val headerTranslation = -refreshHeader.height + min(adjustedDistance, refreshThreshold * 1.5f)
                            refreshHeader.translationY = headerTranslation

                            val contentTranslation = min(adjustedDistance, refreshThreshold * 1.5f)
                            nestedScrollView.translationY = contentTranslation

                            // fill circular progress as you pull (keep 20x scaling)
                            val targetProgress = ((adjustedDistance / refreshThreshold) * 20)
                                .toInt()
                                .coerceIn(0, 100)

                            // Smooth transition instead of direct set (avoids jump)
                            if (kotlin.math.abs(targetProgress - lastProgressValue) > 1) {
                                ValueAnimator.ofInt(lastProgressValue, targetProgress).apply {
                                    duration = 80  // very short, for natural motion
                                    interpolator = DecelerateInterpolator()
                                    addUpdateListener {
                                        val value = it.animatedValue as Int
                                        circularProgress.setProgressCompat(value, true)
                                    }
                                    start()
                                }
                                lastProgressValue = targetProgress
                            }

                            return@setOnTouchListener true
                        }
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isBeingDragged && !isRefreshing) {
                        val pullDistance = event.y - pullDownY
                        val adjustedDistance = pullDistance * 0.6f

                        if (adjustedDistance > refreshThreshold) {
                            startRefresh()
                        } else {
                            resetToStartPosition()
                        }
                        isBeingDragged = false
                    }
                    pullDownY = 0f
                }
            }
            false
        }
    }

    private fun startRefresh() {
        isRefreshing = true
        nestedScrollView.isNestedScrollingEnabled = false

        // Animate smoothly from current progress to 100%
        val fillAnimator = ValueAnimator.ofInt(lastProgressValue, 100).apply {
            duration = 250
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val value = it.animatedValue as Int
                circularProgress.setProgressCompat(value, true)
            }
            doOnEnd {
                circularProgress.isIndeterminate = true

                animateToPosition(
                    headerTargetY = 0f,
                    contentTargetY = refreshHeader.height.toFloat(),
                    duration = 350
                )

                onRefreshListener?.onRefresh()
            }
        }

        fillAnimator.start()
    }


    private fun resetToStartPosition() {
        val resetAnimator = ValueAnimator.ofInt(circularProgress.progress, 0).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                circularProgress.setProgressCompat(it.animatedValue as Int, true)
            }
        }
        resetAnimator.start()

        animateToPosition(
            headerTargetY = -refreshHeader.height.toFloat(),
            contentTargetY = 0f,
            duration = 400
        )
    }


    fun completeRefresh() {

        // ✅ Show check icon
        circularProgress.visibility = View.GONE
        checkIcon.visibility = View.VISIBLE

        // Fade-in animation for the check icon
        val fadeIn = AlphaAnimation(0f, 1f).apply {
            duration = 200
            fillAfter = true
        }
        checkIcon.startAnimation(fadeIn)


        // Animate back to start
        Handler(Looper.getMainLooper()).postDelayed({

            isRefreshing = false

            animateToPosition(
                headerTargetY = -refreshHeader.height.toFloat(),
                contentTargetY = 0f,
                duration = 400
            )

            Handler(Looper.getMainLooper()).postDelayed({
                nestedScrollView.isNestedScrollingEnabled = true
                circularProgress.visibility = View.VISIBLE
            }, 400)

            circularProgress.isIndeterminate = false
            circularProgress.setProgressCompat(100, true)

            onRefreshListener?.onRefreshComplete()

            checkIcon.visibility = View.GONE

            val fadeOut = AlphaAnimation(1f, 0f).apply {
                duration = 50
                fillAfter = true
            }
            checkIcon.startAnimation(fadeOut)

        }, 900)

    }

    private fun animateToPosition(headerTargetY: Float, contentTargetY: Float, duration: Long) {
        val headerAnimator = ValueAnimator.ofFloat(refreshHeader.translationY, headerTargetY)
        val contentAnimator = ValueAnimator.ofFloat(nestedScrollView.translationY, contentTargetY)

        headerAnimator.duration = duration
        headerAnimator.interpolator = DecelerateInterpolator()
        headerAnimator.addUpdateListener { animation ->
            refreshHeader.translationY = animation.animatedValue as Float
        }

        contentAnimator.duration = duration
        contentAnimator.interpolator = DecelerateInterpolator()
        contentAnimator.addUpdateListener { animation ->
            nestedScrollView.translationY = animation.animatedValue as Float
        }

        headerAnimator.start()
        contentAnimator.start()
    }
}
