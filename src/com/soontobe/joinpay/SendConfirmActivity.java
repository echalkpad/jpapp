package com.soontobe.joinpay;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
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
				// TODO Auto-generated method stub
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
				// TODO Auto-generated method stub
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
//		PaymentSummaryAdapter adapter = new PaymentSummaryAdapter(this, paymentInfo, isHistory);
//		list.setAdapter(adapter);
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
		
		Log.d("Payment Info", "" + paymentInfo.toString());
		for (int i = 0;i < paymentInfo.size() - 1;i++) {
/*			if (i != 0) retData += "|";
			for (int j = 0;j < paymentInfo.get(i).length;j++) {
				if (j != 0) retData += ",";
				retData += paymentInfo.get(i)[j];
			}
*/
			if(paymentInfo.get(i)[3].equals(Constants.userName)) {
			
			} else {
				users.add(paymentInfo.get(i)[3]);
				amount.add(paymentInfo.get(i)[4]);
			}
		}
		
		JSONArray arr = new JSONArray();
		for(int i = 0; i < users.size(); i++) {
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
				
				if(response.contains("OK")) {
					Log.d("restResponse", "Received OK, returning");
					Intent data = new Intent();
					data.setData(Uri.parse(response));
					SendConfirmActivity.this.setResult(RESULT_OK, data);
					finish();
				}
			}			
		}
	};
}