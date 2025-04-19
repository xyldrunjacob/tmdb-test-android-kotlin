package com.xyldrun.core.di

import com.xyldrun.core.util.NetworkMonitor
import com.xyldrun.core.util.NetworkUtils
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val coreModule = module {
    single<NetworkMonitor> { NetworkUtils(androidContext()) }
} 