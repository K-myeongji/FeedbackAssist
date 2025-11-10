package com.feedbackassist

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
            R.drawable.ic_feedback, // 아이콘은 R.drawable에 맞게 수정해주세요
            "Welcome to Feedback Assist",
            "Easily capture screenshots and record your feedback. Perfect for app testing and bug reporting."
        ),
        Slide(
            R.drawable.ic_screenshot, // 아이콘은 R.drawable에 맞게 수정해주세요
            "Automatic Screenshot Detection",
            "The app automatically detects when you take a screenshot and prepares it for your feedback."
        ),
        Slide(
            R.drawable.ic_save, // 아이콘은 R.drawable에 맞게 수정해주세요
            "Record and Save",
            "Add a title and detailed description to your screenshot, then save it directly to your device."
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