package com.soontobe.joinpay;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Messenger;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

/**
 * TODO write a description for this activity
 */
public class LoginActivity extends Activity {
	
	final String serviceContext = "LoginActivity";
	EditText mUsername;
	EditText mPassword;
	Button mLogin, mRegister;  // The login and "Need an account?" buttons
	private Boolean changedUser = false;

	Context thisContext;
	SendLocation mService;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);		// No Title Bar
		setContentView(R.layout.activity_login);

		thisContext = this;
        // Acquire login screen elements.
		mUsername = (EditText) findViewById(R.id.editText_username);
        mPassword = (EditText) findViewById(R.id.editText_password);

        // The password field should clear itself whenever the user name changes.
		mUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
			public void afterTextChanged(Editable s) {
				changedUser = true;
			}
		});
		mPassword.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (changedUser && hasFocus) {
					mPassword.setText("");
					changedUser = false;
				}
			}
		});
		
		mLogin = (Button) findViewById(R.id.button_login);
		mLogin.setOnClickListener(loginClicked);
		
		mRegister = (Button) findViewById(R.id.button_register);
		mRegister.setOnClickListener(registerClicked);

		IntentFilter restIntentFilter = new IntentFilter(Constants.RESTRESP);
		registerReceiver(restResponseReceiver, restIntentFilter);
	}
	
	@Override
	protected void onStop(){
	    super.onStop();
	}

	@Override
	protected void onDestroy() {
		try{
			unregisterReceiver(restResponseReceiver);		// remove the receiver
		} catch (Exception e) {
			Log.e("loginActivity", "Failed to unregister receiver: " + e.getMessage());

		}

		super.onDestroy();
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
					thisContext.bindService(locationServiceIntent, mConnection, Context.BIND_AUTO_CREATE);

					
					Log.d("login", "starting main activity");
					Intent intentApplication = new Intent(getApplicationContext(), MainActivity.class);
					startActivity(intentApplication);
					//finish();
				}
				else{															//??? = error, do nothing
					Log.e("login", "response not understoood");
					Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
				}
			}
		}
	};

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the object we can use to
			// interact with the service.  We are communicating with the
			// service using a Messenger, so here we get a client-side
			// representation of that from the raw IBinder object.
			mService = ((SendLocation.LocalBinder) service).getService();
			mService.updateLocation();
			if(mService != null)
				thisContext.unbindService(mConnection);
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			mService = null;
		}
	};

	/**
     * This object handles the process of logging the user in once the "Login" button is pressed.
     */
	View.OnClickListener loginClicked = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(getApplicationContext(), RESTCalls.class);
			JSONObject obj = new JSONObject();			
			String usernameStr = mUsername.getText().toString().trim();
			String passStr = mPassword.getText().toString().trim();
			Boolean validInput = true;
			
			// Validate the username and password and notify user if they are invalid
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
                Log.d("login", "Credentials validated locally. Beginning login process.");
				findViewById(R.id.button_login).setEnabled(false);
				Constants.userName = usernameStr; // Store current user name globally
                // Pack user and password into a JSON object for the REST request.
				try {
					obj.put("username", Constants.userName);
					obj.put("password", passStr);
				} catch (JSONException e) {
                    Log.e("login", "Failed to create user credential JSON: " + e.getMessage());
					Toast.makeText(getApplicationContext(), "Error creating JSON", Toast.LENGTH_SHORT).show();
				}

                ///// Basic Auth Stuff /////
                byte [] data = null;
                try{
                    data = (Constants.userName + ":" + passStr).getBytes("UTF-8");                  //convert to byte array
                }catch(UnsupportedEncodingException e1){

                }
                String base64 = Base64.encodeToString(data, Base64.DEFAULT).trim();                  //convert to base64 encoding
                String [] header = {"Authorization", "Basic " + base64};

                //// Build Req ////
				String url = Constants.baseURL + "/login";
				intent.putExtra("method","post");
                intent.putExtra("headers", header);
				intent.putExtra("url",url);
				intent.putExtra("body", obj.toString());
				intent.putExtra("context", serviceContext);
	
				Log.d("login", "starting REST service");
				startService(intent);

			}
		}
	};

    /**
     * This object handles when the user presses the "Register" button.  This should take the user
     * to a screen where they can create a Citi account.
     */
	View.OnClickListener registerClicked = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			Log.d("registerClicked", "starting registration activity");
			startActivity(new Intent(getApplicationContext(), RegisterActivity.class));
		}
	};

}
