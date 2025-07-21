package com.reddoctor.threadui.data

import android.icu.text.Transliterator

data class GameConfig(
    val name: String,
    val packageName: String,
    val threadConfigs: List<ThreadConfig>,
    val enabled: Boolean = true, // 配置是否启用，默认启用
    val firstLetter: String = getFirstLetter(name) // 自动计算首字母
)

data class ThreadConfig(
    val threadName: String,
    val cpuCores: String
)

data class AppListConfig(
    val games: List<GameConfig>
) {
    companion object {
        fun parseFromContent(content: String): AppListConfig {
            val games = mutableListOf<GameConfig>()
            val lines = content.split('\n')
            var currentGameName = ""
            
            for (line in lines) {
                val trimmedLine = line.trim()
                when {
                    trimmedLine.startsWith("#") -> {
                        // 新游戏开始 - 移除所有前导#字符
                        currentGameName = trimmedLine.dropWhile { it == '#' }.trim()
                        // 去除首尾引号（如果存在）
                        if (currentGameName.startsWith("\"") && currentGameName.endsWith("\"") && currentGameName.length > 1) {
                            currentGameName = currentGameName.substring(1, currentGameName.length - 1)
                        } else if (currentGameName.startsWith("'") && currentGameName.endsWith("'") && currentGameName.length > 1) {
                            currentGameName = currentGameName.substring(1, currentGameName.length - 1)
                        }
                    }
                    trimmedLine.isNotEmpty() && !trimmedLine.contains("=") && !trimmedLine.startsWith("#") -> {
                        // 不带#前缀的游戏名称行
                        currentGameName = trimmedLine.trim()
                        // 去除首尾引号（如果存在）
                        if (currentGameName.startsWith("\"") && currentGameName.endsWith("\"") && currentGameName.length > 1) {
                            currentGameName = currentGameName.substring(1, currentGameName.length - 1)
                        } else if (currentGameName.startsWith("'") && currentGameName.endsWith("'") && currentGameName.length > 1) {
                            currentGameName = currentGameName.substring(1, currentGameName.length - 1)
                        }
                    }
                    trimmedLine.isNotEmpty() && trimmedLine.contains("=") -> {
                        val parts = trimmedLine.split("=")
                        if (parts.size == 2) {
                            val leftPart = parts[0].trim()
                            val cpuCores = parts[1].trim()
                            
                            // 解析包名和线程名，检查是否有(off)后缀
                            val packageName: String
                            val threadName: String
                            val isEnabled: Boolean
                            
                            if (leftPart.contains("{") && leftPart.contains("}")) {
                                val basePackageName = leftPart.substring(0, leftPart.indexOf("{"))
                                threadName = leftPart.substring(leftPart.indexOf("{") + 1, leftPart.indexOf("}"))
                                
                                // 检查包名是否以(off)开头
                                if (basePackageName.startsWith("(off)")) {
                                    packageName = basePackageName.substring(5) // 移除前缀 "(off)"
                                    isEnabled = false
                                } else {
                                    packageName = basePackageName
                                    isEnabled = true
                                }
                            } else {
                                // 主包配置
                                threadName = "主进程"
                                
                                // 检查包名是否以(off)开头
                                if (leftPart.startsWith("(off)")) {
                                    packageName = leftPart.substring(5) // 移除前缀 "(off)"
                                    isEnabled = false
                                } else {
                                    packageName = leftPart
                                    isEnabled = true
                                }
                            }
                            
                            // 查找是否已存在相同包名的配置
                            val existingGameIndex = games.indexOfFirst { 
                                it.packageName == packageName 
                            }
                            
                            if (existingGameIndex != -1) {
                                // 更新现有配置，添加线程配置
                                val existingGame = games[existingGameIndex]
                                // 如果新的配置是禁用的，整个游戏配置都设为禁用
                                val newEnabled = existingGame.enabled && isEnabled
                                games[existingGameIndex] = existingGame.copy(
                                    threadConfigs = existingGame.threadConfigs + ThreadConfig(threadName, cpuCores),
                                    enabled = newEnabled
                                )
                            } else {
                                // 创建新配置
                                // 检查是否已经有同名游戏的其他包名配置
                                val sameGameCount = games.count { game ->
                                    // 提取原始游戏名进行比较
                                    val originalGameName = if (game.name.contains(" (") && game.name.endsWith(")")) {
                                        game.name.substring(0, game.name.lastIndexOf(" ("))
                                    } else {
                                        game.name
                                    }
                                    originalGameName == currentGameName
                                }
                                
                                val displayName = if (sameGameCount > 0) {
                                    // 如果已有同名游戏，添加包名标识符来区分
                                    val packageSuffix = packageName.split(".").lastOrNull() ?: packageName
                                    "$currentGameName ($packageSuffix)"
                                } else {
                                    currentGameName
                                }
                                
                                games.add(GameConfig(
                                    name = displayName,
                                    packageName = packageName,
                                    threadConfigs = listOf(ThreadConfig(threadName, cpuCores)),
                                    enabled = isEnabled
                                ))
                            }
                        }
                    }
                }
            }
            
            return AppListConfig(games)
        }
        
        fun toConfigString(config: AppListConfig): String {
            val builder = StringBuilder()
            // 按原始游戏名分组
            val gamesByOriginalName = config.games.groupBy { game ->
                // 提取原始游戏名（去除包名标识符）
                if (game.name.contains(" (") && game.name.endsWith(")")) {
                    game.name.substring(0, game.name.lastIndexOf(" ("))
                } else {
                    game.name
                }
            }
            
            for ((originalGameName, gameList) in gamesByOriginalName) {
                // 智能处理游戏名称引号 - 修复引号重复问题
                val formattedGameName = when {
                    // 如果游戏名包含空格、特殊字符（但不包括单纯的引号），则需要引号包围
                    originalGameName.contains(" ") || 
                    originalGameName.contains("#") || 
                    originalGameName.contains("=") ||
                    originalGameName.contains("{") ||
                    originalGameName.contains("}") -> {
                        // 清理名称，移除可能存在的外层引号
                        val cleanName = originalGameName.trim().let { name ->
                            when {
                                // 移除完整的外层双引号
                                name.startsWith("\"") && name.endsWith("\"") && name.length > 1 -> {
                                    name.substring(1, name.length - 1)
                                }
                                // 移除完整的外层单引号
                                name.startsWith("'") && name.endsWith("'") && name.length > 1 -> {
                                    name.substring(1, name.length - 1)
                                }
                                else -> name
                            }
                        }
                        "\"$cleanName\"" // 添加双引号包围清理后的名称
                    }
                    // 如果名称只包含引号但没有其他特殊字符，直接使用不加外层引号
                    else -> originalGameName
                }
                builder.append("#$formattedGameName\n")
                for (game in gameList) {
                    for (threadConfig in game.threadConfigs) {
                        val packageNameWithStatus = if (!game.enabled) {
                            "(off)${game.packageName}"
                        } else {
                            game.packageName
                        }
                        
                        if (threadConfig.threadName == "主进程") {
                            builder.append("${packageNameWithStatus}=${threadConfig.cpuCores}\n")
                        } else {
                            builder.append("${packageNameWithStatus}{${threadConfig.threadName}}=${threadConfig.cpuCores}\n")
                        }
                    }
                }
                builder.append("\n")
            }
            return builder.toString()
        }
    }
}

// 通用的中文转拼音首字母函数
fun getFirstLetter(name: String): String {
    if (name.isEmpty()) return "#"
    
    val firstChar = name.first()
    
    // 英文字母直接返回大写
    if (firstChar.isLetter() && firstChar.code < 128) {
        return firstChar.uppercase()
    }
    
    // 数字和特殊字符返回#
    if (!firstChar.isLetter()) {
        return "#"
    }
    
    // 中文字符使用ICU Transliterator转换为拼音
    return try {
        val transliterator = Transliterator.getInstance("Han-Latin")
        val pinyin = transliterator.transliterate(firstChar.toString())
        
        // 提取拼音的首字母，去除音调和特殊字符
        val cleanPinyin = pinyin.replace(Regex("[^a-zA-Z]"), "")
        
        if (cleanPinyin.isNotEmpty() && cleanPinyin.first().isLetter()) {
            cleanPinyin.first().uppercase()
        } else {
            "#"
        }
    } catch (e: Exception) {
        // 如果转换失败，返回#
        "#"
    }
}