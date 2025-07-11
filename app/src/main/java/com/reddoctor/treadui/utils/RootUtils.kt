package com.reddoctor.treadui.utils

import java.io.IOException

object RootUtils {
    fun isRootAvailable(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su -c 'id'")
            process.waitFor()
            process.exitValue() == 0
        } catch (e: Exception) {
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
            Result.failure(e)
        }
    }
    
    fun readFileAsRoot(filePath: String): Result<String> {
        return executeRootCommand("cat '$filePath'")
    }
    
    fun writeFileAsRoot(filePath: String, content: String): Result<Unit> {
        return try {
            val tempFile = "/data/local/tmp/applist_temp.conf"
            // 先写入临时文件，使用 cat 和 heredoc 来处理多行内容
            val escapedContent = content.replace("'", "'\"'\"'")
            val writeResult = executeRootCommand("cat > '$tempFile' << 'EOF'\n$escapedContent\nEOF")
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
}