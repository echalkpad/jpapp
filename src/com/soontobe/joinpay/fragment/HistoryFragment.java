package com.soontobe.joinpay.fragment;

import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnClickListener;
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
import com.soontobe.joinpay.Constants;
import com.soontobe.joinpay.PaymentSummaryAdapter;
import com.soontobe.joinpay.R;
import com.soontobe.joinpay.RESTCalls;
import com.soontobe.joinpay.widget.PendingTransactionItemView;
import com.soontobe.joinpay.widget.PendingTransactionItemView.OnAcceptButtonClickListener;
import com.soontobe.joinpay.widget.PendingTransactionItemView.OnDeclineButtonClickListener;

/**
 * It is one of the three fragments in the radar view activity. It shows a readable list of transaction records.
 */
public class HistoryFragment extends Fragment implements LoaderCallbacks<Void> {
	final String serviceContext = "HistoryFragment";
	//private OnFragmentInteractionListener mListener;
	private View mCurrentView;
	private ArrayList<ArrayList<String[]>> paymentInfoList;
	private ListView mHistoryLayout;
	private ArrayList<PendingTransactionItemView> mPendingTIVList;
	private ArrayList<ArrayList<String []>> mPendingInfoList;
	private ArrayList<Integer> mPendingInfoTypeList;	//0-Transaction, 1-Notification
	public CheckViewUpdateAsyncTask mAsyncTask = null;
	private static final int COMPLETED = 0;
	
	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			if(msg.what == COMPLETED) {
				try{
					Log.d("history", "checking pending info");
					checkPendingInfo(); 						//UI update
				} catch (Exception e){
					Log.e("history", "error with checking pending info");
					e.printStackTrace();
				}
			}
			else Log.d("history", "msg is not complete");
		}
	};

	public HistoryFragment() {
		// Required empty public constructor
		paymentInfoList = new ArrayList<ArrayList<String[]>>();
		mPendingInfoList = new ArrayList<ArrayList<String[]>>();
		mPendingTIVList = new ArrayList<PendingTransactionItemView>();
		mPendingInfoTypeList = new ArrayList<Integer>();
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
		Log.d("history", "destroying history fragment");
		if(mAsyncTask != null){
			mAsyncTask.cancel(true);
			mAsyncTask = null;
		}
	    getActivity().unregisterReceiver(restResponseReceiver);		//remove the receiver
		super.onDestroy();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d("history", "creating history view");
		if(mCurrentView == null) mCurrentView = inflater.inflate(R.layout.fragment_history, container, false);
		
		ViewGroup parent = (ViewGroup) mCurrentView.getParent(); 
		if(parent != null){
			parent.removeView(mCurrentView);
		}
		
		mHistoryLayout = (ListView)mCurrentView.findViewById(android.R.id.list);
		if(mAsyncTask == null){
			Log.d("history", "mAsyncTask is null");
			mAsyncTask = new CheckViewUpdateAsyncTask();
			mAsyncTask.execute();
		}
		else Log.d("history", "mAsyncTask is NOT null");
		
		IntentFilter restIntentFilter = new IntentFilter(Constants.RESTRESP);
		getActivity().registerReceiver(restResponseReceiver, restIntentFilter);
		return mCurrentView;
	}
	
	BroadcastReceiver restResponseReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String receivedServiceContext = intent.getStringExtra("context");
			
			if(serviceContext.equals(receivedServiceContext)) {
				//String url = intent.getStringExtra("url");
				//String method = intent.getStringExtra("method");
				String response = intent.getStringExtra("response");
				int httpCode = intent.getIntExtra("code", 0);
				ArrayList<JSONObject> list = new ArrayList<JSONObject>();
				
				if(httpCode == 500){
					Log.d("history", "got 500 == no transacations");
				}
				else if(httpCode == 200){
					try {
						JSONObject obj = new JSONObject(response);
						
						if(obj.has("moneyIn")){
							JSONArray arrIn = obj.getJSONArray("moneyIn");
							
							for(int i = 0; i < arrIn.length(); i++) {
								JSONObject obj1 = arrIn.getJSONObject(i);
								obj1.put("type","requesting");			//a MONEY IN transaction is money i'm REQUESTING and is an input TO me
								list.add(obj1);
							}
						}
						
						if(obj.has("moneyOut")){
							JSONArray arrOut = obj.getJSONArray("moneyOut");
							
							for(int i = 0; i < arrOut.length(); i++) {
								JSONObject obj1 = arrOut.getJSONObject(i);
								obj1.put("type","sending");				//a MONEY OUT transaction is money i'm SENDING and is an output FROM me
								list.add(obj1);
							}
						}
					}
					catch (JSONException e) {
						Log.e("history", "Error parsing JSON response");
					}
				}
				else{
					Log.e("history", "response not understoood");
				}
				addTransaction(list);
			}
		}
	};
	
	private void checkPendingInfo() {
		Intent intent = new Intent(getActivity().getApplicationContext(), RESTCalls.class);
		String url = Constants.baseURL + "/transactions";
		intent.putExtra("method","get");
		intent.putExtra("url", url);
		intent.putExtra("context", serviceContext);
		Log.d("transBuilder", "getting pending transactions");
		getActivity().startService(intent);
	}
	
	public void addTransaction(ArrayList<JSONObject> obj){		
		if(obj.size() > 0){
			Log.d("transBuilder", "adding transaction");
			PaymentSummaryAdapter adapter = new PaymentSummaryAdapter(getActivity(), obj, true);
			mHistoryLayout.setAdapter(adapter);
		}
		else{
			ArrayList<String> itemsList = new ArrayList<String> ();
            itemsList.add("No transactions yet!") ;
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), R.layout.confirm_page_item_none, R.id.activity_confirm_pay_text, itemsList);
			mHistoryLayout.setAdapter(adapter);
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}
	
	/**
	 * Add a transaction note to history view
	 * @param info
	 */
	public void addTransactionNoteItem(ArrayList<String[]> info){
		PendingTransactionItemView pItemView = new PendingTransactionItemView(getActivity());
		int index = mPendingTIVList.size();
		pItemView.setPaymentInfo(info);
		pItemView.setAcceptButtonClickListener(new OnPendingItemAcceptedListener(index));
		pItemView.setDeclineButtonClickListener(new OnPendingItemDeclinedListener(index));
		mPendingTIVList.add(pItemView);
	}
	
	/**
	 * Add pending transaction info to list
	 * @param info
	 */
	public void addPendingTransItem(ArrayList<String[]> info){
		mPendingInfoList.add(info);
		mPendingInfoTypeList.add(1);
	}
	/*@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (OnFragmentInteractionListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement OnFragmentInteractionListener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
	}*/

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

	public void setNewRecordNotification(ArrayList<String []> newPaymentInfo) {
		paymentInfoList.add(newPaymentInfo);
		mPendingInfoTypeList.add(0);
	}
	
	private class OnPendingItemAcceptedListener implements OnAcceptButtonClickListener{
		private int index;
		
		public OnPendingItemAcceptedListener(int _index){
			index = _index;
		}
		
		@Override
		public void OnClick(View v) {			
			String confirmMsg = "Confirm the payment?";
			new AlertDialog.Builder(getActivity())
				.setMessage(confirmMsg)
				.setPositiveButton("OK", new OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mPendingTIVList.get(index).setAccepted();
					}
				})
				.setNegativeButton("Don\'t allow", new OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						return;
					}
				})
				.show();
		}
	}
	
	private class OnPendingItemDeclinedListener implements OnDeclineButtonClickListener{
		private int index;
		
		public OnPendingItemDeclinedListener(int _index){
			index = _index;
		}
		
		@Override
		public void OnClick(View v) {
			mPendingTIVList.get(index).setDeclined();
		}
	}
	
	/**
	 * Check for ui change every 5 seconds
	 */
	private class CheckViewUpdateAsyncTask extends AsyncTask<Void, Void, Void>{
		
		@Override
		protected Void doInBackground(Void... params) {
			Log.d("history", "STARTED history");
			while(true){				
				try {
					Log.d("history", "running new history task");
					Message msg = new Message();
					msg.what = COMPLETED;
					mHandler.sendMessage(msg);
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					Log.d("history", "STOPPED history");			//not really an error, the stop method might interrupt it
					//e.printStackTrace();
					break;
				}
			}
			return null;
		}
	}
}
