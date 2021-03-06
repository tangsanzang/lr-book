package com.util;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.StringUtil;

public class SMSUtil {
	
	public static void sendVerificationCode(String mobileNumber, String verificationCode) {
		
		_log.debug("The verification code: " + verificationCode);
						
		String [] oldSubs = {"[$USERNAME$]", "[$PASSWORD$]", "[$API_ID$]", "[$TO$]", "[$MESSAGE$]"};
		String [] newSubs = {
				AppConfig.get(IConstants.CFG_CLICKATELL_USERNAME), 
				AppConfig.get(IConstants.CFG_CLICKATELL_PASSWORD), 
				AppConfig.get(IConstants.CFG_CLICKATELL_API_ID),
				mobileNumber, 
				HttpUtil.encodeURL("Your Vefication Code: " + verificationCode)
		};
		
		String httpURL = "http://api.clickatell.com/http/sendmsg?user=[$USERNAME$]&password=[$PASSWORD$]&api_id=[$API_ID$]&to=[$TO$]&text=[$MESSAGE$]";
		httpURL = StringUtil.replace(httpURL, oldSubs, newSubs);
				
		HttpClient client = new HttpClient();
		GetMethod method = new GetMethod(httpURL);
		
		try {
			client.executeMethod(method);
			
			InputStream is = method.getResponseBodyAsStream();
		
			is.close();
		} catch (HttpException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			method.releaseConnection();
		}
	}
	
	static Log _log = LogFactoryUtil.getLog(SMSUtil.class);
}