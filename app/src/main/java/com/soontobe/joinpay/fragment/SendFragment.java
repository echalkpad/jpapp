package com.soontobe.joinpay.fragment;

import android.util.Log;

import com.soontobe.joinpay.Constants;
import com.soontobe.joinpay.model.UserInfo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * This is one of the three fragments in the radar view activity and
 * it is very similar to RequestFragment.
 */
public class SendFragment extends TransactionFragment {

    /**
     * Used for tagging logs from this class.
     */
    private static final String TAG = "paymentInfo";

    /**
     * Creates a list of transaction information for the confirmation
     * screen.
     *
     * @return A list of transaction data.
     */
    public final ArrayList<String[]> getPaymentInfo() {
        ArrayList<String[]> paymentInfo = new ArrayList<String[]>();
        Log.d(TAG, "sending from: " + Constants.userName);
        for (UserInfo info : getmUserBubbles().keySet()) {
            if (info.isSelected()) {
                if (!info.getUserName().equals(Constants.userName)) {
                    String[] item = {"normal", "", // No personal notes
                            getMyUserInfo().getUserName(),
                            info.getUserName(),
                            "$ " + String.format("%.2f", info
                                    .getAmountOfMoney()),
                            "notPending", "sending"};
                    paymentInfo.add(item);
                    Log.d(TAG, item[2]);
                } else {
                    Log.d(TAG, "skipping self");
                }
            }
        }

        String[] groupNote = {"group_note",
                getmGroupNote().getText().toString()};
        if (groupNote[1].length() > 0) {
            paymentInfo.add(groupNote);
        }

        // Generate a date for the transaction.
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy  HH:mm:ss");
        String strDate = sdf.format(c.getTime());

        String[] summary = {"summary", strDate,
                String.valueOf(getKeeper().selectedUsers()),
                "$ " + getmTotalAmount().getText().toString()};
        paymentInfo.add(summary);
        return paymentInfo;
    }
}
