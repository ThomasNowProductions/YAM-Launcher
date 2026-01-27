package eu.ottop.yamlauncher.utils

import android.content.Context
import android.content.res.Configuration
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewTreeObserver
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextClock
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.textfield.TextInputEditText
import eu.ottop.yamlauncher.R
import eu.ottop.yamlauncher.settings.SharedPreferenceManager

class UIUtils(private val context: Context) {

    private val sharedPreferenceManager = SharedPreferenceManager(context)

    fun adjustInsets(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val types = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            } else {
                WindowInsetsCompat.Type.systemBars()
            }
            val bars = insets.getInsets(types)
            v.updatePadding(
                left = bars.left,
                top = bars.top,
                right = bars.right,
                bottom = bars.bottom,
            )
            WindowInsetsCompat.CONSUMED
        }
    }

    // Replicate adjustResize
    fun setLayoutListener(view: View) {
        view.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            private var lastKeyboardState = false

            override fun onPreDraw(): Boolean {
                val rect = Rect()
                view.getWindowVisibleDisplayFrame(rect)

                val screenHeight = view.rootView.height
                val keyboardHeight = screenHeight - rect.bottom

                val isKeyboardVisible = keyboardHeight > screenHeight * 0.15

                if (isKeyboardVisible != lastKeyboardState) {
                    lastKeyboardState = isKeyboardVisible

                    if (isKeyboardVisible) {
                        val availableHeight = screenHeight - keyboardHeight
                        view.layoutParams.height = availableHeight
                    } else {
                        view.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                    }

                    view.requestLayout()
                }
                return true
            }
        })
    }

    // Colors
    fun setBackground(window: Window) {
        window.decorView.setBackgroundColor(
            sharedPreferenceManager.getBgColor()
        )
    }

    fun setImageColor(view: ImageView) {
        view.setColorFilter(sharedPreferenceManager.getTextColor())
    }

    fun setTextColors(view: View) {
        val color = sharedPreferenceManager.getTextColor()
        when {
            view is ViewGroup -> {
                view.children.forEach { child ->
                    setTextColors(child)
                }
            }
            hasMethod(view, "setTextColor") -> {
                val textView = view as TextView
                textView.setTextColor(color)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    textView.compoundDrawables[0]?.colorFilter =
                        BlendModeColorFilter(sharedPreferenceManager.getTextColor(), BlendMode.SRC_ATOP)
                    textView.compoundDrawables[2]?.colorFilter =
                        BlendModeColorFilter(sharedPreferenceManager.getTextColor(), BlendMode.SRC_ATOP)
                } else {
                    textView.compoundDrawables[0]?.colorFilter =
                        PorterDuffColorFilter(sharedPreferenceManager.getTextColor(), PorterDuff.Mode.SRC_ATOP)
                    textView.compoundDrawables[2]?.colorFilter =
                        PorterDuffColorFilter(sharedPreferenceManager.getTextColor(), PorterDuff.Mode.SRC_ATOP)
                }

                // Apply text shadow if enabled
                if (sharedPreferenceManager.isTextShadowEnabled()) {
                    textView.setShadowLayer(4f, 2f, 2f, Color.BLACK)
                } else {
                    textView.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
                }
            }
            else -> {
                view.setBackgroundColor(color)
            }
        }
    }

    fun setStatusBarColor(window: Window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val insetController = window.insetsController
            when (sharedPreferenceManager.getTextString()) {
                "#FFF3F3F3" -> insetController?.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
                "#FF0C0C0C" -> insetController?.setSystemBarsAppearance(WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
                "material" -> {
                    val currentNightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                    when (currentNightMode) {
                        Configuration.UI_MODE_NIGHT_YES -> insetController?.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
                        Configuration.UI_MODE_NIGHT_NO -> insetController?.setSystemBarsAppearance(WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
                    }
                }
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            @Suppress("DEPRECATION")
            val decorView = window.decorView
            @Suppress("DEPRECATION")
            var systemUiVisibility = decorView.systemUiVisibility
            when (sharedPreferenceManager.getTextString()) {
                "#FFF3F3F3" -> {
                    systemUiVisibility = systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                }
                "#FF0C0C0C" -> {
                    systemUiVisibility = systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                }
                "material" -> {
                    val currentNightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                    when (currentNightMode) {
                        Configuration.UI_MODE_NIGHT_YES -> {
                            systemUiVisibility = systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                        }
                        Configuration.UI_MODE_NIGHT_NO -> {
                            systemUiVisibility = systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                        }
                    }
                }
            }
            @Suppress("DEPRECATION")
            decorView.systemUiVisibility = systemUiVisibility
        }
    }

    private fun hasMethod(view: View, methodName: String): Boolean {
        return try {
            view.javaClass.getMethod(methodName, Int::class.java)
            true
        } catch (_: NoSuchMethodException) {
            false
        }
    }

    fun setMenuItemColors(view: TextView, alphaHex: String = "FF") {
        val color = sharedPreferenceManager.getTextColor()
        view.setTextColor(setAlpha(color, alphaHex))
        view.setHintTextColor(setAlpha(color, "A9"))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            view.compoundDrawables[0]?.mutate()?.colorFilter = BlendModeColorFilter(color, BlendMode.SRC_ATOP)
            view.compoundDrawables[2]?.mutate()?.colorFilter = BlendModeColorFilter(color, BlendMode.SRC_ATOP)
        } else {
            view.compoundDrawables[0]?.mutate()?.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP)
            view.compoundDrawables[2]?.mutate()?.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP)
        }
        view.compoundDrawables[0]?.alpha = "A9".toInt(16)
        view.compoundDrawables[2]?.alpha = "A9".toInt(16)

        // Apply text shadow if enabled
        if (sharedPreferenceManager.isTextShadowEnabled()) {
            view.setShadowLayer(4f, 2f, 2f, Color.BLACK)
        } else {
            view.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
        }
    }

    fun setTextFont(view: View) {

        when {
            view is ViewGroup -> {
                view.children.forEach { child ->
                    setTextFont(child)
                }
            }
            hasMethod(view, "setTextAppearance") -> {
                setFont(view as TextView)
            }
        }
    }

    fun setFont(view: TextView) {
        var font = sharedPreferenceManager.getTextFont()
        val style = sharedPreferenceManager.getTextStyle()

        val newFont = when (font) {
            "system" -> {
                val typedArray = context.obtainStyledAttributes(android.R.style.TextAppearance_DeviceDefault, intArrayOf(android.R.attr.fontFamily))
                val systemFont = typedArray.getString(0)
                typedArray.recycle()
                if (systemFont != null) {
                    Typeface.create(systemFont, Typeface.NORMAL)
                } else {
                    Typeface.DEFAULT
                }
            }
            "casual" -> Typeface.SANS_SERIF
            "cursive" -> Typeface.SANS_SERIF // or Typeface.create("cursive", Typeface.NORMAL) if available
            "monospace" -> Typeface.MONOSPACE
            "sans-serif" -> Typeface.SANS_SERIF
            "serif" -> Typeface.SERIF
            "sans-serif-light", "sans-serif-thin", "sans-serif-condensed", "sans-serif-condensed-light", "sans-serif-smallcaps" -> {
                // For these system-defined fonts, create using the string name
                // If the system doesn't have the font, it will fall back to default
                Typeface.create(font, Typeface.NORMAL)
            }
            else -> {
                // For custom fonts, get from FontMap
                val fontId = FontMap.fonts[font]
                if (fontId != null) {
                    ResourcesCompat.getFont(context, fontId)
                } else {
                    // Fallback to default if font not found
                    Typeface.DEFAULT
                }
            }
        }

        when (style) {
            "normal" -> {
                view.typeface = newFont
            }
            "bold" -> {
                view.typeface = Typeface.create(newFont, Typeface.BOLD)
            }
            "italic" -> {
                view.typeface = Typeface.create(newFont, Typeface.ITALIC)
            }
            "bold-italic" -> {
                view.typeface = Typeface.create(newFont, Typeface.BOLD_ITALIC)
            }
        }
    }

    private fun setAlpha(color: Int, alphaHex: String): Int {
        val newAlpha = Integer.parseInt(alphaHex, 16)

        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)

        return Color.argb(newAlpha, r, g, b)
    }

    // Visibility
    fun setClockVisibility(clock: TextClock) {
        val layoutParams = clock.layoutParams

        if (sharedPreferenceManager.isClockEnabled()) {
            layoutParams.height = WRAP_CONTENT
        } else {
            layoutParams.height = 1
        }
        clock.layoutParams = layoutParams
    }

    fun setDateVisibility(dateText: TextClock) {
        dateText.visibility = when (sharedPreferenceManager.isDateEnabled()) {
            true -> {
                View.VISIBLE
            }
            false -> {
                View.GONE
            }
        }
    }

    fun setSearchVisibility(searchView: View, searchLayout: View, replacementView: View) {
        setSearchLayoutVisibility(searchLayout, replacementView)
        if (sharedPreferenceManager.isSearchEnabled()) {
            searchView.visibility = View.VISIBLE
        } else {
            searchView.visibility = View.GONE
        }
    }

    fun setContactsVisibility(contactsView: View, searchLayout: View, replacementView: View) {
        setSearchLayoutVisibility(searchLayout, replacementView)
        if (sharedPreferenceManager.areContactsEnabled()) {
            contactsView.visibility = View.VISIBLE
        } else {
            contactsView.visibility = View.GONE
        }
    }

    fun setWebSearchVisibility(webSearchButton: View) {
        if (sharedPreferenceManager.isWebSearchEnabled()) {
            webSearchButton.visibility = View.VISIBLE
        } else {
            webSearchButton.visibility = View.GONE
        }
    }

    private fun setSearchLayoutVisibility(searchLayout: View, replacementView: View) {
        if (!sharedPreferenceManager.isSearchEnabled() && !sharedPreferenceManager.areContactsEnabled()) {
            searchLayout.visibility = View.GONE
            replacementView.visibility = View.VISIBLE
        } else {
            replacementView.visibility = View.GONE
            searchLayout.visibility = View.VISIBLE
        }
    }

    // Alignment
    fun setClockAlignment(clock: TextClock, dateText: TextClock) {
        val alignment = sharedPreferenceManager.getClockAlignment()
        setTextAlignment(clock, alignment)
        setTextAlignment(dateText, alignment)
    }

    fun setShortcutsAlignment(shortcuts: LinearLayout) {
        val alignment = sharedPreferenceManager.getShortcutAlignment()
        shortcuts.children.forEach {
            if (it is TextView) {
                setTextGravity(it, alignment)
                setDrawables(it, alignment)
            }
        }
    }

    fun setShortcutsVAlignment(topSpace: Space, bottomSpace: Space) {
        val alignment = sharedPreferenceManager.getShortcutVAlignment()
        val topLayoutParams = topSpace.layoutParams as LinearLayout.LayoutParams
        val bottomLayoutParams = bottomSpace.layoutParams as LinearLayout.LayoutParams

        when (alignment) {
            "top" -> {
                topLayoutParams.weight = 0.1F
                bottomLayoutParams.weight = 0.42F
            }
            "center" -> {
                topLayoutParams.weight = 0.22F
                bottomLayoutParams.weight = 0.3F
            }
            "bottom" -> {
                topLayoutParams.weight = 0.42F
                bottomLayoutParams.weight = 0.1F
            }
        }

        topSpace.layoutParams = topLayoutParams
        bottomSpace.layoutParams = bottomLayoutParams
    }

    fun setDrawables(textView: TextView, alignment: String?, alignments: Array<String> = arrayOf("left","center","right")) {
        try {
            when (alignment) {
                alignments[0] -> {
                    textView.setCompoundDrawablesWithIntrinsicBounds(
                        textView.compoundDrawables.filterNotNull().first(),
                        null,
                        null,
                        null
                    )
                }

                alignments[1] -> {
                    textView.setCompoundDrawablesWithIntrinsicBounds(
                        textView.compoundDrawables.filterNotNull().first(),
                        null,
                        textView.compoundDrawables.filterNotNull().first(),
                        null
                    )
                }

                alignments[2] -> {
                    textView.setCompoundDrawablesWithIntrinsicBounds(
                        null,
                        null,
                        textView.compoundDrawables.filterNotNull().first(),
                        null
                    )
                }
            }
        } catch (_: Exception) {}
    }

    fun setAppAlignment(
        textView: TextView,
        editText: TextView? = null,
        regionText: TextView? = null,
    ) {
        val alignment = sharedPreferenceManager.getAppAlignment()
        setTextGravity(textView, alignment)

        if (regionText != null) {
            setTextGravity(regionText, alignment)
            return
        }

        if (editText != null) {
            setDrawables(textView, alignment)
            setTextGravity(editText, alignment)
        }

    }

    fun setSearchAlignment(searchView: TextInputEditText) {
        setTextAlignment(searchView, sharedPreferenceManager.getSearchAlignment())
    }

    fun setMenuTitleAlignment(menuTitle: TextView) {
        val alignment = sharedPreferenceManager.getAppAlignment()
        setTextGravity(menuTitle, alignment)
        setDrawables(menuTitle, alignment, arrayOf("right","center","left"))
    }

    private fun setTextAlignment(view: TextView, alignment: String?) {
        try {
            view.textAlignment = when (alignment) {
            "left" -> View.TEXT_ALIGNMENT_VIEW_START

            "center" -> View.TEXT_ALIGNMENT_CENTER

            "right" -> View.TEXT_ALIGNMENT_VIEW_END

            else -> View.TEXT_ALIGNMENT_VIEW_START
            }
        } catch (_: Exception) {}
    }

    private fun setTextGravity(view: TextView, alignment: String?) {
        try {
            view.gravity = when (alignment) {
                "left" -> Gravity.CENTER_VERTICAL or Gravity.START

                "center" -> Gravity.CENTER

                "right" -> Gravity.CENTER_VERTICAL or Gravity.END

                else -> Gravity.CENTER_VERTICAL or Gravity.START
            }
        } catch (_: Exception) {}
    }

    // Size
    fun setClockSize(clock: TextClock) {
        setTextSize(clock, sharedPreferenceManager.getClockSize(), 48F, 58F, 70F, 78F, 82F, 84F)
    }

    fun setDateSize(dateText: TextClock) {
        setTextSize(dateText, sharedPreferenceManager.getDateSize(), 14F, 17F, 20F, 23F, 26F, 29F)
    }

    fun setShortcutsSize(shortcuts: LinearLayout) {

        val size = sharedPreferenceManager.getShortcutSize()

        shortcuts.children.forEach {
            if (it is TextView) {
                setShortcutSize(it, size)
            }
        }
    }

    private fun setShortcutSize(shortcut: TextView, size: String?) {
        try {
            when (size) {
                "tiny" -> {
                    shortcut.setAutoSizeTextTypeUniformWithConfiguration(
                        5,   // Min text size in SP
                        20,   // Max text size in SP
                        2,    // Step granularity in SP
                        TypedValue.COMPLEX_UNIT_SP // Unit of measurement
                    )
                }

                "small" -> {
                    shortcut.setAutoSizeTextTypeUniformWithConfiguration(
                        5,   // Min text size in SP
                        24,   // Max text size in SP
                        2,    // Step granularity in SP
                        TypedValue.COMPLEX_UNIT_SP // Unit of measurement
                    )
                }

                "medium" -> {
                    shortcut.setAutoSizeTextTypeUniformWithConfiguration(
                        5,   // Min text size in SP
                        28,   // Max text size in SP
                        2,    // Step granularity in SP
                        TypedValue.COMPLEX_UNIT_SP // Unit of measurement
                    )
                }

                "large" -> {
                    shortcut.setAutoSizeTextTypeUniformWithConfiguration(
                        5,   // Min text size in SP
                        32,   // Max text size in SP
                        2,    // Step granularity in SP
                        TypedValue.COMPLEX_UNIT_SP // Unit of measurement
                    )
                }

                "extra" -> {
                    shortcut.setAutoSizeTextTypeUniformWithConfiguration(
                        5,   // Min text size in SP
                        36,   // Max text size in SP
                        2,    // Step granularity in SP
                        TypedValue.COMPLEX_UNIT_SP // Unit of measurement
                    )
                }

                "huge" -> {
                    shortcut.setAutoSizeTextTypeUniformWithConfiguration(
                        5,   // Min text size in SP
                        40,   // Max text size in SP
                        2,    // Step granularity in SP
                        TypedValue.COMPLEX_UNIT_SP // Unit of measurement
                    )
                }
            }
        } catch(_: Exception) {}
    }

    fun setAppSize(
        textView: TextView,
        editText: TextInputEditText? = null,
        regionText: TextView? = null
    ) {
        val size = sharedPreferenceManager.getAppSize()
        setTextSize(textView, size, 21F, 24F, 27F, 30F, 33F, 36F)
        if (editText != null) {
            setTextSize(editText, size, 21F, 24F, 27F, 30F, 33F, 36F)
        }
        if (regionText != null) {
            setTextSize(regionText, size, 11F, 14F, 17F, 20F, 23F, 26F)
        }
    }

    fun setSearchSize(searchView: TextInputEditText) {
        setTextSize(searchView, sharedPreferenceManager.getSearchSize(), 18F, 21F, 25F, 27F, 30F, 33F)
    }

    fun setMenuTitleSize(menuTitle: TextView) {
        setTextSize(menuTitle, sharedPreferenceManager.getAppSize(), 27F, 30F, 33F, 36F, 39F, 42F)
    }

    private fun setTextSize(view: TextView, size: String?, t: Float, s: Float, m: Float, l: Float, x: Float, h: Float) {
        try {
            view.textSize = when (size) {
                "tiny" -> t

                "small" -> s

                "medium" -> m

                "large" -> l

                "extra" -> x

                "huge" -> h

                else -> {
                    0F
                }
            }
        } catch (_: Exception) {}
    }

    // Spacing
    fun setShortcutsSpacing(shortcuts: LinearLayout) {
        val shortcutWeight = sharedPreferenceManager.getShortcutWeight()
        shortcuts.children.forEach {
            if (it is TextView) {
                setShortcutSpacing(it, shortcutWeight)
            }
        }
    }

    private fun setShortcutSpacing(shortcut: TextView, shortcutWeight: Float?) {
        val layoutParams = shortcut.layoutParams as LinearLayout.LayoutParams

        if (shortcutWeight != null) {
            layoutParams.weight = shortcutWeight
        }

        shortcut.layoutParams = layoutParams
    }

    fun setItemSpacing(item: TextView) {
        val spacing = sharedPreferenceManager.getAppSpacing()
        if (spacing != null) {
            val spacingPx = dpToPx(spacing)
            item.setPadding(item.paddingLeft, spacingPx, item.paddingRight, spacingPx)
        }
    }

    fun setWeatherSpacing(item: ConstraintLayout) {
        val spacing = sharedPreferenceManager.getAppSpacing()
        if (spacing != null) {
            val spacingPx = dpToPx(spacing)
            item.setPadding(item.paddingLeft, spacingPx, item.paddingRight, spacingPx)
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }

    // Status bar visibility
    fun setStatusBar(window: Window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowInsetsController = window.insetsController

            windowInsetsController?.let {
                if (sharedPreferenceManager.isBarVisible()) {
                    it.show(WindowInsets.Type.statusBars())
                }
                else {
                    it.hide(WindowInsets.Type.statusBars())
                    it.systemBarsBehavior =
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
        } else {
            @Suppress("DEPRECATION")
            if (sharedPreferenceManager.isBarVisible()) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            } else {
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }
        }
    }

    fun switchFragment(activity: FragmentActivity, fragment: Fragment) {
        activity.supportFragmentManager
            .beginTransaction()
            .replace(R.id.settingsLayout, fragment)
            .addToBackStack(null)
            .commit()
    }
}