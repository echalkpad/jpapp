package com.soontobe.joinpay;

import java.io.IOException;
import java.util.Locale;

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
import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class RESTCalls extends IntentService {
	static HttpClient httpClient;
	int code;
	String strResp;
	
	public RESTCalls(String name) {
		super(name);
	}

	public RESTCalls() {
		super("RESTCalls");
		code = 0;
		strResp = "";
		if(httpClient == null) httpClient = new DefaultHttpClient();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		final String method = intent.getStringExtra("method");
		final String url = intent.getStringExtra("url");
		final String body = intent.getStringExtra("body");
		final String context = intent.getStringExtra("context");
		Log.d("rest", method + " " + url);
		//Log.d("body", ""+body);
		
		if(method.toLowerCase(Locale.ENGLISH).equals("get")) {
			try {
				HttpGet httpget = new HttpGet(url);
				if(Constants.loginToken != null) {
					httpget.addHeader("sessionID", Constants.loginToken);
				}
				ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
					@Override
					public String handleResponse(HttpResponse response)
							throws ClientProtocolException, IOException {
						strResp = EntityUtils.toString(response.getEntity());
						code = response.getStatusLine().getStatusCode();
						Log.d("rest", "HTTP: " + code + ", response: " + strResp);

						Intent responseIntent = new Intent(Constants.RESTRESP);
						responseIntent.putExtra("url", url);
						responseIntent.putExtra("method", method);
						responseIntent.putExtra("response", strResp);
						responseIntent.putExtra("code", code);
						responseIntent.putExtra("context", context);
						sendBroadcast(responseIntent);
						return strResp;
					}
				};
				
				httpClient.execute(httpget, responseHandler);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if(method.toLowerCase(Locale.ENGLISH).equals("post")) {
			try {
				HttpPost post = new HttpPost(url);
				if(Constants.loginToken != null) {
					post.addHeader("sessionID", Constants.loginToken);
				}
				ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
					@Override
					public String handleResponse(HttpResponse response)
							throws ClientProtocolException, IOException {
						strResp = EntityUtils.toString(response.getEntity());
						code = response.getStatusLine().getStatusCode();
						Log.d("rest", "HTTP: " + code + ", response: " + strResp);

						Intent responseIntent = new Intent(Constants.RESTRESP);
						responseIntent.putExtra("url", url);
						responseIntent.putExtra("method", method);
						responseIntent.putExtra("response", strResp);
						responseIntent.putExtra("code", code);
						responseIntent.putExtra("context", context);
						sendBroadcast(responseIntent);
						return strResp;
					}
				};
				post.setEntity(new StringEntity(body, "UTF8"));
				post.addHeader("Content-Type", "application/json");
				
				httpClient.execute(post, responseHandler);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if(method.toLowerCase(Locale.ENGLISH).equals("put")) {
			try {
				HttpPut put = new HttpPut(url);
				if(Constants.loginToken != null) {
					put.addHeader("sessionID", Constants.loginToken);
				}
				ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
					@Override
					public String handleResponse(HttpResponse response)
							throws ClientProtocolException, IOException {
						strResp = EntityUtils.toString(response.getEntity());
						code = response.getStatusLine().getStatusCode();
						Log.d("rest", "HTTP: " + code + ", response: " + strResp);
						
						Intent responseIntent = new Intent(Constants.RESTRESP);
						responseIntent.putExtra("url", url);
						responseIntent.putExtra("method", method);
						responseIntent.putExtra("response", strResp);
						responseIntent.putExtra("code", code);
						responseIntent.putExtra("context", context);
						sendBroadcast(responseIntent);
						return strResp;
					}
				};
				put.setEntity(new StringEntity(body, "UTF8"));
				put.addHeader("Content-Type", "application/json");
				
				httpClient.execute(put, responseHandler);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
