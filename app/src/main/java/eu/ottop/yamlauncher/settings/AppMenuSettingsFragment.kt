package eu.ottop.yamlauncher.settings

import android.Manifest
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import eu.ottop.yamlauncher.R
import eu.ottop.yamlauncher.utils.PermissionUtils
import eu.ottop.yamlauncher.utils.UIUtils

class AppMenuSettingsFragment : PreferenceFragmentCompat(), TitleProvider { private val permissionUtils = PermissionUtils()
    private var contactPref: SwitchPreference? = null
    private var webSearchPref: SwitchPreference? = null
    private var autoLaunchPref: SwitchPreference? = null
    private var searchEnabledPref: SwitchPreference? = null

    private var webSearchBaseSummary: CharSequence? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.app_menu_preferences, rootKey)

        val uiUtils = UIUtils(requireContext())
        val contextMenuSettings = findPreference<Preference>("contextMenuSettings")

        contactPref = findPreference("contactsEnabled")
        webSearchPref = findPreference("webSearchEnabled")
        autoLaunchPref = findPreference("autoLaunch")
        searchEnabledPref = findPreference("searchEnabled")

        webSearchBaseSummary = webSearchPref?.summary

        contactPref?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->

            if (newValue as Boolean && !permissionUtils.hasPermission(requireContext(), Manifest.permission.READ_CONTACTS)) {
                    (requireActivity() as SettingsActivity).requestContactsPermission()
                    return@OnPreferenceChangeListener false
                } else {
                    return@OnPreferenceChangeListener true
                }
        }

        if (webSearchPref != null && autoLaunchPref != null) {
            // A restored/migrated preference state can end up with both enabled; normalize so settings can't get stuck.
            if (webSearchPref?.isChecked == true && autoLaunchPref?.isChecked == true) {
                autoLaunchPref?.isChecked = false
            }

            webSearchPref?.isEnabled = (autoLaunchPref?.isChecked == false)
            autoLaunchPref?.isEnabled = (webSearchPref?.isChecked == false)
            updateAutoLaunchSummary(webSearchPref?.isChecked == true)
            updateWebSearchSummary(searchEnabledPref?.isChecked == true, autoLaunchPref?.isChecked == true)
            webSearchPref?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                val enabled = newValue as Boolean
                autoLaunchPref?.isEnabled = !enabled
                updateAutoLaunchSummary(enabled)
                updateWebSearchSummary(searchEnabledPref?.isChecked == true, autoLaunchPref?.isChecked == true)
                return@OnPreferenceChangeListener true
            }
            autoLaunchPref?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                val enabled = newValue as Boolean
                webSearchPref?.isEnabled = !enabled
                updateAutoLaunchSummary(webSearchPref?.isChecked == true)
                updateWebSearchSummary(searchEnabledPref?.isChecked == true, enabled)
                return@OnPreferenceChangeListener true
            }
        }

        searchEnabledPref?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            val enabled = newValue as Boolean
            updateWebSearchSummary(enabled, autoLaunchPref?.isChecked == true)
            return@OnPreferenceChangeListener true
        }

        contextMenuSettings?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                uiUtils.switchFragment(requireActivity(), ContextMenuSettingsFragment())
                true }
    }

    override fun getTitle(): String {
        return getString(R.string.app_settings_title)
    }

    override fun onResume() {
        super.onResume()
        updateAutoLaunchSummary(webSearchPref?.isChecked == true)
        updateWebSearchSummary(searchEnabledPref?.isChecked == true, autoLaunchPref?.isChecked == true)
    }

    private fun updateAutoLaunchSummary(isWebSearchEnabled: Boolean) {
        val autoLaunch = autoLaunchPref ?: return
        val base = getString(R.string.auto_launch_summary)
        if (!isWebSearchEnabled) {
            autoLaunch.summary = base
            return
        }
        autoLaunch.summary = "$base\n${getString(R.string.auto_launch_disabled_reason_web_search)}"
    }

    private fun updateWebSearchSummary(isSearchEnabled: Boolean, isAutoOpenEnabled: Boolean) {
        val webSearch = webSearchPref ?: return
        val base = webSearchBaseSummary?.toString()?.trim().orEmpty()

        // Don't add a redundant note when search itself is disabled.
        if (!isSearchEnabled) {
            webSearch.summary = webSearchBaseSummary
            return
        }

        if (!isAutoOpenEnabled) {
            webSearch.summary = webSearchBaseSummary
            return
        }

        val reason = getString(R.string.web_search_disabled_reason_auto_open)
        webSearch.summary = if (base.isEmpty()) reason else "$base\n$reason"
    }

    fun setContactPreference(isEnabled: Boolean) {
        contactPref?.isChecked = isEnabled
    }
}
