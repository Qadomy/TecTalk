package com.qadomy.tectalk.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.viewpager.widget.PagerAdapter
import com.qadomy.tectalk.R
import com.qadomy.tectalk.model.ScreenItem

/**
 *
 * Adapter for set screens to viewPager
 */

class IntroViewPagerAdapter(
    private var mContext: Context,
    private var mListScreen: List<ScreenItem>
) : PagerAdapter() {

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val inflater =
            mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val layoutScreen: View = inflater.inflate(R.layout.layout_screen, container, false)

        // views
        val imgSlide =
            layoutScreen.findViewById<ImageView>(R.id.intro_img)
        val title = layoutScreen.findViewById<TextView>(R.id.intro_title)
        val description = layoutScreen.findViewById<TextView>(R.id.intro_description)

        // set data in views
        title.text = mListScreen[position].title
        description.text = mListScreen[position].description
        imgSlide.setImageResource(mListScreen[position].screenImg)

        container.addView(layoutScreen)
        return layoutScreen
    }


    override fun isViewFromObject(view: View, `object`: Any) = view === `object`

    override fun getCount() = mListScreen.size

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

}