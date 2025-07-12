package com.reddoctor.threadui.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.reddoctor.threadui.data.GameConfig
import com.reddoctor.threadui.data.ShareConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object ShareUtils {
    
    private const val FILE_PROVIDER_AUTHORITY = "com.reddoctor.threadui.fileprovider"
    
    /**
     * 分享日志文件
     */
    fun shareLogFile(context: Context, logContent: String) {
        try {
            // 创建临时日志文件
            val logDir = File(context.cacheDir, "logs")
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val logFile = File(logDir, "threadui_error_log_$timestamp.txt")
            
            // 写入日志内容，包含设备和应用信息
            val deviceInfo = buildString {
                appendLine("ThreadUI 错误日志")
                appendLine("生成时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
                appendLine("应用版本: ${com.reddoctor.threadui.BuildConfig.VERSION_NAME} (${com.reddoctor.threadui.BuildConfig.VERSION_CODE})")
                appendLine("设备型号: ${android.os.Build.MODEL}")
                appendLine("系统版本: Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
                appendLine("===== 错误日志 =====")
                appendLine()
                append(logContent)
            }
            
            logFile.writeText(deviceInfo)
            
            // 使用FileProvider分享文件
            val fileUri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, logFile)
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "ThreadUI 错误日志")
                putExtra(Intent.EXTRA_TEXT, "ThreadUI应用错误日志，请发送给开发者进行问题诊断。")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooser = Intent.createChooser(shareIntent, "分享错误日志")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            
        } catch (e: Exception) {
            // 如果文件分享失败，尝试纯文本分享
            shareLogAsText(context, logContent)
        }
    }
    
    /**
     * 作为文本分享日志（备用方案）
     */
    private fun shareLogAsText(context: Context, logContent: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "ThreadUI 错误日志")
            putExtra(Intent.EXTRA_TEXT, logContent)
        }
        
        val chooser = Intent.createChooser(shareIntent, "分享错误日志")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
    
    fun shareGameConfig(context: Context, game: GameConfig) {
        val shareConfig = ShareConfig.fromSingleGame(game)
        val jsonContent = shareConfig.toJsonString()
        
        val fileName = "ThreadUI_${sanitizeFileName(game.name)}_${getCurrentTimestamp()}.json"
        shareJsonContent(context, jsonContent, fileName, "分享 ${game.name} 的配置")
    }
    
    fun shareGameConfigs(context: Context, games: List<GameConfig>) {
        val shareConfig = ShareConfig.fromGameConfigs(games)
        val jsonContent = shareConfig.toJsonString()
        
        val fileName = if (games.size == 1) {
            "ThreadUI_${sanitizeFileName(games.first().name)}_${getCurrentTimestamp()}.json"
        } else {
            "ThreadUI_配置批量导出_${games.size}个游戏_${getCurrentTimestamp()}.json"
        }
        
        val title = if (games.size == 1) {
            "分享 ${games.first().name} 的配置"
        } else {
            "分享 ${games.size} 个游戏的配置"
        }
        
        shareJsonContent(context, jsonContent, fileName, title)
    }
    
    private fun shareJsonContent(context: Context, content: String, fileName: String, title: String) {
        try {
            val tempFile = File(context.cacheDir, fileName)
            tempFile.writeText(content)
            
            val uri: Uri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, tempFile)
            
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "ThreadUI 游戏线程配置文件")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooserIntent = Intent.createChooser(shareIntent, title)
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun createShareFile(context: Context, games: List<GameConfig>): File? {
        return try {
            val shareConfig = ShareConfig.fromGameConfigs(games)
            val jsonContent = shareConfig.toJsonString()
            
            val fileName = if (games.size == 1) {
                "ThreadUI_${sanitizeFileName(games.first().name)}_${getCurrentTimestamp()}.json"
            } else {
                "ThreadUI_配置批量导出_${games.size}个游戏_${getCurrentTimestamp()}.json"
            }
            
            val file = File(context.getExternalFilesDir(null), "exports/$fileName")
            file.parentFile?.mkdirs()
            file.writeText(jsonContent)
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^\\w\\s-]"), "").replace(" ", "_")
    }
    
    private fun getCurrentTimestamp(): String {
        val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return formatter.format(Date())
    }
}