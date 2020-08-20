import { AppState, NativeModules, Platform, DeviceEventEmitter } from 'react-native';
import PushNotificationIOS from "@react-native-community/push-notification-ios";

const SuperPush = Platform.OS === 'android' ? NativeModules.SuperPush : {};

SuperPush.configure = options => {
  
  const { onGetToken, onOpenNotification, onRequestPermissions, titleKey, contentKey, mergeKey, smallIcon, channelName } = options;
  
  if (Platform.OS === 'ios') {
    const onIosNotification = (payload, open, isInitial) => {
      if (!payload) return;
      payload.isForeground = isInitial ? false : AppState.currentState === 'active';
      payload.isInitial = isInitial;
      open(payload);
    }
    PushNotificationIOS.removeEventListener('register');
    PushNotificationIOS.addEventListener('register', obj => onGetToken(obj));
    PushNotificationIOS.removeEventListener('notification');
    PushNotificationIOS.addEventListener('notification', data => onIosNotification(data, onOpenNotification, false));
    PushNotificationIOS.getInitialNotification()
      .then(data => onIosNotification(data, onOpenNotification, true))
      .catch(error => console.error("getInitialNotification error: " + error.message));
    PushNotificationIOS.requestPermissions({ alert: true, badge: true, sound: true })
      .then(obj => onRequestPermissions(obj.alert))
      .catch(e => onRequestPermissions(false));
  } else {
    // android required
    SuperPush.setTitleKey(titleKey);
    SuperPush.setContentKey(contentKey);
    SuperPush.setChannelName(channelName);
    // android optional
    if (typeof mergeKey !== 'undefined') SuperPush.setMergeKey(mergeKey);
    if (typeof smallIcon !== 'undefined') SuperPush.setSmallIcon(smallIcon);
    // android listeners
    if (SuperPush.openNotificationListener) {
      SuperPush.openNotificationListener.remove();
    }
    SuperPush.openNotificationListener = DeviceEventEmitter.addListener('SuperPushOpenNotification', event => {
      if (event.mergeValue) {
        SuperPush.dismissMergedNotification(event.mergeValue);
      }
      onOpenNotification(event);
    });
    if (SuperPush.getTokenListener) {
      SuperPush.getTokenListener.remove();
    }
    SuperPush.getTokenListener = DeviceEventEmitter.addListener('SuperPushRefreshToken', obj => onGetToken(obj.token));
    if (SuperPush.requestPermissionsListener) {
      SuperPush.requestPermissionsListener.remove();
    }
    SuperPush.requestPermissionsListener = DeviceEventEmitter.addListener('SuperPushRequestPermissions', event => onRequestPermissions(event.granted));
    if (SuperPush.pendingNotificationListener) {
      SuperPush.pendingNotificationListener.remove();
    }
    SuperPush.pendingNotificationListener = DeviceEventEmitter.addListener('SuperPushPendingNotification', () => SuperPush.grabNotification());
    SuperPush.init();
  }

};

export default SuperPush;