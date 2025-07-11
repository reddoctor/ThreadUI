package com.reddoctor.treadui.utils

import android.content.Context
import android.net.Uri
import com.reddoctor.treadui.data.AppListConfig
import com.reddoctor.treadui.data.GameConfig
import com.reddoctor.treadui.data.ShareConfig
import java.io.InputStream

object ImportUtils {
    
    data class ImportResult(
        val success: Boolean,
        val message: String,
        val importedGames: List<GameConfig> = emptyList(),
        val overwrittenGames: List<GameConfig> = emptyList(),
        val totalGames: Int = 0
    )
    
    fun importFromUri(context: Context, uri: Uri): ImportResult {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val content = inputStream?.bufferedReader()?.use { it.readText() }
            
            if (content.isNullOrEmpty()) {
                return ImportResult(false, "文件内容为空")
            }
            
            importFromJsonContent(content)
        } catch (e: Exception) {
            ImportResult(false, "读取文件失败: ${e.message}")
        }
    }
    
    fun importFromJsonContent(jsonContent: String): ImportResult {
        return try {
            val shareConfig = ShareConfig.fromJsonString(jsonContent)
            
            if (shareConfig.games.isEmpty()) {
                return ImportResult(false, "配置文件中没有找到游戏配置")
            }
            
            ImportResult(
                success = true,
                message = "成功解析 ${shareConfig.games.size} 个游戏配置",
                importedGames = shareConfig.games,
                totalGames = shareConfig.games.size
            )
        } catch (e: Exception) {
            ImportResult(false, "解析配置文件失败: ${e.message}")
        }
    }
    
    fun mergeWithExistingConfig(
        existingConfig: AppListConfig?,
        importedGames: List<GameConfig>
    ): Pair<AppListConfig, List<GameConfig>> {
        val currentGames = existingConfig?.games?.toMutableList() ?: mutableListOf()
        val overwrittenGames = mutableListOf<GameConfig>()
        
        importedGames.forEach { importedGame ->
            val existingIndex = currentGames.indexOfFirst { 
                it.packageName == importedGame.packageName 
            }
            
            if (existingIndex != -1) {
                // 如果包名已存在，直接覆盖
                overwrittenGames.add(currentGames[existingIndex])
                currentGames[existingIndex] = importedGame
            } else {
                // 如果包名不存在，添加新游戏
                currentGames.add(importedGame)
            }
        }
        
        return Pair(AppListConfig(currentGames), overwrittenGames)
    }
    
    fun validateShareConfig(jsonContent: String): Boolean {
        return try {
            val shareConfig = ShareConfig.fromJsonString(jsonContent)
            shareConfig.games.isNotEmpty() && 
            shareConfig.games.all { game ->
                game.packageName.isNotEmpty() && 
                game.name.isNotEmpty() && 
                game.threadConfigs.isNotEmpty()
            }
        } catch (e: Exception) {
            false
        }
    }
    
    fun getFileInfo(jsonContent: String): Map<String, Any> {
        return try {
            val shareConfig = ShareConfig.fromJsonString(jsonContent)
            mapOf(
                "version" to shareConfig.version,
                "exportTime" to shareConfig.exportTime,
                "gameCount" to shareConfig.games.size,
                "games" to shareConfig.games.map { "${it.name} (${it.packageName})" }
            )
        } catch (e: Exception) {
            mapOf("error" to e.message.orEmpty())
        }
    }
}