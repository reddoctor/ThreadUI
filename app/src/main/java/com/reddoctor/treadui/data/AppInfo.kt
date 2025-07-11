package com.reddoctor.treadui.data

import android.graphics.drawable.Drawable

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable?,
    val versionName: String = "",
    val isSystemApp: Boolean = false
) {
    // 用于搜索和排序的辅助属性
    val displayName: String
        get() = if (name.isNotBlank()) name else packageName
        
    companion object {
        // 游戏相关的包名关键词，用于优先显示
        private val GAME_KEYWORDS = listOf(
            "game", "play", "tmgp", "supercell", "mihoyo", "hypergryph",
            "netease", "tencent", "unity", "unreal", "pubg", "moba",
            "rpg", "action", "adventure", "puzzle", "racing", "sports",
            "simulation", "strategy", "arcade", "casual", "board"
        )
        
        fun isLikelyGame(packageName: String, appName: String): Boolean {
            val lowerPackage = packageName.lowercase()
            val lowerName = appName.lowercase()
            
            return GAME_KEYWORDS.any { keyword ->
                lowerPackage.contains(keyword) || lowerName.contains(keyword)
            }
        }
    }
}