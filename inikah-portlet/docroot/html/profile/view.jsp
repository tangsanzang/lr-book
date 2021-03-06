<%@ include file="/html/common/init.jsp" %>

<%
	List<Profile> profiles = ProfileLocalServiceUtil.getProfilesForUser(user.getUserId());
%>

<liferay-ui:search-container delta="10" emptyResultsMessage="You don't have any profiles yet">

	<liferay-ui:search-container-results
		total="<%= profiles.size() %>"
		results="<%= ListUtil.subList(profiles, searchContainer.getStart(), searchContainer.getEnd()) %>"/>
		
	<liferay-ui:search-container-row className="Profile" modelVar="profile1">
		<liferay-ui:search-container-column-jsp path="/html/list/owner-view.jsp"  />
	</liferay-ui:search-container-row>
	
	<liferay-ui:search-iterator searchContainer="<%= searchContainer %>" />
	
</liferay-ui:search-container>