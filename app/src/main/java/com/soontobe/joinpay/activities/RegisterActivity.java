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
import com.soontobe.joinpay.helpers.Rest;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * This activity shows registration page to the user.
 */
public class RegisterActivity extends Activity {
	/**
	 * Registration submit button.
	 */
	private Button butRegSubmit;

	/**
	 * Username edit text.
	 */
	private static EditText usernameText;

	/**
	 * Password edit text.
	 */
	private static EditText passText;

	/**
	 * Confirm password edit text.
	 */
	private static EditText confirmPassText;

	/**
	 * Context for this activity.
	 */
	private Context mContext;

	/**
	 * Temporary variable for username.
	 */
	private static String tempUser;

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		Log.d("register", "starting points");
		super.onCreate(savedInstanceState);
		mContext = this;
		requestWindowFeature(Window.FEATURE_NO_TITLE);  //No Title Bar
		setContentView(R.layout.activity_register);
		butRegSubmit = (Button) findViewById(R.id.button_registerSubmit);
		butRegSubmit.setOnClickListener(regSubmitClicked);
	}

	/**
	 * Callback for submit button
	 */
	private View.OnClickListener regSubmitClicked = new View.OnClickListener() {

		@Override
		public void onClick(final View v) {
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
			if (!passStr.equals(confirmPassStr)) {
				Log.e("register", "Password and confirm pass do not match");
				Toast tmp = Toast.makeText(getApplicationContext(), "Passwords do not match", Toast.LENGTH_LONG);
				tmp.setGravity(Gravity.TOP, Constants.TOP_X_OFFSET, Constants.TOP_Y_OFFSET);
				tmp.show();
				validInput = false;
			}
			if (validInput && passStr.length() < Constants.PASSWORD_MIN_LENGTH) {
				Log.e("register", "Password is too short, try harder");
				Toast tmp = Toast.makeText(getApplicationContext(), "Password is too small", Toast.LENGTH_LONG);
				tmp.setGravity(Gravity.TOP, Constants.TOP_X_OFFSET, Constants.TOP_Y_OFFSET);
				tmp.show();
				validInput = false;
			}
			if (validInput && usernameStr.length() < Constants.USERNAME_MIN_LENGTH) {
				Log.e("register", "Username is too short, try harder");
				Toast tmp = Toast.makeText(getApplicationContext(), "Username is too small", Toast.LENGTH_LONG);
				tmp.setGravity(Gravity.TOP, Constants.TOP_X_OFFSET, Constants.TOP_Y_OFFSET);
				tmp.show();
				validInput = false;
			}

			///// Register User /////
			if (validInput) {
				findViewById(R.id.button_registerSubmit).setEnabled(false);
				JSONObject obj = new JSONObject();
				try {
					obj.put("id", usernameStr);
					obj.put("password", passStr);
				} catch (JSONException e) {
					Log.e("register", "Error making JSON object for register");
					e.printStackTrace();
				}

				Rest.post(Constants.baseURL + "/users", null, null, obj.toString(), registerResponseHandler);
			}
		}
	};

	/**
	 * Response handler for register http call
	 */
	private Rest.httpResponseHandler registerResponseHandler = new Rest.httpResponseHandler() {
		@Override
		public void handleResponse(final JSONObject response, final boolean error) {
			if (!error) {
				int responseCode = 0;
				try {
					responseCode = response.getInt("responseCode");
				} catch (JSONException e) {
					showUIMessage("Invalid response from server, please try again");
					return;
				}
				switch (responseCode) {
					case Constants.RESPONSE_200:
						Log.d("register", "successfully registered new user");
						showUIMessage("Successfully Registered!");
						Constants.userName = tempUser;
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
