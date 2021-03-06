package com.hook.startup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import com.liferay.portal.kernel.events.ActionException;
import com.liferay.portal.kernel.events.SimpleAction;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.service.LockLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.ServiceContextThreadLocal;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.slayer.service.BridgeLocalServiceUtil;

public class ImportOldUsers extends SimpleAction {
	public void run(String[] args) throws ActionException {
		
		String className = ImportOldUsers.class.getName();
		long companyId = Long.valueOf(args[0]);
		
		Company company = null;
		try {
			company = CompanyLocalServiceUtil.fetchCompany(companyId);
		} catch (SystemException e) {
			e.printStackTrace();
		}
		
		try {
			boolean locked = LockLocalServiceUtil.isLocked(className, company.getKey());
			if (locked) {
				System.out.println("already locked...returning....");
				return;
			}
		} catch (SystemException e) {
			e.printStackTrace();
		}		
		
		System.out.println("going to insert the records...");
		
		InputStream inputStream = ImportOldUsers.class.getClassLoader().getResourceAsStream("data/old-users.csv");
		
		if (Validator.isNull(inputStream)) {
			return; 
		}
				
		BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
		
		long creatorUserId = 0l;
		try {
			creatorUserId = UserLocalServiceUtil.getDefaultUserId(companyId);
		} catch (PortalException e) {
			e.printStackTrace();
		} catch (SystemException e) {
			e.printStackTrace();
		}		
		
		try {
			String line = StringPool.BLANK;
			while (Validator.isNotNull(line = br.readLine())) {
				insertOldUser(companyId, creatorUserId, line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// lock the process
		try {
			LockLocalServiceUtil.lock(className, company.getKey(), "Portal Admin");
		} catch (SystemException e) {
			e.printStackTrace();
		}		
	}
	
	private void insertOldUser(long companyId, long creatorUserId, String line) {
		
		String parts[] = line.split(StringPool.COMMA);
		
		boolean autoPassword = false;
		String password = parts[2];
		boolean autoScreenName = true;
		String screenName = StringPool.BLANK;
		String emailAddress = parts[1];
		long facebookId = 0l;
		String openId = StringPool.BLANK;
		String firstName = parts[3];
		String middleName = StringPool.BLANK;
		String lastName = StringPool.BLANK;
		int prefixId = 0;
		int suffixId = 0;
		boolean male = parts[6].equalsIgnoreCase("1");
		int birthdayMonth = 1;
		int birthdayDay = 1;
		int birthdayYear = 2000;
		String jobTitle = StringPool.BLANK;
		long[] groupIds = null;
		long[] organizationIds = null;
		long[] roleIds = null;
		long[] userGroupIds = null;
		boolean sendEmail = false;
		
		ServiceContext serviceContext = ServiceContextThreadLocal.getServiceContext();
		
		DateFormat dateFormat = new SimpleDateFormat("dd/MM/yy hh:mm");
		
		try {
			User user = UserLocalServiceUtil.addUser(creatorUserId, companyId,
					autoPassword, password, password, autoScreenName, screenName,
					emailAddress, facebookId, openId, Locale.US, firstName,
					middleName, lastName, prefixId, suffixId, male, birthdayMonth,
					birthdayDay, birthdayYear, jobTitle, groupIds, organizationIds,
					roleIds, userGroupIds, sendEmail, serviceContext);
			
			String createDate = parts[0];
			if (Validator.isNotNull(createDate)) {
				try {
					user.setCreateDate(dateFormat.parse(createDate));
				} catch (ParseException e) {
					e.printStackTrace();
				}				
			}
			
			String lastLoginDate = parts[5];
			if (Validator.isNotNull(lastLoginDate)) {
				try {
					user.setLastLoginDate(dateFormat.parse(lastLoginDate));
				} catch (ParseException e) {
					e.printStackTrace();
				}				
			}
			
			user.setLastName(parts[2]);
			user.setPasswordReset(false);
			user.setOpenId("no-invitation-check");
			user.setJobTitle("mail-not-sent");
			
			user = UserLocalServiceUtil.updateUser(user);
			
			_log.debug("User successfully created ==> " + user);
			
			String mobile = parts[4];
			
			if (Validator.isNotNull(mobile) && mobile.startsWith("091")) {
				String[] tokens = mobile.split(StringPool.DASH);
				if (tokens.length == 2 && tokens[1].length() == 10) {
					BridgeLocalServiceUtil.addPhone(user.getUserId(), User.class.getName(), user.getUserId(), tokens[1], "91", true);
				}
			}
			
		} catch (PortalException e) {
			_log.error(e.getMessage());
		} catch (SystemException e) {
			_log.error(e.getMessage());
		}
	}
	
	private static Log _log = LogFactoryUtil.getLog(
			LanguageConfig.class);	
}