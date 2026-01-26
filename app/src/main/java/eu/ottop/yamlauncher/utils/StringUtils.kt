package eu.ottop.yamlauncher.utils

import android.text.Html
import android.text.method.LinkMovementMethod
import android.widget.TextView
import java.util.LinkedHashMap

class StringUtils {
    companion object {
        // Cached regex for cleaning strings (compiled once)
        private val CLEAN_REGEX = Regex("[^\\p{L}0-9]")

        // LRU cache for fuzzy patterns (max 16 entries) - thread-safe access
        private val fuzzyPatternCache = object : LinkedHashMap<String, Regex>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Regex>?): Boolean {
                return size > 16
            }
        }
    }

    fun addEndTextIfNotEmpty(value: String, addition: String): String {
        return if (value.isNotEmpty()) "$value$addition" else value
    }

    fun addStartTextIfNotEmpty(value: String, addition: String): String {
        return if (value.isNotEmpty()) "$addition$value" else value
    }

    fun cleanString(string: String?): String? {
        return string?.replace(CLEAN_REGEX, "")
    }

    fun setLink(view: TextView, link: String) {
        view.text = Html.fromHtml(link, Html.FROM_HTML_MODE_LEGACY)
        view.movementMethod = LinkMovementMethod.getInstance()
    }

    /** Create a Regex pattern for simple Fuzzy search
     *
     * Example:
     * 'cl' will create 'c.*l' which matches 'Clock', 'Calendar'
     * 'msg' will create 'm.*s.*g' which matches 'Messages'
     * 'cmr' will create 'c.*m.*r' which matches 'Camera'
     *
     * Thread-safe: synchronized access to cache
     */
    fun getFuzzyPattern(query: String): Regex {
        synchronized(fuzzyPatternCache) {
            return fuzzyPatternCache.getOrPut(query) {
                val regex = query.toCharArray().joinToString(".*") {
                    Regex.escape(it.toString())
                }
                Regex(regex, RegexOption.IGNORE_CASE)
            }
        }
    }
}
