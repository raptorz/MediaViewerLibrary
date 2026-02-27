package org.mariotaku.mediaviewer.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.mariotaku.mediaviewer.library.databinding.FragmentMediaViewerBinding

abstract class MediaViewerFragment : Fragment() {

    private var _binding: FragmentMediaViewerBinding? = null
    protected val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    fun showProgress(indeterminate: Boolean, progress: Float) {
        val activity = activity ?: return
        if (binding.loadProgress.visibility != View.VISIBLE) {
            activity.supportInvalidateOptionsMenu()
        }
        binding.loadProgress.visibility = View.VISIBLE
        binding.loadProgress.isIndeterminate = indeterminate
        if (!indeterminate) {
            binding.loadProgress.progress = (progress * 100).toInt()
        }
    }

    fun hideProgress() {
        val activity = activity ?: return
        activity.supportInvalidateOptionsMenu()
        binding.loadProgress.visibility = View.GONE
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMediaViewerBinding.inflate(inflater, container, false)
        val mediaView = onCreateMediaView(inflater, binding.mediaContainer, savedInstanceState)
        if (mediaView.parent == null) {
            binding.mediaContainer.addView(mediaView)
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    protected abstract fun onCreateMediaView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedInstanceState: Bundle?
    ): View

    fun setMediaViewVisible(visible: Boolean) {
        val view = view ?: return
        val activity = activity ?: return
        activity.supportInvalidateOptionsMenu()
        binding.mediaContainer.visibility = if (visible) View.VISIBLE else View.GONE
    }

    abstract fun isMediaLoading(): Boolean

    abstract fun isMediaLoaded(): Boolean
}