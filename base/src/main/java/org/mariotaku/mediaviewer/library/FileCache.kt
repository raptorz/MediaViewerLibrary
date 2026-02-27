package org.mariotaku.mediaviewer.library

import android.net.Uri
import androidx.annotation.WorkerThread
import java.io.File
import java.io.IOException
import java.io.InputStream

@WorkerThread
interface FileCache {
    @WorkerThread
    @Throws(IOException::class)
    fun get(key: String): File?

    @WorkerThread
    @Throws(IOException::class)
    fun getExtra(key: String): ByteArray?

    @WorkerThread
    @Throws(IOException::class)
    fun remove(key: String)

    @WorkerThread
    @Throws(IOException::class)
    fun save(
        key: String,
        stream: InputStream,
        extra: ByteArray?,
        listener: CopyListener?
    )

    fun toUri(key: String): Uri

    fun fromUri(uri: Uri): String

    interface CopyListener {
        @WorkerThread
        fun onCopied(current: Int): Boolean
    }
}