# DutyPe App Size Optimization Strategy

## Current Status
- **New Install Size**: 22.9 MB (-263 KB vs previous)
- **Update Size**: 18.7 MB  
- **Target**: 15 MB for new installs, <12 MB for updates

## Size Analysis Breakdown

### 1. **Method Count & Bytecode** (~8-9 MB)
- Jetpack Compose overhead: ~4 MB
- Firebase SDK (Auth, Firestore, Messaging, In-App Review): ~3 MB
- Google Play Services: ~1-2 MB

**Optimization Actions:**
```gradle
// 1. Enable More Aggressive Shrinking in build.gradle.kts
buildTypes {
    release {
        minifyEnabled = true
        shrinkResources = true  // Enable resource shrinking
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}

// 2. Add these ProGuard rules to proguard-rules.pro
# Aggressive Firebase cleanup
-dontwarn com.google.firebase.**
-keep class com.google.firebase.** { *; }
-keep interface com.google.firebase.** { *; }

# Remove unused Firebase modules
-assumenosideeffects class com.google.firebase.crashlytics.** { *; }
-assumenosideeffects class com.google.firebase.analytics.** { *; }

# Compose optimization
-keep class androidx.compose.** { *; }
-keepclasseswithmembernames class androidx.compose.** { *; }

# Remove logging when not debugging
-assumenosideeffects class timber.log.Timber {
    public static void d(...);
    public static void v(...);
    public static void i(...);
}
```

### 2. **Drawable & Image Assets** (~3-4 MB)  
**Current Issue**: Multiple PNG/JPG files at high DPI

**Optimization Actions:**
```bash
# 1. Convert PNG to WebP (saves 25-30% size)
   All PNG files in app/src/main/res/drawable* → WebP format
   Tools: Command line: `cwebp image.png -o image.webp`

# 2. Remove unused resources
   Run: ./gradlew cleanBuildCache --info | grep "unused"
   Remove drawables not referenced in code

# 3. Optimize vector drawables
   Reduce path points in SVG files
   Keep original SVG in assets, not drawable

# 4. Enable resource compression in build.gradle.kts
android {
    packagingOptions {
        compress 'assets/fonts'
        compress 'assets/images'
    }
}
```

### 3. **Native Libraries (ARM64, ARMv7, x86, x86_64)** (~2-3 MB)
**Current Setup**: Supporting 4 architectures for broad device support

**Optimization Actions:**
```gradle
// Option A: RECOMMENDED - Split APK by architecture
    splits {
        abi {
            isEnable = true
            reset()
            // For Play Store: Include most common architectures
            include("arm64-v8a", "armeabi-v7a")  // Covers 99% of devices
            // Exclude: "x86", "x86_64" - only 0.2% device usage
            isUniversalApk = true  // Fallback universal APK
        }
    }

// Option B: Dynamic Feature Modules - Download native libs on demand
// Create dynamic-features/ folder for optional ABIs

// Option C: Reduce native lib sizes
// - Use -O2 optimization in CMakeLists.txt
// - Strip debug symbols: android.useDeprecatedNdk = false
// - Link statically when possible
```

### 4. **Jetpack Compose Overhead** (~4 MB)
**Current Issue**: Full Compose dependency tree for all screens

**Optimization Actions:**
```kotlin
// 1. Use only needed Compose libraries
// REMOVE unused Material Design 2 imports
// -implementation("androidx.compose.material:material") // 400KB
// Already using Material 3, keep:
implementation("androidx.compose.material3:material3")

// 2. Remove unused Jetpack extensions
// Check imports for unused: foundation, animation, animation-core
// Only import what's used in each file

// 3. Enable Compose Compiler optimizations
android {
    kotlinOptions {
        freeCompilerArgs += listOf(
            "plugin:androidx.compose.compiler.plugins.kotlin:featureFlag=StrongSkipping",
            "plugin:androidx.compose.compiler.plugins.kotlin:featureFlag=IntrinsicRemembering"
        )
    }
}
```

### 5. **Language Support Files** (~1-2 MB)
**Current Setup**: Supporting 3 languages (en-GB, hi-IN, te-IN)

**Optimization Actions:**
```gradle
// 1. Use Play Console to split by language
// This automatically generates language-specific APKs
android {
    bundle {
        language {
            enableSplit = true  // Split ~200KB per language
        }
    }
}

// 2. Or compress string resources
// Inlined strings don't need resources.arsc in some cases
```

## Recommended Optimization Roadmap

### Phase 1: Quick Wins (Save ~2-3 MB)
```gradle
// build.gradle.kts
shrinkResources = true
minifyEnabled = true

// proguard-rules.pro - Add aggressive rules above
```

### Phase 2: Medium Effort (Save ~2-4 MB)
```bash
# Convert all PNG → WebP
# Remove 4-architecture split → 2-architecture split
# Reduce unused drawable resources
```

### Phase 3: Long Term (Save ~1-2 MB)
```kotlin
// Remove Compose for non-Compose screens
// Use lazy loading for feature modules
// Dynamic feature delivery for advanced features
```

## Target Next Steps

1. **THIS WEEK**:
   - Enable shrinkResources and minifyEnabled in release builds
   - Add ProGuard rules for Firebase modules
   - Convert PNG images to WebP

2. **NEXT WEEK**:
   - Switch to 2-architecture split (arm64-v8a + armeabi-v7a)
   - Language split configuration
   - Remove unused drawable resources

3. **ONGOING**:
   - Monitor build size after each release
   - Add size tracking to CI/CD pipeline
   - Review dependencies quarterly

## Expected Results

| Phase | New Install | Update | Savings |
|-------|-------------|--------|---------|
| Current | 22.9 MB | 18.7 MB | - |
| After Phase 1 | ~20 MB | ~17 MB | 2-3 MB |
| After Phase 2 | ~17 MB | ~14 MB | 5-6 MB |
| Final Target | ~15 MB | ~12 MB | 7-8 MB |

## Build Size Monitoring Script

```bash
#!/bin/bash
# Size check after build

echo "📊 APK Size Report"
APK_PATH="app/build/outputs/bundle/release/*.aab"

if [ -f "$APK_PATH" ]; then
    SIZE_MB=$(stat -f%z "$APK_PATH" | awk '{print $1/1024/1024}')
    echo "Bundle Size: ${SIZE_MB}MB"
    
    if (( $(echo "$SIZE_MB > 22" | bc -l) )); then
        echo "⚠️ WARNING: Size exceeds 22 MB!"
    fi
fi
```

## Dependencies Review

### Can Remove (No Usage):
- ~~`material`~~ (using Material3)
- ~~Unused Firebase modules~~ (Analytics, Crashlytics if not used)

### Can Downgrade:
- Keep Play Core at latest stable
- Update Firebase BOM to latest

### Must Keep:
- Jetpack Compose (core UI framework)
- Firebase Auth, Firestore, Messaging (core functionality)
- Hilt (dependency injection)
- WorkManager (background jobs)
