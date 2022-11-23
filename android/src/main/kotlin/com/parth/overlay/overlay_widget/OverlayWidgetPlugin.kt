package com.parth.overlay.overlay_widget

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.NonNull
import com.parth.overlay.overlay_widget.WindowSetup.setFlag
import com.parth.overlay.overlay_widget.WindowSetup.setGravityFromAlignment
import com.parth.overlay.overlay_widget.WindowSetup.setNotificationVisibility
import io.flutter.FlutterInjector
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.FlutterEngineGroup
import io.flutter.embedding.engine.dart.DartExecutor.DartEntrypoint
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.*
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result


class OverlayWidgetPlugin : FlutterPlugin, ActivityAware, MethodCallHandler, PluginRegistry.ActivityResultListener, BasicMessageChannel.MessageHandler<Any> {
    private lateinit var channel: MethodChannel
    private var context: Context? = null
    private var mActivity: Activity? = null
    private var messenger: BasicMessageChannel<Any>? = null
    private var pendingResult: Result? = null
    val REQUEST_CODE_FOR_OVERLAY_PERMISSION = 1248

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        this.context = flutterPluginBinding.applicationContext
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, OverlayConstants.CHANNEL_TAG)
        channel.setMethodCallHandler(this)

        messenger = BasicMessageChannel(
            flutterPluginBinding.binaryMessenger, OverlayConstants.MESSENGER_TAG,
            JSONMessageCodec.INSTANCE
        )
        messenger?.setMessageHandler(this)

        WindowSetup.messenger = messenger
        WindowSetup.messenger!!.setMessageHandler(this)
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        pendingResult = result
        if (call.method.equals("checkPermission")) {
            result.success(checkOverlayPermission())
        } else if (call.method.equals("requestPermission")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                intent.data = Uri.parse("package:" + mActivity!!.packageName)
                mActivity!!.startActivityForResult(intent, REQUEST_CODE_FOR_OVERLAY_PERMISSION)
            } else {
                result.success(true)
            }
        } else if (call.method.equals("showOverlay")) {
            if (!checkOverlayPermission()) {
                result.error("PERMISSION", "overlay permission is not enabled", null)
                return
            }
            val height: Int? = call.argument("height")
            val width: Int? = call.argument("width")
            val alignment: String? = call.argument("alignment")
            val flag: String? = call.argument("flag")
            val overlayTitle: String? = call.argument("overlayTitle")
            val overlayContent: String? = call.argument("overlayContent")
            val notificationVisibility: String? = call.argument("notificationVisibility")
            val enableDrag: Boolean = call.argument("enableDrag")!!
            val positionGravity: String? = call.argument("positionGravity")
            WindowSetup.width = width ?: -1
            WindowSetup.height = height ?: -1
            WindowSetup.enableDrag = enableDrag
            setGravityFromAlignment(alignment ?: "center")
            setFlag(flag ?: "flagNotFocusable")
            if (overlayTitle != null) {
                WindowSetup.overlayTitle = overlayTitle
            }
            WindowSetup.overlayContent = overlayContent ?: ""
            if (positionGravity != null) {
                WindowSetup.positionGravity = positionGravity
            }
            if (notificationVisibility != null) {
                setNotificationVisibility(notificationVisibility)
            }
            val intent = Intent(context, OverlayService::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            context!!.startService(intent)
            result.success(null)
        } else if (call.method.equals("isOverlayActive")) {
            result.success(OverlayService.isRunning)
            return
        } else if (call.method.equals("closeOverlay")) {
            if (OverlayService.isRunning) {
                val i = Intent(context, OverlayService::class.java)
                i.putExtra(OverlayService.INTENT_EXTRA_IS_CLOSE_WINDOW, true)
                context!!.startService(i)
                result.success(true)
            }
            return
        } else {
            result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        WindowSetup.messenger?.setMessageHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        mActivity = binding.activity
        val enn = FlutterEngineGroup(context!!)
        val dEntry = DartEntrypoint(
            FlutterInjector.instance().flutterLoader().findAppBundlePath(),
            "overlayMain"
        )
        val engine = enn.createAndRunEngine(context!!, dEntry)
        FlutterEngineCache.getInstance().put(OverlayConstants.CACHED_TAG, engine)
        binding.addActivityResultListener(this)
    }

    override fun onDetachedFromActivityForConfigChanges() {
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        this.mActivity = binding.activity
    }

    override fun onDetachedFromActivity() {}

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == REQUEST_CODE_FOR_OVERLAY_PERMISSION) {
            pendingResult?.success(checkOverlayPermission())
            return true
        }
        return false
    }

    override fun onMessage(message: Any?, reply: BasicMessageChannel.Reply<Any>) {
        val overlayMessageChannel: BasicMessageChannel<Any>? = FlutterEngineCache.getInstance()[OverlayConstants.CACHED_TAG]?.dartExecutor?.let {
            BasicMessageChannel(
                it,
                OverlayConstants.MESSENGER_TAG, JSONMessageCodec.INSTANCE
            )
        }
        overlayMessageChannel?.send(message, reply)
    }

    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true
    }
}
