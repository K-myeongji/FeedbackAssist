package com.volumeassist

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton

class IntroActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var indicatorLayout: LinearLayout
    private lateinit var skipButton: MaterialButton
    private lateinit var nextButton: MaterialButton
    private lateinit var preferences: SharedPreferences

    private val slides = listOf(
        Slide(
            R.drawable.ic_volume,
            "Welcome to Volume Assist",
            "Control your device volume and power without physical buttons. Perfect for devices with broken buttons!"
        ),
        Slide(
            R.drawable.ic_volume_up,
            "Floating Overlay Controls",
            "Tap the floating button to access volume controls, flashlight, and lock screen - all in one place."
        ),
        Slide(
            R.drawable.ic_settings,
            "Customize Everything",
            "Choose your overlay color, adjust transparency and size. Enable auto-start and system volume integration from settings."
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferences = getSharedPreferences("VolumeAssistPrefs", MODE_PRIVATE)

        // Check if intro has already been completed
        if (preferences.getBoolean("intro_completed", false)) {
            // Skip intro and go directly to MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_intro)

        initViews()
        setupViewPager()
        setupListeners()
    }

    private fun initViews() {
        viewPager = findViewById(R.id.viewPager)
        indicatorLayout = findViewById(R.id.indicatorLayout)
        skipButton = findViewById(R.id.skipButton)
        nextButton = findViewById(R.id.nextButton)
    }

    private fun setupViewPager() {
        val adapter = IntroAdapter(slides)
        viewPager.adapter = adapter

        // Setup indicators
        setupIndicators(slides.size)
        setCurrentIndicator(0)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                setCurrentIndicator(position)

                if (position == slides.size - 1) {
                    nextButton.text = "Get Started"
                } else {
                    nextButton.text = "Next"
                }
            }
        })
    }

    private fun setupIndicators(count: Int) {
        val indicators = arrayOfNulls<ImageView>(count)
        val layoutParams = LinearLayout.LayoutParams(
            24, 24
        ).apply {
            setMargins(8, 0, 8, 0)
        }

        for (i in indicators.indices) {
            indicators[i] = ImageView(this).apply {
                setImageDrawable(
                    ContextCompat.getDrawable(
                        this@IntroActivity,
                        R.drawable.indicator_inactive
                    )
                )
                this.layoutParams = layoutParams
            }
            indicatorLayout.addView(indicators[i])
        }
    }

    private fun setCurrentIndicator(index: Int) {
        val childCount = indicatorLayout.childCount
        for (i in 0 until childCount) {
            val imageView = indicatorLayout.getChildAt(i) as ImageView
            if (i == index) {
                imageView.setImageDrawable(
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.indicator_active
                    )
                )
            } else {
                imageView.setImageDrawable(
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.indicator_inactive
                    )
                )
            }
        }
    }

    private fun setupListeners() {
        skipButton.setOnClickListener {
            finishIntro()
        }

        nextButton.setOnClickListener {
            if (viewPager.currentItem + 1 < slides.size) {
                viewPager.currentItem += 1
            } else {
                finishIntro()
            }
        }
    }

    private fun finishIntro() {
        // Mark intro as completed
        preferences.edit().putBoolean("intro_completed", true).apply()

        // Start main activity
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    data class Slide(
        val icon: Int,
        val title: String,
        val description: String
    )

    inner class IntroAdapter(private val slides: List<Slide>) :
        androidx.recyclerview.widget.RecyclerView.Adapter<IntroAdapter.SlideViewHolder>() {

        inner class SlideViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
            val slideImage: ImageView = view.findViewById(R.id.slideImage)
            val slideTitle: TextView = view.findViewById(R.id.slideTitle)
            val slideDescription: TextView = view.findViewById(R.id.slideDescription)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): SlideViewHolder {
            val view = layoutInflater.inflate(R.layout.slide_item, parent, false)
            return SlideViewHolder(view)
        }

        override fun onBindViewHolder(holder: SlideViewHolder, position: Int) {
            val slide = slides[position]
            holder.slideImage.setImageResource(slide.icon)
            holder.slideTitle.text = slide.title
            holder.slideDescription.text = slide.description
        }

        override fun getItemCount(): Int = slides.size
    }
}