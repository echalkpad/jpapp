var app = require('../../server/server');
var geolib = require('geolib');
var aux = require('../lib/_aux');

module.exports = function(User) {
	//User.disableRemoteMethod('find', true);						//removes GET /users
	User.disableRemoteMethod('upsert', true);					//removes PUT /users
	User.disableRemoteMethod('findOne', true);						//removes GET /users/findOne
	User.disableRemoteMethod('count', true);						//removes GET /users/count
	
	User.disableRemoteMethod('confirm', true);					//removes PUT /users
	
	User.disableRemoteMethod('deleteById', true);				//removes DELETE /users/{id}
	User.disableRemoteMethod('updateAll', true);				//removes POST /users/update
	User.disableRemoteMethod('createChangeStream', true);		//removes GET & POST /users/change-stream
	User.disableRemoteMethod('queryView', true);				//removes GET /users/queryView [from couchdb connector]
	User.disableRemoteMethod('resetPassword', true);			//removes POST /reset [password reset]
	
	User.disableRemoteMethod('__delete__accessTokens', false); 			// DELETE
	User.disableRemoteMethod('__destroyById__accessTokens', false); 	// DELETE
	User.disableRemoteMethod('__updateById__accessTokens', false); 		// PUT
	User.disableRemoteMethod('__create__accessTokens', false); 			// POST
	User.disableRemoteMethod('__get__accessTokens', false); 			// GET
	User.disableRemoteMethod('__count__accessTokens', false); 			// GET count
	User.disableRemoteMethod('__findById__accessTokens', false); 		// GET by id
	
	//User.disableRemoteMethod('__create__credits', false); 			// POST [keep this commented out]
	User.disableRemoteMethod('__updateById__credits', false); 			// PUT by id
	
	User.disableRemoteMethod('__create__debits', false); 				// POST
	//User.disableRemoteMethod('__updateById__debits', false); 			// PUT by id [keep this commented out]
	
	User.disableRemoteMethod('__create__citibank', false); 				// POST
	User.disableRemoteMethod('__create__location', false); 				// POST
	
	
	//////////////////////////////////////////
	////////// Get Friends Nearby ///////////
	////////////////////////////////////////
	User.nearby = function(username, cb) {
		console.log('nearby() - fired');
		var ret = [];
		var self = {};
		
		app.models.location.find(null, cb_found_location);
		
		function cb_found_location(err, data){														//got all locations
			console.log('cb_found_location()', err);
			if(err != null){
				var e = {name: "error", status:500, message: err};
				cb(e, null);				
			}
			else if(data.length == 0){
				e = {name: "no location", status:404, message:"cannot find location doc"};
				cb(e, null);
			}
			else{
				//// Look for my location ////
				for(var i in data){
					if (data[i].username != username){
						self = {latitude: data[i].latitude, longitude: data[i].longitude};
					}
				}
				
				//// Compare distances /////
				for(var i in data){
					var friend = {latitude: data[i].latitude, longitude: data[i].longitude};
					var distance = geolib.getDistance(self, friend);
					var feet = geolib.convertUnit("ft", distance, 1);
					
					console.log(data[i].username, 'is ', feet, 'feet away');
					if (feet <= 2640 && data[i].username != username){									//closer than 1/2 mile, don't add self
						ret.push({username: data[i].username, distance: feet, timestamp: data[i].timestamp, units: 'feet'});
					}
				}
				
				ret.sort(function(a, b) {
					return a.distance - b.distance;
				});
				
				cb(null, ret);
			}
		}
	};
	
	//register nearby function
	User.remoteMethod('nearby', 
		{
			http: 	 {path: '/:id/location/friends', verb: 'get'},
			accepts: {arg: 'id', type: 'string', required: true},
			returns: [
						{
							arg: 'friends',
							type: 'array'
						}
					]
		}
	);
	
	
	//////////////////////////////////////////////////
	//////////  Get Citi Account Details  ///////////
	////////////////////////////////////////////////
	User.citibank = function(username, cb) {
		console.log('citi() - fired');
		var e = {};
		app.models.citibank.find({where: {username: username}}, cb_got_citibank);
			
		function cb_got_citibank(err, account){
			console.log('!', err, account);
			if(err != null){
				e = {name: "error", status:500, message:e};
				cb(e, null);
			}
			else if(account.length == 0 || account[0].citi_account.trim() == ''){
				e = {name: "no citi account", status:404, message:"cannot find citi account"};
				cb(e, null);
			}
			else{
				aux.get_citi_account(account[0].citi_account, null, cb);
			}
		}
	};
	
	//register citi function
	User.remoteMethod('citibank', 
		{
			http: 	 {path: '/:id/citibank/details', verb: 'get'},
			accepts: {arg: 'id', type: 'string', required: true},
			returns: {arg: 'citibank', type: 'object'}
		}
	);
	
	
	/////////////////////////////////////////////
	//// Default email and username fields /////
	///////////////////////////////////////////
	User.observe('before save', function(ctx, next) {
		var body = {};
		if (ctx.instance) {					//creating new one
			body = ctx.instance;
		}
		else {								//editing existing
			body = ctx.data;
		}
		
		if(!body.email) body.email = body.id + '@noreply.com';
		if(!body.username && ctx.currentInstance) body.username = ctx.currentInstance.username;
		if(!body.username) body.username = body.id;
		console.log(body);
		
		next();
	});
	
	
	/////////////////////////////////////////////
	//// Init other things /////
	///////////////////////////////////////////
	User.observe('after save', function(ctx, next) {
		var body = {};
		if (ctx.instance) {					//creating new one
			console.log('creating new user: creating things...', ctx.instance);
			app.models.location.create({username: ctx.instance.username});
			app.models.citibank.create({username: ctx.instance.username});
			body = ctx.instance;
		}
		next();
	});
};
