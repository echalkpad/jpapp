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

import org.json.JSONException;
import org.json.JSONObject;

public class SendLocation extends Service {
	final String serviceIntent = "SendLocation";
	LocationManager mLocationManager;
	static Context mApplicationContext;

	public SendLocation() {
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
			
			Intent intent = new Intent(getApplicationContext(), RESTCalls.class);
			String url = Constants.baseURL + "/currentLocation";
			intent.putExtra("method","put");
			intent.putExtra("url",url);
			intent.putExtra("body", obj.toString());
			intent.putExtra("context", serviceIntent);

			startService(intent);
			Toast.makeText(getApplicationContext(), "Lat: " +  location.getLatitude() + "... Long: " + location.getLongitude(), Toast.LENGTH_SHORT).show();
		}
	};

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	public class LocalBinder extends Binder {
		SendLocation getService() {
			return SendLocation.this;
		}
	}

	private final IBinder mBinder = new LocalBinder();

}
