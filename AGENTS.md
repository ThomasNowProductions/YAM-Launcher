# AGENTS.md - YAM Launcher

Guidelines for agentic coding agents operating in this repository.

## Project Overview

YAM Launcher is a minimalist text-based Android launcher with weather integration, built with Kotlin and modern Android practices.

- **Package**: `eu.ottop.yamlauncher`
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 36
- **Language**: Kotlin 2.2.10
- **JVM Target**: Java 17

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install debug on connected device
./gradlew installDebug

# Clean build
./gradlew clean

# Build all variants
./gradlew build
```

## Lint Commands

```bash
# Run lint on default variant
./gradlew lint

# Run lint on debug build
./gradlew lintDebug

# Run lint on release build
./gradlew lintRelease

# Run lint with auto-fix for safe suggestions
./gradlew lintFix
```

## Test Commands

```bash
# Run unit tests for all variants
./gradlew test

# Run unit tests for debug build
./gradlew testDebugUnitTest

# Run instrumentation tests (requires connected device)
./gradlew connectedAndroidTest

# Run instrumentation tests for debug build
./gradlew connectedDebugAndroidTest
```

Note: The project currently has no test files. Tests should be placed in:
- Unit tests: `app/src/test/java/eu/ottop/yamlauncher/`
- Instrumentation tests: `app/src/androidTest/java/eu/ottop/yamlauncher/`

## Code Style Guidelines

### No Comments

Do NOT add any comments in code unless explicitly requested by the user.

### Import Order

Organize imports in this order:
1. Android framework imports (`android.*`)
2. AndroidX imports (`androidx.*`)
3. Third-party libraries (`com.google.*`, `kotlinx.*`, `java.*`)
4. Project-specific imports (`eu.ottop.yamlauncher.*`)

Within each group, organize alphabetically.

### Naming Conventions

- **Classes**: PascalCase (e.g., `MainActivity`, `SharedPreferenceManager`)
- **Variables/Methods**: camelCase (e.g., `weatherSystem`, `getInstalledApps`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `MAX_LOG_FILE_SIZE`, `LOG_FILE_NAME`)
- **XML IDs**: camelCase (e.g., `@+id/appRecycler`)
- **String resources**: snake_case (e.g., `settings_title`, `launch_error`)
- **Fragments**: Suffix with `Fragment` (e.g., `SettingsFragment`)
- **Activities**: Suffix with `Activity` (e.g., `SettingsActivity`)
- **Adapters**: Suffix with `Adapter` (e.g., `AppMenuAdapter`)
- **Utility classes**: Suffix with `Utils` (e.g., `UIUtils`, `AppUtils`)
- **Managers**: Suffix with `Manager` (e.g., `SharedPreferenceManager`)
- **Listeners/Receivers**: Suffix with `Listener` or `Receiver` (e.g., `NotificationListener`, `BatteryReceiver`)

### Kotlin Style

- Use `lateinit` for properties initialized in `onCreate` or similar lifecycle methods
- Use `private` modifier for class members by default
- Use `val` over `var` when possible
- Prefer string templates over concatenation: `"Text $variable"` instead of `"Text " + variable`
- Use scope functions (`apply`, `let`, `also`, `run`, `with`) appropriately
- Use coroutines for async operations with appropriate dispatchers:
  - `Dispatchers.Main` for UI operations
  - `Dispatchers.Default` for CPU-intensive work
  - `Dispatchers.IO` for I/O operations

### Error Handling

- Use try-catch blocks for operations that may fail
- Use underscore (`_`) for unused exception variables: `catch (_: Exception)`
- Use the centralized `Logger` utility for logging errors
- Provide graceful fallbacks for failed operations

Example:
```kotlin
try {
    launcherApps.startMainActivity(componentName, userHandle, null, null)
    logger.i("Tag", "Launched app: ${componentName.packageName}")
} catch (e: Exception) {
    logger.e("Tag", "Failed to launch app", e)
    throw e
}
```

### Logging

Use the centralized `Logger` singleton:
```kotlin
private val logger = Logger.getInstance(context)

logger.d("Tag", "Debug message")
logger.i("Tag", "Info message")
logger.w("Tag", "Warning message")
logger.e("Tag", "Error message", throwable)
```

### View Binding

The project uses View Binding:
```kotlin
private lateinit var binding: ActivityMainBinding

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)
}
```

### SharedPreferences

Use `SharedPreferenceManager` for all preference access:
```kotlin
private val sharedPreferenceManager = SharedPreferenceManager(context)

// Getting values
val isClockEnabled = sharedPreferenceManager.isClockEnabled()
val bgColor = sharedPreferenceManager.getBgColor()

// Setting values (use kotlin extension)
preferences.edit {
    putString("key", "value")
}
```

### RecyclerView Patterns

Use DiffUtil for efficient updates:
```kotlin
class AppDiffCallback(
    private val oldList: List<App>,
    private val newList: List<App>
) : DiffUtil.Callback() {
    override fun getOldListSize() = oldList.size
    override fun getNewListSize() = newList.size
    override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
        return oldList[oldPos].id == newList[newPos].id
    }
    override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
        return oldList[oldPos] == newList[newPos]
    }
}
```

### Fragments

Fragments extend `PreferenceFragmentCompat` for settings or standard Fragment:
```kotlin
class SettingsFragment : PreferenceFragmentCompat(), TitleProvider {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }
}
```

### Interfaces for Callbacks

Define interfaces in the same file as the class using them:
```kotlin
class AppMenuAdapter : RecyclerView.Adapter<AppMenuAdapter.AppViewHolder>() {
    interface OnItemClickListener {
        fun onItemClick(appInfo: LauncherActivityInfo, userHandle: UserHandle)
    }
}
```

### Dependency Management

Dependencies are managed via version catalog in `gradle/libs.versions.toml`:
```kotlin
// In build.gradle.kts
implementation(libs.core.ktx)
implementation(libs.appcompat)

// In libs.versions.toml
[versions]
core-ktx = "1.16.0"

[libraries]
core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "core-ktx" }
```

### Build Variants

- **Debug**: `.dev` applicationId suffix, `-dev` versionName suffix
- **Release**: Minified with ProGuard, shrinkResources enabled

### Resources

- String resources in `res/values/strings.xml`
- Layouts in `res/layout/`
- Preference XMLs in `res/xml/`
- Support RTL layouts with `res/layout-land/` for landscape

### Biometric Authentication

Use `BiometricUtils` for authentication flows:
```kotlin
biometricUtils.startBiometricSettingsAuth(object : BiometricUtils.CallbackSettings {
    override fun onAuthenticationSucceeded() {
        // Handle success
    }
    override fun onAuthenticationFailed() {
        // Handle failure
    }
    override fun onAuthenticationError(errorCode: Int, errorMessage: CharSequence?) {
        // Handle error
    }
})
```

## Key Architecture Patterns

1. **Singleton**: `Logger` uses companion object with double-checked locking
2. **Utility Classes**: Stateless utility classes with Context passed to constructor
3. **Manager Pattern**: `SharedPreferenceManager` encapsulates all preference logic
4. **Fragment Navigation**: Uses `supportFragmentManager` for fragment transactions
5. **Coroutines + Lifecycle**: Uses `lifecycleScope` and `repeatOnLifecycle` for lifecycle-aware coroutines
