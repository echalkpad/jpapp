package com.soontobe.joinpay.activities;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.soontobe.joinpay.Constants;
import com.soontobe.joinpay.R;
import com.soontobe.joinpay.adapters.PointAdapter;
import com.soontobe.joinpay.helpers.Rest;

import org.apache.http.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;

public class RegisterActivity extends Activity {
	
	final String serviceContext = "RegisterActivity";
	Button butRegSubmit;
	private static EditText usernameText;
	private static EditText passText;
	private static EditText confirmPassText;
	private static EditText accountId;
	private Context mContext;
	public static String tempUser;
	PointAdapter adapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d("register", "starting points");
		super.onCreate(savedInstanceState);
		mContext = this;
		requestWindowFeature(Window.FEATURE_NO_TITLE);  //No Title Bar
		setContentView(R.layout.activity_register);
		butRegSubmit = (Button) findViewById(R.id.button_registerSubmit);
		butRegSubmit.setOnClickListener(regSubmitClicked);
	}
	
	@Override
	protected void onStop(){
	    super.onStop();
	}

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

				JSONObject auth = new JSONObject();
				Rest.post(Constants.baseURL + "/register", null, null, obj.toString(), registerResponseHandler);

/*				Intent intent = new Intent(getApplicationContext(), RESTCalls.class);
				String url = Constants.baseURL + "/register";
				intent.putExtra("method","post");
				intent.putExtra("url", url);
				intent.putExtra("body", obj.toString());
				intent.putExtra("context", serviceContext);
		
				Log.d("register", "starting the service");
				startService(intent);
				IntentFilter restIntentFilter = new IntentFilter(Constants.RESTRESP);
				registerReceiver(restResponseReceiver, restIntentFilter);*/
			}
		}
	};

	private Rest.httpResponseHandler registerResponseHandler = new Rest.httpResponseHandler() {
		@Override
		public void handleResponse(HttpResponse response, boolean error) {
			if (!error) {
				int responseCode = response.getStatusLine().getStatusCode();
				switch (responseCode) {
					case Constants.RESPONSE_200:
						Log.d("register", "successfully registered new user");
						showUIMessage("Successfully Registered!");
						Constants.userName = tempUser;
//						Intent intentApplication = new Intent(getApplicationContext(), MainActivity.class);			//send them in
//						startActivity(intentApplication);
						finish();
						break;
					default:
						Log.e("register", "failed to register new user");
						showUIMessage("Failed to register, try again");
						break;
				}
			} else {
				showUIMessage("Error connecting to server, please try again");
			}
		}
	};

	/**
	 * Shows a toast on the screen for short interval.
	 *
	 * @param message The message to be displayed on the screen
	 */
	private void showUIMessage(final String message) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
			}
		});
	}

}
