package com.valencia.geobus

import android.app.Application
import com.valencia.geobus.data.local.AppDatabase

class GeoBusApplication : Application() {

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getInstance(this)
    }

    companion object {
        lateinit var instance: GeoBusApplication
            private set
    }
}
