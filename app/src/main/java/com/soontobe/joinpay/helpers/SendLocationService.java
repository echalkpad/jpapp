package com.soontobe.joinpay.helpers;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.soontobe.joinpay.Constants;

import org.apache.http.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;

public class SendLocationService extends Service {
	LocationManager mLocationManager;

	public SendLocationService() {
	}

	@Override
	public void onCreate() {
		Log.d("location", "creating gps service");
		super.onCreate();
		initService();
	}

	@Override
	public void onDestroy() {
		mLocationManager.removeUpdates(mLocationListener);
		super.onDestroy();
	}

	protected void initService() {
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			Log.d("location", "gps is enabled");
		} else {
			Log.d("location", "gps is disabled");
			Toast.makeText(getApplicationContext(), "GPS is disabled. Location may be flaky", Toast.LENGTH_SHORT).show();
		}

		mLocationManager.requestLocationUpdates(1000L, 1.0f, new Criteria(), mLocationListener, null);
		Log.d("location", "Service Started");
	}

	public void updateLocation() {
		Log.d("location", "updating location");
		mLocationManager.requestSingleUpdate(mLocationManager.getBestProvider(new Criteria(), false), mLocationListener, null);

	}

	LocationListener mLocationListener = new LocationListener() {

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			Log.d("location", "status changed");
		}

		@Override
		public void onProviderEnabled(String provider) {
			Log.d("location", "provider enabled");
		}

		@Override
		public void onProviderDisabled(String provider) {
			Log.d("location", "provider disabled");
		}

		@Override
		public void onLocationChanged(Location location) {
			Log.d("location", "lat: " + location.getLatitude());
			Log.d("location", "long: " + location.getLongitude());
			JSONObject obj = new JSONObject();

			try {
				obj.put("latitude", "" + location.getLatitude());
				obj.put("longitude", "" + location.getLongitude());
			} catch (JSONException e) {
				e.printStackTrace();
			}

			JSONObject auth = new JSONObject();
			try {
				auth.put("type", "basic");
				auth.put("username", Constants.userName);
				auth.put("password", Constants.password);
				Rest.post(Constants.baseURL + "/currentLocation", auth, null, obj.toString(), mSendLocationResponseHandler);
			} catch (JSONException e) {

			}

			Toast.makeText(getApplicationContext(), "Lat: " + location.getLatitude() + "... Long: " + location.getLongitude(), Toast.LENGTH_SHORT).show();
		}
	};

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	public class LocalBinder extends Binder {
		public SendLocationService getService() {
			return SendLocationService.this;
		}
	}

	private final IBinder mBinder = new LocalBinder();

	private Rest.httpResponseHandler mSendLocationResponseHandler = new Rest.httpResponseHandler() {
		@Override
		public void handleResponse(final HttpResponse response, final boolean error) {
		}
	};
}