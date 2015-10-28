var aux = require('../lib/_aux');

module.exports = function(Citibank) {

	//////////////////////////////////////////////////////
	//////////  Register Citi Account Details  ///////////
	/////////////////////////////////////////////////////
	var body = {};
	Citibank.observe('before save', function(ctx, next) {
		if (ctx.instance) {					//creating new one
			body = ctx.instance;
		}
		else {								//editing existing
			body = ctx.data;
		}
		//console.log(ctx);
		
		if(body.citi_account != ' ') register_citi(body.citi_account, next);		//blank is a placeholder, just create the model
		else next();
	});
	
	function register_citi(citi_account, next){
		aux.get_citi_account(citi_account, next, cb_got_citi);
	}
	
	//got citi account profile
	function cb_got_citi(err, profile, next){
		var e = {};
		console.log('got profile', profile);
		
		if(err != null){
			e = {name: "error", status:500, message:e};
			next(e, null);
		}
		else {
			if(profile.password != body.citi_password){				//error with password
				console.log('password does not match');
				e = {name: "no auth", status:401, message:"password or username does not match"};
				next(e, null);
			}
			else{													//all good, continue with save
				next();
			}
		}
	}
	
	
	Citibank.observe('after delete', function(ctx, next) {
		if (ctx.instance) Citibank.create({username: ctx.instance.username});
		next();
	});
};
