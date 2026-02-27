package org.mariotaku.mediaviewer.library

import androidx.annotation.WorkerThread
import java.io.IOException

@WorkerThread
interface MediaDownloader {
    @WorkerThread
    @Throws(IOException::class)
    fun get(url: String, extra: Any?): CacheDownloadLoader.DownloadResult
}