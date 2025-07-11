package com.reddoctor.treadui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reddoctor.treadui.data.AppListConfig
import com.reddoctor.treadui.data.GameConfig
import com.reddoctor.treadui.data.ThreadConfig
import com.reddoctor.treadui.ui.components.AddGameDialog
import com.reddoctor.treadui.ui.components.GameEditDialog
import com.reddoctor.treadui.ui.theme.TreadUITheme
import com.reddoctor.treadui.utils.RootUtils
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
    var isRootAvailable by remember { mutableStateOf(false) }
    var appListConfig by remember { mutableStateOf<AppListConfig?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showAddGameDialog by remember { mutableStateOf(false) }
    var selectedGame by remember { mutableStateOf<GameConfig?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var isModuleInstalled by remember { mutableStateOf(true) }
    
    val scope = rememberCoroutineScope()
    val configPath = "/data/adb/modules/AppOpt/applist.conf"
    
    // 过滤游戏列表
    val filteredGames = remember(appListConfig, searchQuery) {
        if (searchQuery.isBlank()) {
            appListConfig?.games ?: emptyList()
        } else {
            appListConfig?.games?.filter { game ->
                game.name.contains(searchQuery, ignoreCase = true) ||
                game.packageName.contains(searchQuery, ignoreCase = true)
            } ?: emptyList()
        }
    }
    
    // 检查root权限和模块安装状态
    LaunchedEffect(Unit) {
        scope.launch {
            isRootAvailable = withContext(Dispatchers.IO) {
                RootUtils.isRootAvailable()
            }
            
            if (isRootAvailable) {
                isModuleInstalled = withContext(Dispatchers.IO) {
                    RootUtils.checkAppOptModuleExists()
                }
            }
        }
    }
    
    // 加载配置文件
    fun loadConfig() {
        scope.launch {
            isLoading = true
            errorMessage = null
            
            try {
                val result = withContext(Dispatchers.IO) {
                    RootUtils.readFileAsRoot(configPath)
                }
                
                if (result.isSuccess) {
                    val content = result.getOrNull() ?: ""
                    appListConfig = AppListConfig.parseFromContent(content)
                } else {
                    errorMessage = "读取配置文件失败: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                errorMessage = "加载配置时发生错误: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    // 保存配置文件
    fun saveConfig() {
        appListConfig?.let { config ->
            scope.launch {
                isLoading = true
                errorMessage = null
                
                try {
                    val configString = AppListConfig.toConfigString(config)
                    val result = withContext(Dispatchers.IO) {
                        RootUtils.writeFileAsRoot(configPath, configString)
                    }
                    
                    if (result.isFailure) {
                        errorMessage = "保存配置文件失败: ${result.exceptionOrNull()?.message}"
                    } else {
                        // 重新加载配置以验证保存是否成功
                        loadConfig()
                    }
                } catch (e: Exception) {
                    errorMessage = "保存配置时发生错误: ${e.message}"
                } finally {
                    isLoading = false
                }
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
                        IconButton(onClick = { loadConfig() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新")
                        }
                        IconButton(onClick = { saveConfig() }) {
                            Icon(Icons.Default.Check, contentDescription = "保存")
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
                    RootPermissionRequiredCard()
                }
                
                !isModuleInstalled -> {
                    ModuleInstallationRequiredCard(
                        onRefreshClick = {
                            scope.launch {
                                isModuleInstalled = withContext(Dispatchers.IO) {
                                    RootUtils.checkAppOptModuleExists()
                                }
                            }
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
                                items(getQuickSearchTags(appListConfig!!.games)) { tag ->
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
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredGames) { game ->
                                    GameConfigCard(
                                        game = game,
                                        searchQuery = searchQuery,
                                        onEditClick = {
                                            selectedGame = game
                                            showEditDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
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
}

@Composable
fun GameConfigCard(
    game: GameConfig,
    searchQuery: String = "",
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // 游戏名称 - 支持搜索高亮
                    if (searchQuery.isNotEmpty() && game.name.contains(searchQuery, ignoreCase = true)) {
                        HighlightedText(
                            text = game.name,
                            searchQuery = searchQuery,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Text(
                            text = game.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // 包名 - 支持搜索高亮
                    if (searchQuery.isNotEmpty() && game.packageName.contains(searchQuery, ignoreCase = true)) {
                        HighlightedText(
                            text = game.packageName,
                            searchQuery = searchQuery,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = game.packageName,
                            style = MaterialTheme.typography.bodyMedium,
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
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "渠道服",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    
                    Button(
                        onClick = onEditClick,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(
                            "编辑",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 线程配置标题
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
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
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
fun RootPermissionRequiredCard() {
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
        }
    }
}

@Composable
fun ModuleInstallationRequiredCard(
    onRefreshClick: () -> Unit
) {
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
            modifier = Modifier.padding(32.dp),
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
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "需要安装线程优化模块",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "未找到 AppOpt 模块，请先安装线程优化模块",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
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
                        "1. 确保设备已获取 Root 权限",
                        "2. 安装 Magisk 框架",
                        "3. 下载 AppOpt 线程优化模块",
                        "4. 在 Magisk 中激活模块",
                        "5. 重启设备后点击下方刷新按钮"
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
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onRefreshClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(56.dp)
                    .fillMaxWidth(0.6f)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "刷新",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "检查模块状态",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}