package com.soontobe.joinpay.fragment;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.soontobe.joinpay.Constants;
import com.soontobe.joinpay.LoginActivity;
import com.soontobe.joinpay.PaymentSummaryAdapter;
import com.soontobe.joinpay.R;
import com.soontobe.joinpay.RESTCalls;
import com.soontobe.joinpay.model.Transaction;

import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;

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

    @Override
    public final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        // Don't forget this class is a registered receiver.
        getActivity().unregisterReceiver(restResponseReceiver);
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
                mTransactions, inflater);
        mHistoryLayout.setAdapter(mAdapter);

        // Configure activity to only receive transaction history updates
        // Transaction updates are REST responses
        IntentFilter restIntentFilter = new IntentFilter(Constants.RESTRESP);
        getActivity().registerReceiver(restResponseReceiver, restIntentFilter);

        // Update the transaction history
        checkPendingInfo();

        return mCurrentView;
    }

    /**
     * This handles intents that are fired off elsewhere in the application,
     * specifically those that relate to collecting transactions.
     */
    private BroadcastReceiver restResponseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            String receivedServiceContext = intent.getStringExtra("context");

            /////////////////////////////////////////////////
            ///////////  Dialog Response Receiver ///////////
            /////////////////////////////////////////////////
            if (receivedServiceContext.equals("approveTransaction")) {
                String response = intent.getStringExtra("response");
                int httpCode = intent.getIntExtra("code", 0);
                String message = "error";
                Log.d("dialog", "got code: " + httpCode);
                try {
                    JSONObject obj = new JSONObject(response);
                    if (obj.has("message")) {
                        message = obj.getString("message");
                    }
                } catch (JSONException e) {
                    Log.e("dialog", "Error parsing JSON response");
                }

                //// Http Codes ////
                if (httpCode == HttpStatus.SC_BAD_REQUEST
                        || httpCode == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
                    // 400/500 = error, show msg
                    Log.d("dialog", "got " + httpCode + ", showing message");
                    Toast.makeText(getActivity(), message,
                            Toast.LENGTH_LONG).show();
                } else if (httpCode == HttpStatus.SC_OK) {
                    // 200 = all good
                    Toast.makeText(getActivity(), "Success",
                            Toast.LENGTH_SHORT).show();
                    checkPendingInfo(); // UI update
                } else {
                    // ??? = error, do nothing
                    Log.e("dialog", "response not understoood");
                    Toast.makeText(getActivity(), message,
                            Toast.LENGTH_LONG).show();
                }
            }

            ////////////////////////////////////////////////////
            /////////// Getting Transaction Receiver ///////////
            ////////////////////////////////////////////////////
            if (serviceContext.equals(receivedServiceContext)) {
                String response = intent.getStringExtra("response");
                int httpCode = intent.getIntExtra("code", 0);
                ArrayList<Transaction> list = new ArrayList<>();
                String message = "error";
                try {
                    JSONObject obj = new JSONObject(response);
                    if (obj.has("message")) {
                        message = obj.getString("message");
                    }
                } catch (JSONException e) {
                    Log.e("dialog", "Error parsing JSON response");
                }

                //// Http Codes ////
                if (httpCode == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
                    //500 = no transactions, do nothing
                    Log.d("history", "got 500 == no transactions");
                } else if (httpCode == HttpStatus.SC_NOT_FOUND
                        || httpCode == HttpStatus.SC_UNAUTHORIZED) {
                    Log.e("history", "got 404 or 401, back to login");
                    Toast.makeText(getActivity(), "Cannot locate server",
                            Toast.LENGTH_LONG).show();
                    Intent intentApplication = new Intent(getActivity(),
                            LoginActivity.class);
                    startActivity(intentApplication);
                    getActivity().finish();
                } else if (httpCode == HttpStatus.SC_OK) {
                    // 200 = parse the response
                    try {
                        JSONObject obj = new JSONObject(response);

                        if (obj.has("moneyIn")) {
                            JSONArray arrIn = obj.getJSONArray("moneyIn");

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
                        }

                        if (obj.has("moneyOut")) {
                            JSONArray arrOut = obj.getJSONArray("moneyOut");

                            for (int i = 0; i < arrOut.length(); i++) {
                                JSONObject obj1 = arrOut.getJSONObject(i);
                                // a MONEY OUT transaction is money
                                // I'm SENDING and is an output FROM me
                                obj1.put("type", "sending");

                                if (obj1.has("status")
                                        && obj1.getString("status")
                                        .equals("PENDING")) {
                                    list.add(new Transaction(obj1));
                                } else {
                                    list.add(new Transaction(obj1));
                                }
                            }
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing JSON response");
                        Toast.makeText(getActivity(), "Problem with server, "
                                + "try again later", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // ??? = error, do nothing
                    Log.e(TAG, "response not understood");
                    Toast.makeText(getActivity(), message,
                            Toast.LENGTH_LONG).show();
                }

                // Sort and display the collected transactions
                Log.d(TAG, "there are " + list.size() + " transactions");
                Collections.sort(list, Transaction.finalComparator(false));
                mAdapter = new PaymentSummaryAdapter(context, list, mInflater);
                mHistoryLayout.setAdapter(mAdapter);
                mAdapter.notifyDataSetChanged();
                // Shut off the progress bar, we have data
                spinner.setVisibility(View.GONE);
            }
        }
    };

    /**
     * Submits an Intent which collects the user's transaction
     * history from the JoinPay APIs.
     */
    private void checkPendingInfo() {
        Intent intent = new Intent(getActivity().getApplicationContext(),
                RESTCalls.class);
        String url = Constants.baseURL + "/transactions";
        intent.putExtra("method", "get");
        intent.putExtra("url", url);
        intent.putExtra("context", serviceContext);
        Log.d("transBuilder", "getting pending transactions");
        getActivity().startService(intent);
    }

    @Override
    public final void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated to
     * the activity and potentially other fragments contained in that activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/
     * basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {

        /**
         * Unused fragment interaction handler.
         * @param uri Doesn't matter.
         */
        void onFragmentInteraction(Uri uri);
    }
}
