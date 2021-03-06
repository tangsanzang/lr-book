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

package com.slayer.model.impl;

import java.util.List;

import com.liferay.portal.kernel.exception.SystemException;
import com.slayer.model.LMSBorrowing;
import com.slayer.service.LMSBookLocalServiceUtil;

/**
 * The extended model implementation for the LMSBook service. Represents a row in the &quot;LMS_LMSBook&quot; database table, with each column mapped to a property of this class.
 *
 * <p>
 * Helper methods and all application logic should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.slayer.model.LMSBook} interface.
 * </p>
 *
 * @author Ahmed Hasan
 */
public class LMSBookImpl extends LMSBookBaseImpl {
	/*
	 * NOTE FOR DEVELOPERS:
	 *
	 * Never reference this class directly. All methods that expect a l m s book model instance should use the {@link com.slayer.model.LMSBook} interface instead.
	 */
	public LMSBookImpl() {
	}
	
	public List<LMSBorrowing> getLMSBorrowings() 
			throws SystemException {
		return LMSBookLocalServiceUtil.getBorrowings(getBookId());
	}
}