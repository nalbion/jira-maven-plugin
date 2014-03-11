package com.george.plugins.jira.api;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.joda.time.DateTime;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.BasicComponent;
import com.atlassian.jira.rest.client.api.domain.Field;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueField;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.atlassian.jira.rest.client.api.domain.Subtask;
import com.atlassian.jira.rest.client.api.domain.Version;
import com.atlassian.jira.rest.client.api.domain.input.VersionInput;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;

/**
 * Note: The JIRA team has made the conscious decision to stop investing in SOAP. All their efforts will be with REST.
 */
public class JiraRestApi implements JiraApi {
	private String jiraProjectKey;
	private JiraRestClient restClient;
	private URI jiraServerUri;
	
	public JiraRestApi( String jiraUrl ) throws URISyntaxException {
		if( jiraUrl == null ) {
			throw new URISyntaxException("JIRA URL cound not be found. Check your pom.xml configuration.", "jiraUrl is null");
		}
		
		int projectIdx = jiraUrl.indexOf("/browse");
		if (projectIdx > -1) {
			int lastPath = jiraUrl.indexOf('/', projectIdx + 8);
			if (lastPath == -1) {
				lastPath = jiraUrl.length();
			}
			jiraProjectKey = jiraUrl.substring(projectIdx + 8, lastPath);
		}
		
		int slash = jiraUrl.indexOf('/');
		if( (slash < jiraUrl.length() - 1) && jiraUrl.charAt(slash + 1) == '/' ) {
			slash = jiraUrl.indexOf('/', slash + 2);
		}
		jiraServerUri = new URI( jiraUrl.substring(0, slash) );
		System.out.println("JIRA Server URI: " + jiraServerUri + ", project key: " + jiraProjectKey);
	}
	
	@Override
	public String getProjectKey() {
		return jiraProjectKey;
	}
	
	@Override
	public String login( String jiraUser, String jiraPassword ) {
		final AsynchronousJiraRestClientFactory factory = new AsynchronousJiraRestClientFactory();
		restClient = factory.createWithBasicHttpAuthentication(jiraServerUri, jiraUser, jiraPassword);
		return null;
	}

	@Override
	public boolean logout( String loginToken ) throws Exception {
		restClient.destroy();
		return true;
	}
	
	@Override
	public HashMap<String, String> getFieldIdsByName( String loginToken ) {
		HashMap<String, String> customFieldIdsByName = new HashMap<String, String>();
		Iterable<Field> jiraFields = restClient.getMetadataClient().getFields().claim();
		for( Field field : jiraFields ) {
			String id = field.getId();
			String name = field.getName();
			customFieldIdsByName.put( name, id );
		}
		return customFieldIdsByName;
	}
	
	@Override
	public List<JiraVersion> getVersions( String loginToken, String jiraProjectKey ) {
		LinkedList<JiraVersion> versions = new LinkedList<JiraVersion>();
		for( Version version : restClient.getProjectClient().getProject(jiraProjectKey).claim().getVersions() ) {
			DateTime releaseDate = version.getReleaseDate();
			JiraVersion jiraVersion = new JiraVersion( version.getName(), 
														version.isReleased(), 
														releaseDate == null ? 
																null : 
																releaseDate.toCalendar(Locale.getDefault()) );
			versions.add( jiraVersion );
		}
		
		return versions;
	}
	
	@Override
	public JiraVersion addVersion( String loginToken, String jiraProjectKey, String newVersion ) {
		restClient.getVersionRestClient().createVersion( new VersionInput(jiraProjectKey, newVersion, null, null, false, false) );
		JiraVersion jiraVersion = new JiraVersion(newVersion);
		return jiraVersion;
	}

	@Override
	public void releaseVersion(String loginToken, String jiraProjectKey, JiraVersion remoteReleasedVersion) {
//		VersionRestClient versionClient = restClient.getVersionRestClient();
//		versionClient.
		throw new RuntimeException("releaseVersion has not yet been implemented for the REST API");
	}
	
	public List<JiraIssueSummary> getIssuesFromJqlSearch( String loginToken, String jql, int maxIssues ) {
		String fieldNames = "issuetype,created,updated,project,key,summary";
		
		SearchResult searchResult = restClient.getSearchClient().searchJql( jql, maxIssues, 0, fieldNames ).claim();
		
		LinkedList<JiraIssueSummary> issues = new LinkedList<JiraIssueSummary>();
		for( Issue issue : searchResult.getIssues() ) {
			issues.add( new JiraIssueSummary( issue.getKey(), issue.getSummary() ) );
		}
		
		return issues;
	}
	
	@Override
	public List<JiraIssue> getIssuesFromJqlSearch( String loginToken, String jql, String fieldNames, int maxIssues ) {
		SearchResult searchResult = restClient.getSearchClient().searchJql( jql, maxIssues, 0, fieldNames ).claim();
		
		LinkedList<JiraIssue> issues = new LinkedList<JiraIssue>();
		for( Issue issue : searchResult.getIssues() ) {
			JiraIssue jiraIssue = new JiraIssue( issue.getKey(), issue.getSummary() );
			
			// Extract the parent's key
			IssueField field = issue.getField("parent");
			if( field != null && field.getValue() != null ) {
				try {
					JSONObject parentJson = (JSONObject)field.getValue();
					jiraIssue.setParent( new JiraIssueSummary( (String)parentJson.get("key"), "parent" ) );
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			
			// Extract the key of all subtasks
			Iterable<Subtask> subtasks = issue.getSubtasks();
			if( subtasks != null ) {
				Iterator<Subtask> itr = subtasks.iterator();
				if( itr != null ) {
					LinkedList<String> scenarioKeys = new LinkedList<String>();
					
					while( itr.hasNext() ) {
						scenarioKeys.add( itr.next().getIssueKey() );
					}
					
					jiraIssue.setSubtaskKeys( scenarioKeys );
				}
			}
			
			// Extract labels and components
			jiraIssue.setLabels( issue.getLabels() );
			if( issue.getComponents() != null ) {
				LinkedList<String> components = new LinkedList<String>();
				for( BasicComponent component : issue.getComponents() ) {
					components.add( component.getName() );
				}
				jiraIssue.setComponents( components );
			}
			
			
			jiraIssue.setDescription( issue.getDescription() );
			
			if( issue.getStatus() != null ) {
				jiraIssue.setStatus( issue.getStatus().getName() );
			}
			if( issue.getResolution() != null ) {
				jiraIssue.setResolution( issue.getResolution().getName() );
			}
			if( issue.getAssignee() != null ) {
				jiraIssue.setAssignee( issue.getAssignee().getName() );
			}
			if( issue.getReporter() != null ) {
				jiraIssue.setReporter( issue.getReporter().getName() );
			}
			
			HashMap<String, Object> fieldsByName = new HashMap<String, Object>();
			for( IssueField issueField : issue.getFields() ) {
				fieldsByName.put( issueField.getName(), issueField.getValue() );
			}
			jiraIssue.setFieldsByName( fieldsByName );
			
			jiraIssue.setIssueType( issue.getIssueType().getName() );
			
			issues.add( jiraIssue );
		}
		
		return issues;
	}
}
