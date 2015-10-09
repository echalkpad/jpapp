package com.soontobe.joinpay.fragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.soontobe.joinpay.Constants;
import com.soontobe.joinpay.LoginActivity;
import com.soontobe.joinpay.PaymentSummaryAdapter;
import com.soontobe.joinpay.R;
import com.soontobe.joinpay.RESTCalls;
import com.soontobe.joinpay.Transaction;
import com.soontobe.joinpay.widget.PendingTransactionItemView;
import com.soontobe.joinpay.widget.PendingTransactionItemView.OnAcceptButtonClickListener;
import com.soontobe.joinpay.widget.PendingTransactionItemView.OnDeclineButtonClickListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * This Fragment displays a list of user transactions.
 */
public class HistoryFragment extends Fragment {
	final String serviceContext = "HistoryFragment";

    // For logging
    private static final String TAG = "history";

	private View mCurrentView;
	private LayoutInflater mInflater;

    // What we use to show the transaction history to the user;
    private ListView mHistoryLayout;
    private PaymentSummaryAdapter mAdapter; // For beautifying transactions
    private ArrayList<Transaction> mTransactions; // The transactions themselves
    private ProgressBar spinner; // So we can turn the thing on and off during requests

	public HistoryFragment() {
		// Required empty public constructor
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@SuppressLint("NewApi")
	@Override
	public void setUserVisibleHint(boolean isVisibleToUser) {
		// TODO Auto-generated method stub
		super.setUserVisibleHint(isVisibleToUser);
	}

	@Override
	public void onResume() {
		super.onResume();
	}
	
	@Override
	public void onDestroy() {
		Log.d(TAG, "destroying history fragment");
	    getActivity().unregisterReceiver(restResponseReceiver);		//remove the receiver
		super.onDestroy();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(TAG, "creating history view");
		if(mCurrentView == null) mCurrentView = inflater.inflate(R.layout.fragment_history, container, false);
		
		ViewGroup parent = (ViewGroup) mCurrentView.getParent(); 
		if(parent != null){
			parent.removeView(mCurrentView);
		}

		mInflater = inflater;

        // Grab the loading spinner so we can turn it off
        spinner = (ProgressBar) mCurrentView.findViewById(R.id.hist_prog_bar);
        spinner.setVisibility(View.VISIBLE); // Start the spinner until data is acquired

        // Setup transaction list to receive new transactions
		mHistoryLayout = (ListView)mCurrentView.findViewById(R.id.trans_list);
        mTransactions = new ArrayList<>();
        mAdapter = new PaymentSummaryAdapter(inflater.getContext(), mTransactions, inflater);
		mHistoryLayout.setAdapter(mAdapter);

        // Configure activity to only receive transaction history updates
		IntentFilter restIntentFilter = new IntentFilter(Constants.RESTRESP); // Transaction updates are REST responses
		getActivity().registerReceiver(restResponseReceiver, restIntentFilter);

        // Update the transaction history
        checkPendingInfo();

		return mCurrentView;
	}

    /**
     * This handles intents that are fired off elsewhere in the application, specifically those
     * that relate to collecting transactions
     */
	BroadcastReceiver restResponseReceiver = new BroadcastReceiver() {
		// TODO clean this HORRIBLE MESS, THISCODEHASNOHONOR
		@Override
		public void onReceive(Context context, Intent intent) {
			String receivedServiceContext = intent.getStringExtra("context");
			
			/////////////////////////////////////////////////
			///////////  Dialog Response Receiver ///////////
			/////////////////////////////////////////////////
			if(receivedServiceContext.equals("approveTransaction")){
				String response = intent.getStringExtra("response");
				int httpCode = intent.getIntExtra("code", 0);
				String message = "error";
				Log.d("dialog", "got code: " + httpCode);
				try {
					JSONObject obj = new JSONObject(response);
					if(obj.has("message")) message = obj.getString("message");
				}
				catch (JSONException e) {
					Log.e("dialog", "Error parsing JSON response");
				}
				
				//// Http Codes ////
				if(httpCode == 400 | httpCode == 500){							//400/500 = error, show msg
					Log.d("dialog", "got " + httpCode + ", showing message");					
					Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
				}
				else if(httpCode == 200){										//200 = all good
					Toast.makeText(getActivity(), "Success", Toast.LENGTH_SHORT).show();
					checkPendingInfo(); 										//UI update
				}
				else{															//??? = error, do nothing
					Log.e("dialog", "response not understoood");
					Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
				}
			}
			
			////////////////////////////////////////////////////
			/////////// Getting Transaction Receiver ///////////
			////////////////////////////////////////////////////
			if(serviceContext.equals(receivedServiceContext)) {
				String response = intent.getStringExtra("response");
				int httpCode = intent.getIntExtra("code", 0);
				ArrayList<Transaction> list = new ArrayList<>();
				String message = "error";
				try {
					JSONObject obj = new JSONObject(response);
					if(obj.has("message")) message = obj.getString("message");
				}
				catch (JSONException e) {
					Log.e("dialog", "Error parsing JSON response");
				}
				
				//// Http Codes ////
				if(httpCode == 500){											//500 = no transactions, do nothing
					Log.d("history", "got 500 == no transactions");
				}
				else if(httpCode == 404 || httpCode == 401){
					Log.e("history", "got 404 or 401, back to login");
					Toast.makeText(getActivity(), "Cannot locate server", Toast.LENGTH_LONG).show();
					Intent intentApplication = new Intent(getActivity(), LoginActivity.class);
					startActivity(intentApplication);
					getActivity().finish();					
				}
				else if(httpCode == 200){										//200 = parse the response
					try {
						JSONObject obj = new JSONObject(response);
						
						if(obj.has("moneyIn")){
							JSONArray arrIn = obj.getJSONArray("moneyIn");
							
							for(int i = 0; i < arrIn.length(); i++) {
								JSONObject obj1 = arrIn.getJSONObject(i);
								obj1.put("type","requesting");					//a MONEY IN transaction is money i'm SENDING and is an input TO me
								if(obj1.has("status") && obj1.getString("status").equals("PENDING")){
									list.add(new Transaction(obj1));
								}
								else list.add(new Transaction(obj1));						//list.add(obj1);
							}
						}
						
						if(obj.has("moneyOut")){
							JSONArray arrOut = obj.getJSONArray("moneyOut");
							
							for(int i = 0; i < arrOut.length(); i++) {
								JSONObject obj1 = arrOut.getJSONObject(i);
								obj1.put("type","sending");						//a MONEY OUT transaction is money i'm SENDING and is an output FROM me
								//list.add(obj1);
								if(obj1.has("status") && obj1.getString("status").equals("PENDING")){
									list.add(new Transaction(obj1));
								}
								else list.add(new Transaction(obj1));
							}
						}
					}
					catch (JSONException e) {
						Log.e(TAG, "Error parsing JSON response");
						Toast.makeText(getActivity(), "Problem with server, try again later", Toast.LENGTH_SHORT).show();
					}
				}
				else{															//??? = error, do nothing
					Log.e(TAG, "response not understood");
					Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
				}

				Log.d(TAG, "there are " + list.size() + " transactions");
                Collections.sort(list, Transaction.dateComparator(false));
                mAdapter = new PaymentSummaryAdapter(context, list, mInflater);
                mHistoryLayout.setAdapter(mAdapter);
                mAdapter.notifyDataSetChanged();
                spinner.setVisibility(View.GONE);  // Shut off the progress bar, we have data
			}
		}
	};

    /**
     * Submits an Intent which collects the user's transaction history from the JoinPay APIs.
     */
	private void checkPendingInfo() {
		Intent intent = new Intent(getActivity().getApplicationContext(), RESTCalls.class);
		String url = Constants.baseURL + "/transactions";
		intent.putExtra("method","get");
		intent.putExtra("url", url);
		intent.putExtra("context", serviceContext);
		Log.d("transBuilder", "getting pending transactions");
		getActivity().startService(intent);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

    /**
	 * This interface must be implemented by activities that contain this
	 * fragment to allow an interaction in this fragment to be communicated to
	 * the activity and potentially other fragments contained in that activity.
	 * <p>
	 * See the Android Training lesson <a href=
	 * "http://developer.android.com/training/basics/fragments/communicating.html"
	 * >Communicating with Other Fragments</a> for more information.
	 */
	public interface OnFragmentInteractionListener {
		// TODO: Update argument type and name
		public void onFragmentInteraction(Uri uri);
	}
}
