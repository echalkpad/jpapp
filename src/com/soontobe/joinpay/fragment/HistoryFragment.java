package com.soontobe.joinpay.fragment;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.soontobe.joinpay.Constants;
import com.soontobe.joinpay.MainActivity;
import com.soontobe.joinpay.PaymentSummaryAdapter;
import com.soontobe.joinpay.R;
import com.soontobe.joinpay.RESTCalls;
import com.soontobe.joinpay.Utility;
import com.soontobe.joinpay.widget.PendingTransactionItemView;
import com.soontobe.joinpay.widget.PendingTransactionItemView.OnAcceptButtonClickListener;
import com.soontobe.joinpay.widget.PendingTransactionItemView.OnDeclineButtonClickListener;

/**
 * It is one of the three fragments in the radar view activity. It shows a readable list of transaction records.
 */
public class HistoryFragment extends Fragment 
implements LoaderCallbacks<Void> {

	final String serviceContext = "HistoryFragment";
	private OnFragmentInteractionListener mListener;
	private View mCurrentView;
	private ArrayList<ArrayList<String[]>> paymentInfoList;
	private LinearLayout mHistoryLayout;
	private ArrayList<PendingTransactionItemView> mPendingTIVList;
	
	private ArrayList<ArrayList<String []>> mPendingInfoList;
	private ArrayList<Integer> mPendingInfoTypeList;	//0-Transaction, 1-Notification
	private boolean mShouldAsyncTaskStop = false;
	private boolean isViewAvailable = false;
	private CheckViewUpdateAsyncTask mAsyncTask = null;
	
	private static final int COMPLETED = 0;
	private Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			if(msg.what == COMPLETED) {
				if(getActivity() == null)
					return;	//Fragment is not available
				
				try{
					checkPendingInfo(); //UI update
				} catch (Exception e){
					//mShouldAsyncTaskStop = true; //If an error occurs stop the AsyncTask.
				}
				
			}
		}
	};

	public HistoryFragment() {
		// Required empty public constructor
		//newRecordAvailable = false;
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
//		if(isVisibleToUser)
//			checkPaymentInfo();
	}

	@Override
	public void onResume() {
		isViewAvailable = true;
		super.onResume();
	}
	
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		//mShouldAsyncTaskStop = true;
		super.onDestroy();
	}

	@Override
	public void onPause() {
		isViewAvailable = false;
		super.onPause();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		if(mCurrentView == null)
			mCurrentView = inflater.inflate(R.layout.fragment_history, container, false);
		
		ViewGroup parent = (ViewGroup) mCurrentView.getParent(); 
		if(parent != null){
			parent.removeView(mCurrentView);
		}
		
		mHistoryLayout = (LinearLayout)mCurrentView.findViewById(R.id.history_view_pane_items);
		
		//checkPaymentInfo();
		if(mAsyncTask == null){
			mAsyncTask = new CheckViewUpdateAsyncTask();
			mAsyncTask.execute();
		}
		
		IntentFilter restIntentFilter = new IntentFilter(Constants.RESTRESP);
		getActivity().registerReceiver(restResponseReceiver, restIntentFilter);

		return mCurrentView;
	}
	
	BroadcastReceiver restResponseReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String receivedServiceContext = intent.getStringExtra("context");
			
			if(serviceContext.equals(receivedServiceContext)) {
				String url = intent.getStringExtra("url");
				String method = intent.getStringExtra("method");
				String response = intent.getStringExtra("response");
				ArrayList<JSONObject> list = new ArrayList<JSONObject>();

				try {
					JSONObject obj = new JSONObject(response);
					JSONArray arrIn = obj.getJSONArray("moneyIn");
					
					for(int i = 0; i < arrIn.length(); i++) {
						JSONObject obj1 = arrIn.getJSONObject(i);
						obj1.put("type","requesting");			//a MONEY IN transaction is money i'm REQUESTING and is an input TO me
						list.add(obj1);
					}



				} catch (JSONException e) {
					Log.e("response", response);
					Log.e("HistoryFragment REST", "Error parsing JSON response moneyIn");
				}
				

				
				try {
					JSONObject obj = new JSONObject(response);

					JSONArray arrOut = obj.getJSONArray("moneyOut");
					
					for(int i = 0; i < arrOut.length(); i++) {
						JSONObject obj1 = arrOut.getJSONObject(i);
						obj1.put("type","sending");		//a MONEY OUT transaction is money i'm SENDING and is an output FROM me
						list.add(obj1);
					}
					


				} catch (JSONException e) {
					Log.e("response", response);
					Log.e("HistoryFragment REST", "Error parsing JSON response moneyOut");
					
				}
				
				addTransaction(list);
			}
		}
	};
	
	private void checkPendingInfo() {
		Intent intent = new Intent(getActivity().getApplicationContext(), RESTCalls.class);
		String url = Constants.baseURL + "/transactions";
		intent.putExtra("method","get");
		intent.putExtra("url",url);
		intent.putExtra("context", serviceContext);
		
		

		Log.d("checkPendingInfo", "starting Service");
		getActivity().startService(intent);
		Log.d("checkPendingInfo", "started Service");

	
/*		if(mPendingInfoTypeList.isEmpty()){
			return;
		}
		int pendingSize = mPendingInfoTypeList.size();
		int transCount = 0;
		int notiCount = 0;
		for(int i=0; i<pendingSize; i++){
			int pType = mPendingInfoTypeList.get(i);
			if(pType == 0){
				//transaction
				addTransaction(paymentInfoList.get(transCount++));
			} else {
				//notification
				addTransactionNoteItem(mPendingInfoList.get(notiCount++));
			}
		}
		mPendingInfoTypeList.clear();
		mPendingInfoList.clear();
		paymentInfoList.clear();
		*/
	}
	
	public void addTransaction(ArrayList<JSONObject> obj){
		int margin = 15;
		ListView lv;
		LinearLayout.MarginLayoutParams mlp;
		
		lv = new ListView(getActivity());
		PaymentSummaryAdapter adapter = new PaymentSummaryAdapter(getActivity(), obj, true);
		lv.setAdapter(adapter);
		lv.setBackgroundColor(Color.rgb(0xff, 0xff, 0xff)); 
		mHistoryLayout.removeAllViews();
		mHistoryLayout.addView(lv, 0);
		Utility.setListViewHeightBasedOnChildren(lv);
		mlp = (LinearLayout.MarginLayoutParams) lv.getLayoutParams(); 
		mlp.setMargins(0, margin, 0, margin);

	}
/*	public void addTransaction(ArrayList<String[]> info){
		int margin = 15;
		ListView lv;
		LinearLayout.MarginLayoutParams mlp;
		
		lv = new ListView(getActivity());
		PaymentSummaryAdapter adapter = new PaymentSummaryAdapter(getActivity(), info, true);
		lv.setAdapter(adapter);
		lv.setBackgroundColor(Color.rgb(0xff, 0xff, 0xff));
		mHistoryLayout.addView(lv, 0);
		Utility.setListViewHeightBasedOnChildren(lv);
		mlp = (LinearLayout.MarginLayoutParams) lv.getLayoutParams();
		mlp.setMargins(0, margin, 0, margin);
	}*/
	
/*	public void checkPaymentInfo(){
		//LinearLayout historyItems = (LinearLayout) getActivity().findViewById(R.id.history_view_pane_items);
		int margin = 15;
		ListView lv;
		LinearLayout.MarginLayoutParams mlp;
		for (int i = 0;i < paymentInfoList.size();i++) {
			lv = new ListView(getActivity());
			PaymentSummaryAdapter adapter = new PaymentSummaryAdapter(getActivity(), paymentInfoList.get(i), true);
			lv.setAdapter(adapter);
			lv.setBackgroundColor(Color.rgb(0xff, 0xff, 0xff));
			mHistoryLayout.addView(lv, 0);
			Utility.setListViewHeightBasedOnChildren(lv);
			mlp = (LinearLayout.MarginLayoutParams) lv.getLayoutParams();
			mlp.setMargins(0, margin, 0, margin);
		}
	}
*/
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
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
		
		mHistoryLayout.addView(pItemView, 0);
		
		LinearLayout.MarginLayoutParams mlp = (LinearLayout.MarginLayoutParams) pItemView.getLayoutParams();
		mlp.setMargins(0, 15, 0, 15);
	}
	
	/**
	 * Add pending transaction info to list
	 * @param info
	 */
	public void addPendingTransItem(ArrayList<String[]> info){
		mPendingInfoList.add(info);
		mPendingInfoTypeList.add(1);
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
			// TODO Auto-generated method stub
			
			String confirmMsg = "Confirm the payment?";
			new AlertDialog.Builder(getActivity())
				.setMessage(confirmMsg)
				.setPositiveButton("OK", new OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						mPendingTIVList.get(index).setAccepted();
					}
				})
				.setNegativeButton("Don\'t allow", new OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
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
			// TODO Auto-generated method stub
			mPendingTIVList.get(index).setDeclined();
		}
	}
	
	/**
	 * Check for ui change every 5 seconds
	 */
	private class CheckViewUpdateAsyncTask extends AsyncTask<Void, Void, Void>{
		
		@Override
		protected Void doInBackground(Void... params) {
			Log.d("AsyncTask", "STARTED");
			while(true){
				if(mShouldAsyncTaskStop){
					Log.d("AsyncTask", "STOPPED");
					break;
				}
				
				try {
					Message msg = new Message();
					msg.what = COMPLETED;
					mHandler.sendMessage(msg);
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return null;
		}
	}
}
