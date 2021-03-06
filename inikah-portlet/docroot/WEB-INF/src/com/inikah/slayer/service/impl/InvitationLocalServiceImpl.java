/**
 * Copyright (c) 2000-2013 Liferay, Inc. All rights reserved.
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

package com.inikah.slayer.service.impl;

import java.util.Date;
import java.util.List;

import com.inikah.invite.InviteConstants;
import com.inikah.slayer.model.Invitation;
import com.inikah.slayer.service.base.InvitationLocalServiceBaseImpl;
import com.inikah.util.NotifyUtil;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.DynamicQueryFactoryUtil;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.portlet.PortletClassLoaderUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.model.User;
import com.liferay.portal.security.auth.CompanyThreadLocal;

/**
 * The implementation of the invitation local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.inikah.slayer.service.InvitationLocalService} interface.
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author Ahmed Hasan
 * @see com.inikah.slayer.service.base.InvitationLocalServiceBaseImpl
 * @see com.inikah.slayer.service.InvitationLocalServiceUtil
 */
public class InvitationLocalServiceImpl extends InvitationLocalServiceBaseImpl {
	/*
	 * NOTE FOR DEVELOPERS:
	 *
	 * Never reference this interface directly. Always use {@link com.inikah.slayer.service.InvitationLocalServiceUtil} to access the invitation local service.
	 */
	
	/**
	 * 
	 */
	public String getInviteeEmail(long invitationId) {
		String inviteeEmail = StringPool.BLANK;
		try {
			Invitation invitation = fetchInvitation(invitationId);
			
			if (Validator.isNotNull(invitation)) {
				inviteeEmail = invitation.getInviteeEmail();
				
				// update status
				if (invitation.getStatus() == InviteConstants.STATUS_CREATED) {
					invitation.setStatus(InviteConstants.STATUS_CLICKED);
					updateInvitation(invitation);	
				}
			}
		} catch (SystemException e) {
			e.printStackTrace();
		}
		
		return inviteeEmail;
	}
	
	/**
	 * 
	 */
	public void updateInviation(long invitationId, String emailAddress) {
		Invitation invitation = null;
		try {
			invitation = fetchInvitation(invitationId);
		} catch (SystemException e) {
			e.printStackTrace();
		}
		
		if (Validator.isNull(invitation)) return;
		
		if (!emailAddress.equalsIgnoreCase(invitation.getInviteeEmail())) {
			invitation.setRegisteredEmail(emailAddress);
		}
		
		invitation.setStatus(InviteConstants.STATUS_JOINED);
		invitation.setModifiedDate(new Date());
		
		try {
			updateInvitation(invitation);
		} catch (SystemException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @param inviterId
	 * @param emailAddress
	 */
	public Invitation initInvitation(long inviterId, String emailAddress, int status) {
		
		long companyId = CompanyThreadLocal.getCompanyId();
		
		long invitationId = 0l;
		try {
			invitationId = counterLocalService.increment(Invitation.class.getName());
		} catch (SystemException e) {
			e.printStackTrace();
		}
		
		Invitation invitation = createInvitation(invitationId);
		
		invitation.setCompanyId(companyId);
		invitation.setUserId(inviterId);
		invitation.setCreateDate(new Date());
		invitation.setInviteeEmail(emailAddress);
		invitation.setStatus(status);
		
		try {
			addInvitation(invitation);
		} catch (SystemException e) {
			e.printStackTrace();
		}
		
		return invitation;
	}
	
	public void sendInvitation(long inviterId, String inviteeName, String inviteeEmail) {
		
		User inviter = null;
		try {
			inviter = userLocalService.fetchUser(inviterId);
		} catch (SystemException e) {
			e.printStackTrace();
		}
		
		if (Validator.isNull(inviter)) return;
		
		Invitation invitation = initInvitation(inviterId, inviteeEmail, 0);
		invitation.setUserName(inviteeName);
		
		try {
			updateInvitation(invitation);
		} catch (SystemException e) {
			e.printStackTrace();
		}
		
		// email the actual invitation
		String inviterName = inviter.getFirstName();
		String inviterEmail = inviter.getEmailAddress();
		
		NotifyUtil.sendInvitation(invitation.getInvitationId(), inviterEmail, inviterName, inviteeEmail, inviteeName);
	}
	
	public void linkInvitation(User user) {
				
		String noInvitationCheck = user.getOpenId();
		if (Validator.isNotNull(noInvitationCheck) 
				&& noInvitationCheck.equalsIgnoreCase("no-invitation-check")) return;
		
		DynamicQuery dynamicQuery = DynamicQueryFactoryUtil.forClass(
				Invitation.class, PortletClassLoaderUtil.getClassLoader());
		
		String emailAddress = user.getEmailAddress();
		dynamicQuery.add(RestrictionsFactoryUtil.or(
				RestrictionsFactoryUtil.eq("inviteeEmail", emailAddress),
				RestrictionsFactoryUtil.eq("registeredEmail", emailAddress)));
		
		Invitation invitation = null; 
		try {
			@SuppressWarnings("unchecked")
			List<Invitation> records = dynamicQuery(dynamicQuery);
			for (Invitation _invitation: records) {
				invitation = _invitation;
			}
		} catch (SystemException e) {
			e.printStackTrace();
		}
		
		if (Validator.isNull(invitation)) return;
		
		invitation.setInviteeNewUserId(user.getUserId());
		invitation.setStatus(InviteConstants.STATUS_LINKED);
		invitation.setModifiedDate(new Date());
		
		// email the actual inviter. 
		
		// set invitation chain.
		invitation.setInvitationChain(getChain(invitation.getUserId()));
		
		// permanently set this in the user table
		user.setOpenId("no-invitation-check");
		try {
			user = userLocalService.updateUser(user);
		} catch (SystemException e) {
			e.printStackTrace();
		}
		
		try {
			updateInvitation(invitation);
		} catch (SystemException e) {
			e.printStackTrace();
		}		
	}

	private String getChain(long inviterId) {

		StringBuilder sb = new StringBuilder(String.valueOf(inviterId));
		try {
			Invitation invitation = invitationPersistence.fetchByInviteeNewUserId(inviterId);
			
			if (Validator.isNotNull(invitation)) {
				sb.append(StringPool.COMMA);
				sb.append(getChain(invitation.getUserId()));
			} 
		} catch (SystemException e) {
			e.printStackTrace();
		}
		
		return sb.toString();
	}
	
	public boolean isNewEmail(long companyId, String emailAddress) {
		
		boolean flag = true;
		
		try {
			User user = userLocalService.getUserByEmailAddress(companyId, emailAddress);
			flag = !Validator.isNotNull(user);
		} catch (PortalException e) {
			e.printStackTrace();
		} catch (SystemException e) {
			e.printStackTrace();
		}
		
		return flag;
	}
	
	public List<Invitation> getByUserId(long userId, int status) {
		
		List<Invitation> invitations = null;
		
		try {
			invitations = (status < 0) ? invitationPersistence
					.findByUserId(userId) : invitationPersistence
					.findByUserId_Status(userId, status);
		} catch (SystemException e) {
			e.printStackTrace();
		}
		
		return invitations;
	}
}