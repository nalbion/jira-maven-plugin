package com.george.plugins.jira.api;


import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.xml.rpc.ServiceException;

import com.atlassian.jira.rpc.soap.client.JiraSoapService;
import com.atlassian.jira.rpc.soap.client.JiraSoapServiceServiceLocator;
import com.atlassian.jira.rpc.soap.client.RemoteAuthenticationException;
import com.atlassian.jira.rpc.soap.client.RemoteComponent;
import com.atlassian.jira.rpc.soap.client.RemoteCustomFieldValue;
import com.atlassian.jira.rpc.soap.client.RemoteException;
import com.atlassian.jira.rpc.soap.client.RemoteField;
import com.atlassian.jira.rpc.soap.client.RemoteIssue;
import com.atlassian.jira.rpc.soap.client.RemotePermissionException;
import com.atlassian.jira.rpc.soap.client.RemoteVersion;

/**
 * Note: The JIRA team has made the conscious decision to stop investing in SOAP. All their efforts will be with REST.
 */
public class JiraSoapApi implements JiraApi {
	/**
	 * This is the JIRA SOAP Suffix for accessing the webservice
	 */
	public static final String JIRA_SOAP_SUFFIX = "/rpc/soap/jirasoapservice-v2";

	transient JiraSoapService jiraService;
	private String jiraProjectKey;
	
	
	public JiraSoapApi( String jiraURL ) throws MalformedURLException, ServiceException {
		jiraService = getJiraSoapService(jiraURL);
	}
	
	@Override
	public String getProjectKey() {
		return jiraProjectKey;
	}
	
	/**
	 * Returns the stub needed to invoke the WebService
	 * 
	 * @return
	 * @throws MalformedURLException
	 * @throws ServiceException
	 */
	private JiraSoapService getJiraSoapService( String jiraURL )
			throws MalformedURLException, ServiceException {
		if (jiraService == null) {
			JiraSoapServiceServiceLocator locator = new JiraSoapServiceServiceLocator();
			String url = discoverJiraWSURL(jiraURL);
			if (url == null)
				throw new MalformedURLException(
						"JIRA URL cound not be found. Check your pom.xml configuration.");
			URL u = new URL(url);
			jiraService = locator.getJirasoapserviceV2(u);
		}
		return jiraService;
	}
	
	/**
	 * Returns the formatted JIRA WebService URL
	 * 
	 * @return JIRA Web Service URL
	 */
	private String discoverJiraWSURL( String jiraURL ) {
		String url;
		if (jiraURL == null) {
			return null;
		}
		if (jiraURL.endsWith(JIRA_SOAP_SUFFIX)) {
			url = jiraURL;
		} else {
			int projectIdx = jiraURL.indexOf("/browse");
			if (projectIdx > -1) {
				int lastPath = jiraURL.indexOf("/", projectIdx + 8);
				if (lastPath == -1) {
					lastPath = jiraURL.length();
				}
				jiraProjectKey = jiraURL.substring(projectIdx + 8, lastPath);
				url = jiraURL.substring(0, projectIdx) + JIRA_SOAP_SUFFIX;
			} else {
				url = jiraURL + JIRA_SOAP_SUFFIX;
			}
		}
		return url;
	}
	
	@Override
	public String login( String jiraUser, String jiraPassword ) throws RemoteAuthenticationException, RemoteException, java.rmi.RemoteException {
		return jiraService.login( jiraUser, jiraPassword );
	}
	
	@Override
	public boolean logout( String loginToken ) throws java.rmi.RemoteException {
		return jiraService.logout( loginToken );
	}
	
	@Override
	public List<JiraVersion> getVersions( String loginToken, String jiraProjectKey ) throws RemotePermissionException, RemoteAuthenticationException, RemoteException, java.rmi.RemoteException {
		RemoteVersion[] array = jiraService.getVersions(loginToken, jiraProjectKey);
		ArrayList<JiraVersion> versions = new ArrayList<JiraVersion>(array.length);
		
		for( RemoteVersion version : array ) {
			versions.add( new JiraVersion(version.getName()) );
		}
		
		return versions;
	}

	@Override
	public JiraVersion addVersion(String loginToken, String jiraProjectKey, String newVersion) throws RemoteException, java.rmi.RemoteException {
		RemoteVersion newRemoteVersion = new RemoteVersion();
		newRemoteVersion.setName(newVersion);
		jiraService.addVersion( loginToken, jiraProjectKey, newRemoteVersion );
		
		JiraVersion jiraVersion = new JiraVersion(newVersion);
		return jiraVersion;
	}

	@Override
	public void releaseVersion(String loginToken, String jiraProjectKey, JiraVersion releaseVersion) throws RemoteException, java.rmi.RemoteException {
		RemoteVersion remoteReleaseVersion = new RemoteVersion();
		remoteReleaseVersion.setName( releaseVersion.getName() );
		remoteReleaseVersion.setReleaseDate( releaseVersion.getReleaseDate() );
		remoteReleaseVersion.setReleased( true );
		jiraService.releaseVersion(loginToken, jiraProjectKey, remoteReleaseVersion);
	}

	@Override
	public List<JiraIssueSummary> getIssuesFromJqlSearch(String loginToken, String jql, int maxIssues) throws RemoteException, java.rmi.RemoteException {
		RemoteIssue[] remoteIssues = jiraService.getIssuesFromJqlSearch(loginToken, jql, maxIssues);
		
		List<JiraIssueSummary> issues = new ArrayList<JiraIssueSummary>(remoteIssues.length);
		for( RemoteIssue remoteIssue : remoteIssues ) {
			JiraIssueSummary issue = new JiraIssueSummary( remoteIssue.getKey(), remoteIssue.getSummary() );
			issues.add( issue );
		}
		
		return issues;
	}
	
	public List<JiraIssue> getIssuesFromJqlSearch( String loginToken, String jql, String fieldNames, int maxIssues ) throws RemoteException, java.rmi.RemoteException {
		RemoteIssue[] remoteIssues = jiraService.getIssuesFromJqlSearch(loginToken, jql, maxIssues);
		
		// make it easier to find the components
//		fieldNames = "," + fieldNames + ",";
		String[] fieldNameArray = fieldNames.split(",");
		Arrays.sort( fieldNameArray );
		
		if( Arrays.binarySearch( fieldNameArray, "labels") >= 0 ) {	//fieldNames.contains(",labels,") 
			throw new IllegalArgumentException("The SOAP UI does not provide access to labels - use the REST API (https://jira.atlassian.com/browse/JRA-23507)" );
		}
		if( Arrays.binarySearch( fieldNameArray, "parent") >= 0 ) {
			throw new IllegalArgumentException("The SOAP UI does not provide access to the parent - use the REST API" );
		}
		if( Arrays.binarySearch( fieldNameArray, "issuelinks") >= 0 ) {
			throw new IllegalArgumentException("The SOAP UI does not provide access to the issuelinks - use the REST API" );
		}
		if( Arrays.binarySearch( fieldNameArray, "subtasks") >= 0 ) {
			throw new IllegalArgumentException("The SOAP UI does not provide access to the subtasks - use the REST API" );
		}
		
		List<JiraIssue> issues = new ArrayList<JiraIssue>(remoteIssues.length);
		for( RemoteIssue remoteIssue : remoteIssues ) {
			JiraIssue issue = new JiraIssue( remoteIssue.getKey(), remoteIssue.getSummary() );
			
			issue.setIssueType( remoteIssue.getType() );
			issue.setDescription( remoteIssue.getDescription() );
			issue.setAssignee( remoteIssue.getAssignee() );
			issue.setReporter( remoteIssue.getReporter() );
			issue.setResolution( remoteIssue.getResolution() );
			issue.setStatus( remoteIssue.getStatus() );
	
			if( Arrays.binarySearch( fieldNameArray, "components") >= 0 //fieldNames.contains(",components,") 
					&& remoteIssue.getComponents() != null ) {
				ArrayList<String> components = new ArrayList<String>( remoteIssue.getComponents().length );
				for( RemoteComponent component : remoteIssue.getComponents() ) {
					components.add( component.getName() );
				}
				issue.setComponents( components );
			}
			
			HashMap<String, Object> fieldsByName = new HashMap<String, Object>();
			for( RemoteCustomFieldValue customField : remoteIssue.getCustomFieldValues() ) {
				String fieldName = customField.getKey();
				if( Arrays.binarySearch( fieldNameArray, fieldName) >= 0 ) { // fieldNames.contains("," + fieldName + ",") ) {
					fieldsByName.put( fieldName, customField.getValues() ); 
				}
			}
			issue.setFieldsByName(fieldsByName);
			
			issues.add( issue );
		}
		
		return issues;
	}

	@Override
	public HashMap<String, String> getFieldIdsByName( String loginToken ) throws RemoteException, java.rmi.RemoteException {
		HashMap<String, String> fieldsByName = new HashMap<String, String>();
		for( RemoteField remoteField : jiraService.getCustomFields(loginToken) ) {
			fieldsByName.put( remoteField.getName(), remoteField.getId() );
		}
		return fieldsByName;
	}
}
