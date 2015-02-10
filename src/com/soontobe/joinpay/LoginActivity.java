package com.soontobe.joinpay;

import org.json.JSONException;
import org.json.JSONObject;

import com.soontobe.joinpay.Constants;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends Activity {
	
	final String serviceContext = "LoginActivity";
	EditText mUsername;
	EditText mPassword;
	Button mLogin, butRegister;
	private Boolean changedUser = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);  					//No Title Bar
		setContentView(R.layout.activity_login);
		
		mUsername = (EditText) findViewById(R.id.editText_username);
		mUsername.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,int after) {}
			@Override
			public void afterTextChanged(Editable s) {
				changedUser = true;
			}
		});
		mPassword = (EditText) findViewById(R.id.editText_password);
		mPassword.setOnFocusChangeListener(new OnPasswordFocusChangeListener());
		
		mLogin = (Button) findViewById(R.id.button_login);
		mLogin.setOnClickListener(loginClicked);
		
		butRegister = (Button) findViewById(R.id.button_register);
		butRegister.setOnClickListener(registerClicked);
	}
	
	private class OnPasswordFocusChangeListener implements
		OnFocusChangeListener {
		@Override
		public void onFocusChange(View v, boolean hasFocus) {
			if(changedUser){
				mPassword.setText("");
				changedUser = false;
			}
		}
	}
	
	@Override
	protected void onStop(){
		try{
			unregisterReceiver(restResponseReceiver);		//remove the receiver
		}
		catch(Exception e){}
	    super.onStop();
	}
	
	BroadcastReceiver restResponseReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String receivedServiceContext = intent.getStringExtra("context");
			
			if(serviceContext.equals(receivedServiceContext)) {
				String response = intent.getStringExtra("response");
				int httpCode = intent.getIntExtra("code", 403);
				findViewById(R.id.button_login).setEnabled(true);
				String message = "error";
				try {
					JSONObject obj = new JSONObject(response);
					if(obj.has("message")) message = obj.getString("message");
				}
				catch (JSONException e) {
					Log.e("login", "Error parsing JSON response");
				}
				
				Log.d("login", "Received Response - " + response);
				
				//// Http Codes ////
				if(httpCode == 404){
					Log.d("login", "its a 404");
					Toast.makeText(getApplicationContext(), "Problem with server, try again later", Toast.LENGTH_SHORT).show();				
				}
				else if(httpCode == 401 || httpCode == 403){
					Toast tmp = Toast.makeText(getApplicationContext(), "Invalid credentials", Toast.LENGTH_SHORT);
					tmp.setGravity(Gravity.TOP, 0, 150);
					tmp.show();
				}
				else if(httpCode == 200){										//200 = parse the response
					Log.d("login", "starting location service");
					Intent locationServiceIntent = new Intent(getApplicationContext(), SendLocation.class);
					startService(locationServiceIntent);
					
					Log.d("login", "starting main activity");
					Intent intentApplication = new Intent(getApplicationContext(), MainActivity.class);
					startActivity(intentApplication);
					finish();
				}
				else{															//??? = error, do nothing
					Log.e("login", "response not understoood");
					Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
				}
			}
		}
	};
	
	View.OnClickListener loginClicked = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(getApplicationContext(), RESTCalls.class);
			JSONObject obj = new JSONObject();			
			String usernameStr = mUsername.getText().toString().trim();
			String passStr = mPassword.getText().toString().trim();
			Boolean validInput = true;
			
			///// Verify Input /////
			if(validInput && passStr.length() < 1){
				Log.e("login", "Password is too short, try harder");
				validInput = false;
			}
			if(validInput && usernameStr.length() < 3){
				Log.e("login", "Username is too short, try harder");
				validInput = false;
			}
			if(!validInput){
				Toast tmp = Toast.makeText(getApplicationContext(), "Invalid credentials", Toast.LENGTH_LONG);
				tmp.setGravity(Gravity.TOP, 0, 150);
				tmp.show();
			}
			
			///// Send Login /////
			if(validInput){
				findViewById(R.id.button_login).setEnabled(false);
				Constants.userName = usernameStr;
				try {
					obj.put("username", Constants.userName);
					obj.put("password", passStr);
				} catch (JSONException e) {
					Toast.makeText(getApplicationContext(), "Error creating JSON", Toast.LENGTH_SHORT).show();
				}
	
				String url = Constants.baseURL + "/login";
				intent.putExtra("method","post");
				intent.putExtra("url",url);
				intent.putExtra("body", obj.toString());
				intent.putExtra("context", serviceContext);
	
				Log.d("login", "starting Service");
				startService(intent);	
				IntentFilter restIntentFilter = new IntentFilter(Constants.RESTRESP);
				registerReceiver(restResponseReceiver, restIntentFilter);
			}
		}
	};
	
	View.OnClickListener registerClicked = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			Log.d("registerClicked", "starting activity");
			startActivity(new Intent(getApplicationContext(), RegisterActivity.class));
			//Intent intentApplication = new Intent(getApplicationContext(), RegisterActivity.class);
			//startActivity(intentApplication);
		}
	};

}
