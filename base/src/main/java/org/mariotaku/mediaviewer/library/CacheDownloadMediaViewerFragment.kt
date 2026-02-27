package org.mariotaku.mediaviewer.library

import android.net.Uri
import android.os.Bundle
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader

abstract class CacheDownloadMediaViewerFragment : MediaViewerFragment(),
    LoaderManager.LoaderCallbacks<CacheDownloadLoader.Result>,
    CacheDownloadLoader.Listener {

    private val EXTRA_IGNORE_CACHE = "ignore_cache"

    private var loaderInitialized = false
    private var downloadResult: CacheDownloadLoader.Result? = null

    fun hasDownloadedData(): Boolean {
        return downloadResult != null && downloadResult!!.cacheUri != null
    }

    @Nullable
    fun getDownloadResult(): CacheDownloadLoader.Result? {
        return downloadResult
    }

    override fun onDownloadError(t: Throwable, nonce: Long) {
        if (activity == null || isDetached) return
        hideProgress()
    }

    override fun onDownloadFinished(nonce: Long) {
        if (activity == null || isDetached) return
        hideProgress()
    }

    override fun onDownloadStart(total: Long, nonce: Long) {
        if (activity == null || isDetached) return
        showProgress(false, 0f)
    }

    override fun onDownloadRequested(nonce: Long) {
        if (activity == null || isDetached) return
        showProgress(true, 0f)
    }

    override fun onProgressUpdate(current: Long, total: Long, nonce: Long) {
        if (activity == null || isDetached) return
        showProgress(false, current.toFloat() / total)
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<CacheDownloadLoader.Result> {
        val downloadUri = getDownloadUri()
        if (downloadUri == null) throw NullPointerException()
        val ignoreCache = args?.getBoolean(EXTRA_IGNORE_CACHE) ?: false
        return CacheDownloadLoader(
            requireContext(),
            getDownloader(),
            getFileCache(),
            this,
            downloadUri,
            getDownloadExtra(),
            getResultCreator(),
            ignoreCache
        )
    }

    fun startLoading(ignoreCache: Boolean) {
        if (!isAbleToLoad()) return
        LoaderManager.getInstance(this).destroyLoader(0)
        val args = Bundle()
        args.putBoolean(EXTRA_IGNORE_CACHE, ignoreCache)
        if (!loaderInitialized) {
            LoaderManager.getInstance(this).initLoader(0, args, this)
            loaderInitialized = true
        } else {
            LoaderManager.getInstance(this).restartLoader(0, args, this)
        }
    }

    override fun onLoadFinished(
        loader: Loader<CacheDownloadLoader.Result>,
        @NonNull data: CacheDownloadLoader.Result
    ) {
        downloadResult = data
        hideProgress()
        displayMedia(data)
    }

    override fun onLoaderReset(loader: Loader<CacheDownloadLoader.Result>) {
        releaseMediaResources()
    }

    override fun isMediaLoading(): Boolean {
        return LoaderManager.getInstance(this).hasRunningLoaders()
    }

    override fun isMediaLoaded(): Boolean {
        return hasDownloadedData()
    }

    @Nullable
    protected abstract fun getDownloadUri(): Uri?

    @Nullable
    protected abstract fun getDownloadExtra(): Any?

    @Nullable
    protected open fun getResultCreator(): CacheDownloadLoader.ResultCreator? {
        return null
    }

    protected abstract fun displayMedia(data: CacheDownloadLoader.Result)

    protected abstract fun releaseMediaResources()

    protected abstract fun isAbleToLoad(): Boolean

    private fun getFileCache(): FileCache {
        return (activity as IMediaViewerActivity).fileCache
    }

    private fun getDownloader(): MediaDownloader {
        return (activity as IMediaViewerActivity).downloader
    }
}