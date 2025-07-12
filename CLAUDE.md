# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

TreadUI is an Android application built with Kotlin and Jetpack Compose that serves as a configuration manager for AppList configurations. The app allows users to view and edit game optimization settings stored in `/data/adb/modules/AppOpt/applist.conf` with root privileges.

**Package**: `com.reddoctor.threadui`
**Min SDK**: 35 (Android 14)
**Target SDK**: 36
**Compile SDK**: 36

## Core Functionality

The application provides the following features:
- **Root Permission Management**: Detects and manages root access for system file operations
- **Configuration File Parsing**: Reads and parses applist.conf files containing game thread configurations
- **Visual Configuration Editor**: Card-based UI for viewing and editing game settings
- **Intelligent Search**: Real-time search functionality with highlighting and quick search tags
- **Real-time Editing**: Add, modify, and delete thread configurations for games
- **System File Operations**: Read from and write to protected system directories
- **Configuration Sharing**: Export and import game configurations via JSON files with secure validation
- **Batch Operations**: Support for bulk export/import of multiple game configurations

## Search Functionality

The app includes comprehensive search capabilities:
- **Real-time Search**: Filter games by name or package name with instant results
- **Search Highlighting**: Visual highlighting of search terms in results
- **Quick Search Tags**: Pre-defined tags for common developers and game types
- **Search State Management**: Toggle between normal and search modes
- **Empty State Handling**: User-friendly messages when no results are found

## Architecture

### Project Structure
- **Main Application**: `app/src/main/java/com/reddoctor/threadui/MainActivity.kt`
- **Data Models**: `app/src/main/java/com/reddoctor/threadui/data/GameConfig.kt`
- **Share Configuration**: `app/src/main/java/com/reddoctor/threadui/data/ShareConfig.kt`
- **Root Utilities**: `app/src/main/java/com/reddoctor/threadui/utils/RootUtils.kt`
- **Share Utilities**: `app/src/main/java/com/reddoctor/threadui/utils/ShareUtils.kt`
- **Import Utilities**: `app/src/main/java/com/reddoctor/threadui/utils/ImportUtils.kt`
- **Permission Utilities**: `app/src/main/java/com/reddoctor/threadui/utils/PermissionUtils.kt`
- **UI Components**: `app/src/main/java/com/reddoctor/threadui/ui/components/`
- **UI Theme**: `app/src/main/java/com/reddoctor/threadui/ui/theme/`
- **Unit Tests**: `app/src/test/java/com/reddoctor/threadui/`
- **Instrumented Tests**: `app/src/androidTest/java/com/reddoctor/threadui/`

### Key Technologies
- **Jetpack Compose**: Modern declarative UI toolkit
- **Material 3**: Design system with dynamic color support
- **Kotlin**: Primary programming language
- **AndroidX**: Core Android libraries
- **Root Access**: System-level file operations using shell commands

### Data Models
- **GameConfig**: Represents a game with its package name and thread configurations
- **ThreadConfig**: Represents individual thread-to-CPU-core mappings
- **AppListConfig**: Container for all game configurations with parsing utilities
- **ShareConfig**: Handles export/import data format with security validation
- **AppInfo**: Contains application information for sharing features

### Root Operations
The app uses `RootUtils` to perform system-level operations:
- Root permission detection
- File reading from protected directories
- File writing to protected directories
- Command execution with root privileges

### Sharing and Import System
The app includes a comprehensive sharing system:
- **Individual Game Sharing**: Export single game configurations via ShareUtils
- **Batch Export**: Export multiple games in one operation
- **Secure Import**: Import configurations with malicious script detection
- **FileProvider Integration**: Uses Android FileProvider for secure file sharing
- **JSON Format**: All exports use structured JSON format via ShareConfig
- **Security Validation**: All imports are validated against malicious content

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

### UI Development
- Use `@Preview` annotations for component previews
- Follow Material 3 design guidelines
- Support both light and dark themes
- Ensure proper error state handling

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