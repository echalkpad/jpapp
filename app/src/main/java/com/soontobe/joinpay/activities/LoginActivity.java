package com.soontobe.joinpay.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.util.Base64;

import com.soontobe.joinpay.Constants;
import com.soontobe.joinpay.R;
import com.soontobe.joinpay.helpers.RESTCalls;
import com.soontobe.joinpay.helpers.SendLocation;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

/**
 * This is the first activity that a user sees when they start the application.  It is a
 * login screen that allows users to input a username and password and login to JoinPay's
 * APIs and the application.
 */
public class LoginActivity extends Activity {

    private final String serviceContext = "LoginActivity";
    private static final String TAG = "login_activity";
    private EditText metUsername;
    private EditText metPassword;

    // The login and "Need an account?" buttons
    private Button mbtnLogin, mbtnRegister;
    private Boolean mbChangedUser = false;

    private Context mContext;

    // Used to send our location to other users
    private SendLocation mLocationService;

    @Override
    protected final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);        // No Title Bar
        setContentView(R.layout.activity_login);

        mContext = this;
        initUI();
        // This activity sends REST requests in order to log users in.  This sets it up
        // to receive the results of these requests.
        IntentFilter restIntentFilter = new IntentFilter(Constants.RESTRESP);
        registerReceiver(restResponseReceiver, restIntentFilter);
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
        try {
            // remove the receiver
            unregisterReceiver(restResponseReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Failed to unregister receiver: " + e.getMessage());
        }
        super.onDestroy();
    }

    /**
     * Rest calls response receiver.
     */
    private BroadcastReceiver restResponseReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(final Context context, final Intent intent) {
            String receivedServiceContext = intent.getStringExtra("context");

            // Check that the response is actually intended for us.
            if (serviceContext.equals(receivedServiceContext)) {
                String response = intent.getStringExtra("response");
                int httpCode = intent.getIntExtra("code", Constants.RESPONSE_403);
                Log.d(TAG, String.format("Received %d Response: %s", httpCode, response));

                findViewById(R.id.button_login).setEnabled(true);  // It's OK to send another request now
                String message = "";
                try {
                    // Extract the message from the JSON
                    JSONObject obj = new JSONObject(response);
                    message = obj.optString("message", "error");
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing JSON response");
                }

                //// Http Codes ////
                if (httpCode == Constants.RESPONSE_404) {
                    Log.d(TAG, "failed, alerting user");
                    Toast.makeText(getApplicationContext(), "Problem with server, try again later", Toast.LENGTH_SHORT).show();
                } else if (httpCode == Constants.RESPONSE_401 || httpCode == Constants.RESPONSE_403) {
                    Log.d(TAG, "invalid credentials, alerting user");
                    Toast tmp = Toast.makeText(getApplicationContext(), "Invalid credentials", Toast.LENGTH_SHORT);
                    tmp.setGravity(Gravity.TOP, Constants.TOP_X_OFFSET, Constants.TOP_Y_OFFSET);
                    tmp.show();
                } else if (httpCode == Constants.RESPONSE_200) {                                //200 = parse the response
                    // Start the location service so other users can see you on their radars
                    Log.d(TAG, "starting location service");
                    Intent locationServiceIntent = new Intent(getApplicationContext(), SendLocation.class);
                    mContext.bindService(locationServiceIntent, mConnection, Context.BIND_AUTO_CREATE);

                    // MainActivity is currently the landing screen for the user
                    Log.d(TAG, "starting main activity");
                    Intent intentApplication = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intentApplication);
                } else {                                                            //??? = error, do nothing
                    Log.e(TAG, "response not understoood");
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                }
            }
        }
    };

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
            mLocationService = ((SendLocation.LocalBinder) service).getService();
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
            Intent intent = new Intent(getApplicationContext(), RESTCalls.class);

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

                // Pack user and password into a JSON object for the REST request.
                try {
                    obj.put("username", Constants.userName);
                    obj.put("password", passStr);
                } catch (JSONException e) {
                    Log.e(tag, "Failed to create user credential JSON: " + e.getMessage());
                    Toast.makeText(getApplicationContext(), "Error creating JSON", Toast.LENGTH_SHORT).show();
                }

                ///// Basic Auth Stuff /////
                byte[] data = null;
                String encoding = "UTF-8";
                try {
                    data = (Constants.userName + ":" + passStr).getBytes(encoding);                  //convert to byte array
                } catch (UnsupportedEncodingException e1) {
                    Log.e(tag, "Failed to encode user credentials to " + encoding);
                }
                String base64 = Base64.encodeToString(data, Base64.DEFAULT).trim();                  //convert to base64 encoding
                String[] header = {"Authorization", "Basic " + base64};

                //// Build Req ////
                String url = Constants.baseURL + "/login";
                intent.putExtra("method", "post");
                intent.putExtra("headers", header);
                intent.putExtra("url", url);
                intent.putExtra("body", obj.toString());
                intent.putExtra("context", serviceContext);

                Log.d(tag, "starting REST service");
                startService(intent);

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

}
