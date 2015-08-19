package com.soontobe.joinpay.fragment;

import java.util.ArrayList;
import java.util.Random;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.TextView;
import com.soontobe.joinpay.Constants;
import com.soontobe.joinpay.PositionHandler;
import com.soontobe.joinpay.R;
import com.soontobe.joinpay.RadarViewActivity;
import com.soontobe.joinpay.Utility;
import com.soontobe.joinpay.model.UserInfo;
import com.soontobe.joinpay.widget.BigBubblePopupWindow;
import com.soontobe.joinpay.widget.RadarUserView;
import com.soontobe.joinpay.widget.RadarUserView.OnCenterButtonClickedListener;
import com.soontobe.joinpay.widget.RadarUserView.OnDeselectButtonClickedListener;
import com.soontobe.joinpay.widget.RadarUserView.OnEditButtonClickedListener;
import com.soontobe.joinpay.widget.RadarUserView.OnLockButtonClickedListener;

/**
 * 
 * This is the base(parent) fragment for Send/Request fragment.
 *
 */

public abstract class TransactionFragment extends Fragment implements LoaderCallbacks<Void> {
	static Context mApplicationContext;
	private OnFragmentInteractionListener mListener;
	private FrameLayout mBubbleFrameLayout;
	public static ArrayList<RadarUserView> mUserBubbles;
	public static RadarUserView mSelfBubble;
	private View mCurrentView;
	private BigBubblePopupWindow mBigBubble;
	protected static TextView mSelectCountText; 					//Number of selected user
	public static EditText mTotalAmount;
	public static Button mSendMoneyButton;
	protected static EditText mGroupNote;
	public static int totalLockedAmount;
	public static UserInfo myUserInfo;
	public static ArrayList<UserInfo> mUserInfoList; 				//User info list except for myself
	protected ArrayList<Integer> mUserPositions; 					//User info list except for myself

	public TransactionFragment() {
		// Required empty public constructor
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onStop() {

		super.onStop();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		if (mCurrentView == null) {
			mCurrentView = inflater.inflate(R.layout.main_tab, container,false);
			init();
		}

		// Create ViewGroup if the object does not exist, otherwise use the current one.

		ViewGroup parent = (ViewGroup) mCurrentView.getParent();
		if (parent != null) {
			parent.removeView(mCurrentView);
		}
		Utility.setupKeyboardAutoHidden(mCurrentView, getActivity());

		return mCurrentView;
	}

	private void init() {
		mBubbleFrameLayout = (FrameLayout) mCurrentView.findViewById(R.id.layout_send_frag_bubbles);
		mBubbleFrameLayout.getViewTreeObserver().addOnGlobalLayoutListener( new MyOnGlobalLayoutChgListener() );
		mBubbleFrameLayout.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// Un-expand small bubbles
				mSelfBubble.switchExpandPanel(false);
				for (RadarUserView ruv : mUserBubbles) {
					ruv.switchExpandPanel(false);
				}

				// Catch the focus (from TotalAmout EditText)
				mBubbleFrameLayout.requestFocus();
			}
		});

		mSelfBubble = (RadarUserView) mCurrentView.findViewById(R.id.user_bubble_myself);
		mSelectCountText = (TextView) mCurrentView.findViewById(R.id.send_num_of_people);
		mTotalAmount = (EditText) mCurrentView.findViewById(R.id.edit_text_total_amount);
		mSendMoneyButton = (Button) mCurrentView.findViewById(R.id.send_money_next);
		mGroupNote = (EditText) mCurrentView.findViewById(R.id.group_note);

		myUserInfo = new UserInfo();
		myUserInfo.setUserId(new Random().nextInt());
		myUserInfo.setUserName(Constants.DemoMyName);
		// myUserInfo.setContactState(true);
		myUserInfo.setMyself(true);
		totalLockedAmount = 0;
		mSelfBubble.setUserInfo(myUserInfo);
		mSelfBubble.setEditBtnClickedListener(new OnEditButtonClickedListener() {
				@Override
				public void OnClick(View v) {
					showBigBubble(myUserInfo);
				}
		});
		mSelfBubble.setCenterBtnClickedListener(new OnCenterButtonClickedListener() {
				@Override
				public void OnClick(View v, boolean isSelected) {
					myUserInfo.setSelecetd(isSelected);
					updateSelectedUserNumber();
					//Editable edit = mTotalAmount.getText();
					splitMoney();
				}
		});
		mSelfBubble.setDeselectBtnClickedListener(new OnDeselectButtonClickedListener() {
				@Override
				public void OnClick(View v) {
					Log.d("bubble", "self bubble deselect");
					myUserInfo.setSelecetd(false);
					updateSelectedUserNumber();
					splitMoney();
				}
		});

		mUserInfoList = new ArrayList<UserInfo>();
		mUserBubbles = new ArrayList<RadarUserView>();
		mUserPositions = new ArrayList<Integer>();
		mTotalAmount.setOnFocusChangeListener(new OnTotalMoneyFocusChangeListener());
		mTotalAmount.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,int after) {}
			@Override
			public void afterTextChanged(Editable s) {
				splitMoney();
			}
		});

		mGroupNote.setOnFocusChangeListener(new OnGroupNoteFocusChangeListener());
	}	
	
	//convert string that has dollars and decimal point to integer of pennies
	public static int stringToPennies(String str){
		int pennies = 0;
		int int_dollars = 0;
		int int_change = 0;
		int pos = str.indexOf('.');														//find decimal position
		if(pos >= 0){																	//input has decimal, ie: $15.10
			String dollars = str.substring(0, pos);
			String change = str.substring(pos+1);
			try {int_dollars = Integer.parseInt(dollars);}
			catch(Exception e){}
			if(change.length() <= 1) change += '0';										//input has 1 decimal... ie: $15.1
			if(change.length() >= 3) change = change.substring(0, 2);					//only 2 digits allowed... no fractions of a penny
			try{int_change = Integer.parseInt(change);}
			catch(Exception e){}
			pennies = int_dollars * 100 + int_change;
		}
		else{																			//input has NO decimal, ie: $15
			try {int_dollars = Integer.parseInt(str);}
			catch(Exception e){}
		}
		pennies = int_dollars * 100 + int_change;
		//Log.d("money", "PENNIES: " + pennies);
		return pennies;
	}
	
	//convert integer of pennies to string with dollar and decimal point
	public static String penniesToString(int pennies){
		String str = "";
		String dollars = "0";
		String change = "0";
		String str_pennies = Integer.toString(pennies);
		if(pennies >= 100){														//there are dollars, ie: 100 = $1.00
			dollars = str_pennies.substring(0, str_pennies.length() - 2);
			change = str_pennies.substring(str_pennies.length() - 2);
		}
		else if(pennies >= 10){													//there are NO dollars, 2 digits, ie: 20 = $0.20
			dollars = "0";
			change = str_pennies;
		}
		else{																	//there are NO dollars, 1 digit, ie: 5 = $0.5
			dollars = "0";
			change = "0" + str_pennies;
		}
		
		str = dollars + '.' + change;
		//Log.d("money", "input: " + pennies + ", (dollars): " + dollars + ", (change): " + change);
		return str;
	}
	
	//// Split the Total Bill - evenly between currently selected users ////
	public static void splitMoney(){
		Editable edit = mTotalAmount.getText();
		int totalPennies = stringToPennies(edit.toString());
		
		if(totalPennies == 0) mSendMoneyButton.setEnabled(false);
		else mSendMoneyButton.setEnabled(true);

		Log.d("money", "recalculting split!, locked amount: " + totalLockedAmount + ", totalPennies: " + totalPennies);
		ArrayList<Integer> targetUserIndex = getUnlockedSelectedUserIndex();
		int size = targetUserIndex.size();
		if(size > 0){			
			int safeTotal = totalPennies - totalLockedAmount;										//remove locked amount, divide the rest evenly
			int safeSplit = safeTotal / size;														//the largest amount that we can evenly split between users
			//if(safeTotal < size) safeSplit = 0;														//corner case... less than 1 penny per person, let the round robin handle it below
			int safeCheckRounding = safeSplit * size;
			int roundErrorRecover = 0;
			Log.d("money","(pennies) total: " + safeTotal + " == splitTotal: " + safeCheckRounding + ", #:" + size + ", split: " + safeSplit);
						
			int i = 1;
			//// Divide the total equally ////
			for(Integer index : targetUserIndex) {
				if(index == -1) {
					myUserInfo.setAmountOfMoney(safeSplit);
					mSelfBubble.setUserInfo(myUserInfo);
					Log.d("money", "[initial] user: " + myUserInfo.getUserName() + " $" + penniesToString(safeSplit));
				}
				else {
					mUserInfoList.get(index).setAmountOfMoney(safeSplit);
					mUserBubbles.get(index).setUserInfo(mUserInfoList.get(index));
					Log.d("money", "[initial] user: " + mUserInfoList.get(index).getUserName() + " $" + penniesToString(safeSplit) + ", i:" + i);
				}
				i++;
			}
			
			//// Divide the rounding error remainder ////
			if(safeCheckRounding != safeTotal){
				roundErrorRecover = safeTotal - safeCheckRounding;
				Log.d("money", "total is not evenly divisable!, round robin and divvy up the remaining cent(s): " + roundErrorRecover);
				safeSplit += 1;
				for(Integer index : targetUserIndex) {
					if(index == -1) {																//do self bubble first
						myUserInfo.setAmountOfMoney(safeSplit);
						mSelfBubble.setUserInfo(myUserInfo);
						Log.d("money", "[final] user: " + myUserInfo.getUserName() + " $" + penniesToString(safeSplit));
					}
					else {																			//do other bubbles next
						mUserInfoList.get(index).setAmountOfMoney(safeSplit);
						mUserBubbles.get(index).setUserInfo(mUserInfoList.get(index));
						Log.d("money", "[final] user: " + mUserInfoList.get(index).getUserName() + " $" + penniesToString(safeSplit));
					}
					roundErrorRecover--;
					if(roundErrorRecover <= 0) break;												//its all been divvied up, end
				}
			}
		}
		else{
			Log.d("money", "there are no users to split payment!");
		}
	}
	
	/**
	 * Remove user from RadarView by his index in mUserInfoList of mUserBubbles
	 * 
	 * @param index
	 */
	public void removeUserFromView(int index) {
		mBubbleFrameLayout.removeView(mUserBubbles.get(index));
		mUserBubbles.remove(index);
		mUserInfoList.remove(index);
	}

	/**
	 * Add a contact user to view
	 * 
	 * @param contactName
	 */
	public void addContactToView(String contactName, int position) {
		if (!generateBubbles(1, position, contactName)) return;
		int index = mUserInfoList.size() - 1;
		mUserInfoList.get(index).setContactState(true);
		mUserInfoList.get(index).setUserName(contactName);
		mUserBubbles.get(index).setUserInfo(mUserInfoList.get(index));
	}

	/**
	 * Add a user to view
	 * 
	 * @param userName
	 */
	public void addUserToView(String userName, int position) {
		for (UserInfo userInfo : mUserInfoList) {
			if (userName.equals(userInfo.getUserName())) {
				Log.d("bubble", "user already has bubble, skipping");
				return;
			}
		}
		if (!generateBubbles(1, position, userName))
			return;
		int index = mUserInfoList.size() - 1;
		mUserInfoList.get(index).setUserName(userName);
		mUserBubbles.get(index).setUserInfo(mUserInfoList.get(index));
	}

	/**
	 * Generate user bubbles.
	 * 
	 * @param qty
	 *            Amount of users to be generated.
	 */
	@SuppressLint("RtlHardcoded")
	public boolean generateBubbles(int qty, int position, String username) {
		Log.d("bubble", "starting generating bubble for: " + username);
		if(position > PositionHandler.MAX_USER_SUPPORTED) {
			Log.e("SendFragment::generateBubbles", "Maximum user quantity exceed!");
			Log.d("bubble", "can't generate bubble at pos: " + position);
			return false;
		}
		Log.d("bubble", "generating bubble at pos: " + position);
		int frameHeight = mBubbleFrameLayout.getHeight();
		int frameWidth = mBubbleFrameLayout.getWidth();
		int widgetWidth = mSelfBubble.getWidth();
		Random random = new Random();
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(mSelfBubble.getLayoutParams());
		
		float pos[] = { PositionHandler.RAND_BUBBLE_CENTER_POS_X[position], PositionHandler.RAND_BUBBLE_CENTER_POS_Y[position] };
		pos[0] = pos[0] * frameWidth - widgetWidth / 2;
		pos[1] = pos[1] * frameHeight - widgetWidth / 2;
		Log.d("bubble", "x=" + pos[0] + ", y=" + pos[1]);
		params = new FrameLayout.LayoutParams(mSelfBubble.getLayoutParams());
		params.gravity = Gravity.LEFT | Gravity.TOP;
		params.setMargins((int) pos[0], (int) pos[1], 0, 0);
		RadarUserView ruv = new RadarUserView(getActivity());
		mBubbleFrameLayout.addView(ruv, params);

		UserInfo info = new UserInfo();
		info.setUserName(username);
		info.setUserId(random.nextInt());
		mUserPositions.add(position);
		mUserInfoList.add(info);

		int index = mUserInfoList.size() - 1;
//		mUserInfoList.set(position, info);
		ruv.setUserInfo(info);
		ruv.setEditBtnClickedListener(new EditButtonOnClickListener(index));
		ruv.setLockBtnClickedListener(new LockButtonOnClickListener(index));
		ruv.setCenterBtnClickedListener(new SelectUserOnClickListener(index));
		ruv.setDeselectBtnClickedListener(new DeselectUserOnClickListener(index));
		mUserBubbles.add(ruv);
//		mUserBubbles.set(position, ruv);
		return true;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		setMyName(Constants.userName);
		super.onActivityCreated(savedInstanceState);
	}

	// TODO: Rename method, update argument and hook method into UI event
	public void onButtonPressed(Uri uri) {
		if (mListener != null) {
			mListener.onFragmentInteraction(uri);
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (OnFragmentInteractionListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement OnFragmentInteractionListener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
	}

	public interface OnFragmentInteractionListener {
		// TODO: Update argument type and name
		public void onFragmentInteraction(Uri uri);
	}

	@Override
	public Loader<Void> onCreateLoader(int arg0, Bundle arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onLoadFinished(Loader<Void> arg0, Void arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onLoaderReset(Loader<Void> arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onResume() {
		super.onResume();

	}

	/**
	 * Show big bubble (transaction detail)
	 * 
	 * @param userInfo Pass UserInfo object in.
	 */
	public void showBigBubble(UserInfo userInfo) {
		View popupView = getActivity().getLayoutInflater().inflate(R.layout.big_bubble, null);
		mBigBubble = new BigBubblePopupWindow(popupView, null);
		mBigBubble.setTouchable(true);
		mBigBubble.setBackgroundDrawable(new BitmapDrawable()); // Outside
																// disimss-able
		//TODO: Values of outer torus should be set here.
		ArrayList<Float> dataList = new ArrayList<Float>();
		dataList.add(1.05f);
		dataList.add(2.55f);

		mBigBubble.setDonutChartData(dataList);
		mBigBubble.setUserInfo(userInfo);
		mBigBubble.showUserInfo();
		mBigBubble.setOnDismissListener(new OnBigBubbleDismissListener());
		mBigBubble.showAtLocation(
				getActivity().findViewById(R.id.btn_radar_view_back),
				Gravity.CENTER | Gravity.TOP, 0, 200);
	}

	private class OnBigBubbleDismissListener implements OnDismissListener {

		@Override
		public void onDismiss() {
			UserInfo userInfo = mBigBubble.getUserInfo();
			int uid = userInfo.getUserId();
			int index = findUserIndexById(uid);
			if (index == -1) {
				myUserInfo = userInfo;
				mSelfBubble.setUserInfo(myUserInfo);
				//applyFurtherMoneyChange(index, mOldMoneyAmount, myUserInfo.getAmountOfMoney());
			} else if (index == -2) {
				Log.w("OnBigBubbleDismissListener", "Could not find user id="+ userInfo.getUserId());
			} else {
				mUserInfoList.set(index, userInfo);
				mUserBubbles.get(index).setUserInfo(userInfo);
				//applyFurtherMoneyChange(index, mOldMoneyAmount, mUserInfoList.get(index).getAmountOfMoney());
			}

			Log.d("OnBigBubbleDismissListener", userInfo.toString());

		}

		/*private void applyFurtherMoneyChange(int indexOfUser, float oldAmount, float currentAmount) {
			if (!getTotalLockState()) {
				// Total amount is not locked
				float moneyChanged = currentAmount - oldAmount;
				float oldTotalAmount = 0;
				try {
					oldTotalAmount = Float.valueOf(mTotalAmount.getEditableText().toString());
				} catch (NumberFormatException e) {
					oldTotalAmount = 0;
				}
				float newAmount = oldTotalAmount + moneyChanged;
			} else {
				// Total amount is locked, split the balance to (unlocked && selected) users
				float moneyChanged = currentAmount - oldAmount;
				ArrayList<Integer> unlockedSelectedUserIndexList = getUnlockedSelectedUserIndex();
				int size = unlockedSelectedUserIndexList.size() - 1; // except
																		// me
				int moneyToSplit = moneyChanged / (float) size;
				for (int index : unlockedSelectedUserIndexList) {
					if (indexOfUser == index)
						continue;
					if (-1 == index) {
						int old = myUserInfo.getAmountOfMoney();
						myUserInfo.setAmountOfMoney(old - moneyToSplit);
						mSelfBubble.setUserInfo(myUserInfo);
						continue;
					}

					float old = mUserInfoList.get(index).getAmountOfMoney();
					mUserInfoList.get(index).setAmountOfMoney(old - moneyToSplit);
					mUserBubbles.get(index).setUserInfo(mUserInfoList.get(index));
				}
			}
		}*/

	}

	/**
	 * 
	 * @param userId
	 * @return index of UserInfo object in ArrayList, Myserlf = -1, not found = 
	 *         -2
	 */
	public int findUserIndexById(int userId) {
		if (myUserInfo.getUserId() == userId)
			return -1;
		for (int i = 0; i < mUserInfoList.size(); i++) {
			UserInfo info = mUserInfoList.get(i);
			if (info.getUserId() == userId)
				return i;
		}
		return -2;
	}

	private class MyOnGlobalLayoutChgListener implements
			ViewTreeObserver.OnGlobalLayoutListener {

		@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
		@Override
		public void onGlobalLayout() {

			// generateBubbles(2);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
				mBubbleFrameLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
			else
				mBubbleFrameLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
		}

	}

	/**
	 * Customized OnClickListener listening to the button-click of
	 *  `EDIT` button in the expanded small bubble of each user
	 *
	 */
	private class EditButtonOnClickListener implements
			OnEditButtonClickedListener {
		int indexOfBubble;

		public EditButtonOnClickListener(int index) {
			indexOfBubble = index;
		}

		@Override
		public void OnClick(View v) {
			showBigBubble(mUserInfoList.get(indexOfBubble));

		}

	}
	
	/**
	 * Customized OnClickListener listening to the button-click of
	 *  `LOCK` button in the expanded small bubble of each user
	 *
	 */
	private class LockButtonOnClickListener implements
			OnLockButtonClickedListener {
		int indexOfBubble;

		public LockButtonOnClickListener(int index) {
			indexOfBubble = index;
		}

		@Override
		public void OnClick(View v, boolean isLocked) {
			mUserInfoList.get(indexOfBubble).setLocked(isLocked);
			if(isLocked) {
				totalLockedAmount += mUserInfoList.get(indexOfBubble).getAmountOfMoney();
				Log.d("money","adding locked amount: " + mUserInfoList.get(indexOfBubble).getAmountOfMoney());
			}
			else{
				totalLockedAmount -= mUserInfoList.get(indexOfBubble).getAmountOfMoney();
				Log.d("money","removing locked amount: " + mUserInfoList.get(indexOfBubble).getAmountOfMoney());
				if(totalLockedAmount < 0) totalLockedAmount = 0;
			}
			Log.d("money","locked total: " + totalLockedAmount);
			Log.d(getTag(), "User" + indexOfBubble + " lock state = " + isLocked);
			splitMoney();
		}

	}
	
	/**
	 * Customized OnClickListener listening to the button-click of
	 *     small bubble of each user
	 *
	 */
	private class SelectUserOnClickListener implements
			OnCenterButtonClickedListener {
		int indexOfBubble;

		public SelectUserOnClickListener(int index) {
			indexOfBubble = index;
		}

		@Override
		public void OnClick(View v, boolean isSelected) {
			mUserInfoList.get(indexOfBubble).setSelecetd(isSelected);
			Log.d(getTag(), "User" + indexOfBubble + " select state = " + isSelected);
			updateSelectedUserNumber();
			//Editable edit = mTotalAmount.getText();
			splitMoney();
		}
	}
	
	/**
	 * Customized OnClickListener listening to the button-click of
	 *      user de-selection action
	 *
	 */
	private class DeselectUserOnClickListener implements
			OnDeselectButtonClickedListener {
		int indexOfBubble;

		public DeselectUserOnClickListener(int index) {
			indexOfBubble = index;
		}

		@Override
		public void OnClick(View v) {
			Log.d("bubble", "deselect bubble");
			mUserInfoList.get(indexOfBubble).setSelecetd(false);
			Log.d(getTag(), "User" + indexOfBubble + " deselected");
			updateSelectedUserNumber();
			
			if(mUserInfoList.get(indexOfBubble).isLocked()){
				Log.d("money","removing locked amount: " + mUserInfoList.get(indexOfBubble).getAmountOfMoney());
				totalLockedAmount -= mUserInfoList.get(indexOfBubble).getAmountOfMoney();
				if(totalLockedAmount < 0) totalLockedAmount = 0;
			}
			splitMoney();
		}

	}

	public abstract ArrayList<String[]> getPaymentInfo();

	private class OnTotalMoneyFocusChangeListener implements OnFocusChangeListener {
		@Override
		public void onFocusChange(View v, boolean hasFocus) {
			splitMoney();
		}
	}

	private class OnGroupNoteFocusChangeListener implements
			OnFocusChangeListener {

		@Override
		public void onFocusChange(View v, boolean hasFocus) {
			if (hasFocus)
				return;
			String groupNote = mGroupNote.getEditableText().toString();
			myUserInfo.setPublicNote(groupNote);
			mSelfBubble.setUserInfo(myUserInfo);
			for (int i = 0; i < mUserInfoList.size(); i++) {
				mUserInfoList.get(i).setPublicNote(groupNote);
				mUserBubbles.get(i).setUserInfo(mUserInfoList.get(i));
			}

		}

	}

	/**
	 * Get selected user index
	 * 
	 * @return Index array. The index of myself = -1;
	 */
	public ArrayList<Integer> getSelectedUserIndex() {
		// TODO:
		ArrayList<Integer> retList = new ArrayList<Integer>();
		if (myUserInfo.isSelecetd()) {
			retList.add(-1);
		}

		int userSize = mUserInfoList.size();
		for (int i = 0; i < userSize; i++) {
			if (mUserInfoList.get(i).isSelecetd())
				retList.add(i);
		}
		return retList;
	}

	public int getSelectedUserSize() {
		return getSelectedUserIndex().size();
	}

	public void updateSelectedUserNumber() {
		int selectedUserNum = getSelectedUserSize();
		mSelectCountText.setText(String.valueOf(selectedUserNum));
		if (selectedUserNum > 0) {
			mTotalAmount.setEnabled(true);
			mSendMoneyButton.setEnabled(true);
		} else {
			mTotalAmount.setEnabled(false);
			mSendMoneyButton.setEnabled(false);
		}
	}

	public boolean getTotalLockState() {
		RadarViewActivity activity = (RadarViewActivity) getActivity();
		Boolean b = activity.lockInfo.get("total");
		return b;
	}

	/**
	 * Get unlocked && selected user index
	 * 
	 * @return Index array. The index of myself = -1;
	 */
	public static ArrayList<Integer> getUnlockedSelectedUserIndex() {
		ArrayList<Integer> retList = new ArrayList<Integer>();
		if (myUserInfo.isSelecetd() && !myUserInfo.isLocked()) {
			retList.add(-1);
		}
		int userSize = mUserInfoList.size();
		for (int i = 0; i < userSize; i++) {
			if (mUserInfoList.get(i).isSelecetd()
					&& !mUserInfoList.get(i).isLocked())
				retList.add(i);
		}
		return retList;
	}

	/**
	 * This function will clear all money amounts including the total amount.
	 */
	public static void clearUserMoneyAmount() {
		myUserInfo.setAmountOfMoney(0);
		myUserInfo.setSelecetd(false);
		mTotalAmount.setText("");
		mGroupNote.setText("");
		String groupNote = "";
		myUserInfo.setPublicNote(groupNote);
		myUserInfo.setPersonalNote("");
		mSelfBubble.setUserInfo(myUserInfo);
		for (int i = 0; i < mUserInfoList.size(); i++) {
			mUserInfoList.get(i).setLocked(false);
			mUserInfoList.get(i).setAmountOfMoney(0);
			mUserInfoList.get(i).setPublicNote(groupNote);
			mUserInfoList.get(i).setPersonalNote("");
			mUserInfoList.get(i).setSelecetd(false);
			mUserBubbles.get(i).setUserInfo(mUserInfoList.get(i));
		}
		mSelectCountText.setText("0");
		
		mTotalAmount.setEnabled(false);
		mSendMoneyButton.setEnabled(false);
	}

	/**
	 * Set name of myself (this method will update UI)
	 * 
	 * @param name
	 */
	public void setMyName(String name) {
		if (myUserInfo == null)
			return;
		myUserInfo.setUserName(name);
		if (mSelfBubble == null)
			return;
		mSelfBubble.setUserInfo(myUserInfo);
	}

}
