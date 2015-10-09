package com.soontobe.joinpay;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class corresponds to the confirmation pane of a transaction (both sending and requesting money). It shows a summary of 
 * the transaction and provides buttons for users to take the next step.
 *
 */
public class SendConfirmActivity extends ListActivity {
	// for testing only
	private ArrayList<String[]> paymentInfo;
	private ArrayAdapter<String> adapter;
	final String serviceContext = "SendConfirmActivity";
	private JSONObject objTransaction;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);  					//No Title Bar
		setContentView(R.layout.send_confirm);
		Bundle bundle = getIntent().getExtras();
		paymentInfo = (ArrayList<String[]>) bundle.get("paymentInfo");
		setListView();
		setEventListeners();
		findViewById(R.id.transaction_confirm_button).setEnabled(true);
		IntentFilter restIntentFilter = new IntentFilter(Constants.RESTRESP);
		registerReceiver(restResponseReceiver, restIntentFilter);

	}

	private void setEventListeners() {
		Button transactionConfirmButton = (Button) findViewById(R.id.transaction_confirm_button);
		OnTouchListener buttonOnTouchListener = new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
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
			public boolean onTouch(View v, MotionEvent event) {
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

	@Override
	protected void onStop(){
		try{
			unregisterReceiver(restResponseReceiver);		//remove the receiver
		}
		catch(Exception e){}
	    super.onStop();
	}

	private void setListView() {
		ListView list = getListView();
		boolean isHistory = false;
		String groupNote = "";
		ArrayList<Transaction> transList = new ArrayList<>();
		
		for (String[] sa : paymentInfo){										//iter over each payment detail, build JSON
			try {
				JSONObject tmp = new JSONObject();								//reset
				for(int i=0; i < sa.length; i++){
					if(i == 0) {
						if(sa[i].equals("normal")) tmp.put("type", sa[i]);
						else if(sa[i].equals("group_note")){
							Log.d("payment", "got a group note, adding it in");
							groupNote = sa[i+1];
							break;
						}
						else break;												//if its not normal, skip 
					}
					if(i == 1) tmp.put("description", sa[i]);
					if(i == 2) tmp.put("fromUser", sa[i]);
					if(i == 3) tmp.put("toUser", sa[i]);
					if(i == 4) tmp.put("amount", sa[i]);
					if(i == 5){
						tmp.put("type", sa[i]);									//take over the type field...
						tmp.put("description", groupNote);

						transList.add(new Transaction(tmp));											//all good, add it
					}
				}
			}
			catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		if(transList.size() > 0){
            Object[] arr = transList.toArray();
            Arrays.sort(arr, Transaction.dateComparator(false));
            Log.d("payment", "transactions: " + transList);
            List sortedList = Arrays.asList();
			PaymentSummaryAdapter adapter = new PaymentSummaryAdapter(this, sortedList, this.getLayoutInflater());
			list.setAdapter(adapter);
		}
		else Log.d("payment", "transaction list is size 0");
	}

	public void backToSendInfo(View v) {
		finish();
	}

	public void proceedToConfirmSend(View v) {
		ArrayList<String> users = new ArrayList<String>();
		ArrayList<String> amount = new ArrayList<String>();
		String groupNote = "";
		objTransaction = new JSONObject();
		Integer targetIndex = 0;
		Boolean check = false;
		findViewById(R.id.transaction_confirm_button).setEnabled(false);
		Log.d("paymentBuilder", "procced to confirm send fired");
		Constants.debug(paymentInfo);
		for(int i=0; i < paymentInfo.size() - 1; i++) {
			check = false;
			if(paymentInfo.get(i)[0].equals("normal")){
				if(paymentInfo.get(i)[6].equals("requesting")){
					Log.d("paymentBuilder", "requesting money");
					targetIndex = 2;								//if requesting look at position 2
					check = true;									//only check if requesting money, ie don't request money from myself
				}
				else{
					Log.d("paymentBuilder", "sending money");
					targetIndex = 3;								//if sending look at position 3
					finish();										//WE DON'T SUPPORT SENDING MONEY YET, just die
				}
				
				if(check && paymentInfo.get(i)[targetIndex].equals(Constants.userName)) {
					Log.d("paymentBuilder", "skipping over self");
				} else {
					Log.d("paymentBuilder", "adding user to users: " + paymentInfo.get(i)[targetIndex]);
					users.add(paymentInfo.get(i)[targetIndex]);
					amount.add(paymentInfo.get(i)[4]);
				}
			}
			else if(paymentInfo.get(i)[0].equals("group_note")){
				Log.d("paymentBuilder", "got a group note, adding it in" + paymentInfo.get(i)[1]);
				groupNote = paymentInfo.get(i)[1];
			}
		}
		
		Constants.debug(users);
		JSONArray arr = new JSONArray();
		for(int i=0; i < users.size(); i++) {
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
		Intent intent = new Intent(getApplicationContext(), RESTCalls.class);
		String url = Constants.baseURL + "/charge";
		intent.putExtra("method","post");
		intent.putExtra("url",url);
		intent.putExtra("body", objTransaction.toString());
		intent.putExtra("context", serviceContext);
		startService(intent);
	}

	BroadcastReceiver restResponseReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String receivedServiceContext = intent.getStringExtra("context");
			
			if(serviceContext.equals(receivedServiceContext)) {
				//String url = intent.getStringExtra("url");
				//String method = intent.getStringExtra("method");
				String response = intent.getStringExtra("response");
				int httpCode = intent.getIntExtra("code", 0);
				Log.d("confirm", "response: " + response);
				//findViewById(R.id.transaction_confirm_button).setEnabled(true);

				
				if(httpCode == 200) {												//200 = return the UI control
					Log.d("confirm", "Received 200, returning");
					Intent data = new Intent();
					data.setData(Uri.parse(response));
					SendConfirmActivity.this.setResult(RESULT_OK, data);
					
					unregisterReceiver(restResponseReceiver);
					finish();
				}
				else if(httpCode == 404 || httpCode == 401) {						//404 = api went down, will need to relogin, should just return UI control, let others move me to login
					Log.d("confirm", "Received 404 or 401, closing activity");
					Intent data = new Intent();
					data.setData(Uri.parse(response));
					SendConfirmActivity.this.setResult(RESULT_OK, data);
					
					unregisterReceiver(restResponseReceiver);
					finish();
				}
				else if(httpCode == 502){											//502 = try again
					Log.d("confirm", "Received 502, trying again");
					Log.d("confirm", objTransaction.toString());
					findViewById(R.id.transaction_confirm_button).setEnabled(false);
					Intent intent2 = new Intent(getApplicationContext(), RESTCalls.class);
					String url = Constants.baseURL + "/charge";
					intent2.putExtra("method","post");
					intent2.putExtra("url",url);
					intent2.putExtra("body", objTransaction.toString());
					intent2.putExtra("context", serviceContext);
					startService(intent2);
				}
				else{																//500 = let user try again...
					String message = "Unknown issue with server, try again later";
					try {
						JSONObject obj = new JSONObject(response);
						if(obj.has("message")) message = obj.getString("message");
					}
					catch (JSONException e) {
						Log.e("confirm", "Error parsing JSON response");
					}
					Log.e("confirm", "error with api response");
					Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
					findViewById(R.id.transaction_confirm_button).setEnabled(true);
				}
			}			
		}
	};
}