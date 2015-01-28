package com.soontobe.joinpay;

import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import com.soontobe.joinpay.Constants;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends Activity {
	
	final String serviceContext = "LoginActivity";
	EditText mUsername;
	EditText mPassword;
	Button mLogin;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		
		mUsername = (EditText) findViewById(R.id.editText_username);
		mPassword = (EditText) findViewById(R.id.editText_password);
		
		mLogin = (Button) findViewById(R.id.button_login);
		mLogin.setOnClickListener(loginClicked);
	}
	
	BroadcastReceiver restResponseReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String receivedServiceContext = intent.getStringExtra("context");
			
			if(serviceContext.equals(receivedServiceContext)) {
				String url = intent.getStringExtra("url");
				String method = intent.getStringExtra("method");
				String response = intent.getStringExtra("response");
				
				try {
					JSONObject obj = new JSONObject(response);
//					Constants.loginToken = obj.getString("sessionID");
				} catch (JSONException e) {
					Toast.makeText(getApplicationContext(), "Invalid credentials", Toast.LENGTH_SHORT).show();
					return;
				}
				Log.d("bcReceiver", "Received Response - " + response);
				
				Intent locationServiceIntent = new Intent(getApplicationContext(), SendLocation.class);
				startService(locationServiceIntent);
				Log.d("bcReceiver", "Service Started");
				Intent intentApplication = new Intent(getApplicationContext(), MainActivity.class);
				startActivity(intentApplication);
			}
		}
	};
	
	View.OnClickListener loginClicked = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(getApplicationContext(), RESTCalls.class);
			JSONObject obj = new JSONObject();
			Constants.userName = mUsername.getText().toString();
			try {
				obj.put("username", Constants.userName);
				obj.put("password", mPassword.getText());
			} catch (JSONException e) {
				Toast.makeText(getApplicationContext(), "Error creating JSON", Toast.LENGTH_SHORT).show();
			}

			String url = Constants.baseURL + "/login";
			intent.putExtra("method","post");
			intent.putExtra("url",url);
			intent.putExtra("body", obj.toString());
			intent.putExtra("context", serviceContext);
			
			

			Log.d("loginClicked", "starting Service");
			startService(intent);
			Log.d("loginClicked", "started Service");

			IntentFilter restIntentFilter = new IntentFilter(Constants.RESTRESP);
			registerReceiver(restResponseReceiver, restIntentFilter);
		
		}
	};

}