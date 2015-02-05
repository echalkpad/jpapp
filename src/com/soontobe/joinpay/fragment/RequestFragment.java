package com.soontobe.joinpay.fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import android.util.Log;

import com.soontobe.joinpay.Constants;
import com.soontobe.joinpay.model.UserInfo;

/**
 * It is one of the three fragments in the radar view activity. In this fragment, users can specify the transaction detail and click "Next" to
 * move forwards to next step.
 */
public class RequestFragment extends TransactionFragment {
	public ArrayList<String[]> getPaymentInfo() {
		ArrayList<String[]> paymentInfo = new ArrayList<String[]>();
		Log.d("paymentInfo","requesting to: " + Constants.userName);
		for (UserInfo info : mUserInfoList) {
			if (info.isSelecetd()) {
				if(!info.getUserName().equals(Constants.userName)){			//shouldn't be possible, just double check
					String[] item = {"normal", info.getPersonalNote(), info.getUserName(), myUserInfo.getUserName(), "$ " + String.format("%.2f",info.getAmountOfMoney()), "isPending", "requesting"};
					paymentInfo.add(item);
				}
				else Log.d("paymentInfo", "skipping self");
			}
		}
		
		/* Doesn't make sense to send money to ourselves */
		/*if (myUserInfo.isSelecetd()) {
			String[] item = {"normal", myUserInfo.getPersonalNote(), myUserInfo.getUserName(), myUserInfo.getUserName(), "$ " + String.format("%.2f",myUserInfo.getAmountOfMoney()), "notPending", "request"};
			paymentInfo.add(item);
		}*/

		String[] groupNote = {"group_note", mGroupNote.getText().toString() };
		if (groupNote[1].length() > 0) paymentInfo.add(groupNote);


		Calendar c = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy  HH:mm:ss");
		String strDate = sdf.format(c.getTime());

		String[] summary = {"summary", strDate, String.valueOf(getSelectedUserSize()), "$ " + mTotalAmount.getText().toString()};
		paymentInfo.add(summary);
		/* example */
//		{
//			{"normal", "", "Luna", "Itziar", "$ 500", "Pending"},
//			{"normal", "Pay one extra beer", "Patrick", "Itziar", "$ 30", "Pending"},   //	name, amount, personal note
//			{"normal", "", "asd", "Itziar", "$ 20", "Pending"},
//			{"normal", "", "Itziar", "Itziar", "$ 20", ""},
//			{"group_note", "This is a group note"},
//			{"summary", "2014-11-14", "5", "$ 130"}
//		}

		return paymentInfo;
	}
}

