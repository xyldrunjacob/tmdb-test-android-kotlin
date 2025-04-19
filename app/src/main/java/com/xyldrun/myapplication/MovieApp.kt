package com.xyldrun.myapplication

import android.app.Application
import com.xyldrun.core.di.coreModule
import com.xyldrun.data.di.dataModule
import com.xyldrun.domain.di.domainModule
import com.xyldrun.presentation.di.presentationModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import timber.log.Timber

class MovieApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        startKoin {
            androidLogger()
            androidContext(this@MovieApp)
            modules(
                coreModule,
                dataModule,
                domainModule,
                presentationModule
            )
        }
    }
} 