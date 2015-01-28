package com.soontobe.joinpay;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class SendLocation extends Service {

	final String serviceIntent = "SendLocation";
	LocationManager mLocationManager;

	static Context mApplicationContext;
	
	public SendLocation() {
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		initService();
	}
	
	protected void initService() {
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		if(mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
//			mProvider = LocationManager.GPS_PROVIDER;
		} else { 
//			Toast.makeText(getApplicationContext(), "GPS is disabled. Using Network for location.", Toast.LENGTH_SHORT).show();
//			mProvider = LocationManager.NETWORK_PROVIDER;
		}

		mLocationManager.requestLocationUpdates(1000L, 1.0f, new Criteria(), mLocationListener, null);
		Log.d("Location", "Service Started");
	}

	LocationListener mLocationListener = new LocationListener() {
		
		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void onLocationChanged(Location location) {
			Log.d("Location", "Got updated Location");
			Log.d("Latitude", "" + location.getLatitude());
			Log.d("Longitude", "" + location.getLongitude());
			
			JSONObject obj = new JSONObject();
			
			try {
				obj.put("latitude", "" + location.getLatitude());
				obj.put("longitude", "" + location.getLongitude());
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Intent intent = new Intent(getApplicationContext(), RESTCalls.class);
			
			String url = Constants.baseURL + "/currentLocation";
			intent.putExtra("method","put");
			intent.putExtra("url",url);
			intent.putExtra("body", obj.toString());
			intent.putExtra("context", serviceIntent);

			Log.d("loginClicked", "starting Service");
			startService(intent);
			Log.d("loginClicked", "started Service");

			Toast.makeText(getApplicationContext(), "Lat: " +  location.getLatitude() + "... Long: " + location.getLongitude(), Toast.LENGTH_SHORT).show();
			
			
		}
	};

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

}
