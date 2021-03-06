/**
 * Copyright (c) 2000-2012 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.mpower.slayer.service.impl;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.DynamicQueryFactoryUtil;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.model.User;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.mpower.slayer.model.SiteInvitation;
import com.mpower.slayer.model.impl.SiteInvitationImpl;
import com.mpower.slayer.service.base.SiteInvitationLocalServiceBaseImpl;
import com.mpower.slayer.service.persistence.SiteInvitationFinderUtil;
import com.mpower.util.InvitationConstants;

/**
 * The implementation of the site invitation local service.
 * 
 * <p>
 * All custom service methods should be put in this class. Whenever methods are
 * added, rerun ServiceBuilder to copy their definitions into the
 * {@link com.mpower.slayer.service.SiteInvitationLocalService} interface.
 * 
 * <p>
 * This is a local service. Methods of this service will not have security
 * checks based on the propagated JAAS credentials because this service can only
 * be accessed from within the same VM.
 * </p>
 * 
 * @author Brian Wing Shun Chan
 * @see com.mpower.slayer.service.base.SiteInvitationLocalServiceBaseImpl
 * @see com.mpower.slayer.service.SiteInvitationLocalServiceUtil
 */
public class SiteInvitationLocalServiceImpl extends
		SiteInvitationLocalServiceBaseImpl {
	/*
	 * NOTE FOR DEVELOPERS:
	 * 
	 * Never reference this interface directly. Always use {@link
	 * com.mpower.slayer.service.SiteInvitationLocalServiceUtil} to access the
	 * site invitation local service.
	 */

	public boolean isAnExistingUser(String emailAddress, long companyId) {

		boolean anExistingUser = false;
		try {
			User user = UserLocalServiceUtil.fetchUserByEmailAddress(companyId,
					emailAddress);
			anExistingUser = Validator.isNotNull(user);
		} catch (SystemException e) {
			e.printStackTrace();
		}

		return anExistingUser;
	}

	public boolean alreadyInvited(long inviterId, String inviteeEmail) {

		boolean alreadyInvited = false;
		try {
			SiteInvitation siteInvitation = 
				siteInvitationPersistence.fetchByUserId_InviteeEmail(
					inviterId, inviteeEmail);
			alreadyInvited = Validator.isNotNull(siteInvitation);
		} catch (SystemException e) {
			e.printStackTrace();
		}

		return alreadyInvited;
	}

	public void setInvitationAccepted(long inviterId, String inviteeEmail,
			long inviteeNewUserId) {

		SiteInvitation siteInvitation = null;
		try {
			siteInvitation = siteInvitationPersistence
					.fetchByUserId_InviteeEmail(inviterId, inviteeEmail);
		} catch (SystemException e) {
			e.printStackTrace();
		}

		if (Validator.isNull(siteInvitation))
			return;

		siteInvitation.setStatus(InvitationConstants.STATUS_ACCEPTED);
		siteInvitation.setInviteeNewUserId(inviteeNewUserId);
		siteInvitation.setModifiedDate(new Date());

		try {
			siteInvitationPersistence.update(siteInvitation, false);
		} catch (SystemException e) {
			e.printStackTrace();
		}
	}

	public void saveSiteInvitation(String inviteeEmail, long inviterId,
			long companyId, long groupId) {

		long invitationId = 0l;
		try {
			invitationId = counterLocalService.increment();
		} catch (SystemException e) {
			e.printStackTrace();
		}

		SiteInvitation siteInvitation = new SiteInvitationImpl();
		siteInvitation.setInvitationId(invitationId);
		siteInvitation.setInviteeEmail(inviteeEmail);
		siteInvitation.setUserId(inviterId);
		siteInvitation.setCompanyId(companyId);
		siteInvitation.setGroupId(groupId);
		siteInvitation.setCreateDate((new Date()));
		siteInvitation.setStatus(InvitationConstants.STATUS_PENDING);

		try {
			siteInvitationPersistence.update(siteInvitation, false);
		} catch (SystemException e) {
			e.printStackTrace();
		}
	}

	public List<SiteInvitation> getUserInvitations(long inviterId, int status) 
			throws SystemException {
		
		List<SiteInvitation> siteInvitations = null;
		
		siteInvitations = (status == InvitationConstants.STATUS_ALL)? 
			siteInvitationPersistence.findByUserId(inviterId) : 
				siteInvitationPersistence.findByUserId_Status(inviterId, status);
		
		return siteInvitations;
	}
	
	public int getUserInvitationsCount(long inviterId, int status) {
		
		int count = 0;
		
		if (userHasInvitations(inviterId)) {
			List<SiteInvitation> siteInvitations = null;
			try {
				siteInvitations = getUserInvitations(inviterId, status);
			} catch (SystemException e) {
				e.printStackTrace();
			}
			if (Validator.isNotNull(siteInvitations) && !siteInvitations.isEmpty()) {
				count = siteInvitations.size();
			}
		} 
		
		return count;
	}
	
	public String getUserRank(long userId) {
		
		int acceptedInvitations = 0;
		try {
			acceptedInvitations = siteInvitationPersistence.countByUserId_Status(userId, InvitationConstants.STATUS_ACCEPTED);
		} catch (SystemException e) {
			e.printStackTrace();
		}
		
		if (acceptedInvitations > 0) { 
			return String.valueOf(SiteInvitationFinderUtil.getUserRank(userId));
		} else {
			return "Not Applicable";
		}
	}
	
	public int getTotalPoints(long userId, int pointsForInviting, int pointsForAccepting) {
		
		int points = 0;
		
		try {
			int invitedCount = siteInvitationPersistence.countByUserId_Status(userId, InvitationConstants.STATUS_INVITED);
			points += invitedCount * pointsForInviting;
		} catch (SystemException e) {
			e.printStackTrace();
		}
		
		try {
			int acceptedCount = siteInvitationPersistence.countByUserId_Status(userId, InvitationConstants.STATUS_ACCEPTED);
			points += acceptedCount * pointsForAccepting;
		} catch (SystemException e) {
			e.printStackTrace();
		}
		
		return points;
	}
	
	public boolean userHasInvitations(long userId) {
		
		int count = 0;
		try {
			count = siteInvitationPersistence.countByUserId(userId);
		} catch (SystemException e) {
			e.printStackTrace();
		}
		
		return (count > 0);
	}
	
	public List<SiteInvitation> getListForSendingReminder() {
		List<SiteInvitation> siteInvitations = null;
		
		DynamicQuery dynamicQuery = DynamicQueryFactoryUtil.forClass(SiteInvitation.class);
		dynamicQuery.add(RestrictionsFactoryUtil.eq("status", InvitationConstants.STATUS_INVITED));
		dynamicQuery.add(RestrictionsFactoryUtil.lt("reminders", 3));
		
		Calendar calendar = new GregorianCalendar();
		calendar.roll(Calendar.DATE, -(10));
		
		dynamicQuery.add(RestrictionsFactoryUtil.lt("lastReminderDate", calendar.getTime()));
		
		try {
			siteInvitations = dynamicQuery(dynamicQuery);
		} catch (SystemException e) {
			e.printStackTrace();
		}
		
		return siteInvitations;
	}
}