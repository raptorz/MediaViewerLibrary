package org.mariotaku.mediaviewer.library

import java.io.Closeable
import java.io.IOException

object Utils {
    fun closeSilently(c: Closeable?) {
        if (c == null) return
        try {
            c.close()
        } catch (e: IOException) {
            // Ignore
        }
    }
}