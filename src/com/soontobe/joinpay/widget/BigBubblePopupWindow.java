package com.soontobe.joinpay.widget;

import java.util.ArrayList;

import com.echo.holographlibrary.PieGraph;
import com.echo.holographlibrary.PieSlice;
import com.soontobe.joinpay.R;
import com.soontobe.joinpay.model.UserInfo;

import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.TextView;


/**
 * Customized PopupWindow 
 * This popupwindow will show when editing the transaction details with someone
 *
 */
public class BigBubblePopupWindow extends PopupWindow {
	final public int LAYOUT_ID = R.id.layout_big_bubble;
	final public String TAG = "BIG_BUBBLE";
	final public String[] COLOR_MAP = {"#99CC00", "#FFBB33", "#AA66CC", "#0000AA"}; //TODO: ...
	private UserInfo mUserInfo;
	private PieGraph mPieGraph;	  				//Outer torus which will be further developed to show the ratio of different types of transaction
	private Button mLockButton;   				//Lock button
	private EditText mEditText;   				//Amount of money
	private EditText mEditPersonalNote;  		//Personal note editor
	private TextView mTextView; 			 	//Name
	private TextView mTextPersonalNote;  	 	//Personal note label
	private TextView mTextPublicNote;   	 	//Group note label
	private boolean sysEdit = false;
	
	public UserInfo getUserInfo() {
		return mUserInfo;
	}

	public void setUserInfo(UserInfo mUserInfo) {
		this.mUserInfo = mUserInfo;
	}

	public BigBubblePopupWindow(View contentView, UserInfo userInfo){
		//super(contentView);
		super(contentView, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, true);
		
		// Initialize pie graph
		mPieGraph = (PieGraph)contentView.findViewById(R.id.piegraph_boarder);
		initDonutChart();
		mLockButton = (Button)contentView.findViewById(R.id.button_lock_inbubble);
		mLockButton.setOnClickListener(new LockButtonOnClickListener());
		mEditText = (EditText)contentView.findViewById(R.id.edittext_inbubble);
		//mEditText.setOnFocusChangeListener(new AmountOfMoneyFocusChangeListener());
		mEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,int after) {}
			@Override
			public void afterTextChanged(Editable s) {
				if(sysEdit) Log.d("bigUi", "sys changed amount");						//the code that populates the amount field will trigger this listener, but this should only run if a USER edited the field
				else {
					Log.d("bigUi", "user changed amount");
					Float currentMoney = 0.0f;
					try{
						currentMoney = Float.valueOf(mEditText.getText().toString());
					} catch (NumberFormatException e){
						;
					}
					
					if (currentMoney != mUserInfo.getAmountOfMoney()){
						mUserInfo.setAmountOfMoney(currentMoney);
						determineLock();
					}
				}
			}
		});
		
		mEditPersonalNote = (EditText)contentView.findViewById(R.id.edittext_private_note_inbubble);
		mEditPersonalNote.setOnFocusChangeListener(new PersonalNoteChangeListener());

		mTextPersonalNote = (TextView)contentView.findViewById(R.id.textview_private_note_inbubble);
		mTextPersonalNote.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mTextPersonalNote.setVisibility(View.GONE);
				mEditPersonalNote.setVisibility(View.VISIBLE);
				if(null == mUserInfo)
					return;
				String personalNote = mUserInfo.getPersonalNote();
				if (null != personalNote &&
						!personalNote.isEmpty())
					mEditPersonalNote.setText(personalNote);
			}
		});
		
		mTextPublicNote = (TextView)contentView.findViewById(R.id.textview_public_note_inbubble);
		mTextView = (TextView)contentView.findViewById(R.id.textview_name_inbubble);

		mUserInfo = userInfo;
		showUserInfo();
	}
	
	public boolean isPersonalNoteEmpty(){
		if(null == mUserInfo)
			return true;
		String personalNote = mUserInfo.getPersonalNote();
		if (personalNote == null)
			return true;
		else if(personalNote.isEmpty())
			return true;
		else
			return false;
	}
	
	public boolean isPublicNoteEmpty(){
		if(null == mUserInfo)
			return true;
		String publicNote = mUserInfo.getPublicNote();
		if (publicNote == null)
			return true;
		else if(publicNote.isEmpty())
			return true;
		else
			return false;
	}
	
	/**
	 * Call setUserInfo() beforehand!!
	 */
	public void showUserInfo() {
		if (null == mUserInfo){
			Log.w(TAG, "Please call setUserInfo first!");
			return;
		}
		UserInfo userInfo = mUserInfo;
		mTextView.setText(userInfo.getUserName());
		
		float moneyAmount = userInfo.getAmountOfMoney();
		sysEdit = true;
		if(moneyAmount < 0.01f){
			mEditText.setText("");
		} else {
			mEditText.setText(String.format("%.2f", userInfo.getAmountOfMoney()));
		}
		sysEdit = false;
		
		if(userInfo.isLocked()){
			mLockButton.setBackgroundResource(R.drawable.locked_darkgreen);
		}
		
		if(!isPublicNoteEmpty()){
			mTextPublicNote.setText(userInfo.getPublicNote());
		} else {
			mTextPublicNote.setText("Group note not set yet.");
		}
		if(isPersonalNoteEmpty()){
			mTextPersonalNote.setVisibility(View.GONE);
			mEditPersonalNote.setVisibility(View.VISIBLE);
		} else {
			mTextPersonalNote.setText(userInfo.getPersonalNote());
		}
	}

	private void initDonutChart() {
		if(null == mPieGraph){
			Log.e(TAG, "Unable to get mPieGraph");
			return;
		}
		mPieGraph.setInnerCircleRatio(230); //TODO: Set a precise value...

	}

	/**
	 * Set chart data of outer donut chart (border)
	 * @param values Values 
	 */
	public void setDonutChartData(ArrayList<Float> values){
		mPieGraph.removeSlices(); //Clear all slices first
		PieSlice pieSlice ;
		int i = 0;
		for(Float value: values){
			Log.d(TAG, "i=" + i);
			pieSlice = new PieSlice();
			pieSlice.setColor(Color.parseColor(COLOR_MAP[i++]));
			pieSlice.setValue(value);
			mPieGraph.addSlice(pieSlice);
			if(i >= COLOR_MAP.length) i = 0; //In case of over-length
		}
	}

	// Customized listeners below //

	private class LockButtonOnClickListener implements View.OnClickListener{

		@Override
		public void onClick(View v) {
			if (mUserInfo.isLocked()){ 						//Unlock
				mUserInfo.setLocked(false);
				mLockButton.setBackgroundResource(R.drawable.unlocked_darkgreen2);
			} else {
				mUserInfo.setLocked(true);
				mLockButton.setBackgroundResource(R.drawable.locked_darkgreen);
			}
		}

	}

	private void determineLock(){
		if (mEditText.getText().toString().equals("")){ 						//Unlock
			mUserInfo.setLocked(false);
			mLockButton.setBackgroundResource(R.drawable.unlocked_darkgreen2);
		} else {
			mUserInfo.setLocked(true);
			mLockButton.setBackgroundResource(R.drawable.locked_darkgreen);
		}
	}

	private class PersonalNoteChangeListener implements View.OnFocusChangeListener{

		@Override
		public void onFocusChange(View v, boolean hasFocus) {
			if (hasFocus)
				return;

			String currNote = mEditPersonalNote.getText().toString();
			if (currNote.isEmpty()) {
				mUserInfo.setPersonalNote("");
				mTextPersonalNote.setText("");
			}
			if (currNote != null && !currNote.isEmpty()){
				mUserInfo.setPersonalNote(currNote);
				mEditPersonalNote.setVisibility(View.GONE);
				mTextPersonalNote.setVisibility(View.VISIBLE);
				mTextPersonalNote.setText(currNote);
			}
		}
	}
}
