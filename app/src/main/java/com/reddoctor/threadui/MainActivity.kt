package com.reddoctor.threadui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.derivedStateOf
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.reddoctor.threadui.data.AppListConfig
import com.reddoctor.threadui.data.GameConfig
import com.reddoctor.threadui.data.ThreadConfig
import com.reddoctor.threadui.ui.components.AboutDialog
import com.reddoctor.threadui.ui.components.AddGameDialog
import com.reddoctor.threadui.ui.components.BatchDeleteDialog
import com.reddoctor.threadui.ui.components.DeleteConfirmationDialog
import com.reddoctor.threadui.ui.components.GameEditDialog
import com.reddoctor.threadui.ui.components.ImportDialog
import com.reddoctor.threadui.ui.components.ShareDialog
import com.reddoctor.threadui.ui.theme.TreadUITheme
import com.reddoctor.threadui.utils.ImportUtils
import com.reddoctor.threadui.utils.RootUtils
import com.reddoctor.threadui.utils.ConfigManager
import com.reddoctor.threadui.ui.components.SettingsDialog
import com.reddoctor.threadui.utils.ShareUtils
import com.reddoctor.threadui.utils.GlobalExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TreadUITheme {
                AppListConfigScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListConfigScreen() {
    val context = LocalContext.current
    var isRootAvailable by remember { mutableStateOf(false) }
    var appListConfig by remember { mutableStateOf<AppListConfig?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showAddGameDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var selectedGame by remember { mutableStateOf<GameConfig?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var isModuleInstalled by remember { mutableStateOf(true) }
    
    // 悬浮滚动条状态
    var showFloatingScrollbar by remember { mutableStateOf(false) }
    var isDraggingScrollbar by remember { mutableStateOf(false) }
    var currentLetter by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()
    var configPath by remember { mutableStateOf(ConfigManager.getConfigPath(context)) }
    val listState = rememberLazyListState()
    
    // 监听滚动状态，显示/隐藏悬浮滚动条
    val isScrolling by remember {
        derivedStateOf {
            listState.isScrollInProgress
        }
    }
    
    LaunchedEffect(isScrolling, isDraggingScrollbar) {
        if (isScrolling || isDraggingScrollbar) {
            showFloatingScrollbar = true
        } else {
            // 停止滚动后延迟隐藏
            delay(2000)
            if (!isDraggingScrollbar) {
                showFloatingScrollbar = false
            }
        }
    }
    
    // 过滤和排序游戏列表
    val filteredGames = remember(appListConfig, searchQuery) {
        val games = if (searchQuery.isBlank()) {
            appListConfig?.games ?: emptyList()
        } else {
            appListConfig?.games?.filter { game ->
                game.name.contains(searchQuery, ignoreCase = true) ||
                game.packageName.contains(searchQuery, ignoreCase = true)
            } ?: emptyList()
        }
        // 按首字母排序，使用GameConfig中的firstLetter字段
        games.sortedWith(compareBy<GameConfig> { 
            when(it.firstLetter) {
                "#" -> "ZZ" // # 排在最后
                else -> it.firstLetter
            }
        }.thenBy { it.name }) // 相同首字母按游戏名称排序
    }
    
    // 生成字母索引列表
    val alphabetIndexes = remember(filteredGames) {
        val indexes = mutableMapOf<String, Int>()
        filteredGames.forEachIndexed { index, game ->
            val key = game.firstLetter
            if (!indexes.containsKey(key)) {
                indexes[key] = index
            }
        }
        // 按A-Z排序，#在最后
        val sortedKeys = indexes.keys.sortedWith { a, b ->
            when {
                a == "#" && b != "#" -> 1
                a != "#" && b == "#" -> -1
                else -> a.compareTo(b)
            }
        }
        sortedKeys.associateWith { indexes[it] ?: 0 }
    }
    
    // 检查root权限和模块安装状态
    LaunchedEffect(Unit) {
        scope.launch(GlobalExceptionHandler.createCoroutineExceptionHandler("RootCheck")) {
            isRootAvailable = withContext(Dispatchers.IO) {
                RootUtils.isRootAvailable()
            }
            
            if (isRootAvailable) {
                isModuleInstalled = withContext(Dispatchers.IO) {
                    RootUtils.checkModuleByConfigPath(configPath)
                }
            }
        }
    }
    
    // 加载配置文件
    fun loadConfig() {
        scope.launch(GlobalExceptionHandler.createCoroutineExceptionHandler("ConfigLoad")) {
            isLoading = true
            errorMessage = null
            
            val result = withContext(Dispatchers.IO) {
                RootUtils.readFileAsRoot(configPath)
            }
            
            if (result.isSuccess) {
                val content = result.getOrNull() ?: ""
                appListConfig = AppListConfig.parseFromContent(content)
            } else {
                val error = "读取配置文件失败: ${result.exceptionOrNull()?.message}"
                errorMessage = error
                GlobalExceptionHandler.logException("ConfigLoad", error, result.exceptionOrNull())
            }
            
            isLoading = false
        }
    }
    
    // 保存配置文件
    fun saveConfig() {
        appListConfig?.let { config ->
            scope.launch(GlobalExceptionHandler.createCoroutineExceptionHandler("ConfigSave")) {
                isLoading = true
                errorMessage = null
                
                val configString = AppListConfig.toConfigString(config)
                val result = withContext(Dispatchers.IO) {
                    RootUtils.writeFileAsRoot(configPath, configString)
                }
                
                if (result.isFailure) {
                    val error = "保存配置文件失败: ${result.exceptionOrNull()?.message}"
                    errorMessage = error
                    GlobalExceptionHandler.logException("ConfigSave", error, result.exceptionOrNull())
                } else {
                    // 重新加载配置以验证保存是否成功
                    loadConfig()
                }
                
                isLoading = false
            }
        }
    }
    
    Scaffold(
        topBar = {
            if (isSearching) {
                // 搜索模式的TopAppBar
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("搜索游戏名称或包名...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "清空搜索")
                                    }
                                }
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { 
                            isSearching = false
                            searchQuery = ""
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "退出搜索")
                        }
                    }
                )
            } else {
                // 正常模式的TopAppBar
                TopAppBar(
                    title = { Text("AppList 配置管理器") },
                    actions = {
                        IconButton(onClick = { showAddGameDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "新增游戏")
                        }
                        IconButton(onClick = { isSearching = true }) {
                            Icon(Icons.Default.Search, contentDescription = "搜索")
                        }
                        
                        // 更多操作菜单
                        var showMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "更多")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                // 只有在有Root权限且模块已安装时才显示完整菜单
                                if (isRootAvailable && isModuleInstalled && !isLoading) {
                                    DropdownMenuItem(
                                        text = { Text("导入配置") },
                                        onClick = {
                                            showImportDialog = true
                                            showMenu = false
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Add, contentDescription = null)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("批量分享") },
                                        onClick = {
                                            if (appListConfig?.games?.isNotEmpty() == true) {
                                                showShareDialog = true
                                            }
                                            showMenu = false
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Share, contentDescription = null)
                                        },
                                        enabled = appListConfig?.games?.isNotEmpty() == true
                                    )
                                    DropdownMenuItem(
                                        text = { Text("批量删除") },
                                        onClick = {
                                            if (appListConfig?.games?.isNotEmpty() == true) {
                                                showBatchDeleteDialog = true
                                            }
                                            showMenu = false
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Delete, contentDescription = null)
                                        },
                                        enabled = appListConfig?.games?.isNotEmpty() == true
                                    )
                                    DropdownMenuItem(
                                        text = { Text("刷新") },
                                        onClick = {
                                            loadConfig()
                                            showMenu = false
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Refresh, contentDescription = null)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("保存") },
                                        onClick = {
                                            saveConfig()
                                            showMenu = false
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("设置") },
                                        onClick = {
                                            showSettingsDialog = true
                                            showMenu = false
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Settings, contentDescription = null)
                                        }
                                    )
                                }
                                
                                // 关于选项始终显示
                                DropdownMenuItem(
                                    text = { Text("关于") },
                                    onClick = {
                                        showAboutDialog = true
                                        showMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Info, contentDescription = null)
                                    }
                                )
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                !isRootAvailable -> {
                    RootPermissionRequiredCard(
                        onRefreshClick = {
                            scope.launch(GlobalExceptionHandler.createCoroutineExceptionHandler("RefreshRoot")) {
                                isRootAvailable = withContext(Dispatchers.IO) {
                                    RootUtils.isRootAvailable()
                                }
                                if (isRootAvailable) {
                                    // 检查模块状态
                                    isModuleInstalled = withContext(Dispatchers.IO) {
                                        RootUtils.checkModuleByConfigPath(configPath)
                                    }
                                    if (isModuleInstalled) {
                                        loadConfig()
                                    }
                                }
                            }
                        }
                    )
                }
                
                !isModuleInstalled -> {
                    ModuleInstallationRequiredCard(
                        configPath = configPath,
                        onRefreshClick = {
                            scope.launch(GlobalExceptionHandler.createCoroutineExceptionHandler("RefreshModule")) {
                                isModuleInstalled = withContext(Dispatchers.IO) {
                                    RootUtils.checkModuleByConfigPath(configPath)
                                }
                                if (isModuleInstalled) {
                                    loadConfig()
                                }
                            }
                        },
                        onSettingsClick = {
                            showSettingsDialog = true
                        },
                        onInstallClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://appopt.suto.top/#download"))
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        }
                    )
                }
                
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                errorMessage != null -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Card(
                                modifier = Modifier.size(80.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                                ),
                                shape = RoundedCornerShape(40.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = "错误",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "错误",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                errorMessage!!,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { loadConfig() },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text(
                                    "重试",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                
                appListConfig == null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Card(
                            modifier = Modifier.size(100.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(50.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "刷新",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "点击刷新按钮加载配置文件",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { loadConfig() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(56.dp)
                                .fillMaxWidth(0.6f)
                        ) {
                            Text(
                                "加载配置",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                else -> {
                    Column {
                        // 搜索快捷操作栏（仅在正常模式显示）
                        if (!isSearching && appListConfig?.games?.isNotEmpty() == true) {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(getQuickSearchTags(appListConfig?.games ?: emptyList())) { tag ->
                                    QuickSearchChip(
                                        text = tag,
                                        onClick = {
                                            searchQuery = tag
                                            isSearching = true
                                        }
                                    )
                                }
                            }
                        }
                        
                        // 搜索结果统计
                        if (isSearching && searchQuery.isNotEmpty()) {
                            // 搜索结果统计
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "找到 ${filteredGames.size} 个匹配的游戏",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        
                        if (filteredGames.isEmpty() && searchQuery.isNotEmpty()) {
                            // 无搜索结果
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(40.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Card(
                                        modifier = Modifier.size(80.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                        ),
                                        shape = RoundedCornerShape(40.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Search,
                                                contentDescription = null,
                                                modifier = Modifier.size(40.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "未找到匹配的游戏",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth(),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "尝试搜索游戏名称或包名",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        } else {
                            // 游戏列表
                            Box(modifier = Modifier.fillMaxSize()) {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(filteredGames) { game ->
                                        GameConfigCard(
                                            game = game,
                                            searchQuery = searchQuery,
                                            onEditClick = {
                                                selectedGame = game
                                                showEditDialog = true
                                            },
                                            onShareClick = {
                                                ShareUtils.shareGameConfig(context, game)
                                            },
                                            onDeleteClick = {
                                                selectedGame = game
                                                showDeleteDialog = true
                                            }
                                        )
                                    }
                                }
                                
                                // 悬浮滚动条和字母指示器
                                FloatingScrollbar(
                                    alphabetIndexes = alphabetIndexes,
                                    listState = listState,
                                    visible = showFloatingScrollbar,
                                    onDragStart = { isDraggingScrollbar = true },
                                    onDragEnd = { isDraggingScrollbar = false },
                                    onLetterChanged = { letter -> currentLetter = letter },
                                    modifier = Modifier.align(Alignment.CenterEnd)
                                )
                                
                                // 字母指示器
                                if (isDraggingScrollbar && currentLetter.isNotEmpty()) {
                                    LetterIndicator(
                                        letter = currentLetter,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // 删除确认对话框
    if (showDeleteDialog && selectedGame != null) {
        DeleteConfirmationDialog(
            gameName = selectedGame!!.name,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                appListConfig = appListConfig?.copy(
                    games = appListConfig!!.games.filter { it.packageName != selectedGame!!.packageName }
                )
                saveConfig()
                showDeleteDialog = false
                selectedGame = null
            }
        )
    }
    
    // 批量删除对话框
    if (showBatchDeleteDialog && appListConfig != null) {
        BatchDeleteDialog(
            games = appListConfig!!.games,
            onDismiss = { showBatchDeleteDialog = false },
            onConfirm = { gamesToDelete ->
                val packageNamesToDelete = gamesToDelete.map { it.packageName }.toSet()
                appListConfig = appListConfig?.copy(
                    games = appListConfig!!.games.filter { !packageNamesToDelete.contains(it.packageName) }
                )
                saveConfig()
                showBatchDeleteDialog = false
            }
        )
    }
    
    // 分享对话框
    if (showShareDialog && appListConfig != null) {
        ShareDialog(
            games = appListConfig!!.games,
            onDismiss = { showShareDialog = false }
        )
    }
    
    // 导入对话框
    if (showImportDialog) {
        ImportDialog(
            onDismiss = { showImportDialog = false },
            onImport = { importedGames ->
                val (newConfig, overwrittenGames) = ImportUtils.mergeWithExistingConfig(
                    appListConfig, 
                    importedGames
                )
                appListConfig = newConfig
                saveConfig()
                showImportDialog = false
            }
        )
    }
    
    // 编辑对话框
    if (showEditDialog && selectedGame != null) {
        GameEditDialog(
            game = selectedGame!!,
            onDismiss = { showEditDialog = false },
            onSave = { updatedGame ->
                // 更新配置
                appListConfig = appListConfig?.copy(
                    games = appListConfig!!.games.map { if (it.packageName == selectedGame!!.packageName) updatedGame else it }
                )
                // 保存到文件
                saveConfig()
                showEditDialog = false
            }
        )
    }
    
    // 新增游戏对话框
    if (showAddGameDialog) {
        AddGameDialog(
            onDismiss = { showAddGameDialog = false },
            onSave = { newGame: GameConfig ->
                // 添加新游戏到配置
                appListConfig = appListConfig?.copy(
                    games = appListConfig!!.games + listOf(newGame)
                ) ?: AppListConfig(listOf(newGame))
                // 保存到文件
                saveConfig()
                showAddGameDialog = false
            }
        )
    }
    
    // 关于对话框
    if (showAboutDialog) {
        AboutDialog(
            onDismiss = { showAboutDialog = false }
        )
    }
    
    // 设置对话框
    if (showSettingsDialog) {
        SettingsDialog(
            currentPath = configPath,
            onDismiss = { showSettingsDialog = false },
            onPathChanged = { newPath ->
                scope.launch(GlobalExceptionHandler.createCoroutineExceptionHandler("PathChange")) {
                    // 首先清空配置，避免状态冲突
                    appListConfig = null
                    errorMessage = null
                    
                    // 然后更新路径配置
                    ConfigManager.setConfigPath(context, newPath)
                    configPath = newPath
                    
                    // 最后检查模块状态
                    isModuleInstalled = withContext(Dispatchers.IO) {
                        RootUtils.checkModuleByConfigPath(newPath)
                    }
                }
            }
        )
    }
}

@Composable
fun GameConfigCard(
    game: GameConfig,
    searchQuery: String = "",
    onEditClick: () -> Unit,
    onShareClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isGameNameExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // 游戏信息和操作按钮区域
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // 游戏名称 - 支持搜索高亮和点击展开
                    val displayName = if (isGameNameExpanded || game.name.length <= 5) {
                        game.name
                    } else {
                        game.name.take(5) + "..."
                    }
                    
                    if (searchQuery.isNotEmpty() && game.name.contains(searchQuery, ignoreCase = true)) {
                        HighlightedText(
                            text = displayName,
                            searchQuery = searchQuery,
                            modifier = if (game.name.length > 5) Modifier.clickable { isGameNameExpanded = !isGameNameExpanded } else Modifier,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = if (game.name.length > 5) Modifier.clickable { isGameNameExpanded = !isGameNameExpanded } else Modifier
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    // 包名 - 支持搜索高亮
                    if (searchQuery.isNotEmpty() && game.packageName.contains(searchQuery, ignoreCase = true)) {
                        HighlightedText(
                            text = game.packageName,
                            searchQuery = searchQuery,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = game.packageName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 如果是渠道服（游戏名包含包名标识），显示提示
                    if (game.name.contains(" (") && game.name.endsWith(")")) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "渠道服",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    Row {
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "删除",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        
                        IconButton(
                            onClick = onShareClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "分享",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        Button(
                            onClick = onEditClick,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text(
                                "编辑",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 线程配置折叠区域
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Card(
                        modifier = Modifier.size(6.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(3.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "线程配置",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${game.threadConfigs.size} 项",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "收起" else "展开",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // 动画展开的线程配置列表
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 线程配置列表
                    game.threadConfigs.forEach { threadConfig ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = threadConfig.threadName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                                
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "CPU: ${threadConfig.cpuCores}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GameConfigCardPreview() {
    TreadUITheme {
        val sampleGame = GameConfig(
            name = "王者荣耀",
            packageName = "com.tencent.tmgp.sgame",
            threadConfigs = listOf(
                ThreadConfig("UnityMain", "7"),
                ThreadConfig("UnityGfxDeviceW", "2-4"),
                ThreadConfig("主进程", "2-6")
            )
        )
        GameConfigCard(game = sampleGame, onEditClick = {})
    }
}

@Preview(showBackground = true)
@Composable
fun SearchHighlightPreview() {
    TreadUITheme {
        Column(modifier = Modifier.padding(16.dp)) {
            HighlightedText(
                text = "王者荣耀",
                searchQuery = "王者",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            HighlightedText(
                text = "包名: com.tencent.tmgp.sgame",
                searchQuery = "tencent",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun QuickSearchChipsPreview() {
    TreadUITheme {
        LazyRow(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(listOf("腾讯", "网易", "MOBA", "射击")) { tag ->
                QuickSearchChip(
                    text = tag,
                    onClick = { }
                )
            }
        }
    }
}

@Composable
fun HighlightedText(
    text: String,
    searchQuery: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontWeight: FontWeight? = null
) {
    if (searchQuery.isEmpty()) {
        Text(
            text = text,
            modifier = modifier,
            style = style,
            color = color,
            fontWeight = fontWeight
        )
        return
    }

    val annotatedString = buildAnnotatedString {
        val lowerText = text.lowercase()
        val lowerQuery = searchQuery.lowercase()
        var lastIndex = 0
        
        while (true) {
            val index = lowerText.indexOf(lowerQuery, lastIndex)
            if (index == -1) break
            
            // 添加高亮前的普通文本
            if (index > lastIndex) {
                append(text.substring(lastIndex, index))
            }
            
            // 添加高亮文本
            withStyle(
                style = SpanStyle(
                    background = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            ) {
                append(text.substring(index, index + searchQuery.length))
            }
            
            lastIndex = index + searchQuery.length
        }
        
        // 添加剩余的普通文本
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
    
    Text(
        text = annotatedString,
        modifier = modifier,
        style = style,
        color = color,
        fontWeight = fontWeight
    )
}

// 获取快捷搜索标签
fun getQuickSearchTags(games: List<GameConfig>): List<String> {
    val tags = mutableSetOf<String>()

//    // 添加常见的公司/开发商标签
//    games.forEach { game ->
//        when {
//            game.packageName.contains("tencent") -> tags.add("腾讯")
//            game.packageName.contains("miHoYo") -> tags.add("米哈游")
//            game.packageName.contains("netease") -> tags.add("网易")
//        }
//    }
//
//    // 添加游戏类型标签
//    if (games.any { it.name.contains("王者") || it.name.contains("MOBA") }) tags.add("MOBA")
//    if (games.any { it.name.contains("吃鸡") || it.name.contains("和平精英") || it.name.contains("绝地求生") }) tags.add("射击")
//    if (games.any { it.name.contains("角色扮演") || it.name.contains("RPG") }) tags.add("RPG")
//
    return tags.take(5) // 限制最多5个标签
}

@Composable
fun QuickSearchChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun RootPermissionRequiredCard(
    onRefreshClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.size(80.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(40.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "警告",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "需要Root权限",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "此应用需要Root权限来访问系统配置文件",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 刷新权限按钮
            Button(
                onClick = onRefreshClick,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(56.dp)
                    .fillMaxWidth(0.6f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(
                    text = "刷新权限",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ModuleInstallationRequiredCard(
    configPath: String,
    onRefreshClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onInstallClick: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.size(80.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(40.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "安装",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "需要安装线程优化模块",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "未找到 ${ConfigManager.getModuleName(configPath)} 模块",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "配置路径: $configPath",
                style = MaterialTheme.typography.bodySmall,
                color = if (ConfigManager.isUsingDefaultPath(context)) {
                    MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.primary
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                fontWeight = if (ConfigManager.isUsingDefaultPath(context)) {
                    FontWeight.Normal
                } else {
                    FontWeight.Medium
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        "安装步骤：",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val steps = listOf(
                        "1. 获取 Root 权限",
                        "2. 安装 Magisk 框架", 
                        "3. 下载线程优化模块",
                        "4. 激活模块并重启"
                    )
                    
                    steps.forEach { step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Card(
                                modifier = Modifier.size(6.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(3.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 操作按钮区域
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 自定义路径按钮
                    OutlinedButton(
                        onClick = onSettingsClick,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(56.dp)
                            .weight(1f),
                        contentPadding = PaddingValues(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "自定义路径",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    // 下载模块按钮
                    Button(
                        onClick = onInstallClick,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(56.dp)
                            .weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        ),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        Text(
                            text = "下载模块",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                // 检查状态按钮
                Button(
                    onClick = onRefreshClick,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(56.dp)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Text(
                        text = "检查模块状态",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun FloatingScrollbar(
    alphabetIndexes: Map<String, Int>,
    listState: LazyListState,
    visible: Boolean,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onLetterChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val alphabetKeys = alphabetIndexes.keys.toList()
    
    if (!visible || alphabetKeys.isEmpty()) return
    
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.zIndex(1f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(40.dp)
                .padding(vertical = 32.dp, horizontal = 8.dp)
        ) {
            // 蓝色滚动块 - 移除轨道背景
            Canvas(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(24.dp)
                    .align(Alignment.Center)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                onDragStart()
                                // 处理初始点击位置
                                val totalHeight = size.height.toFloat()
                                val currentY = (offset.y / totalHeight).coerceIn(0f, 1f)
                                val letterIndex = (currentY * alphabetKeys.size).toInt()
                                    .coerceIn(0, alphabetKeys.size - 1)
                                
                                val letter = alphabetKeys[letterIndex]
                                onLetterChanged(letter)
                                
                                scope.launch {
                                    val index = alphabetIndexes[letter] ?: 0
                                    listState.animateScrollToItem(index)
                                }
                            },
                            onDragEnd = {
                                onDragEnd()
                            }
                        ) { change, _ ->
                            // 计算当前拖拽位置对应的字母
                            val totalHeight = size.height.toFloat()
                            val currentY = (change.position.y / totalHeight).coerceIn(0f, 1f)
                            val letterIndex = (currentY * alphabetKeys.size).toInt()
                                .coerceIn(0, alphabetKeys.size - 1)
                            
                            val letter = alphabetKeys[letterIndex]
                            onLetterChanged(letter)
                            
                            // 滚动到对应位置
                            scope.launch {
                                val index = alphabetIndexes[letter] ?: 0
                                listState.animateScrollToItem(index)
                            }
                        }
                    }
            ) {
                val trackHeight = size.height
                val thumbHeight = 40.dp.toPx()
                
                // 计算当前滚动位置
                val firstVisibleItem = listState.firstVisibleItemIndex
                val totalItems = listState.layoutInfo.totalItemsCount
                val scrollProgress = if (totalItems > 0) {
                    firstVisibleItem.toFloat() / totalItems.coerceAtLeast(1)
                } else 0f
                
                val thumbY = (trackHeight - thumbHeight) * scrollProgress
                
                // 绘制带箭头的滚动块
                drawRoundRect(
                    color = androidx.compose.ui.graphics.Color(0xFF2196F3),
                    topLeft = Offset(0f, thumbY),
                    size = androidx.compose.ui.geometry.Size(size.width, thumbHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx())
                )
                
                // 绘制上箭头
                val arrowSize = 8.dp.toPx()
                val centerX = size.width / 2f
                val upArrowY = thumbY + 10.dp.toPx()
                drawPath(
                    path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(centerX, upArrowY)
                        lineTo(centerX - arrowSize / 2, upArrowY + arrowSize / 2)
                        lineTo(centerX + arrowSize / 2, upArrowY + arrowSize / 2)
                        close()
                    },
                    color = androidx.compose.ui.graphics.Color.White
                )
                
                // 绘制下箭头
                val downArrowY = thumbY + thumbHeight - 10.dp.toPx()
                drawPath(
                    path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(centerX, downArrowY)
                        lineTo(centerX - arrowSize / 2, downArrowY - arrowSize / 2)
                        lineTo(centerX + arrowSize / 2, downArrowY - arrowSize / 2)
                        close()
                    },
                    color = androidx.compose.ui.graphics.Color.White
                )
            }
        }
    }
}

@Composable
fun LetterIndicator(
    letter: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.size(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(40.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = letter,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
        }
    }
}