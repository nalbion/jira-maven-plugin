package com.george.plugins.jira;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.codehaus.plexus.util.StringUtils;

import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueField;
import com.george.plugins.jira.api.JiraApi;
import com.george.plugins.jira.api.JiraIssue;
import com.george.plugins.jira.bdd.Feature;
import com.george.plugins.jira.bdd.Scenario;


/**
 * Downloads Story and Test issues from JIRA and extracts BDD feature files to <code>target/generated-test-sources/cucumber</code>
 * 
 * @goal download-tests
 * @phase generate-test-sources
 * 
 * @author Nicholas Albion
 */
public class FeatureDownloadMojo extends AbstractJiraMojo {
	private static final int MAX_RESULTS = 1000;
	
	/**
	 * @parameter default-value="${project.build.directory}/generated-test-sources/cucumber"
	 */
	private String outputDirectory;
	
	/** 
	 * Comma-delimited list of labels to filter against
	 * @parameter 
	 */
	private String labels;
	/** 
	 * Comma-delimited list of labels to filter against
	 * @parameter 
	 */
	private String components;
	
	/**
	 * Optional comma-delimited list of custom fields to retrieve for each issue
	 * @parameter
	 */
	private String customFields;
	
	/**
	 * @parameter default-value="Epic,Story"
	 */
	private String featureIssueTypes;
	
	/**
	 * @parameter default-value="Test"
	 */
	private String scenarioIssueTypes;
		
	/**
	 * Use this if you have a custom field that provides a URL to a BDD feature file (eg Git web UI)
	 * @parameter
	 */
	private String featureFileUrlFieldName;
	
	/**
	 * The name of the field that holds the BDD narrative (As a &lt;ROLE> I want &lt;FEATURE> So that &lt;BENEFIT>)
	 * @parameter default-value="User Story"
	 */
	private String narrativeFieldName;
	
	/** 
	 * The name of the field that holds the BDD Scenario (Scenario: ... Given... When... Then...)
	 * @parameter default-value="Acceptance Criteria"
	 */
	private String scenarioFieldName;
	
	
	/**
	 * Get all of the Epics and Stories that match <code>labels</code> <em>AND</em> <code>components</code>.
	 * If either of these parameters are null they are ignored.  
	 * If either of them contain commas they are treated as a <em>OR</em> list.
	 */
	@Override
	public void doExecute(JiraApi jiraApi, String loginToken) throws Exception {
		StringBuilder fieldNames = new StringBuilder("issuetype,created,updated,project," +	// These fields are required by the JSON parser
													"summary,labels,components,issuelinks,parent,subtasks,status,resolution,reporter,assignee");
		if( customFields != null ) {
			HashMap<String, String> fieldIdsByName = jiraApi.getFieldsByName();
			for( String customFieldName : customFields.split(",") ) {
				appendCustomField( customFieldName, fieldNames, fieldIdsByName );
			}
		}
		
		LinkedList<Feature> features = new LinkedList<Feature>();
		HashMap<String, Scenario> scenariosByKey = new HashMap<String, Scenario>();
		
		for( JiraIssue issue : getIssues( loginToken, featureIssueTypes, labels, components, fieldNames.toString() ) ) {
			Feature feature = parseFeatureFromIssue( issue );
			if( feature != null ) {
				features.add( feature );
			}
		}
		
		for( JiraIssue issue : getIssues( loginToken, scenarioIssueTypes, labels, components, fieldNames.toString() ) ) {
			Scenario scenario = parseScenarioFromIssue(issue);
			if( scenario != null ) {
				scenariosByKey.put( issue.getKey(), scenario );
			}
		}
		
		
		// As we add the parsed Scenarios into the Features, we remove them from orphannedScenarios.
		// Hopefully orphannedScenarios will be empty after this process, otherwise we will need to 
		// Create some new Features to contain each of the individual Scenarios. 
		HashMap<String, Scenario> orphannedScenarios = new HashMap<String, Scenario>();
		orphannedScenarios.putAll(scenariosByKey);
		
		for( Feature feature : features ) {
			feature.addScenarios( scenariosByKey, orphannedScenarios );
		}
		
		if( !orphannedScenarios.isEmpty() ) {
			for( Scenario scenario : orphannedScenarios.values() ) {
				Feature feature = new Feature( null, null );
				feature.addScenario(scenario);
				features.add( feature );
			}
		}
		
		// Now write the Feature files
		for( Feature feature : features ) {
			try {
				feature.writeToFile(outputDirectory);
			} catch (IOException e) {
				getLog().error("Failed to write feature to file", e);
			}
		}
	}
	
	
	/**
	 * Supports BDD Features defined directly in the Story's "User Story" and "Acceptance Criteria" fields
	 * OR at a URL provided in the <code>featureFileUrlFieldName</code> field.
	 * 
	 * @param issue
	 * @return
	 */
	private Feature parseFeatureFromIssue( JiraIssue issue ) {
		IssueField field;
		
		if( featureFileUrlFieldName != null ) {
			String storyFileUrl = issue.getFieldByName(featureFileUrlFieldName);
			if( storyFileUrl != null && !storyFileUrl.isEmpty() ) {
				return new Feature(storyFileUrl, issue);
			}
		}
		
		String narrative = null;
		String acceptanceCriteria = null;
				
		String userStory = issue.getFieldByName(narrativeFieldName);
		if( userStory != null && !userStory.isEmpty() ) {
			narrative = userStory;
		}
		
		acceptanceCriteria = issue.getFieldByName(scenarioFieldName);
		if( acceptanceCriteria != null && acceptanceCriteria.isEmpty() ) {
			acceptanceCriteria = null;
		}
		
		
		if( narrative != null || acceptanceCriteria != null ) {
			return new Feature(narrative, acceptanceCriteria, issue);
		}
		
		// Didn't find anything
		return null;
	}
	
	/**
	 * In order to parse a {@link Scenario} from an {@link Issue} we need to find the <code>Given, When, Then</code>
	 * statements.
	 * 
	 * @param issue
	 * @return
	 */
	private Scenario parseScenarioFromIssue( JiraIssue issue ) {
		String description = issue.getDescription();
		if( description == null ) {
			description = issue.getFieldByName("Description");
		}
		if( description != null ) {
			description = description.trim();
			if( description.startsWith("Scenario") || description.startsWith("Given ") || description.startsWith("When ") ) {
				return new Scenario(description, issue);
			}
		}
		return null;
	}
	
	/**
	 * Get all of the issues that match <code>issueType</code>, <code>labels</code> <em>AND</em> <code>components</code>.
	 * If any of these parameters are null they are ignored.  
	 * If any of them contain commas they are treated as a <em>OR</em> list. 
	 * 
	 * @param loginToken - required for the SOAP API
	 * @param issueType - Story or Test
	 * @param labels - null, a single label name or a comma delimited list
	 * @param components - null, a single component name or a comma delimited list
	 * @param fieldNames - null, "*all" or a comma delimited list
	 * @return
	 */
	private List<JiraIssue> getIssues( String loginToken, String issueType, String labels, String components, String fieldNames ) {
		StringBuilder jql = new StringBuilder("project=").append(jiraProjectKey);
		appendJqlFieldFilter( jql, "issuetype", issueType );
		appendJqlFieldFilter( jql, "labels", labels );
		appendJqlFieldFilter( jql, "components", components );
			
		return jiraApi.getIssuesFromJqlSearch(loginToken, jql.toString(), fieldNames, MAX_RESULTS);
	}
	
	/**
	 * Used to prepare a JQL string to filter issues from JIRA
	 * @param jql
	 * @param fieldName - "issuetype", "labels", "components" etc
	 * @param values - null, a single string or a comma delimited list
	 */
	private void appendJqlFieldFilter( StringBuilder jql, String fieldName, String values ) {
		if( values != null ) {
			jql.append(" AND ").append(fieldName);
			if( values.indexOf(',') > 0 ) {
				jql.append(" in (\"")
					.append( StringUtils.join( values.split(","), "\",\"") )
					.append("\")");
			} else {
				jql.append("=\"").append(values).append('"');
			}
		}
	}

	/** Appends a custom field, if it has been defined on the JIRA server */
	private void appendCustomField( String fieldName, StringBuilder str, HashMap<String, String> customFieldIdsByName ) {
		String fieldId = customFieldIdsByName.get(fieldName);
		if( fieldId != null ) {
			str.append(',').append(fieldId);
		}
	}
}
