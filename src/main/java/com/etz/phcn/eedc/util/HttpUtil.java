/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.etz.phcn.eedc.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.apache.log4j.Logger;

/**
 *
 * 
 */
public class HttpUtil {

   private static final Logger L = Logger.getLogger(HttpUtil.class);
   private final static String USER_AGENT = "Mozilla/5.0";

   public static String []sendGet(String url,String accesscode,String origin) throws Exception {

      URL obj = new URL(url);
      HttpURLConnection con = (HttpURLConnection) obj.openConnection();

      // optional default is GET
      con.setRequestMethod("GET");

      //add request header
      con.setRequestProperty("User-Agent", USER_AGENT);
      con.setRequestProperty("Authorization", "Bearer "+accesscode);
      con.setRequestProperty("Origin", origin);

      int responseCode = con.getResponseCode();
      L.info("Sending 'GET' request to URL : " + url);
      L.info("Response Code : " + responseCode);

      BufferedReader in = new BufferedReader(
      new InputStreamReader(con.getInputStream()));
      String inputLine;
      StringBuffer response = new StringBuffer();

      while ((inputLine = in.readLine()) != null) {
         response.append(inputLine);
      }
      in.close();
      String[]resp = {responseCode+"",response.toString()};
      //print result
      L.info(response.toString());
      return  resp;
   }

   public static String[]sendPost(String url, String jsonString,String accesscode,String origin, int timeout) throws Exception {
      System.out.println("\nSending 'POST' request to URL : " + url);
      System.out.println("Post parameters : " + jsonString);
      URL obj = new URL(url);
      //HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
      HttpURLConnection con = (HttpURLConnection) obj.openConnection();
      //con.setConnectTimeout(500);
      con.setReadTimeout(timeout*1000);
      //add reuqest header
      con.setRequestMethod("POST");
      con.setRequestProperty("User-Agent", USER_AGENT);
      con.setRequestProperty("Content-Type", "application/json");
      con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
      con.setRequestProperty("Authorization", "Bearer "+accesscode);
      con.setRequestProperty("Origin", origin);
      con.setDoOutput(true);
      DataOutputStream wr = new DataOutputStream(con.getOutputStream());
      wr.writeBytes(jsonString);
      wr.flush();
      wr.close();

      int responseCode = con.getResponseCode();
      System.out.println("Response Code : " + responseCode);

      BufferedReader in = new BufferedReader(
              new InputStreamReader(con.getInputStream()));
      String inputLine;
      StringBuffer response = new StringBuffer();

      while ((inputLine = in.readLine()) != null) {
         response.append(inputLine);
      }
      in.close();
      String[]resp = {responseCode+"",response.toString()};
      //print result
      return resp;

   }
}
