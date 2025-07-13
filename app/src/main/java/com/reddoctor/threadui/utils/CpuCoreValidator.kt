package com.reddoctor.threadui.utils

import java.io.File

object CpuCoreValidator {
    
    private var cachedCoreCount: Int? = null
    private var cachedCpuInfo: CpuInfo? = null
    
    /**
     * CPU信息数据类
     */
    data class CpuInfo(
        val totalCores: Int,
        val architecture: String,
        val bigCores: Int = 0,
        val midCores: Int = 0,
        val littleCores: Int = 0,
        val superCores: Int = 0,  // 新增超大核心
        val cpuModel: String = "未知",
        val maxFreq: List<String> = emptyList(),
        val minFreq: List<String> = emptyList()
    )
    
    /**
     * 获取详细的CPU信息
     */
    fun getCpuInfo(): CpuInfo {
        if (cachedCpuInfo != null) {
            return cachedCpuInfo!!
        }
        
        val totalCores = getCpuCoreCount()
        val architecture = getCpuArchitecture()
        val coreInfo = analyzeCoreTypes()
        val cpuModel = getCpuModel()
        val frequencies = getCpuFrequencies()
        
        val cpuInfo = CpuInfo(
            totalCores = totalCores,
            architecture = architecture,
            superCores = coreInfo.first,
            bigCores = coreInfo.second,
            midCores = coreInfo.third,
            littleCores = coreInfo.fourth,
            cpuModel = cpuModel,
            maxFreq = frequencies.first,
            minFreq = frequencies.second
        )
        
        cachedCpuInfo = cpuInfo
        return cpuInfo
    }
    
    /**
     * 获取CPU架构
     */
    private fun getCpuArchitecture(): String {
        return try {
            val abi = System.getProperty("os.arch") ?: "unknown"
            when {
                abi.contains("aarch64") || abi.contains("arm64") -> "ARM64"
                abi.contains("arm") -> "ARM32"
                abi.contains("x86_64") -> "x86_64"
                abi.contains("x86") -> "x86"
                else -> abi
            }
        } catch (e: Exception) {
            "未知"
        }
    }
    
    /**
     * 获取CPU型号
     */
    private fun getCpuModel(): String {
        return try {
            val cpuInfo = File("/proc/cpuinfo")
            if (cpuInfo.exists()) {
                val lines = cpuInfo.readLines()
                
                // 尝试多种字段来获取CPU型号
                val modelPatterns = listOf(
                    "model name",
                    "Hardware",
                    "Processor",
                    "cpu model",
                    "machine",
                    "system type"
                )
                
                for (pattern in modelPatterns) {
                    val modelLine = lines.find { line ->
                        line.trim().lowercase().startsWith(pattern.lowercase())
                    }
                    if (modelLine != null) {
                        val model = modelLine.substringAfter(":").trim()
                        if (model.isNotEmpty() && model != "unknown" && model != "0") {
                            return model
                        }
                    }
                }
                
                // 如果没找到，尝试从其他字段获取
                val fallbackLine = lines.find { line ->
                    line.contains("qualcomm", ignoreCase = true) ||
                    line.contains("snapdragon", ignoreCase = true) ||
                    line.contains("mediatek", ignoreCase = true) ||
                    line.contains("exynos", ignoreCase = true) ||
                    line.contains("kirin", ignoreCase = true)
                }
                if (fallbackLine != null) {
                    return fallbackLine.substringAfter(":").trim()
                }
            }
            
            // 尝试从系统属性获取
            tryGetSystemProperty() ?: "未知型号"
        } catch (e: Exception) {
            GlobalExceptionHandler.logException("CpuCoreValidator", "获取CPU型号失败", e)
            "未知型号"
        }
    }
    
    /**
     * 尝试从系统属性获取CPU信息
     */
    private fun tryGetSystemProperty(): String? {
        return try {
            val properties = listOf(
                "ro.board.platform",
                "ro.product.board",
                "ro.chipname",
                "ro.hardware",
                "ro.product.cpu.abi",
                "ro.soc.manufacturer",
                "ro.soc.model"
            )
            
            for (prop in properties) {
                try {
                    val process = Runtime.getRuntime().exec("getprop $prop")
                    val result = process.inputStream.bufferedReader().readText().trim()
                    if (result.isNotEmpty() && result != "unknown" && !result.startsWith("arm")) {
                        return result
                    }
                } catch (e: Exception) {
                    // 忽略单个属性获取失败
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 分析大中小核心数量
     * 返回 Quadruple(超大核心数, 大核心数, 中核心数, 小核心数)
     */
    private fun analyzeCoreTypes(): Quadruple<Int, Int, Int, Int> {
        return try {
            val frequencies = getCpuFrequencies()
            val maxFreqs = frequencies.first
            
            if (maxFreqs.isEmpty()) {
                return Quadruple(0, 0, 0, getCpuCoreCount())
            }
            
            // 将频率转换为Long并分组统计
            val freqGroups = maxFreqs.mapNotNull { it.toLongOrNull() }
                .groupingBy { it }
                .eachCount()
                .toList()
                .sortedByDescending { it.first }  // 按频率从高到低排序
            
            when (freqGroups.size) {
                1 -> {
                    // 同构核心
                    Quadruple(0, 0, 0, getCpuCoreCount())
                }
                2 -> {
                    // 大小核心架构
                    val highFreqCores = freqGroups[0].second
                    val lowFreqCores = freqGroups[1].second
                    Quadruple(0, highFreqCores, 0, lowFreqCores)
                }
                3 -> {
                    // 三种频率：可能是超大+大+小 或 大+中+小
                    val highest = freqGroups[0]
                    val middle = freqGroups[1]
                    val lowest = freqGroups[2]
                    
                    // 如果最高频率核心只有1个，很可能是超大核心
                    if (highest.second == 1) {
                        Quadruple(1, middle.second, 0, lowest.second)
                    } else {
                        // 否则是大+中+小架构
                        Quadruple(0, highest.second, middle.second, lowest.second)
                    }
                }
                4 -> {
                    // 四种频率：超大+大+中+小 (8 Gen 3类型)
                    val superCore = freqGroups[0].second
                    val bigCores = freqGroups[1].second
                    val midCores = freqGroups[2].second
                    val littleCores = freqGroups[3].second
                    Quadruple(superCore, bigCores, midCores, littleCores)
                }
                else -> {
                    // 复杂架构，简化处理
                    val sorted = freqGroups.sortedByDescending { it.first }
                    when {
                        sorted.size >= 4 -> {
                            // 尝试识别为四核心类型
                            Quadruple(
                                sorted[0].second,
                                sorted[1].second,
                                sorted[2].second,
                                sorted.drop(3).sumOf { it.second }
                            )
                        }
                        sorted.size >= 2 -> {
                            // 简化为大小核心
                            val bigCores = sorted[0].second
                            val littleCores = sorted.drop(1).sumOf { it.second }
                            Quadruple(0, bigCores, 0, littleCores)
                        }
                        else -> {
                            Quadruple(0, 0, 0, getCpuCoreCount())
                        }
                    }
                }
            }
        } catch (e: Exception) {
            GlobalExceptionHandler.logException("CpuCoreValidator", "分析核心类型失败", e)
            Quadruple(0, 0, 0, getCpuCoreCount())
        }
    }
    
    /**
     * 四元组数据类，用于返回四个核心类型数量
     */
    private data class Quadruple<out A, out B, out C, out D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )
    
    /**
     * 获取CPU频率信息
     * 返回 Pair(最大频率列表, 最小频率列表)
     */
    private fun getCpuFrequencies(): Pair<List<String>, List<String>> {
        return try {
            val maxFreqs = mutableListOf<String>()
            val minFreqs = mutableListOf<String>()
            
            val totalCores = getCpuCoreCount()
            
            for (i in 0 until totalCores) {
                // 读取最大频率
                val maxFreqFile = File("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq")
                if (maxFreqFile.exists()) {
                    val maxFreq = maxFreqFile.readText().trim()
                    maxFreqs.add(maxFreq)
                } else {
                    maxFreqs.add("0")
                }
                
                // 读取最小频率
                val minFreqFile = File("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_min_freq")
                if (minFreqFile.exists()) {
                    val minFreq = minFreqFile.readText().trim()
                    minFreqs.add(minFreq)
                } else {
                    minFreqs.add("0")
                }
            }
            
            Pair(maxFreqs, minFreqs)
        } catch (e: Exception) {
            Pair(emptyList(), emptyList())
        }
    }
    
    /**
     * 获取当前设备的CPU核心数量
     */
    fun getCpuCoreCount(): Int {
        if (cachedCoreCount != null) {
            return cachedCoreCount!!
        }
        
        return try {
            // 方法1: 通过 /proc/cpuinfo 获取
            val cpuInfo = File("/proc/cpuinfo")
            if (cpuInfo.exists()) {
                val processorCount = cpuInfo.readLines()
                    .count { it.startsWith("processor") }
                if (processorCount > 0) {
                    cachedCoreCount = processorCount
                    return processorCount
                }
            }
            
            // 方法2: 通过 Runtime.availableProcessors()
            val runtimeCores = Runtime.getRuntime().availableProcessors()
            if (runtimeCores > 0) {
                cachedCoreCount = runtimeCores
                return runtimeCores
            }
            
            // 方法3: 通过 /sys/devices/system/cpu/ 目录
            val cpuDir = File("/sys/devices/system/cpu/")
            if (cpuDir.exists()) {
                val cpuDirs = cpuDir.listFiles { file ->
                    file.isDirectory && file.name.matches(Regex("cpu\\d+"))
                }
                if (cpuDirs != null && cpuDirs.isNotEmpty()) {
                    cachedCoreCount = cpuDirs.size
                    return cpuDirs.size
                }
            }
            
            // 默认值
            8
        } catch (e: Exception) {
            GlobalExceptionHandler.logException("CpuCoreValidator", "获取CPU核心数失败", e)
            // 默认返回8核心
            8
        }.also {
            cachedCoreCount = it
        }
    }
    
    /**
     * 校验CPU核心范围字符串
     * @param cpuCores CPU核心范围字符串，如 "0-3", "4", "0,2,4", "0-3,6-7"
     * @return ValidationResult 校验结果
     */
    fun validateCpuCores(cpuCores: String): ValidationResult {
        if (cpuCores.isBlank()) {
            return ValidationResult(false, "CPU核心不能为空")
        }
        
        val maxCore = getCpuCoreCount() - 1 // 核心编号从0开始
        
        try {
            // 解析CPU核心字符串
            val coreNumbers = parseCpuCores(cpuCores)
            
            if (coreNumbers.isEmpty()) {
                return ValidationResult(false, "CPU核心格式无效")
            }
            
            // 检查是否超出范围
            val invalidCores = coreNumbers.filter { it > maxCore || it < 0 }
            if (invalidCores.isNotEmpty()) {
                return ValidationResult(
                    false, 
                    "CPU核心超出范围：${invalidCores.joinToString(",")}。" +
                    "当前设备有${getCpuCoreCount()}个核心（0-$maxCore）"
                )
            }
            
            return ValidationResult(true, "CPU核心范围有效")
            
        } catch (e: Exception) {
            return ValidationResult(false, "CPU核心格式错误：${e.message}")
        }
    }
    
    /**
     * 解析CPU核心字符串为核心编号列表
     * 支持格式: "0-7 | 4 | 0,2,4 | 0-3,6-7"
     */
    private fun parseCpuCores(cpuCores: String): List<Int> {
        val cores = mutableSetOf<Int>()
        
        // 按逗号分割
        val parts = cpuCores.split(",").map { it.trim() }
        
        for (part in parts) {
            if (part.contains("-")) {
                // 处理范围，如 "0-3"
                val rangeParts = part.split("-")
                if (rangeParts.size == 2) {
                    val start = rangeParts[0].toInt()
                    val end = rangeParts[1].toInt()
                    if (start <= end) {
                        cores.addAll(start..end)
                    } else {
                        throw IllegalArgumentException("范围起始值不能大于结束值: $part")
                    }
                } else {
                    throw IllegalArgumentException("范围格式错误: $part")
                }
            } else {
                // 处理单个核心，如 "4"
                cores.add(part.toInt())
            }
        }
        
        return cores.toList().sorted()
    }
    
    /**
     * 获取CPU核心范围的建议提示
     */
    fun getCpuCoreHint(): String {
        val maxCore = getCpuCoreCount() - 1
        return "例: 0-${maxCore} 或 ${maxCore} (当前设备: ${getCpuCoreCount()}核心)"
    }
    
    /**
     * 校验结果数据类
     */
    data class ValidationResult(
        val isValid: Boolean,
        val message: String
    )
}