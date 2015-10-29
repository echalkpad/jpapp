"use strict";
/*******************************************************************************
 * Copyright (c) 2015 IBM Corp.
 *
 * All rights reserved. 
 *
 * Contributors:
 *   David Huffman - Initial implementation
 *******************************************************************************/
 
var rest = require('../../utils/rest');

//get account data from citi bank APIs
module.exports.get_citi_account = function (username, next, cb){
	console.log('citi account:', username);

	var options = 	{
						host: "citi-online-banking.mybluemix.net",
						path: "/api/account/" + username,
						headers: {
							'Content-Type': 'application/json',
							'API-Auth-Token': 'Swooba!'
						}
					};
	options.success = function(statusCode, data){
		console.log("Get CitiBank - success", data);
		try{
			data = JSON.parse(data);
		}
		catch(e){}
		if(cb) cb(null, data, next);
	};
	options.failure = function(statusCode, e){
		console.log("Get CitiBank Location  - failure", e);
		if(cb) cb(statusCode, e, next);
	};
	rest.get(options, '');
};


//send push notification with IBM push service
module.exports.send_push_notification = function (username, msg, cb){
	console.log('send_push_notification() - fired:', username, ":", msg);
	var options = 	{
						host: "mobile.ng.bluemix.net",
						path: "push/v1/apps/3fdaeee4-d711-4ffe-9681-6afee65a120a/messages",
						headers: {
							'Content-Type': 'application/json',
							'IBM-Application-Secret': '8c55c943843f34c14672d6f36cfe4fe3f6961d1d'
						}
					};
	var body = {
					"message": {
									"alert": msg
								},
					"target": {
								"consumerIds":[ {"consumerId": username }]
					}
				};
	options.success = function(statusCode, data){
		console.log("POST Push - success", data);
		if(cb) cb(null, data);
	};
	options.failure = function(statusCode, e){
		console.log("POST Push - failure", e);
		if(cb) cb(statusCode, e);
	};
	rest.post(options, '', body);
};