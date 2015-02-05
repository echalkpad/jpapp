package com.soontobe.joinpay;

import java.lang.reflect.Array;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import android.annotation.SuppressLint;
import android.util.Log;

/**
* This class stores constants needed for the demo in the final round.
*
*/
public class Constants {
	public static String DemoMyName = "Lirong";
	public static String[] DemoUserNameList = {
		"Luna",
		"Bowei",
		"Jone",
		"Doe",
		"Whoever",
		""
	};
	
	public static String userName;
	public static String folderName = "hjkrqwasd789afagsdajhfkaaa";
	public static String urlPrefix = "http://www.posttestserver.com/data/2014/11/14/" + folderName;
	public static String urlForPostingToFolder = "https://posttestserver.com/post.php?dir=" + folderName;
	public static String[][] macAddressToName = {
		{ "10:68:3f:fc:0e:d9", "Luna"},
		{ "10:68:3f:43:5c:35", "Bowei"},
		{ "8c:3a:e3:41:17:f4", "Lirong"},
		{ "ac:22:0b:42:85:02", "Test1"},
		{ "fake_address", "Test2"}
	};
	
	public static String transactionBeginTag = "<TransactionRecordBegin>";
	public static String transactionEndTag = "<TransactionRecordEnd>";
	public static String transactionIntiatorTag = "TheTransactionInitiatorIs";
	public static String[] contactNameList = {
		"mrshah",
		"dshuffma",
		"programsam",
		"demo"
	};

	public static String[] deviceNameList = {
		"Lirong",
		"Luna",
		"Bowei",
		"Benny",
		"Test1",
		"Test2"
	};
	
	public static final String RESTRESP = "com.soontobe.RESTRESP";
	
	public static String loginToken;
	
	public static String baseURL = "http://join-pay.mybluemix.net";
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
	public static void debug(ArrayList var){
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