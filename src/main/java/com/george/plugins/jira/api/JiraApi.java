package com.george.plugins.jira.api;

import java.util.HashMap;
import java.util.List;

public abstract class JiraApi {
	protected String jiraProjectKey;
	
	public abstract String login( String jiraUser, String jiraPassword );
	public abstract void logout( String loginToken ) throws Exception;
	
	public abstract HashMap<String, String> getFieldsByName();
	public abstract List<JiraVersion> getVersions( String loginToken, String jiraProjectKey );
	public abstract void addVersion( String loginToken, String jiraProjectKey, String newDevVersion );
	public abstract void releaseVersion( String loginToken, String jiraProjectKey, JiraVersion remoteReleasedVersion );
	
	public abstract List<JiraIssueSummary> getIssuesFromJqlSearch( String loginToken, String jql, int maxIssues );
	
	/**
	 * 
	 * @param loginToken
	 * @param jql
	 * @param fieldNames - null, "*all" or a comma delimited list (issuetype,created,updated,project are required by the JSON parser)
	 * @param maxIssues
	 * @return
	 */
	public abstract List<JiraIssue> getIssuesFromJqlSearch( String loginToken, String jql, String fieldNames, int maxIssues );
}
