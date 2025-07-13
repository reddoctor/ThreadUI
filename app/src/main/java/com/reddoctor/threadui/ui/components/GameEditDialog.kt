package com.reddoctor.threadui.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.reddoctor.threadui.data.GameConfig
import com.reddoctor.threadui.data.ThreadConfig
import com.reddoctor.threadui.utils.CpuCoreValidator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameEditDialog(
    game: GameConfig,
    onDismiss: () -> Unit,
    onSave: (GameConfig) -> Unit
) {
    var editedGame by remember { mutableStateOf(game) }
    var showAddThreadDialog by remember { mutableStateOf(false) }
    // 存储正在编辑的线程配置状态
    var editingStates by remember { mutableStateOf<Map<ThreadConfig, Pair<String, String>>>(emptyMap()) }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "编辑游戏配置",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 游戏名称
                OutlinedTextField(
                    value = editedGame.name,
                    onValueChange = { newName ->
                        editedGame = editedGame.copy(name = newName)
                    },
                    label = { Text("游戏名称") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 包名
                OutlinedTextField(
                    value = editedGame.packageName,
                    onValueChange = { editedGame = editedGame.copy(packageName = it) },
                    label = { Text("包名") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 线程配置标题
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "线程配置",
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(onClick = { showAddThreadDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "添加线程配置")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 线程配置列表
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    items(editedGame.threadConfigs) { threadConfig ->
                        ThreadConfigItem(
                            threadConfig = threadConfig,
                            onUpdate = { updatedConfig ->
                                editedGame = editedGame.copy(
                                    threadConfigs = editedGame.threadConfigs.map {
                                        if (it == threadConfig) updatedConfig else it
                                    }
                                )
                            },
                            onDelete = {
                                editedGame = editedGame.copy(
                                    threadConfigs = editedGame.threadConfigs.filter { it != threadConfig }
                                )
                            },
                            onEditStateChanged = { isEditing, editedName, editedCores ->
                                editingStates = if (isEditing) {
                                    editingStates + (threadConfig to (editedName to editedCores))
                                } else {
                                    editingStates - threadConfig
                                }
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { 
                        // 在保存前，应用所有正在编辑的线程配置
                        var finalGame = editedGame
                        editingStates.forEach { (originalConfig, editedValues) ->
                            val (editedName, editedCores) = editedValues
                            if (editedName.isNotBlank() && editedCores.isNotBlank()) {
                                finalGame = finalGame.copy(
                                    threadConfigs = finalGame.threadConfigs.map { config ->
                                        if (config == originalConfig) {
                                            // 主进程保持原名称，其他线程使用编辑后的名称
                                            val finalName = if (originalConfig.threadName == "主进程") {
                                                originalConfig.threadName
                                            } else {
                                                editedName
                                            }
                                            ThreadConfig(finalName, editedCores)
                                        } else config
                                    }
                                )
                            }
                        }
                        onSave(finalGame) 
                    }) {
                        Text("保存")
                    }
                }
            }
        }
    }
    
    // 添加线程配置对话框
    if (showAddThreadDialog) {
        AddThreadConfigDialog(
            onDismiss = { showAddThreadDialog = false },
            onAdd = { threadConfig ->
                editedGame = editedGame.copy(
                    threadConfigs = editedGame.threadConfigs + threadConfig
                )
                showAddThreadDialog = false
            }
        )
    }
}

@Composable
fun ThreadConfigItem(
    threadConfig: ThreadConfig,
    onUpdate: (ThreadConfig) -> Unit,
    onDelete: () -> Unit,
    onEditStateChanged: (Boolean, String, String) -> Unit = { _, _, _ -> }
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedThreadName by remember { mutableStateOf(threadConfig.threadName) }
    var editedCpuCores by remember { mutableStateOf(threadConfig.cpuCores) }
    var cpuCoreValidation by remember { mutableStateOf(CpuCoreValidator.ValidationResult(true, "")) }
    
    // 监听编辑状态变化
    LaunchedEffect(isEditing, editedThreadName, editedCpuCores) {
        onEditStateChanged(isEditing, editedThreadName, editedCpuCores)
    }
    
    // CPU核心校验
    LaunchedEffect(editedCpuCores) {
        if (editedCpuCores.isNotBlank()) {
            cpuCoreValidation = CpuCoreValidator.validateCpuCores(editedCpuCores)
        } else {
            cpuCoreValidation = CpuCoreValidator.ValidationResult(true, "")
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            if (isEditing) {
                // 编辑模式
                val isMainProcess = threadConfig.threadName == "主进程"
                
                if (isMainProcess) {
                    // 主进程线程名称只显示，不可编辑
                    Text(
                        text = "线程名称: ${threadConfig.threadName}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    // 其他线程可以编辑名称
                    OutlinedTextField(
                        value = editedThreadName,
                        onValueChange = { 
                            editedThreadName = it
                        },
                        label = { Text("线程名称") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                OutlinedTextField(
                    value = editedCpuCores,
                    onValueChange = { 
                        editedCpuCores = it
                    },
                    label = { Text("CPU核心") },
                    placeholder = { Text(CpuCoreValidator.getCpuCoreHint()) },
                    isError = !cpuCoreValidation.isValid,
                    supportingText = {
                        if (!cpuCoreValidation.isValid) {
                            Text(
                                text = cpuCoreValidation.message,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text("支持格式: 0-7 | 4 | 0,2,4 | 0-3,6-7")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { 
                        // 重置编辑内容
                        editedThreadName = threadConfig.threadName
                        editedCpuCores = threadConfig.cpuCores
                        cpuCoreValidation = CpuCoreValidator.ValidationResult(true, "")
                        isEditing = false 
                    }) {
                        Text("取消")
                    }
                    TextButton(
                        onClick = {
                            if (cpuCoreValidation.isValid) {
                                // 根据是否为主进程决定是否更新线程名称
                                val finalThreadName = if (isMainProcess) threadConfig.threadName else editedThreadName
                                onUpdate(ThreadConfig(finalThreadName, editedCpuCores))
                                isEditing = false
                            }
                        },
                        enabled = cpuCoreValidation.isValid
                    ) {
                        Text("保存")
                    }
                }
            } else {
                // 显示模式
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = threadConfig.threadName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "CPU核心: ${threadConfig.cpuCores}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Row {
                        IconButton(onClick = { 
                            editedThreadName = threadConfig.threadName
                            editedCpuCores = threadConfig.cpuCores
                            cpuCoreValidation = CpuCoreValidator.ValidationResult(true, "")
                            isEditing = true 
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑")
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "删除")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddThreadConfigDialog(
    onDismiss: () -> Unit,
    onAdd: (ThreadConfig) -> Unit
) {
    var threadName by remember { mutableStateOf("") }
    var cpuCores by remember { mutableStateOf("") }
    var isMainProcess by remember { mutableStateOf(false) }
    var cpuCoreValidation by remember { mutableStateOf(CpuCoreValidator.ValidationResult(true, "")) }
    
    // 当选择主进程时，自动设置线程名称
    LaunchedEffect(isMainProcess) {
        if (isMainProcess) {
            threadName = "主进程"
        } else {
            threadName = ""
        }
    }
    
    // CPU核心校验
    LaunchedEffect(cpuCores) {
        if (cpuCores.isNotBlank()) {
            cpuCoreValidation = CpuCoreValidator.validateCpuCores(cpuCores)
        } else {
            cpuCoreValidation = CpuCoreValidator.ValidationResult(true, "")
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加线程配置") },
        text = {
            Column {
                // 主进程选择器
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isMainProcess,
                        onCheckedChange = { isMainProcess = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "主进程",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 线程名称输入框（主进程时隐藏）
                if (!isMainProcess) {
                    OutlinedTextField(
                        value = threadName,
                        onValueChange = { threadName = it },
                        label = { Text("线程名称") },
                        placeholder = { Text("例: UnityMain") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    // 主进程时显示固定名称
                    Text(
                        text = "线程名称: 主进程",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                OutlinedTextField(
                    value = cpuCores,
                    onValueChange = { cpuCores = it },
                    label = { Text("CPU核心") },
                    placeholder = { Text(CpuCoreValidator.getCpuCoreHint()) },
                    isError = !cpuCoreValidation.isValid,
                    supportingText = {
                        if (!cpuCoreValidation.isValid) {
                            Text(
                                text = cpuCoreValidation.message,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text("支持格式: 0-7 | 4 | 0,2,4 | 0-3,6-7")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalThreadName = if (isMainProcess) "主进程" else threadName
                    if (finalThreadName.isNotBlank() && cpuCores.isNotBlank() && cpuCoreValidation.isValid) {
                        onAdd(ThreadConfig(finalThreadName, cpuCores))
                    }
                },
                enabled = cpuCores.isNotBlank() && 
                         (isMainProcess || threadName.isNotBlank()) && 
                         cpuCoreValidation.isValid
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGameDialog(
    onDismiss: () -> Unit,
    onSave: (GameConfig) -> Unit
) {
    var gameName by remember { mutableStateOf("") }
    var packageName by remember { mutableStateOf("") }
    var threadConfigs by remember { mutableStateOf(listOf<ThreadConfig>()) }
    var showAddThreadDialog by remember { mutableStateOf(false) }
    var showAppSelector by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "新增游戏配置",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 应用选择按钮
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showAppSelector = true },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.AccountBox,
                            contentDescription = "从应用列表选择",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "从应用列表选择",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "或手动输入",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 游戏名称
                OutlinedTextField(
                    value = gameName,
                    onValueChange = { gameName = it },
                    label = { Text("游戏名称") },
                    placeholder = { Text("例: 王者荣耀") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 包名
                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = { Text("包名") },
                    placeholder = { Text("例: com.tencent.tmgp.sgame") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 线程配置标题
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "线程配置",
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(onClick = { showAddThreadDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "添加线程配置")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 线程配置列表
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    items(threadConfigs) { threadConfig ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = threadConfig.threadName,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "CPU核心: ${threadConfig.cpuCores}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = {
                                    threadConfigs = threadConfigs.filter { it != threadConfig }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除")
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { 
                            if (gameName.isNotBlank() && packageName.isNotBlank() && threadConfigs.isNotEmpty()) {
                                onSave(GameConfig(gameName, packageName, threadConfigs))
                            }
                        },
                        enabled = gameName.isNotBlank() && packageName.isNotBlank() && threadConfigs.isNotEmpty()
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
    
    // 应用选择器对话框
    if (showAppSelector) {
        AppSelectorDialog(
            onDismiss = { showAppSelector = false },
            onAppSelected = { app ->
                gameName = app.displayName
                packageName = app.packageName
                showAppSelector = false
            }
        )
    }
    
    // 添加线程配置对话框
    if (showAddThreadDialog) {
        AddThreadConfigDialog(
            onDismiss = { showAddThreadDialog = false },
            onAdd = { threadConfig ->
                threadConfigs = threadConfigs + threadConfig
                showAddThreadDialog = false
            }
        )
    }
}