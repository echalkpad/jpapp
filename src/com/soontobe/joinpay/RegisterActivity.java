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
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class RegisterActivity extends Activity {
	
	final String serviceContext = "RegisterActivity";
	Button butRegSubmit;
	private static EditText usernameText;
	private static EditText passText;
	private static EditText confirmPassText;
	private static EditText accountId;
	public static String tempUser;
	PointAdapter adapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d("register", "starting points");
		super.onCreate(savedInstanceState);
		
		MainActivity.context = this;
		setContentView(R.layout.activity_register);
		butRegSubmit = (Button) findViewById(R.id.button_registerSubmit);
		butRegSubmit.setOnClickListener(regSubmitClicked);
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
				int httpCode = intent.getIntExtra("code", 403);
				Log.d("register", "Received Response - " + response);
				findViewById(R.id.button_registerSubmit).setEnabled(true);
				if(httpCode == 200){
					Log.d("register", "successfully registered new user");
					Toast tmp = Toast.makeText(getApplicationContext(), "Successfully Registered!", Toast.LENGTH_LONG);
					tmp.setGravity(Gravity.TOP, 0, 150);
					tmp.show();
					Constants.userName = tempUser;
					Intent intentApplication = new Intent(getApplicationContext(), MainActivity.class);			//send them in
					startActivity(intentApplication);
					finish();
				}
				else{
					Log.e("register", "failed to register new user");
					Toast tmp = Toast.makeText(getApplicationContext(), "Failed to register, try again", Toast.LENGTH_LONG);
					tmp.setGravity(Gravity.TOP, 0, 150);
					tmp.show();
				}
			}
		}
	};
	
	View.OnClickListener regSubmitClicked = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			Log.d("register", "clicked submit");
			usernameText = (EditText) findViewById(R.id.editText_username);
			passText = (EditText) findViewById(R.id.editText_password);
			confirmPassText = (EditText) findViewById(R.id.editText_passwordConfirm);
			String usernameStr = usernameText.getText().toString().trim();
			String passStr = passText.getText().toString().trim();
			String confirmPassStr = confirmPassText.getText().toString().trim();
			Boolean validInput = true;
			tempUser = usernameStr;
			
			///// Verify Input /////
			if(!passStr.equals(confirmPassStr)){				
				Log.e("register", "Password and confirm pass do not match");
				Toast tmp = Toast.makeText(getApplicationContext(), "Passwords do not match", Toast.LENGTH_LONG);
				tmp.setGravity(Gravity.TOP, 0, 150);
				tmp.show();
				validInput = false;
			}
			if(validInput && passStr.length() < 4){
				Log.e("register", "Password is too short, try harder");
				Toast tmp = Toast.makeText(getApplicationContext(), "Password is too small", Toast.LENGTH_LONG);
				tmp.setGravity(Gravity.TOP, 0, 150);
				tmp.show();
				validInput = false;
			}
			if(validInput && usernameStr.length() < 4){
				Log.e("register", "Username is too short, try harder");
				Toast tmp = Toast.makeText(getApplicationContext(), "Username is too small", Toast.LENGTH_LONG);
				tmp.setGravity(Gravity.TOP, 0, 150);
				tmp.show();
				validInput = false;
			}
			
			///// Register User /////
			if(validInput){
				findViewById(R.id.button_registerSubmit).setEnabled(false);
				JSONObject obj = new JSONObject();
				try {
					obj.put("username", usernameStr);
					obj.put("password", passStr);
				} catch (JSONException e) {
					Log.e("register", "Error making JSON object for register");
					e.printStackTrace();
				}
				
				Intent intent = new Intent(getApplicationContext(), RESTCalls.class);
				String url = Constants.baseURL + "/register";
				intent.putExtra("method","post");
				intent.putExtra("url", url);
				intent.putExtra("body", obj.toString());
				intent.putExtra("context", serviceContext);
		
				Log.d("register", "starting the service");
				startService(intent);
				IntentFilter restIntentFilter = new IntentFilter(Constants.RESTRESP);
				registerReceiver(restResponseReceiver, restIntentFilter);
			}
		}
	};
}
