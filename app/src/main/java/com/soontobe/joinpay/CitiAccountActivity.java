package com.soontobe.joinpay;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.method.MovementMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;

import static com.soontobe.joinpay.R.id.accountListView;

/**
 * CitiAccountActivity shows the current user a summary of their Citi account.
 */
public class CitiAccountActivity extends Activity {

	// For navigating the JSONs from the Citi API
	private static final String TAG_ACCOUNTS = "accounts";
    private static final String TAG_FIRST_NAME = "first_name";
    private static final String TAG_LAST_NAME = "last_name";

	final static String ContextString = "CitiAccountActivity";
    final static String TAG = "citi";
	Context thisContext;
	EditText mUsername;
	EditText mPassword;
	View currentView;

	// For displaying the list of accounts.
	private ListView mAccountListView;
	private AccountJSONAdapter mAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        Log.d(TAG, "Creating CitiAccountActivity");
        requestWindowFeature(Window.FEATURE_NO_TITLE);  //No Title Bar
		setContentView(R.layout.layout_citi_account);

        // Initialize the account list Views
        mAccountListView = (ListView) findViewById(R.id.accountListView);
        mAdapter = new AccountJSONAdapter(this, getLayoutInflater());
        mAccountListView.setAdapter(mAdapter);

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
	    super.onDestroy();
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
								
								/*TextView link = (TextView) currentView.findViewById(R.id.citiLink);
							    String linkText = "Visit the <a href='http://stackoverflow.com'>StackOverflow</a> web page.";
							    link.setText(Html.fromHtml(linkText));
							    link.setMovementMethod(test);
							    link.setMovementMethod(LinkMovementMethod.getInstance());*/
								
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

    /**
     * Parses a JSON object containing user account information and populates it to the screen.
     * @param accountDetails The account information to be parsed.
     */
	protected void showAccount(JSONObject accountDetails) {

		// Attempt to pull the users accounts from the given JSON
		Log.d(TAG, "Parsing account details.");
        JSONArray accounts = accountDetails.optJSONArray(TAG_ACCOUNTS);
		if(accounts == null) {
			Log.e(TAG, "Tag:\"" + TAG_ACCOUNTS + "\" yielded no account information.");
			return;
		}

        // Parse out the users name from the JSON
        String first = accountDetails.optString(TAG_FIRST_NAME);
        String last = accountDetails.optString(TAG_LAST_NAME);
        String name = first + " " + last;
        if(first == null || last == null) {
            Log.e(TAG, "Tags:\"" + TAG_FIRST_NAME + "\" and \"" + TAG_LAST_NAME +
                    "\" yielded \"" + name + "\"");
            name = "@string/citi_fallback_holder";
        }

		// Update the adapter's dataset
		mAdapter.updateData(accounts, name);

		//

		/*
		String account_name_temp = "";
		String account_number_temp = "";
		String balance_temp = "";
		String first_name_temp = "";
		String last_name_temp = "";

        // Attempt to parse the JSON object.
		try {
			JSONObject account = accountDetails.getJSONArray("accounts").getJSONObject(0);
			account_name_temp = account.getString("account_name");
			account_number_temp = account.getString("account_number");
			balance_temp = account.getString("balance");
			first_name_temp = accountDetails.getString("first_name");
			last_name_temp = accountDetails.getString("last_name");
		} catch (JSONException e) {
			Log.e(TAG, "Failed to parse account information: " + e.getMessage());
			return;
		}

        // Format the account balance into a pretty string
		double amount = Double.parseDouble(balance_temp);
		DecimalFormat formatter = new DecimalFormat("#,###.00");
		Log.d(TAG, formatter.format(amount));
		final String account_name = account_name_temp;
		final String account_number = account_number_temp;
		final String balance = formatter.format(amount);
		final String first_name = first_name_temp;
		final String last_name = last_name_temp;

        // Populate the Views with the account information.
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
                FrameLayout rootLayout = (FrameLayout)findViewById(android.R.id.content);
				currentView = View.inflate(thisContext, R.layout.layout_citi_account, rootLayout);
				((TextView)currentView.findViewById(R.id.TextView_account_name)).setText(account_name);
				((TextView)currentView.findViewById(R.id.TextView_account_number)).setText(account_number);
				((TextView)currentView.findViewById(R.id.TextView_account_balance)).setText("$" + balance);
				((TextView)currentView.findViewById(R.id.TextView_account_firstname)).setText(first_name);
				((TextView)currentView.findViewById(R.id.TextView_account_lastname)).setText(last_name);
			}
		});
		*/
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
			
			Log.d("citi", authpair.toString());
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
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra("method","get");
		intent.putExtra("url",url);
//		intent.putExtra("body", );
		intent.putExtra("context", ContextString);

		Log.d("Get myAccount", "starting Service");
		startService(intent);	
	}

}
