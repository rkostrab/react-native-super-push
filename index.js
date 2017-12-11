import { NativeModules, Platform, DeviceEventEmitter } from 'react-native';

const { RNSuperPush } = NativeModules;

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
      options.onOpenNotification(event);
    });
	}

  if (typeof options.onGetToken !== 'undefined') {
    if (RNSuperPush.getTokenListener) {
      RNSuperPush.getTokenListener.remove();
    }
    RNSuperPush.getTokenListener = DeviceEventEmitter.addListener('RNSuperPushRefreshToken', (event) => {
      options.onGetToken(event.token);
    });
	}

  if (typeof options.onRequestPermissions !== 'undefined') {
    if (RNSuperPush.requestPermissionsListener) {
      RNSuperPush.requestPermissionsListener.remove();
    }
    RNSuperPush.requestPermissionsListener = DeviceEventEmitter.addListener('RNSuperPushRequestPermissions', (event) => {
      options.onRequestPermissions(event.granted, event.errMsg);
    });
	}

  if (Platform.OS === 'android') {
    if (RNSuperPush.pendingNotificationListener) {
      RNSuperPush.pendingNotificationListener.remove();
    }
    RNSuperPush.pendingNotificationListener = DeviceEventEmitter.addListener('RNSuperPushPendingNotification', () => {
      RNSuperPush.grabNotification();
    });
    RNSuperPush.init();
  }

};

export default RNSuperPush;
