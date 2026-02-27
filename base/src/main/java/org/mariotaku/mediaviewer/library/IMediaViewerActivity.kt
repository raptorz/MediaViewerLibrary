package org.mariotaku.mediaviewer.library

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.viewpager.widget.ViewPager
import java.util.Locale

interface IMediaViewerActivity {
    fun isBarShowing(): Boolean

    fun setBarVisibility(visible: Boolean)

    fun toggleBar()

    val downloader: MediaDownloader

    val fileCache: FileCache

    fun getLayoutRes(): Int

    fun findViewPager(): ViewPager

    fun instantiateMediaFragment(position: Int): MediaViewerFragment

    fun getMediaCount(): Int

    fun getInitialPosition(): Int

    class Helper(private val activity: FragmentActivity) : ViewPager.OnPageChangeListener {

        private var viewPager: ViewPager? = null
        private var pagerAdapter: MediaPagerAdapter? = null

        fun onCreate(savedInstanceState: Bundle?) {
            activity.setContentView((activity as IMediaViewerActivity).getLayoutRes())
            pagerAdapter = MediaPagerAdapter(activity)
            viewPager?.adapter = pagerAdapter
            viewPager?.addOnPageChangeListener(this)
            val currentIndex = (activity as IMediaViewerActivity).getInitialPosition()
            if (currentIndex != -1) {
                viewPager?.setCurrentItem(currentIndex, false)
            }
            updatePositionTitle()
        }

        fun onContentChanged() {
            viewPager = (activity as IMediaViewerActivity).findViewPager()
        }

        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            // Do nothing
        }

        override fun onPageSelected(position: Int) {
            updatePositionTitle()
            (activity as IMediaViewerActivity).setBarVisibility(true)
        }

        override fun onPageScrollStateChanged(state: Int) {
            // Do nothing
        }

        private fun updatePositionTitle() {
            viewPager?.let {
                activity.title = String.format(
                    Locale.US,
                    "%d / %d",
                    it.currentItem + 1,
                    pagerAdapter?.count ?: 0
                )
            }
        }
    }

    class MediaPagerAdapter(activity: FragmentActivity) :
        androidx.fragment.app.FragmentStatePagerAdapter(activity.supportFragmentManager) {

        private val mediaViewerActivity = activity as IMediaViewerActivity

        override fun getCount(): Int {
            return mediaViewerActivity.getMediaCount()
        }

        override fun getItem(position: Int): MediaViewerFragment {
            return mediaViewerActivity.instantiateMediaFragment(position)
        }
    }
}