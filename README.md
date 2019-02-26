
# react-native-super-push

Stable react native remote push notifications based on FCM (Android) and APNs (iOS).

## Features
- Proper workflow when user tap on notification

- Merging multiple notifications into one big InboxStyle notification using defined payload key

- Setting title and content text keys to define android notifications (solution - how to show android notifications while app is killed)

## Android installation

- Fire `npm install --save react-native-super-push@git+https://github.com/rkostrab/react-native-super-push.git#1.1.0`

- Download your `google-services.json` file and place it in `android/app` directory

- Edit `android/settings.gradle`

```diff
+ include ':react-native-super-push'
+ project(':react-native-super-push').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-super-push/android')
  include ':app'
```

- Edit `{YOUR_MAIN_PROJECT}/app/build.gradle`:

```diff
  dependencies {
+   implementation project(':react-native-super-push')
    implementation fileTree(dir: "libs", include: ["*.jar"])
    implementation "com.android.support:appcompat-v7:${rootProject.ext.supportLibVersion}"
    implementation 'com.facebook.react:react-native:+'  // From node_modules
  }
```

- Edit `android/build.gradle`

```diff
  dependencies {
    classpath 'com.android.tools.build:gradle:3.2.1'
+   classpath 'com.google.gms:google-services:4.2.0'
  }
```

- Edit `android/app/build.gradle`. Add **at the bottom** of the file:

```diff
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

- Add package in `MainApplication`

```diff
  @Override
  protected List<ReactPackage> getPackages() {
    return Arrays.<ReactPackage>asList(
        new MainReactPackage(),
+       new RNSuperPushPackage()
    );
  }
```

## iOS installation

We're using `PushNotificationIOS` from `react-native` so follow [this guide](https://facebook.github.io/react-native/docs/pushnotificationios.html#content)

## Usage
```javascript
import RNSuperPush from 'react-native-super-push';

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
