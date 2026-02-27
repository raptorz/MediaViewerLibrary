/*
 * Twidere - Twitter client for Android
 *
 *  Copyright (C) 2012-2015 Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mariotaku.mediaviewer.library

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.UiThread
import androidx.loader.content.AsyncTaskLoader
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.lang.ref.WeakReference

class CacheDownloadLoader(
    context: Context,
    private val downloader: MediaDownloader,
    private val fileCache: FileCache,
    listener: Listener,
    private val uri: Uri,
    private val extra: Any?,
    creator: ResultCreator?,
    private val ignoreCache: Boolean
) : AsyncTaskLoader<CacheDownloadLoader.Result>(context) {

    private val handler = Handler(Looper.getMainLooper())
    private val listenerRef = WeakReference(listener)
    private val creator = creator ?: DefaultResultCreator()

    private fun isValid(entry: File?): Boolean {
        return entry != null
    }

    override fun loadInBackground(): Result {
        val scheme = uri.scheme
        var result: DownloadResult? = null
        if (scheme == "http" || scheme == "https") {
            val uriString = uri.toString()
            if (uriString == null) return Result.nullInstance()
            var cacheFile: File?
            val nonce = System.currentTimeMillis()
            try {
                if (!ignoreCache) {
                    cacheFile = fileCache.get(uriString)
                    if (isValid(cacheFile)) {
                        return creator.create(fileCache.toUri(uriString))
                    }
                }
                handler.post(DownloadRequestedRunnable(this, listenerRef.get(), nonce))
                // from SD cache
                result = downloader.get(uriString, extra)
                val length = result.getLength()
                handler.post(DownloadStartRunnable(this, listenerRef.get(), nonce, length))

                val extraBytes = result.getExtra()
                fileCache.save(uriString, result.getStream(), extraBytes,
                    object : FileCache.CopyListener {
                        override fun onCopied(current: Int): Boolean {
                            handler.post(
                                ProgressUpdateRunnable(
                                    listenerRef.get(),
                                    current.toLong(),
                                    length,
                                    nonce
                                )
                            )
                            return !isAbandoned
                        }
                    })
                handler.post(DownloadFinishRunnable(this, listenerRef.get(), nonce))
                cacheFile = fileCache.get(uriString)
                if (isValid(cacheFile)) {
                    return creator.create(fileCache.toUri(uriString))
                } else {
                    fileCache.remove(uriString)
                    throw IOException("Invalid cache file")
                }
            } catch (e: Exception) {
                handler.post(DownloadErrorRunnable(this, listenerRef.get(), e, nonce))
                return Result.getInstance(e)
            } finally {
                Utils.closeSilently(result)
            }
        }
        return creator.create(uri)
    }

    override fun onReset() {
        super.onReset()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onStartLoading() {
        forceLoad()
    }

    interface Listener {
        @UiThread
        fun onDownloadError(t: Throwable, nonce: Long)

        @UiThread
        fun onDownloadFinished(nonce: Long)

        @UiThread
        fun onDownloadStart(total: Long, nonce: Long)

        @UiThread
        fun onDownloadRequested(nonce: Long)

        @UiThread
        fun onProgressUpdate(current: Long, total: Long, nonce: Long)
    }

    interface DownloadResult : Closeable {
        @Throws(IOException::class)
        fun getLength(): Long

        @Throws(IOException::class)
        fun getStream(): InputStream

        @Throws(IOException::class)
        fun getExtra(): ByteArray?
    }

    interface ResultCreator {
        fun create(uri: Uri): Result
    }

    class DefaultResultCreator : ResultCreator {
        override fun create(uri: Uri): Result {
            return Result.getInstance(uri)
        }
    }

    class Result(val cacheUri: Uri?, val exception: Exception?) {
        companion object {
            fun getInstance(uri: Uri): Result {
                return Result(uri, null)
            }

            fun getInstance(e: Exception): Result {
                return Result(null, e)
            }

            fun nullInstance(): Result {
                return Result(null, null)
            }
        }
    }

    private class DownloadErrorRunnable(
        private val loader: CacheDownloadLoader,
        private val listener: Listener?,
        private val t: Throwable,
        private val nonce: Long
    ) : Runnable {
        override fun run() {
            if (listener == null || loader.isAbandoned || loader.isReset) return
            listener.onDownloadError(t, nonce)
        }
    }

    private class DownloadFinishRunnable(
        private val loader: CacheDownloadLoader,
        private val listener: Listener?,
        private val nonce: Long
    ) : Runnable {
        override fun run() {
            if (listener == null || loader.isAbandoned || loader.isReset) return
            listener.onDownloadFinished(nonce)
        }
    }

    private class DownloadStartRunnable(
        private val loader: CacheDownloadLoader,
        private val listener: Listener?,
        private val nonce: Long,
        private val total: Long
    ) : Runnable {
        override fun run() {
            if (listener == null || loader.isAbandoned || loader.isReset) return
            listener.onDownloadStart(total, nonce)
        }
    }

    private class DownloadRequestedRunnable(
        private val loader: CacheDownloadLoader,
        private val listener: Listener?,
        private val nonce: Long
    ) : Runnable {
        override fun run() {
            if (listener == null || loader.isAbandoned || loader.isReset) return
            listener.onDownloadRequested(nonce)
        }
    }

    private class ProgressUpdateRunnable(
        private val listener: Listener?,
        private val current: Long,
        private val total: Long,
        private val nonce: Long
    ) : Runnable {
        override fun run() {
            listener?.onProgressUpdate(current, total, nonce)
        }
    }
}