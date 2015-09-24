package com.soontobe.joinpay;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

public class PointBalance extends Activity {
	
	final String serviceContext = "PointsActivity";
	Button butBack;
	private static TextView pointsText;
	PointAdapter adapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d("points", "starting points");
		super.onCreate(savedInstanceState);
		
		MainActivity.context = this;
		setContentView(R.layout.activity_points);
		butBack = (Button) findViewById(R.id.points_button_back);
		butBack.setOnClickListener(backClicked);
		pointsText = (TextView) findViewById(R.id.point_balance);
		
		Intent intent = new Intent(getApplicationContext(), RESTCalls.class);
		String url = Constants.baseURL + "/rewards";
		intent.putExtra("method","get");
		intent.putExtra("url", url);
		intent.putExtra("context", serviceContext);

		Log.d("points", "starting the service");
		startService(intent);
		IntentFilter restIntentFilter = new IntentFilter(Constants.RESTRESP);
		registerReceiver(restResponseReceiver, restIntentFilter);
		
	}
	
	@Override
	protected void onStop(){
		try{
			unregisterReceiver(restResponseReceiver);		//remove the receiver
		}
		catch(Exception e){}
	    super.onStop();
	}
	public BroadcastReceiver restResponseReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String receivedServiceContext = intent.getStringExtra("context");
			
			if(serviceContext.equals(receivedServiceContext)) {
				String response = intent.getStringExtra("response");
				Log.d("points", "Received Response - " + response);
				parsePoints(response);
			}
		}
	};
	
	View.OnClickListener backClicked = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			Log.d("points", "clicked back");
			Intent intentApplication = new Intent(getApplicationContext(), MainActivity.class);			//go back
			startActivity(intentApplication);
			finish();
		}
	};
	
	public boolean parsePoints(String res){
		pointsText.setText("");
		final ListView listview = (ListView) findViewById(R.id.listview);
		try {
			JSONArray arr = new JSONArray(res);
			adapter = new PointAdapter(this, arr);
			listview.setAdapter(adapter);
		} catch (JSONException e) {
			pointsText.setText("error");
			return false;
		}
		return true;
	}

}
