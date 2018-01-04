import ReactNative, { AppState, NativeModules, Platform, DeviceEventEmitter } from 'react-native';

var RNSuperPush;
if (Platform.OS === 'android') {
  RNSuperPush = NativeModules.RNSuperPush;
} else {
  RNSuperPush = {};
  var PushNotificationIOS = ReactNative.PushNotificationIOS;
}

RNSuperPush.createPayload = (payload) => {
  payload.isForeground = AppState.currentState === 'active';
  return payload;
}

RNSuperPush.configure = (options) => {

  if (Platform.OS === 'android' && typeof options.titleKey !== 'undefined') {
    RNSuperPush.setTitleKey(options.titleKey);
  }

  if (Platform.OS === 'android' && typeof options.contentKey !== 'undefined') {
    RNSuperPush.setContentKey(options.contentKey);
  }

  if (Platform.OS === 'android' && typeof options.mergeKey !== 'undefined') {
    RNSuperPush.setMergeKey(options.mergeKey);
  }

  if (Platform.OS === 'android' && typeof options.smallIcon !== 'undefined') {
    RNSuperPush.setSmallIcon(options.smallIcon);
  }

  if (typeof options.onOpenNotification !== 'undefined') {
    if (RNSuperPush.openNotificationListener) {
      RNSuperPush.openNotificationListener.remove();
    }
    RNSuperPush.openNotificationListener = DeviceEventEmitter.addListener('RNSuperPushOpenNotification', (event) => {
      if (event.mergeValue) {
        RNSuperPush.dismissMergedNotification(event.mergeValue);
      }
      options.onOpenNotification(RNSuperPush.createPayload(event));
    });
	}

  if (typeof options.onGetToken !== 'undefined') {
    if (Platform.OS === 'android') {
      if (RNSuperPush.getTokenListener) {
        RNSuperPush.getTokenListener.remove();
      }
      RNSuperPush.getTokenListener = DeviceEventEmitter.addListener('RNSuperPushRefreshToken', (obj) => {
        options.onGetToken(obj.token);
      });
    } else {
      PushNotificationIOS.removeEventListener('register');
      PushNotificationIOS.addEventListener('register', (obj) => {
        options.onGetToken(obj);
      });
    }
	}

  if (typeof options.onRequestPermissions !== 'undefined') {
    if (RNSuperPush.requestPermissionsListener) {
      RNSuperPush.requestPermissionsListener.remove();
    }
    if (Platform.OS === 'android') {
      RNSuperPush.requestPermissionsListener = DeviceEventEmitter.addListener('RNSuperPushRequestPermissions', (event) => {
        options.onRequestPermissions(event.granted);
      });
    }
	}

  if (Platform.OS === 'android') {
    if (RNSuperPush.pendingNotificationListener) {
      RNSuperPush.pendingNotificationListener.remove();
    }
    RNSuperPush.pendingNotificationListener = DeviceEventEmitter.addListener('RNSuperPushPendingNotification', () => {
      RNSuperPush.grabNotification();
    });
    RNSuperPush.init();
  } else {
    // remote iOS notification
    PushNotificationIOS.removeEventListener('notification');
    PushNotificationIOS.addEventListener('notification', (data) => {
      if (data) {
        options.onOpenNotification(RNSuperPush.createPayload(data));
      }
    });
    PushNotificationIOS.getInitialNotification().then((data) => {
      if (data) {
        options.onOpenNotification(RNSuperPush.createPayload(data));
      }
    }).catch((error) => {
      console.error("getInitialNotification error: " + error.message);
    });
    PushNotificationIOS.requestPermissions({
      alert: true,
      badge: true,
      sound: true,
    }).then((obj) => {
      var granted = obj.alert || obj.badge || obj.sound ? true : false;
      options.onRequestPermissions(granted);
    }).catch((error) => {
      options.onRequestPermissions(false);
    });
  }

};

export default RNSuperPush;
