"use strict";
/* global process */
/*******************************************************************************
 * Copyright (c) 2015 IBM Corp.
 *
 * All rights reserved. 
 *
 * Contributors:
 *   David Huffman - Initial implementation
 *******************************************************************************/
 
// ============================================================================
// SETUP CLOUDANT
// ============================================================================
var cloudant = module.exports;
///////////////////////    Nano Code    ////////////////////////


// NOT IN USE YET - 10/28/2015


// Build Cloudant credentials object.
function build_cloudant_credentials() {
	var env = {};
	var cloudant_credentials = {
									host: "bc16f920-82be-4164-bf7e-cd5cb582a5d0-bluemix.cloudant.com",
									port: "443",
									username: "bc16f920-82be-4164-bf7e-cd5cb582a5d0-bluemix",
									password: "0a931142261553e941343f18ad16f86870b182e8906d1c43816d2015ac8d6e92"
								};
	if (process.env.VCAP_SERVICES) {
		env = JSON.parse(process.env.VCAP_SERVICES);
		if (env.cloudantNoSQLDB) {
			cloudant_credentials = env.cloudantNoSQLDB[0].credentials;
		}
	}
	return cloudant_credentials;
}
var cl = build_cloudant_credentials();
