package eu.ottop.yamlauncher

import android.app.Dialog
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.Bundle
import android.os.UserHandle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import eu.ottop.yamlauncher.settings.SharedPreferenceManager

class AppActionBottomSheet : BottomSheetDialogFragment() {

    interface AppActionListener {
        fun onPinApp(appActivity: LauncherActivityInfo, workProfile: Int)
        fun onAppInfo(appActivity: LauncherActivityInfo, userHandle: UserHandle)
        fun onUninstallApp(appActivity: LauncherActivityInfo, userHandle: UserHandle)
        fun onRenameApp(appActivity: LauncherActivityInfo, userHandle: UserHandle, workProfile: Int)
        fun onHideApp(appActivity: LauncherActivityInfo, workProfile: Int)
    }

    private lateinit var sharedPreferenceManager: SharedPreferenceManager
    private lateinit var appNameTitle: TextView
    private lateinit var pinButton: LinearLayout
    private lateinit var pinIcon: ImageView
    private lateinit var pinLabel: TextView

    private var appActivity: LauncherActivityInfo? = null
    private var userHandle: UserHandle? = null
    private var workProfile: Int = 0
    private var listener: AppActionListener? = null

    companion object {
        const val TAG = "AppActionBottomSheet"
        private const val ARG_COMPONENT_NAME = "component_name"
        private const val ARG_USER_HANDLE_ID = "user_handle_id"
        private const val ARG_WORK_PROFILE = "work_profile"

        fun newInstance(
            appActivity: LauncherActivityInfo,
            userHandle: UserHandle,
            workProfile: Int,
            listener: AppActionListener
        ): AppActionBottomSheet {
            val fragment = AppActionBottomSheet()
            fragment.listener = listener
            val args = Bundle().apply {
                putString(ARG_COMPONENT_NAME, appActivity.componentName.flattenToString())
                putInt(ARG_USER_HANDLE_ID, workProfile)
                putInt(ARG_WORK_PROFILE, workProfile)
            }
            fragment.arguments = args
            fragment.appActivity = appActivity
            fragment.userHandle = userHandle
            fragment.workProfile = workProfile
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), R.style.Theme_YamLauncher_BottomSheet)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.app_action_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferenceManager = SharedPreferenceManager(requireContext())

        // Restore arguments if fragment was recreated
        if (appActivity == null) {
            val componentNameStr = arguments?.getString(ARG_COMPONENT_NAME)
            val userHandleId = arguments?.getInt(ARG_USER_HANDLE_ID, 0) ?: 0
            workProfile = arguments?.getInt(ARG_WORK_PROFILE, 0) ?: 0

            if (componentNameStr != null) {
                val launcherApps = requireContext().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                userHandle = launcherApps.profiles.getOrNull(userHandleId)
                val componentName = android.content.ComponentName.unflattenFromString(componentNameStr)
                appActivity = componentName?.let { cn ->
                    userHandle?.let { uh ->
                        launcherApps.getActivityList(cn.packageName, uh).firstOrNull { it.componentName == cn }
                    }
                }
            }
        }

        val currentApp = appActivity
        val currentUser = userHandle
        val currentListener = listener

        if (currentApp == null || currentUser == null || currentListener == null) {
            dismiss()
            return
        }

        appNameTitle = view.findViewById(R.id.appNameTitle)
        appNameTitle.text = sharedPreferenceManager.getAppName(
            currentApp.componentName.flattenToString(),
            workProfile,
            currentApp.label
        )

        setupActionButtons(view, currentApp, currentUser, currentListener)
    }

    private fun setupActionButtons(view: View, appActivity: LauncherActivityInfo, userHandle: UserHandle, listener: AppActionListener) {
        pinButton = view.findViewById(R.id.pin)
        pinIcon = pinButton.findViewById(R.id.pinIcon)
        pinLabel = pinButton.findViewById(R.id.pinLabel)
        val infoButton = view.findViewById<LinearLayout>(R.id.info)
        val uninstallButton = view.findViewById<LinearLayout>(R.id.uninstall)
        val renameButton = view.findViewById<LinearLayout>(R.id.rename)
        val hideButton = view.findViewById<LinearLayout>(R.id.hide)

        val enablePin = sharedPreferenceManager.isPinEnabled()
        val enableInfo = sharedPreferenceManager.isInfoEnabled()
        val enableUninstall = sharedPreferenceManager.isUninstallEnabled()
        val enableRename = sharedPreferenceManager.isRenameEnabled()
        val enableHide = sharedPreferenceManager.isHideEnabled()

        setPinState(appActivity)

        setupButton(pinButton, enablePin) {
            listener.onPinApp(appActivity, workProfile)
        }

        setupButton(infoButton, enableInfo) {
            listener.onAppInfo(appActivity, userHandle)
        }

        setupButton(uninstallButton, enableUninstall, appActivity.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
            listener.onUninstallApp(appActivity, userHandle)
        }

        setupButton(renameButton, enableRename) {
            listener.onRenameApp(appActivity, userHandle, workProfile)
        }

        setupButton(hideButton, enableHide) {
            listener.onHideApp(appActivity, workProfile)
        }
    }

    private fun setupButton(button: View, enabled: Boolean, additionalCondition: Boolean = true, action: () -> Unit) {
        if (enabled && additionalCondition) {
            button.visibility = View.VISIBLE
            button.setOnClickListener {
                action()
                dismiss()
            }
        } else {
            button.visibility = View.GONE
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    private fun setPinState(appActivity: LauncherActivityInfo) {
        val isPinned = sharedPreferenceManager.isAppPinned(
            appActivity.componentName.flattenToString(),
            workProfile
        )
        val iconRes = when (isPinned) {
            true -> R.drawable.keep_off_24px
            false -> R.drawable.keep_24px
        }
        pinIcon.setImageResource(iconRes)

        val labelText = when (isPinned) {
            true -> getString(R.string.unpin)
            false -> getString(R.string.pin)
        }
        pinLabel.text = labelText
    }
}