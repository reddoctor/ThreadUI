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
        val totalGames: Int = 0,
        val hasFormatScript: Boolean = false,
        val formatScriptWarning: String? = null
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
            // 检测格机脚本
            val formatScriptResult = detectFormatScript(jsonContent)
            
            val shareConfig = ShareConfig.fromJsonString(jsonContent)
            
            if (shareConfig.games.isEmpty()) {
                return ImportResult(false, "配置文件中没有找到游戏配置")
            }
            
            ImportResult(
                success = true,
                message = "成功解析 ${shareConfig.games.size} 个游戏配置",
                importedGames = shareConfig.games,
                totalGames = shareConfig.games.size,
                hasFormatScript = formatScriptResult.first,
                formatScriptWarning = formatScriptResult.second
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
    
    /**
     * 检测格机脚本的危险内容
     * @param content 文件内容
     * @return Pair<是否包含危险脚本, 警告信息>
     */
    private fun detectFormatScript(content: String): Pair<Boolean, String?> {
        val dangerousPatterns = listOf(
            // 格式化命令
            "rm -rf" to "删除文件命令",
            "format" to "格式化命令",
            "mkfs" to "创建文件系统命令",
            "fdisk" to "磁盘分区命令",
            "dd if=" to "磁盘操作命令",
            "fsck" to "文件系统检查命令",
            
            // 系统破坏命令
            "chmod 000" to "权限破坏命令",
            "chown root" to "所有者修改命令",
            "mount" to "挂载命令",
            "umount" to "卸载命令",
            "reboot" to "重启命令",
            "shutdown" to "关机命令",
            
            // 恶意脚本标识
            "#!/bin/sh" to "Shell脚本",
            "#!/bin/bash" to "Bash脚本",
            "busybox" to "BusyBox命令",
            "su -c" to "Root权限执行命令",
            "exec" to "执行命令",
            "eval" to "动态执行代码",
            
            // 危险目录操作
            "/system/" to "系统目录操作",
            "/data/" to "数据目录操作",
            "/boot/" to "启动目录操作",
            "/recovery/" to "恢复目录操作",
            "/cache/" to "缓存目录操作",
            "/dev/block/" to "块设备操作",
            
            // 恶意网络请求
            "wget" to "网络下载命令",
            "curl" to "网络请求命令",
            "nc " to "网络连接命令",
            "netcat" to "网络工具",
            
            // 进程操作
            "kill -9" to "强制终止进程",
            "killall" to "批量终止进程",
            "ps aux" to "进程查看命令",
            "nohup" to "后台执行命令"
        )
        
        val lowerContent = content.lowercase()
        val foundPatterns = mutableListOf<String>()
        
        for ((pattern, description) in dangerousPatterns) {
            if (lowerContent.contains(pattern.lowercase())) {
                foundPatterns.add(description)
            }
        }
        
        return if (foundPatterns.isNotEmpty()) {
            val warning = "⚠️ 检测到潜在危险内容:\n${foundPatterns.joinToString("\n• ", "• ")}\n\n" +
                    "此配置文件可能包含格机脚本或其他危险代码，导入前请仔细检查文件来源和内容。\n" +
                    "建议只导入来自可信来源的配置文件。"
            true to warning
        } else {
            false to null
        }
    }
}