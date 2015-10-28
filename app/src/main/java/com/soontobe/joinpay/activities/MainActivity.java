package com.soontobe.joinpay.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.soontobe.joinpay.Constants;
import com.soontobe.joinpay.Globals;
import com.soontobe.joinpay.R;
import com.soontobe.joinpay.helpers.IBMPushService;
import com.soontobe.joinpay.helpers.SendLocationService;

import java.io.File;
import java.util.Date;

/**
 * This class is the access point of the whole application.
 * After the user hit the "JoinPay" button, it will jump to the radar view pane.
 */
public class MainActivity extends Activity {

    /**
     * Debug tag for this activity.
     */
    private static final String TAG = "main_screen";

    /**
     * Context for this activity.
     */
    private static Context mContext;

    // UI variables
    /**
     * Sign up text view.
     */
    private TextView mtvLink;

    /**
     * Welcome username text view.
     */
    private TextView mtvUserView;

    @Override
    protected final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);    // No Title Bar
        setContentView(R.layout.activity_main);
        mContext = this;

        // Flush the cache whenever the app is started, so that the chat tab stays
        // up to date with the chat web page
        clearCache(this, getResources().getInteger(R.integer.minutes_to_keep));


        Intent pushServiceIntent = new Intent(this, IBMPushService.class);
        startService(pushServiceIntent);

        initUI();
    }

    @Override
    protected final void onPause() {
        super.onPause();
        unregisterReceiver(Globals.onPushNotificationReceived);
    }

    @Override
    protected final void onResume() {
        super.onResume();
        IntentFilter pushReceiveFilter = new IntentFilter(IBMPushService.MESSAGE_RECEIVED);
        registerReceiver(Globals.onPushNotificationReceived, pushReceiveFilter);
    }

    /**
     * When the user exits by clicking logout or clicking back button.
     */
    @Override
    protected final void onDestroy() {
        super.onDestroy();
        // Updates to the user's location should stop when they log out
        Intent locationServiceIntent = new Intent(getApplicationContext(), SendLocationService.class);
        stopService(locationServiceIntent);

        // Push notification service should stop when user logs out
        Intent pushServiceIntent = new Intent(this, IBMPushService.class);
        stopService(pushServiceIntent);

    }

    /**
     * Initialize the UI variables.
     */
    private void initUI() {
        // Populate a URL to allow users to create a Citi Bank account
        mtvLink = (TextView) findViewById(R.id.citiSignupTextView);
        mtvLink.setMovementMethod(LinkMovementMethod.getInstance());

        // Display a welcome message to the currently logged in user
        mtvUserView = (TextView) findViewById(R.id.welcome_user_name);
        String username = String.valueOf(Constants.userName.charAt(0)).toUpperCase() + Constants.userName.substring(1, Constants.userName.length());
        Log.d("USERNAME", username);
        mtvUserView.setText(String.format(getString(R.string.welcome_message), username));
    }


    /**
     * Recursively clears and deletes the given directory.
     *
     * @param dir        The directory to be deleted.
     * @param numMinutes The age after which files can be destroyed.
     * @return The number of deleted files.
     */
    private static int clearCacheFolder(final File dir, final int numMinutes) {
        int deletedFiles = 0;
        if (dir != null && dir.isDirectory()) {
            try {
                for (File child : dir.listFiles()) {
                    // Recursively delete subdirectories
                    if (child.isDirectory()) {
                        deletedFiles += clearCacheFolder(child, numMinutes);
                    }

                    // Delete files and subdirectories in this dir
                    // only empty dirs can be deleted, so subdirs are deleted recursively first
                    if (child.lastModified() < new Date().getTime() - numMinutes * DateUtils.MINUTE_IN_MILLIS) {
                        if (child.delete()) {
                            deletedFiles++;
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, String.format("Failed to clean the cache, error %s", e.getMessage()));
            }
        }
        return deletedFiles;
    }

    /**
     * Clears the cache folder of the application.
     *
     * @param context    The context in which the cache is stored.
     * @param numMinutes The age after which files are deleted.
     */
    public static void clearCache(final Context context, final int numMinutes) {
        Log.i(TAG, String.format("Starting cache prune. Deleting files older than %d minutes", numMinutes));
        int numDeletedFiles = clearCacheFolder(context.getCacheDir(), numMinutes);
        Log.i(TAG, String.format("Cache pruning completed. %d files deleted", numDeletedFiles));
    }

    /**
     * This method is called when the user presses the "Enter" button.  It starts the radar
     * activity.
     *
     * @param view The View that was clicked.
     */
    public final void onEnterClicked(final View view) {
        Log.d(TAG, "\"" + getString(R.string.button_enter) + "\" clicked");
        startActivity(new Intent(this, RadarViewActivity.class));
    }

    /**
     * This method is called when the user presses the "my accounts" button. It starts the
     * account activity.
     *
     * @param view The View that was clicked.
     */
    public final void onAccountsClicked(final View view) {
        Log.d(TAG, "\"" + getString(R.string.button_accounts) + "\" clicked");
        startActivity(new Intent(this, CitiAccountActivity.class));
    }

    /**
     * This method is called when the user presses the "logout" button.  It returns to the login
     * activity and ends this activity.
     *
     * @param view The View that was clicked.
     */
    public final void onLogoutClicked(final View view) {
        Log.d(TAG, "\"" + getString(R.string.button_logout) + "\" clicked");
        finish();
    }


}
