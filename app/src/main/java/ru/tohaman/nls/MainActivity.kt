package ru.tohaman.nls

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationManager
import android.content.*
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.*
import android.content.Intent
import android.os.IBinder


class MainActivity : AppCompatActivity() {
    private val ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners"
    private val ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"
    private lateinit var actionBroadcastReceiver : ActionBroadcastReceiver

    private var notificationManager: NotificationManager? = null
    private var oldReceivedText = ""
    private var counter = 0

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

        //        //Подключаем сервис (не работает)
//        val mConnection = object : ServiceConnection {
//            override fun onServiceConnected(className: ComponentName, service: IBinder) {
//                // TODO
//            }
//            override fun onServiceDisconnected(className: ComponentName) {
//                // TODO
//            }
//        }
//        bindService(Intent(this,MyNotesListenerService::class.java), mConnection, Context.BIND_AUTO_CREATE)


        // Variable to hold service class name
        val serviceClass = NLS_Service::class.java
        val intent = Intent(applicationContext, serviceClass)

        //Проверяем запущен ли NLS_Service, если нет, то запускаем
        if (!isServiceRunning(serviceClass)) { startService(intent) }

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        btn_check_access.setOnClickListener { startActivity(Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS)) }

        btn_send_message.setOnClickListener {
            val notificationId = "ru.tohaman.nls"
            val title = "Test"
            val text = "Тестовое сообщение"
            createNotification(notificationId, title, text)
        }

        btn_start_service.setOnClickListener {
            if (!isServiceRunning(serviceClass)) {
                // Start the service
                startService(intent)
            } else {
                toast("Service already running.")
            }
        }

        btn_stop_service.setOnClickListener {
            // If the service is not running then start it
            if (isServiceRunning(serviceClass)) {
                // Stop the service
                stopService(intent)
            } else {
                toast("Service already stopped.")
            }
        }


        // Finally we register a receiver to tell the MainActivity when a notification has been received
        // Регистрируем броадкастовый ресивер, который принимает сообщения от сервиса, когда получен нотификейшен
        // от какого-либо приложения
        actionBroadcastReceiver = ActionBroadcastReceiver()
        val intentFilter = IntentFilter()
        intentFilter.addAction("ru.tohaman.nls")
        registerReceiver(actionBroadcastReceiver, intentFilter)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(actionBroadcastReceiver)
    }

    // Custom method to determine whether a service is running
    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        // Loop through the running services
        for (service in activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                // If the service is running then return true
                return true
            }
        }
        return false
    }


    /**
     * Image Change Broadcast Receiver.
     * We use this Broadcast Receiver to notify the Main Activity when
     * a new notification has arrived. И обрабатываем это событие
     */
    inner class ActionBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            counter += 1
            text_counter.text = counter.toString()

            var receivedTextFromShazam = intent.getStringExtra("Notification Code")
            if (receivedTextFromShazam.startsWith("Сейчас:")) {
                var textViewText = list_of_shazam.text.toString()

                receivedTextFromShazam = receivedTextFromShazam.substringAfter(' ')
                val artist = receivedTextFromShazam.substringBefore('—')
                val song = receivedTextFromShazam.substringAfter('—')
                textViewText += "$counter $receivedTextFromShazam \n"

                if (oldReceivedText != receivedTextFromShazam) {
                    oldReceivedText = receivedTextFromShazam
                    list_of_shazam.text = textViewText
                }
            }
            last_message.text = receivedTextFromShazam
        }
    }

    @Suppress("DEPRECATION")
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
     * К сожалению, данная проверка проверяет только есть ли разрешение для сервиса, но не проверяет запущен ли сервис
     * пока не нашел как проверять еще и запуск сервиса, обычно он автоматически стартует когда выдается разрешение на запуск
     * поэтому сделан отдельная кнопка для сброса установки "разрешения на доступ к notification", чтобы при установке
     * разрешения, сервис стартовал.
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
