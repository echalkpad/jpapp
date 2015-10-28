module.exports = function(Location) {
	Location.disableRemoteMethod('deleteById', true);				//removes DELETE /Locations/{id}
	Location.disableRemoteMethod('updateAll', true);				//removes POST /Locations/update
	Location.disableRemoteMethod('createChangeStream', true);		//removes GET & POST /Locations/change-stream
	Location.disableRemoteMethod('queryView', true);				//removes GET /Locations/queryView [from couchdb connector]
	

	
	/////////////////////////////////////////////
	//// Default  fields /////
	///////////////////////////////////////////
	Location.observe('before save', function(ctx, next) {
		var body = {};
		if (ctx.instance) {					//creating new one
			body = ctx.instance;
		}
		else {								//editing existing
			body = ctx.data;
		}
		//console.log(ctx);
		if(!body.timestamp) body.timestamp = Date.now();
		if(!body.username && ctx.currentInstance) body.username = ctx.currentInstance.username;
		console.log(body);
		next();
	});
	
	Location.observe('after delete', function(ctx, next) {
		if (ctx.instance) Location.create({username: ctx.instance.username});
		next();
	});
};
