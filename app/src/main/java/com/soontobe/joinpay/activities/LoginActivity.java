package com.soontobe.joinpay.activities;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
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

import com.soontobe.joinpay.Constants;
import com.soontobe.joinpay.R;
import com.soontobe.joinpay.helpers.Rest;
import com.soontobe.joinpay.helpers.SendLocationService;

import org.apache.http.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This is the first activity that a user sees when they start the application.  It is a
 * login screen that allows users to input a username and password and login to JoinPay's
 * APIs and the application.
 */
public class LoginActivity extends Activity {

    /**
     * Debug Tag for this class.
     */
    private static final String TAG = "login_activity";

    /**
     * Edit text for username.
     */
    private EditText metUsername;

    /**
     * Edit text for password.
     */
    private EditText metPassword;

    /**
     * Login & "need an account?" button.
     */
    private Button mbtnLogin, mbtnRegister;

    /**
     * To check if username is changed.
     */
    private Boolean mbChangedUser = false;

    /**
     * Save the context of this activity.
     */
    private Context mContext;

    // Used to send our location to other users
    /**
     * Location service to keep publishing the location of the user.
     */
    private SendLocationService mLocationService;

    @Override
    protected final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);        // No Title Bar
        setContentView(R.layout.activity_login);

        mContext = this;
        initUI();
    }

    /**
     * Initialize the UI components.
     */
    private void initUI() {
        // Acquire login screen elements.
        metUsername = (EditText) findViewById(R.id.editText_username);
        metPassword = (EditText) findViewById(R.id.editText_password);

        // The password field should clear itself whenever the user name changes.
        metUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
            }

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
            }

            @Override
            public void afterTextChanged(final Editable s) {
                mbChangedUser = true;
            }
        });

        // Clear the password when the focus is on the password field
        // and username was changed
        metPassword.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(final View v, final boolean hasFocus) {
                if (mbChangedUser && hasFocus) {
                    metPassword.setText("");
                    mbChangedUser = false;
                }
            }
        });

        // Set login button callback
        mbtnLogin = (Button) findViewById(R.id.button_login);
        mbtnLogin.setOnClickListener(loginClicked);

        // Set register button callback
        mbtnRegister = (Button) findViewById(R.id.button_register);
        mbtnRegister.setOnClickListener(registerClicked);
    }

    @Override
    protected final void onStop() {
        super.onStop();
    }

    @Override
    protected final void onDestroy() {
        super.onDestroy();
    }

    /**
     * This allows us to update the user's location in the database.
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        /**
         * Fires off an updateLocation() call on the location service.
         * and then immediately unbinds the service
         * because we only want to update the location once.
         */
        public void onServiceConnected(final ComponentName className, final IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service.  We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            mLocationService = ((SendLocationService.LocalBinder) service).getService();
            mLocationService.updateLocation();
            if (mLocationService != null) {
                mContext.unbindService(mConnection);
            }
        }

        // Will get called when the service is unbound above
        public void onServiceDisconnected(final ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mLocationService = null;
        }
    };

    /**
     * This object handles the process of logging the user in once the "Login" button is pressed.
     */
    private View.OnClickListener loginClicked = new View.OnClickListener() {

        private final String tag = "login";

        @Override
        public void onClick(final View v) {
            JSONObject obj = new JSONObject();
            String usernameStr = metUsername.getText().toString().trim();
            String passStr = metPassword.getText().toString().trim();
            Boolean validInput = true;

            // Validate the username and password and notify user if they are invalid
            if (validInput && passStr.length() < Constants.PASSWORD_MIN_LENGTH) {
                Log.e(tag, "Password is too short, try harder");
                validInput = false;
            }
            if (validInput && usernameStr.length() < Constants.USERNAME_MIN_LENGTH) {
                Log.e(tag, "Username is too short, try harder");
                validInput = false;
            }
            if (!validInput) {
                Toast tmp = Toast.makeText(getApplicationContext(), "Invalid credentials", Toast.LENGTH_LONG);
                tmp.setGravity(Gravity.TOP, Constants.TOP_X_OFFSET, Constants.TOP_Y_OFFSET);
                tmp.show();
            }

            ///// Send Login /////
            if (validInput) {
                Log.d(tag, "Credentials validated locally. Beginning login process.");
                findViewById(R.id.button_login).setEnabled(false);  // Don't want two of these processes firing off
                Constants.userName = usernameStr; // Store current user name globally
                Constants.password = passStr;
                JSONObject auth = new JSONObject();
                try {
                    auth.put("type", "basic");
                    auth.put("username", Constants.userName);
                    auth.put("password", passStr);
                    Rest.post(Constants.baseURL + "/login", auth, null, auth.toString(), loginResponseHandler);
                } catch (JSONException e) {

                }
            }
        }
    };

    /**
     * This object handles when the user presses the "Register" button.  This should take the user
     * to a screen where they can create a Citi account.
     */
    private View.OnClickListener registerClicked = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            Log.d("registerClicked", "starting registration activity");
            startActivity(new Intent(getApplicationContext(), RegisterActivity.class));
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

    /**
     * Response handler for login http call.
     */
    private Rest.httpResponseHandler loginResponseHandler = new Rest.httpResponseHandler() {
        @Override
        public void handleResponse(final HttpResponse response, final boolean error) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    findViewById(R.id.button_login).setEnabled(true);  // It's OK to send another request now
                }
            });
            if (!error) {
                int httpCode = response.getStatusLine().getStatusCode();
                switch (httpCode) {
                    case Constants.RESPONSE_404:
                        Log.d(TAG, "failed, alerting user");
                        showUIMessage("Problem with server, try again later");
                        break;

                    case Constants.RESPONSE_401:
                    case Constants.RESPONSE_403:
                        Log.d(TAG, "invalid credentials, alerting user");
                        showUIMessage("Invalid credentials");
                        break;

                    case Constants.RESPONSE_200:
                        // Start the location service so other users can see you on their radars
                        Log.d(TAG, "NEW starting location service");
                        Intent locationServiceIntent = new Intent(getApplicationContext(), SendLocationService.class);
                        mContext.bindService(locationServiceIntent, mConnection, Context.BIND_AUTO_CREATE);

                        // MainActivity is currently the landing screen for the user
                        Log.d(TAG, "NEW starting main activity");
                        Intent intentApplication = new Intent(getApplicationContext(), MainActivity.class);
                        startActivity(intentApplication);
                        break;

                    default:
                        String message = response.getEntity().toString();
                        showUIMessage(message);
                        break;
                }
            } else {
                showUIMessage("Error connecting to server. Check internet?");
            }
        }
    };
}
