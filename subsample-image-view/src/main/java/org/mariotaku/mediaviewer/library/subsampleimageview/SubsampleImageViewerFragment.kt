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

open class SubsampleImageViewerFragment : CacheDownloadMediaViewerFragment(),
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
    private val subBinding get() = _binding!!

    private var hasPreview = false

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
        subBinding.imageView.setOnClickListener(this)
        subBinding.imageView.setOnImageEventListener(object :
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
        setupImageView(subBinding.imageView)
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
        return subBinding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onClick(v: View) {
        val activity = activity as? IMediaViewerActivity ?: return
        activity.toggleBar()
    }

    override fun isBarShowing(): Boolean {
        val activity = activity as? IMediaViewerActivity ?: return false
        return activity.isBarShowing
    }

    override fun isMediaLoading(): Boolean {
        return false
    }

    override fun isMediaLoaded(): Boolean {
        return hasDownloadedData()
    }

    override fun onCreateMediaView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedInstanceState: Bundle?
    ): View {
        // This method is called from the parent fragment's onCreateView
        // We already have the binding from onCreateView, so we just return the image view
        return subBinding.root
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
            subBinding.imageView.setImage(getImageSource(data), previewSource)
        } else {
            setMediaViewVisible(false)
        }
    }

    @NonNull
    protected open fun getImageSource(@NonNull data: CacheDownloadLoader.Result): ImageSource {
        val cacheUri = data.cacheUri ?: error("cacheUri must not be null")
        val imageSource = ImageSource.uri(cacheUri)
        imageSource.tilingEnabled()
        return imageSource
    }

    @Nullable
    protected open fun getPreviewImageSource(@NonNull data: CacheDownloadLoader.Result): ImageSource? {
        return null
    }

    override fun releaseMediaResources() {
        subBinding.imageView.recycle()
    }

    protected open fun onMediaLoadStateChange(@State state: Int) {
        // Subclasses can override this
    }

    protected open fun setupImageView(imageView: SubsamplingScaleImageView) {
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