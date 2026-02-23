package eu.ottop.yamlauncher.utils

import android.content.Context
import android.content.pm.LauncherActivityInfo
import java.util.concurrent.ConcurrentHashMap

object AppNameResolver {

    private val packageNamePattern = Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*\\.[A-Za-z][A-Za-z0-9_]*$")
    private val baseLabelCache = ConcurrentHashMap<String, String>()

    private fun isLikelyPackageName(value: String): Boolean {
        return packageNamePattern.matches(value)
    }

    // Safe to call from UI thread: quick string checks and an in-memory cache; PackageManager lookup is lazy and only used when needed.
    fun resolveBaseLabel(context: Context, appInfo: LauncherActivityInfo): String {
        val componentKey = appInfo.componentName.flattenToShortString()
        return baseLabelCache.getOrPut(componentKey) {
            val packageName = appInfo.applicationInfo.packageName
            val activityLabel = appInfo.label?.toString()?.trim().orEmpty()

            val activityLooksLikePackageName = activityLabel == packageName || isLikelyPackageName(activityLabel)
            if (activityLabel.isNotEmpty() && !activityLooksLikePackageName) {
                return@getOrPut activityLabel
            }

            val applicationLabel = appInfo.applicationInfo
                .loadLabel(context.packageManager)
                ?.toString()
                ?.trim()
                .orEmpty()
            val applicationLooksLikePackageName = applicationLabel == packageName || isLikelyPackageName(applicationLabel)

            when {
                applicationLabel.isNotEmpty() && !applicationLooksLikePackageName -> applicationLabel
                activityLabel.isNotEmpty() -> activityLabel
                applicationLabel.isNotEmpty() -> applicationLabel
                else -> packageName
            }
        }
    }
}
