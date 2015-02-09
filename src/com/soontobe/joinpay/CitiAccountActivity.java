package com.soontobe.joinpay;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

public class CitiAccountActivity extends Activity {

	final static String ContextString = "CitiAccountActivity";
	Context thisContext;
	EditText mUsername;
	EditText mPassword;
	View currentView;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_citi_account);
		IntentFilter restIntentFilter = new IntentFilter(Constants.RESTRESP);
		registerReceiver(bcReceiver, restIntentFilter);
		thisContext = getApplicationContext();
		getAccountInfo(Constants.userName);
		
	}
	
	@Override
	protected void onDestroy(){
		try{
			unregisterReceiver(bcReceiver);		//remove the receiver
		}
		catch(Exception e){}
	    super.onStop();
	}
	
	private BroadcastReceiver bcReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String receivedServiceContext = intent.getStringExtra("context");
			String url = intent.getStringExtra("url");
			int responseCode = intent.getIntExtra("code", 0);
			
			if(ContextString.equals(receivedServiceContext)) {
				if(url.equals(Constants.baseURL + "/myAccount")) {
					if(responseCode == 404) {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								FrameLayout rootLayout = (FrameLayout)findViewById(android.R.id.content);
								currentView = View.inflate(thisContext, R.layout.layout_create_account, rootLayout);
								mUsername = (EditText) currentView.findViewById(R.id.editText_account_username);
								mPassword = (EditText) currentView.findViewById(R.id.editText_account_password);
								Button linkButton = (Button) currentView.findViewById(R.id.button_link);
								linkButton.setOnClickListener(onLinkClicked);
							}
						});
					} else if(responseCode == 200) {
						String response = intent.getStringExtra("response");
						JSONObject obj;
						try {
							obj = new JSONObject(response);
						} catch (JSONException e) {
							e.printStackTrace();
							return;
						}
						showAccount(obj);
					} else {
						Toast.makeText(getApplicationContext(), "Error: Server Error", Toast.LENGTH_SHORT).show();
					}
				} else if(url.equals(Constants.baseURL + "/registerAccount")) {
					if(responseCode == 200) {
						String response = intent.getStringExtra("response");
						JSONObject obj;
						try {
							obj = new JSONObject(response);
						} catch (JSONException e) {
							e.printStackTrace();
							return;
						}
						showAccount(obj);
					} else if(responseCode == 403) {
						Toast.makeText(getApplicationContext(), "Invalid username/password", Toast.LENGTH_SHORT).show();						
					} else {
						Toast.makeText(getApplicationContext(), "Error: Server Error", Toast.LENGTH_SHORT).show();
					}
				}
			}
		}
	};
	
	protected void showAccount(JSONObject accountDetails) {
		String account_name_temp = "";
		String account_number_temp = "";
		String balance_temp = "";
		String first_name_temp = "";
		String last_name_temp = "";
		try {
			JSONObject account = accountDetails.getJSONArray("accounts").getJSONObject(0);

			account_name_temp = account.getString("account_name");
			account_number_temp = account.getString("account_number");
			balance_temp = account.getString("balance");
			first_name_temp = accountDetails.getString("first_name");
			last_name_temp = accountDetails.getString("last_name");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
		final String account_name = account_name_temp;
		final String account_number = account_number_temp;
		final String balance = balance_temp;
		final String first_name = first_name_temp;
		final String last_name = last_name_temp;
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
/*				if(currentView != null) {
					((ViewManager)currentView.getParent()).removeView(currentView);
				}
*/				FrameLayout rootLayout = (FrameLayout)findViewById(android.R.id.content);
				currentView = View.inflate(thisContext, R.layout.layout_citi_account, rootLayout);
				((TextView)currentView.findViewById(R.id.TextView_account_name)).setText(account_name);
				((TextView)currentView.findViewById(R.id.TextView_account_number)).setText(account_number);
				((TextView)currentView.findViewById(R.id.TextView_account_balance)).setText("$" + balance);
				((TextView)currentView.findViewById(R.id.TextView_account_firstname)).setText(first_name);
				((TextView)currentView.findViewById(R.id.TextView_account_lastname)).setText(last_name);
			}
		});
	}
	
	OnClickListener onLinkClicked = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			String username = mUsername.getText().toString();
			String password = mPassword.getText().toString();
			JSONObject authpair = new JSONObject();
			try {
				authpair.put("username", username);
				authpair.put("password", password);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			String url = Constants.baseURL + "/registerAccount";
			Intent intent = new Intent(getApplicationContext(), RESTCalls.class);
			intent.putExtra("method","post");
			intent.putExtra("body", authpair.toString());
			intent.putExtra("url",url);
			intent.putExtra("context", ContextString);

			Log.d("Citi Account Login", "starting Service");
			startService(intent);	
		}
	};
	
	private void getAccountInfo(String username) {
		String url = Constants.baseURL + "/myAccount";
		Intent intent = new Intent(getApplicationContext(), RESTCalls.class);
		intent.putExtra("method","get");
		intent.putExtra("url",url);
//		intent.putExtra("body", );
		intent.putExtra("context", ContextString);

		Log.d("Get myAccount", "starting Service");
		startService(intent);	
	}

}
