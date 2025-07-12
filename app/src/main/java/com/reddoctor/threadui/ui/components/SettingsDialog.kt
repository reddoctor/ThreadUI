package com.reddoctor.threadui.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.reddoctor.threadui.utils.ConfigManager

@Composable
fun SettingsDialog(
    currentPath: String,
    onDismiss: () -> Unit,
    onPathChanged: (String) -> Unit
) {
    val context = LocalContext.current
    var tempPath by remember { mutableStateOf(currentPath) }
    var isValidPath by remember { mutableStateOf(true) }
    
    fun validatePath(path: String): Boolean {
        return path.isNotBlank() && 
               path.startsWith("/") && 
               path.endsWith(".conf") &&
               !path.contains("..") &&
               path.length < 256
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "设置",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "配置文件路径",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                
                OutlinedTextField(
                    value = tempPath,
                    onValueChange = { 
                        tempPath = it
                        isValidPath = validatePath(it)
                    },
                    label = { Text("配置文件完整路径") },
                    placeholder = { Text(ConfigManager.getDefaultPath()) },
                    isError = !isValidPath,
                    supportingText = {
                        if (!isValidPath) {
                            Text(
                                text = "路径格式无效。请输入以'/'开头，'.conf'结尾的完整路径",
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text("输入配置文件的完整路径，支持自定义模块目录")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "常用路径示例：",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "• ${ConfigManager.getDefaultPath()}",
                            fontSize = 11.sp
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (isValidPath) {
                                onPathChanged(tempPath)
                                onDismiss()
                            }
                        },
                        enabled = isValidPath && tempPath != currentPath
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}