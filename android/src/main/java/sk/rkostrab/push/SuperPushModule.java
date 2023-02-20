
package sk.rkostrab.push;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.modules.core.PermissionAwareActivity;
import com.facebook.react.modules.core.PermissionListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class SuperPushModule extends ReactContextBaseJavaModule implements ActivityEventListener, PermissionListener {

    public static final String SHARED_PREFS = "sk.rkostrab.push.sharedprefs";
    public static final String SHARED_PREFS_NOTIFICATIONS = "sk.rkostrab.push.sharedprefs.notifications";
    public static final String PREFS_MERGE_KEY = "sk.rkostrab.push.sharedprefs.mergekey";
    public static final String PREFS_MERGE_VALUE_PREFIX = "sk.rkostrab.push.sharedprefs.mergevalue.";
    public static final String PREFS_TITLE_KEY = "sk.rkostrab.push.sharedprefs.titlekey";
    public static final String PREFS_CONTENT_KEY = "sk.rkostrab.push.sharedprefs.contentkey";
    public static final String PREFS_SMALL_ICON_KEY = "SuperPushSmallIcon";
    public static final String PREFS_CHANNEL_NAME_KEY = "SuperPushChannelName";
    public static final String EVENT_REFRESH_TOKEN = "SuperPushRefreshToken";
    public static final String EVENT_OPEN_NOTIFICATION = "SuperPushOpenNotification";
    public static final String EVENT_PENDING_NOTIFICATION = "SuperPushPendingNotification";
    public static final String EVENT_REQUEST_PERMISSIONS = "SuperPushRequestPermissions";
    public static final String BROADCAST_FOREGROUND_PUSH = "sk.rkostrab.push.broadcast.foregroundpush";
    private Bundle pendingNotification;
    private BroadcastReceiver foregroundPushReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            sendEvent(EVENT_OPEN_NOTIFICATION, Arguments.fromBundle(intent.getExtras()));
        }
    };

    public SuperPushModule(ReactApplicationContext reactContext) {
        super(reactContext);
        getReactApplicationContext().addActivityEventListener(this);
        LocalBroadcastManager.getInstance(getReactApplicationContext()).registerReceiver(foregroundPushReceiver, new IntentFilter(BROADCAST_FOREGROUND_PUSH));
    }

    @Override
    public void onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy();
        LocalBroadcastManager.getInstance(getReactApplicationContext()).unregisterReceiver(foregroundPushReceiver);
    }

    @Override
    public String getName() {
        return "SuperPush";
    }

    @ReactMethod
    public void init() {
        requestPermissions();
        // open notification stuff
        try {
            handleIntent(getCurrentActivity().getIntent(), true);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private void setNotificationKey(String prefsKey, String notificationKey) {
        getReactApplicationContext()
                .getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(prefsKey, notificationKey)
                .apply();
    }

    @ReactMethod
    public void getToken() {
        if (!NotificationManagerCompat.from(getReactApplicationContext()).areNotificationsEnabled()) {
            return;
        }
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                if (!task.isSuccessful()) {
                    return;
                }
                String token = task.getResult();
                Bundle bundle = new Bundle();
                bundle.putString("token", token);
                sendEvent(EVENT_REFRESH_TOKEN, Arguments.fromBundle(bundle));
            }
        });
    }

    @ReactMethod
    public void setMergeKey(String mergeKey) {
        setNotificationKey(PREFS_MERGE_KEY, mergeKey);
    }

    @ReactMethod
    public void setTitleKey(String titleKey) {
        setNotificationKey(PREFS_TITLE_KEY, titleKey);
    }

    @ReactMethod
    public void setContentKey(String contentKey) {
        setNotificationKey(PREFS_CONTENT_KEY, contentKey);
    }

    @ReactMethod
    public void setSmallIcon(String smallIcon) {
        setNotificationKey(PREFS_SMALL_ICON_KEY, smallIcon);
    }

    @ReactMethod
    public void setChannelName(String channelName) {
        setNotificationKey(PREFS_CHANNEL_NAME_KEY, channelName);
    }

    @ReactMethod
    public void requestPermissions() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(getCurrentActivity(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                PermissionAwareActivity permissionAwareActivity = (PermissionAwareActivity) getCurrentActivity();
                if (permissionAwareActivity != null) {
                    permissionAwareActivity.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101, this);
                }
            }
            else {
                Bundle bundle = new Bundle();
                bundle.putBoolean("granted", true);
                sendEvent(EVENT_REQUEST_PERMISSIONS, Arguments.fromBundle(bundle));
                getToken();
            }
        }
    }

    @ReactMethod
    public void dismissMergedNotification(String mergeValue) {
        if (!TextUtils.isEmpty(mergeValue)) {
            Intent intent = new Intent(getReactApplicationContext(), DismissMergedNotificationReceiver.class);
            intent.setAction(mergeValue);
            getCurrentActivity().sendBroadcast(intent);
        }
    }

    @ReactMethod
    public void dismissAllNotifications() {
        dismissMergedNotification(DismissMergedNotificationReceiver.ALL_NOTIFICATIONS_MERGEVALUE);
    }

    @ReactMethod
    public void grabNotification() {
        if (pendingNotification != null) {
            sendEvent(EVENT_OPEN_NOTIFICATION, Arguments.fromBundle(pendingNotification));
            pendingNotification = null;
        }
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        Log.d("TAG", "onActivityResult: ");
    }

    @Override
    public void onNewIntent(Intent intent) {
        getCurrentActivity().setIntent(intent);
        handleIntent(intent, false);
    }

    private void sendEvent(final String eventName, final Object params) {
        if (getReactApplicationContext().hasActiveCatalystInstance()) {
            getReactApplicationContext()
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(eventName, params);
        }
    }

    private void handleIntent(Intent intent, boolean isInitial) {
        pendingNotification = intent.getBundleExtra("notification");
        if (pendingNotification != null) {
            pendingNotification.putBoolean("isInitial", isInitial);
            sendEvent(EVENT_PENDING_NOTIFICATION, null);
        }
    }

    @Override
    public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Bundle bundle = new Bundle();
        if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            bundle.putBoolean("granted", true);
            getToken();
        } else {
            bundle.putBoolean("granted", false);
            bundle.putString("errMsg", "Push notifications disabled");
        }
        sendEvent(EVENT_REQUEST_PERMISSIONS, Arguments.fromBundle(bundle));
        return false;
    }
}
