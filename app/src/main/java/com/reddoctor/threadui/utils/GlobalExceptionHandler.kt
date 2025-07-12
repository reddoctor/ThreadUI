package com.reddoctor.threadui.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * 全局异常处理器
 */
object GlobalExceptionHandler {
    private const val TAG = "GlobalExceptionHandler"
    private var appContext: Context? = null
    
    /**
     * 初始化全局异常处理器
     */
    fun init(context: Context) {
        appContext = context.applicationContext
        
        // 设置未捕获异常处理器
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            handleUncaughtException(thread, exception)
        }
    }
    
    /**
     * 创建协程异常处理器
     */
    fun createCoroutineExceptionHandler(tag: String): CoroutineExceptionHandler {
        return CoroutineExceptionHandler { _, exception ->
            handleCoroutineException(tag, exception)
        }
    }
    
    /**
     * 处理未捕获的异常（主要是主线程异常）
     */
    private fun handleUncaughtException(thread: Thread, exception: Throwable) {
        try {
            Log.e(TAG, "Uncaught exception in thread: ${thread.name}", exception)
            
            appContext?.let { context ->
                GlobalScope.launch {
                    ErrorLogger.logError(
                        context = context,
                        tag = "UncaughtException",
                        message = "线程 ${thread.name} 发生未捕获异常: ${exception.message}",
                        throwable = exception
                    )
                }
            }
            
            // 调用默认处理器，让应用正常崩溃
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            defaultHandler?.uncaughtException(thread, exception)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in uncaught exception handler", e)
        }
    }
    
    /**
     * 处理协程异常
     */
    private fun handleCoroutineException(tag: String, exception: Throwable) {
        try {
            Log.e(TAG, "Coroutine exception in $tag", exception)
            
            appContext?.let { context ->
                GlobalScope.launch {
                    ErrorLogger.logError(
                        context = context,
                        tag = "CoroutineException_$tag",
                        message = "协程异常: ${exception.message}",
                        throwable = exception
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in coroutine exception handler", e)
        }
    }
    
    /**
     * 手动记录异常（用于try-catch块中）
     */
    fun logException(tag: String, message: String, throwable: Throwable? = null) {
        try {
            if (throwable != null) {
                Log.e(TAG, "[$tag] $message", throwable)
            } else {
                Log.e(TAG, "[$tag] $message")
            }
            
            appContext?.let { context ->
                GlobalScope.launch {
                    ErrorLogger.logError(
                        context = context,
                        tag = tag,
                        message = message,
                        throwable = throwable
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in manual exception logging", e)
        }
    }
    
    /**
     * 记录警告信息
     */
    fun logWarning(tag: String, message: String) {
        try {
            Log.w(TAG, "[$tag] $message")
            
            appContext?.let { context ->
                GlobalScope.launch {
                    ErrorLogger.logError(
                        context = context,
                        tag = "Warning_$tag",
                        message = "警告: $message"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in warning logging", e)
        }
    }
}