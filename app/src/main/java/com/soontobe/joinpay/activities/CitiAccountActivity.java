package com.soontobe.joinpay.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.soontobe.joinpay.Constants;
import com.soontobe.joinpay.R;
import com.soontobe.joinpay.adapters.AccountJSONAdapter;
import com.soontobe.joinpay.helpers.RESTCalls;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * CitiAccountActivity shows the current user a summary of their Citi account.
 * TODO clean the code in this activity
 */
public class CitiAccountActivity extends Activity {

	// For navigating the JSONs from the Citi API
	private static final String TAG_ACCOUNTS = "accounts";
    private static final String TAG_FIRST_NAME = "first_name";
    private static final String TAG_LAST_NAME = "last_name";

	final static String ContextString = "CitiAccountActivity";
    final static String TAG = "citi";
	Context mContext;
	EditText metUsername;
	EditText metPassword;

	// For displaying the list of accounts.
	private ListView mlvAccounts;
	private AccountJSONAdapter mAdapter;
	private ProgressBar mpbSpinner;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        Log.d(TAG, "Creating CitiAccountActivity");
		setContentView(R.layout.layout_citi_account);

		//No Title Bar
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		// Get application context
		mContext = getApplicationContext();

		// Init UI
		initUI();

		// Set receiver on REST Calls
		IntentFilter restIntentFilter = new IntentFilter(Constants.RESTRESP);
		registerReceiver(bcReceiver, restIntentFilter);

		// Get account info
		getAccountInfo(Constants.userName);
	}

	/**
	 * Initialize the UI
	 */
	private void initUI() {
		// Initialize the account list Views
		mlvAccounts = (ListView) findViewById(R.id.accountListView);
		mAdapter = new AccountJSONAdapter(this, getLayoutInflater());
		mlvAccounts.setAdapter(mAdapter);

		// Capture the loading Spinner
		mpbSpinner = (ProgressBar) findViewById(R.id.accountsProgressBar);
		mpbSpinner.setVisibility(View.VISIBLE);
	}
	
	@Override
	protected void onDestroy(){
		try{
			unregisterReceiver(bcReceiver);		//remove the receiver
		}
		catch(Exception e){}
	    super.onDestroy();
	}

	/**
	 * Shows a toast on the screen for short interval
	 * @param message The message to be displayed on the screen
	 */
	private void showUIMessage(String message) {
		Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
	}
	
	private BroadcastReceiver bcReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String receivedServiceContext = intent.getStringExtra("context");
			String url = intent.getStringExtra("url");
			int responseCode = intent.getIntExtra("code", 0);
			
			if(ContextString.equals(receivedServiceContext)) {
				// If the response if for get my account
				if(url.equals(Constants.baseURL + "/myAccount")) {
					if(responseCode == 404) {
						// If there is not citi account linked, Ask to link a citi account
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								FrameLayout rootLayout = (FrameLayout)findViewById(android.R.id.content);
								View currentView = View.inflate(mContext, R.layout.layout_create_account, rootLayout);
								metUsername = (EditText) currentView.findViewById(R.id.editText_account_username);
								metPassword = (EditText) currentView.findViewById(R.id.editText_account_password);
								
								Button linkButton = (Button) currentView.findViewById(R.id.button_link);
								linkButton.setOnClickListener(onLinkClicked);
							}
						});
					} else if(responseCode == 200) {
						// If we get the details, show the account details on the screen
						String response = intent.getStringExtra("response");
						JSONObject obj;
						try {
							obj = new JSONObject(response);
						} catch (JSONException e) {
							showUIMessage("Unable to get the info. Please try again.");
							e.printStackTrace();
							return;
						}
						showAccount(obj);
					} else {
						// If there is any other response, show error on screen
						showUIMessage("Error: Server Error");
					}
				} else if(url.equals(Constants.baseURL + "/registerAccount")) {
					// If we are registering a new account
					if(responseCode == 200) {
						// The account is registered, show details on screen
						String response = intent.getStringExtra("response");
						JSONObject obj;
						try {
							obj = new JSONObject(response);
						} catch (JSONException e) {
							showUIMessage("Unable to get the info. Please try again.");
							e.printStackTrace();
							return;
						}
						showAccount(obj);
					} else if(responseCode == 403) {
						showUIMessage("Invalid username/password");
					} else {
						showUIMessage("Error: Server Error");
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
			mpbSpinner.setVisibility(View.GONE);
			showUIMessage("Unable to get account details, please try again.");
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
		mpbSpinner.setVisibility(View.GONE);
	}

	/**
	 * When link button is clicked, http post is performed
	 */
	OnClickListener onLinkClicked = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			String username = metUsername.getText().toString();
			String password = metPassword.getText().toString();
			JSONObject authpair = new JSONObject();
			try {
				authpair.put("username", username);
				authpair.put("password", password);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
//			Log.d("citi", authpair.toString());
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

	/**
	 * HTTP get performed to get the account info for the username
	 * @param username The username of the account of which the details are requested
	 */
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
