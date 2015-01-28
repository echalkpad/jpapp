package com.soontobe.joinpay;

import com.soontobe.joinpay.widget.PaymentNotification;
import com.soontobe.joinpay.widget.PaymentNotification.NotificationObj;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class MessageRetrievalService extends Service {
	private final String TAG = "MessageRetrievalService";
	private PendingWorkThread mWorkThred;
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO something useful
		Log.d(TAG, "onStartCommand() executed"); 
		mWorkThred = new PendingWorkThread();
		mWorkThred.start();
		
		return super.onStartCommand(intent, flags, startId);
	}
	
	
	public NotificationObj getTestNotiObj(){
		NotificationObj notiObj = new NotificationObj();
		notiObj.setAmountOfMoney(50.6f);
		notiObj.setNoticeType(0);
		notiObj.setSourceUserName("ANONYMOUS");
		return notiObj;
	}
	
	public void showNotification(NotificationObj notiObj){
		PaymentNotification payNoti;
		
		NotificationManager notiMgr = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		
		Intent recvIntent = new Intent(this, RadarViewActivity.class);
		recvIntent.putExtra(RadarViewActivity.JUMP_KEY, RadarViewActivity.historyRequestCode);
		PendingIntent pIntent = PendingIntent.getActivity(this, 0, recvIntent, 0);
		

		payNoti = new PaymentNotification();
		Notification noti = payNoti.getPaymentNotification(getApplicationContext(), notiObj, pIntent);
		notiMgr.notify((int) System.currentTimeMillis(), noti);
	}

	


	@Override
	public void onCreate() {
		Log.d(TAG, "onCreate");
		super.onCreate();
	}




	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		mWorkThred.stopMe();
		Log.d(TAG, "onDestroy() executed"); 
	}




	@Override
	public IBinder onBind(Intent arg0) {
		
		return null;
	}
	
	
	private class PendingWorkThread extends Thread {
		private boolean shouldStop;
		public void stopMe(){
			shouldStop = true;
		}
		
		
		
		public PendingWorkThread() {
			super();
			shouldStop = false;
		}



		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				for(int i=0; i<10; i++){
					if(shouldStop)
						return;
					Thread.sleep(3000);
					showNotification(getTestNotiObj());
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
	}
}
