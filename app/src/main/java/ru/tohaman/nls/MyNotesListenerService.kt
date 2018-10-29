package ru.tohaman.nls

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class MyNotesListenerService : NotificationListenerService() {

    private var mBinder: IBinder? = null

    override fun onBind(intent: Intent): IBinder? {
        Log.d("MyNotificationListener", "onBind: ")
        if (mBinder == null)
            mBinder = super.onBind(intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            onListenerConnected()
        return mBinder
    }

    override fun onCreate() {
        Log.d("MyNotificationListener", "onCreate: ")
        super.onCreate()
        if (!isListeningAuthorized(this))
            return
    }

    override fun onDestroy() {
        Log.d("MyNotificationListener", "onDestroy: ")
        super.onDestroy()
        this.mBinder = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        Log.d("MyNotificationListener", "onNotificationPosted: ")
        //val notificationCode = matchNotificationCode(sbn)

//        if (notificationCode == InterceptedNotificationCode.SHAZAM_CODE) {
            val retString = if (sbn.notification.tickerText == null) {
                sbn.packageName
            } else {sbn.notification.tickerText.toString()}

            val intent = Intent("ru.tohaman.nls")
            intent.putExtra("Notification Code", retString)
            sendBroadcast(intent)
//        }
    }


    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        Log.d("MyNotificationListener", "onNotificationRemoved: ")
        val notificationCode = matchNotificationCode(sbn)

        val activeNotifications = this.activeNotifications

        if (activeNotifications != null && activeNotifications.isNotEmpty()) {
            for (i in activeNotifications.indices) {
                if (notificationCode == matchNotificationCode(activeNotifications[i])) {
                    val intent = Intent("ru.tohaman.nls")
                    intent.putExtra("Notification Code", notificationCode)
                    sendBroadcast(intent)
                    break
                }
            }
        }
    }

     private fun matchNotificationCode(sbn: StatusBarNotification): String {
         return sbn.packageName
    }


    /**
     * check if permission is there to read notifications
     * @param context If you are android developer and you don't know context, may god bless you
     * @return boolean status about permission
     */
    fun isListeningAuthorized(context: Context): Boolean {
        val contentResolver = context.contentResolver
        val enabledNotificationListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        val packageName = context.packageName

        return !(enabledNotificationListeners == null || !enabledNotificationListeners.contains(packageName))
    }
}
