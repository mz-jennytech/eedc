/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.etz.phcn.eedc.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.log4j.Logger;


/**
 *
 * @author matthew.abikoye
 */
public class EEDCService {
    //private String accessCode;
    private Date validity;
    
   // private String username;
    //private String password;
    private String apikey;
    private String baseUrl;
    private Logger logger;
    private String origin;
   // private String auth_url;

    public EEDCService(String apikey, String baseUrl,Logger logger,String origin) {
        this.apikey = apikey;
        this.baseUrl = baseUrl;
        this.logger = logger;
        this.origin = origin;
    }
    
    

    public EEDCService() {
//        username = "09cc4f8d76f2_demo";
//            password = "bCG#v4*Mj0*TZb9_g7uW^";
//            baseUrl = "http://api.kvg.com.ng/";
//            hash = "0192572A2678DA5ADC5CD8B9B";
    }
    
    
//    private void getAccessCode() throws Exception{
//        String req = "{\"username\":\""+username+"\",\"password\":\""+password+"\"}";
//        if(accessCode == null || validity == null || validity.compareTo(new Date())<=0){
//            logger.info("GETTING ACCESS CODE");
//            String resp = HttpUtil.sendPost(auth_url, req);
//            JsonParser parser = new JsonParser();
//            JsonObject obj = parser.parse(resp).getAsJsonObject();
//            accessCode = obj.get("accessCode").getAsString();
//            String valid = obj.get("validUntil").getAsString();
//            SimpleDateFormat sdf = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z");
//            validity = sdf.parse(valid);
//            logger.info("SUCCESSFULLY GOT ACCESS CODE");
//        }
//    }
    
    public String[]validateMeter(String meter,String paymentPlan) throws Exception{
        //getAccessCode();
        String request =baseUrl+"customer/detail?accountNumber="+meter+"&paymentPlan="+paymentPlan;
        logger.info("CALLING ENUGU VENDING GATEWAY FOR METER VALIDATION ");
        logger.info("CALLED URL :: "+request);
        logger.info(apikey);
        String[]resp = HttpUtil.sendGet(request, apikey,origin);
        logger.info("RESPONSE FROM ENUGU VENDING GATEWAY :: "+resp[1]);
//        JsonParser parser = new JsonParser();
//        JsonObject obj = parser.parse(resp).getAsJsonObject();
    
        return resp;
    }
    
    public String[]requery(String unique_transid) throws Exception{
        //getAccessCode();
        String request = baseUrl+"payment/detail?transactionRef="+unique_transid;
        logger.info("CALLING ENUGU VENDING GATEWAY FOR REQUERY");
        logger.info("CALLED URL :: "+request);
        String[]resp = HttpUtil.sendGet(request, apikey,origin);
        logger.info("RESPONSE FROM ENUGU VENDING GATEWAY :: "+resp[1]);
//        JsonParser parser = new JsonParser();
//        JsonObject obj = parser.parse(resp).getAsJsonObject();
        return resp;
    }
    
    public String[]reversal(String unique_transid) throws Exception{
        //getAccessCode();https://dev.myeedc.com/cashcollection/business/api/payment/reverse
        String url = baseUrl+"payment/reverse";
        String request = "{\"transactionRef\":\""+unique_transid+"\"}";
        logger.info("CALLING ENUGU VENDING GATEWAY FOR TOKEN VENDING");
        logger.info("CALLED URL :: "+request);
        String[]resp = HttpUtil.sendPost(url, request, apikey,origin,8);
        logger.info("RESPONSE FROM ENUGU VENDING GATEWAY :: "+resp[1]);
//        JsonParser parser = new JsonParser();
//        JsonObject obj = parser.parse(resp).getAsJsonObject();
        return resp;
    }
    
    public String[] vendPin(JsonObject request, int timeout) throws SocketTimeoutException, Exception{
        //getAccessCode();
        String url = baseUrl+"payment/pay";
        logger.info("CALLING ENUGU VENDING GATEWAY FOR TOKEN VENDING");
        logger.info("CALLED URL :: "+url);
        String[]resp = HttpUtil.sendPost(url, request.toString(), apikey,origin, timeout);
        logger.info("RESPONSE FROM ENUGU VENDING GATEWAY :: "+resp[1]);
//        JsonParser parser = new JsonParser();
//        JsonObject obj = parser.parse(resp).getAsJsonObject();
        return resp;
    }
}


				try {
		     		Properties prop = new Properties();
		     		String PropFileName = "application.properties";
		     		inputStream = getClass().getClassLoader().getResourceAsStream(PropFileName);
		     		if (inputStream != null) {
		     			prop.load(inputStream);
		     						}
		     		else {
		     			throw new FileNotFoundException("Property file" + PropFileName+ "not found int the classpath ");
		     							}
		     		apikey	= prop.getProperty("Apikey");
		     		System.out.println("-----Apikey----" + apikey);
		     		origin	= prop.getProperty("origin");
		     		System.out.println("-----Origin----" + origin);
		     		USER_AGENT	= prop.getProperty("USER_AGENT");
		     		System.out.println("----USER_AGENT----" + USER_AGENT);
		     		
		     	}
		     	catch (Exception e) {
		     		System.out.println("Exception: "+ e);
		     				}finally {
		     					inputStream.close();
		     				}
		       