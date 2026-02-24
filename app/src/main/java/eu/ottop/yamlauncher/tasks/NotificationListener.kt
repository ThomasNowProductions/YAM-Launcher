package eu.ottop.yamlauncher.tasks

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import eu.ottop.yamlauncher.utils.Logger

class NotificationListener : NotificationListenerService() {

    private val logger = Logger.getInstance(this)

    companion object {
        const val ACTION_NOTIFICATIONS_CHANGED = "eu.ottop.yamlauncher.NOTIFICATIONS_CHANGED"
        
        private var instance: NotificationListener? = null
        
        fun getInstance(): NotificationListener? = instance
        
        fun isEnabled(context: Context): Boolean {
            val componentName = ComponentName(context, NotificationListener::class.java)
            val enabledListeners = android.provider.Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            return enabledListeners?.contains(componentName.flattenToString()) == true
        }
        
        fun requestPermission(context: Context) {
            val intent = Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        logger.i("NotificationListener", "NotificationListener service created")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        logger.i("NotificationListener", "NotificationListener service destroyed")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        logger.i("NotificationListener", "NotificationListener connected")
        broadcastUpdate()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        logger.i("NotificationListener", "NotificationListener disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let {
            logger.d("NotificationListener", "Notification posted from: ${it.packageName}")
        }
        broadcastUpdate()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        sbn?.let {
            logger.d("NotificationListener", "Notification removed from: ${it.packageName}")
        }
        broadcastUpdate()
    }

    private fun broadcastUpdate() {
        val intent = Intent(ACTION_NOTIFICATIONS_CHANGED)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    fun hasNotification(packageName: String): Boolean {
        val notifications = activeNotifications
        return notifications.any { it.packageName == packageName }
    }

    fun getPackagesWithNotifications(): Set<String> {
        return activeNotifications.map { it.packageName }.toSet()
    }
}
