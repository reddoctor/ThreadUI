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