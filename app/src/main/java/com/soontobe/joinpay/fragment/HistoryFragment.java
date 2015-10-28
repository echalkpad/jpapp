package com.soontobe.joinpay.fragment;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.soontobe.joinpay.Constants;
import com.soontobe.joinpay.Globals;
import com.soontobe.joinpay.adapters.PaymentSummaryAdapter;
import com.soontobe.joinpay.R;
import com.soontobe.joinpay.helpers.Rest;
import com.soontobe.joinpay.model.Transaction;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import static com.ibm.mobile.services.data.internal.CLClientManager.runOnUiThread;

/**
 * This Fragment displays a list of user transactions.
 */
public class HistoryFragment extends Fragment {

    /**
     * Allows us to check whether REST responses are intended for
     * this activity.
     */
    private final String serviceContext = "HistoryFragment";

    /**
     * Used for tagging logs from this class.
     */
    private static final String TAG = "history";

    /**
     * The View which is returned by the LayoutInflater in onCreateView.
     */
    private View mCurrentView;

    /**
     * The inflater which inflated the fragment.  Used by the list adapter
     * to inflate items for the ListView.
     */
    private LayoutInflater mInflater;

    /**
     * The ListView used to display the user's transactions.
     */
    private ListView mHistoryLayout;

    /**
     * Adapts and beautifies transactions for the ListView.
     */
    private PaymentSummaryAdapter mAdapter;

    /**
     * The list used to actually store transactions.  Adapted for
     * the ListView by the adapter.
     */
    private ArrayList<Transaction> mTransactions;

    /**
     * Used to fill space and distract the user while the
     * transactions are requested from the database.
     */
    private ProgressBar spinner;

    /**
     * Constructs a new HistoryFragment.
     */
    public HistoryFragment() {
        // Required empty public constructor
    }

    private Context mContext;

    ArrayList<Transaction> list = new ArrayList<>();


    @Override
    public final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
    }

    @SuppressLint("NewApi")
    @Override
    public final void setUserVisibleHint(final boolean isVisibleToUser) {
        // TODO Auto-generated method stub
        super.setUserVisibleHint(isVisibleToUser);
    }

    @Override
    public final void onResume() {
        super.onResume();
    }

    @Override
    public final void onDestroy() {
        Log.d(TAG, "destroying history fragment");
        super.onDestroy();
    }

    @Override
    public final void onPause() {
        super.onPause();
    }

    @Override
    public final View onCreateView(final LayoutInflater inflater,
                                   final ViewGroup container,
                                   final Bundle savedInstanceState) {
        Log.d(TAG, "creating history view");
        if (mCurrentView == null) {
            mCurrentView = inflater.inflate(R.layout.fragment_history,
                    container, false);
        }

        ViewGroup parent = (ViewGroup) mCurrentView.getParent();
        if (parent != null) {
            parent.removeView(mCurrentView);
        }

        mInflater = inflater;

        // Grab the loading spinner so we can turn it off
        spinner = (ProgressBar) mCurrentView.findViewById(R.id.hist_prog_bar);
        // Start the spinner until data is acquired
        spinner.setVisibility(View.VISIBLE);

        // Setup transaction list to receive new transactions
        mHistoryLayout = (ListView) mCurrentView.findViewById(R.id.trans_list);
        mTransactions = new ArrayList<>();
        mAdapter = new PaymentSummaryAdapter(inflater.getContext(),
                mTransactions, inflater, approvalResponseHandler);
        mHistoryLayout.setAdapter(mAdapter);

        // Update the transaction history
        checkPendingInfo();

        return mCurrentView;
    }

    /**
     * Submits an Intent which collects the user's transaction
     * history from the JoinPay APIs.
     */
    private void checkPendingInfo() {
        String url = Constants.baseURL + "/users/" + Constants.userName + "/credits?"
                + "access_token=" + Globals.msToken;
        Rest.get(url, null, null, getCreditsResponseHandler);
    }

    @Override
    public final void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated to
     * the activity and potentially other fragments contained in that activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/
     * basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {

        /**
         * Unused fragment interaction handler.
         *
         * @param uri Doesn't matter.
         */
        void onFragmentInteraction(Uri uri);
    }

    private Rest.httpResponseHandler approvalResponseHandler = new Rest.httpResponseHandler() {
        @Override
        public void handleResponse(final JSONObject response, final boolean error) {
            if (!error) {
                int responseCode = 0;
                try {
                    responseCode = response.getInt("responseCode");
                } catch (JSONException e) {
                    e.printStackTrace();
                    showUIMessage("Invalid response from server, please retry.");
                    return;
                }
                String responseStr = "";
                String message = "error";
                Log.d("responseCode", "" + responseCode);
                try {
                    responseStr = response.getString("data");
                    Log.d("responseString", responseStr);
                    JSONObject obj = new JSONObject(responseStr);
                    if (obj.has("message")) {
                        message = obj.getString("message");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    showUIMessage("Invalid response from server, please retry.");
                    return;
                }
                switch (responseCode) {
                    case Constants.RESPONSE_200:
                        showUIMessage("Success");
                        checkPendingInfo(); // UI update
                        break;

                    case Constants.RESPONSE_400:
                    case Constants.RESPONSE_500:
                        // 400/500 = error, show msg
                        Log.d("dialog", "got " + responseCode + ", showing message");
                        showUIMessage(message);
                        break;

                    default:
                        Log.e("dialog", "response not understoood");
                        showUIMessage(message);
                        break;
                }
            } else {
                showUIMessage("Error connecting to server, please try again.");
            }
        }
    };

    /**
     * Shows a toast on the screen for short interval.
     *
     * @param message The message to be displayed on the screen
     */
    private void showUIMessage(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
            }
        });
    }


    private Rest.httpResponseHandler getCreditsResponseHandler = new Rest.httpResponseHandler() {
        @Override
        public void handleResponse(final JSONObject response, final boolean error) {
            Log.d("getPendingTransactions", "Received response: " + error);
            if (!error) {
                int httpCode = 0;
                try {
                    httpCode = response.getInt("responseCode");
                } catch (JSONException e) {
                    e.printStackTrace();
                    showUIMessage("Invalid response from server, please try again.");
                    getActivity().finish();
                }

                list = new ArrayList<>();
                String message = "error";
                String responseStr = "";
                try {
                    responseStr = response.getString("data");
                    JSONObject obj = new JSONObject(responseStr);
                    if (obj.has("message")) {
                        message = obj.getString("message");
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing JSON response");
                }

                switch (httpCode) {
                    case Constants.RESPONSE_500:
                        //500 = no transactions, do nothing
                        Log.d("history", "got 500 == no transactions");
                        break;

                    case Constants.RESPONSE_401:
                    case Constants.RESPONSE_404:
                        Log.e("history", "got 404 or 401, back to login");
                        showUIMessage("Cannot locate server");
//                        Intent intentApplication = new Intent(getActivity(),
//                               LoginActivity.class);
//                        startActivity(intentApplication);
                        getActivity().finish();
                        break;

                    case Constants.RESPONSE_200:
                        try {
                            JSONArray arrIn = new JSONArray(responseStr);

                            for (int i = 0; i < arrIn.length(); i++) {
                                JSONObject obj1 = arrIn.getJSONObject(i);
                                // a MONEY IN transaction is money i'm
                                // SENDING and is an input TO me
                                obj1.put("type", "requesting");
                                if (obj1.has("status")
                                        && obj1.getString("status")
                                        .equals("PENDING")) {
                                    list.add(new Transaction(obj1));
                                } else {
                                    list.add(new Transaction(obj1));
                                }
                            }
                            String url = Constants.baseURL + "/users/" + Constants.userName + "/debits?"
                                    + "access_token=" + Globals.msToken;
                            Rest.get(url, null, null, getDebitsResponseHandler);

                        } catch (JSONException e) {
                            showUIMessage("Error getting list of transactions. Please try again.");
                            e.printStackTrace();
                        }
                        break;

                    default:
                        Log.e(TAG, "response not understood");
                        showUIMessage(message);
                        break;
                }
            } else {
                showUIMessage("Cannot connect to server. Please try again.");
                getActivity().finish();
            }
        }
    };


    private Rest.httpResponseHandler getDebitsResponseHandler = new Rest.httpResponseHandler() {
        @Override
        public void handleResponse(final JSONObject response, final boolean error) {
            Log.d("getPendingTransactions", "Received response: " + error);
            if (!error) {
                int httpCode = 0;
                try {
                    httpCode = response.getInt("responseCode");
                } catch (JSONException e) {
                    e.printStackTrace();
                    showUIMessage("Invalid response from server, please try again.");
                    getActivity().finish();
                }

                String message = "error";
                String responseStr = "";
                try {
                    responseStr = response.getString("data");
                    JSONObject obj = new JSONObject(responseStr);
                    if (obj.has("message")) {
                        message = obj.getString("message");
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing JSON response");
                }

                switch (httpCode) {
                    case Constants.RESPONSE_500:
                        //500 = no transactions, do nothing
                        Log.d("history", "got 500 == no transactions");
                        break;

                    case Constants.RESPONSE_401:
                    case Constants.RESPONSE_404:
                        Log.e("history", "got 404 or 401, back to login");
                        showUIMessage("Cannot locate server");
//                        Intent intentApplication = new Intent(getActivity(),
//                               LoginActivity.class);
//                        startActivity(intentApplication);
                        getActivity().finish();
                        break;

                    case Constants.RESPONSE_200:
                        try {
                            JSONArray arrOut = new JSONArray(responseStr);

                            for (int i = 0; i < arrOut.length(); i++) {
                                JSONObject obj1 = arrOut.getJSONObject(i);
                                // a MONEY IN transaction is money i'm
                                // SENDING and is an input TO me
                                obj1.put("type", "sending");
                                if (obj1.has("status")
                                        && obj1.getString("status")
                                        .equals("PENDING")) {
                                    list.add(new Transaction(obj1));
                                } else {
                                    list.add(new Transaction(obj1));
                                }
                            }
                        } catch (JSONException e) {
                            showUIMessage("Error getting list of transactions. Please try again.");
                            e.printStackTrace();
                        }
                        break;

                    default:
                        Log.e(TAG, "response not understood");
                        showUIMessage(message);
                        break;
                }

                // Sort and display the collected transactions
                Log.d(TAG, "there are " + list.size() + " transactions");
                Collections.sort(list, Transaction.finalComparator(false));
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter = new PaymentSummaryAdapter(mContext, list, mInflater, approvalResponseHandler);
                        mHistoryLayout.setAdapter(mAdapter);
                        mAdapter.notifyDataSetChanged();
                        // Shut off the progress bar, we have data
                        spinner.setVisibility(View.GONE);

                    }
                });
            } else {
                showUIMessage("Cannot connect to server. Please try again.");
                getActivity().finish();
            }
        }
    };

}
