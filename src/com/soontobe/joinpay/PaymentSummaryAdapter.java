package com.soontobe.joinpay;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

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
		Log.d("getView", obj.toString());
//		personalNoteView.setText(obj.getString());

		try {
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
			payerView.setText(from);
			payeeView.setText(to);
			Log.d("getView", "Past payer, payee");
			amountView.setText(obj.getString("amount"));

			TableLayout tr = (TableLayout) rowView;
			boolean hasPersonalNote = false;
			boolean isPending = !obj.getBoolean("authorized");
	
			if (isPending) {
				TextView tv = (TextView) tr.findViewById(R.id.payment_status);
				tv.setText("Pending");
			}
			
			tr.requestLayout();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Log.d("getView", "returning rowView");
		return rowView;

	}

} 
