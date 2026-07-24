package com.ayan.p2pxpert

import android.animation.ObjectAnimator
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.progressindicator.CircularProgressIndicator

class HomeActivity : AppCompatActivity() {
    private lateinit var bannerViewPager: ViewPager2

    private lateinit var usdtChartWebView: WebView
    private lateinit var autoScrollHandler: Handler
    private lateinit var autoScrollRunnable: Runnable
    private lateinit var animationHandler: Handler
    private var animationRunnable: Runnable? = null

    private lateinit var refreshDashboardController: PullToRefreshController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)  
        setContentView(R.layout.main_activity)

        autoScrollHandler = Handler(Looper.getMainLooper())
        animationHandler = Handler(Looper.getMainLooper())

        setupNotificationBadge()
        setupLiveAnimation()
        setupBannerCarousel()
        setupWebView()
        setupPullToRefreshDashboard()
    }



    private fun setupPullToRefreshDashboard() {
        val nestedScrollView = findViewById<NestedScrollView>(R.id.nestedScrollView)
        val refreshHeader = findViewById<LinearLayout>(R.id.refreshHeader)
        val circularProgress = findViewById<CircularProgressIndicator>(R.id.circular_progress)

        refreshDashboardController = PullToRefreshHelper.setupPullToRefresh(
            nestedScrollView,
            refreshHeader,
            circularProgress,
            object : PullToRefreshHelper.OnRefreshListener {
                override fun onRefresh() {
                    // Refresh logic here
                    Handler(Looper.getMainLooper()).postDelayed({
                        refreshDashboardController.completeRefresh()
                    }, 2000)
                }

                override fun onRefreshComplete() {
                    // On Refresh Complete
                }
            }
        )

    }




    override fun onResume() {
        super.onResume()
        startLiveAnimation()
    }

    override fun onPause() {
        super.onPause()
        stopLiveAnimation()
    }

    override fun onDestroy() {
        super.onDestroy()
        autoScrollHandler.removeCallbacks(autoScrollRunnable)
        stopLiveAnimation()
    }

    private fun setupNotificationBadge() {
        val badge = findViewById<TextView>(R.id.notification_badge)
        badge.text = "3"
    }

    private fun setupLiveAnimation() {
        // Method 1: Using AnimationDrawable (Recommended)
        setupAnimationDrawable()

        // Method 2: Using Manual Handler Animation (Alternative)
        // setupManualAnimation()

        // Method 3: Using ObjectAnimator (If above don't work)
        // setupObjectAnimator()
    }

    private fun setupAnimationDrawable() {
        val liveIndicator = findViewById<View>(R.id.live_indicator)
        val trendLiveIndicator = findViewById<View>(R.id.trend_live_indicator)

        // Set animation drawable as background
        liveIndicator.setBackgroundResource(R.drawable.live_indicator_animation)
        trendLiveIndicator.setBackgroundResource(R.drawable.live_indicator_animation)

        // Start animation
        val liveAnimation = liveIndicator.background as? AnimationDrawable
        val trendAnimation = trendLiveIndicator.background as? AnimationDrawable

        liveAnimation?.start()
        trendAnimation?.start()
    }

    private fun setupManualAnimation() {
        val liveIndicator = findViewById<View>(R.id.live_indicator)
        val trendLiveIndicator = findViewById<View>(R.id.trend_live_indicator)

        var isRed = true

        animationRunnable = object : Runnable {
            override fun run() {
                val color = if (isRed) {
                    Color.parseColor("#f6465d")
                } else {
                    Color.parseColor("#30f6465d")
                }

                liveIndicator.setBackgroundColor(color)
                trendLiveIndicator.setBackgroundColor(color)
                isRed = !isRed

                animationHandler.postDelayed(this, 500)
            }
        }
        animationHandler.post(animationRunnable!!)
    }

    private fun setupObjectAnimator() {
        val liveIndicator = findViewById<View>(R.id.live_indicator)
        val trendLiveIndicator = findViewById<View>(R.id.trend_live_indicator)

        // Create color animation
        val blinkAnimation = ObjectAnimator.ofArgb(
            liveIndicator,
            "backgroundColor",
            Color.parseColor("#f6465d"),
            Color.parseColor("#30f6465d")
        )
        blinkAnimation.duration = 1000
        blinkAnimation.repeatCount = ObjectAnimator.INFINITE
        blinkAnimation.repeatMode = ObjectAnimator.REVERSE
        blinkAnimation.start()

        val trendBlinkAnimation = ObjectAnimator.ofArgb(
            trendLiveIndicator,
            "backgroundColor",
            Color.parseColor("#f6465d"),
            Color.parseColor("#30f6465d")
        )
        trendBlinkAnimation.duration = 1000
        trendBlinkAnimation.repeatCount = ObjectAnimator.INFINITE
        trendBlinkAnimation.repeatMode = ObjectAnimator.REVERSE
        trendBlinkAnimation.start()
    }

    private fun stopLiveAnimation() {
        // Stop AnimationDrawable
        val liveIndicator = findViewById<View>(R.id.live_indicator)
        val trendLiveIndicator = findViewById<View>(R.id.trend_live_indicator)

        (liveIndicator.background as? AnimationDrawable)?.stop()
        (trendLiveIndicator.background as? AnimationDrawable)?.stop()

        // Stop manual animation
        animationRunnable?.let { animationHandler.removeCallbacks(it) }

        // Clear ObjectAnimator
        liveIndicator.clearAnimation()
        trendLiveIndicator.clearAnimation()
    }

    private fun startLiveAnimation() {
        setupLiveAnimation()
    }

    private fun setupBannerCarousel() {
        bannerViewPager = findViewById(R.id.bannerViewPager)


        bannerViewPager.clipToPadding = false
        bannerViewPager.clipChildren = false

        val bannerList = listOf(
            BannerItem(R.drawable.banner1, "Promo 1"),
            BannerItem(R.drawable.banner1, "Promo 2"),
            BannerItem(R.drawable.banner1, "Promo 3")
        )

        val adapter = BannerAdapter(bannerList)
        bannerViewPager.adapter = adapter
        bannerViewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL

        setupAutoScroll()
    }



    private fun setupAutoScroll() {
        bannerViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {

            }
        })

        autoScrollRunnable = object : Runnable {
            override fun run() {
                val itemCount = bannerViewPager.adapter?.itemCount ?: 1
                val nextItem = (bannerViewPager.currentItem + 1) % itemCount
                bannerViewPager.setCurrentItem(nextItem, true)
                autoScrollHandler.postDelayed(this, 3000)
            }
        }
        autoScrollHandler.postDelayed(autoScrollRunnable, 3000)
    }



    private fun setupWebView() {
        usdtChartWebView = findViewById(R.id.usdtChartWebView)

        val webSettings = usdtChartWebView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true

        usdtChartWebView.webViewClient = WebViewClient()
        usdtChartWebView.loadUrl("https://p2pxpert.com/charts/usdt.html")
    }


    /// last


    // Extension for dp conversion
    private val Int.dp: Int
        get() = (this * Resources.getSystem().displayMetrics.density).toInt()
}

class BannerAdapter(private val bannerList: List<BannerItem>) :
    RecyclerView.Adapter<BannerAdapter.BannerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_banner, parent, false)

        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        return BannerViewHolder(view)
    }

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        holder.bind(bannerList[position])
    }

    override fun getItemCount() = bannerList.size

    class BannerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val bannerImage: ImageView = itemView.findViewById(R.id.bannerImage)

        fun bind(banner: BannerItem) {
            bannerImage.setImageResource(banner.imageRes)
            bannerImage.scaleType = ImageView.ScaleType.CENTER_CROP
        }
    }
}

data class BannerItem(val imageRes: Int, val title: String)