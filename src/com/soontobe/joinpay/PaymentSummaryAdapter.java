package com.soontobe.joinpay;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * This class is an adapter for the layout of the transaction summary view or history view.
 *
 */
public class PaymentSummaryAdapter extends ArrayAdapter<JSONObject> {
	private final Context context;

	private boolean isHistory;

	/*
	 * [][0]: type
	 * 
	 *  type: normal/normal_pn
	 *  	[1] personal note
	 *  	[2] payer
	 *  	[3] payee
	 *  	[4] amount
	 *  
	 *  type: summary
	 *  	[1] date (and maybe time)
	 *  	[2] # of ppl
	 *  	[3] total amount
	 *  
	 *  type: group_note
	 */
//	private final ArrayList<String[]> values;
	private final ArrayList<JSONObject> values;

	/*
	 * isHistory: true if this list view is shown in History, false if in transaction confirmation view
	 */
/*	public PaymentSummaryAdapter(Context context, List<String[]> values, boolean isHistory) {
		super(context, R.layout.confirm_page_item, values);
		this.context = context;
		this.values = (ArrayList<String[]>) values;
		this.isHistory = isHistory;
	} 
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView;
		if (values.get(position)[0].equals("normal")) {
			rowView = inflater.inflate(R.layout.confirm_page_item, parent, false);
			TextView personalNoteView = (TextView) rowView.findViewById(R.id.confirm_personal_note_normal);
			TextView payerView = (TextView) rowView.findViewById(R.id.activity_confirm_payer);
			TextView payeeView = (TextView) rowView.findViewById(R.id.activity_confirm_payee);
			TextView amountView = (TextView) rowView.findViewById(R.id.amount_confirm);
			personalNoteView.setText(values.get(position)[1]);
			payerView.setText(values.get(position)[2]);
			payeeView.setText(values.get(position)[3]);
			amountView.setText(values.get(position)[4]);

			TableLayout tr = (TableLayout) rowView;
			boolean hasPersonalNote = false;
			boolean isPending = false;
			String pendingString = values.get(position)[5];
			
			// Initiator pays himself/herself
			if (values.get(position)[2].equals(values.get(position)[3])) {
				payerView.setText("You paid");
				payerView.setTextColor(Color.rgb(0xb3, 0xb3, 0xb3));
				amountView.setTextColor(Color.rgb(0xb3, 0xb3, 0xb3));
				LinearLayout item = (LinearLayout) rowView.findViewById(R.id.activity_confirm_pay_item_layout);
				item.removeView(item.findViewById(R.id.activity_confirm_pay_text));
				item.removeView(payeeView);
				item.requestLayout();
			}

			if (values.get(position)[1].length() > 0) {	//	without personal note
				hasPersonalNote = true;
			}
			
			if (pendingString.equals("isPending") && isHistory) {
				TextView tv = (TextView) tr.findViewById(R.id.payment_status);
				tv.setText("Pending");
				isPending = true;
			}

			if (hasPersonalNote == false && isPending == false) {
				tr.removeView(tr.findViewById(R.id.confirm_item_second_row));
			}
			tr.requestLayout();

		} else if (values.get(position)[0].equals("summary")) {
			rowView = inflater.inflate(R.layout.confirm_page_total, parent, false);
			TextView dateView = (TextView) rowView.findViewById(R.id.activity_confirm_date);
			TextView numOfPeopleView = (TextView) rowView.findViewById(R.id.activity_confirm_num_ppl);
			TextView totalAmountView = (TextView) rowView.findViewById(R.id.activity_confirm_total);
			dateView.setText(values.get(position)[1]);
			numOfPeopleView.setText(values.get(position)[2]);
			totalAmountView.setText(values.get(position)[3]);
		} else if (values.get(position)[0].equals("group_note")) {
			rowView = inflater.inflate(R.layout.confirm_page_group_note, parent, false);
			TextView groupNoteView = (TextView) rowView.findViewById(R.id.confirm_group_note);
			groupNoteView.setText(values.get(position)[1]);
		} else {
			Log.e("ConfirmPageArrayAdapter", "Wrong value type");
			rowView = inflater.inflate(R.layout.confirm_page_item, parent, false);
		}

		return rowView;
	}
	*/
	
	public PaymentSummaryAdapter(Context context, List<JSONObject> values, boolean isHistory) {
		super(context, R.layout.confirm_page_item, values);
		this.context = context;
		this.values = (ArrayList<JSONObject>) values;
		this.isHistory = isHistory;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView;
		
		JSONObject obj = values.get(position);
		
		if(convertView == null) {
			rowView = inflater.inflate(R.layout.confirm_page_item, parent, false);
		} else {
			rowView = convertView;
		}
		TextView personalNoteView = (TextView) rowView.findViewById(R.id.confirm_personal_note_normal);
		TextView payerView = (TextView) rowView.findViewById(R.id.activity_confirm_payer);
		TextView payeeView = (TextView) rowView.findViewById(R.id.activity_confirm_payee);
		TextView amountView = (TextView) rowView.findViewById(R.id.amount_confirm);
		TextView transcation = (TextView) rowView.findViewById(R.id.activity_confirm_transacation);
		Log.d("getView", obj.toString());
//		personalNoteView.setText(obj.getString());

//		try {
			String to = Constants.userName;
			String from = Constants.userName;
			
			try {
				to = obj.getString("to");
			} catch(Exception e) {			
				to = Constants.userName;
			}
			try {
				from = obj.getString("from");
			} catch(Exception e) {			
				from = Constants.userName;
			}
			try {
				transcation.setText(obj.getString("id"));
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
			payerView.setText(from);
			payeeView.setText(to);
			Log.d("getView", "Past payer, payee");
			try {
				amountView.setText(obj.getString("amount"));
			} catch(Exception e) {
				e.printStackTrace();
			}
			TableLayout tr = (TableLayout) rowView;
			boolean hasPersonalNote = false;
			boolean isPending = false;
			try{
				isPending = !obj.getBoolean("authorized");
			} catch(Exception e) {
				e.printStackTrace();
			}
	
			if (isPending) {
				TextView tv = (TextView) tr.findViewById(R.id.payment_status);
				tv.setText("Pending");
			}
			
			tr.requestLayout();
			
			
			rowView.setOnClickListener(new View.OnClickListener() {
				
				
				@Override
				public void onClick(View v) {
					Log.d("dsh", "clicked");
					TextView transId = (TextView)  v.findViewById(R.id.activity_confirm_transacation);
					TextView payeeView = (TextView)  v.findViewById(R.id.activity_confirm_payee);
					Log.d("dsh", "test " + transId.getText());
					
					///////////////// Open Approve / Deny Dialog /////////////////
					try{
						final Dialog dialog = new Dialog(context);
						dialog.setContentView(R.layout.dialog);
						dialog.setTitle("Approve pending transacation?");
						TextView text = (TextView) dialog.findViewById(R.id.text);
						text.setText("Sending money to " + payeeView.getText());
						TextView hidden = (TextView) dialog.findViewById(R.id.hidden);
						hidden.setText(transId.getText());
						Log.d("dsh", "dialog now has trans: " + transId.getText());
						
						TextView dialogButtonPOS = (TextView) dialog.findViewById(R.id.dialogButtonPOS);
						dialogButtonPOS.setText("Approve");
						dialogButtonPOS.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {
								TextView transId = (TextView)  dialog.findViewById(R.id.hidden);
								Log.d("dsh", "user approved: " + transId.getText());
								dialog.dismiss();
								transAction(true, (String) transId.getText());
							}
						});
						
						TextView dialogButtonNEG = (TextView) dialog.findViewById(R.id.dialogButtonNEG);
						dialogButtonNEG.setVisibility(1);
						dialogButtonNEG.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {
								TextView transId = (TextView)  dialog.findViewById(R.id.hidden);
								Log.d("dsh", "user denied: " + transId.getText());
								dialog.dismiss();
								transAction(false, (String) transId.getText());
							}
						});
						dialog.show();
					}
					catch(Exception e){
						Log.e("dsh","dialog error");
						e.printStackTrace();
					}
				}
				
			});
			
			
			
			
			
			
//		} catch (JSONException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		Log.d("getView", "returning rowView");
		return rowView;

	}
	
	///////////////// Confirm/Deny the Push Msg /////////////////
	public void transAction(boolean action, String id){
		final String serviceContext = "transAction";
/*		BroadcastReceiver restResponseReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String receivedServiceContext = intent.getStringExtra("context");
				
				if(serviceContext.equals(receivedServiceContext)) {
					String url = intent.getStringExtra("url");
					String method = intent.getStringExtra("method");
					String response = intent.getStringExtra("response");
					Log.d("dsh", "REST Response for push " + response);
				}
			}
		};						
	*/	
		if(action){															//only authorize if true...  later we will revisit this and send different REST call
			Intent intent = new Intent(context, RESTCalls.class);
			JSONObject obj = new JSONObject();
			try {
				obj.put("username", Constants.userName);
			} catch (JSONException e) {
				Toast.makeText(context, "Error creating JSON", Toast.LENGTH_SHORT).show();
			}
	
			String url = Constants.baseURL + "/authorize/" + id;
			//String url = Constants.baseURL + "";
			intent.putExtra("method","put");
			intent.putExtra("url",url);
			intent.putExtra("body", obj.toString());
			intent.putExtra("context", serviceContext);
			Log.d("dsh", "starting rest service for push: " + action);
			getContext().startService(intent);
		}
	}

} 
