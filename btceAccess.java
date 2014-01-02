package com.roteware.btcetrader;

/**
 * API wrapper class for BTCE.com api v2 with convenience methods for api v3
 * 
 * @author Stbot
 */

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONObject;
import org.json.JSONTokener;


public class btceAccess {
	private String apiKey;
	private String apiSecret;
	private long nonce;
	
	
	/**
	 * constructor to set the apikey and secret
	 * 
	 * @param key  String key provided on your btce account
	 * 	
	 * @param secret  String secret provided on your btce account
	 */
	public btceAccess(String key, String secret) {

		setApiKey(key);
		setApiSecret(secret);
		nonce = (System.currentTimeMillis() / 1000);
	}

	/**
	 * @param apiKey
	 *            the apiKey to set
	 */
	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	/**
	 * @param apiSecret
	 *            the apiSecret to set
	 */
	public void setApiSecret(String apiSecret) {
		this.apiSecret = apiSecret;
	}
	
	
	/**
	 * constructor using a file to set the key and secret
	 * 
	 * @param configname  the name of the configuration file containing a key and secret combination
	 * 			currently they should be separated by a line containing anything.
	 * 
	 * @throws FileNotFoundException
	 */
	public btceAccess(String configname) throws IOException {
		Properties prop = new Properties();
		prop.load(new FileInputStream(configname));
		
		this.apiKey = prop.getProperty("key");
		this.apiSecret = prop.getProperty("secret");
		this.nonce = (System.currentTimeMillis() / 1000);
		

	}

	/**
	 * 
	 * Performs an Http POST request to BTCE. requests are encoded using the secret provided for each account.
	 * responses are in Json format.
	 * 
	 * @param params  HashMap of Parameters to be encoded in the HTTP header
	 * @param request  String Request type, options specified on the API page
	 * @return	a JSON object containing the response from BTCE
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	@SuppressWarnings("rawtypes")
	public final JSONObject accountInfoRequest(HashMap<String, String> params,
			String request) throws NoSuchAlgorithmException,
			InvalidKeyException, ClientProtocolException, IOException {

		String postBody = "";

		if (params == null) { // If the user provided no arguments, just create
								// an empty argument array.
			params = new HashMap<String, String>();
		}

		params.put("method", request); // Add the method to the post data.
		params.put("nonce", "" + nonce++); // incremented nonce added
		
		//iterate our params and format them for POST
		for (Iterator<Entry<String, String>> argumentIterator = params
				.entrySet().iterator(); argumentIterator.hasNext();) {
			Map.Entry argument = argumentIterator.next();

			if (postBody.length() > 0) {
				postBody += "&";
			}
			postBody += argument.getKey() + "=" + argument.getValue();
		}
		
		//build mac from apiSecretKey 
		SecretKeySpec keySpec = new SecretKeySpec(apiSecret.getBytes("UTF-8"),
				"HmacSHA512");
		Mac mac = Mac.getInstance("HmacSHA512");
		mac.init(keySpec);
		
		//Open HTTP client add paramas and key to headers encrypted and POST
		CloseableHttpClient client = HttpClientBuilder.create().build();
		HttpPost post = new HttpPost("https://btc-e.com/tapi");
		post.setHeader("Content-type", "application/x-www-form-urlencoded");
		post.setHeader("Key", apiKey);
		post.setHeader("Sign",
				Hex.encodeHexString(mac.doFinal(postBody.getBytes("UTF-8"))));
		StringEntity finalData = new StringEntity(postBody);
		post.setEntity(finalData);
		CloseableHttpResponse answer = client.execute(post);

		//Read response and build a string to send to JSON tokener
		BufferedReader reader = new BufferedReader(new InputStreamReader(answer
				.getEntity().getContent(), "UTF-8"));
		StringBuilder builder = new StringBuilder();
		for (String line = null; (line = reader.readLine()) != null;) {
			builder.append(line).append("\n");
		}
		
		//tokenise our response string and build a Json Object with it
		JSONTokener tokener = new JSONTokener(builder.toString());
		JSONObject finalResult = new JSONObject(tokener);
		//System.out.println(finalResult);
		
		//return our JSON Object
		return finalResult;

	}

	/**
	 * 
	 *  performs a Http GET request to BTCE retrieving publicly available information 
	 * 	No key or Secret is send. responses are in JSON format.
	 * 
	 * @param currency 
	 * 			one of the valid currency strings from the site
	 * @param method
	 * 			a string that is a request method. "Ticker" or "Trades" for example
	 * 
	 * @return a JSON object containing the response from BTCE
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public JSONObject publicInfoRequest(String currency, String method,int version)
			throws ClientProtocolException, IOException {
		//Open HTTP Client and perform GET request from BTCE
		CloseableHttpClient client = HttpClientBuilder.create().build();
		HttpGet get = null;
		if(version==3){
			get = new HttpGet("http://btc-e.com/api/3/" + method + "/"
						+ currency);
		}else{
			 get = new HttpGet("http://btc-e.com/api/2/" + currency + "/"
					+ method);
		}
		
		CloseableHttpResponse response = client.execute(get);
		
		//Read the response into a String for the Tokener
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				response.getEntity().getContent(), "UTF-8"));
		StringBuilder builder = new StringBuilder();
		for (String line = null; (line = reader.readLine()) != null;) {
			builder.append(line).append("\n");
			//System.out.println(builder.toString());
		}
		
		//Tokenise and Construct a JSON Object
		JSONTokener tokener = new JSONTokener(builder.toString());
		JSONObject finalResult = new JSONObject(tokener);
		
		//return JSON Object
		return finalResult;

	}
	
	/*
	 * methods for accountInfoRequest
	 * getInfo
	 * TransHistory
	 * TradeHistory
	 * ActiveOrders
	 * Trade
	 * Cancel Order
	 * 
	 */

	/**
	 * 
	 * Convince Method to do a public ticker request (API 2)
	 * 
	 * @param currency String of currency pair to be queried 
	 * @return JsonObject response of the request
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public JSONObject tickerRequest(String currency) throws ClientProtocolException, IOException{
		return publicInfoRequest(currency, "Ticker", 2);
		
	}
	/**
	 * Convince Method to do a public trades request (API 2)
	 * 
	 * @param currency String of currency pair to be queried 
	 * @return JsonObject response of the request
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public JSONObject tradesRequest(String currency) throws ClientProtocolException, IOException{
		return publicInfoRequest(currency, "Trades",2);
		
	}
	/**
	 * 
	 * Convince Method to do a public depth request (API 2)
	 * 
	 * @param currency String of currency pair to be queried 
	 * @return JsonObject response of the request
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public JSONObject depthRequest(String currency) throws ClientProtocolException, IOException{
		return publicInfoRequest(currency, "depth",2);
		
	}
	
	/**
	 *  Convince Method to do a public info request (API 3)
	 * 
	 * @return Json Object of the response
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public JSONObject info3Request() throws ClientProtocolException, IOException{

		return publicInfoRequest(" ","info",3);
		
	}
	
	
	
	/**
	 * 
	 * Convince Method to do a public ticker request (API 3)
	 * 
	 * @param currency a String array of currencies to display
	 * @return Json Object of the response
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public JSONObject ticker3Request(String[] currency) throws ClientProtocolException, IOException{
		String params = null;
		if (currency.length>1){
			for(String x:currency){
				if (params.length()>0){
					params += "-";
				}
				params += x;
			}
		}
		return publicInfoRequest(params,"ticker",3);
		
	}
	/**
	 * 
	 * Convince Method to do a public depth request (API 3)
	 * 
	 * @param currency a String array of currencies to display
	 * @return Json Object of the response
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public JSONObject depth3Request(String[] currency) throws ClientProtocolException, IOException{
		String params = null;
		if (currency.length>1){
			for(String x:currency){
				if (params.length()>0){
					params += "-";
				}
				params += x;
			}
		}
		return publicInfoRequest(params,"depth",3);
		
	}
	
	
	/**
	 * 
	 * Convince Method to do a public trades request (API 3)
	 * 
	 * @param currency a String array of currencies to display
	 * @param limit the limit of entries per currency 0 for default(150) must be less than 2000
	 * @return Json Object of the response
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public JSONObject trades3Request(String[] currency, int limit) throws ClientProtocolException, IOException{
		String params = null;
		if (currency.length>1){
			for(String x:currency){
				if (params.length()>0){
					params += "-";
				}
				params += x;
			}
		}
		params += "?limit="+limit;
		return publicInfoRequest(params,"trades",3);
		
	}
}	
