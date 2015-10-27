package com.soontobe.joinpay.helpers;

import android.app.Dialog;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.ibm.mobile.services.core.IBMBluemix;
import com.ibm.mobile.services.push.IBMPush;
import com.ibm.mobile.services.push.IBMPushNotificationListener;
import com.ibm.mobile.services.push.IBMSimplePushNotification;
import com.soontobe.joinpay.Constants;

import java.util.List;

import bolts.Continuation;
import bolts.Task;

import static com.ibm.mobile.services.data.internal.CLClientManager.runOnUiThread;

/**
 * Created by mrshah on 10/26/2015.
 */
public class IBMPushService extends Service {

    LocalBinder mBinder = new LocalBinder();

    private IBMPush mPush;

    public static final String MESSAGE_RECEIVED = "com.soontobe.joinpay.helpers.IBMPushService";

    public class LocalBinder extends Binder {
        public IBMPushService getService() {
            return IBMPushService.this;
        }
    }


    @Override
    public void onCreate() {
        super.onCreate();
        ///////////////////////////////////////////////////
        /////////////////  IBM Push Code  /////////////////
        ///////////////////////////////////////////////////
        IBMBluemix.initialize(this, Constants.appKey, Constants.appSecret, Constants.baseURL);            //init for IBM push
        mPush = IBMPush.initializeService();
        registerService();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPush.unsubscribe("testtag");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Push notification listener. Sends a message to the UI for each push notification received.
     */

    private IBMPushNotificationListener mNotificationlistener = new IBMPushNotificationListener() {

        @Override
        public void onReceive(final IBMSimplePushNotification message) {
            Log.d(Constants.PUSH_TAG, "I got a push msg");
            Intent broadcastIntent = new Intent(MESSAGE_RECEIVED);
            broadcastIntent.putExtra("message", message.getAlert());
            sendBroadcast(broadcastIntent);
        }
    };

    /**
     * Registers with the push notification service
     */
    private void registerService() {
        ///////////////// More Push Registration /////////////////
        mPush.register("dev4", Constants.userName).continueWith(new Continuation<String, Void>() {
            @Override
            public Void then(Task<String> task) throws Exception {
                if (task.isFaulted()) {
                    Log.e(Constants.PUSH_TAG, "failed to push list of subscriptions");
                    return null;
                } else {
                    mPush.getSubscriptions().continueWith(new Continuation<List<String>, Void>() {
                        public Void then(Task<List<String>> task1) throws Exception {
                            if (task1.isFaulted()) {
                                Log.e(Constants.PUSH_TAG, "failed to push list of subscriptions");
                            } else {
                                List<String> tags = task1.getResult();
                                if (tags.size() > 0) {
                                    mPush.unsubscribe(tags.get(0)).continueWith(new Continuation<String, Void>() {

                                        @Override
                                        public Void then(Task<String> task2) throws Exception {
                                            if (task2.isFaulted()) {
                                                Log.e(Constants.PUSH_TAG, "subscribe failed");
                                            } else {
                                                subscribe();
                                            }
                                            return null;
                                        }
                                    });
                                } else {
                                    Log.d(Constants.PUSH_TAG, "" + task1.getResult());
                                    subscribe();
                                }
                            }
                            return null;
                        }
                    });
                    return null;
                }
            }
        });
    }

    /**
     * Subscribe to push notification on 'testtag'
     */
    private void subscribe() {
        mPush.subscribe("testtag").continueWith(new Continuation<String, Void>() {
            public Void then(Task<String> task1) throws Exception {
                if (task1.isFaulted()) {
                    Log.e(Constants.PUSH_TAG, "Push Subscription Failed" + task1.getError().getMessage());
                } else {
                    mPush.listen(mNotificationlistener);
                    Log.d(Constants.PUSH_TAG, "Push Subscription Success");
                }
                return null;
            };
        });
    }
}
