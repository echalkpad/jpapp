package com.soontobe.joinpay.activities;


import android.annotation.SuppressLint;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import android.widget.Toast;

import com.soontobe.joinpay.Constants;
import com.soontobe.joinpay.Globals;
import com.soontobe.joinpay.PositionHandler;
import com.soontobe.joinpay.R;
import com.soontobe.joinpay.fragment.ChatFragment;
import com.soontobe.joinpay.fragment.HistoryFragment;
import com.soontobe.joinpay.fragment.RequestFragment;
import com.soontobe.joinpay.fragment.TransactionFragment;
import com.soontobe.joinpay.helpers.IBMPushService;
import com.soontobe.joinpay.helpers.Rest;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/* TODO:
 * 		1. Numeric Keyboard should contain arithmetic operations
 */

/**
 * Radar View Activity.
 * Users can see the friends nearby, select them and split check with them.
 */
public class RadarViewActivity extends FragmentActivity
implements OnTabChangeListener, TransactionFragment.OnFragmentInteractionListener,
HistoryFragment.OnFragmentInteractionListener {

	/**
	 * The tabhost for all the tabs.
	 */
	private TabHost mTabHost;

	/**
	 * Request fragment object.
	 */
	private RequestFragment mRequestFragment;

	/**
	 * History fragment object.
	 */
	private HistoryFragment mHistoryFragment;

	/**
	 * Chat fragment object.
	 */
	private ChatFragment mChatFragment;

	/**
	 * Key to put the value from MessageRetrievalService.
	 */
	public static final String JUMP_KEY = "_jump";

	/**
	 * Debug tag for radar view activity.
	 */
	private static final String TAG = "RadarViewActivity";

	/**
	 * Debug tag for request fragment.
	 */
	private static final String TAG_REQUEST = "tab_request";

	/**
	 * Debug tag for history fragment.
	 */
	private static final String TAG_HISTORY = "tab_history";

	/**
	 * Debug tag for chat fragment.
	 */
	private static final String TAG_CHAT = "tab_chat";

	/**
	 * Request code to communicate with ContactListActivity.
	 */
	private static final int CONTACT_LIST_REQUEST_CODE = 1;

	/**
	 * Request code to communicate with SendConfirmActivity.
	 */
	private static final int PROCEED_TO_CONFIRM_REQUEST_CODE = 2;

	/**
	 * Request code to get data from MessageRetrievalService.
	 */
	public static final int HISTORY_REQUEST_CODE = 3;

	/**
	 * Request Tab id.
	 */
	private static final int REQUEST_TAB = 0;

	/**
	 * History tab id.
	 */
	private static final int HISTORY_TAB = 1;

	/**
	 * To identify completion in message handler.
	 */
	private static final int COMPLETED = 0;

	/**
	 * Task to find nearby users.
	 */
	private NearbyUsersAsyncTask mAsyncTaskNearby = null;

	/**
	 * List of payment information history.
	 */
	private ArrayList<String[]> paymentInfo;

	/**
	 *
	 */
	public Map<String, Boolean> lockInfo;

	/**
	 * Maximum positions of friends can be shown on radar view.
	 */
	static final int MAX_POSITIONS = PositionHandler.MAX_USER_SUPPORTED;

	/**
	 * List of positions already filled.
	 */
	private ArrayList<Integer> usedPositionsListSendFragment = new ArrayList<Integer>();

	/**
	 * List of names on the screen.
	 */
	private ArrayList<String> namesOnScreen = new ArrayList<String>();

	/**
	 * Context for this activity.
	 */
	private Context mContext;

	/**
	 * Period to fetch nearby users.
	 */
	private static final int NEARBY_USERS_PERIOD = 5000;

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);  //No Title Bar
		setContentView(R.layout.activity_radar_view);
		mContext = this;
		mTabHost = (TabHost) findViewById(android.R.id.tabhost);
		setupTabs();
		mTabHost.setOnTabChangedListener(this);

		if (savedInstanceState != null) {
			Log.d("history", "resuming history fragment");
			Log.d("request", "resuming history fragment");
			Log.d("chat", "resuming chat fragment");
			mHistoryFragment = (HistoryFragment) getFragmentManager().findFragmentByTag(TAG_HISTORY);
			mRequestFragment = (RequestFragment) getFragmentManager().findFragmentByTag(TAG_REQUEST);
			mChatFragment = (ChatFragment) getFragmentManager().findFragmentByTag(TAG_CHAT);
		} else {
			Log.d("history", "creating history fragment");
			Log.d("request", "creating request fragment");
			Log.d("chat", "creating chat fragment");
			mHistoryFragment = new HistoryFragment();
			mRequestFragment = new RequestFragment();
			mChatFragment = new ChatFragment();
		}

		mTabHost.setCurrentTab(REQUEST_TAB);
		getFragmentManager().beginTransaction().replace(R.id.tab_request, mRequestFragment, TAG_REQUEST).commit();


		lockInfo = new HashMap<String, Boolean>();
		lockInfo.put("total", false);
		if (mAsyncTaskNearby == null) {
			mAsyncTaskNearby = new NearbyUsersAsyncTask();
			mAsyncTaskNearby.execute();
		}
	}

	@Override
	protected final void onStop() {
		try {
			Log.d("nearby", "stopping nearby task");
			if (mAsyncTaskNearby != null) {
				mAsyncTaskNearby.cancel(true);
				mAsyncTaskNearby = null;
			}
		} catch (Exception e) {

		}
	    super.onStop();
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
	 * Periodic look up for nearby users.
	 */
	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(final Message msg) {
			if (msg.what == COMPLETED) {
				try {
					Log.d("nearby", "getting nearby users");
					JSONObject auth = new JSONObject();
					try {
						auth.put("type", "basic");
						auth.put("username", Constants.userName);
						auth.put("password", Constants.password);
						Rest.get(Constants.baseURL + "/nearby/users", auth, null, getNearbyUsersResponseHandler);
					} catch (JSONException e) {

					}
				} catch (Exception e) {
					Log.e("nearby", "error with checking pending info");
					e.printStackTrace();
				}
			} else {
				Log.d("nearby", "msg is not complete");
			}
		}
	};

	/**
	 * An async task to periodically send message to start /nearby/users http call.
	 */
	private class NearbyUsersAsyncTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(final Void... params) {
			Log.d("nearby", "STARTED nearby");
			while (true) {
				try {
					Log.d("nearby", "running new nearby task");
					Message msg = new Message();
					msg.what = COMPLETED;
					mHandler.sendMessage(msg);
					Thread.sleep(NEARBY_USERS_PERIOD);
				} catch (InterruptedException e) {
					Log.d("nearby", "STOPPED nearby");			//not really an error, the stop method might interrupt it
					break;
				}
			}
			return null;
		}
	}

	/**
	 * Setup tabs of the view.
	 */
	private void setupTabs() {
		// Setup tabs
		mTabHost.setup();
		mTabHost.addTab(newTab(TAG_REQUEST, R.string.tab_request, R.id.tab_request));
		mTabHost.addTab(newTab(TAG_HISTORY, R.string.tab_history, R.id.tab_history));
		mTabHost.addTab(newTab(TAG_CHAT, R.string.tab_chat, R.id.tab_chat));
	}

	/**
	 * Create a new tab with the following specs.
	 * @param tag Tag on the tab
	 * @param labelId Label of the tab
	 * @param tabContentId ID of the tab content
	 * @return TabSpec of the generated tab
	 */
	private TabSpec newTab(final String tag, final int labelId, final int tabContentId) {
		//Log.d(TAG, "buildTab(): tag=" + tag);
		View indicator = LayoutInflater.from(this).inflate(R.layout.tab, (ViewGroup) findViewById(android.R.id.tabs), false);
		((TextView) indicator.findViewById(R.id.tab_text)).setText(labelId);

		TabSpec tabSpec = mTabHost.newTabSpec(tag);
		tabSpec.setIndicator(indicator);
		tabSpec.setContent(tabContentId);
		return tabSpec;
	}

	/**
	 * Initialize the fragments.
	 */
	private boolean mFragmentInitState[] = {true, false, false};

	/**
	 * switches UI and backend processes for selected tab.
	 * @param tabId the tab switched to
	 */
	@Override
	public final void onTabChanged(final String tabId) {
		//Log.d(TAG, "onTabChanged(): tabId=" + tabId);
		FragmentManager fm = getFragmentManager();
		if (TAG_REQUEST.equals(tabId)) {
			Log.d("tab", "changing tab to request");
			mFragmentInitState[1] = true;
			mRequestFragment.setMyName(Constants.userName);
			//start nearby task again if its dead
			if (mAsyncTaskNearby == null) {
				mAsyncTaskNearby = new NearbyUsersAsyncTask();
				mAsyncTaskNearby.execute();
			}
			fm.beginTransaction().replace(R.id.tab_request, mRequestFragment, TAG_REQUEST).commit();
		} else if (TAG_HISTORY.equals(tabId)) {
			Log.d("tab", "changing tab to history");

			if (mAsyncTaskNearby != null) {
				//only 1 async task at a time apparently,
				// kill nearby to allow the history task to open
				Log.d("nearby", "stopping nearby task");
				mAsyncTaskNearby.cancel(true);
				mAsyncTaskNearby = null;
			}
			fm.beginTransaction().replace(R.id.tab_history, mHistoryFragment, TAG_HISTORY).commit();

			//Reset selected users and amounts
			onClickClearButton(mTabHost);
		} else if (TAG_CHAT.equals(tabId)) {
			Log.d("tab", "changing tab to chat");

			if (mAsyncTaskNearby != null) {
				Log.d("nearby", "stopping nearby task");
				mAsyncTaskNearby.cancel(true);
				mAsyncTaskNearby = null;
			}

			fm.beginTransaction().replace(R.id.tab_chat, mChatFragment, TAG_CHAT).commit();
		} else {
			Log.w("RadViewAct_onTabChanged", "Cannot find tab id=" + tabId);
		}

		/*// change history tab color. Should be refactored later.
		if (tabId.equals("tab_history")) {
			mTabHost.getTabWidget().getChildAt(mTabHost.getCurrentTab()).setBackgroundColor(Color.parseColor("#2F5687"));//light navy blue
		} else {
			//TabWidget tabWidget = mTabHost.getTabWidget();
			//tabWidget.getChildAt(2).setBackgroundColor(Color.rgb(0xe6, 0xe6, 0xe6));
		}*/


	}

	@Override
	public final void onFragmentInteraction(final Uri uri) {
		Log.d(TAG, uri.toString());
	}

	@Override
	public final void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		if (requestCode == CONTACT_LIST_REQUEST_CODE) {
			if (resultCode == RESULT_OK) {
				String nameArray[];

				//Send
				nameArray = data.getStringArrayExtra("name");
				Constants.debug(nameArray);
				for (String name: nameArray) {
					boolean foundFree = false;
					for (int i = MAX_POSITIONS - 1; i >= 0; i--) {
						if (namesOnScreen.contains(name)) {
							Toast.makeText(getApplicationContext(), "User '" + name + "' is already added", Toast.LENGTH_SHORT).show();
							foundFree = true;
							break;
						}
						if (!usedPositionsListSendFragment.contains(i)) {
							Log.d("bubble", "adding user to position: " + i);
							namesOnScreen.add(name);
							mRequestFragment.addContactToView(name, i);
							usedPositionsListSendFragment.add(i);
							foundFree = true;
							break;
						} else {
							Log.d("bubble", "skipping, position taken: " + i);
						}
					}
					if (!foundFree) {
						Toast.makeText(getApplicationContext(), "Maximum users reached", Toast.LENGTH_SHORT).show();
						break;
					}
				}
			}
		} else if (requestCode == PROCEED_TO_CONFIRM_REQUEST_CODE) {
			if (resultCode == RESULT_OK) {
				paymentInfo = new ArrayList<>();
				String dataString = data.getData().toString();
				String[] paymentStrings = dataString.split("\\|");
				for (int i = 0; i < paymentStrings.length; i++) {
					String[] items = paymentStrings[i].split(",");
					paymentInfo.add(items);
				}
				mTabHost.setCurrentTab(HISTORY_TAB);
			}
		}
	}

	/**
	 * On send button click, proceed to confirmation page.
	 * @param v View of the send button
	 */
	public final void proceedToConfirm(final View v) {
		Intent i = new Intent(this, SendConfirmActivity.class);
		ArrayList<String[]> paymentInfo = mRequestFragment.getPaymentInfo();
		Constants.debug(paymentInfo);
		if (paymentInfo.size() > 1) {
			i.putExtra("transactionType", "Request");

			Bundle extras = new Bundle();
			extras.putSerializable("paymentInfo", paymentInfo);
			i.putExtras(extras);
			startActivityForResult(i, PROCEED_TO_CONFIRM_REQUEST_CODE);
		} else {
			Log.e("approveTransaction", "payment obj too small");
		}
	}

	/**
	 * On Click listener for contact button.
	 * @param v View of the contact button
	 */
	public final void contactButtonOnClick(final View v) {
		//Log.d("contactButtonOnClick", "clicked");
		startActivityForResult(new Intent(this, ContactListActivity.class), CONTACT_LIST_REQUEST_CODE);
	}

	/**
	 * On click listener for back button.
	 * @param v View of the back button
	 */
	public final void onClickBackButton(final View v) {
//		Intent i = new Intent(this, MainActivity.class);
//		startActivity(i);
		finish();
	}

	/**
	 * On click listener for the clear button.
	 * @param v View of the clear button
	 */
	public final void onClickClearButton(final View v) {
		TransactionFragment.clearUserMoneyAmount();

		//Clear total lock state
		lockInfo.put("total", false);
		findViewById(R.id.edit_text_total_amount).setEnabled(false);
	}

	@Override
	public final boolean onKeyDown(final int keyCode, final KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			onClickBackButton(getCurrentFocus());
			return true;
		}

		return super.onKeyDown(keyCode, event);
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
	 * Response handler for get nearby users http call.
	 */
	private Rest.httpResponseHandler getNearbyUsersResponseHandler = new Rest.httpResponseHandler() {
		@Override
		public void handleResponse(final HttpResponse response, final boolean error) {
			Log.d("getNearbyHandler", "Received response: " + error);
			if (!error) {
				int httpCode = response.getStatusLine().getStatusCode();

//				Intent intentApplication = new Intent(getApplicationContext(), LoginActivity.class);
				switch (httpCode) {
					case Constants.RESPONSE_404:
						Log.e("nearby", "got 404, back to login");
						showUIMessage("Cannot locate server");
//						startActivity(intentApplication);
						finish();
						break;

					case Constants.RESPONSE_401:
					case Constants.RESPONSE_403:
						Log.e("nearby", "got " + httpCode + ", unauthorized, back to login");
						showUIMessage("Lost connection to server, login again");
//						startActivity(intentApplication);
						finish();
						break;

					case Constants.RESPONSE_502:
						Log.d("nearby", "got 502, skipping");
						break;

					case Constants.RESPONSE_200:
						String responseString = "";
						try {
							responseString = EntityUtils.toString(response.getEntity());
						} catch (Exception e) {
						}

						final String responseStr = responseString;

						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								try {
									int pos = 0;
									JSONArray arr = new JSONArray(responseStr);
									for (int i = 0; i < arr.length(); i++) {
										JSONObject objUser = arr.getJSONObject(i);
										String user = objUser.getString("username");
										if (namesOnScreen.contains(user)) {
											continue;
										}
										if (!usedPositionsListSendFragment.contains(pos)) {
											namesOnScreen.add(user);
											mRequestFragment.addContactToView(user, pos);
											usedPositionsListSendFragment.add(pos);
										} else {
											i--;
										}
										pos++;
									}
								} catch (Exception e) {
									showUIMessage("Unknown problem with server...");
									Log.e("nearby", "failed to parse response =(");
									e.printStackTrace();
								}

							}
						});
						break;

					default:
						String message = "Unknown problem with server..";
						try {
							String responseStr1 = EntityUtils.toString(response.getEntity());
							JSONObject obj = new JSONObject(responseStr1);
							if (obj.has("message")) {
								message = obj.getString("message");
							}
						} catch (Exception e) {
							Log.e("nearby", "Error parsing JSON response");
						}
						Log.e("nearby", "got odd code, something is wrong, not sure what...");
						showUIMessage(message);
						break;
				}
			} else {
				showUIMessage("Error connecting to server, please login again");
				finish();
			}
		}
	};
}
