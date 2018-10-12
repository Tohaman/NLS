package ru.tohaman.nls

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class MyNotesListenerService : NotificationListenerService() {

    /*
        These are the package names of the apps. for which we want to
        listen the notifications
     */
    private object ApplicationPackageNames {
        const val NLS_PACK_NAME = "ru.tohaman.nls"
        const val SHAZAM_PACK_NAME = "com.shazam.android"
        const val WHATSAPP_PACK_NAME = "com.whatsapp"
        const val INSTAGRAM_PACK_NAME = "com.instagram.android"
    }

    /*
        These are the return codes we use in the method which intercepts
        the notifications, to decide whether we should do something or not
     */
    object InterceptedNotificationCode {
        const val NLS_CODE = 1
        const val WHATSAPP_CODE = 2
        const val SHAZAM_CODE = 3
        const val OTHER_NOTIFICATIONS_CODE = 4 // We ignore all notification with code == 4
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val notificationCode = matchNotificationCode(sbn)

        if (notificationCode == InterceptedNotificationCode.SHAZAM_CODE) {
            val retString = sbn.notification.tickerText.toString()

            val intent = Intent("ru.tohaman.nls")
            intent.putExtra("Notification Code", retString)
            sendBroadcast(intent)
        }
    }


//    override fun onNotificationRemoved(sbn: StatusBarNotification) {
//        val notificationCode = matchNotificationCode(sbn)
//
//        val activeNotifications = this.activeNotifications
//
//        if (activeNotifications != null && activeNotifications.isNotEmpty()) {
//            for (i in activeNotifications.indices) {
//                if (notificationCode == matchNotificationCode(activeNotifications[i])) {
//                    val intent = Intent("ru.tohaman.nls")
//                    intent.putExtra("Notification Code", notificationCode)
//                    sendBroadcast(intent)
//                    break
//                }
//            }
//        }
//    }

     private fun matchNotificationCode(sbn: StatusBarNotification): Int {
        val packageName = sbn.packageName

        return if (packageName == ApplicationPackageNames.NLS_PACK_NAME) {
            InterceptedNotificationCode.NLS_CODE
        } else if (packageName == ApplicationPackageNames.SHAZAM_PACK_NAME) {
            InterceptedNotificationCode.SHAZAM_CODE
        } else if (packageName == ApplicationPackageNames.WHATSAPP_PACK_NAME) {
            InterceptedNotificationCode.WHATSAPP_CODE
        } else {
            InterceptedNotificationCode.OTHER_NOTIFICATIONS_CODE
        }
    }
}
