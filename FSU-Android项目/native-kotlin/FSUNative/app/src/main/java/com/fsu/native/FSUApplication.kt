package com.fsu.native

import android.app.Application
import android.content.Context
import com.fsu.native.data.FSUDatabase

/**
 * FSU应用入口，初始化全局依赖注入容器
 */
class FSUApplication : Application() {

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = AppContainer(this)
    }

    companion object {
        @Volatile
        private lateinit var instance: FSUApplication

        fun getInstance(): FSUApplication = instance

        fun getContext(): Context = instance.applicationContext
    }
}
