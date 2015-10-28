package com.soontobe.joinpay.helpers;

import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;

/**
 * Created by mrshah on 10/27/2015.
 */
public class Rest {

    public interface httpResponseHandler {
        void handleResponse(JSONObject response, boolean error);
    }

    private static void addAuth(HttpURLConnection request, JSONObject auth) {
        if(auth == null) {
            return;
        }
        try {
            String type = auth.getString("type");
            if(type.toLowerCase().equals("basic")) {
                String username = auth.getString("username");
                String password = auth.getString("password");
                final String basicAuth = "Basic " + Base64.encodeToString((username + ":" + password).getBytes(), Base64.NO_WRAP);
                request.setRequestProperty("Authorization", basicAuth);
            } else if(type.toLowerCase().equals("token")) {
                String header = auth.getString("header");
                String value = auth.getString("value");
                request.setRequestProperty(header, value);
            }
        } catch (JSONException e1) {
            e1.printStackTrace();
        }
    }

    private static void addHeaders(HttpURLConnection request, JSONObject headers) {
        if(headers != null) {
            @SuppressWarnings("unchecked")
            Iterator<String> iter = headers.keys();
            while(iter.hasNext()) {
                String key = iter.next();
                String value = null;
                try {
                    value = headers.getString(key);
                    request.setRequestProperty(key, value);
                } catch (JSONException e1) {
                    // If there is an error getting value of the header, still continue
                    e1.printStackTrace();
                }
            }
        }
    }

    public static void get(final String url, final JSONObject auth, final JSONObject headers, final httpResponseHandler responseHandler) {

        Log.d("Rest get", "Called");
        Log.d("Rest get", "URL: " + url);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL mURL = new URL(url);

                    HttpURLConnection urlConnection = (HttpURLConnection) mURL.openConnection();
                    urlConnection.setRequestMethod("GET");
                    addAuth(urlConnection, auth);

                    urlConnection.setDoInput(true);

                    int responseCode = urlConnection.getResponseCode();
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    InputStreamReader inr = new InputStreamReader(in);
                    BufferedReader br = new BufferedReader(inr);
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        result.append(line);
                    }

                    JSONObject response = new JSONObject();

                    response.put("responseCode", responseCode);
                    response.put("data", result.toString());
                    Log.d("Http get", "Code: " + responseCode);
                    Log.d("Http get", "Data: " + result.toString());
                    urlConnection.disconnect();
                    responseHandler.handleResponse(response, false);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    JSONObject response = new JSONObject();

                    try {
                        response.put("responseCode", 404);
                        response.put("data", "");
                    } catch (JSONException e1) {
                        // Will never reach here
                        e1.printStackTrace();
                    }
                    responseHandler.handleResponse(response, false);
                } catch (IOException e) {
                    e.printStackTrace();
                    responseHandler.handleResponse(null, true);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
    }

    public static void post(final String url, final JSONObject auth, final JSONObject headers, final String body, final httpResponseHandler responseHandler) {
        Log.d("Rest post", "URL: " + url);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL mURL = new URL(url);

                    HttpURLConnection urlConnection = (HttpURLConnection) mURL.openConnection();
                    urlConnection.setRequestMethod("POST");
                    addAuth(urlConnection, auth);
                    addHeaders(urlConnection, headers);
                    try {
                        JSONObject testObj = new JSONObject(body);
                        urlConnection.setRequestProperty("Content-Type", "application/json");
                    } catch (JSONException e) {

                    }

                    urlConnection.setRequestProperty("Content-Length", Integer.toString(body.length()));
                    urlConnection.setDoInput(true);
                    urlConnection.setDoOutput(true);
                    DataOutputStream os = new DataOutputStream(urlConnection.getOutputStream());
                    os.write(body.getBytes());
                    os.flush();
                    os.close();

                    int responseCode = urlConnection.getResponseCode();
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    InputStreamReader inr = new InputStreamReader(in);
                    BufferedReader br = new BufferedReader(inr);
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        result.append(line);
                    }

                    JSONObject response = new JSONObject();

                    response.put("responseCode", responseCode);
                    response.put("data", result.toString());
                    Log.d("Http post", "Code: " + responseCode);
                    Log.d("Http post", "Data: " + result.toString());
                    urlConnection.disconnect();
                    responseHandler.handleResponse(response, false);
                } catch (IOException e) {
                    e.printStackTrace();
                    responseHandler.handleResponse(null, true);
                } catch (JSONException e) {
                    e.printStackTrace();
                    responseHandler.handleResponse(null, true);
                }

            }
        });
        t.start();
    }

    public static void put(final String url, final JSONObject auth, final JSONObject headers, final String body, final httpResponseHandler responseHandler) {
        Log.d("Rest put", "URL: " + url);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL mURL = new URL(url);

                    HttpURLConnection urlConnection = (HttpURLConnection) mURL.openConnection();
                    urlConnection.setRequestMethod("PUT");
                    addAuth(urlConnection, auth);
                    addHeaders(urlConnection, headers);
                    try {
                        JSONObject testObj = new JSONObject(body);
                        urlConnection.setRequestProperty("Content-Type", "application/json");
                    } catch (JSONException e) {

                    }

                    urlConnection.setRequestProperty("Content-Length", Integer.toString(body.length()));
                    urlConnection.setDoInput(true);
                    urlConnection.setDoOutput(true);
                    DataOutputStream os = new DataOutputStream(urlConnection.getOutputStream());
                    os.write(body.getBytes());
                    os.flush();
                    os.close();

                    int responseCode = urlConnection.getResponseCode();
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    InputStreamReader inr = new InputStreamReader(in);
                    BufferedReader br = new BufferedReader(inr);
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        result.append(line);
                    }

                    JSONObject response = new JSONObject();

                    response.put("responseCode", responseCode);
                    response.put("data", result.toString());
                    Log.d("Http put", "Code: " + responseCode);
                    Log.d("Http put", "Data: " + result.toString());
                    urlConnection.disconnect();
                    responseHandler.handleResponse(response, false);
                } catch (IOException e) {
                    e.printStackTrace();
                    responseHandler.handleResponse(null, true);
                } catch (JSONException e) {
                    e.printStackTrace();
                    responseHandler.handleResponse(null, true);
                }

            }
        });
        t.start();
    }

}
