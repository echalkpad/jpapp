package com.soontobe.joinpay.activities;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.soontobe.joinpay.Constants;
import com.soontobe.joinpay.R;
import com.soontobe.joinpay.adapters.PaymentSummaryAdapter;
import com.soontobe.joinpay.helpers.Rest;
import com.soontobe.joinpay.model.Transaction;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class corresponds to the confirmation pane of a transaction (both sending and requesting money). It shows a summary of
 * the transaction and provides buttons for users to take the next step.
 *
 */
public class SendConfirmActivity extends ListActivity {

	/**
	 * Array of payments.
	 */
	private ArrayList<String[]> paymentInfo;
	/**
	 * Transaction object.
	 */
	private JSONObject objTransaction;
	/**
	 * Context for this activity.
	 */
	private Context mContext;

	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		requestWindowFeature(Window.FEATURE_NO_TITLE);  					//No Title Bar
		setContentView(R.layout.send_confirm);
		Bundle bundle = getIntent().getExtras();
		paymentInfo = (ArrayList<String[]>) bundle.get("paymentInfo");
		setListView();
		setEventListeners();
		findViewById(R.id.transaction_confirm_button).setEnabled(true);
	}

	/**
	 * Setup event listeners for touch events of transaction confirm button & edit pencil.
	 */
	private void setEventListeners() {
		Button transactionConfirmButton = (Button) findViewById(R.id.transaction_confirm_button);
		OnTouchListener buttonOnTouchListener = new OnTouchListener() {
			@Override
			public boolean onTouch(final View v, final MotionEvent event) {
				Button btn = (Button) v;
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					btn.setBackgroundResource(R.drawable.button_active);
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					btn.setBackgroundResource(R.drawable.button_normal);
				}
				return false;
			}
		};
		transactionConfirmButton.setOnTouchListener(buttonOnTouchListener);

		Button sendEditPencil = (Button) findViewById(R.id.send_edit_pencil);
		buttonOnTouchListener = new OnTouchListener() {
			@Override
			public boolean onTouch(final View v, final MotionEvent event) {
				Button btn = (Button) v;
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					btn.setBackgroundResource(R.drawable.pencil_grey);
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					btn.setBackgroundResource(R.drawable.pencil_white);
				}
				return false;
			}
		};
		sendEditPencil.setOnTouchListener(buttonOnTouchListener);
	}

	/**
	 * Set the list view of transactions to confirm.
	 */
	private void setListView() {
		ListView list = getListView();
		String groupNote = "";
		ArrayList<Transaction> transList = new ArrayList<>();

		//iter over each payment detail, build JSON
		for (String[] sa : paymentInfo) {
			try {
				//reset
				JSONObject tmp = new JSONObject();
				for (int i = 0; i < sa.length; i++) {
					if (i == 0) {
						if (sa[i].equals("normal")) {
							tmp.put("type", sa[i]);
						} else if (sa[i].equals("group_note")) {
							Log.d("payment", "got a group note, adding it in");
							groupNote = sa[i + 1];
							break;
						} else {
							//if its not normal, skip
							break;
						}
					}
					if (i == 1) {
						tmp.put("description", sa[i]);
					}
					if (i == 2) {
						tmp.put("fromUser", sa[i]);
					}
					if (i == 3) {
						tmp.put("toUser", sa[i]);
					}
					if (i == 4) {
						tmp.put("amount", sa[i]);
					}
					if (i == 5) {
						tmp.put("type", sa[i]);									//take over the type field...
						tmp.put("description", groupNote);

						transList.add(new Transaction(tmp));											//all good, add it
					}
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		if (transList.size() > 0) {
            Object[] arr = transList.toArray();
            Arrays.sort(arr, Transaction.dateComparator(false));
            Log.d("payment", "transactions: " + transList);
            List sortedList = Arrays.asList(arr);
			PaymentSummaryAdapter adapter = new PaymentSummaryAdapter(this, sortedList, this.getLayoutInflater(), null);
			list.setAdapter(adapter);
			adapter.notifyDataSetChanged();
		} else {
			Log.d("payment", "transaction list is size 0");
		}
	}

	/**
	 * Callback to go back to the Send Info View.
	 * @param v View of the button
	 */
	public final void backToSendInfo(final View v) {
		finish();
	}

	/**
	 * Callback to proceed to confirmation page on send button click.
	 * @param v View of the button
	 */
	public final void proceedToConfirmSend(final View v) {
		ArrayList<String> users = new ArrayList<>();
		ArrayList<String> amount = new ArrayList<>();
		String groupNote = "";
		objTransaction = new JSONObject();
		Integer targetIndex = 0;
		Boolean check = false;
		findViewById(R.id.transaction_confirm_button).setEnabled(false);
		Log.d("paymentBuilder", "procced to confirm send fired");
		Constants.debug(paymentInfo);
		for (int i = 0; i < paymentInfo.size() - 1; i++) {
			check = false;
			if (paymentInfo.get(i)[0].equals("normal")) {
				if (paymentInfo.get(i)[6].equals("requesting")) {
					Log.d("paymentBuilder", "requesting money");
					//if requesting look at position 2
					targetIndex = 2;
					//only check if requesting money, ie don't request money from myself
					check = true;
				} else {
					Log.d("paymentBuilder", "sending money");
					//if sending look at position 3
					targetIndex = 3;
					//WE DON'T SUPPORT SENDING MONEY YET, just die
					finish();
				}

				if (check && paymentInfo.get(i)[targetIndex].equals(Constants.userName)) {
					Log.d("paymentBuilder", "skipping over self");
				} else {
					Log.d("paymentBuilder", "adding user to users: " + paymentInfo.get(i)[targetIndex]);
					users.add(paymentInfo.get(i)[targetIndex]);
					amount.add(paymentInfo.get(i)[4]);
				}
			} else if (paymentInfo.get(i)[0].equals("group_note")) {
				Log.d("paymentBuilder", "got a group note, adding it in" + paymentInfo.get(i)[1]);
				groupNote = paymentInfo.get(i)[1];
			}
		}

		Constants.debug(users);
		JSONArray arr = new JSONArray();
		for (int i = 0; i < users.size(); i++) {
			JSONObject obj = new JSONObject();
			try {
				obj.put("username", users.get(i));
				obj.put("amount", amount.get(i));
				arr.put(obj);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		try {
			objTransaction.put("charges", arr);
			objTransaction.put("description", groupNote);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Log.d("paymentBuilder", objTransaction.toString());

		JSONObject auth = new JSONObject();
		JSONObject headers = new JSONObject();
		try {
			auth.put("type", "basic");
			auth.put("username", Constants.userName);
			auth.put("password", Constants.password);
			headers.put("Content-Type", "application/json");
			Rest.post(Constants.baseURL + "/charge", auth, headers, objTransaction.toString(), chargeResponseHandler);
		} catch (JSONException e) {

		}

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
	 * Response handler for charge http call.
	 */
	private Rest.httpResponseHandler chargeResponseHandler = new Rest.httpResponseHandler() {
		@Override
		public void handleResponse(final HttpResponse response, final boolean error) {
			if (!error) {
				int responseCode = response.getStatusLine().getStatusCode();
				String responseStr = "";
				try {
					responseStr = EntityUtils.toString(response.getEntity());
				} catch (IOException e) {
					e.printStackTrace();
				}
				switch (responseCode) {
					case Constants.RESPONSE_200:                                                //200 = return the UI control
						Log.d("confirm", "Received 200, returning");
						Intent data = new Intent();
						data.setData(Uri.parse(responseStr));
						SendConfirmActivity.this.setResult(RESULT_OK, data);
						finish();
						break;

					case Constants.RESPONSE_404:
					case Constants.RESPONSE_401:
						Log.d("confirm", "Received 404 or 401, closing activity");
						Intent data1 = new Intent();
						data1.setData(Uri.parse(responseStr));
						SendConfirmActivity.this.setResult(RESULT_OK, data1);
						finish();
						break;

					case Constants.RESPONSE_502: // Retry
						Log.d("confirm", "Received 502, trying again");
						Log.d("confirm", objTransaction.toString());
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								findViewById(R.id.transaction_confirm_button).setEnabled(false);
							}
						});

						JSONObject auth = new JSONObject();
						JSONObject headers = new JSONObject();
						try {
							auth.put("type", "basic");
							auth.put("username", Constants.userName);
							auth.put("password", Constants.password);
							headers.put("Content-Type", "application/json");
							Rest.post(Constants.baseURL + "/charge", auth, headers, objTransaction.toString(), chargeResponseHandler);
						} catch (JSONException e) {

						}
						break;

					default: // Let user try again
						String message = "Unknown issue with server, try again later";
						try {
							JSONObject obj = new JSONObject(responseStr);
							if (obj.has("message")) {
								message = obj.getString("message");
							}
						} catch (JSONException e) {
							Log.e("confirm", "Error parsing JSON response");
						}
						Log.e("confirm", "error with api response: " + responseCode + " " + responseStr);
						showUIMessage(message);
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								findViewById(R.id.transaction_confirm_button).setEnabled(true);
							}
						});
						break;
				}
			} else {
				showUIMessage("Error connecting to server, please try again.");
			}
		}
	};

}