/**
 * 
 */
package com.stbutler.TradeBot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONObject;
import org.json.JSONTokener;


/**
 * @author Stbot
 *
 */
public class btceAccess {
	private static String apiKey;
	private static String apiSecret;
	private static long nonce;
	
	 public btceAccess(String key, String secret){
		setApiKey(key);
		setApiSecret(secret);
		nonce = (System.currentTimeMillis()/1000);
	}
	 
	
	 
	 @SuppressWarnings("rawtypes")
	public JSONObject  accountInfoRequest(HashMap<String,String> params, String request) throws NoSuchAlgorithmException, InvalidKeyException, ClientProtocolException, IOException{
		 
		 String postBody = "";
		 
		 
		 if( params == null) {  // If the user provided no arguments, just create an empty argument array.
	           params = new HashMap<String, String>();
	        }
		 incNonce();
		 
		 params.put( "method", request);  // Add the method to the post data.
	     params.put( "nonce",  "" + getNonce());  //incremented nonce added
	     
	     for( Iterator<Entry<String, String>> argumentIterator = params.entrySet().iterator(); argumentIterator.hasNext(); ) {
	            Map.Entry argument = (Map.Entry)argumentIterator.next();
	           
	            if( postBody.length() > 0) {
	                postBody += "&";
	            }
	            postBody += argument.getKey() + "=" + argument.getValue();
	        }
	     SecretKeySpec keySpec = new SecretKeySpec(apiSecret.getBytes("UTF-8"),"HmacSHA512");
		 Mac mac = Mac.getInstance( "HmacSHA512" );
		 mac.init( keySpec);
		 
		 
		 
		 
		 CloseableHttpClient client = HttpClientBuilder.create().build();
		 HttpPost post = new HttpPost("https://btc-e.com/tapi");
		 post.setHeader("Content-type" ,"application/x-www-form-urlencoded");
		 post.setHeader("Key", apiKey);
		 post.setHeader("Sign", Hex.encodeHexString( mac.doFinal( postBody.getBytes( "UTF-8"))));
		 StringEntity finalData = new StringEntity(postBody);
		 post.setEntity(finalData);
		 CloseableHttpResponse answer = client.execute(post);
		 
		 //System.out.println(answer.getEntity().getContent());
		 
		 BufferedReader reader = new BufferedReader(new InputStreamReader(answer.getEntity().getContent(), "UTF-8"));
		 StringBuilder builder = new StringBuilder();
		 for (String line = null; (line = reader.readLine()) != null;) {
		     builder.append(line).append("\n");
		 }
		 
		 //System.out.println(builder.toString());
		 JSONTokener tokener = new JSONTokener(builder.toString());
		 JSONObject finalResult = new JSONObject(tokener);
		 //System.out.println(finalResult);
				
		return finalResult;
		 
	 }
	 
	 public JSONObject publicInfoRequest(String currency, String method) throws ClientProtocolException, IOException {
		 CloseableHttpClient client = HttpClientBuilder.create().build();
		 HttpGet get = new HttpGet("http://btc-e.com/api/2/"+currency+"/"+method);
		 CloseableHttpResponse response = client.execute(get);
		 
		 
		 BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
		 StringBuilder builder = new StringBuilder();
		 for (String line = null; (line = reader.readLine()) != null;) {
		     builder.append(line).append("\n");
		 System.out.println(builder.toString());
		 }
		 
		 JSONTokener tokener = new JSONTokener(builder.toString());
		 JSONObject finalResult = new JSONObject(tokener);
		 

		 return finalResult;
		 
	 }

	 
	 
	 
	 
	 
	 
	 

	/**
	 * @param apiKey the apiKey to set
	 */
	public static void setApiKey(String apiKey) {
		btceAccess.apiKey = apiKey;
	}

	/**
	 * @param apiSecret the apiSecret to set
	 */
	public static void setApiSecret(String apiSecret) {
		btceAccess.apiSecret = apiSecret;
	}

	/**
	 * @return the nonce
	 */
	public static long getNonce() {
		return nonce;
	}

	/**
	 * @param nonce the nonce to set
	 */
	public static void incNonce() {
		btceAccess.nonce = nonce++;
	}
	
		
	
}
