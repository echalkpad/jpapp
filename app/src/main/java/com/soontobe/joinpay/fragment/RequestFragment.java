package com.soontobe.joinpay.fragment;

import android.util.Log;

import com.soontobe.joinpay.Constants;
import com.soontobe.joinpay.model.UserInfo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * It is one of the three fragments in the radar view activity. In this
 * fragment, users can specify the transaction detail and click "Next" to
 * move forwards to next step.
 */
public class RequestFragment extends TransactionFragment {

    /**
     * Used for tagging logs from this class.
     */
    private static final String TAG = "paymentInfo";

    /**
     * This creates a list containing transaction information for the
     * transaction confirmation page.
     *
     * @return Transaction information.
     */
    public final ArrayList<String[]> getPaymentInfo() {
        ArrayList<String[]> paymentInfo = new ArrayList<String[]>();
        Log.d(TAG, "requesting to: " + Constants.userName);


        String[] groupNote = {TAG, mGroupNote.getText().toString()};
        if (groupNote[1].length() > 0) {
            Log.d(TAG, "adding the group note: " + groupNote[1]);
            paymentInfo.add(groupNote);
        }

        for (UserInfo info : mUserInfoList) {
            if (info.isSelected()) {
                // shouldn't be possible, just double check
                if (!info.getUserName().equals(Constants.userName)) {
                    if (info.getAmountOfMoney() > 0) {
                        String[] item = {"normal", info.getPersonalNote(),
                                info.getUserName(),
                                myUserInfo.getUserName(),
                                "$ " + TransactionFragment
                                .penniesToString(info.getAmountOfMoney()),
                                "isPending", "requesting"};
                        paymentInfo.add(item);
                    }
                } else {
                    Log.d(TAG, "skipping self");
                }
            }
        }

        // Generate a date for the transaction.
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf =
                new SimpleDateFormat("dd-MM-yyyy  HH:mm:ss");
        String strDate = sdf.format(c.getTime());

        String[] summary = {"summary", strDate,
                String.valueOf(getSelectedUserSize()),
                "$ " + mTotalAmount.getText().toString()};
        paymentInfo.add(summary);
        return paymentInfo;
    }
}

