package com.soontobe.joinpay;

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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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
