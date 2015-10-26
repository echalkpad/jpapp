---
#Uber
---

<center>
![Uber](./imgs/uber.png)
</center>

[Uber](https://www.uber.com/) provides a great set of APIs for their transportation network. 
They provide a service for finding/paying for rides across most major cities.
Below is a few helpful reference links to get started. 
This tutorial will get you through the OAuth2 step and get a price estimate on a ride.

- [Authorization](https://developer.uber.com/v1/auth/)
- [API Documentation](https://developer.uber.com/v1/endpoints/)
- [Uber made Tutorials](https://developer.uber.com/v1/tutorials/)



---
##Setup
1. Start off by [signing](https://login.uber.com/login) up for a regular user account on Uber.
2. Then [register](https://developer.uber.com/dashboard) an application to create your developer account.

	![register](./imgs/register.png)

3. Now fill out your application profile and pay special attention to a few fields:
		- The redirect URL should point to some backend of your own. For testing purposes this can be localhost and the port number being used.
		- Check the correct permissions for your application.  For this tutorial we only need "profile".
		- The "CLIENT ID" and "CLIENT SECRET" fields (near the bottom) will be neede in a later step.  Copy them now for convince.
![app](./imgs/app.png)
---
##OAuth2 
OAuth2 can seem complicated but its really a straight forward two step process. 
It involves getting an authorizaiton code (step 1) and exchanging if for an access token (step 2).
Acquiring an authorization code takes the user's consent. 
Therefore your application should send them to an Uber authorization page.
In android this can be done from within your application with a [webview](http://developer.android.com/reference/android/webkit/WebView.html) or by breaking out of your app with the full browser.
They user will be prompted by Uber to allow your app to his account and the user will need to click an accept button.
Then Uber will redirect to the "redirect URI" you provided in your app's registration.
This redirect will contain the "code" as a parameter.
Finally you can exchange this code for an access token by sending an HTTP request to an Uber API endpoint.
 
1. First up is to send the user to https://login.uber.com/oauth/v2/authorize with the following parameters attached
	- response_type - This should be set to "code" without the quotes
	- client_id	- This can be found in the details of your Uber application
	- For testing purposes you can manually create this link and try it out in your browser!  It would look like:
	
			https://login.uber.com/oauth/v2/authorize?response_type=code&client_id=YOUR_CLIENT_ID_HERE

	####Response: 
	Once the user accepts, Uber will redirect to the link you provided in the "redirect URI" field in your Uber application profile.
	- code - The redirect will appear with a query parameter "code" which is needed in the next step.
	- (Your app's backend will see this as a normal HTTP GET request)
	
2. Next we need to exchange this code into an access token by sending a HTTP POST to https://login.uber.com/oauth/v2/token. 
You will need to include a few body parameters:
	- client_secret - This can be found in the details of your Uber application
	- client_id - This can be found in the details of your Uber application
	- grant_type - This should be set to "authorization_code" without the quotes
	- redirect_uri - This can be found in the details of your Uber application
	- code - This must be set to the value you got back in step 1.
	
	####Response
	Uber will respond back with the following JSON object:
	
		{
			"access_token": "EE1IDxytP04tJ767GbjH7ED9PpGmYvL",
			"token_type": "Bearer",
			"expires_in": 2592000,
			"refresh_token": "Zx8fJ8qdSRRseIVlsGgtgQ4wnZBehr",
			"scope": "profile history"
		}

	Store the access_token and refresh_token for later user. 
	Access tokens expire in 30 days.
	Exchange the refresh token to generate a new access and refresh token.
	
---
##Get a Quote
We are almost there now.
To get a quote we are going to use the Price Estimates [endpoint](https://developer.uber.com/v1/endpoints/).

1. Create a HTTP GET request with the following query parameters:
	
	####Header
	- Authorization - the authorization header should follow OAuth2 spec:
			Authorization: Basic YOUR_ACCESS_TOKEN_HERE
	
	####Query Parameters:
	- start_latitude - latitude of start location as float
	- start_longitude - longitude of start location as float
	- end_latitude - latitude of end location as float
	- end_longitude - longitude of end location as float
	
	####Response
	Uber will response back with the following JSON object. 
	This response contains the price estimate from UberBLACK, UberSUV, and UberX.
	
	
		{
			"prices": [
				{
				"product_id": "08f17084-23fd-4103-aa3e-9b660223934b",
				"currency_code": "USD",
				"display_name": "UberBLACK",
				"estimate": "$23-29",
				"low_estimate": 23,
				"high_estimate": 29,
				"surge_multiplier": 1,
				"duration": 640,
				"distance": 5.34
				},
				{
				"product_id": "9af0174c-8939-4ef6-8e91-1a43a0e7c6f6",
				"currency_code": "USD",
				"display_name": "UberSUV",
				"estimate": "$36-44",
				"low_estimate": 36,
				"high_estimate": 44,
				"surge_multiplier": 1.25,
				"duration": 640,
				"distance": 5.34
				},
				{
				"product_id": "aca52cea-9701-4903-9f34-9a2395253acb",
				"currency_code": null,
				"display_name": "uberTAXI",
				"estimate": "Metered",
				"low_estimate": null,
				"high_estimate": null,
				"surge_multiplier": 1,
				"duration": 640,
				"distance": 5.34
				},
				{
				"product_id": "a27a867a-35f4-4253-8d04-61ae80a40df5",
				"currency_code": "USD",
				"display_name": "uberX",
				"estimate": "$15",
				"low_estimate": 15,
				"high_estimate": 15,
				"surge_multiplier": 1,
				"duration": 640,
				"distance": 5.34
				}
			]
		}
---
##Final Words
Uber's APIs are pretty simple and straightforward.
I liked the full suite of APIs they offer.
Their JSON responses are very descriptive which makes them developer friendly. 
Once your application has the access_token making requests is quite easy.
A real production app should keep tabs on the access token's expiration.
You will need to use the refresh token to regenerate a new access_token when the old one expires.
