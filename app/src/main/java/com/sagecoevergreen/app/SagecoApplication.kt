package com.sagecoevergreen.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

/**
 * Application class — configures Coil with solid memory + disk caching
 * so property images load fast and reliably, even on slow networks.
 */
class SagecoApp : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader = try {
        ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024) // 50 MB
                    .build()
            }
            .crossfade(true)
            .respectCacheHeaders(false)
            .build()
    } catch (e: Exception) {
        // Never let cache config kill the app — fall back to defaults
        ImageLoader.Builder(this).crossfade(true).build()
    }
}
