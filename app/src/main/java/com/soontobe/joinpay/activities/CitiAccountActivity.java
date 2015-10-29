package com.soontobe.joinpay.activities;

import android.app.Activity;
import android.content.Context;
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
import com.soontobe.joinpay.Globals;
import com.soontobe.joinpay.R;
import com.soontobe.joinpay.adapters.AccountJSONAdapter;
import com.soontobe.joinpay.helpers.Rest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * CitiAccountActivity shows the current user a summary of their Citi account.
 * TODO clean the code in this activity
 */
public class CitiAccountActivity extends Activity {

	/**
	 * "accounts" key For navigating the JSONs from the Citi API.
	 */
	private static final String TAG_ACCOUNTS = "accounts";

	/**
	 * "first_name" key For navigating the JSONs from the Citi API.
	 */
	private static final String TAG_FIRST_NAME = "first_name";

	/**
	 * "last_name" key For navigating the JSONs from the Citi API.
	 */
	private static final String TAG_LAST_NAME = "last_name";

	/**
	 * Debug tag for this class.
	 */
	private static final String TAG = "citi";

	/**
	 * To store the context when in this activity.
	 */
	private Context mContext;

	/**
	 * To store the username edit text object.
	 */
	private EditText metUsername;

	/**
	 * To store the password edit text object.
	 */
	private EditText metPassword;

	/**
	 * Listview to display the list of accounts.
	 */
	private ListView mlvAccounts;

	/**
	 * Adapter supporting the accounts list view.
	 */
	private AccountJSONAdapter mAdapter;

	/**
	 * The wait spinner when accounts are being loaded.
	 */
	private ProgressBar mpbSpinner;

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "Creating CitiAccountActivity");

		//No Title Bar
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.layout_citi_account);

		// Get application context
		mContext = getApplicationContext();

		// Init UI
		initUI();

		// Get account info
		getAccountInfo();
	}

	/**
	 * Initialize the UI.
	 */
	private void initUI() {
		// Initialize the account list Views
		mlvAccounts = (ListView) findViewById(R.id.accountListView);
		mAdapter = new AccountJSONAdapter(this, getLayoutInflater());
		mlvAccounts.setAdapter(mAdapter);

		// Capture the loading Spinner
		mpbSpinner = (ProgressBar) findViewById(
				R.id.accountsProgressBar);
		mpbSpinner.setVisibility(View.VISIBLE);
	}

	@Override
	protected final void onDestroy() {
		super.onDestroy();
	}

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

	/**
	 * To handle account not found response.
	 * If the account not found response is received,
	 * user should be able to link an account
	 */
	private Runnable mrHandleAccountNotFound = new Runnable() {
		@Override
		public void run() {
			FrameLayout rootLayout = (FrameLayout) findViewById(android.R.id.content);
			View currentView = View.inflate(mContext, R.layout.layout_create_account, rootLayout);
			metUsername = (EditText) currentView.findViewById(R.id.editText_account_username);
			metPassword = (EditText) currentView.findViewById(R.id.editText_account_password);

			Button linkButton = (Button) currentView.findViewById(R.id.button_link);
			linkButton.setOnClickListener(onLinkClicked);
		}
	};

	/**
	 * Parses a JSON object containing user account information and populates it to the screen.
	 *
	 * @param accountDetails The account information to be parsed.
	 */
	protected final void showAccount(final JSONObject accountDetails) {


		JSONObject citibank = accountDetails.optJSONObject("citibank");
		// Attempt to pull the users accounts from the given JSON
		Log.d(TAG, "Parsing account details.");
		JSONArray accounts = citibank.optJSONArray(TAG_ACCOUNTS);
		if (accounts == null) {
			Log.e(TAG, "Tag:\"" + TAG_ACCOUNTS + "\" yielded no account information.");
			mpbSpinner.setVisibility(View.GONE);
			showUIMessage("Unable to get account details, please try again.");
			return;
		}

		Log.d("AccountDetails", accountDetails.toString());
		// Parse out the users name from the JSON
		String first = citibank.optString(TAG_FIRST_NAME);
		String last = citibank.optString(TAG_LAST_NAME);
		String name = first + " " + last;
		if (first == null || last == null) {
			Log.e(TAG, "Tags:\"" + TAG_FIRST_NAME + "\" and \"" + TAG_LAST_NAME
					+ "\" yielded \"" + name + "\"");
			name = "@string/citi_fallback_holder";
		}

		// Update the adapter's dataset
		mAdapter.updateData(accounts, name);
		mpbSpinner.setVisibility(View.GONE);
	}

	/**
	 * When link button is clicked, http post is performed.
	 */
	private OnClickListener onLinkClicked = new View.OnClickListener() {

		@Override
		public void onClick(final View v) {
			String username = metUsername.getText().toString();
			String password = metPassword.getText().toString();
			JSONObject body = new JSONObject();
			try {
				body.put("type", "basic");
				body.put("citi_account", username);
				body.put("citi_password", password);
				String url = Constants.baseURL + "/users/" + Constants.userName + "/citibank?"
						+ "access_token=" + Globals.msToken;
				Rest.put(url, null, null, body.toString(), registerAccountResponseHandler);
			} catch (JSONException e) {

			}
		}
	};

	/**
	 * HTTP get performed to get the account info for the username.
	 */
	private void getAccountInfo() {
		String url = Constants.baseURL + "/users/" + Constants.userName + "/citibank/details?" + "access_token=" + Globals.msToken
				+ "&username=" + Constants.userName;
		Rest.get(url, null, null, getAccountsResponseHandler);
	}

	/**
	 * Response handler for get accounts http call.
	 */
	private Rest.httpResponseHandler getAccountsResponseHandler = new Rest.httpResponseHandler() {
		@Override
		public void handleResponse(final JSONObject response, final boolean error) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Button btn = (Button) findViewById(R.id.button_login);
					if (btn != null) {
						btn.setEnabled(true);  // It's OK to send another request now
					}
				}
			});
			if (!error) {
				int responseCode = 0;
				try {
					responseCode = response.getInt("responseCode");
				} catch (JSONException e) {
					e.printStackTrace();
					showUIMessage("Unable to get the info. Please try again.");
					return;
				}
				switch (responseCode) {
					case Constants.RESPONSE_404:
						runOnUiThread(mrHandleAccountNotFound);
						break;

					case Constants.RESPONSE_200:
						// If we get the details,
						// show the account details on the screen
						String responseStr = "";
						final JSONObject obj;
						try {
							responseStr = response.getString("data");
							obj = new JSONObject(responseStr);
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									showAccount(obj);
								}
							});
						} catch (Exception e) {
							e.printStackTrace();
							showUIMessage("Unable to get the info. Please try again.");
							return;
						}
						break;

					default:
						showUIMessage("Error: Server Error");
						break;

				}
			} else {
				showUIMessage("Error: Server Error");
			}
		}
	};

	/**
	 * Response handler for register account http call.
	 */
	private Rest.httpResponseHandler registerAccountResponseHandler = new Rest.httpResponseHandler() {
		@Override
		public void handleResponse(final JSONObject response, final boolean error) {
			if (!error) {
				int responseCode = 0;
				try {
					responseCode = response.getInt("responseCode");
				} catch (JSONException e) {
					showUIMessage("Unable to get the info. Please try again.");
					e.printStackTrace();
					return;
				}
				switch (responseCode) {
					case Constants.RESPONSE_200:
						final JSONObject obj;
						try {
							String responseStr = response.getString("data");
							obj = new JSONObject(responseStr);
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									showUIMessage("Account Linked Successfully");
									finish();
								}
							});
						} catch (Exception e) {
							showUIMessage("Unable to get the info. Please try again.");
							e.printStackTrace();
							return;
						}
						break;

					case Constants.RESPONSE_403:
						showUIMessage("Invalid username/password");
						break;

					default:
						showUIMessage("Error: Server Error");
						break;
				}
			} else {
				showUIMessage("Error: Server Error. Please try again.");
			}
		}
	};
}