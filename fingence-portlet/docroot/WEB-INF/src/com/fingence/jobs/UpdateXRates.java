package com.fingence.jobs;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;

import com.fingence.slayer.model.Currency;
import com.fingence.slayer.service.CurrencyLocalServiceUtil;
import com.fingence.slayer.service.CurrencyServiceUtil;
import com.liferay.counter.service.CounterLocalServiceUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.messaging.BaseMessageListener;
import com.liferay.portal.kernel.messaging.Message;
import com.liferay.portal.kernel.util.Validator;

public class UpdateXRates extends BaseMessageListener {
	
	@Override
	protected void doReceive(Message arg) throws Exception {
		update();
	}

	private void update() {
						
		String appId = "d87f7d237e0b4a58bf5e85c40b777cb0";
		String url = "http://openexchangerates.org/api/latest.json?app_id="+ appId;
	
		HttpClient client = new HttpClient();
		GetMethod method = new GetMethod(url);
		
		try {
			client.executeMethod(method);
		} catch (HttpException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		InputStream inputStream = null;
		try {
			inputStream = method.getResponseBodyAsStream();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String jsonResponse = null;
		try {
			jsonResponse = IOUtils.toString(inputStream);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		JSONObject jsonObject = null;
		try {
			jsonObject = JSONFactoryUtil.createJSONObject(jsonResponse);
		} catch (JSONException e) {
			e.printStackTrace();
		}
				
		Iterator<String> itr = jsonObject.getJSONObject("rates").keys();
						
		while (itr.hasNext()) {
			
			String currencyCode = itr.next();
		
			double xrate = jsonObject.getJSONObject("rates").getDouble(currencyCode);
									
			Currency currency = CurrencyServiceUtil.getCurrency(currencyCode);
			
			if (Validator.isNull(currency)) {
				long currencyId = 0;
				try {
					currencyId = CounterLocalServiceUtil.increment(Currency.class.getName());
				} catch (SystemException e) {
					e.printStackTrace();
				}
				currency = CurrencyLocalServiceUtil.createCurrency(currencyId);
				currency.setCurrencyCode(currencyCode);	
				try {
					currency = CurrencyLocalServiceUtil.addCurrency(currency);
				} catch (SystemException e) {
					e.printStackTrace();
				}
			}
			
			if (xrate == currency.getConversion()) continue;
			
			currency.setConversion(1/xrate);
			
			try {
				CurrencyLocalServiceUtil.updateCurrency(currency);
			} catch (SystemException e) {
				e.printStackTrace();
			}
		}
	}
}