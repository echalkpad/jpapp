package com.soontobe.joinpay;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import static com.ibm.mobile.services.data.internal.CLClientManager.runOnUiThread;

/**
 * Created by mrshah on 10/26/2015.
 */
public class Globals {
    public static BroadcastReceiver onPushNotificationReceived = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {
            final String message = intent.getStringExtra("message");
            runOnUiThread(new Runnable() {

                ///////////////////////////////////////////////////
                /////////////////  PUSH Received  /////////////////
                ///////////////////////////////////////////////////
                @Override
                public void run() {
                    Log.d(Constants.PUSH_TAG, "msg: " + message);

                    ///////////////// Open Approve / Deny Dialog /////////////////
                    try {
                        final Dialog dialog = new Dialog(context);
                        dialog.setContentView(R.layout.push_msg_dialog);
                        dialog.setTitle("New Message");
                        TextView text = (TextView) dialog.findViewById(R.id.push_msg_text);
                        text.setText(message);

                        Button dialogButtonCancel = (Button) dialog.findViewById(R.id.push_dialog_button);
                        dialogButtonCancel.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dialog.dismiss();
                            }
                        });
                        dialog.show();
                    } catch (Exception e) {
                        Log.e(Constants.PUSH_TAG, "dialog error");
                        e.printStackTrace();
                    }
                }
            });
        }
    };

}
