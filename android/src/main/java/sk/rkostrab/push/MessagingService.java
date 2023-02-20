package sk.rkostrab.push;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static sk.rkostrab.push.SuperPushModule.PREFS_CHANNEL_NAME_KEY;
import static sk.rkostrab.push.SuperPushModule.PREFS_CONTENT_KEY;
import static sk.rkostrab.push.SuperPushModule.PREFS_MERGE_KEY;
import static sk.rkostrab.push.SuperPushModule.PREFS_MERGE_VALUE_PREFIX;
import static sk.rkostrab.push.SuperPushModule.PREFS_SMALL_ICON_KEY;
import static sk.rkostrab.push.SuperPushModule.PREFS_TITLE_KEY;
import static sk.rkostrab.push.SuperPushModule.SHARED_PREFS;
import static sk.rkostrab.push.SuperPushModule.SHARED_PREFS_NOTIFICATIONS;

/**
 * Created by rasto on 4.12.17.
 */

public class MessagingService extends FirebaseMessagingService {

    private static final String TAG = MessagingService.class.getSimpleName();
    private static final String DELIMITER = "|-|";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Map<String, String> data = remoteMessage.getData();
        if (data.size() > 0) {
            Log.d(TAG, "PUSH NOTIFICATION: " + data);
        }
        Bundle bundle = new Bundle();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            bundle.putString(entry.getKey(), entry.getValue());
        }
        try {
            bundle.putBoolean("isForeground", isApplicationInForeground());
            showNotification(bundle);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showNotification(Bundle bundle) throws Exception {
        int flags = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT : PendingIntent.FLAG_UPDATE_CURRENT;
        Context context = getApplicationContext();
        SharedPreferences sharedPrefs = context.getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        String mergeKey = sharedPrefs.getString(PREFS_MERGE_KEY, null);
        String titleKey = sharedPrefs.getString(PREFS_TITLE_KEY, null);
        String contentKey = sharedPrefs.getString(PREFS_CONTENT_KEY, null);
        String smallIconString = sharedPrefs.getString(PREFS_SMALL_ICON_KEY, null);
        int notificationId = (int) System.currentTimeMillis();
        String contentTitle = bundle.getString(titleKey);
        if (TextUtils.isEmpty(contentTitle)) {
            contentTitle = context.getPackageManager().getApplicationLabel(context.getApplicationInfo()).toString();
        }
        String contentText = bundle.getString(contentKey);
        if (TextUtils.isEmpty(contentText)) {
            throw new Exception("Missing 'message' in push notification");
        }
        Resources res = context.getResources();
        String packageName = context.getPackageName();
        String resDirectory = "mipmap";
        String resName = "ic_launcher";
        if (!TextUtils.isEmpty(smallIconString)) {
            String[] smallIconArray = smallIconString.split("/");
            resDirectory = smallIconArray[0];
            resName = smallIconArray[1];
        }
        int smallIcon = res.getIdentifier(resName, resDirectory, packageName);
        if(smallIcon == 0) {
            throw new Exception("resource name '" + resName + "' not found in directory '" + resDirectory + "'");
        }
        // merge notifications
        String mergeValue = TextUtils.isEmpty(mergeKey) ? null : bundle.getString(mergeKey);
        PendingIntent dismissMergedNotificationPendingIntent = null;
        ArrayList<String> array = null;
        if(!TextUtils.isEmpty(mergeValue)) {
            notificationId = mergeValue.hashCode();
            bundle.putString("mergeValue", mergeValue);
            SharedPreferences prefsNotifications = context.getSharedPreferences(SHARED_PREFS_NOTIFICATIONS, MODE_PRIVATE);
            String arrayString = prefsNotifications.getString(PREFS_MERGE_VALUE_PREFIX + mergeValue, "");
            array = new ArrayList<>();
            if (!TextUtils.isEmpty(arrayString)) {
                StringTokenizer tokenizer = new StringTokenizer(arrayString, DELIMITER);
                while(tokenizer.hasMoreTokens()) {
                    array.add(tokenizer.nextToken());
                }
            }
            array.add(contentText);
            if (array.size() > 1) {
                contentTitle += " (" + array.size() + ")";
            }
            arrayString = TextUtils.join(DELIMITER, array);
            prefsNotifications.edit().putString(PREFS_MERGE_VALUE_PREFIX + mergeValue, arrayString).apply();
//            // dissmiss broadcast
            Intent dismissMergedNotificationIntent = new Intent(context, DismissMergedNotificationReceiver.class);
            dismissMergedNotificationIntent.setAction(mergeValue);
            dismissMergedNotificationPendingIntent = PendingIntent.getBroadcast(context, 0, dismissMergedNotificationIntent, flags);
        }
        // build notification
        Notification.Builder notification = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notification = new Notification.Builder(context, context.getPackageName());
        } else {
            notification = new Notification.Builder(context);
        }
        notification.setContentTitle(contentTitle);
        notification.setTicker(contentTitle);
        notification.setContentText(contentText);
        notification.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notification.setVisibility(Notification.VISIBILITY_PRIVATE);
        }
        notification.setPriority(Notification.PRIORITY_HIGH);
        notification.setAutoCancel(true);
        notification.setSmallIcon(smallIcon);
        if (array != null) {
            Notification.InboxStyle inboxStyle = new Notification.InboxStyle();
            for (String line : array) {
                inboxStyle.addLine(line);
            }
            inboxStyle.setBigContentTitle(contentTitle);
            notification.setStyle(inboxStyle);
        }
        // set intents
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        String className = launchIntent.getComponent().getClassName();
        Intent intent = new Intent(context, Class.forName(className));
        intent.putExtra("notification", bundle);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent launchPendingIntent = PendingIntent.getActivity(context, 0, intent, flags);
        notification.setContentIntent(launchPendingIntent);
        if (dismissMergedNotificationPendingIntent != null) {
            notification.setDeleteIntent(dismissMergedNotificationPendingIntent);
        }
        // show
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelName = sharedPrefs.getString(PREFS_CHANNEL_NAME_KEY, null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(context.getPackageName(), channelName, NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }
        notificationManager.notify(notificationId, notification.build());
    }

    private boolean isApplicationInForeground() {
        ActivityManager activityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses != null) {
            for (ActivityManager.RunningAppProcessInfo processInfo : appProcesses) {
                if (processInfo.processName.equals(getApplication().getPackageName()) && processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    return processInfo.pkgList.length > 0;
                }
            }
        }
        return false;
    }

}
