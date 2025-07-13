package com.reddoctor.threadui.utils

import android.content.Context
import android.content.SharedPreferences

object ConfigManager {
    private const val PREFS_NAME = "threadui_config"
    private const val KEY_CONFIG_PATH = "config_file_path"
    private const val DEFAULT_CONFIG_PATH = "/data/adb/modules/AppOpt/applist.conf"
    
    fun getConfigPath(context: Context): String {
        val prefs = getPreferences(context)
        return prefs.getString(KEY_CONFIG_PATH, DEFAULT_CONFIG_PATH) ?: DEFAULT_CONFIG_PATH
    }
    
    fun setConfigPath(context: Context, path: String) {
        val prefs = getPreferences(context)
        prefs.edit().putString(KEY_CONFIG_PATH, path).apply()
    }
    
    fun resetToDefault(context: Context) {
        setConfigPath(context, DEFAULT_CONFIG_PATH)
    }
    
    fun getDefaultPath(): String {
        return DEFAULT_CONFIG_PATH
    }
    
    fun isUsingDefaultPath(context: Context): Boolean {
        return getConfigPath(context) == DEFAULT_CONFIG_PATH
    }
    
    fun getModuleName(path: String): String {
        return try {
            if (path.contains("/data/adb/modules/")) {
                val modulePart = path.substringAfter("/data/adb/modules/")
                val moduleName = modulePart.substringBefore("/")
                moduleName
            } else {
                "自定义模块"
            }
        } catch (e: Exception) {
            GlobalExceptionHandler.logException("ConfigManager", "获取模块名称失败: $path", e)
            "未知模块"
        }
    }
    
    fun validateConfigPath(path: String): Boolean {
        return path.isNotBlank() && 
               path.startsWith("/") && 
               (path.endsWith(".conf") || path.endsWith(".prop")) &&
               !path.contains("..") &&
               path.length < 256 &&
               path.split("/").all { it.isNotEmpty() || it == path.split("/").first() }
    }
    
    fun getConfigFileType(path: String): String {
        return when {
            path.endsWith(".conf") -> "conf"
            path.endsWith(".prop") -> "prop"
            else -> "unknown"
        }
    }
    
    fun isConfigFile(path: String): Boolean {
        return path.endsWith(".conf") || path.endsWith(".prop")
    }
    
    fun getSupportedExtensions(): List<String> {
        return listOf(".conf", ".prop")
    }
    
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}