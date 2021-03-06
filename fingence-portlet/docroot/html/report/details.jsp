<%@page import="com.fingence.slayer.service.PortfolioItemServiceUtil"%>

<%@ include file="/html/report/init.jsp"%>

<%
	long portfolioId = ParamUtil.getLong(renderRequest, "portfolioId");
	String currentPrice = ParamUtil.getString(renderRequest, "currentPrice");
	Portfolio portfolio = PortfolioLocalServiceUtil.fetchPortfolio(portfolioId);
	String backURL = ParamUtil.getString(request, "backURL");
	String managerName = BridgeServiceUtil.getUserName(portfolio.getRelationshipManagerId());
	
	int portfolioItemCount = PortfolioItemServiceUtil.getPortfolioItems(portfolioId).size();
%>

<aui:fieldset>
	<aui:row>
		<aui:column columnWidth="100"><b>Portfolio Name :</b> <%= portfolio.getPortfolioName() %></aui:column>
		<aui:column columnWidth="100"><b>Market Value (USD) :</b> <%= currentPrice %></aui:column>
	</aui:row>
</aui:fieldset>

<aui:fieldset>
	<aui:row>
		<aui:column columnWidth="20"><b>Investor</b></aui:column>
		<aui:column columnWidth="20"><b>Managed By</b></aui:column>
		<aui:column columnWidth="20"><b>Wealth Advisor</b></aui:column>
		<aui:column columnWidth="20"><b>Institution</b></aui:column>
		<aui:column columnWidth="20"><b>Base Currency</b></aui:column>
	</aui:row>
	
	<aui:row>
		<aui:column columnWidth="20"><%= BridgeServiceUtil.getUserName(portfolio.getInvestorId()) %></aui:column>
		<aui:column columnWidth="20"><%= (Validator.isNull(managerName) ? "Not Assigned" : managerName) %></aui:column>
		<aui:column columnWidth="20"><%= BridgeServiceUtil.getUserName(portfolio.getWealthAdvisorId()) %></aui:column>
		<aui:column columnWidth="20"><%= BridgeServiceUtil.getOrganizationName(portfolio.getInstitutionId()) %></aui:column>
		<aui:column columnWidth="20"><%= portfolio.getBaseCurrency() %></aui:column>
	</aui:row>	
</aui:fieldset>

<br/><a href="javascript:void(0);" onClick="javascript:updateItem(0, <%= portfolioId %>)">Add Asset</a><hr/>
<div id="myDataTable"></div>

<aui:script>
	<c:if test="<%= portfolioItemCount > 0 %>">
		AUI().ready(function(A) {
			Liferay.Service(
				'/fingence-portlet.myresult/get-my-results',
				{
					portfolioIds : '<%= portfolioId %>'
				},
				function(data) {
					displayItemsGrid(data, '#myDataTable', getTotalNetWorth(data));
				}
			);
		});
	</c:if>
</aui:script>