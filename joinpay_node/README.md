#JoinPay

##Notes: 
- money/amount should now be a number, not a string
- login will return an access token, store this and set it as a header in all future calls
		Authorization: ACCESS_TOKEN
- Full route is: https://joinpay.mybluemix.net/api/ENDPOINT_HERE

##API Map
See swagger for all parameters! https://joinpay.mybluemix.net/swagger/index.html
- Format is [Old API] -> [New API]
- GET /reinitialize -> n/a
- POST /register -> POST /users
- POST /login -> POST /login
- POST /logout -> POST /logout
- POST /setSession -> n/a
- GET /showSession -> n/a
- GET /nearby/users -> GET /users/location/friends
- PUT /currentLocation -> PUT /users/:id/location
- POST /charge -> POST /users/:id/credits
- GET /transactions -> GET /users/:id/debits && GET /users/:id/credits
- PUT transactions/approve/:transid -> PUT /users/:id/debits/:transid
- PUT transactions/deny/:transid -> PUT /users/:id/debits/:transid
- GET /myAccount -> GET /users/citibank
- POST /registerAccount -> PUT /users/:id/citibank

##Test Users Accounts
	joinpay: david password
	citi: dshuffma password

	joinpay: mihir password
	citi: programsam password

	joinpay: programsam password
	citi: programsam password


##New Models
	tbd

##Old Models
###User
	{
		"_id": "david",
		"username": "david",
		"password": "string",
		"default_account": "string",
		"latitude":  "35.9037670",
		"longitude": "-78.849840"
	}
	
###transacations
	{
		"_id": "094043b3-fdac-4b9f-a467-a632193e6267",
		"toUser": "mrshah",
		"toAccount": "mrshah",
		"description": " ",
		"fromUser": "david",
		"amount": "4",
		"created": 1445528879541,
		"status": "APPROVED"
	}
	