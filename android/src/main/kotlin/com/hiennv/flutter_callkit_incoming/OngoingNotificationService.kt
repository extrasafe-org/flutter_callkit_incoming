package com.hiennv.flutter_callkit_incoming

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.text.TextUtils
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.PermissionChecker
import androidx.core.graphics.toColorInt
import com.hiennv.flutter_callkit_incoming.widgets.CircleTransform
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import okhttp3.OkHttpClient

class OngoingNotificationService : Service() {
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private var notificationViews: RemoteViews? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.extras != null) {
            showOngoingCallNotification(intent.extras!!)
        } else {
            stopSelf()
        }

        return START_STICKY
    }

    private fun showOngoingCallNotification(data: Bundle) {
        val cameraPermission =
            PermissionChecker.checkSelfPermission(this, Manifest.permission.CAMERA)
        val microphonePermission =
            PermissionChecker.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        val permissionGrantedFlag = PermissionChecker.PERMISSION_GRANTED

        // Without neither camera nor audio permissions, the service cannot run in the foreground
        if (cameraPermission != permissionGrantedFlag && microphonePermission != permissionGrantedFlag) {
            return stopSelf()
        }

        val onGoingNotificationId = data.getInt(
            CallkitConstants.EXTRA_CALLKIT_MISSED_CALL_ID,
            data.getString(CallkitConstants.EXTRA_CALLKIT_ID, "callkit_incoming").hashCode() + 999
        )

        notificationBuilder = NotificationCompat.Builder(
            this, CallkitNotificationManager.NOTIFICATION_CHANNEL_ID_ONGOING
        )
        notificationBuilder.setChannelId(CallkitNotificationManager.NOTIFICATION_CHANNEL_ID_ONGOING)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            notificationBuilder.setCategory(Notification.CATEGORY_CALL)
        }
        val textCalling = data.getString(CallkitConstants.EXTRA_CALLKIT_CALLING_SUBTITLE, "")
        if (textCalling.isNotBlank()) {
            notificationBuilder.setSubText(textCalling)
        }

        notificationBuilder.setSmallIcon(R.drawable.ic_accept)
        val isCustomNotification =
            data.getBoolean(CallkitConstants.EXTRA_CALLKIT_IS_CUSTOM_NOTIFICATION, false)
        if (isCustomNotification) {
            notificationViews =
                RemoteViews(packageName, R.layout.layout_custom_ongoing_notification)
            notificationViews?.setTextViewText(
                R.id.tvNameCaller, data.getString(CallkitConstants.EXTRA_CALLKIT_NAME_CALLER, "")
            )

            notificationViews?.setOnClickPendingIntent(
                R.id.llHangup, getHangupPendingIntent(onGoingNotificationId, data)
            )
            val isShowHangup = data.getBoolean(
                CallkitConstants.EXTRA_CALLKIT_CALLING_HANG_UP_SHOW, true
            )
            notificationViews?.setViewVisibility(
                R.id.llHangup, if (isShowHangup) View.VISIBLE else View.GONE
            )
            val textHangup = data.getString(CallkitConstants.EXTRA_CALLKIT_CALLING_HANG_UP_TEXT, "")
            notificationViews?.setTextViewText(
                R.id.tvHangUp,
                if (TextUtils.isEmpty(textHangup)) getString(R.string.text_hang_up) else textHangup
            )
            val notificationContent =
                data.getString(CallkitConstants.EXTRA_CALLKIT_CALLING_CONTENT, "")
            notificationViews?.setTextViewText(
                R.id.tvTapOpen,
                if (TextUtils.isEmpty(notificationContent)) getString(R.string.text_tab_open) else notificationContent
            )

            val avatarUrl = getAvatarUrl(data)
            if (avatarUrl != null) {
                val headers =
                    data.getSerializable(CallkitConstants.EXTRA_CALLKIT_HEADERS) as HashMap<String, Any?>

                getPicassoInstance(this, headers).load(avatarUrl).transform(CircleTransform())
                    .into(createAvatarTargetCustom(onGoingNotificationId))
            }
            notificationBuilder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
            notificationBuilder.setCustomContentView(notificationViews)
            notificationBuilder.setCustomBigContentView(notificationViews)
        } else {
            notificationBuilder.setContentTitle(
                data.getString(
                    CallkitConstants.EXTRA_CALLKIT_NAME_CALLER, ""
                )
            )
            notificationBuilder.setContentText(
                data.getString(
                    CallkitConstants.EXTRA_CALLKIT_CALLING_CONTENT, ""
                )
            )
            val avatarUrl = getAvatarUrl(data)
            if (avatarUrl != null) {
                val headers =
                    data.getSerializable(CallkitConstants.EXTRA_CALLKIT_HEADERS) as HashMap<String, Any?>

                getPicassoInstance(this@OngoingNotificationService, headers).load(avatarUrl)
                    .into(createAvatarTargetDefault(onGoingNotificationId))
            }
            val isShowHangup = data.getBoolean(
                CallkitConstants.EXTRA_CALLKIT_CALLING_HANG_UP_SHOW, true
            )
            if (isShowHangup) {
                val textHangup =
                    data.getString(CallkitConstants.EXTRA_CALLKIT_CALLING_HANG_UP_TEXT, "")
                val hangUpAction: NotificationCompat.Action = NotificationCompat.Action.Builder(
                    R.drawable.transparent,
                    if (TextUtils.isEmpty(textHangup)) this.getString(R.string.text_hang_up) else textHangup,
                    getHangupPendingIntent(onGoingNotificationId, data)
                ).build()
                notificationBuilder.addAction(hangUpAction)
            }
        }
        notificationBuilder.priority = NotificationManager.IMPORTANCE_LOW
        notificationBuilder.setSound(null)
        notificationBuilder.setContentIntent(getAppPendingIntent(onGoingNotificationId, data))

        // set extra callkit action color
        data.getString(CallkitConstants.EXTRA_CALLKIT_ACTION_COLOR, "#4CAF50")?.toColorInt()?.let {
            try {
                notificationBuilder.color = it
            } catch (_: Exception) {
                // noop
            }
        }
        notificationBuilder.setOngoing(true)
        val notification = notificationBuilder.build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // if the camera permission is granted add it
            if (cameraPermission == permissionGrantedFlag) {
                startForeground(
                    onGoingNotificationId,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA,
                )
            }

            // always request the microphone service permission to be able to record audio while in background
            startForeground(
                onGoingNotificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
            )
        } else {
            startForeground(onGoingNotificationId, notification)
        }
    }

    private fun getHangupPendingIntent(notificationId: Int, data: Bundle): PendingIntent {
        val endedIntent = CallkitIncomingBroadcastReceiver.getIntentEnded(this, data)
        return PendingIntent.getBroadcast(this, notificationId, endedIntent, getFlagPendingIntent())
    }


    private fun getAppPendingIntent(notificationId: Int, data: Bundle): PendingIntent {
        return PendingIntent.getActivity(
            this,
            notificationId,
            AppUtils.getAppIntent(this, data = data),
            getFlagPendingIntent(),
        )
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    private fun getFlagPendingIntent(): Int {
        return PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    }

    private fun getPicassoInstance(context: Context, headers: HashMap<String, Any?>): Picasso {
        val client = OkHttpClient.Builder().addNetworkInterceptor { chain ->
            val newRequestBuilder: okhttp3.Request.Builder = chain.request().newBuilder()
            for ((key, value) in headers) {
                newRequestBuilder.addHeader(key, value.toString())
            }
            chain.proceed(newRequestBuilder.build())
        }.build()
        return Picasso.Builder(context).downloader(OkHttp3Downloader(client)).build()
    }

    private fun createAvatarTargetCustom(notificationId: Int): Target {
        return object : Target {
            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val canPostNotifications = PermissionChecker.checkSelfPermission(
                        this@OngoingNotificationService, Manifest.permission.POST_NOTIFICATIONS
                    )

                    if (canPostNotifications != PermissionChecker.PERMISSION_GRANTED) return
                }

                notificationViews?.setImageViewBitmap(R.id.ivAvatar, bitmap)
                notificationViews?.setViewVisibility(R.id.ivAvatar, View.VISIBLE)
                getNotificationManager().notify(notificationId, notificationBuilder.build())
            }

            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {}

            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}
        }
    }

    private fun createAvatarTargetDefault(notificationId: Int): Target {
        return object : Target {
            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val canPostNotifications = PermissionChecker.checkSelfPermission(
                        this@OngoingNotificationService, Manifest.permission.POST_NOTIFICATIONS
                    )

                    if (canPostNotifications != PermissionChecker.PERMISSION_GRANTED) return
                }

                notificationBuilder.setLargeIcon(bitmap)
                getNotificationManager().notify(notificationId, notificationBuilder.build())
            }

            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {}

            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}
        }
    }

    private fun getAvatarUrl(bundle: Bundle): String? {
        val bundleAvatarUrl = bundle.getString(CallkitConstants.EXTRA_CALLKIT_AVATAR, "")

        return if (bundleAvatarUrl.isNotBlank()) {
            val startsWithHttp = bundleAvatarUrl.startsWith("http://", true)
            val startsWithHttps = bundleAvatarUrl.startsWith("https://", true)

            if (startsWithHttp || startsWithHttps) {
                bundleAvatarUrl
            } else {
                "file:///android_asset/flutter_assets/$bundleAvatarUrl"
            }
        } else {
            null
        }
    }

    private fun getNotificationManager(): NotificationManagerCompat {
        return NotificationManagerCompat.from(this)
    }
}

