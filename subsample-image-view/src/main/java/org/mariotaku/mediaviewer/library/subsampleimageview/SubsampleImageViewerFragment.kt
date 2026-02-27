package org.mariotaku.mediaviewer.library.subsampleimageview

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IntDef
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.loader.app.LoaderManager
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import org.mariotaku.mediaviewer.library.CacheDownloadLoader
import org.mariotaku.mediaviewer.library.CacheDownloadMediaViewerFragment
import org.mariotaku.mediaviewer.library.IMediaViewerActivity
import org.mariotaku.mediaviewer.library.subsampleimageview.databinding.LayoutMediaViewerSubsampleImageViewBinding

class SubsampleImageViewerFragment : CacheDownloadMediaViewerFragment(),
    CacheDownloadLoader.Listener,
    LoaderManager.LoaderCallbacks<CacheDownloadLoader.Result>,
    View.OnClickListener {

    companion object {
        const val EXTRA_MEDIA_URI = "media_url"

        fun get(mediaUri: Uri): SubsampleImageViewerFragment {
            val args = Bundle()
            args.putParcelable(EXTRA_MEDIA_URI, mediaUri)
            val f = SubsampleImageViewerFragment()
            f.arguments = args
            return f
        }
    }

    private var _binding: LayoutMediaViewerSubsampleImageViewBinding? = null
    private val binding get() = _binding!!

    private var hasPreview = false

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
        binding.imageView.setOnClickListener(this)
        binding.imageView.setOnImageEventListener(object :
            SubsamplingScaleImageView.DefaultOnImageEventListener() {

            private var previewLoadError = false
            private var imageLoadError = false

            override fun onReady() {
                previewLoadError = false
                imageLoadError = false
                onMediaLoadStateChange(State.READY)
            }

            override fun onImageLoaded() {
                previewLoadError = false
                imageLoadError = false
                onMediaLoadStateChange(State.LOADED)
            }

            override fun onPreviewLoadError(e: Exception) {
                previewLoadError = true
                if (hasPreview && imageLoadError) {
                    onMediaLoadStateChange(State.ERROR)
                }
            }

            override fun onImageLoadError(e: Exception) {
                imageLoadError = true
                if (hasPreview && previewLoadError) {
                    onMediaLoadStateChange(State.ERROR)
                }
            }
        })
        setupImageView(binding.imageView)
        startLoading(false)
        showProgress(true, 0f)
        setMediaViewVisible(false)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = LayoutMediaViewerSubsampleImageViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onClick(v: View) {
        val activity = activity as? IMediaViewerActivity ?: return
        activity.toggleBar()
    }

    override fun onCreateMediaView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedInstanceState: Bundle?
    ): View {
        // This method is called from the parent fragment's onCreateView
        // We already have the binding from onCreateView, so we just return the image view
        return binding.root
    }

    override fun isAbleToLoad(): Boolean {
        return getDownloadUri() != null
    }

    @Nullable
    override fun getDownloadUri(): Uri? {
        return arguments?.getParcelable(EXTRA_MEDIA_URI)
    }

    override fun getDownloadExtra(): Any? {
        return null
    }

    override fun displayMedia(data: CacheDownloadLoader.Result) {
        onMediaLoadStateChange(State.NONE)
        if (data.cacheUri != null) {
            setMediaViewVisible(true)
            val previewSource = getPreviewImageSource(data)
            hasPreview = previewSource != null
            binding.imageView.setImage(getImageSource(data), previewSource)
        } else {
            setMediaViewVisible(false)
        }
    }

    @NonNull
    protected fun getImageSource(@NonNull data: CacheDownloadLoader.Result): ImageSource {
        requireNotNull(data.cacheUri)
        val imageSource = ImageSource.uri(data.cacheUri)
        imageSource.tilingEnabled()
        return imageSource
    }

    @Nullable
    protected fun getPreviewImageSource(@NonNull data: CacheDownloadLoader.Result): ImageSource? {
        return null
    }

    override fun releaseMediaResources() {
        binding.imageView.recycle()
    }

    protected fun onMediaLoadStateChange(@State state: Int) {
        // Subclasses can override this
    }

    protected fun setupImageView(imageView: SubsamplingScaleImageView) {
        // Subclasses can override this
    }

    @IntDef(State.NONE, State.READY, State.LOADED, State.ERROR)
    @Retention(AnnotationRetention.SOURCE)
    annotation class State {
        companion object {
            const val NONE = 0
            const val READY = 1
            const val LOADED = 2
            const val ERROR = -1
        }
    }
}