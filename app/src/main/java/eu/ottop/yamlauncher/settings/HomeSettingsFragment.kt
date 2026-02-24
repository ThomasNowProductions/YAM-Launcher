package eu.ottop.yamlauncher.settings

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import eu.ottop.yamlauncher.R
import eu.ottop.yamlauncher.tasks.NotificationListener
import eu.ottop.yamlauncher.utils.PermissionUtils
import eu.ottop.yamlauncher.utils.UIUtils

class HomeSettingsFragment : PreferenceFragmentCompat(), TitleProvider {

    private lateinit var sharedPreferenceManager: SharedPreferenceManager
    private val permissionUtils = PermissionUtils()

    private var gpsLocationPref: SwitchPreference? = null
    private var manualLocationPref: Preference? = null
    private var leftSwipePref: Preference? = null
    private var rightSwipePref: Preference? = null
    private var doubleTapTogglePref: SwitchPreference? = null
    private var doubleTapActionPref: Preference? = null
    private var doubleTapAppPref: Preference? = null
    private var clockApp: Preference? = null
    private var dateApp: Preference? = null
    private var notificationDotsPref: SwitchPreference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.home_preferences, rootKey)
        val uiUtils = UIUtils(requireContext())

        sharedPreferenceManager = SharedPreferenceManager(requireContext())

        clockApp = findPreference("clockSwipeApp")
        dateApp = findPreference("dateSwipeApp")

        gpsLocationPref = findPreference("gpsLocation")
        manualLocationPref = findPreference("manualLocation")
        leftSwipePref = findPreference("leftSwipeApp")
        rightSwipePref = findPreference("rightSwipeApp")
        doubleTapTogglePref = findPreference("doubleTap")
        doubleTapActionPref = findPreference("doubleTapAction")
        doubleTapAppPref = findPreference("doubleTapSwipeApp")
        notificationDotsPref = findPreference("notificationDots")

        // Only enable manual location when gps location is disabled
        if (gpsLocationPref != null && manualLocationPref != null) {
            manualLocationPref?.isEnabled = gpsLocationPref?.isChecked == false

            gpsLocationPref?.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, newValue ->
                    if (newValue as Boolean && !permissionUtils.hasPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)) {
                        (requireActivity() as SettingsActivity).requestLocationPermission()
                        return@OnPreferenceChangeListener false
                    } else {
                        manualLocationPref?.isEnabled = !newValue
                        return@OnPreferenceChangeListener true
                    }
                }

            manualLocationPref?.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    uiUtils.switchFragment(requireActivity(), LocationFragment())
                    true
                }
        }

        leftSwipePref?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                uiUtils.switchFragment(requireActivity(), GestureAppsFragment("left"))
                true
            }

        rightSwipePref?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                uiUtils.switchFragment(requireActivity(), GestureAppsFragment("right"))
                true
            }

        doubleTapTogglePref?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                val launchesApp = sharedPreferenceManager.getDoubleTapAction() == "app"
                doubleTapAppPref?.isEnabled = (newValue as Boolean) && launchesApp
                true
            }

        doubleTapActionPref?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                doubleTapAppPref?.isEnabled = (doubleTapTogglePref?.isChecked == true) && (newValue as String == "app")
                true
            }

        doubleTapAppPref?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                uiUtils.switchFragment(requireActivity(), GestureAppsFragment("doubleTap"))
                true
            }

        clockApp?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                uiUtils.switchFragment(requireActivity(), GestureAppsFragment("clock"))
                true
            }

        dateApp?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                uiUtils.switchFragment(requireActivity(), GestureAppsFragment("date"))
                true
            }

        notificationDotsPref?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                if (newValue as Boolean && !NotificationListener.isEnabled(requireContext())) {
                    NotificationListener.requestPermission(requireContext())
                    false
                } else {
                    true
                }
            }

        updateDoubleTapAppPreferenceState()
    }

    override fun onResume() {
        super.onResume()
        clockApp?.summary = sharedPreferenceManager.getGestureName("clock")

        dateApp?.summary = sharedPreferenceManager.getGestureName("date")

        manualLocationPref?.summary = sharedPreferenceManager.getWeatherRegion()

        leftSwipePref?.summary = sharedPreferenceManager.getGestureName("left")

        rightSwipePref?.summary = sharedPreferenceManager.getGestureName("right")

        doubleTapAppPref?.summary = sharedPreferenceManager.getGestureName("doubleTap")

        updateDoubleTapAppPreferenceState()
    }

    private fun updateDoubleTapAppPreferenceState() {
        val launchesApp = sharedPreferenceManager.getDoubleTapAction() == "app"
        val isDoubleTapEnabled = doubleTapTogglePref?.isChecked == true
        doubleTapAppPref?.isEnabled = isDoubleTapEnabled && launchesApp
    }

    override fun getTitle(): String {
        return getString(R.string.home_settings_title)
    }

    fun setLocationPreference(isEnabled: Boolean) {
        manualLocationPref?.isEnabled = !isEnabled
        gpsLocationPref?.isChecked = isEnabled
    }
}
