# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

ThreadUI is an Android application built with Kotlin and Jetpack Compose that serves as a configuration manager for AppList configurations. The app allows users to view and edit game optimization settings with dynamic path configuration, comprehensive error handling, and enhanced user interface features.

**Package**: `com.reddoctor.threadui`
**Min SDK**: 35 (Android 14)
**Target SDK**: 36
**Compile SDK**: 36
**Current Version**: 1.0.3
**Build Tools**: Gradle 8.11.0
**Kotlin Version**: 2.0.21

## Core Functionality

The application provides the following features:
- **Root Permission Management**: Detects and manages root access with refresh capabilities and contextual UI
- **Dynamic Configuration Path Management**: Customizable module paths with settings dialog and validation
- **Configuration File Parsing**: Reads and parses applist.conf files containing game thread configurations
- **Visual Configuration Editor**: Card-based UI with floating scrollbar and enhanced navigation
- **Intelligent Search**: Real-time search functionality with highlighting and quick search tags
- **Real-time Editing**: Add, modify, and delete thread configurations for games
- **System File Operations**: Read from and write to protected system directories
- **Configuration Sharing**: Export and import game configurations via JSON files with secure validation
- **Batch Operations**: Support for bulk export/import of multiple game configurations
- **Error Logging & Sharing**: Local error logging with shareable log files via hidden gesture
- **Enhanced Module Management**: Download links, custom path validation, and status checking
- **Global Exception Handling**: Centralized error management across the entire application
- **Advanced UI Components**: Floating scrollbar with alphabet indexing and directional arrows
- **About Dialog with QQ Support**: Comprehensive app information with QQ group integration
- **Hidden Debug Features**: Log sharing via consecutive clicks (5x) on version information

## Search Functionality

The app includes comprehensive search capabilities:
- **Real-time Search**: Filter games by name or package name with instant results
- **Search Highlighting**: Visual highlighting of search terms in results
- **Quick Search Tags**: Pre-defined tags for common developers and game types
- **Search State Management**: Toggle between normal and search modes
- **Empty State Handling**: User-friendly messages when no results are found

## User Interface Features

### Advanced Scrolling System
- **Floating Scrollbar**: Non-intrusive scrollbar that floats over content without reserving space
- **Alphabet Indexing**: Quick navigation using alphabetical letter indicators
- **Directional Arrows**: Visual up/down arrow indicators on the scrollbar
- **Auto-hide**: Scrollbar automatically appears during scrolling and hides after inactivity
- **Letter Indicator**: Large centered letter display during alphabet navigation
- **Transparent Track**: Minimalist design with near-transparent track (alpha = 0.1f)

### Enhanced About Dialog
- **Project Information**: Complete application details and build information including tech stack
- **QQ Group Integration**: Direct access to support QQ group (975905874) with join link and proper centering
- **GitHub Repository**: Direct link to project repository (https://github.com/reddoctor/ThreadUI)
- **Hidden Log Access**: Consecutive clicks (5x) on version info card triggers log sharing with progress indicator
- **Device Information**: Comprehensive device and app information for debugging
- **Core Features**: Visual list of all major application capabilities

### Module Status Management
- **Dynamic Path Configuration**: Settings dialog for custom module paths with real-time validation
- **Module Detection**: Intelligent detection of module installation status based on dynamic paths
- **Multiple Action Buttons**: Custom path settings (with distinct color), module download, and status checking
- **Contextual Menus**: Simplified menu options (only "关于") for Root/Module detection pages
- **Refresh Functionality**: Manual refresh buttons for permissions and module status
- **Download Integration**: Direct links to module download site (http://appopt.suto.top/#download)

## Architecture

### Project Structure
- **Main Application**: `app/src/main/java/com/reddoctor/threadui/MainActivity.kt`
- **Application Class**: `app/src/main/java/com/reddoctor/threadui/ThreadUIApplication.kt`
- **Data Models**: `app/src/main/java/com/reddoctor/threadui/data/GameConfig.kt`
- **Share Configuration**: `app/src/main/java/com/reddoctor/threadui/data/ShareConfig.kt`
- **Root Utilities**: `app/src/main/java/com/reddoctor/threadui/utils/RootUtils.kt`
- **Share Utilities**: `app/src/main/java/com/reddoctor/threadui/utils/ShareUtils.kt`
- **Import Utilities**: `app/src/main/java/com/reddoctor/threadui/utils/ImportUtils.kt`
- **Permission Utilities**: `app/src/main/java/com/reddoctor/threadui/utils/PermissionUtils.kt`
- **Configuration Management**: `app/src/main/java/com/reddoctor/threadui/utils/ConfigManager.kt`
- **Error Logging**: `app/src/main/java/com/reddoctor/threadui/utils/ErrorLogger.kt`
- **Global Exception Handler**: `app/src/main/java/com/reddoctor/threadui/utils/GlobalExceptionHandler.kt`
- **UI Components**: `app/src/main/java/com/reddoctor/threadui/ui/components/`
  - `AboutDialog.kt` - Enhanced about dialog with QQ group integration and log sharing
  - `SettingsDialog.kt` - Configuration path management with validation
  - `GameEditDialog.kt` - Game configuration editing interface
  - `ShareImportDialogs.kt` - Configuration sharing and import interfaces
  - `AppSelectorDialog.kt` - Application selection for configuration creation
- **UI Theme**: `app/src/main/java/com/reddoctor/threadui/ui/theme/`
- **Unit Tests**: `app/src/test/java/com/reddoctor/threadui/`
- **Instrumented Tests**: `app/src/androidTest/java/com/reddoctor/threadui/`

### Key Technologies
- **Jetpack Compose**: Modern declarative UI toolkit (2024.09.00)
- **Material 3**: Design system with dynamic color support
- **Kotlin**: Primary programming language (2.0.21)
- **AndroidX**: Core Android libraries
- **Root Access**: System-level file operations using shell commands
- **Coroutines**: Asynchronous programming with global exception handling
- **SharedPreferences**: Configuration persistence and settings management
- **Gradle**: Build system with Kotlin DSL (8.11.0)

### Data Models
- **GameConfig**: Represents a game with its package name and thread configurations
- **ThreadConfig**: Represents individual thread-to-CPU-core mappings
- **AppListConfig**: Container for all game configurations with parsing utilities
- **ShareConfig**: Handles export/import data format with security validation
- **AppInfo**: Contains application information for sharing features

### Key Utility Classes
- **RootUtils**: System-level operations with root privileges and global exception handling
- **ConfigManager**: Configuration path management using SharedPreferences
- **ErrorLogger**: Local error logging with file management and sharing capabilities
- **GlobalExceptionHandler**: Centralized exception handling for the entire application
- **ShareUtils**: File sharing functionality including error log sharing
- **ImportUtils**: Secure configuration import with validation

### Root Operations
The app uses `RootUtils` to perform system-level operations:
- Root permission detection
- File reading from protected directories
- File writing to protected directories
- Command execution with root privileges
- Dynamic module detection based on configuration paths

### Sharing and Import System
The app includes a comprehensive sharing system:
- **Individual Game Sharing**: Export single game configurations via ShareUtils
- **Batch Export**: Export multiple games in one operation
- **Secure Import**: Import configurations with malicious script detection
- **FileProvider Integration**: Uses Android FileProvider for secure file sharing
- **JSON Format**: All exports use structured JSON format via ShareConfig
- **Security Validation**: All imports are validated against malicious content

### Configuration Management
The app supports dynamic configuration paths:
- **ConfigManager**: Centralized management of configuration file paths using SharedPreferences
- **Default Path**: `/data/adb/modules/AppOpt/applist.conf`
- **Custom Paths**: Users can specify custom module paths via Settings dialog
- **Path Validation**: Comprehensive validation of custom configuration paths
- **Module Detection**: Automatic detection of module installation based on configuration path

### Error Handling and Logging
The application implements comprehensive error handling:
- **Global Exception Handler**: Centralized exception management for all components with `ThreadUIApplication` initialization
- **Error Logger**: Local error logging with automatic file management (max 1MB) in app private storage
- **Coroutine Exception Handling**: All coroutines use `GlobalExceptionHandler.createCoroutineExceptionHandler(tag)` with specific tags
- **Log Sharing**: Hidden feature to share error logs (click version info 5 times in About dialog) with progress indicator
- **Device Information**: Error logs include device and application information for debugging
- **Automatic Cleanup**: Old logs are automatically cleaned when size limit is reached
- **Thread Safety**: Exception handling works across main thread and background coroutines
- **Context Tagging**: Each exception is tagged with its source component for easier debugging (e.g., "ConfigLoad", "RootCheck")
- **File Provider Integration**: Secure log file sharing using Android FileProvider with timestamped log files
- **Replacement Strategy**: All individual try-catch blocks replaced with global exception handling
- **Application Class**: `ThreadUIApplication` registered in AndroidManifest.xml for global handler initialization

## Build System & Commands

This project uses Gradle with Kotlin DSL. All commands should be run from the project root.

### Essential Commands
```bash
# Build the project
./gradlew build

# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Install debug APK
./gradlew installDebug

# Clean build
./gradlew clean

# Assemble debug APK
./gradlew assembleDebug

# Assemble release APK
./gradlew assembleRelease
```

### Testing Commands
```bash
# Run specific unit test class
./gradlew test --tests com.reddoctor.threadui.ExampleUnitTest

# Run specific instrumented test class
./gradlew connectedAndroidTest --tests com.reddoctor.threadui.ExampleInstrumentedTest
```

## Permissions & Security

The app requires the following permissions:
- `WRITE_EXTERNAL_STORAGE`: For file operations
- `READ_EXTERNAL_STORAGE`: For file operations  
- `MANAGE_EXTERNAL_STORAGE`: For Android 11+ storage access
- **Root Access**: Required for system directory access

### Security Considerations
- All root operations are performed through controlled utility functions
- File paths are validated before root operations
- Error handling prevents unauthorized file access
- No sensitive data is logged or exposed

## Development Notes

### Root Development
- Use `RootUtils.isRootAvailable()` to check root status
- All file operations in `/data/adb/modules/` require root access
- Test root functionality on physical devices with root access
- Use `GlobalExceptionHandler.createCoroutineExceptionHandler(tag)` for all coroutines

### UI Development
- Use `@Preview` annotations for component previews
- Follow Material 3 design guidelines
- Support both light and dark themes
- Ensure proper error state handling

### Error Handling Guidelines
- **Mandatory Global Handling**: All coroutines MUST use `GlobalExceptionHandler.createCoroutineExceptionHandler(tag)`
- **Manual Logging**: Use `GlobalExceptionHandler.logException(tag, message, throwable)` for manual exception logging
- **No Individual Try-Catch**: Avoid try-catch blocks in favor of global exception handling architecture
- **Centralized Management**: Exception handling is centralized in `GlobalExceptionHandler` and `ErrorLogger`
- **Automatic Context**: All exceptions are automatically logged with device and application context information
- **Tag Consistency**: Use consistent tags like "ConfigLoad", "RootCheck", "PathChange", "RefreshRoot", "RefreshModule" for different operations
- **Application Initialization**: Ensure `ThreadUIApplication` is properly registered in AndroidManifest.xml
- **Coroutine Scope**: All `scope.launch` calls must include the global exception handler parameter

### Configuration Management
- Use `ConfigManager` for all configuration path operations
- Always use dynamic paths instead of hardcoded `/data/adb/modules/AppOpt/`
- Validate custom paths using `ConfigManager.validateConfigPath()`
- Check module existence with `RootUtils.checkModuleByConfigPath()`

### UI Component Development
- **Floating Scrollbar**: Use `FloatingScrollbar` composable with alphabet indexing
- **Settings Dialog**: Implement `SettingsDialog` for configuration path management
- **About Dialog**: Enhanced `AboutDialog` with QQ group integration and hidden log access
- **Module Status Cards**: Use contextual action buttons based on application state
- **Search Functionality**: Implement real-time search with highlighting using `HighlightedText`

### User Experience Guidelines
- **Hidden Features**: Implement hidden debug features via consecutive clicks (5x pattern)
- **Contextual Menus**: Show simplified menus based on application state (root/module status)
- **Visual Feedback**: Provide clear progress indicators and click count feedback
- **Path Validation**: Real-time validation feedback in settings dialogs
- **Auto-refresh**: Automatic state refresh after configuration changes

### Configuration Format
The app parses configuration files in this format:
```
#Game Name
package.name{ThreadName}=cpu-cores
package.name=cpu-cores
```

### Testing Strategy
- Unit tests for configuration parsing logic
- UI tests for component interactions
- Integration tests for root operations (requires root device)

### Build Configurations
- **Debug**: Standard debug build with UI tooling enabled
- **Release**: Optimized build with ProGuard disabled (can be enabled in `app/build.gradle.kts`)

### Dependencies Management
Dependencies are centralized in `gradle/libs.versions.toml` using Gradle version catalogs. When adding new dependencies, update the version catalog rather than hardcoding versions in build files.

### Key Dependencies
- **Compose BOM**: 2024.09.00
- **Activity Compose**: 1.9.2
- **Material 3**: Included in Compose BOM
- **Core KTX**: AndroidX core extensions
- **Lifecycle**: AndroidX lifecycle components

### Java Version
The project uses Java 11 for compilation. Ensure `sourceCompatibility` and `targetCompatibility` are set to `JavaVersion.VERSION_11` in build configurations.

## Known Issues and Fixes

### Share Functionality Package Name Issue (FIXED)
**Issue**: ShareUtils.kt contained incorrect FileProvider authority causing share functionality to fail
**Location**: `app/src/main/java/com/reddoctor/threadui/utils/ShareUtils.kt:15`
**Problem**: Used `com.reddoctor.treadui.fileprovider` instead of `com.reddoctor.threadui.fileprovider`
**Fix**: Corrected package name to match AndroidManifest.xml FileProvider declaration
**Impact**: Share feature now works correctly for exporting game configurations

### Critical Package Naming
**Important**: Always ensure consistency between:
- Package declarations in `build.gradle.kts`
- AndroidManifest.xml authorities and package names  
- FileProvider authorities in ShareUtils.kt
- Test package names in test files

The project was renamed from `treadui` to `threadui` - ensure all references use the correct `threadui` spelling.

## Troubleshooting

### Share Feature Issues
- **Problem**: Share button doesn't work or app crashes when sharing
- **Check**: Verify FileProvider authority in ShareUtils.kt matches AndroidManifest.xml
- **Solution**: Ensure `FILE_PROVIDER_AUTHORITY = "com.reddoctor.threadui.fileprovider"`

### Root Access Issues  
- **Problem**: Configuration file operations fail
- **Check**: Device root status with `RootUtils.isRootAvailable()`
- **Solution**: Ensure device is properly rooted and app has root permissions

### Import Security
- **Feature**: All imported configurations are scanned for malicious scripts
- **Location**: `ImportUtils.detectFormatScript()` provides security validation
- **Safe**: The app blocks potentially dangerous script injections in configuration files