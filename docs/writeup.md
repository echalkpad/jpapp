---
#JoinPay Write Up
---

##Introduction
This article outlines a common scenario in the modern application development marketplace:
_How do we create a scalable, secure, and cloud-ready application?_
Oh, and by the way, this application needs to be finished yesterday.
Today, we're going to talk about our own experience and outcome with an Android application called JoinPay.
In a short period of time we took JoinPay from a UI mockup to a production ready Android app that includes scalability and security.
We were able to do this by using the plethora of services and applications that are built in to <a href="http://bluemix.net">IBM Bluemix</a>.

<br/>

##Where we started
Citibank is taking full advantage of the unique opportunities offered in the world of cloud development by crowd-sourcing their technology innovations.
They have staged a hackathon called the <a href="http://www.citimobilechallenge.com/index.php">Citi Mobile Challenge</a>,
in which they gave developers access to a set of mocked-up APIs that allowed programmatic access to Citi's banking services.
Developers then competed to produce the most innovative mobile app using these APIs.
JoinPay started its life as the winner of the 2014 Citi Mobile Challenge.
The idea behind JoinPay was to allow Citi account holders to split a bill at a restaurant by authorizing transfers between Citi accounts using their mobile devices.
The JoinPay app had a fantastic UI for demonstrating this story, but JoinPay's authors were rightfully focused on the story, rather than designing and implementing the app with scalability, performance, and security.
This is the point where we, IBM, stepped in.

###What we needed
The features we needed JoinPay to deliver can be summarized as the following:
1. A mobile backend that can scale and is secure
2. Persistent database store
3. Generate and receive Push Notifications
4. Find nearby users
5. Interaction with Citi APIs
6. Completed as soon as possible

##Architecture
This type of architecture boils down to what has been dubbed "Two speed IT".
In two speed IT a large cog (Citi) is powering a smaller faster cog (JoinPay).
The two companies intersect via APIs to deliver a compelling product.
In our specific case the large cog will expose restful APIs which Joinpay will leverage to implement their bill splitting application.
The small cog is able to iterate quickly and focus on specific functionality.
Their quick speed is in part due to their small size (employee wise), but also their focused scope of a UI driven product.
The large cog is able to concentrate on the slower moving parts such as DB compliance, availability and privacy.
An architecture that implements the features we need is in the figure below:

###Two Speed IT
![Architecture](./imgs/arch_img.png)
	
##User Story
Lets first flush out the particualr user story we want to fulfill:

A user, let's call him Bob, goes out for dinner with some friends including Alice.
Bob picks up the tab and now wants to receive payment from Alice.
JoinPay allows Bob to select users that are nearby which includes Alice.
He can then type the total and split the bill among his selected friends.
The recipients are then notified of pending transactions via push notifications, and can approve or deny the transaction.
When a transaction is approved JoinPay will use the Citi APIs to make the money transfer.

##How we built it
The first step to making the JoinPay story a reality was to implement a backend.
A "backend" in this case was a collection of REST APIs and an accompanying database.
This allows the data provided by the instance of JoinPay on Bob's phone to be utilized by the instance running on Alice's phone.
Specifically, part of the story is to identify when Alice and Bob have sat down together at a restaurant. 
Android's APIs allow for the collection of the geocordinates, but for the instance on Bob's phone to know that Alice is nearby, Alice's
instance must report her position, and Bob's instance must be able to retrieve this information.
<br/>
<br/>
At this point, you may be thinking: "But why does this involve using an API? Couldn't I just have each app instance connect to a shared database in the cloud?" 
You could.
However, this design choice reveals an unreasonable amount of information about each user to the other users.
For example, why does Alice need to know Bob's location?  She really just needs to know if he's nearby.
The API can provide this safer level of abstraction.
<br/>
<br/>
Utilizing a REST API also allows us to separate development to multiple developers (everybody grab an API and go!).
First, we established an agreement between developers on what the specifications for the API will look like.
The specs identify aspects of the api such as what URLs provide which information, what parameters are required for each endpoint, and the format of each.
We used a representation framework for REST APIs called <a href="http://swagger.io">Swagger</a> to communicate and broadcast the specifications for each API to each other.
Swagger allowed us to focus on the inputs and outputs of the API rather than the implementation details.
We authored our swagger specification, using version control to track and share it.
One we agreed on a final form of the spec, the backend team could focus on implementing the API, and the UI team could focus on integrating the story-based UI with the backend.
We agreed on the following specification:

## API Documentation (Swagger)
![swagger](./imgs/swagger_img.png)
After that we chose how to structure our backend, we need to choose how to implement it.
We chose to use <a href="https://nodejs.org/">Node.js</a> within <a href="http://bluemix.net">Bluemix</a>.
<a href="https://nodejs.org/">Node.js</a> with <a href="http://expressjs.com/">Express</a> is a great platform for developing REST APIs quickly and easily.
<a href="http://bluemix.net">Bluemix</a> is a IBM's PaaS Solution that allows us to host our running Node.js code without us having to setup a host machine/VM.
The <a href="https://github.com/cloudfoundry/nodejs-buildpack">Node.js buildpack</a> allowed us to simply push our Node.js code along with a package.json file and end up with a running API in the cloud.
<a href="http://bluemix.net">Bluemix</a> also enables our app to scale at the literal push of a button.
By simply increasing the number of app instances it will deploy a load-balanced environment to meet our user demand.
Not only that, but when it comes to databases selection we have several options that are already integrated in <a href="http://bluemix.net">Bluemix</a>.
We choose to use <a href="https://www.ng.bluemix.net/docs/#services/Cloudant/index.html#Cloudant">Cloudant</a> as our database solution because of its great high availability, and ease of use.
Now that all of the structure is decided let's see the architecture diagram:
<br/>

We will not bore you with every API endpoint in detail, but its probably worth it to examine one or two.
Our APIs are secured by requiring a session ID that is only achieved after the user has logged in.
The Login API is listed below:

- POST /login
	- This api expects the username and password to be in the body of the message.  It should be formatted as JSON.
	- A small bit of code is needed to enforce HTTPS in our Node.js application.  This will encrypt our request and keep it's body hidden from prying eyes.

	
<br/>

###Login Code###
	app.post('/login', function (req, res){
		res.set("Content-Type", "application/json")
		var db = new PouchDB(dbConnectionString + "/users")
		
		/**
		* You'got to at least send user a username and password.
		*/
		if (! req.body.hasOwnProperty('username') ||
			! req.body.hasOwnProperty('password'))
		{
			res.status(400).end(JSON.stringify({"message":"Incorrect request. Please specify a username and password."}))
		}
		else //correctly formed request.
		{
			/**
			* Access the user database to check and see if they sent the right password.
			*/
			db.get(req.body.username, function(err, doc) {
				if (err && err.status != 404)
				{
					res.status(500).end(JSON.stringify({
						"message":	"There was a problem communicating with the database while logging in.",
						"error"	 :	err
						}))
				}
				
				/**
				* Is the user even IN the database?
				*/
				if (typeof doc == 'undefined')
				{
					res.status(403).end(JSON.stringify({"message":"Incorrect username or password"}))
				} 
				else //Yeah, user's in the database
				{
					/**
					* Did they have the right password?
					*/
					if (req.body.password === doc.password)
					{
						/**
						* They did, so set the session object to be this user, and
						* reply with OK.  The session cookie will be set automatically
						* by the middleware.
						*/
						req.session.user = doc
						res.end(JSON.stringify({"message":"OK"}))
					}
					else //Wrong password.
					{
						/**
						* Make it indistinguishable whether the username or password was incorrect.
						*/
						res.status(403).end(JSON.stringify({"message":"Incorrect username or password"}))
					}
				}
			})
		}
	})
	
<br/>

- GET /transactions
	- This api expects nothing!  Since all API calls are secured with a unique session ID the backend will know who is calling and can look up the requester's known transactions.
	- Transactions in this context are pending/approved/denied payment transfers.  Each is stored as its own document in a 'transactions' database within Cloudant.
	- It will respond with a JSON payload containing an array of transaction details.
	
###Transactions Code
	app.get('/transactions', function(req, res) {
		res.set("Content-Type", "application/json")
		var transdb = new PouchDB(dbConnectionString + "/transactions")
		transdb.allDocs({include_docs: true}, function (err, response) {
			/**
			* There may be some problem loading the documents.
			*/
			if (err)
			{
				res.status(500).end(JSON.stringify({
					"message":"An error occurred while looking up transactions.",
					"error": err
						}))
			}
			else
			{
				/**
				* We'll return an empty object if there's nothing in the table
				* for this particular user.
				*/
				var toRet = {
						moneyIn: [],
						moneyOut: []
				}
				/**
				* Iterate through the things in the table; if the logged in user
				* is either the FROM user or the TO user, add the doc to the list
				* in the appropriate part of the data structure.
				*/
				response.rows.forEach(function (element, index, array) {
					/**
					* Money in is if he is the TO user,
					* Money out is if he is the FROM user.
					*/
					if (element.doc.toUser === req.session.user._id)
					{
						toRet.moneyIn.push(element.doc)
					}
					else if (element.doc.fromUser === req.session.user._id)
					{
						toRet.moneyOut.push(element.doc)
					}
				})
				
				/**
				* If there were no errors, go ahead and send the response.
				*/
				res.end(JSON.stringify(toRet))
			} //else the databse lookup was OK
		})
	})


All the other APIs were created in a similar manner.
Since most APIs are independent they can be divvied up to speed up development.



Now that our first 2 tasks are taken care of let's look at how we would implement Push Notifications.
Well, just like before, <a href="http://bluemix.net">Bluemix</a> comes to save the day with its built-in <a href="https://www.ng.bluemix.net/docs/#services/push/index.html#gettingstarted">Push Service</a>.

##IBM Push
![IBM Push](./imgs/push_img.png)
<br/>

Great, so once we add the tile we can configure it to work with our Google Developer Account (it can also do iOS if you're into that kind of thing).
If you open the Push tile there will be a link to starter code that shows how to integrate the reception of a Push Notification.

As a bonus there is an ability to manage/test push notifications in the dashboard of this tile.
From here we can send push to specific devices, device users, device categories, or to topics (devices can subscribe to topics in their native code).

On the initiating side of things we have a JS function to trigger the push notification seen below.

###Trigger Push Notification Code
	function sendPushNotification(sendTo, messageToSend) {
		var options = { 
			host: "mobile.ng.bluemix.net",
			path: "push/v1/apps/[our app key here]/messages",
			method: "POST",
			headers: {
				'Content-Type': 'application/json',
				'IBM-Application-Secret': '[our app secret here]'
			}
		};
	
		var myReq = https.request(options, function(myRes) {
			var responseString = '';
	
			myRes.on('data', function(data) {
				responseString += data;
			});
	
			myRes.on('end', function() {
				try
				{
					responseObject2 = JSON.parse(responseString)
					console.log("Sent push notification and got response: " + JSON.stringify(responseObject2))
				}
				catch (err)
				{
					console.log("Error while sending push notification: " + err)
				}
			});
		});
		var notification = 
		{
				"message": 
				{
					"alert": messageToSend
				},
				"target":
				{
					"consumerIds":
						[ 
							{"consumerId": sendTo } 
						]
				}
		}
		console.log("Sending push notification: " + JSON.stringify(notification))
		myReq.write(JSON.stringify(notification))
		myReq.end()
	}

It's simply another RESTFUL endpoint.  
It's actually a rather straightforward REST call using Node.js's "request" module.
First an "options" object is set up that contains critical fields for describing the HTTP request such as "host", "path", "method", and 'headers".
The payload of the request is setup with the line myReq.write(JSON.stringify(notification)); where notification is an object containing the text of the notification.
Then it's just a matter of implementing the "requests" module's way of receiving the data, and finally initiating the call itself with myReq.end().

Next on our list is to find nearby users.
We decided to implement this in our Node.js APIs with two simiple endpoints:
- PUT /currentLocation
	- This api expects the location to be in the body of the message.  Formatted as JSON with the fields "latitude" and "longitude".
- GET /nearby/users
	- This api expects nothing!  Since all API calls are secured with a unique session ID the backend will know who is calling and can look up the requester's last known location.
	- It will respond with a JSON payload containing an array of usernames and their distances to the requester's location.


Some basic Android code is needed to periodically talk to the device's GPS and receive latitude and longitude coordinates. 
Then its just a matter of invoking the API to update the user's location, and periodically call GET /nearby/users to update the user map.

Nearly there now.  This may be the most important part of the whole application. 
It's time to interact with Citi Bank's APIs to move money!
Unfortunately they do not exist in a usable form for this application.
This may seem like a major deal-breaker, but's actually quite common of a problem.
By leveraging a rapid development model, you may often find yourself ahead of the development of other projects that yours depends on.
Waiting is one option, but that's boring.
When the road runs out our group's preference is to start building the road our self.
Therefore we spin up another Node.js app, and start writing our own implementation of the Citi Bank APIs.
Obviously we do not have real money to move around, so from a money perspective these APIs are fake.
In every other regard these APIs are very real.  They know about users, accounts, balances, transactions, and security elements (authorization).
Ideally we built these APIs as close to the real thing as possible, such that flipping to the real ones involves only changing the endpoint's URL.
After all from JoinPay's perspective the Citi APIs are just a black box.
Below is one example of calling our Citi APIs.
In particular this one deals with getting the user's Citi account information.

###Citi Account Balance Code
	app.get('/myAccount', function(req, res) {
		res.set('Content-Type', 'application/json')
		if (req.session.user.default_account == "")
		{
			res.status(404).end(JSON.stringify({"message":"You do not currently have an associated account."}))
		}
		else //don't do any of this without an associated account
		{
			var options = { 
				host: "[url to citi apis here]",
				path: "/api/account/" + req.session.user.default_account,
				method: "GET",
				headers: {
					'Content-Type': 'application/json',
					'API-Auth-Token': '[token here]'
				}
			};
		
		var myReq = http.request(options, function(myRes) {
			var responseString = '';
		
			myRes.on('data', function(data) {
				responseString += data;
			});
		
			myRes.on('end', function() {
				try
				{
					console.log(responseString)
					responseObject2 = JSON.parse(responseString)
					console.log("Communicated with Citi backend and received response: " + JSON.stringify(responseObject2))
					res.send(responseObject2)
				}
				catch (err)
				{
					res.status(500).end(JSON.stringify({
						"message":"An error occurred while communicating with the Citi APIs",
						"error": JSON.stringify(err)}))
				} //catch
			}) //myres.on
		}) //myreq
		console.log("Sending request for account information")
		myReq.end()
		} //else they have an associated account
	}) //put approve/deny

It's another straightforward REST call using Node.js's "request" module.
The very first thing we do is inspect the user's session to see if there is currently a known Citi account id.
This would have been populated when the user logged in.
Next an "options" object is set up that contains critical fields for describing the HTTP request such as "host", "path", "method", and 'headers" (same as before).
Then it's just a matter of implementing the "requests" module's way of receiving the data, and finally initiating the call itself with myReq.end().
When the request is successful it will attempt to parse the result from Citi (looking for JSON format) and if that's also successful it will send the JSON back to the app that made the original call.

Finally we come to the last task which was to build it as fast as possible.
The time saving efforts were largely possible with <a href="http://bluemix.net">Bluemix</a> because:
1. We never had to configure a host machine to run our code.
2. We never have to figure out how to load balance our code instances.
3. We got starter code to hit the ground running for our Node.js apps and Push Notification
4. We never had to hardcode/protect our database credentials
5. We never had to build a monitoring service to keep our service up should it crash

##Final Words
Development at lightning speed can have its pitfalls and limitations, but if you plan accordingly and leverage tool sets such as those found in <a href="http://bluemix.net">Bluemix</a> its quite possible.
The end result of our endeavor is quite positive.
If we did this again we would like to use some of the other services found in the Bluemix catalog such as the debugging service for mobile apps: 
<a href="https://mqedg.mybluemix.net/MQEHelp.jsp#">Mobile Quality Extension</a>.
