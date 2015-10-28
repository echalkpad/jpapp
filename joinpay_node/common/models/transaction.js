var app = require('../../server/server');
var rest = require('../../utils/rest');
var aux = require('../lib/_aux');

module.exports = function(Transaction) {
	Transaction.disableRemoteMethod('deleteById', true);			//removes DELETE /transactions/{id}
	Transaction.disableRemoteMethod('updateAll', true);				//removes POST /transactions/update
	Transaction.disableRemoteMethod('createChangeStream', true);	//removes GET & POST /transactions/change-stream
	Transaction.disableRemoteMethod('queryView', true);				//removes GET /transactions/queryView [from couchdb connector]
	
	//dsh to do:
	// - check state of trans before modifiying
	// - protect things, change user roles
	// - handle all the errors
	// - change find() to findByID() where I can, also check if ctx has current instance already!
	
	// NOtes:
	// ctx has "where" with the trans ID field, does not have user ID field though
		
	Transaction.observe('before save', function(ctx, next) {
		var body = {}, temp = {}, e = {}, msg = "";
		var APPROVAL_MODE = false;
		if (ctx.instance) {					//creating new one [creating initial thing]
			body = ctx.instance;
			APPROVAL_MODE = false;
			msg = body.toUser + " has charged you $" + body.amount;
			aux.send_push_notification(body.fromUser, msg);
		}
		else {								//editing existing [approv or deny!]
			body = ctx.data;
			APPROVAL_MODE = true;
		}
		//console.log(ctx);
		console.log((APPROVAL_MODE?'- editing trans':'- creating new trans'));
		
		if(APPROVAL_MODE && body.status){
			if(body.status == 'APPROVED'){
				console.log(' - approving transaction');
				msg = ctx.currentInstance.fromUser + " has approved your request of $" + ctx.currentInstance.amount;
				aux.send_push_notification(ctx.currentInstance.toUser, msg);
				app.models.citibank.find({where: {username: ctx.currentInstance.toUser}}, cb_got_profile);			//we already have it in currentInstance
				
				//got TO user profile
				function cb_got_profile(err, user){
					console.log('cb_got_profile() - fired');
					if(err != null){
						e = {name: "error", status:500, message:e};
						next(e, null);
					}
					else if(user.length == 0){
						e = {name: "no profile", status:404, message:"cannot find TO user profile"};
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
						next(e, null);
					}
					else if(fromUser.length == 0){
						e = {name: "no profile", status:404, message:"cannot find FROM user profile"};
						next(e, null);
					}
					else{
						citi_bank(temp[0].citi_account, fromUser[0].citi_account, ctx.currentInstance.amount, next);
					}
				}
			}
			else{
				console.log(' - denying transaction');
				msg = ctx.currentInstance.fromUser + " has denied your request of $" + ctx.currentInstance.amount;
				aux.send_push_notification(ctx.currentInstance.toUser, msg);
				next();
			}
		}
		else next();
	});
	
	
	/////////////////////////////////////////////
	//// Default fields /////
	///////////////////////////////////////////
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


//citi rest call
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
		if(cb) cb(null, data);
	};
	options.failure = function(statusCode, e){
		console.log("Post CitiBank Location  - failure", e);
		if(cb) cb(statusCode, e);
	};
	rest.post(options, '', body);
}