package com.reddoctor.threadui.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*

object ErrorLogger {
    private const val TAG = "ThreadUI_ErrorLogger"
    private const val LOG_FILE_NAME = "threadui_error.log"
    private const val MAX_LOG_SIZE = 1024 * 1024 // 1MB
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    
    /**
     * 记录错误日志到本地文件
     */
    suspend fun logError(context: Context, tag: String, message: String, throwable: Throwable? = null) {
        withContext(Dispatchers.IO) {
            try {
                val logFile = getLogFile(context)
                val timestamp = dateFormat.format(Date())
                
                // 检查文件大小，如果超过限制则清理
                if (logFile.length() > MAX_LOG_SIZE) {
                    clearOldLogs(logFile)
                }
                
                // 写入错误日志
                FileWriter(logFile, true).use { writer ->
                    PrintWriter(writer).use { printWriter ->
                        printWriter.println("[$timestamp] ERROR/$tag: $message")
                        
                        // 如果有异常，记录异常堆栈
                        throwable?.let { e ->
                            printWriter.println("Exception: ${e.javaClass.simpleName}: ${e.message}")
                            e.printStackTrace(printWriter)
                        }
                        
                        printWriter.println("---")
                        printWriter.flush()
                    }
                }
                
                // 同时输出到Logcat
                if (throwable != null) {
                    Log.e(tag, message, throwable)
                } else {
                    Log.e(tag, message)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write error log", e)
            }
        }
    }
    
    /**
     * 记录一般错误信息
     */
    suspend fun logError(context: Context, tag: String, message: String) {
        logError(context, tag, message, null)
    }
    
    /**
     * 获取日志文件
     */
    private fun getLogFile(context: Context): File {
        val logDir = File(context.filesDir, "logs")
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
        return File(logDir, LOG_FILE_NAME)
    }
    
    /**
     * 清理旧日志，保留最近的一半
     */
    private fun clearOldLogs(logFile: File) {
        try {
            val lines = logFile.readLines()
            val keepLines = lines.takeLast(lines.size / 2)
            
            logFile.writeText(keepLines.joinToString("\n") + "\n")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear old logs", e)
            // 如果清理失败，直接删除文件重新开始
            logFile.delete()
        }
    }
    
    /**
     * 获取日志内容用于分享
     */
    suspend fun getLogContent(context: Context): String {
        return withContext(Dispatchers.IO) {
            try {
                val logFile = getLogFile(context)
                if (logFile.exists()) {
                    logFile.readText()
                } else {
                    "暂无错误日志"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read log content", e)
                "读取日志文件失败: ${e.message}"
            }
        }
    }
    
    /**
     * 检查日志文件是否存在且不为空
     */
    fun hasLogs(context: Context): Boolean {
        return try {
            val logFile = getLogFile(context)
            logFile.exists() && logFile.length() > 0
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取日志文件大小（人类可读格式）
     */
    fun getLogFileSize(context: Context): String {
        return try {
            val logFile = getLogFile(context)
            if (logFile.exists()) {
                val sizeBytes = logFile.length()
                when {
                    sizeBytes < 1024 -> "${sizeBytes}B"
                    sizeBytes < 1024 * 1024 -> "${sizeBytes / 1024}KB"
                    else -> "${sizeBytes / (1024 * 1024)}MB"
                }
            } else {
                "0B"
            }
        } catch (e: Exception) {
            "未知"
        }
    }
    
    /**
     * 清除所有日志
     */
    suspend fun clearAllLogs(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val logFile = getLogFile(context)
                if (logFile.exists()) {
                    logFile.delete()
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear logs", e)
                false
            }
        }
    }
}