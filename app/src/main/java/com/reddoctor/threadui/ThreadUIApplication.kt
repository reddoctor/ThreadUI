package com.reddoctor.threadui

import android.app.Application
import com.reddoctor.threadui.utils.GlobalExceptionHandler

class ThreadUIApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // 初始化全局异常处理器
        GlobalExceptionHandler.init(this)
    }
}