package ru.tohaman.nls

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.IBinder
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.toast
import java.util.*

class NLS_Service : Service() {

    val LOG_TAG = "NLS_Service - "
    private var mBinder: IBinder? = null

    private lateinit var mHandler: Handler
    private lateinit var mRunnable: Runnable
    private lateinit var actionBroadcastReceiver : ActionBroadcastReceiver
    private var notificationManager: NotificationManager? = null
    private var oldReceivedText = ""
    private var counter = 0



    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        // Send a notification that service is started
        toast("Service started.")

        // Do a periodic task
//        mHandler = Handler()
//        mRunnable = Runnable { showRandomNumber() }
//        mHandler.postDelayed(mRunnable, 5000)


        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Finally we register a receiver to tell the MainActivity when a notification has been received
        // Регистрируем броадкастовый ресивер, который принимает сообщения от сервиса, когда получен нотификейшен
        // от какого-либо приложения
        actionBroadcastReceiver = ActionBroadcastReceiver()
        val intentFilter = IntentFilter()
        intentFilter.addAction("ru.tohaman.nls")
        registerReceiver(actionBroadcastReceiver, intentFilter)

        return Service.START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        toast("Service destroyed.")
        unregisterReceiver(actionBroadcastReceiver)
//        mHandler.removeCallbacks(mRunnable)
    }


    inner class ActionBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            counter += 1
            var receivedTextFromShazam = intent.getStringExtra("Notification Code")
//            toast ("Получено сообщение")
            if (receivedTextFromShazam.startsWith("Сейчас:")) {

                receivedTextFromShazam = receivedTextFromShazam.substringAfter(' ')
                val artist = receivedTextFromShazam.substringBefore('—')
                val song = receivedTextFromShazam.substringAfter('—')
                if (oldReceivedText != receivedTextFromShazam) {
                    createNotification("ru.tohaman.nls", artist, song)
                    oldReceivedText = receivedTextFromShazam
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun createNotification (id: String, name: String, description: String) {
        val notificationID = 101

        val notification = Notification.Builder( this@NLS_Service)
            .setContentTitle(name)
            .setContentText(description)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        notificationManager?.notify(notificationID, notification)
    }


    // Custom method to do a task
//    private fun showRandomNumber() {
//        val rand = Random()
//        val number = rand.nextInt(100)
//        toast("Random Number : $number")
//        mHandler.postDelayed(mRunnable, 5000)
//    }
}
