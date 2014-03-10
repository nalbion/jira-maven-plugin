//package com.george.plugins.jira.api;
//
//
//import java.net.MalformedURLException;
//import java.net.URL;
//import java.util.ArrayList;
//import java.util.List;
//
//import javax.xml.rpc.ServiceException;
//
//import com.atlassian.jira.rpc.soap.client.JiraSoapService;
//import com.atlassian.jira.rpc.soap.client.JiraSoapServiceServiceLocator;
//import com.atlassian.jira.rpc.soap.client.RemoteVersion;
//
//public class JiraSoapApi extends JiraApi {
//	/**
//	 * This is the JIRA SOAP Suffix for accessing the webservice
//	 */
//	private static final String JIRA_SOAP_SUFFIX = "/rpc/soap/jirasoapservice-v2";
//
//
//	transient JiraSoapService jiraService;
//	
//	public JiraSoapApi( String jiraURL ) {
//		jiraService = getJiraSoapService(jiraURL);
//	}
//	
//	/**
//	 * Returns the stub needed to invoke the WebService
//	 * 
//	 * @return
//	 * @throws MalformedURLException
//	 * @throws ServiceException
//	 */
//	private JiraSoapService getJiraSoapService( String jiraURL )
//			throws MalformedURLException, ServiceException {
//		if (jiraService == null) {
//			JiraSoapServiceServiceLocator locator = new JiraSoapServiceServiceLocator();
//			String url = discoverJiraWSURL(jiraURL);
//			if (url == null)
//				throw new MalformedURLException(
//						"JIRA URL cound not be found. Check your pom.xml configuration.");
//			URL u = new URL(url);
//			jiraService = locator.getJirasoapserviceV2(u);
//		}
//		return jiraService;
//	}
//	
//	/**
//	 * Returns the formatted JIRA WebService URL
//	 * 
//	 * @return JIRA Web Service URL
//	 */
//	String discoverJiraWSURL( String jiraURL ) {
//		String url;
//		if (jiraURL == null) {
//			return null;
//		}
//		if (jiraURL.endsWith(JIRA_SOAP_SUFFIX)) {
//			url = jiraURL;
//		} else {
//			int projectIdx = jiraURL.indexOf("/browse");
//			if (projectIdx > -1) {
//				int lastPath = jiraURL.indexOf("/", projectIdx + 8);
//				if (lastPath == -1) {
//					lastPath = jiraURL.length();
//				}
//				jiraProjectKey = jiraURL.substring(projectIdx + 8, lastPath);
//				url = jiraURL.substring(0, projectIdx) + JIRA_SOAP_SUFFIX;
//			} else {
//				url = jiraURL + JIRA_SOAP_SUFFIX;
//			}
//		}
//		return url;
//	}
//	
//	@Override
//	public String login( String jiraUser, String jiraPassword ) {
//		return jiraService.login( jiraUser, jiraPassword );
//	}
//	
//	@Override
//	public void logout( String loginToken ) throws Exception {
//		jiraService.logout( loginToken );
//	}
//	
//	@Override
//	public List<JiraVersion> getVersions( String loginToken, String jiraProjectKey ) {
//		RemoteVersion[] array = jiraService.getVersions(loginToken, jiraProjectKey);
//		ArrayList<JiraVersion> versions = new ArrayList<String>(array.length);
//		
//		for( RemoteVersion version : array ) {
//			versions.add( new JiraVersion(version.getName()) );
//		}
//		
//		return versions;
//	}
//
//	@Override
//	public void addVersion(String loginToken, String jiraProjectKey, String newDevVersion) {
//		jiraService.addVersion( loginToken, jiraProjectKey, newDevVersion );
//	}
//
//	@Override
//	public void releaseVersion(String loginToken, String jiraProjectKey,
//			JiraVersion remoteReleasedVersion) {
//		jiraService.releaseVersion(loginToken, jiraProjectKey, remoteReleasedVersion);
//	}
//
//	@Override
//	public List<JiraIssue> getIssuesFromJqlSearch(String loginToken,
//			String jql, int maxIssues) {
//		RemoteIssue[] remoteIssues = jiraService.getIssuesFromJqlSearch(loginToken, jql, maxIssues);
//		
//		List<JiraIssue> issues = new ArrayList<JiraIssue>(remoteIssues.length);
//		for( RemoteIssue remoteIssue : remoteIssues ) {
//			JiraIssue issue = new JiraIssue();
//			issue.setKey( remoteIssue.getKey() );
//			issue.setSummary( remoteIssue.getSummary() );
//			issues.add( issue );
//		}
//		
//		return null;
//	}
//}
