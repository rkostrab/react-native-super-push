
# react-native-super-push

Stable react native remote push notifications based on FCM (Android) and APNs (iOS - soon).

## Features
- Proper workflow when user tap on notification

- Merging multiple notifications into one big InboxStyle notification using defined payload key

- Setting title and content text keys to define android notifications (solution - how to show android notifications while app is killed)

## Installation (Android)

- Fire `npm install --save react-native-super-push@git+https://github.com/rkostrab/react-native-super-push.git#1.0.0`

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
+   compile project(':react-native-super-push')
+   compile 'com.google.firebase:firebase-messaging:11.6.2'
    compile fileTree(dir: "libs", include: ["*.jar"])
    compile 'com.android.support:appcompat-v7:23.0.1'
    compile 'com.facebook.react:react-native:+'  // From node_modules
  }
```

- Edit `android/build.gradle`

```diff
  dependencies {
    classpath 'com.android.tools.build:gradle:3.0.1'
+   classpath 'com.google.gms:google-services:3.1.1'
  }
  allprojects {
    repositories {
        mavenLocal()
        jcenter()
+       google()
        maven {
            // All of React Native (JS, Obj-C sources, Android binaries) is installed from npm
            url "$rootDir/../node_modules/react-native/android"
        }
    }
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

- More about notification icons and colors [here](https://firebase.google.com/docs/cloud-messaging/android/client):

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

  // optional (default will not merge notifications)
  // android only: key from notification payload which will be used to merge multiple into one big InboxStyle notification
  mergeKey: "type",

  // optional (default: mipmap/ic_launcher)
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

## TODO
- integrating ios notifications (soon)
