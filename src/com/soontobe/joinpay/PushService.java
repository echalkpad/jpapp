package com.soontobe.joinpay;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.util.EntityUtils;
import android.app.IntentService;
import android.content.Intent;
import android.util.Log;


/*NOT USED ANYWHERE YET 1/29/2015 */

public class PushService extends IntentService {
	static HttpClient httpClient;
	
	public PushService(String name) {
		super(name);
	}

	public PushService() {
		super("PushService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		final String method = intent.getStringExtra("method");
		final String url = intent.getStringExtra("url");
		final String body = intent.getStringExtra("body");
		final String context = intent.getStringExtra("context");
		Log.d("push_service", method);
		
		if(method.toLowerCase().equals("get")) {
			try {
				Log.d("push_service", url);
				ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
					@Override
					public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
						String strResp = EntityUtils.toString(response.getEntity());
						Log.d("push_service", strResp);
						
						Intent responseIntent = new Intent(Constants.RESTRESP);
						responseIntent.putExtra("url", url);
						responseIntent.putExtra("method", method);
						responseIntent.putExtra("response", strResp);
						responseIntent.putExtra("context", context);
						sendBroadcast(responseIntent);
						return strResp;
					}
				};
			} catch (Exception e) {
				e.printStackTrace();
			}
		} 
	}

}
