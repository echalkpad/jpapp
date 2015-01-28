package com.soontobe.joinpay;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class RESTCalls extends IntentService {

	public RESTCalls(String name) {
		super(name);
	}

	public RESTCalls() {
		super("RESTCalls");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		final String method = intent.getStringExtra("method");
		final String url = intent.getStringExtra("url");
		final String body = intent.getStringExtra("body");
		final String context = intent.getStringExtra("context");
		Log.d("method", method);
		Log.d("url", url);
		Log.d("body", ""+body);
		
		if(method.toLowerCase().equals("get")) {
			try {
				Log.d("url", url);
				HttpClient httpClient = new DefaultHttpClient();
				HttpGet httpget = new HttpGet(url);
				if(Constants.loginToken != null) {
					httpget.addHeader("sessionID", Constants.loginToken);
				}
				ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
					@Override
					public String handleResponse(HttpResponse response)
							throws ClientProtocolException, IOException {
						String strResp = EntityUtils.toString(response.getEntity());
						Log.d("httpget", strResp);
						
						Intent responseIntent = new Intent(Constants.RESTRESP);
						responseIntent.putExtra("url", url);
						responseIntent.putExtra("method", method);
						responseIntent.putExtra("response", strResp);
						responseIntent.putExtra("context", context);
						sendBroadcast(responseIntent);
						return strResp;
					}
				};
				
				String out = httpClient.execute(httpget, responseHandler);
				Log.d("out", out);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if(method.toLowerCase().equals("post")) {
			try {
				Log.d("url", url);
				HttpClient httpClient = new DefaultHttpClient();
				HttpPost post = new HttpPost(url);
				if(Constants.loginToken != null) {
					post.addHeader("sessionID", Constants.loginToken);
				}
				ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
					@Override
					public String handleResponse(HttpResponse response)
							throws ClientProtocolException, IOException {
						String strResp = EntityUtils.toString(response.getEntity());
						Log.d("httppost", strResp);
						
						Intent responseIntent = new Intent(Constants.RESTRESP);
						responseIntent.putExtra("url", url);
						responseIntent.putExtra("method", method);
						responseIntent.putExtra("response", strResp);
						responseIntent.putExtra("context", context);
						sendBroadcast(responseIntent);
						return strResp;
					}
				};
				post.setEntity(new StringEntity(body, "UTF8"));
				post.addHeader("Content-Type", "application/json");
				
				String out = httpClient.execute(post, responseHandler);
				Log.d("out", out);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if(method.toLowerCase().equals("put")) {
			try {
				Log.d("url", url);
				HttpClient httpClient = new DefaultHttpClient();
				HttpPut put = new HttpPut(url);
				if(Constants.loginToken != null) {
					put.addHeader("sessionID", Constants.loginToken);
				}
				ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
					@Override
					public String handleResponse(HttpResponse response)
							throws ClientProtocolException, IOException {
						String strResp = EntityUtils.toString(response.getEntity());
						Log.d("httppost", strResp);
						
						Intent responseIntent = new Intent(Constants.RESTRESP);
						responseIntent.putExtra("url", url);
						responseIntent.putExtra("method", method);
						responseIntent.putExtra("response", strResp);
						responseIntent.putExtra("context", context);
						sendBroadcast(responseIntent);
						return strResp;
					}
				};
				put.setEntity(new StringEntity(body, "UTF8"));
				put.addHeader("Content-Type", "application/json");
				
				String out = httpClient.execute(put, responseHandler);
				Log.d("out", out);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}


	}

}
