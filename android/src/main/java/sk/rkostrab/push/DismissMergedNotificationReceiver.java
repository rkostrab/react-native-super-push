package sk.rkostrab.push;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationManagerCompat;
import android.text.TextUtils;
import android.util.Log;

import static android.content.Context.MODE_PRIVATE;
import static sk.rkostrab.push.RNSuperPushModule.PREFS_MERGE_VALUE_PREFIX;
import static sk.rkostrab.push.RNSuperPushModule.SHARED_PREFS_NOTIFICATIONS;

/**
 * Created by rasto on 16.05.16.
 */
public class DismissMergedNotificationReceiver extends BroadcastReceiver {

    private static final String TAG = DismissMergedNotificationReceiver.class.getSimpleName();
    public static final String ALL_NOTIFICATIONS_MERGEVALUE = "_all_";

    @Override
    public void onReceive(Context context, Intent intent) {
        String mergeValue = intent.getAction();
        if (!TextUtils.isEmpty(mergeValue)) {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            SharedPreferences prefsNotifications = context.getSharedPreferences(SHARED_PREFS_NOTIFICATIONS, MODE_PRIVATE);
            if (ALL_NOTIFICATIONS_MERGEVALUE.equals(mergeValue)) {
                prefsNotifications.edit().clear().apply();
                notificationManager.cancelAll();
            } else {
                prefsNotifications.edit().remove(PREFS_MERGE_VALUE_PREFIX + mergeValue).apply();
                notificationManager.cancel(mergeValue.hashCode());
            }
        }
        else {
            Log.e(TAG, "Missing action - 'mergeKey'");
        }
    }

}
