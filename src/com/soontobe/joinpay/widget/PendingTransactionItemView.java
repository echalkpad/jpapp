package com.soontobe.joinpay.widget;

import java.util.ArrayList;

import com.soontobe.joinpay.R;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Pending transaction item
 * This object will be shown in the transaction list for its pending state.
 * Buttons for either `Accept` or `Decline` of the transaction is set
 * 	with customized OnClickListener for further implementation.
 * 
 */
public class PendingTransactionItemView extends LinearLayout {
	private int transactionId;
	
	private TextView mTextLeftName;		//Payer
	private TextView mTextPayType;
	private TextView mTextRightName;	//Payee
	private TextView mTextNote;			//Group note and private note
	private TextView mTextMoneyAmount;
	private TextView mTextDate;
	private TextView mTextTime;
	private Button mButtonAccept;
	private Button mButtonDecline;
	private LinearLayout mCurrentLayout;
	
	OnAcceptButtonClickListener acceptButtonClickListener = null;
	OnDeclineButtonClickListener declineButtonClickListener = null;
	

	public void setAcceptButtonClickListener(
			OnAcceptButtonClickListener acceptButtonClickListener) {
		this.acceptButtonClickListener = acceptButtonClickListener;
	}

	public void setDeclineButtonClickListener(
			OnDeclineButtonClickListener declineButtonClickListener) {
		this.declineButtonClickListener = declineButtonClickListener;
	}

	public PendingTransactionItemView(Context context) {
		super(context);
		LayoutInflater.from(context).inflate(R.layout.pending_payment_item, this);
		init();
	}
	
	public PendingTransactionItemView(Context context, AttributeSet attrs){
		super(context, attrs);
		LayoutInflater.from(context).inflate(R.layout.pending_payment_item, this);
		init();
	}
	
	private void init(){
		mTextDate = (TextView)findViewById(R.id.textview_pending_date);
		mTextPayType = (TextView)findViewById(R.id.textview_paytype);
		mTextLeftName = (TextView)findViewById(R.id.textview_left_name);
		mTextRightName= (TextView)findViewById(R.id.textview_right_name);
		mTextNote = (TextView)findViewById(R.id.textview_payment_note);
		mTextMoneyAmount = (TextView)findViewById(R.id.textview_money_amount);
		mTextTime = (TextView)findViewById(R.id.textview_pending_time);
		mButtonAccept = (Button)findViewById(R.id.button_accept_transaction);
		mButtonAccept.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(acceptButtonClickListener != null)
					acceptButtonClickListener.OnClick(v);
			}
		});
		mButtonDecline = (Button)findViewById(R.id.button_decline_transaction);
		mButtonDecline.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(declineButtonClickListener != null)
					declineButtonClickListener.OnClick(v);
			}
		});
		//mCurrentLayout = (LinearLayout)findViewById(R.id.layout_pending_item);
		this.setBackgroundResource(R.color.color_pending_item);
	}
	
	public void setAccepted(){
		mButtonAccept.setVisibility(View.INVISIBLE);
		mButtonDecline.setVisibility(View.INVISIBLE);
		mTextPayType.setText("paid");
		mTextPayType.setTextColor(Color.parseColor("#88bfa3"));
		this.setBackgroundResource(R.color.color_pending_item_done);
	}
	
	public void setDeclined(){
		mButtonAccept.setVisibility(View.INVISIBLE);
		mButtonDecline.setVisibility(View.INVISIBLE);
		mTextPayType.setText("refused to pay");
		this.setBackgroundResource(R.color.color_pending_item_done);
	}
	
	public void setPaymentInfo(ArrayList<String[]> info){
		String[] groupNotes = info.get(1);
		String groupNote;
		if(null == groupNotes ||
				groupNotes.length <= 0)
			groupNote = "";
		else
			groupNote = groupNotes[1];
		
		String[] payinfo = info.get(0);
		String payer = payinfo[2];
		String payee = payinfo[3];
		String amount = payinfo[4];
		boolean isPending = (payinfo[5].equals("isPending"));
		String personalNote = payinfo[1];
		if(personalNote == null)
			personalNote = "";
		
		String[] summary = info.get(2);
		String datetime = summary[1];
		
		mTextLeftName.setText(payer);
		mTextRightName.setText(payee);
		if(isPending){
			mTextPayType.setText("will pay");
			mTextLeftName.setTypeface(null, Typeface.BOLD);
			this.setBackgroundResource(R.color.color_pending_item);
			mButtonAccept.setVisibility(View.VISIBLE);
			mButtonDecline.setVisibility(View.VISIBLE);
		} else {
			mTextPayType.setText("paid");
			mTextPayType.setTextColor(Color.parseColor("#88bfa3"));
			mTextRightName.setTypeface(null, Typeface.BOLD);
			this.setBackgroundResource(R.color.color_pending_item_done);
			mButtonAccept.setVisibility(View.INVISIBLE);
			mButtonDecline.setVisibility(View.INVISIBLE);
		}
			
		String note = groupNote;
		if(personalNote != null &&
				!personalNote.isEmpty())
			note += " & " + personalNote;
		mTextNote.setText(note);
		mTextMoneyAmount.setText(amount);
		mTextDate.setText(datetime);
		//mTextTime.setText(datetime.split(" ")[1]);
		
	}
	
	public interface OnAcceptButtonClickListener{
		public void OnClick(View v);
	}
	
	public interface OnDeclineButtonClickListener{
		public void OnClick(View v);
	}

	public int getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(int transactionId) {
		this.transactionId = transactionId;
	}
	
	
}
