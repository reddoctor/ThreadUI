package com.reddoctor.threadui.utils

object RootUtils {
    fun isRootAvailable(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su -c 'id'")
            process.waitFor()
            process.exitValue() == 0
        } catch (e: Exception) {
            GlobalExceptionHandler.logException("RootUtils", "检查Root权限失败", e)
            false
        }
    }
    
    fun executeRootCommand(command: String): Result<String> {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val outputStream = process.outputStream
            val inputStream = process.inputStream
            val errorStream = process.errorStream
            
            // 发送命令
            outputStream.write("$command\n".toByteArray())
            outputStream.write("exit\n".toByteArray())
            outputStream.flush()
            outputStream.close()
            
            // 读取输出
            val output = inputStream.bufferedReader().readText()
            val error = errorStream.bufferedReader().readText()
            
            process.waitFor()
            
            if (process.exitValue() == 0) {
                Result.success(output)
            } else {
                Result.failure(Exception("Command failed: $error"))
            }
        } catch (e: Exception) {
            GlobalExceptionHandler.logException("RootUtils", "执行Root命令失败: $command", e)
            Result.failure(e)
        }
    }
    
    fun readFileAsRoot(filePath: String): Result<String> {
        return executeRootCommand("cat '$filePath'")
    }
    
    fun writeFileAsRoot(filePath: String, content: String): Result<Unit> {
        return try {
            // 根据文件扩展名生成临时文件名
            val fileExtension = when {
                filePath.endsWith(".conf") -> ".conf"
                filePath.endsWith(".prop") -> ".prop"
                else -> ".tmp"
            }
            val tempFile = "/data/local/tmp/applist_temp$fileExtension"
            
            // 使用base64编码来避免引号转义问题
            val encodedContent = android.util.Base64.encodeToString(
                content.toByteArray(Charsets.UTF_8), 
                android.util.Base64.NO_WRAP
            )
            
            // 写入base64编码的内容，然后解码
            val writeResult = executeRootCommand("echo '$encodedContent' | base64 -d > '$tempFile'")
            if (writeResult.isFailure) {
                return Result.failure(writeResult.exceptionOrNull()!!)
            }
            
            // 然后移动到目标位置
            val moveResult = executeRootCommand("cp '$tempFile' '$filePath'")
            if (moveResult.isFailure) {
                return Result.failure(moveResult.exceptionOrNull()!!)
            }
            
            // 清理临时文件
            executeRootCommand("rm '$tempFile'")
            
            Result.success(Unit)
        } catch (e: Exception) {
            GlobalExceptionHandler.logException("RootUtils", "写入Root文件失败: $filePath", e)
            Result.failure(e)
        }
    }
    
    fun createDirectoryAsRoot(dirPath: String): Result<Unit> {
        return executeRootCommand("mkdir -p '$dirPath'").map { }
    }
    
    fun fileExistsAsRoot(filePath: String): Boolean {
        val result = executeRootCommand("test -f '$filePath' && echo 'exists' || echo 'not_exists'")
        return result.getOrNull()?.trim() == "exists"
    }
    
    fun directoryExistsAsRoot(dirPath: String): Boolean {
        val result = executeRootCommand("test -d '$dirPath' && echo 'exists' || echo 'not_exists'")
        return result.getOrNull()?.trim() == "exists"
    }
    
    @Deprecated("Use checkModuleByConfigPath() instead for dynamic path support")
    fun checkAppOptModuleExists(): Boolean {
        return directoryExistsAsRoot("/data/adb/modules/AppOpt")
    }
    
    fun checkConfigFileExists(configPath: String): Boolean {
        return fileExistsAsRoot(configPath)
    }
    
    fun checkModuleByConfigPath(configPath: String): Boolean {
        // 首先检查配置文件是否存在
        if (fileExistsAsRoot(configPath)) {
            return true
        }
        
        // 如果配置文件不存在，检查对应的模块目录是否存在
        if (configPath.contains("/data/adb/modules/")) {
            val modulePath = configPath.substringBeforeLast("/")
            return directoryExistsAsRoot(modulePath)
        }
        
        return false
    }
}