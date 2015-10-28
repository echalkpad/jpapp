package com.soontobe.joinpay.helpers;

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Iterator;

/**
 * Created by mrshah on 10/27/2015.
 */
public class Rest {

    private static HttpClient mClient = null;

    public interface httpResponseHandler {
        void handleResponse(HttpResponse response, boolean error);
    }

    private static void addAuth(HttpRequestBase request, JSONObject auth) {
        try {
            String type = auth.getString("type");
            if(type.toLowerCase().equals("basic")) {
                String username = auth.getString("username");
                String password = auth.getString("password");
                final String basicAuth = "Basic " + Base64.encodeToString((username + ":" + password).getBytes(), Base64.NO_WRAP);
                request.setHeader("Authorization", basicAuth);
            } else if(type.toLowerCase().equals("token")) {
                String header = auth.getString("header");
                String value = auth.getString("value");
                request.setHeader(header, value);
            }
        } catch (JSONException e1) {
            e1.printStackTrace();
        }
    }

    private static void addHeaders(HttpRequestBase request, JSONObject headers) {
        if(headers != null) {
            @SuppressWarnings("unchecked")
            Iterator<String> iter = headers.keys();
            while(iter.hasNext()) {
                String key = iter.next();
                String value = null;
                try {
                    value = headers.getString(key);
                    request.setHeader(key, value);
                } catch (JSONException e1) {
                    // If there is an error getting value of the header, still continue
                    e1.printStackTrace();
                }
            }
        }
    }

    public static void get(final String url, final JSONObject auth, final JSONObject headers, final httpResponseHandler responseHandler) {

        Log.d("Rest get", "Called");
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                if(mClient == null) {
                    mClient  = new DefaultHttpClient();
                }
//                HttpClient httpClient = new DefaultHttpClient();
                HttpGet httpget = new HttpGet(url);

                addAuth(httpget, auth);
                addHeaders(httpget, headers);

                ResponseHandler<String> localResponseHandler = new ResponseHandler<String>() {
                    @Override
                    public String handleResponse(HttpResponse response) throws IOException {
//                        Log.d("Rest get", "Response: " + EntityUtils.toString(response.getEntity()));
                        HttpResponse httpResp = response;
                        responseHandler.handleResponse(httpResp, false);
                        return null;
                    }
                };

                String out = null;
                try {
                    Log.d("Rest get", "executing");
                    out = mClient.execute(httpget, localResponseHandler);
                } catch (IOException e) {
                    responseHandler.handleResponse(null, true);
                    e.printStackTrace();
                }
//                Log.d("out", out);
            }
        });
        t.start();
    }

    public static void post(final String url, final JSONObject auth, final JSONObject headers, final String body, final httpResponseHandler responseHandler) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                if(mClient == null) {
                    mClient  = new DefaultHttpClient();
                }
//                HttpClient httpClient = new DefaultHttpClient();
                HttpPost httppost = new HttpPost(url);

                addAuth(httppost, auth);
                addHeaders(httppost, headers);
                httppost.setEntity(new StringEntity(body, "UTF8"));

                try {
                    JSONObject testObj = new JSONObject(body);
                    httppost.setHeader("Content-Type", "application/json");
                } catch (JSONException e) {

                }

                ResponseHandler<String> localResponseHandler = new ResponseHandler<String>() {
                    @Override
                    public String handleResponse(HttpResponse response) {
                        responseHandler.handleResponse(response, false);
                        return null;
                    }
                };

                try {
                    mClient.execute(httppost, localResponseHandler);
                } catch (IOException e) {
                    responseHandler.handleResponse(null, true);
                    e.printStackTrace();
                }
            }
        });
        t.start();
    }

    public static void put(final String url, final JSONObject auth, final JSONObject headers, final String body, final httpResponseHandler responseHandler) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                if(mClient == null) {
                    mClient  = new DefaultHttpClient();
                }
//                HttpClient httpClient = new DefaultHttpClient();
                HttpPut httpput = new HttpPut(url);

                addAuth(httpput, auth);
                addHeaders(httpput, headers);
                httpput.setEntity(new StringEntity(body, "UTF8"));

                try {
                    JSONObject testObj = new JSONObject(body);
                    httpput.setHeader("Content-Type", "application/json");
                } catch (JSONException e) {

                }

                ResponseHandler<String> localResponseHandler = new ResponseHandler<String>() {
                    @Override
                    public String handleResponse(HttpResponse response) {
                        responseHandler.handleResponse(response, false);
                        return null;
                    }
                };

                String out = null;
                try {
                    out = mClient.execute(httpput, localResponseHandler);
                } catch (IOException e) {
                    responseHandler.handleResponse(null, true);
                    e.printStackTrace();
                }
                Log.d("out", out);
            }
        });
        t.start();
    }

}
