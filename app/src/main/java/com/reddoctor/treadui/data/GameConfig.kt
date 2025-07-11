package com.reddoctor.treadui.data

data class GameConfig(
    val name: String,
    val packageName: String,
    val threadConfigs: List<ThreadConfig>
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
                        // 新游戏开始
                        currentGameName = trimmedLine.substring(1).trim()
                    }
                    trimmedLine.isNotEmpty() && trimmedLine.contains("=") -> {
                        val parts = trimmedLine.split("=")
                        if (parts.size == 2) {
                            val leftPart = parts[0].trim()
                            val cpuCores = parts[1].trim()
                            
                            // 解析包名和线程名
                            val packageName: String
                            val threadName: String
                            
                            if (leftPart.contains("{") && leftPart.contains("}")) {
                                packageName = leftPart.substring(0, leftPart.indexOf("{"))
                                threadName = leftPart.substring(leftPart.indexOf("{") + 1, leftPart.indexOf("}"))
                            } else {
                                // 主包配置
                                packageName = leftPart
                                threadName = "主进程"
                            }
                            
                            // 查找是否已存在相同包名的配置
                            val existingGameIndex = games.indexOfFirst { 
                                it.packageName == packageName 
                            }
                            
                            if (existingGameIndex != -1) {
                                // 更新现有配置，添加线程配置
                                val existingGame = games[existingGameIndex]
                                games[existingGameIndex] = existingGame.copy(
                                    threadConfigs = existingGame.threadConfigs + ThreadConfig(threadName, cpuCores)
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
                                    threadConfigs = listOf(ThreadConfig(threadName, cpuCores))
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
                builder.append("#$originalGameName\n")
                for (game in gameList) {
                    for (threadConfig in game.threadConfigs) {
                        if (threadConfig.threadName == "主进程") {
                            builder.append("${game.packageName}=${threadConfig.cpuCores}\n")
                        } else {
                            builder.append("${game.packageName}{${threadConfig.threadName}}=${threadConfig.cpuCores}\n")
                        }
                    }
                }
                builder.append("\n")
            }
            return builder.toString()
        }
    }
}