/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.etz.phcn.eedc.core;

import com.etz.phcn.eedc.util.EEDCService;
import com.etz.phcn.utils.DiscoProcessor;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Properties;
import org.apache.log4j.Logger;
import com.etz.vasgate.lib.RedisUtility;

/**
 *
 * @author matthew.abikoye
 */
public class EEDCProcessor extends DiscoProcessor {

    private static org.apache.log4j.Logger logger = null;
    public static String username;
    public static String password;
    public static String baseUrl;
    public static String apikey;
    public static String auth_url;
    public static String origin;
    private static Properties prop;
    private final String phone;
    private static int timeout;
    private static int requeryDelay;
    private static int lookupRetry;
    private final String phoneRegex;
    //private static double maxAmount;
    private final EEDCService service;

    //private static String runningMode;
    // private Logger logger;
    static {
        prop = new Properties();
        try {
            prop.load(new FileInputStream(new File("cfg/phcndb-config.properties")));
            //runningMode = prop.getProperty("RUNNING_MODE");
            baseUrl = prop.getProperty("ENU_BASEURL");
            apikey = prop.getProperty("ENU_API_KEY");
            origin = prop.getProperty("ENU_ORIGIN");
            timeout = Integer.parseInt(prop.getProperty("ENU_TIMEDOUT"));
            requeryDelay = Integer.parseInt(prop.getProperty("ENU_LOOKUP_DELAY"));
            lookupRetry = Integer.parseInt(prop.getProperty("ENU_LOOKUP_RETRY"));
            //maxAmount = Double.parseDouble(prop.getProperty("ENU_MAX_AMOUNT"));

//            username = "09cc4f8d76f2_demo";
//            password = "bCG#v4*Mj0*TZb9_g7uW^";
//            baseUrl = "https://api.kvg.com.ng/";
//            hash = "0192572A2678DA5ADC5CD8B9B";
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public EEDCProcessor(Logger log) {
        logger = log;
        service = new EEDCService(apikey, baseUrl, log, origin);
        phone = prop.getProperty("ENU_DEFAULT_MOBILE");
        phoneRegex = prop.getProperty("PHONE_REGEX");
    }

    @Override
    public String doPostPaidCustomerInfo(JsonObject jsonData) {
        // In Transaction Post method
        String result;
        String accountOrMeterNo = jsonData.get("accountNumber").getAsString();
        String accNum=accountOrMeterNo;
        String uniqueTransId = jsonData.get("uniqueTransId").getAsString();
        JsonObject verificationDataObj = new JsonObject();
        logger.info("ACCOUNT BEFORE FORMAT :: " + accountOrMeterNo);
        if (accountOrMeterNo.length() == 12) {
           accNum = accountOrMeterNo.substring(0, 2) + "/" + accountOrMeterNo.substring(2, 4) + "/" + accountOrMeterNo.substring(4, 6) + "/" + accountOrMeterNo.substring(6, 10) + "-" + accountOrMeterNo.substring(10);

        }

        verificationDataObj.addProperty("requestType", "verification");
        verificationDataObj.addProperty("disco", "ENU");
        verificationDataObj.addProperty("accountType", "Postpaid");
        verificationDataObj.addProperty("uniqueTransId", uniqueTransId);
        verificationDataObj.addProperty("accountNumber", accNum);

        try {
            logger.info("CALLING ENUGU");
            logger.info("ACCOUNT :: " + accNum);
            String[] postResponse = service.validateMeter(accNum, "Postpaid");
            logger.info("RESPONSE CODE ==== " + postResponse[0]);
            if (postResponse[0].equalsIgnoreCase("200")) { //Not Timeout

                //logger.info("ENU POSTPAID VERIFICATION RESPONSE : " + postResponse[1]);
                JsonObject responseObj = new JsonParser().parse(postResponse[1]).getAsJsonObject();
                int code = responseObj.get("responseCode").getAsInt();
                String message = responseObj.get("responseMessage").getAsString();
                if (code == 200) {
                    JsonObject customerDetal = responseObj.getAsJsonObject("customer");
                    //JsonObject meterDetail = responseObj.getAsJsonObject("MeterDetail");
                    String firstname;

                    try {
                        firstname = customerDetal.get("firstName").getAsString();
                        logger.info("LAST NAME :: " + firstname);
                    } catch (Exception e) {
                        firstname = "";
                    }
                    String lastname;
                    try {
                        lastname = customerDetal.get("lastName").getAsString();
                        logger.info("LAST NAME :: " + lastname);
                    } catch (Exception e) {
                        lastname = "";
                    }
                    String name = firstname + " " + lastname;
                    verificationDataObj.addProperty("meterNumber", customerDetal.get("meterNumber").getAsString());
                    verificationDataObj.addProperty("customerName", name);
                    String customerAddress;
                    try {
                        customerAddress = customerDetal.get("address").getAsString();
                    } catch (Exception e) {
                        customerAddress = "";
                    }
                    String district;
                    try {
                        district = customerDetal.get("district").getAsString();
                    } catch (Exception e) {
                        district = "";
                    }
                    verificationDataObj.addProperty("customerAddress", customerAddress);
                    verificationDataObj.addProperty("businessUnit", district + " BUSINESS UNIT");
                    String state;
                    try {
                        state = customerDetal.get("state").getAsString();
                    } catch (Exception e) {
                        state = "";
                    }
                    verificationDataObj.addProperty("state", state);
                    verificationDataObj.addProperty("minimumPurchase", "0");
                    verificationDataObj.addProperty("customerArrears", customerDetal.get("arrearsBalance").getAsString());
                    verificationDataObj.addProperty("externalReference", "");
//                    verificationDataObj.addProperty("phone", "");
                    verificationDataObj.addProperty("errorCode", code);
                    verificationDataObj.addProperty("responseCode", "00");
                    verificationDataObj.addProperty("responseDesc", message);
                    verificationDataObj.addProperty("email", "");
                    verificationDataObj.addProperty("tariffRate", customerDetal.get("tariffRate").getAsString());
                    verificationDataObj.addProperty("phoneNumber", "");
                    verificationDataObj.addProperty("tariff", customerDetal.get("tariffRate").getAsString());
                    //verificationDataObj.addProperty("tariffIndex", meterDetail.get("TariffIndex").getAsString());
                    verificationDataObj.addProperty("tariffCode", customerDetal.get("tariffCode").getAsString());
                    verificationDataObj.addProperty("customerType", "");

                    //verificationDataObj.addProperty("minVendAmount", responseObj.get("MinVendAmount").getAsString());
                    //verificationDataObj.addProperty("maxVendAmount", responseObj.get("MaxVendAmount").toString());
                } else {
                    verificationDataObj.addProperty("errorCode", code);
                    verificationDataObj.addProperty("responseCode", "56");
                    verificationDataObj.addProperty("responseDesc", code + " - " + message);
                }

            } else {
                verificationDataObj.addProperty("responseCode", "06");
                verificationDataObj.addProperty("responseDesc", "Request Timeout");
            }

        } catch (Exception e) {
            logger.error("Error : ", e);
            verificationDataObj.addProperty("responseCode", "56");
            verificationDataObj.addProperty("responseDesc", "Account number details not found");
        }

        result = verificationDataObj.toString();
        return result;

    }

    @Override
    public String doPostPaidTransactionPosting(JsonObject jsonData) {
        String amount = jsonData.get("amount").getAsString();
        String accountOrMeterNo = jsonData.get("accountNumber").getAsString();
        String uniqueTransId = jsonData.get("uniqueTransId").getAsString();
        String district = jsonData.get("district").getAsString();
        String accNum = accountOrMeterNo;
        
        if(accountOrMeterNo.length() == 12)
        {
            accNum = accountOrMeterNo.substring(0, 2) + "/" + accountOrMeterNo.substring(2, 4) + "/" + accountOrMeterNo.substring(4, 6) + "/" + accountOrMeterNo.substring(6, 10) + "-" + accountOrMeterNo.substring(10);
        }
        
        logger.info("DISTRICT :: " + district);
        logger.info("DISTRICT :: " + accNum);
        district = district.split(" ")[0];
        district = district.charAt(0)+district.substring(1).toLowerCase();
        // String paymentChannel = jsonData.get("channelCode").getAsString();
        String mobileNo = jsonData.get("mobileNo").getAsString();
        if (mobileNo.isEmpty() || !mobileNo.matches(phoneRegex)) {
            mobileNo = phone;
        } else if (mobileNo.startsWith("234")) {
            mobileNo = mobileNo.replace("234", "0");
        }
        String name = jsonData.get("customerName").getAsString();

        JsonObject paymentDataObj = new JsonObject();
        paymentDataObj.addProperty("requestType", "payment");
        paymentDataObj.addProperty("disco", "ENU");
        paymentDataObj.addProperty("accountType", "postpaid");
        paymentDataObj.addProperty("uniqueTransId", jsonData.get("uniqueTransId").getAsString());

        JsonObject request = new JsonObject();
        request.addProperty("transactionRef", uniqueTransId);
        request.addProperty("accountNumber", accNum);
        request.addProperty("meterNumber", "");
        request.addProperty("customerName", name);
        request.addProperty("paymentType", "bill");
        request.addProperty("amount", Double.parseDouble(amount));
        request.addProperty("currency", "NGN");
        request.addProperty("phoneNumber", mobileNo);
        request.addProperty("paymentPlan", "Postpaid");
        request.addProperty("customerDistrict", district);
        
        try {
            logger.info("REQUEST ==== " + request.toString());
            String[] postResponse = service.vendPin(request, timeout);
            logger.info("RESPONSE CODE ==== " + postResponse[0]);
            if (postResponse[0].equalsIgnoreCase("200")) {
                logger.info("ENU PREPAID PAYMENT RESPONSE : " + postResponse[1]);

                JsonObject responseObj = new JsonParser().parse(postResponse[1]).getAsJsonObject();

                //JsonObject mainToken = new JsonObject();
                //JsonObject otherToken = new JsonObject();
                //  System.out.println("Double Token :: stdToken|bsstToken :: " + tokenField);
                String respcode = responseObj.get("responseCode").getAsString();
                String responseMessage = responseObj.get("responseMessage").getAsString();
                String accNumber = responseObj.get("accountNumber").getAsString();
                if (!respcode.equals("200")) {
                    //paymentDataObj.addProperty("errorCode", respcode);
                    //paymentDataObj.addProperty("responseMessage", responseMessage);
                    paymentDataObj.addProperty("errorCode", respcode);
                    paymentDataObj.addProperty("responseCode", "06");
                    paymentDataObj.addProperty("responseDesc", responseMessage);
                    paymentDataObj.addProperty("accountNumber", accNumber);

                    return paymentDataObj.toString();
                }
                //String firstToken = responseObj.get("token").getAsString();
                String vat = responseObj.get("vat").getAsString();
                String amt = responseObj.get("amountPaid").getAsString();
                String units = responseObj.get("units").getAsString();
                String extRef = responseObj.get("transactionId").getAsString();
                String arrears = responseObj.get("arrearsBalance").getAsString();
                String recieptNum = extRef;
                paymentDataObj.addProperty("receiptNumber", recieptNum);
                paymentDataObj.addProperty("units", units);
                paymentDataObj.addProperty("vat", vat);
                paymentDataObj.addProperty("amount", amt);
                paymentDataObj.addProperty("undertaking", "");
                paymentDataObj.addProperty("customerName", name);
                paymentDataObj.addProperty("customerArrears", arrears);
                paymentDataObj.addProperty("businessUnit", district + " businessUnit");//businessUnit
                paymentDataObj.addProperty("errorCode", respcode);
                paymentDataObj.addProperty("externalReference", extRef);
                // paymentDataObj.add("mainToken", mainToken);
//                if (doubleToken) {
//                    paymentDataObj.add("otherToken", otherToken);
//                }
                paymentDataObj.addProperty("responseCode", "00");
                paymentDataObj.addProperty("responseDesc", "Successful");
            } else {
                paymentDataObj.addProperty("errorCode", postResponse[0]);
                paymentDataObj.addProperty("responseCode", "06");
                paymentDataObj.addProperty("responseDesc", "");
            }

        } catch (SocketTimeoutException ex) {
            int count = 0;
            while (count < lookupRetry) {
                try {
                    logger.info(uniqueTransId + " :: Sleeping for " + requeryDelay + " secs (" + (count + 1) + ")");
                    Thread.sleep(requeryDelay * 1000);//"05500850931521799597168032334077232594046125"
                    String[] postResponse = service.requery(uniqueTransId);
                    if (postResponse[0].equals("200")) {
                        JsonObject responseObj = new JsonParser().parse(postResponse[1]).getAsJsonObject();
                        //String extRef = responseObj.get("transactionId").getAsString();
                        if (responseObj.get("responseCode").getAsInt() == 200) {
                            String arrears = responseObj.get("arrearsBalance").getAsString();
                            String vat = responseObj.get("vat").getAsString();
                            String amt = responseObj.get("amountPaid").getAsString();
                            String unit = responseObj.get("units").getAsString();
                            //Strin vat = responseObj.get("vat").getAsString();
                            String respcode = responseObj.get("responseCode").getAsString();
                            String ref = responseObj.get("transactionRef").getAsString();
                            String extRef = "";
                            try {
                                extRef = responseObj.get("transactionId").getAsString();
                            } catch (Exception e) {
                                extRef = "";
                            }
                            paymentDataObj.addProperty("customerName", name);
                            paymentDataObj.addProperty("errorCode", respcode);
                            paymentDataObj.addProperty("uniqueTransId", ref);
                            paymentDataObj.addProperty("customerName", name);
                            paymentDataObj.addProperty("customerArrears", arrears);
                            paymentDataObj.addProperty("externalReference", extRef);
                            paymentDataObj.addProperty("units", unit);
                            paymentDataObj.addProperty("undertaking", "");
                            paymentDataObj.addProperty("businessUnit", district + " businessUnit");//businessUnit
                            paymentDataObj.addProperty("vat", vat);
                            paymentDataObj.addProperty("responseCode", "00");
                            paymentDataObj.addProperty("responseDesc", responseObj.get("responseMessage").getAsString());
                            return paymentDataObj.toString();
                        } else {
                            paymentDataObj.addProperty("errorCode", "");
                            paymentDataObj.addProperty("responseCode", "06");
                            paymentDataObj.addProperty("responseDesc", responseObj.get("responseMessage").getAsString());
                            count++;
                        }
                    } else {
                        count++;
                    }
                } catch (Exception e) {
                    count++;
                    logger.error("Transaction Posting", ex);
                    paymentDataObj.addProperty("errorCode", "");
                    paymentDataObj.addProperty("responseCode", "06");
                    paymentDataObj.addProperty("responseDesc", "Iimed out");
                }

            }
            logger.error("Transaction Posting", ex);
            paymentDataObj.addProperty("errorCode", "");
            paymentDataObj.addProperty("responseCode", "06");
            paymentDataObj.addProperty("responseDesc", "Iimed out");
        } catch (Exception ex) {
            //logger.error("CustomerInfo Failure", ex);
            logger.error("Transaction Posting", ex);
            paymentDataObj.addProperty("errorCode", "");
            paymentDataObj.addProperty("responseCode", "56");
            paymentDataObj.addProperty("responseDesc", "Iimed out");
        }
        return paymentDataObj.toString();

    }

    @Override
    public String doPostPaidTransactionReversal(JsonObject jsonData) {
        String uniqueTransId = jsonData.get("uniqueTransId").getAsString();
        String accountOrMeterNo = jsonData.get("accountNumber").getAsString();
        JsonObject reversalDataObj = new JsonObject();
        reversalDataObj.addProperty("requestType", "reversal");
        reversalDataObj.addProperty("disco", "ENU");
        reversalDataObj.addProperty("accountType", "Postpaid");
        reversalDataObj.addProperty("uniqueTransId", jsonData.get("uniqueTransId").getAsString());
        reversalDataObj.addProperty("accountNumber", jsonData.get("accountNumber").getAsString());

        logger.info("REVERSAL REQUEST ==== " + uniqueTransId);
        String[] postResponse;
        try {
            postResponse = service.reversal(uniqueTransId);
            logger.info("RESPONSE CODE ==== " + postResponse[0]);

            if (postResponse[0].equalsIgnoreCase("200")) {
                JsonObject responseObj = new JsonParser().parse(postResponse[1]).getAsJsonObject();
                String respcode = responseObj.get("responseCode").getAsString();
                String responseMessage = responseObj.get("responseMessage").getAsString();
                String receipt = responseObj.get("transactionId").getAsString();
                //String reference = responseObj.get("transactionRef").getAsString();
                reversalDataObj.addProperty("uniqueTransId", uniqueTransId);
                reversalDataObj.addProperty("accountNumber", accountOrMeterNo);
                reversalDataObj.addProperty("externalReference", receipt);
                reversalDataObj.addProperty("errorCode", respcode);
                reversalDataObj.addProperty("responseCode", "00");
                reversalDataObj.addProperty("responseDesc", responseMessage);
            } else {
                reversalDataObj.addProperty("errorCode", postResponse[0]);
                reversalDataObj.addProperty("responseCode", "06");
                reversalDataObj.addProperty("responseDesc", "");
            }
        } catch (Exception ex) {
            reversalDataObj.addProperty("errorCode", "");
            reversalDataObj.addProperty("responseCode", "56");
            reversalDataObj.addProperty("responseDesc", "Timed out");
        }
        return reversalDataObj.toString();
    }

    @Override
    public String doPostPaidTransactionRequery(JsonObject jsonData) {
        String accountOrMeterNo = jsonData.get("accountNumber").getAsString();
        JsonObject requeryDataObj = new JsonObject();
        requeryDataObj.addProperty("requestType", "Requery");
        requeryDataObj.addProperty("disco", "ENU");
        requeryDataObj.addProperty("accountType", "Postpaid");
        requeryDataObj.addProperty("uniqueTransId", jsonData.get("uniqueTransId").getAsString());
        requeryDataObj.addProperty("accountNumber", jsonData.get("accountNumber").getAsString());
        if (accountOrMeterNo.length() != 12) {
            requeryDataObj.addProperty("errorCode", "");
            requeryDataObj.addProperty("responseCode", "06");
            requeryDataObj.addProperty("responseDesc", "Invalid Account Number");
            return requeryDataObj.toString();
        }
        String accNum = accountOrMeterNo.substring(0, 2) + "/" + accountOrMeterNo.substring(2, 4) + "/" + accountOrMeterNo.substring(4, 6) + "/" + accountOrMeterNo.substring(6, 10) + "-" + accountOrMeterNo.substring(10);
        try {
            String[] postResponse = service.requery(accNum);
            if (postResponse[0].equals("200")) {
                JsonObject responseObj = new JsonParser().parse(postResponse[1]).getAsJsonObject();
                //JsonObject mainToken = new JsonObject();
                //JsonObject otherToken = new JsonObject();
                //  System.out.println("Double Token :: stdToken|bsstToken :: " + tokenField);
                //String firstToken = responseObj.get("token").getAsString();
                String vat = responseObj.get("vat").getAsString();
                String amt = responseObj.get("amountPaid").getAsString();
                String unit = responseObj.get("units").getAsString();
                String respcode = responseObj.get("responseCode").getAsString();
                String ref = responseObj.get("transactionRef").getAsString();
                String extRef = responseObj.get("transactionId").getAsString();
                //String secondToken = tokenField.substring(index + 1);

//                mainToken.addProperty("unit", unit); //KILOWATT
//                mainToken.addProperty("amount", amt); //AMOUNT
//                mainToken.addProperty("vat", vat); //TAX
//                mainToken.addProperty("fixedCharge", ""); //FIXED CHARGE
//                mainToken.addProperty("token", firstToken); //TOKEN
                requeryDataObj.addProperty("errorCode", respcode);
                requeryDataObj.addProperty("uniqueTransId", ref);
                requeryDataObj.addProperty("externalReference", extRef);
                //requeryDataObj.add("mainToken", mainToken);
                requeryDataObj.addProperty("responseCode", "00");
                requeryDataObj.addProperty("responseDesc", responseObj.get("responseMessage").getAsString());
            } else {
                requeryDataObj.addProperty("errorCode", postResponse[0]);
                requeryDataObj.addProperty("responseCode", "06");
                requeryDataObj.addProperty("responseDesc", "");
            }
        } catch (Exception ex) {
            requeryDataObj.addProperty("errorCode", "");
            requeryDataObj.addProperty("responseCode", "56");
            requeryDataObj.addProperty("responseDesc", "Timed out");
            logger.error("Transaction requesry Failure", ex);
        }
        return requeryDataObj.toString();

    }

    @Override
    public String doPrepaidCustomerInfo(JsonObject jsonData) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        String result;
        String accountOrMeterNo = jsonData.get("accountNumber").getAsString();
        String uniqueTransId = jsonData.get("uniqueTransId").getAsString();
        JsonObject verificationDataObj = new JsonObject();

        verificationDataObj.addProperty("requestType", "verification");
        verificationDataObj.addProperty("disco", "ENU");
        verificationDataObj.addProperty("accountType", "Prepaid");
        verificationDataObj.addProperty("uniqueTransId", uniqueTransId);
        verificationDataObj.addProperty("accountNumber", accountOrMeterNo);
//        if (accountOrMeterNo.length() != 12) {
//            verificationDataObj.addProperty("errorCode", "");
//            verificationDataObj.addProperty("responseCode", "06");
//            verificationDataObj.addProperty("responseDesc", "Invalid Account Number");
//            return verificationDataObj.toString();
//        }
        //String accNum = accountOrMeterNo.substring(0, 2) + "/" + accountOrMeterNo.substring(2, 4) + "/" + accountOrMeterNo.substring(4, 6) + "/" + accountOrMeterNo.substring(6, 10) + "-" + accountOrMeterNo.substring(10);
        try {
            logger.info("CALLING ENUGU");
            //logger.info("ACCOUNT :: " + accNum);
            String[] postResponse = service.validateMeter(accountOrMeterNo, "Prepaid");
            logger.info("RESPONSE CODE ==== " + postResponse[0]);
            if (postResponse[0].equalsIgnoreCase("200")) { //Not Timeout

                //logger.info("ENU POSTPAID VERIFICATION RESPONSE : " + postResponse[1]);
                JsonObject responseObj = new JsonParser().parse(postResponse[1]).getAsJsonObject();
                int code = responseObj.get("responseCode").getAsInt();
                String message = responseObj.get("responseMessage").getAsString();
                if (code == 200) {
                    JsonObject customerDetal = responseObj.getAsJsonObject("customer");
                    //JsonObject meterDetail = responseObj.getAsJsonObject("MeterDetail");
                    String firstname;

                    try {
                        firstname = customerDetal.get("firstName").getAsString();
                        logger.info("LAST NAME :: " + firstname);
                    } catch (Exception e) {
                        firstname = "";
                    }
                    String lastname;
                    try {
                        lastname = customerDetal.get("lastName").getAsString();
                        logger.info("LAST NAME :: " + lastname);
                    } catch (Exception e) {
                        lastname = "";
                    }
                    String meterNumber = customerDetal.get("meterNumber").getAsString();
                    String accountNumber = customerDetal.get("accountNumber").getAsString();
                    long start = System.currentTimeMillis();
                    RedisUtility redis = new RedisUtility("");
                    redis.set("phcnenu-account-" + accountOrMeterNo, accountNumber);
                    redis.set("phcnenu-meter-" + accountOrMeterNo, meterNumber);
                    logger.info("Redis Set response - " + (System.currentTimeMillis() - start) + " ms");
                    String name = firstname + " " + lastname;
                    verificationDataObj.addProperty("meterNumber", meterNumber);
                    verificationDataObj.addProperty("customerName", name);
                    String customerAddress;
                    try {
                        customerAddress = customerDetal.get("address").getAsString();
                    } catch (Exception e) {
                        customerAddress = "";
                    }
                    String district;
                    try {
                        district = customerDetal.get("district").getAsString();
                    } catch (Exception e) {
                        district = "";
                    }
                    //verificationDataObj.addProperty("customerAddress", customerAddress);
                    //verificationDataObj.addProperty("businessUnit", district+" BUSINESS UNIT");
                    verificationDataObj.addProperty("customerAddress", customerAddress);
                    verificationDataObj.addProperty("businessUnit", district + " BUSINESS UNIT");
                    String state;
                    try {
                        state = customerDetal.get("state").getAsString();
                    } catch (Exception e) {
                        state = "";
                    }
                    verificationDataObj.addProperty("state", state);
                    verificationDataObj.addProperty("minimumPurchase", "0");
                    verificationDataObj.addProperty("customerArrears", customerDetal.get("arrearsBalance").getAsString());
                    verificationDataObj.addProperty("externalReference", "");
//                    verificationDataObj.addProperty("phone", "");
                    verificationDataObj.addProperty("errorCode", code);
                    verificationDataObj.addProperty("responseCode", "00");
                    verificationDataObj.addProperty("responseDesc", message);
                    verificationDataObj.addProperty("email", "");
                    verificationDataObj.addProperty("tariffRate", customerDetal.get("tariffRate").getAsString());
                    verificationDataObj.addProperty("phoneNumber", "");
                    verificationDataObj.addProperty("tariff", customerDetal.get("tariffRate").getAsString());
                    verificationDataObj.addProperty("tariffCode", customerDetal.get("tariffCode").getAsString());
                    verificationDataObj.addProperty("customerType", "");

                    //verificationDataObj.addProperty("minVendAmount", responseObj.get("MinVendAmount").getAsString());
                    //verificationDataObj.addProperty("maxVendAmount", responseObj.get("MaxVendAmount").toString());
                } else {
                    verificationDataObj.addProperty("errorCode", code);
                    verificationDataObj.addProperty("responseCode", "56");
                    verificationDataObj.addProperty("responseDesc", code + " - " + message);
                }

            } else {
                verificationDataObj.addProperty("responseCode", "06");
                verificationDataObj.addProperty("responseDesc", "Request Timeout");
            }

        } catch (Exception e) {
            logger.error("Error : ", e);
            verificationDataObj.addProperty("responseCode", "56");
            verificationDataObj.addProperty("responseDesc", "Account number details not found");
        }

        result = verificationDataObj.toString();
        return result;
    }

    @Override
    public String doPrePaidTransactionPosting(JsonObject jsonData) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        String amount = jsonData.get("amount").getAsString();
        String accountOrMeterNo = jsonData.get("accountNumber").getAsString();
        String uniqueTransId = jsonData.get("uniqueTransId").getAsString();
        String district = jsonData.get("district").getAsString();
        String meterNumber;
        String account;
        
//        redis.set("phcnenu-account-" + accountOrMeterNo, accountNumber);
//                    redis.set("phcnenu-meter-" + accountNumber, meterNumber);
        long start = System.currentTimeMillis();
        RedisUtility redis = new RedisUtility("");

        account = redis.get("phcnenu-account-" + accountOrMeterNo);
        meterNumber = redis.get("phcnenu-meter-" + accountOrMeterNo);
        //accountOrMeterNo = 
        logger.info("Redis Set response account - " +account);
        logger.info("Redis Set response meter - " + meterNumber);
        logger.info("Redis Set response - " + (System.currentTimeMillis() - start) + " ms");
        district = district.split(" ")[0];
        district = district.charAt(0)+district.substring(1).toLowerCase();
        // String paymentChannel = jsonData.get("channelCode").getAsString();
        String mobileNo = jsonData.get("mobileNo").getAsString();
        logger.info("PHONE ========= " + phone + " mobilE LENGTH :: " + mobileNo.length());
//        if (mobileNo.isEmpty() || mobileNo.length() < 11 || (mobileNo.startsWith("234") && mobileNo.length() != 13) || (mobileNo.startsWith("0") && mobileNo.length() != 11)) {
//            mobileNo = phone;
//            logger.info("MOBILE ========= " + mobileNo);
//        }
        //String mobileNo = jsonData.get("mobileNo").getAsString();
        if (mobileNo.isEmpty() || !mobileNo.matches(phoneRegex)) {
            mobileNo = phone;
        } else if (mobileNo.startsWith("234")) {
            mobileNo = mobileNo.replace("234", "0");
        }
        JsonObject paymentDataObj = new JsonObject();
//        if (accountOrMeterNo.length() != 12) {
//            paymentDataObj.addProperty("errorCode", "");
//            paymentDataObj.addProperty("responseCode", "06");
//            paymentDataObj.addProperty("responseDesc", "Invalid Account Number");
//            return paymentDataObj.toString();
//        }
        //String accNum = accountOrMeterNo.substring(0, 2) + "/" + accountOrMeterNo.substring(2, 4) + "/" + accountOrMeterNo.substring(4, 6) + "/" + accountOrMeterNo.substring(6, 10) + "-" + accountOrMeterNo.substring(10);
        String name = jsonData.get("customerName").getAsString();
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.

        paymentDataObj.addProperty("requestType", "payment");
        paymentDataObj.addProperty("disco", "ENU");
        paymentDataObj.addProperty("accountType", "prepaid");
        paymentDataObj.addProperty("uniqueTransId", jsonData.get("uniqueTransId").getAsString());
        paymentDataObj.addProperty("accountNumber", jsonData.get("accountNumber").getAsString());
//        double amt2 = Double.parseDouble(amount);
//        if(amt2 > maxAmount)
//        {
//            paymentDataObj.addProperty("errorCode", "");
//            paymentDataObj.addProperty("responseCode", "06");
//            paymentDataObj.addProperty("responseDesc", "Maximum amount exceeded");
//            return paymentDataObj.toString();
//        }
        JsonObject request = new JsonObject();
        request.addProperty("transactionRef", uniqueTransId);
        request.addProperty("accountNumber", account);
        request.addProperty("meterNumber", meterNumber);
        request.addProperty("customerName", name);
        request.addProperty("paymentType", "bill");
        request.addProperty("amount", Double.parseDouble(amount));
        request.addProperty("currency", "NGN");
        request.addProperty("phoneNumber", mobileNo);
        request.addProperty("paymentPlan", "Prepaid");
        request.addProperty("customerDistrict", district);
        try {
            logger.info("REQUEST ==== " + request.toString());
            String[] postResponse = service.vendPin(request, timeout);
            logger.info("RESPONSE CODE ==== " + postResponse[0]);
            if (postResponse[0].equalsIgnoreCase("200")) {
                logger.info("ENU PREPAID PAYMENT RESPONSE : " + postResponse[1]);

                JsonObject responseObj = new JsonParser().parse(postResponse[1]).getAsJsonObject();

                JsonObject mainToken = new JsonObject();
                //JsonObject otherToken = new JsonObject();
                //  System.out.println("Double Token :: stdToken|bsstToken :: " + tokenField);
                String respcode = responseObj.get("responseCode").getAsString();

                String responseMessage = responseObj.get("responseMessage").getAsString();
                if (!respcode.equals("200")) {
                    //paymentDataObj.addProperty("errorCode", respcode);
                    //paymentDataObj.addProperty("responseMessage", responseMessage);
                    paymentDataObj.addProperty("errorCode", respcode);
                    paymentDataObj.addProperty("responseCode", "06");
                    paymentDataObj.addProperty("responseDesc", responseMessage);
                    return paymentDataObj.toString();
                }
                //String tariffCode = responseObj.get("tariffCode").getAsString();
                String firstToken = responseObj.get("token").getAsString();
                String vat = responseObj.get("vat").getAsString();
                String amt = responseObj.get("amountPaid").getAsString();
                String unit = responseObj.get("units").getAsString();
                String extRef = responseObj.get("transactionId").getAsString();
                String recieptNum = extRef;
                //JsonObject custDetail = responseObj.get("CustomerDetail").getAsJsonObject();
                String arrears = responseObj.get("appliedToArrears").getAsString();

                //String customerArrears = responseObj.get("customerArrears").getAsString();
                //String secondToken = tokenField.substring(index + 1);
                mainToken.addProperty("unit", unit); //KILOWATT
                mainToken.addProperty("amount", amt); //AMOUNT
                mainToken.addProperty("vat", vat); //TAX
                mainToken.addProperty("fixedCharge", ""); //FIXED CHARGE
                mainToken.addProperty("token", firstToken); //TOKEN

//                otherToken.addProperty("unit", ""); //KILOWATT
//                otherToken.addProperty("amount", ""); //AMOUNT
//                otherToken.addProperty("vat", ""); //TAX
//                otherToken.addProperty("fixedCharge", ""); //FIXED CHARGEReceiptNumber
//                otherToken.addProperty("token", secondToken); //TOKEN
                paymentDataObj.addProperty("receiptNumber", recieptNum);
                paymentDataObj.addProperty("customerArrears", arrears);
                //  paymentDataObj.addProperty("undertaking", tariffCode);
                paymentDataObj.addProperty("undertaking", "");
                paymentDataObj.addProperty("customerName", name);
                paymentDataObj.addProperty("businessUnit", district);
                paymentDataObj.addProperty("errorCode", respcode);
                paymentDataObj.addProperty("externalReference", extRef);
                paymentDataObj.add("mainToken", mainToken);
//                if (doubleToken) {
//                    paymentDataObj.add("otherToken", otherToken);
//                }
                paymentDataObj.addProperty("responseCode", "00");
                paymentDataObj.addProperty("responseDesc", "Successful");
            } else {

                paymentDataObj.addProperty("errorCode", postResponse[0]);
                paymentDataObj.addProperty("responseCode", "56");
                paymentDataObj.addProperty("responseDesc", "Timed out");
            }

        } catch (SocketTimeoutException ex) {
            int count = 0;
            while (count < lookupRetry) {
                try {
                    logger.info(uniqueTransId + " :: Sleeping for " + requeryDelay + " secs (" + (count + 1) + ")");
                    Thread.sleep(requeryDelay * 1000);//030000000400003400081
                    String[] postResponse = service.requery(uniqueTransId);

                    if (postResponse[0].trim().equals("200")) {

                        JsonObject responseObj = new JsonParser().parse(postResponse[1]).getAsJsonObject();
                        if (responseObj.get("responseCode").getAsString().equals("200")) {
                            JsonObject mainToken = new JsonObject();
                            //JsonObject otherToken = new JsonObject();

                            //  System.out.println("Double Token :: stdToken|bsstToken :: " + tokenField);
                            String firstToken = "";
                            try {
                                firstToken = responseObj.get("token").getAsString();
                            } catch (Exception exp) {
                                firstToken = "";
                            }
                            String vat = "";
                            try {
                                vat = responseObj.get("vat").getAsString();
                            } catch (Exception exp) {
                                firstToken = "";
                            }
                            String extRef = "";
                            try {
                                extRef = responseObj.get("transactionRef").getAsString();
                            } catch (Exception exp) {
                                extRef = "";
                            }
                            //String vat = responseObj.get("vat").getAsString();
                            String amt = responseObj.get("amountPaid").getAsString();
                            String unit = responseObj.get("units").getAsString();
                            String respcode = responseObj.get("responseCode").getAsString();
                            String ref = responseObj.get("transactionRef").getAsString();

                            //String secondToken = tokenField.substring(index + 1);
                            logger.info("RESPONSE CODE REQUERY :: == =============" + postResponse[0]);
                            mainToken.addProperty("unit", unit); //KILOWATT
                            mainToken.addProperty("amount", amt); //AMOUNT
                            mainToken.addProperty("vat", vat); //TAX
                            mainToken.addProperty("fixedCharge", ""); //FIXED CHARGE
                            mainToken.addProperty("token", firstToken); //TOKEN
                            paymentDataObj.addProperty("errorCode", respcode);
                            paymentDataObj.addProperty("uniqueTransId", ref);
                            paymentDataObj.addProperty("externalReference", extRef);
                            paymentDataObj.add("mainToken", mainToken);
                            paymentDataObj.addProperty("responseCode", "00");
                            paymentDataObj.addProperty("responseDesc", responseObj.get("responseMessage").getAsString());

                            break;
                        } else {
                            count++;
                            //logger.error("PaymentPost Failure");
                            paymentDataObj.addProperty("errorCode", "");
                            paymentDataObj.addProperty("responseCode", "06");
                            paymentDataObj.addProperty("responseDesc", "");

                        }
                    } else {
                        count++;
                        // logger.error("PaymentPost Failure", ex);
                        paymentDataObj.addProperty("errorCode", "");
                        paymentDataObj.addProperty("responseCode", "06");
                        paymentDataObj.addProperty("responseDesc", "");
                    }
                } catch (Exception e) {
                    count++;
                    logger.error("PaymentPost Failure", ex);
                    paymentDataObj.addProperty("errorCode", "");
                    paymentDataObj.addProperty("responseCode", "06");
                    paymentDataObj.addProperty("responseDesc", "");
                }

            }
            return paymentDataObj.toString();

        } catch (Exception ex) {
            logger.error("Transaction Posting", ex);
            paymentDataObj.addProperty("errorCode", "");
            paymentDataObj.addProperty("responseCode", "06");
            paymentDataObj.addProperty("responseDesc", "");
        }

        return paymentDataObj.toString();
    }

    @Override
    public String doPrePaidTransactionReversal(JsonObject jsonData) {
        String uniqueTransId = jsonData.get("uniqueTransId").getAsString();
        String accountOrMeterNo = jsonData.get("accountNumber").getAsString();
        JsonObject reversalDataObj = new JsonObject();
        reversalDataObj.addProperty("requestType", "Reversal");
        reversalDataObj.addProperty("disco", "ENU");
        reversalDataObj.addProperty("accountType", "Postpaid");
        reversalDataObj.addProperty("uniqueTransId", jsonData.get("uniqueTransId").getAsString());
        reversalDataObj.addProperty("accountNumber", jsonData.get("accountNumber").getAsString());
        if (accountOrMeterNo.length() != 12) {
            reversalDataObj.addProperty("errorCode", "");
            reversalDataObj.addProperty("responseCode", "06");
            reversalDataObj.addProperty("responseDesc", "Invalid Account Number");
            return reversalDataObj.toString();
        }
        String accNum = accountOrMeterNo.substring(0, 2) + "/" + accountOrMeterNo.substring(2, 4) + "/" + accountOrMeterNo.substring(4, 6) + "/" + accountOrMeterNo.substring(6, 10) + "-" + accountOrMeterNo.substring(10);
        logger.info("REVERSAL REQUEST ==== " + uniqueTransId);
        String[] postResponse;
        try {
            postResponse = service.reversal(accNum);
            logger.info("RESPONSE CODE ==== " + postResponse[0]);

            if (postResponse[0].equalsIgnoreCase("200")) {
                JsonObject responseObj = new JsonParser().parse(postResponse[1]).getAsJsonObject();
                String respcode = responseObj.get("responseCode").getAsString();
                String responseMessage = responseObj.get("responseMessage").getAsString();
                String receipt = responseObj.get("transactionId").getAsString();
                //String reference = responseObj.get("transactionRef").getAsString();
                reversalDataObj.addProperty("uniqueTransId", uniqueTransId);
                reversalDataObj.addProperty("accountNumber", accountOrMeterNo);
                reversalDataObj.addProperty("externalReference", receipt);
                reversalDataObj.addProperty("errorCode", respcode);
                reversalDataObj.addProperty("responseCode", "00");
                reversalDataObj.addProperty("responseDesc", responseMessage);
            } else {
                reversalDataObj.addProperty("errorCode", postResponse[0]);
                reversalDataObj.addProperty("responseCode", "06");
                reversalDataObj.addProperty("responseDesc", "");
            }
        } catch (Exception ex) {
            reversalDataObj.addProperty("errorCode", "");
            reversalDataObj.addProperty("responseCode", "56");
            reversalDataObj.addProperty("responseDesc", "Timed out");
        }
        return reversalDataObj.toString();
    }

    @Override
    public String doPrePaidTransactionRequery(JsonObject jsonData) {
        String accountOrMeterNo = jsonData.get("accountNumber").getAsString();
        JsonObject requeryDataObj = new JsonObject();
        requeryDataObj.addProperty("requestType", "Requery");
        requeryDataObj.addProperty("disco", "ENU");
        requeryDataObj.addProperty("accountType", "Prepaid");
        requeryDataObj.addProperty("uniqueTransId", jsonData.get("uniqueTransId").getAsString());
        requeryDataObj.addProperty("accountNumber", jsonData.get("accountNumber").getAsString());
        String accNum = accountOrMeterNo;//.substring(0, 2) + "/" + accountOrMeterNo.substring(2, 4) + "/" + accountOrMeterNo.substring(4, 6) + "/" + accountOrMeterNo.substring(6, 10) + "-" + accountOrMeterNo.substring(10);
        if (accountOrMeterNo.length() != 12) {
            requeryDataObj.addProperty("errorCode", "");
            requeryDataObj.addProperty("responseCode", "06");
            requeryDataObj.addProperty("responseDesc", "Invalid Account Number");
            return requeryDataObj.toString();
        }
        try {
            String[] postResponse = service.requery(accNum);
            if (postResponse[0].equals("200")) {
                JsonObject responseObj = new JsonParser().parse(postResponse[1]).getAsJsonObject();
                JsonObject mainToken = new JsonObject();
                //JsonObject otherToken = new JsonObject();
                //  System.out.println("Double Token :: stdToken|bsstToken :: " + tokenField);
                String firstToken = responseObj.get("token").getAsString();
                String vat = responseObj.get("vat").getAsString();
                String amt = responseObj.get("amountPaid").getAsString();
                String unit = responseObj.get("units").getAsString();
                String respcode = responseObj.get("responseCode").getAsString();
                String ref = responseObj.get("transactionRef").getAsString();
                String extRef = responseObj.get("transactionId").getAsString();
                //String secondToken = tokenField.substring(index + 1);

                mainToken.addProperty("unit", unit); //KILOWATT
                mainToken.addProperty("amount", amt); //AMOUNT
                mainToken.addProperty("vat", vat); //TAX
                mainToken.addProperty("fixedCharge", ""); //FIXED CHARGE
                mainToken.addProperty("token", firstToken); //TOKEN
                requeryDataObj.addProperty("errorCode", respcode);
                requeryDataObj.addProperty("uniqueTransId", ref);
                requeryDataObj.addProperty("externalReference", extRef);
                requeryDataObj.add("mainToken", mainToken);
                requeryDataObj.addProperty("responseCode", "00");
                requeryDataObj.addProperty("responseDesc", responseObj.get("responseMessage").getAsString());
                //return requeryDataObj.toString();
            } else {
                requeryDataObj.addProperty("errorCode", postResponse[0]);
                requeryDataObj.addProperty("responseCode", "06");
                requeryDataObj.addProperty("responseDesc", "");
            }
        } catch (Exception ex) {
            requeryDataObj.addProperty("errorCode", "");
            requeryDataObj.addProperty("responseCode", "56");
            requeryDataObj.addProperty("responseDesc", "Timed out");
            logger.error("Transaction requery", ex);
        }
        return requeryDataObj.toString();

    }

}
