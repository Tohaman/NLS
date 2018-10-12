package ru.tohaman.nls

import android.content.*
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.text.TextUtils
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.noButton
import org.jetbrains.anko.toast
import org.jetbrains.anko.yesButton

class MainActivity : AppCompatActivity() {
    private val ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners"
    private val ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"
    private lateinit var actionBroadcastReceiver : ActionBroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // If the user did not turn the notification listener service on we prompt him to do so
        if (!isNotificationServiceEnabled()) {
            alert(
                getString(R.string.notification_listener_service_explanation),
                getString(R.string.notification_listener_service)
            ) {
                yesButton { startActivity(Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                noButton { toast("Жаль то какая...") }
            }.show()
        }


        val title = "Мяу!"
        val text = "А кота кто кормить будет?"
        btn_send_message.setOnClickListener {
            //TODO Переделать на нормальную отправку нотификейшенов из котлина
            val notificationId = 1

            val notificationBuilder = NotificationCompat.Builder(this@MainActivity)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(text)

            val notificationManager = NotificationManagerCompat.from(this@MainActivity)
            notificationManager.notify(notificationId, notificationBuilder.build())
        }

        // Finally we register a receiver to tell the MainActivity when a notification has been received
        actionBroadcastReceiver = ActionBroadcastReceiver()
        val intentFilter = IntentFilter()
        intentFilter.addAction("ru.tohaman.nls")
        registerReceiver(actionBroadcastReceiver, intentFilter)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(actionBroadcastReceiver)
    }


    /**
     * Image Change Broadcast Receiver.
     * We use this Broadcast Receiver to notify the Main Activity when
     * a new notification has arrived. И обрабатываем это событие
     */
    inner class ActionBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val receivedTextFromShazam = intent.getStringExtra("Notification Code")
            text.text = receivedTextFromShazam
            if (receivedTextFromShazam.startsWith("Сейчас:")) {
                
            }

        }
    }


    /**
     * Is Notification Service Enabled.
     * Verifies if the notification listener service is enabled.
     * Got it from: https://github.com/kpbird/NotificationListenerService-Example/blob/master/NLSExample/src/main/java/com/kpbird/nlsexample/NLService.java
     * @return True if eanbled, false otherwise.
     */
    private fun isNotificationServiceEnabled(): Boolean {
        //получаем список приложений с доступом к нотификейшенам
        val flat = Settings.Secure.getString(
            contentResolver,
            ENABLED_NOTIFICATION_LISTENERS
        )
        if (!TextUtils.isEmpty(flat)) {
            val names = flat.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (i in names.indices) {
                val cn = ComponentName.unflattenFromString(names[i])
                if (cn != null) {
                    if (TextUtils.equals(packageName, cn.packageName)) {
                        return true
                    }
                }
            }
        }
        return false
    }

}
