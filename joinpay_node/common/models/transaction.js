var app = require('../../server/server');
var rest = require('../../utils/rest');
var aux = require('../lib/_aux');

module.exports = function(Transaction) {
	Transaction.disableRemoteMethod('deleteById', true);										//removes DELETE /transactions/{id}
	Transaction.disableRemoteMethod('updateAll', true);											//removes POST /transactions/update
	Transaction.disableRemoteMethod('createChangeStream', true);								//removes GET & POST /transactions/change-stream
	Transaction.disableRemoteMethod('queryView', true);											//removes GET /transactions/queryView [from couchdb connector]
	
	//happens when editing/creating
	Transaction.observe('before save', function(ctx, next) {
		var body = {}, temp = {}, e = {}, msg = "", status ="";
		if (ctx.instance) {																		//creating new one [creating initial thing]
			console.log('- creating new trans');
			body = ctx.instance;
			if(body.status) body.status = body.status.toUpperCase();
			body.amount = Number(body.amount);													//NaN will trip vaildator
			if(body.amount && body.amount > 0) {
				body.amount = body.amount.toFixed(2);
				msg = body.toUser + " has charged you $" + body.amount;
				aux.send_push_notification(body.fromUser, msg, null);
			}
			else{
				body.amount = null;
			}
			console.log('next 0');
			next();
		}
		else {																					//editing existing [approv or deny]
			console.log('- editing trans');
			body = ctx.data;
			
			if(body.status){
				status = body.status.toUpperCase();												//store temp desired status
				delete body.amount;																//there is a default value here, remove it
				if(ctx.currentInstance.status.toUpperCase() != 'PENDING'){
					e = {name: "invalid state", status:400, message:"the transaction is no longer pending"};
					console.log('next 1');
					console.log(e);
					next(e, null);
				}
				else if(body.status == 'APPROVED'){												//approving
					console.log(' - approving transaction', ctx);
					if(ctx.currentInstance.amount) ctx.currentInstance.amount = ctx.currentInstance.amount.toFixed(2);
					msg = ctx.currentInstance.fromUser + " has approved your request of $" + ctx.currentInstance.amount;
					aux.send_push_notification(ctx.currentInstance.toUser, msg, cb_sent_push);	//send push first
				}
				else{
					body.status = 'DENIED';														//denying
					console.log(' - denying transaction');
					if(ctx.currentInstance.amount) ctx.currentInstance.amount = ctx.currentInstance.amount.toFixed(2);
					msg = ctx.currentInstance.fromUser + " has denied your request of $" + ctx.currentInstance.amount;
					aux.send_push_notification(ctx.currentInstance.toUser, msg);
					console.log('next 6');
					next();
				}
			}
			else {																				//error will be caught by vaildator
				console.log('next 7');
				next();
			}
		}
	
		//got TO user profile
		function cb_sent_push(err, data){
			console.log('cb_sent_push() - fired');
			app.models.citibank.find({where: {username: ctx.currentInstance.toUser}}, cb_got_profile);	//we already have it in currentInstance
		}
	
		//got TO user profile
		function cb_got_profile(err, user){
			console.log('cb_got_profile() - fired');
			if(err != null){
				e = {name: "error", status:500, message:e};
				console.log('next 2');
				console.log(e);
				next(e, null);
			}
			else if(user.length == 0){
				e = {name: "no profile", status:404, message:"cannot find TO user profile"};
				console.log('next 3');
				console.log(e);
				next(e, null);
			}
			else {
				temp = user;
				app.models.citibank.find({where: {username: ctx.currentInstance.fromUser}}, cb_got_fromuser);
			}
		}
		
		//got FROM user profile
		function cb_got_fromuser(err, fromUser){
			console.log('cb_got_fromuser() - fired');
			if(err != null){
				e = {name: "error", status:500, message:e};
				console.log('next 4');
				console.log(e);
				next(e, null);
			}
			else if(fromUser.length == 0){
				e = {name: "no profile", status:404, message:"cannot find FROM user profile"};
				console.log('next 5');
				console.log(e);
				next(e, null);
			}
			else{
				citi_bank(temp[0].citi_account, fromUser[0].citi_account, ctx.currentInstance.amount, next);
			}
		}
	});
	
	
	/////////////////////////////
	////   Default fields   /////
	/////////////////////////////
	Transaction.observe('before save', function(ctx, next) {
		var body = {};
		if (ctx.instance) {					//creating new one
			body = ctx.instance;
		}
		else {								//editing existing
			body = ctx.data;
		}
		
		if(!body.timestamp) body.timestamp = Date.now();
		if(!body.status) body.status = "PENDING";
		console.log(body);
		
		next();
	});
};


//citi rest call to move money
function citi_bank(toAccount, fromAccount, amount, cb){
	var options = 	{
						host: "citi-online-banking.mybluemix.net",
						path: "/api/transfer",
						headers: {
							'Content-Type': 'application/json',
							'API-Auth-Token': 'Swooba!'
						}
					};
	var body = {
					"fromCustomer": fromAccount, 
					"toCustomer": toAccount, 
					"amount": amount 
				};
	console.log('- sending charge', body);
	options.success = function(statusCode, data){
		console.log("Post CitiBank - success", data);
		console.log('next 8');
		if(cb) cb();
	};
	options.failure = function(statusCode, e){
		console.log("Post CitiBank Location  - failure");
		e = {name: "error", status:statusCode, message:e};
		console.log('next 9');
		console.log(e);
		if(cb) cb(e, null);
	};
	rest.post(options, '', body);
}