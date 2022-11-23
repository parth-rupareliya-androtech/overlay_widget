package com.parth.overlay.overlay_widget

import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import io.flutter.plugin.common.BasicMessageChannel

object WindowSetup {
    var height = WindowManager.LayoutParams.MATCH_PARENT
    var width = WindowManager.LayoutParams.MATCH_PARENT
    var flag = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
    var gravity = Gravity.CENTER
    var messenger: BasicMessageChannel<Any>? = null
    var overlayTitle = "Overlay is activated"
    var overlayContent = "Tap to edit settings or disable"
    var positionGravity = "none"
    var notificationVisibility = NotificationCompat.VISIBILITY_PRIVATE
    var enableDrag = false
    fun setNotificationVisibility(name: String) {
        if (name.equals("visibilityPublic", ignoreCase = true)) {
            notificationVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }
        if (name.equals("visibilitySecret", ignoreCase = true)) {
            notificationVisibility = NotificationCompat.VISIBILITY_SECRET
        }
        if (name.equals("visibilityPrivate", ignoreCase = true)) {
            notificationVisibility = NotificationCompat.VISIBILITY_PRIVATE
        }
    }

    fun setFlag(name: String) {
        if (name.equals("flagNotFocusable", ignoreCase = true) || name.equals("defaultFlag", ignoreCase = true)) {
            flag = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        if (name.equals("flagNotTouchable", ignoreCase = true) || name.equals("clickThrough", ignoreCase = true)) {
            flag = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        }
        if (name.equals("flagNotTouchModal", ignoreCase = true) || name.equals("focusPointer", ignoreCase = true)) {
            flag = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        }
    }

    fun setGravityFromAlignment(alignment: String) {
        if (alignment.equals("topLeft", ignoreCase = true)) {
            gravity = Gravity.TOP or Gravity.LEFT
            return
        }
        if (alignment.equals("topCenter", ignoreCase = true)) {
            gravity = Gravity.TOP
        }
        if (alignment.equals("topRight", ignoreCase = true)) {
            gravity = Gravity.TOP or Gravity.RIGHT
            return
        }
        if (alignment.equals("centerLeft", ignoreCase = true)) {
            gravity = Gravity.CENTER or Gravity.LEFT
            return
        }
        if (alignment.equals("center", ignoreCase = true)) {
            gravity = Gravity.CENTER
        }
        if (alignment.equals("centerRight", ignoreCase = true)) {
            gravity = Gravity.CENTER or Gravity.RIGHT
            return
        }
        if (alignment.equals("bottomLeft", ignoreCase = true)) {
            gravity = Gravity.BOTTOM or Gravity.LEFT
            return
        }
        if (alignment.equals("bottomCenter", ignoreCase = true)) {
            gravity = Gravity.BOTTOM
        }
        if (alignment.equals("bottomRight", ignoreCase = true)) {
            gravity = Gravity.BOTTOM or Gravity.RIGHT
            return
        }
    }
}