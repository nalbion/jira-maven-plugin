package com.george.plugins.jira.api;

import java.util.HashMap;
import java.util.List;

public interface JiraApi {
	
	public String getProjectKey();
	
	public String login( String jiraUser, String jiraPassword ) throws java.rmi.RemoteException;
	public boolean logout( String loginToken ) throws Exception;
	
	public HashMap<String, String> getFieldIdsByName( String loginToken ) throws java.rmi.RemoteException;
	public List<JiraVersion> getVersions( String loginToken, String jiraProjectKey ) throws java.rmi.RemoteException;
	public JiraVersion addVersion( String loginToken, String jiraProjectKey, String newDevVersion ) throws java.rmi.RemoteException;
	public void releaseVersion( String loginToken, String jiraProjectKey, JiraVersion remoteReleasedVersion ) throws java.rmi.RemoteException;
	
	public List<JiraIssueSummary> getIssuesFromJqlSearch( String loginToken, String jql, int maxIssues ) throws java.rmi.RemoteException;
	
	/**
	 * 
	 * @param loginToken
	 * @param jql
	 * @param fieldNames - null, "*all" or a comma delimited list (issuetype,created,updated,project are required by the JSON parser)
	 * @param maxIssues
	 * @return
	 */
	public List<JiraIssue> getIssuesFromJqlSearch( String loginToken, String jql, String fieldNames, int maxIssues ) throws java.rmi.RemoteException;
}
