package com.reddoctor.threadui.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.reddoctor.threadui.data.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppListUtils {
    
    /**
     * 获取已安装的应用列表
     * @param context 上下文
     * @param includeSystemApps 是否包含系统应用
     * @param gamesOnly 是否只显示疑似游戏的应用
     * @return 应用信息列表
     */
    suspend fun getInstalledApps(
        context: Context,
        includeSystemApps: Boolean = false,
        gamesOnly: Boolean = false
    ): List<AppInfo> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val installedApps = mutableListOf<AppInfo>()
        
        try {
            val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            
            for (packageInfo in packages) {
                try {
                    val isSystemApp = (packageInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    
                    // 如果不包含系统应用且当前是系统应用，跳过
                    if (!includeSystemApps && isSystemApp) {
                        continue
                    }
                    
                    val appName = packageInfo.loadLabel(packageManager).toString()
                    val packageName = packageInfo.packageName
                    val icon = try {
                        packageInfo.loadIcon(packageManager)
                    } catch (e: Exception) {
                        null
                    }
                    
                    // 获取版本信息
                    val versionName = try {
                        val packageInfo = packageManager.getPackageInfo(packageName, 0)
                        packageInfo.versionName ?: ""
                    } catch (e: Exception) {
                        ""
                    }
                    
                    val appInfo = AppInfo(
                        name = appName,
                        packageName = packageName,
                        icon = icon,
                        versionName = versionName,
                        isSystemApp = isSystemApp
                    )
                    
                    // 如果只显示游戏类应用，检查是否疑似游戏
                    if (gamesOnly && !AppInfo.isLikelyGame(packageName, appName)) {
                        continue
                    }
                    
                    installedApps.add(appInfo)
                    
                } catch (e: Exception) {
                    // 忽略获取单个应用信息失败的情况
                    continue
                }
            }
            
        } catch (e: Exception) {
            // 获取应用列表失败
            return@withContext emptyList()
        }
        
        // 排序：游戏类应用优先，然后按名称排序
        installedApps.sortedWith(compareBy<AppInfo> { appInfo ->
            if (AppInfo.isLikelyGame(appInfo.packageName, appInfo.name)) 0 else 1
        }.thenBy { it.displayName.lowercase() })
    }
    
    /**
     * 搜索应用
     * @param apps 应用列表
     * @param query 搜索关键词
     * @return 过滤后的应用列表
     */
    fun searchApps(apps: List<AppInfo>, query: String): List<AppInfo> {
        if (query.isBlank()) return apps
        
        val lowerQuery = query.lowercase()
        return apps.filter { app ->
            app.displayName.lowercase().contains(lowerQuery) ||
            app.packageName.lowercase().contains(lowerQuery)
        }
    }
    
    /**
     * 按类别过滤应用
     */
    fun filterByCategory(apps: List<AppInfo>, showGamesOnly: Boolean = false, showSystemApps: Boolean = false): List<AppInfo> {
        return apps.filter { app ->
            val passGameFilter = if (showGamesOnly) {
                AppInfo.isLikelyGame(app.packageName, app.name)
            } else true
            
            val passSystemFilter = if (showSystemApps) {
                true
            } else {
                !app.isSystemApp
            }
            
            passGameFilter && passSystemFilter
        }
    }
}