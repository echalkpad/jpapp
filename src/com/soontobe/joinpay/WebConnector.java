package com.soontobe.joinpay;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.util.Log;

/**
 * This class is specially designed for our "weird" way of communication among multiple devices.
 * We use posttestserver.com as a simple server. Each android device posts a sign-in message at the very beginning
 * and checks whether other devices are online every 3-5 seconds.
 */
public class WebConnector {

	private String userName;

	public WebConnector(String userName) {
		this.userName = userName;
	}

	public void postTransactionRecord(String url, String paymentInfoString) {
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(url);

		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		nameValuePairs.add(new BasicNameValuePair("transaction", Constants.transactionBeginTag + paymentInfoString + Constants.transactionEndTag));
		nameValuePairs.add(new BasicNameValuePair("payer", Constants.transactionIntiatorTag + Constants.userName));

		try {
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			HttpResponse response = httpclient.execute(httppost);
			String result = inputStreamToString(response.getEntity().getContent()).toString();
						Log.d("HttpPost result", result);
		} catch (IOException e) {
			// TODO Auto-generated catch block
		}
	}

	public void onlineSignIn(String url) {
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(url);

		try {
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			nameValuePairs.add(new BasicNameValuePair("status", Constants.userName + "IsOnline"));
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			HttpResponse response = httpclient.execute(httppost);


		} catch (IOException e) {
			// TODO Auto-generated catch block
		}
	} 

	public String getFile(String url)
	{
		HttpClient httpclient = new DefaultHttpClient();
		HttpGet httpget = new HttpGet(url); 

		ArrayList<String> ret = new ArrayList<String>();
		HttpResponse response;
		String result = "";
		try {
			response = httpclient.execute(httpget);
			HttpEntity entity = response.getEntity();
			if (entity != null) {

				InputStream instream = entity.getContent();
				result = inputStreamToString(instream).toString();
			}
		} catch (Exception e) {
			Log.e("HttpGet", e.toString());
		}

		return result;
	}

	public ArrayList<String> getFileNameList(String url, int visitedFilesCount)
	{
		HttpClient httpclient = new DefaultHttpClient();
		HttpGet httpget = new HttpGet(url); 

		ArrayList<String> ret = new ArrayList<String>();
		HttpResponse response;
		try {
			response = httpclient.execute(httpget);
			HttpEntity entity = response.getEntity();

			if (entity != null) {

				InputStream instream = entity.getContent();
				String result= inputStreamToString(instream).toString();

				String html = result;
				Document doc = Jsoup.parse(html);
				Elements link = doc.select("a");

				int cnt = 0;
				for (Element elm : link) {
					String text = elm.text();
					if (text.matches("[0-9][0-9].[0-9][0-9].[0-9]+")) {
						if (cnt >= visitedFilesCount) {
							ret.add(text);
						}
						cnt++;
					}
				}
				instream.close();
			}
		} catch (Exception e) {
			Log.e("HttpGet", e.toString());
		}
		Log.d("getFileNameList", "files num: " + ret.size());
		return ret;
	}

	private StringBuilder inputStreamToString(InputStream is) {
		String line = "";
		StringBuilder total = new StringBuilder();
		BufferedReader rd = new BufferedReader(new InputStreamReader(is));

		// Read response until the end
		try {
			while ((line = rd.readLine()) != null) { 
				total.append(line); 
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Return full string
		return total;
	}		
}
