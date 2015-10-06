package com.soontobe.joinpay.fragment;

import android.util.Log;

import com.soontobe.joinpay.Constants;
import com.soontobe.joinpay.model.UserInfo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * It is one of the three fragments in the radar view activity and it is very similar to ReuqestFragment.
 */
public class SendFragment extends TransactionFragment {
	public ArrayList<String[]> getPaymentInfo() {
		ArrayList<String[]> paymentInfo = new ArrayList<String[]>();
		Log.d("paymentInfo","sending from: " + Constants.userName);
		for (UserInfo info : mUserInfoList) {
			if (info.isSelecetd()) {
				if(!info.getUserName().equals(Constants.userName)){
					String[] item = {"normal", info.getPersonalNote(), myUserInfo.getUserName(), info.getUserName(), "$ " + String.format("%.2f",info.getAmountOfMoney()), "notPending", "sending"};
					paymentInfo.add(item);
					Log.d("paymentInfo", item[2]);
				}
				else Log.d("paymentInfo", "skipping self");
			}
		}
		
		/* Doesn't make sense to send money to ourselves */
//		if (myUserInfo.isSelecetd()) {
//			String[] item = {"normal", myUserInfo.getPersonalNote(), myUserInfo.getUserName(), myUserInfo.getUserName(), "$ " + String.format("%.2f",myUserInfo.getAmountOfMoney())};
//			paymentInfo.add(item);
////			Log.d(" paymentInfo", item[2]);
//		}

		String[] groupNote = {"group_note", mGroupNote.getText().toString() };
		if (groupNote[1].length() > 0) paymentInfo.add(groupNote);


		Calendar c = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy  HH:mm:ss");
		String strDate = sdf.format(c.getTime());

		String[] summary = {"summary", strDate, String.valueOf(getSelectedUserSize()), "$ " + mTotalAmount.getText().toString()};
		paymentInfo.add(summary);
//		Log.d(" paymentInfo", summary[3]);
		/* example */
//		{
//			{"normal", "", "Luna", "Itziar", "$ 500", ""},
//			{"normal", "Pay one extra beer", "Patrick", "Itziar", "$ 30", ""},   //	name, amount, personal note
//			{"normal", "", "asd", "Itziar", "$ 20", ""},
//			{"normal", "", "Itziar", "Itziar", "$ 20", ""},
//			{"group_note", "This is a group note"},
//			{"summary", "2014-11-14", "5", "$ 130"}
//		}
		return paymentInfo;
	}
}