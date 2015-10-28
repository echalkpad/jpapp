package com.soontobe.joinpay;

import android.annotation.SuppressLint;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
* This class stores constants needed for the demo in the final round.
*
*/
public class Constants {

	public static final int RESPONSE_200 = 200;
	public static final int RESPONSE_400 = 400;
	public static final int RESPONSE_401 = 401;
	public static final int RESPONSE_403 = 403;
	public static final int RESPONSE_404 = 404;
	public static final int RESPONSE_500 = 500;
	public static final int RESPONSE_502 = 502;

	public static final int TOP_X_OFFSET = 0;
	public static final int TOP_Y_OFFSET = 150;

	public static final int USERNAME_MIN_LENGTH = 3;
	public static final int PASSWORD_MIN_LENGTH = 1;

	public static String DemoMyName = "Lirong";
	public static String userName = "no_user";
	public static String password = "";
	public static String transactionBeginTag = "<TransactionRecordBegin>";
	public static String transactionEndTag = "<TransactionRecordEnd>";
	public static String transactionIntiatorTag = "TheTransactionInitiatorIs";
	public static String[] contactNameList = {
		"mrshah",
		"dshuffma",
		"programsam",
		"david",
		"kostas",
		"elana",
		"heather",
		"jerry",
		"nick",
		"curtis",
		"guy1"
	};

	public static final String PUSH_TAG = "push";
	
	public static String baseURL = "http://joinpay.mybluemix.net/api";
	//public static String baseURL = "http://join-pay.mybluemix.net";
	public static String appSecret = "8c55c943843f34c14672d6f36cfe4fe3f6961d1d";
	public static String appKey = "3fdaeee4-d711-4ffe-9681-6afee65a120a";
	
	
	/*Some slightly useful short hand debug prints*/
	@SuppressLint("NewApi")
	public static void debug(String[] var){
		JSONArray temp = null;
		try {
			temp = new JSONArray(var);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		Log.d("debug", temp.toString());
	}
	public static void debug(ArrayList<?> var){
		JSONArray temp = new JSONArray(var);
		Log.d("debug", temp.toString());
	}
	@SuppressLint("NewApi")
	public void debug(Array var){
		JSONArray temp = null;
		try {
			temp = new JSONArray(var);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		Log.d("debug", temp.toString());
	}


}