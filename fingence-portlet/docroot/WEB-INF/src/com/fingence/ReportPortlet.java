package com.fingence;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.PortletException;
import javax.portlet.PortletSession;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import com.fingence.slayer.model.Portfolio;
import com.fingence.slayer.service.PortfolioItemServiceUtil;
import com.fingence.slayer.service.PortfolioLocalServiceUtil;
import com.fingence.util.PrefsUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.upload.UploadPortletRequest;
import com.liferay.portal.kernel.util.Constants;
import com.liferay.portal.kernel.util.MethodKey;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.PortalClassInvoker;
import com.liferay.portal.kernel.util.PortalClassLoaderUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.service.PortletPreferencesLocalServiceUtil;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.PortletKeys;
import com.liferay.util.bridges.mvc.MVCPortlet;

/**
 * Portlet implementation class RegisterPortlet
 */
public class ReportPortlet extends MVCPortlet {

	public void serveResource(ResourceRequest resourceRequest,
			ResourceResponse resourceResponse) throws IOException,
			PortletException {

		String cmd = ParamUtil.getString(resourceRequest, Constants.CMD);
		PortletSession portletSession = resourceRequest.getPortletSession();

		if (cmd.equalsIgnoreCase(IConstants.CMD_SET_PORTFOLIO_ID)) {
			long portfolioId = ParamUtil
					.getLong(resourceRequest, "portfolioId");
			portletSession.setAttribute("PORTFOLIO_ID",
					String.valueOf(portfolioId),
					PortletSession.APPLICATION_SCOPE);
		} else if (cmd.equalsIgnoreCase(IConstants.CMD_SET_ALLOCATION_BY)) {
			int allocationBy = ParamUtil.getInteger(resourceRequest,
					"allocationBy");
			portletSession.setAttribute("ALLOCATION_BY",
					String.valueOf(allocationBy),
					PortletSession.APPLICATION_SCOPE);
		} else if (cmd.equalsIgnoreCase(IConstants.CMD_CHECK_DUPLICATE_PORTFOLIO)) {
			
			long portfolioId = ParamUtil.getLong(resourceRequest, "portfolioId", 0l);
			String portfolioName = ParamUtil.getString(resourceRequest, "portfolioName");
			
			long userId = PortalUtil.getUserId(resourceRequest);
			List<Portfolio> userPortfolios = PortfolioLocalServiceUtil.getPortfolios(userId);
			
			boolean flag = false;
			for (Portfolio portfolio: userPortfolios) {
				if ((portfolioId == 0l && portfolio.getPortfolioName().equalsIgnoreCase(portfolioName)) 
						|| (portfolioId > 0l && portfolioId != portfolio.getPortfolioId() && portfolio.getPortfolioName().equalsIgnoreCase(portfolioName))) {
					flag = true;
					break;
				}
			}
			
			PrintWriter writer = resourceResponse.getWriter();
			writer.println(flag);
		} else if (cmd.equalsIgnoreCase(IConstants.CMD_SET_ASSETS_TO_SHOW)) {
			int assetsToShow = ParamUtil.getInteger(resourceRequest, "assetsToShow", 5);
			
			long ownerId = PortalUtil.getUserId(resourceRequest);
			ThemeDisplay themeDisplay = (ThemeDisplay) resourceRequest.getAttribute(WebKeys.THEME_DISPLAY);
			
			String[] values = {String.valueOf(assetsToShow)};
									
			try {				
				PortletPreferencesLocalServiceUtil.updatePreferences(ownerId,
						PortletKeys.PREFS_OWNER_TYPE_USER, themeDisplay.getPlid(), themeDisplay.getPortletDisplay().getRootPortletId(),
						PrefsUtil.getPortletPreferencesXML("assetsToShow", values));
			} catch (SystemException e) {
				e.printStackTrace();
			}
		}
	}

	public void savePortfolio(ActionRequest actionRequest,
			ActionResponse actionResponse) throws IOException, PortletException {

		UploadPortletRequest uploadPortletRequest = PortalUtil
				.getUploadPortletRequest(actionRequest);

		long userId = PortalUtil.getUserId(actionRequest);
		String portfolioName = ParamUtil.getString(uploadPortletRequest, "portfolioName");
		String baseCurrency = ParamUtil.getString(uploadPortletRequest, "baseCurrency");

		long portfolioId = ParamUtil.getLong(uploadPortletRequest, "portfolioId");
		long investorId = ParamUtil.getLong(uploadPortletRequest, "investorId");
		long institutionId = ParamUtil.getLong(uploadPortletRequest, "institutionId");
		long wealthAdvisorId = ParamUtil.getLong(uploadPortletRequest, "wealthAdvisorId");
		long relationshipManagerId = ParamUtil.getLong(uploadPortletRequest, "relationshipManagerId");

		boolean trial = ParamUtil.getBoolean(uploadPortletRequest, "trial", false);
		boolean social = ParamUtil.getBoolean(uploadPortletRequest, "social", false);

		File excelFile = uploadPortletRequest.getFile("excelFile");
		
		PortfolioLocalServiceUtil.updatePortfolio(portfolioId, userId,
				portfolioName, investorId, institutionId, wealthAdvisorId,
				trial, relationshipManagerId, social,baseCurrency, excelFile);
		
		PortletSession portletSession = actionRequest.getPortletSession();
		portletSession.setAttribute("MENU_ITEM", IConstants.PAGE_REPORTS_HOME, PortletSession.APPLICATION_SCOPE);
		
		actionResponse.sendRedirect("/reports");
	}

	public void updatePortfolioItem(ActionRequest actionRequest,
			ActionResponse actionResponse) throws IOException, PortletException {
				
		String isinId = ParamUtil.getString(actionRequest, "isinId");
		long portfolioItemId = ParamUtil.getLong(actionRequest, "itemId");
		long portfolioId = ParamUtil.getLong(actionRequest, "portfolioId");
		String ticker = ParamUtil.getString(actionRequest, "ticker");
		String purchaseDate = ParamUtil.getString(actionRequest, "purchaseDate");
		
		double purchasePrice = Double.parseDouble(ParamUtil.getString(actionRequest, "purchasePrice").replaceAll(StringPool.COMMA, StringPool.BLANK));
		double purchaseQty = Double.parseDouble(ParamUtil.getString(actionRequest, "purchaseQty").replaceAll(StringPool.COMMA, StringPool.BLANK));
		double purchasedFx = Double.parseDouble(ParamUtil.getString(actionRequest, "purchasedFx", "0.0").replaceAll(StringPool.COMMA, StringPool.BLANK));
						
		PortfolioItemServiceUtil.updateItem(portfolioItemId, portfolioId,
				isinId, ticker, purchasePrice, purchaseQty, purchasedFx,
				purchaseDate);		
	}
	
	public void discussOnPortfolio(ActionRequest actionRequest,
			ActionResponse actionResponse) throws IOException, PortletException {

		String declaringClassName = "com.liferay.portlet.messageboards.action.EditDiscussionAction";
		Class<?> declaringClass = null;
		try {
			declaringClass = Class.forName(declaringClassName, true, PortalClassLoaderUtil.getClassLoader());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		String methodName = "processAction";
		
		Class<?> actionMappingClass = null;
		try {
			actionMappingClass = Class.forName("org.apache.struts.action.ActionMapping", true, PortalClassLoaderUtil.getClassLoader());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		Class<?> actionFormClass = null;
		try {
			actionFormClass = Class.forName("org.apache.struts.action.ActionForm", true, PortalClassLoaderUtil.getClassLoader());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}		
		
		Class<?>[] parameterTypes = {
			actionMappingClass,
			actionFormClass,
			PortletConfig.class, 
			ActionRequest.class,
			ActionResponse.class 
		};
		Object[] arguments = { null, null, getPortletConfig(), actionRequest,
				actionResponse };
		
		MethodKey methodKey = new MethodKey(declaringClass, methodName, parameterTypes);
		
		try {
			PortalClassInvoker.invoke(true, methodKey, arguments);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}