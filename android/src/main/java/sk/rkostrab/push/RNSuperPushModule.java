
package sk.rkostrab.push;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.NotificationManagerCompat;
import android.text.TextUtils;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.firebase.iid.FirebaseInstanceId;

public class RNSuperPushModule extends ReactContextBaseJavaModule implements ActivityEventListener {

    public static final String SHARED_PREFS = "sk.rkostrab.push.sharedprefs";
    public static final String SHARED_PREFS_NOTIFICATIONS = "sk.rkostrab.push.sharedprefs.notifications";
    public static final String PREFS_MERGE_KEY = "sk.rkostrab.push.sharedprefs.mergekey";
    public static final String PREFS_MERGE_VALUE_PREFIX = "sk.rkostrab.push.sharedprefs.mergevalue.";
    public static final String PREFS_TITLE_KEY = "sk.rkostrab.push.sharedprefs.titlekey";
    public static final String PREFS_CONTENT_KEY = "sk.rkostrab.push.sharedprefs.contentkey";
    public static final String PREFS_SMALL_ICON_KEY = "RNSuperPushSmallIcon";
    public static final String EVENT_REFRESH_TOKEN = "RNSuperPushRefreshToken";
    public static final String EVENT_OPEN_NOTIFICATION = "RNSuperPushOpenNotification";
    public static final String EVENT_PENDING_NOTIFICATION = "RNSuperPushPendingNotification";
    public static final String EVENT_REQUEST_PERMISSIONS = "RNSuperPushRequestPermissions";
    private Bundle pendingNotification;

    public RNSuperPushModule(ReactApplicationContext reactContext) {
        super(reactContext);
        getReactApplicationContext().addActivityEventListener(this);
    }

    @Override
    public String getName() {
        return "RNSuperPush";
    }

    @ReactMethod
    public void init() {
        requestPermissions();
        // token stuff
        getToken();
        // open notification stuff
        handleIntent(getCurrentActivity().getIntent());
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
        Bundle bundle = new Bundle();
        bundle.putString("token", FirebaseInstanceId.getInstance().getToken());
        sendEvent(EVENT_REFRESH_TOKEN, Arguments.fromBundle(bundle));
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
    public void requestPermissions() {
        Bundle bundle = new Bundle();
        if (NotificationManagerCompat.from(getReactApplicationContext()).areNotificationsEnabled()) {
            bundle.putBoolean("granted", true);
        } else {
            bundle.putBoolean("granted", false);
            bundle.putString("errMsg", "Push notifications disabled");
        }
        sendEvent(EVENT_REQUEST_PERMISSIONS, Arguments.fromBundle(bundle));
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
    }

    @Override
    public void onNewIntent(Intent intent) {
        getCurrentActivity().setIntent(intent);
        handleIntent(intent);
    }

    private void sendEvent(final String eventName, final Object params) {
        if (getReactApplicationContext().hasActiveCatalystInstance()) {
            getReactApplicationContext()
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(eventName, params);
        }
    }

    private void handleIntent(Intent intent) {
        pendingNotification = intent.getBundleExtra("notification");
        if (pendingNotification != null) {
            sendEvent(EVENT_PENDING_NOTIFICATION, null);
        }
    }
}
