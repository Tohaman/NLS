package ru.tohaman.nls

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.*
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.text.TextUtils
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.*

class MainActivity : AppCompatActivity() {
    private val ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners"
    private val ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"
    private lateinit var actionBroadcastReceiver : ActionBroadcastReceiver
    private var notificationManager: NotificationManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        //Подключаем сервис
//        val mConnection = object : ServiceConnection {
//            override fun onServiceConnected(className: ComponentName, service: IBinder) {
//                // TODO
//            }
//
//            override fun onServiceDisconnected(className: ComponentName) {
//                // TODO
//            }
//        }
//        bindService(Intent(this,MyNotesListenerService::class.java), mConnection, Context.BIND_AUTO_CREATE)
//

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
        val text = "Тестовое сообщение"
        btn_send_message.setOnClickListener {
            //TODO Переделать на нормальную отправку нотификейшенов из котлина
            val notificationId = 1

            @Suppress("DEPRECATION")
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
            var receivedTextFromShazam = intent.getStringExtra("Notification Code")

            if (receivedTextFromShazam.startsWith("Сейчас:")) {
                receivedTextFromShazam = receivedTextFromShazam.drop(7)
                val artist = receivedTextFromShazam.substringBefore('—')
                val song = receivedTextFromShazam.substringAfter('—')
                notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                createNotification("ru.tohaman.nls", artist, song)
//                toast(artist)
//                Log.v ("NLS", song)
            }
            text.text = receivedTextFromShazam
        }
    }

    private fun createNotification (id: String, name: String, description: String) {

        val notificationID = 101

        val notification = Notification.Builder( this@MainActivity)
            .setContentTitle(name)
            .setContentText(description)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        notificationManager?.notify(notificationID, notification)
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
