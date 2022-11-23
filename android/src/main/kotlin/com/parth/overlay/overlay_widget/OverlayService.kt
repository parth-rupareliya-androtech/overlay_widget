package com.parth.overlay.overlay_widget

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import io.flutter.embedding.android.FlutterTextureView
import io.flutter.embedding.android.FlutterView
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.plugin.common.BasicMessageChannel
import io.flutter.plugin.common.JSONMessageCodec
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.util.*
import kotlin.math.abs


object OverlayConstants {
    const val CACHED_TAG = "myCachedEngine"
    const val CHANNEL_TAG = "x-slayer/overlay_channel"
    const val OVERLAY_TAG = "x-slayer/overlay"
    const val MESSENGER_TAG = "x-slayer/overlay_messenger"
    const val CHANNEL_ID = "Overlay Channel"
    const val NOTIFICATION_ID = 4579
}

class OverlayService : Service(), View.OnTouchListener {
    private var windowManager: WindowManager? = null
    private var flutterView: FlutterView? = null
    private val flutterChannel = MethodChannel(FlutterEngineCache.getInstance()[OverlayConstants.CACHED_TAG]!!.dartExecutor, OverlayConstants.OVERLAY_TAG)
    private val overlayMessageChannel =
        BasicMessageChannel(FlutterEngineCache.getInstance()[OverlayConstants.CACHED_TAG]!!.dartExecutor, OverlayConstants.MESSENGER_TAG, JSONMessageCodec.INSTANCE)
    private val clickableFlag = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
    private val mAnimationHandler: Handler = Handler()
    private var lastX = 0f
    private var lastY = 0f
    private var lastYPosition = 0
    private var dragging = false
    private val szWindow: Point = Point()
    private var mTrayAnimationTimer: Timer? = null
    private var mTrayTimerTask: TrayAnimationTimerTask? = null

    @Nullable
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun onDestroy() {
        Log.d("OverLay", "Destroying the overlay window service")
        isRunning = false
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(OverlayConstants.NOTIFICATION_ID)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val isCloseWindow = intent.getBooleanExtra(INTENT_EXTRA_IS_CLOSE_WINDOW, false)
        if (isCloseWindow) {
            if (windowManager != null) {
                windowManager!!.removeView(flutterView)
                windowManager = null
                stopSelf()
            }
            isRunning = false
            return START_STICKY
        }
        if (windowManager != null) {
            windowManager!!.removeView(flutterView)
            windowManager = null
            stopSelf()
        }
        isRunning = true
        Log.d("onStartCommand", "Service started")
        val engine = FlutterEngineCache.getInstance()[OverlayConstants.CACHED_TAG]
        engine!!.lifecycleChannel.appIsResumed()
        flutterView = FlutterView(applicationContext, FlutterTextureView(applicationContext))
        FlutterEngineCache.getInstance()[OverlayConstants.CACHED_TAG]?.let { flutterView?.attachToFlutterEngine(it) }
        flutterView?.fitsSystemWindows = true
        flutterView?.isFocusable = true
        flutterView?.isFocusableInTouchMode = true
        flutterView?.setBackgroundColor(Color.TRANSPARENT)
        flutterChannel.setMethodCallHandler { call: MethodCall, result: MethodChannel.Result ->
            if (call.method == "updateFlag") {
                val flag = call.argument<Any>("flag").toString()
                updateOverlayFlag(result, flag)
            } else if (call.method == "resizeOverlay") {
                val width = call.argument<Int>("width")!!
                val height = call.argument<Int>("height")!!
                resizeOverlay(width, height, result)
            }
        }
        overlayMessageChannel.setMessageHandler { message: Any?, _: BasicMessageChannel.Reply<Any?>? ->
            WindowSetup.messenger?.send(message)
        }
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager?
        val LAYOUT_TYPE: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            windowManager!!.defaultDisplay.getSize(szWindow)
        } else {
            val w = windowManager!!.defaultDisplay.width
            val h = windowManager!!.defaultDisplay.height
            szWindow.set(w, h)
        }
        val params = WindowManager.LayoutParams(
            WindowSetup.width,
            WindowSetup.height,
            LAYOUT_TYPE,
            WindowSetup.flag or WindowManager.LayoutParams.FLAG_SECURE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && WindowSetup.flag === clickableFlag) {
            params.alpha = MAXIMUM_OPACITY_ALLOWED_FOR_S_AND_HIGHER
        }
        params.gravity = WindowSetup.gravity
        flutterView?.setOnTouchListener(this)
        windowManager!!.addView(flutterView, params)
        return START_STICKY
    }

    private fun updateOverlayFlag(result: MethodChannel.Result, flag: String) {
        if (windowManager != null) {
            WindowSetup.setFlag(flag)
            val params = flutterView?.layoutParams as WindowManager.LayoutParams
            params.flags = WindowSetup.flag
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && WindowSetup.flag === clickableFlag) {
                params.alpha = MAXIMUM_OPACITY_ALLOWED_FOR_S_AND_HIGHER
            }
            windowManager!!.updateViewLayout(flutterView, params)
            result.success(true)
        } else {
            result.success(false)
        }
    }

    private fun resizeOverlay(width: Int, height: Int, result: MethodChannel.Result) {
        if (windowManager != null) {
            val params = flutterView?.layoutParams as WindowManager.LayoutParams
            params.width = width
            params.height = height
            windowManager!!.updateViewLayout(flutterView, params)
            result.success(true)
        } else {
            result.success(false)
        }
    }

    override fun onCreate() {
        createNotificationChannel()
        val notificationIntent = Intent(this, OverlayWidgetPlugin::class.java)
        val pendingFlags: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, pendingFlags
        )
        val notifyIcon = getDrawableResourceId("mipmap", "launcher")
        val notification: Notification = NotificationCompat.Builder(this, OverlayConstants.CHANNEL_ID)
            .setContentTitle(WindowSetup.overlayTitle)
            .setContentText(WindowSetup.overlayContent)
            .setSmallIcon(if (notifyIcon == 0) R.drawable.notification_icon else notifyIcon)
            .setContentIntent(pendingIntent)
            .setVisibility(WindowSetup.notificationVisibility)
            .build()
        startForeground(OverlayConstants.NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                OverlayConstants.CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager: NotificationManager = getSystemService(NotificationManager::class.java)!!
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun getDrawableResourceId(resType: String, name: String): Int {
        return applicationContext.resources.getIdentifier(String.format("ic_%s", name), resType, applicationContext.packageName)
    }

    override fun onTouch(view: View?, event: MotionEvent): Boolean {
        if (windowManager != null && WindowSetup.enableDrag) {
            val params = flutterView?.layoutParams as WindowManager.LayoutParams
            Log.e("TAG", event.rawX.toString())
            Log.e("TAG", event.rawY.toString())
            Log.e("TAG", event.x.toString())
            Log.e("TAG", event.y.toString())
            Log.e("TAG", params.x.toString())
            Log.e("TAG", params.y.toString())
            Log.e("TAG", dragging.toString())
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dragging = false
                    lastX = event.rawX
                    lastY = event.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - lastX
                    val dy = event.rawY - lastY
                    if (!dragging && dx * dx + dy * dy < 25) {
                        return false
                    }
                    lastX = event.rawX
                    lastY = event.rawY
                    val xx = params.x + dx.toInt()
                    val yy = params.y + dy.toInt()
                    params.x = xx
                    params.y = yy
                    windowManager!!.updateViewLayout(flutterView, params)
                    dragging = true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {

                    lastYPosition = params.y
                    if (WindowSetup.positionGravity !== "none") {
                        windowManager!!.updateViewLayout(flutterView, params)
                        mTrayTimerTask = TrayAnimationTimerTask()
                        mTrayAnimationTimer = Timer()
                        mTrayAnimationTimer?.schedule(mTrayTimerTask, 0, 25)
                    }
                    return dragging
                }
                else -> return false
            }
            return false
        }
        return false
    }

    private inner class TrayAnimationTimerTask : TimerTask() {
        var mDestX = 0
        var mDestY: Int
        var params = flutterView?.layoutParams as WindowManager.LayoutParams
        override fun run() {
            mAnimationHandler.post {
                params.x = 2 * (params.x - mDestX) / 3 + mDestX
                params.y = 2 * (params.y - mDestY) / 3 + mDestY
                windowManager!!.updateViewLayout(flutterView, params)
                if (abs(params.x - mDestX) < 2 && abs(params.y - mDestY) < 2) {
                    this@TrayAnimationTimerTask.cancel()
                    mTrayAnimationTimer?.cancel()
                }
            }
        }

        init {
            mDestY = lastYPosition
            when (WindowSetup.positionGravity) {
                "auto" -> mDestX = if (params.x + flutterView!!.width / 2 <= szWindow.x / 2) 0 else szWindow.x - flutterView!!.width
                "left" -> mDestX = 0
                "right" -> mDestX = szWindow.x - flutterView!!.width
                else -> {
                    mDestX = params.x
                    mDestY = params.y
                }
            }
        }
    }

    companion object {
        const val INTENT_EXTRA_IS_CLOSE_WINDOW = "IsCloseWindow"
        var isRunning = false
        private const val MAXIMUM_OPACITY_ALLOWED_FOR_S_AND_HIGHER = 0.8f
    }
}