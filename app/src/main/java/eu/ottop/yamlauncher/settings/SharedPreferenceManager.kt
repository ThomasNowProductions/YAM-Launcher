package eu.ottop.yamlauncher.settings

import android.content.Context
import android.content.SharedPreferences
import android.util.TypedValue
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.ottop.yamlauncher.R
import eu.ottop.yamlauncher.utils.Logger

private inline fun SharedPreferences.getStringAsInt(key: String, default: Int): Int {
    return getString(key, default.toString())?.toInt() ?: default
}

private inline fun SharedPreferences.getStringAsLong(key: String, default: Long): Long {
    return getString(key, default.toString())?.toLong() ?: default
}

private inline fun SharedPreferences.getStringAsFloat(key: String, default: Float): Float {
    return getString(key, default.toString())?.toFloat() ?: default
}

private inline fun SharedPreferences.getBooleanOrDefault(key: String, default: Boolean): Boolean {
    return getBoolean(key, default)
}

class SharedPreferenceManager(private val context: Context) {

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val logger = Logger.getInstance(context)

    // General UI
    fun getBgColor(): Int {
        val bgColor = preferences.getString("bgColor", "#00000000")
        if (bgColor == "material") {
            return getThemeColor(com.google.android.material.R.attr.colorOnPrimary)
        }
        return try {
            bgColor?.toColorInt() ?: 0x00000000.toInt()
        } catch (e: Exception) {
            logger.e("SharedPreferenceManager", "Error parsing bgColor: $bgColor", e)
            0x00000000.toInt()
        }
    }

    fun getTextColor(): Int {
        val textColor = getTextString()
        if (textColor == "material") {
            return getThemeColor(com.google.android.material.R.attr.colorPrimary)
        }
        return try {
            textColor?.toColorInt() ?: 0xFFF3F3F3.toInt()
        } catch (e: Exception) {
            logger.e("SharedPreferenceManager", "Error parsing textColor: $textColor", e)
            0xFFF3F3F3.toInt()
        }
    }

    fun getTextString(): String? {
        return preferences.getString("textColor", "#FFF3F3F3")
    }

    private fun getThemeColor(attr: Int): Int {
        val typedValue = TypedValue()
        val theme = context.theme
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    fun getTextFont(): String? {
        return preferences.getString("textFont", "system")
    }

    fun getTextStyle(): String? {
        return preferences.getString("textStyle", "normal")
    }

    fun isTextShadowEnabled(): Boolean = preferences.getBooleanOrDefault("textShadow", false)

    fun isBarVisible(): Boolean = preferences.getBooleanOrDefault("barVisibility", false)

    fun getAnimationSpeed(): Long {
        return preferences.getStringAsLong("animationSpeed", 200)
    }

    fun getSwipeThreshold(): Int {
        return preferences.getStringAsInt("swipeThreshold", 100)
    }

    fun getSwipeVelocity(): Int {
        return preferences.getStringAsInt("swipeVelocity", 100)
    }

    fun isConfirmationEnabled(): Boolean = preferences.getBooleanOrDefault("enableConfirmation", false)

    fun isSettingsLocked(): Boolean = preferences.getBooleanOrDefault("lockSettings", false)

    fun isClockEnabled(): Boolean = preferences.getBooleanOrDefault("clockEnabled", true)

    fun getClockAlignment(): String? {
        return preferences.getString("clockAlignment", "left")
    }

    fun getClockSize(): String? {
        return preferences.getString("clockSize", "medium")
    }

    fun isDateEnabled(): Boolean = preferences.getBooleanOrDefault("dateEnabled", true)

    fun getDateSize(): String? {
        return preferences.getString("dateSize", "medium")
    }

    fun setShortcut(index: Int, text: CharSequence, componentName: String, profile: Int, isContact: Boolean = false) {
        preferences.edit {
            putString("shortcut${index}", "$componentName§splitter§$profile§splitter§${text}§splitter§${isContact}")
        }
    }

    fun getShortcut(index: Int): List<String>? {
        val value = preferences.getString("shortcut${index}", "e§splitter§e§splitter§e§splitter§e")
        return value?.split("§splitter§")
    }

    fun getShortcutNumber(): Int {
        return preferences.getStringAsInt("shortcutNo", 4)
    }

    fun getShortcutAlignment(): String? {
        return preferences.getString("shortcutAlignment", "left")
    }

    fun getShortcutVAlignment(): String? {
        return preferences.getString("shortcutVAlignment", "center")
    }

    fun getShortcutSize(): String? {
        return preferences.getString("shortcutSize", "medium")
    }

    fun getShortcutWeight(): Float {
        return preferences.getStringAsFloat("shortcutWeight", 0.09f)
    }

    fun areShortcutsLocked(): Boolean = preferences.getBooleanOrDefault("lockShortcuts", false)

    fun isNotificationDotsEnabled(): Boolean = preferences.getBooleanOrDefault("notificationDots", false)

    fun showHiddenShortcuts(): Boolean = preferences.getBooleanOrDefault("showHiddenShortcuts", true)

    fun setPinnedApp(componentName: String, profile: Int) {
        preferences.edit {

            val pinnedAppString = when (isAppPinned(componentName, profile)) {
                true -> {
                    getPinnedAppString()?.replace("§section§$componentName§splitter§$profile", "")
                }

                false -> {
                    "${getPinnedAppString()}§section§$componentName§splitter§$profile"
                }
            }

            putString(
                "pinnedApps",
                pinnedAppString
            )
        }
    }

    private fun getPinnedAppString(): String? {
        return preferences.getString("pinnedApps", "")
    }

    private fun getPinnedApps(): List<Pair<String, Int?>> {
        val pinnedApps = mutableListOf<Pair<String, Int?>>()
        val pinnedAppString = getPinnedAppString() ?: return pinnedApps
        val pinnedAppList = pinnedAppString.split("§section§")

        for (item in pinnedAppList) {
            if (item.isBlank()) continue
            val app = item.split("§splitter§")
            if (app.size > 1) {
                val profile = app.getOrNull(1)?.toIntOrNull()
                pinnedApps.add(Pair(app[0], profile))
            }
        }

        return pinnedApps
    }

    fun isAppPinned(componentName: String, profile: Int): Boolean {
        return getPinnedApps().contains(Pair(componentName, profile))
    }

    fun isBatteryEnabled(): Boolean = preferences.getBooleanOrDefault("batteryEnabled", false)

    // Weather
    fun isWeatherEnabled(): Boolean = preferences.getBooleanOrDefault("weatherEnabled", false)

    fun isWeatherGPS(): Boolean = preferences.getBooleanOrDefault("gpsLocation", false)

    fun setWeatherGPS(isEnabled: Boolean) {
        preferences.edit {
            putBoolean("gpsLocation", isEnabled)
        }
    }

    fun setWeatherLocation(location: String, region: String?) {
        preferences.edit {
            putString("location", location)
            putString("locationRegion", region)
        }
    }

    fun getWeatherLocation(): String? {
        return preferences.getString("location", "")
    }

    fun getWeatherRegion(): String? {
        return preferences.getString("locationRegion", "")
    }

    fun getTempUnits(): String? {
        return preferences.getString("tempUnits", "celsius")
    }

    fun getWeatherUpdateIntervalMs(): Long {
        val defaultMs = 15 * 60_000L
        val raw = preferences.getString("weatherUpdateInterval", "15m")
        val ms = parseUpdateIntervalMs(raw, defaultMs)
        return ms.coerceAtLeast(60_000L)
    }

    private fun parseUpdateIntervalMs(raw: String?, defaultMs: Long): Long {
        val s = raw?.trim()?.lowercase().orEmpty()
        if (s.isEmpty()) return defaultMs

        // Convenience: treat bare numbers as minutes (e.g. "15" == "15m"); use m/h/d to specify other units.
        if (s.all { it.isDigit() }) {
            val minutes = s.toLongOrNull() ?: return defaultMs
            if (minutes <= 0L) return defaultMs
            return minutes * 60_000L
        }

        val match = Regex("^(\\d+)\\s*([mhd])$").find(s) ?: return defaultMs
        val value = match.groupValues[1].toLongOrNull() ?: return defaultMs
        if (value <= 0L) return defaultMs

        val multiplier = when (match.groupValues[2]) {
            "m" -> 60_000L
            "h" -> 60 * 60_000L
            "d" -> 24 * 60 * 60_000L
            else -> return defaultMs
        }

        return try {
            Math.multiplyExact(value, multiplier)
        } catch (_: ArithmeticException) {
            Long.MAX_VALUE
        }
    }

    fun isClockGestureEnabled(): Boolean = preferences.getBooleanOrDefault("clockClick", true)

    fun isDateGestureEnabled(): Boolean = preferences.getBooleanOrDefault("dateClick", true)

    // Gestures
    fun setGestures(direction: String, appInfo: String?) {
        preferences.edit {
            putString("${direction}SwipeApp", appInfo)
        }
    }

    fun getGestureName(direction: String): String? {
        val name = preferences.getString("${direction}SwipeApp", "")?.split("§splitter§")
        return name?.get(0)
    }

    fun getGestureInfo(direction: String): List<String>? {
        return preferences.getString("${direction}SwipeApp", "")?.split("§splitter§")
    }

    fun isGestureEnabled(direction: String): Boolean = preferences.getBooleanOrDefault("${direction}Swipe", false)

    fun isDoubleTapEnabled(): Boolean = preferences.getBooleanOrDefault("doubleTap", false)

    fun getDoubleTapAction(): String {
        val action = preferences.getString("doubleTapAction", null)
        if (action != null) {
            return action
        }

        val migratedAction = if (preferences.getBooleanOrDefault("doubleTapSwipe", false)) "app" else "lock"
        preferences.edit {
            putString("doubleTapAction", migratedAction)
            remove("doubleTapSwipe")
        }
        return migratedAction
    }

    // Application Menu
    fun getAppAlignment(): String? {
        return preferences.getString("appMenuAlignment", "left")
    }

    fun getAppSize(): String? {
        return preferences.getString("appMenuSize", "medium")
    }

    fun isPinEnabled(): Boolean = preferences.getBooleanOrDefault("pinEnabled", true)

    fun isInfoEnabled(): Boolean = preferences.getBooleanOrDefault("infoEnabled", true)

    fun isUninstallEnabled(): Boolean = preferences.getBooleanOrDefault("uninstallEnabled", true)

    fun isRenameEnabled(): Boolean = preferences.getBooleanOrDefault("renameEnabled", true)

    fun isHideEnabled(): Boolean = preferences.getBooleanOrDefault("hideEnabled", true)

    fun isCloseEnabled(): Boolean = preferences.getBooleanOrDefault("closeEnabled", true)

    fun isSearchEnabled(): Boolean = preferences.getBooleanOrDefault("searchEnabled", true)

    fun getSearchAlignment(): String? {
        return preferences.getString("searchAlignment", "left")
    }

    fun getSearchSize(): String? {
        return preferences.getString("searchSize", "medium")
    }

    fun isFuzzySearchEnabled(): Boolean = preferences.getBooleanOrDefault("fuzzySearchEnabled", false)

    fun getAppSpacing(): Int {
        return preferences.getStringAsInt("appSpacing", 20)
    }

    fun isAutoKeyboardEnabled(): Boolean = preferences.getBooleanOrDefault("autoKeyboard", false)

    fun isAutoLaunchEnabled(): Boolean = preferences.getBooleanOrDefault("autoLaunch", false)

    fun areContactsEnabled(): Boolean = preferences.getBooleanOrDefault("contactsEnabled", false)

    fun setContactsEnabled(isEnabled: Boolean) {
        preferences.edit {
            putBoolean("contactsEnabled", isEnabled)
        }
    }

    fun isWebSearchEnabled(): Boolean {
        return preferences.getBooleanOrDefault("webSearchEnabled", false) && isSearchEnabled() && !isAutoLaunchEnabled()
    }

    fun isAlphabetIndexEnabled(): Boolean = preferences.getBooleanOrDefault("alphabetIndexEnabled", false)

    fun getAlphabetIndexPosition(): String? {
        return preferences.getString("alphabetIndexPosition", "right")
    }

    // Hidden Apps
    fun setAppHidden(componentName: String, profile: Int, hidden: Boolean) {
        preferences.edit {
            putBoolean("hidden$componentName-$profile", hidden)
        }
    }

    fun isAppHidden(componentName: String, profile: Int): Boolean {
        return preferences.getBoolean("hidden$componentName-$profile", false) // Default to false (visible)
    }

    fun setAppVisible(componentName: String, profile: Int) {
        preferences.edit {
            remove("hidden$componentName-$profile")
        }
    }

    //Renaming apps
    fun setAppName(componentName: String, profile: Int, newName: String) {
        preferences.edit {
            putString("name$componentName-$profile", newName)
        }
    }

    fun getAppName(componentName: String, profile: Int, appName: CharSequence): CharSequence? {
        val key = "name$componentName-$profile"
        val savedName = preferences.getString(key, null)
        if (savedName.isNullOrBlank()) {
            preferences.edit(commit = true) {
                val latestValue = preferences.getString(key, null)
                if (latestValue.isNullOrBlank()) {
                    remove(key)
                }
            }
            return appName
        }

        val packageName = try {
            componentName.substringBefore("/")
        } catch (e: Exception) {
            logger.w("SharedPreferenceManager", "Error parsing component name: $componentName")
            componentName
        }
        if (savedName == packageName || savedName == componentName) {
            preferences.edit(commit = true) {
                val latestValue = preferences.getString(key, null)
                if (latestValue == packageName || latestValue == componentName) {
                    remove(key)
                }
            }
            return appName
        }

        return savedName
    }

    fun resetAppName(componentName: String, profile: Int) {
        preferences.edit {
            remove("name$componentName-$profile")
        }
    }

    fun resetAllPreferences() {
        MaterialAlertDialogBuilder(context).apply {
            setTitle(context.getString(R.string.confirm_title))
            setMessage(context.getString(R.string.reset_confirm_text))
            setPositiveButton(context.getString(R.string.confirm_yes)) { _, _ ->
                performReset()
            }

            setNegativeButton(context.getString(R.string.confirm_no)) { _, _ ->
            }
        }.create().show()
    }

    private fun performReset() {
        logger.i("SharedPreferenceManager", "Resetting all preferences")
        preferences.edit {
            clear()
            putBoolean("isRestored", true)
        }
    }
}
