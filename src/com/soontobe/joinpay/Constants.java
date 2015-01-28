package com.soontobe.joinpay;

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
		"Ben",
		"David"
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

}