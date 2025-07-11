package com.reddoctor.treadui.data

import org.json.JSONArray
import org.json.JSONObject

data class ShareConfig(
    val version: String = "1.0",
    val exportTime: Long = System.currentTimeMillis(),
    val games: List<GameConfig>
) {
    companion object {
        fun fromGameConfigs(games: List<GameConfig>): ShareConfig {
            return ShareConfig(games = games)
        }
        
        fun fromSingleGame(game: GameConfig): ShareConfig {
            return ShareConfig(games = listOf(game))
        }
        
        fun fromJsonString(jsonString: String): ShareConfig {
            val json = JSONObject(jsonString)
            val version = json.optString("version", "1.0")
            val exportTime = json.optLong("exportTime", System.currentTimeMillis())
            val gamesArray = json.getJSONArray("games")
            
            val games = mutableListOf<GameConfig>()
            for (i in 0 until gamesArray.length()) {
                val gameJson = gamesArray.getJSONObject(i)
                val name = gameJson.getString("name")
                val packageName = gameJson.getString("packageName")
                val threadConfigsArray = gameJson.getJSONArray("threadConfigs")
                
                val threadConfigs = mutableListOf<ThreadConfig>()
                for (j in 0 until threadConfigsArray.length()) {
                    val threadJson = threadConfigsArray.getJSONObject(j)
                    threadConfigs.add(
                        ThreadConfig(
                            threadName = threadJson.getString("threadName"),
                            cpuCores = threadJson.getString("cpuCores")
                        )
                    )
                }
                
                games.add(GameConfig(name, packageName, threadConfigs))
            }
            
            return ShareConfig(version, exportTime, games)
        }
    }
    
    fun toJsonString(): String {
        val json = JSONObject()
        json.put("version", version)
        json.put("exportTime", exportTime)
        
        val gamesArray = JSONArray()
        games.forEach { game ->
            val gameJson = JSONObject()
            gameJson.put("name", game.name)
            gameJson.put("packageName", game.packageName)
            
            val threadConfigsArray = JSONArray()
            game.threadConfigs.forEach { threadConfig ->
                val threadJson = JSONObject()
                threadJson.put("threadName", threadConfig.threadName)
                threadJson.put("cpuCores", threadConfig.cpuCores)
                threadConfigsArray.put(threadJson)
            }
            gameJson.put("threadConfigs", threadConfigsArray)
            gamesArray.put(gameJson)
        }
        json.put("games", gamesArray)
        
        return json.toString(2)
    }
}