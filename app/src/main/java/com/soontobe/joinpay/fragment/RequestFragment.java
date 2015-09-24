package com.soontobe.joinpay.fragment;

import android.util.Log;

import com.soontobe.joinpay.Constants;
import com.soontobe.joinpay.model.UserInfo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * It is one of the three fragments in the radar view activity. In this fragment, users can specify the transaction detail and click "Next" to
 * move forwards to next step.
 */
public class RequestFragment extends TransactionFragment {
	public ArrayList<String[]> getPaymentInfo() {
		ArrayList<String[]> paymentInfo = new ArrayList<String[]>();
		Log.d("paymentInfo","requesting to: " + Constants.userName);
		
		
		String[] groupNote = {"group_note", mGroupNote.getText().toString()};
		if (groupNote[1].length() > 0) {
			Log.d("paymentInfo", "adding the group note: " + groupNote[1]);
			paymentInfo.add(groupNote);
		}
		
		for (UserInfo info : mUserInfoList) {
			if (info.isSelecetd()) {
				if(!info.getUserName().equals(Constants.userName)){			//shouldn't be possible, just double check
					if(info.getAmountOfMoney() > 0){
						String[] item = {"normal", info.getPersonalNote(), info.getUserName(), myUserInfo.getUserName(), "$ " + TransactionFragment.penniesToString(info.getAmountOfMoney()), "isPending", "requesting"};
						paymentInfo.add(item);
					}
				}
				else Log.d("paymentInfo", "skipping self");
			}
		}
		
		Calendar c = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy  HH:mm:ss");
		String strDate = sdf.format(c.getTime());

		String[] summary = {"summary", strDate, String.valueOf(getSelectedUserSize()), "$ " + mTotalAmount.getText().toString()};
		paymentInfo.add(summary);
		return paymentInfo;
	}
}

