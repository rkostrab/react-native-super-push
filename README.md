
# react-native-super-push

Stable react native remote push notifications based on FCM (Android) and APNs (iOS). Compatible with newer than RN 61.5

## Features
- Proper workflow when user tap on notification

- Merging multiple notifications into one big InboxStyle notification using defined payload key

- Setting title and content text keys to define android notifications (solution - how to show android notifications while app is killed)

## Android installation

- Fire `yarn add react-native-super-push@git+https://github.com/rkostrab/react-native-super-push.git#1.3.0`

- Download your `google-services.json` file and place it in `android/app` directory

- Edit project-level `build.gradle`

```diff
  dependencies {
    classpath 'com.android.tools.build:gradle:3.2.1'
+   classpath 'com.google.gms:google-services:4.3.3'
  }
```

- Edit app-level `build.gradle`

```diff
apply plugin: "com.android.application"
+ apply plugin: 'com.google.gms.google-services'
```

- Edit `android/app/src/main/AndroidManifest.xml`:

```diff
+ <uses-permission android:name="android.permission.VIBRATE" />

  <application>

+   <service
+     android:name="sk.rkostrab.push.MessagingService"
+     android:exported="true">
+     <intent-filter>
+       <action android:name="com.google.firebase.MESSAGING_EVENT"/>
+     </intent-filter>
+   </service>

    <activity
      android:name=".MainActivity"
      android:label="@string/app_name"
      android:windowSoftInputMode="adjustResize"
+     android:launchMode="singleTop"
      android:configChanges="keyboard|keyboardHidden|orientation|screenSize">
      <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
      </intent-filter>
    </activity>

  </application>
```


## iOS installation

We're using `PushNotificationIOS` from `react-native-community` so follow [this guide](https://github.com/react-native-community/push-notification-ios)

## Usage
```javascript
import PushNotificationIOS from "@react-native-community/push-notification-ios";

RNSuperPush.configure({

  // required
  // android only: key from notification payload which will be title of notification
  titleKey: "title",

  // required
  // android only: key from notification payload which will be content text of notification
  contentKey: "message",

  // optional (if not set: will not merge notifications)
  // android only: key from notification payload which will be used to merge multiple into one big InboxStyle notification
  mergeKey: "type",

  // optional (if not set: mipmap/ic_launcher)
  // android only: define small icon into notifications from android res directory
  smallIcon: "mipmap/ic_notification_small",

  // required
  // android only: define channel name for android Oreo and above
  channelName: "Notifications",

  onOpenNotification: (payload) => {
    console.log(JSON.stringify(payload));
  },

  onGetToken: (token) => {
    console.log(token);
  },

  onRequestPermissions: (granted, errMsg) => {
    console.log("Permissions: " + granted);
  },

});
```

## Methods
```javascript
// request permission to receive push notifications (they are requested automatically every app start)
RNSuperPush.requestPermissions();

// dismiss all app's notifications from system tray
RNSuperPush.dismissAllNotifications();

// get token (it is requested automatically every app start)
RNSuperPush.getToken();
```

## Troubleshooting
- **Push notifications are not incoming** - Make sure your `package_name` in `google-services.json` file is same with your apps's package name. If not then change `applicationId` in `android/app/build.gradle` to match `package_name` from `google-services.json`

## Support
Right now, I am supporting this library for only my own purposes - a few RN based apps. I am going to add more configurable features later.
