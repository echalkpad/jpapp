package com.soontobe.joinpay;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Dialog;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

/**
 * This class corresponds to the confirmation pane of a transaction (both sending and requesting money). It shows a summary of 
 * the transaction and provides buttons for users to take the next step.
 *
 */
public class SendConfirmActivity extends ListActivity {
	// for testing only
	private ArrayList<String[]> paymentInfo;
	private String[][] paymentInfoArray;

	private ArrayAdapter<String> adapter;
	private ListView lv;

	private final static String ACTIVITY_MSG_ID ="activity_confirm";
	private final static String AMOUNT_ID ="amount_confirm"; 
	private final static String PERSONAL_NOTE_ID ="personal_note_confirm";   

	private String transactionType;
	final String serviceContext = "SendConfirmActivity";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.send_confirm);
		//setConstant();
		Bundle bundle = getIntent().getExtras();
		paymentInfo = (ArrayList<String[]>) bundle.get("paymentInfo");
		transactionType = getIntent().getExtras().getString("transactionType");
		setConfirmButtonText();
		updatePaneTitle();
		setListView();
		setEventListeners();
		IntentFilter restIntentFilter = new IntentFilter(Constants.RESTRESP);
		registerReceiver(restResponseReceiver, restIntentFilter);

	}

	private void setConfirmButtonText() {
		Button confirmButton = (Button) findViewById(R.id.transaction_confirm_button);
		confirmButton.setText(transactionType);
	}

	private void updatePaneTitle() {
		TextView tv = (TextView) findViewById(R.id.title_transaction_confirm);
		tv.setText(transactionType);
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


	private void setListView() {
		ListView list = getListView();
		boolean isPending = false;	//	Don't care
		boolean isHistory = false;
		
		ArrayList<JSONObject> obj = new ArrayList<JSONObject>();
		for (String[] sa : paymentInfo){										//iter over each payment detail, build JSON
			try {
				JSONObject tmp = new JSONObject();								//reset
				for(int i=0; i < sa.length; i++){
					if(i == 0) {
						if(sa[i].equals("normal")) tmp.put("type", sa[i]);
						else break;												//if its not normal, skip 
					}
					if(i == 1) tmp.put("description", sa[i]);
					if(i == 2) tmp.put("from", sa[i]);
					if(i == 3) tmp.put("to", sa[i]);
					if(i == 4) tmp.put("amount", sa[i]);
					if(i == 5){
						tmp.put("type", sa[i]);
						obj.add(tmp);											//all good, add it
					}
				}
			}
			catch (JSONException e) {
				e.printStackTrace();
			}
			
		}
		Log.d("payment", "jsonarray: " + obj);
		
		PaymentSummaryAdapter adapter = new PaymentSummaryAdapter(this, obj, isHistory);
		list.setAdapter(adapter);
	}

	private void setConstant() {
		String[][] tmp = 
			{
				{"normal", "", "Luna", "Itziar", "$ 500"},
				{"normal", "Pay one extra beer", "Patrick", "Itziar", "$ 30"},   //	name, amount, personal note
				{"normal", "", "asd", "Itziar", "$ 20"},
				{"normal", "", "Itziar", "Itziar", "$ 20"},
				{"group_note", "This is a group note"},
				{"summary", "2014-11-14", "5", "$ 130"}
			};
		paymentInfo = new ArrayList<String[]>();
		for (int i = 0;i < tmp.length;i++) {
			paymentInfo.add(tmp[i]);
		}
		paymentInfoArray = tmp;
	}

	public void backToSendInfo(View v) {
		finish();
	}

	public void proceedToConfirmSend(View v) {
		String retData = "";
		ArrayList<String> users = new ArrayList<String>();
		ArrayList<String> amount = new ArrayList<String>();
		JSONObject objTransaction = new JSONObject();
		Integer targetIndex = 0;
		Boolean check = false;
		
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
			objTransaction.put("total", paymentInfo.get(paymentInfo.size() - 1)[3]);
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
		
		/*
		final String paymentInfoString = retData;
		new Thread() {
			@Override
			public void run() {
				WebConnector webConnector = new WebConnector(Constants.userName);
				webConnector.postTransactionRecord(Constants.urlForPostingToFolder, paymentInfoString);
			}
		}.start();
		Intent data = new Intent();
		data.setData(Uri.parse(retData));
		setResult(RESULT_OK, data);
		finish();
		*/
	}

	BroadcastReceiver restResponseReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String receivedServiceContext = intent.getStringExtra("context");
			
			if(serviceContext.equals(receivedServiceContext)) {
				String url = intent.getStringExtra("url");
				String method = intent.getStringExtra("method");
				String response = intent.getStringExtra("response");
				Log.d("debug", "rest response: " + response);
				
				if(response.contains("OK")) {
					Log.d("restResponse", "Received OK, returning");
					Intent data = new Intent();
					data.setData(Uri.parse(response));
					SendConfirmActivity.this.setResult(RESULT_OK, data);
					
					unregisterReceiver(restResponseReceiver);
					finish();
				}
			}			
		}
	};
}