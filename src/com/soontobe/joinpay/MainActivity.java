package com.soontobe.joinpay;

import java.util.List;

import bolts.Continuation;
import bolts.Task;

import com.ibm.mobile.services.core.IBMBluemix;
import com.ibm.mobile.services.push.IBMPush;
import com.ibm.mobile.services.push.IBMPushNotificationListener;
import com.ibm.mobile.services.push.IBMSimplePushNotification;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;


/**
 * This class is the access point of the whole application. After the user hit the "JoinPay" button, it will jump to the radar view pane.
 *
 */
public class MainActivity extends Activity {
	private boolean mIsServiceStarted;
	private IBMPush push = null;
	private IBMPushNotificationListener notificationlistener = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		IBMBluemix.initialize(this, "bd885f97-e6e7-4f91-8365-98fc61162760", "25798ac2eb6b2d28f3fcee2f3795e4261d9591a0", "http://join-pay.mybluemix.net");
		//setContentView(R.layout.activity_main);
		
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);  //No Title Bar
		setContentView(R.layout.activity_main);
		
/*		WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		WifiInfo info = manager.getConnectionInfo();
		String address = info.getMacAddress();
		if(null == address){
			address = "fake_address";
		}
		Constants.userName = getUserNameByMacAddress(address);
		Log.d("MAC address", address);
		Log.d("User name", Constants.userName);
*/
		
		mIsServiceStarted = false;
		
		
	
		push = IBMPush.initializeService();
		notificationlistener = new IBMPushNotificationListener() {

			@Override
			public void onReceive(final IBMSimplePushNotification message) {
				Log.e("Message Received", "Push Notification Received" + message.toString());
				
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						//status.setText(message.toString());
						Log.d("push", "I GOT A FUCKING PUSH MESSAGE");
						Log.d("push", message.getAlert());
					}
				});
			}

		}; 

		push.register("dev4", Constants.userName).continueWith(new Continuation<String, Void>() {
			@Override
			public Void then(Task<String> task) throws Exception {
				if(task.isFaulted()) {
					//status.setText("Push Registration Failed");
					Log.e("push", "failed to push list of subscriptions");
					return null;
				} else {
					push.getSubscriptions().continueWith(new Continuation<List<String>, Void>()
							{
								public Void then(Task<List<String>> task1) throws Exception
								{
									if(task1.isFaulted()) {
										//status.setText("Push List of Subscriptions failed");
										Log.e("push", "failed to push list of subscriptions");
									} else {
										List<String> tags = task1.getResult();
										if(tags.size() > 0) {
											push.unsubscribe(tags.get(0)).continueWith(new Continuation<String, Void>() {

												@Override
												public Void then(
														Task<String> task2)
														throws Exception {
													if(task2.isFaulted()) {
														Log.e("push", "subscribe failed");
													} else {
														push.subscribe("testtag").continueWith(new Continuation<String, Void>() {
															public Void then(bolts.Task<String> task1) throws Exception {
																if(task1.isFaulted()) {
																	Log.e("push","Push Subscription Failed" + task1.getError().getMessage());	
																} else {
																	push.listen(notificationlistener);
																	Log.d("push","Push Subscription Success");								
																}
																return null;
															};
														});
													}
													// TODO Auto-generated method stub
													return null;
												}
											});
										} else {
											Log.d("push", "" + task1.getResult());
											push.subscribe("testtag").continueWith(new Continuation<String, Void>() {
												public Void then(bolts.Task<String> task1) throws Exception {
													if(task1.isFaulted()) {
														Log.e("push","Push Subscription Failed" + task1.getError().getMessage());	
													} else {
														push.listen(notificationlistener);
														Log.d("push","Push Subscription Success");								
													}
													return null;
												};
											});
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
	/*
	public String getUserNameByMacAddress(String address) {
		String ret = "User";
		for (int i = 0;i < Constants.macAddressToName.length;i++) {
			if (Constants.macAddressToName[i][0].equals(address)) {
				return Constants.macAddressToName[i][1];
			}
		}
		return ret;
	}
*/
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void onButtonClick(View view){
		Log.d("button", "click");
		startActivity(new Intent(this, RadarViewActivity.class));
		finish(); //Close current activity
	}

	public void onStartServiceClick(View v){
		Intent i = new Intent(getBaseContext(), MessageRetrievalService.class);;
		if(!mIsServiceStarted){
			startService(i);
			Log.d("Service", "started");
			mIsServiceStarted = true;
		} else {
			stopService(i);
			Log.d("Service", "stopped");
			mIsServiceStarted = false;
		}
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
	}
}
