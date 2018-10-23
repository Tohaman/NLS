package ru.tohaman.nls

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.*
import android.graphics.BitmapFactory
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.text.TextUtils
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.*

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


        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        btn_check_access.setOnClickListener { startActivity(Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS)) }

        createSimpleNotificationChannel()
        val simpleNotificationBuilder = createSimpleNotification("Test","Тестовое сообщение")
        btn_send_message.setOnClickListener {
//            val notificationId = "ru.tohaman.nls"
//            val title = "Тест!"
//            val text = "Тестовое сообщение"
//            createNotification(notificationId, title, text)
            val notificationManager = NotificationManagerCompat.from(this)
            notificationManager.notify(SIMPLE_NOTIFICATION, simpleNotificationBuilder.build())
        }


//        createExpandableNotificationChannel()
//        val expandableNotificationBuilder = createExpandableNotification()
//        btnExpandableNotification.setOnClickListener {
//            val notificationManager = NotificationManagerCompat.from(this)
//            notificationManager.notify(EXPANDABLE_NOTIFICATION, expandableNotificationBuilder.build())
//        }
//
//        createMultipleLinesNotificationChannel()
//        val multipleLinesNotificationBuilder = createMultipleLinesNotification()
//        btnMultipleLinesNotification.setOnClickListener {
//            val notificationManager = NotificationManagerCompat.from(this)
//            notificationManager.notify(MULTIPLE_LINES_NOTIFICATION, multipleLinesNotificationBuilder.build())
//        }
//
//        createImagesNotificationChannel()
//        val imagesNotificationBuilder = createImagesNotification()
//        btnImagesNotification.setOnClickListener {
//            val notificationManager = NotificationManagerCompat.from(this)
//            notificationManager.notify(IMAGES_NOTIFICATION, imagesNotificationBuilder.build())
//        }
//

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
                    createNotification( artist, song)
                    oldReceivedText = receivedTextFromShazam
                    list_of_shazam.text = textViewText
                }
            }
            last_message.text = receivedTextFromShazam
        }
    }

    @Suppress("DEPRECATION")
    private fun createNotification (name: String, description: String) {
//        val notificationID = 101
//
//        val notification = Notification.Builder( this@MainActivity)
//            .setContentTitle(name)
//            .setContentText(description)
//            .setSmallIcon(android.R.drawable.ic_dialog_info)
//            .build()
//        notificationManager?.notify(notificationID, notification)
        createSimpleNotificationChannel()
        val simpleNotificationBuilder = createSimpleNotification(name,description)
        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.notify(SIMPLE_NOTIFICATION, simpleNotificationBuilder.build())

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

    private fun createSimpleNotification(name: String, description: String): NotificationCompat.Builder {
        val intent = Intent(this, SecondActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        val builder = NotificationCompat.Builder(this, "channel_id")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(name)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        return builder
    }

    private fun createSimpleNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "channel_name"
            val description = "channel_description"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("channel_id", name, importance)
            channel.description = description
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager!!.createNotificationChannel(channel)
        }
    }

    private fun createExpandableNotification(): NotificationCompat.Builder {
        val intent = Intent(this, SecondActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        val builder = NotificationCompat.Builder(this, "channel_id2")
            .setSmallIcon(R.drawable.notification_template_icon_bg)
            .setContentTitle("Expandable notification")
            .setContentText("Open second activiy")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Much longer text that cannot fit one line, bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla ..."))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        return builder
    }

    private fun createExpandableNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "channel_name"
            val description = "channel_description"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("channel_id2", name, importance)
            channel.description = description
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager!!.createNotificationChannel(channel)
        }
    }

    private fun createMultipleLinesNotification(): NotificationCompat.Builder {
        val intent = Intent(this, SecondActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        val builder = NotificationCompat.Builder(this, "channel_id3")
            .setSmallIcon(R.drawable.notification_template_icon_bg)
            .setContentTitle("Multiple lines notification")
            .setContentText("Open second activiy")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setStyle(NotificationCompat.InboxStyle()
                .addLine("asd asd asd asd asd asd asd asd asd asd asd asd asd asd asd asd asd asd asd asd asd asd asd asd asd asd asd asd asd asd asd asd")
                .addLine("bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla")
                .addLine("cvb cvb cvb cvb cvb cvb cvb cvb cvb cvb cvb cvb cvb cvb cvb cvb cvb cvb cvb cvb cvb cvb cvb cvb cvb cvb cvb cvb cvb cvb cvb cvb"))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        return builder
    }

    private fun createMultipleLinesNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "channel_name"
            val description = "channel_description"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("channel_id3", name, importance)
            channel.description = description
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager!!.createNotificationChannel(channel)
        }
    }

    private fun createImagesNotification(): NotificationCompat.Builder {
        val intent = Intent(this, SecondActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        val builder = NotificationCompat.Builder(this, "channel_id4")
            .setSmallIcon(R.drawable.notification_template_icon_bg)
            .setContentTitle("Multiple lines notification")
            .setContentText("Open second activiy")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.abc_ic_arrow_drop_right_black_24dp))
            .setStyle(NotificationCompat.BigPictureStyle()
                .bigPicture(BitmapFactory.decodeResource(resources, R.drawable.abc_ic_menu_cut_mtrl_alpha)))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        return builder
    }

    private fun createImagesNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "channel_name"
            val description = "channel_description"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("channel_id4", name, importance)
            channel.description = description
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager!!.createNotificationChannel(channel)
        }
    }


    companion object NotificationId {
        val SIMPLE_NOTIFICATION = 0
        val EXPANDABLE_NOTIFICATION = 1
        val MULTIPLE_LINES_NOTIFICATION = 2
        val IMAGES_NOTIFICATION = 3
    }
}
