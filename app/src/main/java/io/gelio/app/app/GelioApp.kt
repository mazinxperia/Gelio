package io.gelio.app.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import io.gelio.app.core.image.AppImageLoader

class GelioApp : Application(), ImageLoaderFactory {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
        appContainer.warmUp()
    }

    override fun newImageLoader(): ImageLoader = AppImageLoader.build(this)
}
