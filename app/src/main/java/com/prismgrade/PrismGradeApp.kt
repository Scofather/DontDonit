package com.prismgrade

import android.app.Application
import com.prismgrade.di.ServiceLocator

class PrismGradeApp : Application() {

    lateinit var services: ServiceLocator
        private set

    override fun onCreate() {
        super.onCreate()
        services = ServiceLocator(this)
    }
}
