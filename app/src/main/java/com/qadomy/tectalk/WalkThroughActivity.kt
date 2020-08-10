package com.qadomy.tectalk

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout
import com.qadomy.tectalk.adapter.IntroViewPagerAdapter
import com.qadomy.tectalk.model.ScreenItem
import com.qadomy.tectalk.ui.main_avtivity.MainActivity
import kotlinx.android.synthetic.main.activity_walk_through.*
import java.util.*

class WalkThroughActivity : AppCompatActivity() {

    private var introViewPagerAdapter: IntroViewPagerAdapter? = null
    private var position = 0
    private var btnAnim: Animation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // when this activity is about to be launch we need to check if its opened before or not
        if (restorePrefData()) {
            startActivity(Intent(applicationContext, MainActivity::class.java))
            finish()
        }

        setContentView(R.layout.activity_walk_through)

        // init animation
        btnAnim = AnimationUtils.loadAnimation(
            applicationContext,
            R.anim.button_animation
        )


        // region fill list screen
        val mList: MutableList<ScreenItem> = ArrayList()
        mList.add(
            ScreenItem(
                "One to One Chat",
                "Chat with your friends with simple and secure realtime messaging",
                R.drawable.ic_undraw_chatting_2yvo
            )
        )
        mList.add(
            ScreenItem(
                "Share Files",
                "Send photos, videos , audios instantly to your friends",
                R.drawable.ic_undraw_online_connection_6778
            )
        )
        mList.add(
            ScreenItem(
                "Group Chat",
                "Create groups and add more friends to increase your connection. No limits on the size of group chats.",
                R.drawable.ic_undraw_group_chat_unwm
            )
        )
        mList.add(
            ScreenItem(
                "Beautiful Camera",
                "Express yourself with your beautiful selfies and connect with friends",
                R.drawable.ic_undraw_selfie_time_cws4
            )
        )

        mList.add(
            ScreenItem(
                "Augmented Reality",
                "Use the power of augmented reality to bring your dream world in front of you",
                R.drawable.arselfie_card_icon
            )
        )
        mList.add(
            ScreenItem(
                "Audio and Video Calls",
                "Call your friends and family free (Yet to implement)",
                R.drawable.ic_undraw_group_hangout_5gmq
            )
        )
        mList.add(
            ScreenItem(
                "Live Streaming",
                "Enjoy Live streaming by you or your friends",
                R.drawable.ic_undraw_youtube_tutorial_2gn3
            )
        )// endregion


        // setup viewpager
        introViewPagerAdapter = IntroViewPagerAdapter(this@WalkThroughActivity, mList)
        // TODO: 8/9/20 we need to switch from viewPager to viewPager2
        screenPager.adapter = introViewPagerAdapter


        // next button click Listener
        btn_next.setOnClickListener {
            // set current item in viewPager in position
            position = screenPager.currentItem
            if (position < mList.size) {
                position++
                screenPager.currentItem = position
            }
            if (position == mList.size - 1) { // when we reach to the last screen

                // show the GET-STARTED Button and hide the indicator and the next button
                loadLastScreen()
            }
        }


        // TODO: 8/9/20 tabIndicator not displayed
        // tab-layout add change listener
        tabIndicator.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab!!.position == mList.size - 1) {
                    loadLastScreen()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })


        // Get Started button click listener
        btn_get_started.setOnClickListener { //open main activity
            startActivity(Intent(applicationContext, MainActivity::class.java))
            // also we need to save a boolean value to storage so next time when the user run the app
            // we could know that he is already checked the intro screen activity
            // i'm going to use shared preferences to that process
            savePrefsData()
            finish()
        }

        // skip button click listener
        tv_skip.setOnClickListener {
            screenPager.currentItem = mList.size
            loadLastScreen()
        }

    }

    // save button clicked in shared preference to know in next time if opened the app or not 
    private fun savePrefsData() {
        val pref = applicationContext.getSharedPreferences(
            "myPrefs",
            Context.MODE_PRIVATE
        )
        val editor = pref.edit()
        editor.putBoolean("isIntroOpened", true)
        editor.apply()
    }

    // show the GET-STARTED Button and hide the indicator and the next button
    private fun loadLastScreen() {
        btn_next!!.visibility = View.INVISIBLE
        btn_get_started!!.visibility = View.VISIBLE
        tv_skip!!.visibility = View.INVISIBLE
        tabIndicator!!.visibility = View.INVISIBLE

        // Add an animation the get-started button
        btn_get_started!!.animation = btnAnim
    }


    // function for check if this page opened or not by get data from shared preference
    private fun restorePrefData(): Boolean {
        val pref = applicationContext.getSharedPreferences(
            "myPrefs",
            Context.MODE_PRIVATE
        )
        return pref.getBoolean("isIntroOpened", false)
    }
}