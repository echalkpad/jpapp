package com.soontobe.joinpay.widget;

import com.soontobe.joinpay.R;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;

/**
 * Payment notification builder
 *
 */


public class PaymentNotification {

	@SuppressLint("NewApi")
	public Notification getBasicNotification(Context context) {
		Notification notification = new Notification.Builder(context)
				.setSmallIcon(R.drawable.ic_launcher).setAutoCancel(true)
				.setContentTitle("Joinpay: new push message...").build();
		return notification;
	}

	@SuppressLint("NewApi")
	public Notification getPaymentNotification(Context context,
			NotificationObj notiObj, PendingIntent pIntent) {
		Notification notification = new Notification.Builder(context)
				.setSmallIcon(R.drawable.ic_launcher).setAutoCancel(true)
				.setContentTitle("Joinpay: new push message...")
				.setContentText(notiObj.toString()).setContentIntent(pIntent)
				.build();
		return notification;
	}

	public static class NotificationObj {
		public NotificationObj() {

		}

		private int noticeType; // 0-Request of payment, 1-Notification of
								// receving money
		private String sourceUserName;
		private float amountOfMoney;

		public int getNoticeType() {
			return noticeType;
		}

		public void setNoticeType(int noticeType) {
			this.noticeType = noticeType;
		}

		public String getSourceUserName() {
			return sourceUserName;
		}

		public void setSourceUserName(String sourceUserName) {
			this.sourceUserName = sourceUserName;
		}

		public float getAmountOfMoney() {
			return amountOfMoney;
		}

		public void setAmountOfMoney(float amountOfMoney) {
			this.amountOfMoney = amountOfMoney;
		}

		@Override
		public String toString() {
			String str = sourceUserName;
			if (noticeType == 0) {
				str += " requests a payment amount of $";
			} else {
				str += " pays you $";
			}
			str += amountOfMoney;
			return str;
		}
	}
}
