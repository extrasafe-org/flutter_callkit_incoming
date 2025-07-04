package com.hiennv.flutter_callkit_incoming

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.View
import android.widget.RemoteViews
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.graphics.toColorInt
import com.hiennv.flutter_callkit_incoming.widgets.CircleTransform
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import okhttp3.OkHttpClient


class CallkitNotificationManager(private val context: Context) {
    companion object {
        const val PERMISSION_NOTIFICATION_REQUEST_CODE = 6969

        const val EXTRA_TIME_START_CALL = "EXTRA_TIME_START_CALL"

        private const val NOTIFICATION_CHANNEL_ID_INCOMING = "callkit_incoming_channel_id"
        const val NOTIFICATION_CHANNEL_ID_ONGOING = "callkit_ongoing_channel_id"
        private const val NOTIFICATION_CHANNEL_ID_MISSED = "callkit_missed_channel_id"
    }

    private lateinit var notificationBuilder: NotificationCompat.Builder
    private var notificationViews: RemoteViews? = null
    private var notificationSmallViews: RemoteViews? = null
    private var notificationId: Int = 9696
    private var dataNotificationPermission: Map<String, Any> = HashMap()

    @SuppressLint("MissingPermission")
    private fun createAvatarTargetDefault(notificationId: Int): Target {
        return object : Target {
            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                notificationBuilder.setLargeIcon(bitmap)
                getNotificationManager().notify(notificationId, notificationBuilder.build())
            }

            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {}

            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}
        }
    }

    @SuppressLint("MissingPermission")
    private fun createAvatarTargetCustom(notificationId: Int): Target {
        return object : Target {
            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                notificationViews?.setImageViewBitmap(R.id.ivAvatar, bitmap)
                notificationViews?.setViewVisibility(R.id.ivAvatar, View.VISIBLE)
                notificationSmallViews?.setImageViewBitmap(R.id.ivAvatar, bitmap)
                notificationSmallViews?.setViewVisibility(R.id.ivAvatar, View.VISIBLE)
                getNotificationManager().notify(notificationId, notificationBuilder.build())
            }

            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {}

            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}
        }
    }

    @SuppressLint("MissingPermission")
    fun showIncomingNotification(data: Bundle) {
        data.putLong(EXTRA_TIME_START_CALL, System.currentTimeMillis())

        notificationId =
            data.getString(CallkitConstants.EXTRA_CALLKIT_ID, "callkit_incoming").hashCode()
        val activityPendingIntent = getActivityPendingIntent(notificationId, data)
        val timeoutPendingIntent = getTimeOutPendingIntent(notificationId, data)

        createNotificationChannel(data)
        notificationBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID_INCOMING)
            .setAutoCancel(false)
            .setChannelId(NOTIFICATION_CHANNEL_ID_INCOMING)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setWhen(0)
            .setTimeoutAfter(data.getLong(CallkitConstants.EXTRA_CALLKIT_DURATION, 0L))
            .setOnlyAlertOnce(true)
            .setSound(null)
            .setContentIntent(activityPendingIntent)
            .setDeleteIntent(timeoutPendingIntent)
            .setOngoing(true)

        // set call accept button icon
        val callType = data.getInt(CallkitConstants.EXTRA_CALLKIT_TYPE, -1)
        var smallIcon = context.applicationInfo.icon

        if (callType > 0) {
            smallIcon = R.drawable.ic_video
        } else {
            if (smallIcon >= 0) {
                smallIcon = R.drawable.ic_accept
            }
        }

        notificationBuilder.setSmallIcon(smallIcon)

        // set action color
        data.getString(CallkitConstants.EXTRA_CALLKIT_ACTION_COLOR, "#4CAF50")?.toColorInt()?.let {
            try {
                notificationBuilder.color = it
            } catch (_: Exception) {
                // noop
            }
        }
        val isCustomNotification =
            data.getBoolean(CallkitConstants.EXTRA_CALLKIT_IS_CUSTOM_NOTIFICATION, false)
        val isCustomSmallExNotification =
            data.getBoolean(CallkitConstants.EXTRA_CALLKIT_IS_CUSTOM_SMALL_EX_NOTIFICATION, false)

        if (isCustomNotification) {
            notificationViews =
                RemoteViews(context.packageName, R.layout.layout_custom_notification)
            initNotificationViews(notificationViews!!, data)

            // render a different layout of notification in case this is a Samsung phone
            if ((Build.MANUFACTURER.equals(
                    "Samsung", ignoreCase = true
                ) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) || isCustomSmallExNotification
            ) {
                notificationSmallViews =
                    RemoteViews(context.packageName, R.layout.layout_custom_small_ex_notification)
                initNotificationViews(notificationSmallViews!!, data)
            } else {
                notificationSmallViews =
                    RemoteViews(context.packageName, R.layout.layout_custom_small_notification)
                initNotificationViews(notificationSmallViews!!, data)
            }

            notificationBuilder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(notificationSmallViews)
                .setCustomBigContentView(notificationViews)
                .setCustomHeadsUpContentView(notificationSmallViews)
        } else {
            notificationBuilder.setContentText(
                data.getString(
                    CallkitConstants.EXTRA_CALLKIT_HANDLE, ""
                )
            )

            val avatarUrl = getAvatarUrl(data)
            if (avatarUrl != null) {
                val headers =
                    data.getSerializable(CallkitConstants.EXTRA_CALLKIT_HEADERS) as HashMap<String, Any?>
                getPicassoInstance(context, headers).load(avatarUrl)
                    .into(createAvatarTargetDefault(notificationId))
            }

            val caller = data.getString(CallkitConstants.EXTRA_CALLKIT_NAME_CALLER, "")
            val declinePendingIntent = getDeclinePendingIntent(notificationId, data)
            val acceptPendingIntent = getAcceptPendingIntent(notificationId, data)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val person = Person.Builder().setName(caller).setImportant(
                    data.getBoolean(CallkitConstants.EXTRA_CALLKIT_IS_IMPORTANT, false)
                ).setBot(data.getBoolean(CallkitConstants.EXTRA_CALLKIT_IS_BOT, false)).build()
                notificationBuilder.setStyle(
                    NotificationCompat.CallStyle.forIncomingCall(
                        person,
                        declinePendingIntent,
                        acceptPendingIntent,
                    ).setIsVideo(callType > 0)
                )
            } else {
                notificationBuilder.setContentTitle(caller)
                val textDecline = data.getString(CallkitConstants.EXTRA_CALLKIT_TEXT_DECLINE, "")
                val declineAction: NotificationCompat.Action = NotificationCompat.Action.Builder(
                    R.drawable.ic_decline,
                    if (TextUtils.isEmpty(textDecline)) context.getString(R.string.text_decline) else textDecline,
                    declinePendingIntent
                ).build()
                notificationBuilder.addAction(declineAction)
                val textAccept = data.getString(CallkitConstants.EXTRA_CALLKIT_TEXT_ACCEPT, "")
                val acceptAction: NotificationCompat.Action = NotificationCompat.Action.Builder(
                    R.drawable.ic_accept,
                    if (TextUtils.isEmpty(textDecline)) context.getString(R.string.text_accept) else textAccept,
                    acceptPendingIntent
                ).build()

                notificationBuilder.addAction(acceptAction)
            }
        }
        val notification = notificationBuilder
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setFullScreenIntent(activityPendingIntent, true)
            .build()
        notification.flags = Notification.FLAG_INSISTENT
        getNotificationManager().notify(notificationId, notification)
    }

    private fun initNotificationViews(remoteViews: RemoteViews, data: Bundle) {
        remoteViews.setTextViewText(
            R.id.tvNameCaller, data.getString(CallkitConstants.EXTRA_CALLKIT_NAME_CALLER, "")
        )
        val isShowCallID = data.getBoolean(CallkitConstants.EXTRA_CALLKIT_IS_SHOW_CALL_ID, false)
        if (isShowCallID) {
            remoteViews.setTextViewText(
                R.id.tvNumber, data.getString(CallkitConstants.EXTRA_CALLKIT_HANDLE, "")
            )
        }
        remoteViews.setOnClickPendingIntent(
            R.id.llDecline, getDeclinePendingIntent(notificationId, data)
        )
        val textDecline = data.getString(CallkitConstants.EXTRA_CALLKIT_TEXT_DECLINE, "")
        remoteViews.setTextViewText(
            R.id.tvDecline,
            if (TextUtils.isEmpty(textDecline)) context.getString(R.string.text_decline) else textDecline
        )
        remoteViews.setOnClickPendingIntent(
            R.id.llAccept, getAcceptPendingIntent(notificationId, data)
        )
        val textAccept = data.getString(CallkitConstants.EXTRA_CALLKIT_TEXT_ACCEPT, "")
        remoteViews.setTextViewText(
            R.id.tvAccept,
            if (TextUtils.isEmpty(textAccept)) context.getString(R.string.text_accept) else textAccept
        )

        val avatarUrl = getAvatarUrl(data)
        if (avatarUrl != null) {
            val headers =
                data.getSerializable(CallkitConstants.EXTRA_CALLKIT_HEADERS) as HashMap<String, Any?>
            getPicassoInstance(context, headers).load(avatarUrl).transform(CircleTransform())
                .into(createAvatarTargetCustom(notificationId))
        }
    }

    @SuppressLint("MissingPermission")
    fun showMissCallNotification(data: Bundle) {
        val missedNotificationId = data.getInt(
            CallkitConstants.EXTRA_CALLKIT_MISSED_CALL_ID,
            data.getString(CallkitConstants.EXTRA_CALLKIT_ID, "callkit_incoming").hashCode() + 1
        )
        createNotificationChannel(data);
        val missedCallSound: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val typeCall = data.getInt(CallkitConstants.EXTRA_CALLKIT_TYPE, -1)
        var smallIcon = context.applicationInfo.icon
        if (typeCall > 0) {
            smallIcon = R.drawable.ic_video_missed
        } else {
            if (smallIcon >= 0) {
                smallIcon = R.drawable.ic_call_missed
            }
        }
        notificationBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID_MISSED)
        notificationBuilder.setChannelId(NOTIFICATION_CHANNEL_ID_MISSED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            notificationBuilder.setCategory(Notification.CATEGORY_MISSED_CALL)
        }
        val textMissedCall = data.getString(CallkitConstants.EXTRA_CALLKIT_MISSED_CALL_SUBTITLE, "")
        notificationBuilder.setSubText(if (TextUtils.isEmpty(textMissedCall)) context.getString(R.string.text_missed_call) else textMissedCall)
        notificationBuilder.setSmallIcon(smallIcon)
        val isCustomNotification =
            data.getBoolean(CallkitConstants.EXTRA_CALLKIT_IS_CUSTOM_NOTIFICATION, false)
        val count = data.getInt(CallkitConstants.EXTRA_CALLKIT_MISSED_CALL_COUNT, 1)
        if (count > 1) {
            notificationBuilder.setNumber(count)
        }
        if (isCustomNotification) {
            notificationViews =
                RemoteViews(context.packageName, R.layout.layout_custom_miss_notification)
            notificationViews?.setTextViewText(
                R.id.tvNameCaller, data.getString(CallkitConstants.EXTRA_CALLKIT_NAME_CALLER, "")
            )
            val isShowCallID =
                data.getBoolean(CallkitConstants.EXTRA_CALLKIT_IS_SHOW_CALL_ID, false)
            if (isShowCallID) {
                notificationViews?.setTextViewText(
                    R.id.tvNumber, data.getString(CallkitConstants.EXTRA_CALLKIT_HANDLE, "")
                )
            }
            notificationViews?.setOnClickPendingIntent(
                R.id.llCallback, getCallbackPendingIntent(missedNotificationId, data)
            )
            val isShowCallback = data.getBoolean(
                CallkitConstants.EXTRA_CALLKIT_MISSED_CALL_CALLBACK_SHOW, true
            )
            notificationViews?.setViewVisibility(
                R.id.llCallback, if (isShowCallback) View.VISIBLE else View.GONE
            )
            val textCallback =
                data.getString(CallkitConstants.EXTRA_CALLKIT_MISSED_CALL_CALLBACK_TEXT, "")
            notificationViews?.setTextViewText(
                R.id.tvCallback,
                if (TextUtils.isEmpty(textCallback)) context.getString(R.string.text_call_back) else textCallback
            )

            val avatarUrl = getAvatarUrl(data)
            if (avatarUrl != null) {
                val headers =
                    data.getSerializable(CallkitConstants.EXTRA_CALLKIT_HEADERS) as HashMap<String, Any?>

                getPicassoInstance(context, headers).load(avatarUrl).transform(CircleTransform())
                    .into(createAvatarTargetCustom(missedNotificationId))
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
                    CallkitConstants.EXTRA_CALLKIT_HANDLE, ""
                )
            )
            val avatarUrl = getAvatarUrl(data)
            if (avatarUrl != null) {
                val headers =
                    data.getSerializable(CallkitConstants.EXTRA_CALLKIT_HEADERS) as HashMap<String, Any?>

                getPicassoInstance(context, headers).load(avatarUrl)
                    .into(createAvatarTargetDefault(missedNotificationId))
            }
            val isShowCallback = data.getBoolean(
                CallkitConstants.EXTRA_CALLKIT_MISSED_CALL_CALLBACK_SHOW, true
            )
            if (isShowCallback) {
                val textCallback =
                    data.getString(CallkitConstants.EXTRA_CALLKIT_MISSED_CALL_CALLBACK_TEXT, "")
                val callbackAction: NotificationCompat.Action = NotificationCompat.Action.Builder(
                    R.drawable.ic_accept,
                    if (TextUtils.isEmpty(textCallback)) context.getString(R.string.text_call_back) else textCallback,
                    getCallbackPendingIntent(missedNotificationId, data)
                ).build()
                notificationBuilder.addAction(callbackAction)
            }
        }
        notificationBuilder.priority = NotificationManager.IMPORTANCE_HIGH

        notificationBuilder.setSound(missedCallSound)
        notificationBuilder.setContentIntent(getAppPendingIntent(missedNotificationId, data))

        // set action color
        data.getString(CallkitConstants.EXTRA_CALLKIT_ACTION_COLOR, "#4CAF50")?.toColorInt()?.let {
            try {
                notificationBuilder.color = it
            } catch (_: Exception) {
                // noop
            }
        }
        val notification = notificationBuilder.build()
        getNotificationManager().notify(missedNotificationId, notification)
    }


    fun clearIncomingNotification(data: Bundle, isAccepted: Boolean) {
        context.sendBroadcast(CallkitIncomingActivity.getIntentEnded(context, isAccepted))
        notificationId =
            data.getString(CallkitConstants.EXTRA_CALLKIT_ID, "callkit_incoming").hashCode()
        getNotificationManager().cancel(notificationId)
    }

    fun clearMissCallNotification(data: Bundle) {
        val missedNotificationId = data.getInt(
            CallkitConstants.EXTRA_CALLKIT_MISSED_CALL_ID,
            data.getString(CallkitConstants.EXTRA_CALLKIT_ID, "callkit_incoming").hashCode() + 1
        )
        getNotificationManager().cancel(missedNotificationId)
    }

    fun incomingChannelEnabled(): Boolean = getNotificationManager().run {
        val channel = getNotificationChannel(NOTIFICATION_CHANNEL_ID_INCOMING)

        return areNotificationsEnabled() && (channel != null && channel.importance > NotificationManagerCompat.IMPORTANCE_NONE)
    }

    fun createNotificationChannel(data: Bundle) {
        val incomingCallChannelName = data.getString(
            CallkitConstants.EXTRA_CALLKIT_INCOMING_CALL_NOTIFICATION_CHANNEL_NAME, "Incoming Call"
        )
        val missedCallChannelName = data.getString(
            CallkitConstants.EXTRA_CALLKIT_MISSED_CALL_NOTIFICATION_CHANNEL_NAME, "Missed Call"
        )
        val ongoingCallChannelName = data.getString(
            CallkitConstants.EXTRA_CALLKIT_ONGOING_CALL_NOTIFICATION_CHANNEL_NAME, "Ongoing Call"
        );

        getNotificationManager().apply {
            var channelCall = getNotificationChannel(NOTIFICATION_CHANNEL_ID_INCOMING)
            if (channelCall != null) {
                channelCall.setSound(null, null)
            } else {
                channelCall = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID_INCOMING,
                    incomingCallChannelName,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = ""
                    vibrationPattern = longArrayOf(0, 1000, 500, 1000, 500)
                    lightColor = Color.RED
                    enableLights(true)
                    enableVibration(true)
                    setSound(null, null)
                }
            }
            channelCall.lockscreenVisibility = Notification.VISIBILITY_PUBLIC

            channelCall.importance = NotificationManager.IMPORTANCE_HIGH

            createNotificationChannel(channelCall)

            val channelMissedCall = NotificationChannel(
                NOTIFICATION_CHANNEL_ID_MISSED,
                missedCallChannelName,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = ""
                vibrationPattern = longArrayOf(0, 1000)
                lightColor = Color.RED
                enableLights(true)
                enableVibration(true)
            }
            channelMissedCall.importance = NotificationManager.IMPORTANCE_DEFAULT
            createNotificationChannel(channelMissedCall)

            val channelOngoingCall = NotificationChannel(
                NOTIFICATION_CHANNEL_ID_ONGOING,
                ongoingCallChannelName,
                NotificationManager.IMPORTANCE_LOW // disables notification popup for ongoing call
            )
            createNotificationChannel(channelOngoingCall)
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

    private fun getAcceptPendingIntent(id: Int, data: Bundle): PendingIntent {
        val intentTransparent = TransparentActivity.getIntent(
            context, CallkitConstants.ACTION_CALL_ACCEPT, data
        )
        return PendingIntent.getActivity(context, id, intentTransparent, getFlagPendingIntent())
    }

    private fun getDeclinePendingIntent(id: Int, data: Bundle): PendingIntent {
        val declineIntent = CallkitIncomingBroadcastReceiver.getIntentDecline(context, data)
        return PendingIntent.getBroadcast(context, id, declineIntent, getFlagPendingIntent())
    }

    private fun getTimeOutPendingIntent(id: Int, data: Bundle): PendingIntent {
        val timeOutIntent = CallkitIncomingBroadcastReceiver.getIntentTimeout(context, data)
        return PendingIntent.getBroadcast(context, id, timeOutIntent, getFlagPendingIntent())
    }

    private fun getCallbackPendingIntent(id: Int, data: Bundle): PendingIntent {
        val intentTransparent = TransparentActivity.getIntent(
            context, CallkitConstants.ACTION_CALL_CALLBACK, data
        )
        return PendingIntent.getActivity(context, id, intentTransparent, getFlagPendingIntent())
    }

    private fun getActivityPendingIntent(id: Int, data: Bundle): PendingIntent {
        val intent = CallkitIncomingActivity.getIntent(context, data)
        return PendingIntent.getActivity(context, id, intent, getFlagPendingIntent())
    }

    private fun getAppPendingIntent(id: Int, data: Bundle): PendingIntent {
        val intent: Intent? = AppUtils.getAppIntent(context, data = data)
        return PendingIntent.getActivity(context, id, intent, getFlagPendingIntent())
    }

    private fun getHangUpIntent(id: Int, data: Bundle): PendingIntent {
        val intent = CallkitIncomingActivity.getIntentEnded(context, true)
        return PendingIntent.getActivity(context, id, intent, getFlagPendingIntent())
    }

    private fun getFlagPendingIntent(): Int {
        return PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    }


    private fun getNotificationManager(): NotificationManagerCompat {
        return NotificationManagerCompat.from(context)
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


    fun requestNotificationPermission(activity: Activity?, map: Map<String, Any>) {
        this.dataNotificationPermission = map
        if (Build.VERSION.SDK_INT > 32) {
            activity?.let {
                ActivityCompat.requestPermissions(
                    it,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    PERMISSION_NOTIFICATION_REQUEST_CODE
                )
            }
        }
    }

    fun canUseFullScreenIntent(): Boolean {
        return getNotificationManager().canUseFullScreenIntent()
    }

    fun openFullScreenNotificationsSettings(activity: Activity?) {
        val canUseFullScreenIntent = canUseFullScreenIntent()

        if (!canUseFullScreenIntent && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                data = Uri.fromParts("package", activity?.packageName, null)
            }
            activity?.startActivity(intent)
        }
    }

    fun onRequestPermissionsResult(activity: Activity?, requestCode: Int, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_NOTIFICATION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // allow
                } else {
                    //deny
                    activity?.let {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(
                                it, Manifest.permission.POST_NOTIFICATIONS
                            )
                        ) {
                            val rationaleMessagePermission =
                                this.dataNotificationPermission["rationaleMessagePermission"]

                            //showDialogPermissionRationale()
                            if (rationaleMessagePermission != null) {
                                showDialogMessage(
                                    it,
                                    rationaleMessagePermission as String
                                ) { dialog, _ ->
                                    dialog?.dismiss()
                                    requestNotificationPermission(
                                        activity, this.dataNotificationPermission
                                    )
                                }
                            } else {
                                requestNotificationPermission(
                                    activity, this.dataNotificationPermission
                                )
                            }
                        } else {
                            val postNotificationMessageRequired =
                                this.dataNotificationPermission["postNotificationMessageRequired"]
                            val message = if (postNotificationMessageRequired != null) {
                                postNotificationMessageRequired as String
                            } else {
                                it.resources.getString(R.string.text_post_notification_message_required)
                            }

                            //Open Setting
                            showDialogMessage(it, message) { dialog, _ ->
                                dialog?.dismiss()
                                val intent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", it.packageName, null),
                                )
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                it.startActivity(intent)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showDialogMessage(
        activity: Activity?, message: String, okListener: DialogInterface.OnClickListener
    ) {
        activity?.let {
            AlertDialog.Builder(it, R.style.DialogTheme).setMessage(message)
                .setPositiveButton(android.R.string.ok, okListener)
                .setNegativeButton(android.R.string.cancel, null).create().show()
        }
    }
}


