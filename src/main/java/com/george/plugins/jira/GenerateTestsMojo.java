package com.george.plugins.jira;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.xml.rpc.ServiceException;

import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.StringUtils;

import com.george.plugins.jira.api.JiraApi;
import com.george.plugins.jira.api.JiraIssue;
import com.george.plugins.jira.api.JiraRestApi;
import com.george.plugins.jira.test.TestScenario;
import com.george.plugins.jira.test.Story;
import com.george.plugins.jira.test.TestGenerator;


/**
 * Downloads Story and Test issues from JIRA and extracts BDD feature files to <code>target/generated-test-sources/cucumber</code>
 * 
 * @goal generate-tests
 * @phase generate-test-sources
 * 
 * @author Nicholas Albion
 */
public class GenerateTestsMojo extends AbstractJiraMojo {
	private static final int MAX_RESULTS = 1000;
	
	/**
	 * @parameter default-value="${project.build.directory}/generated-test-sources/cucumber"
	 */
	private String outputDirectory;
	
	/**
	 * The name of the TestGenerator implementation.  
	 * Can be a class relative to the package of TestGenerator, or the full package and class name can be provided 
	 * 
	 * @parameter default-value="GherkinFeatureGenerator"
	 */
	private String generatorClass;
	
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
	 * A comma-delimited list of issue types to be downloaded and parsed as stories
	 * Example: Epic,Story
	 * @parameter
	 */
	private String featureIssueTypes;
	
	/**
	 * A comma-delimited list of issue types to be downloaded and parsed as stories
	 * Example: Test
	 * @parameter
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
	
	
	protected JiraApi initialiseJiraApi() throws URISyntaxException, MalformedURLException, ServiceException {
		jiraApi = new JiraRestApi( jiraURL );
		return jiraApi;
	}

	/**
	 * Get all of the Epics and Stories that match <code>labels</code> <em>AND</em> <code>components</code>.
	 * If either of these parameters are null they are ignored.  
	 * If either of them contain commas they are treated as a <em>OR</em> list.
	 */
	@Override
	public void doExecute(JiraApi jiraApi, String loginToken) throws Exception {
		@SuppressWarnings("rawtypes")
		Class clazz = Class.forName( generatorClass.indexOf('.') > 0 ? generatorClass :
									TestGenerator.class.getPackage().getName() + "." + generatorClass );
		TestGenerator generator = (TestGenerator) clazz.newInstance();
		generator.configureFromMojo( this );
		
		StringBuilder fieldNames = new StringBuilder("issuetype,created,updated,project," +	// These fields are required by the JSON parser
													"summary,labels,components,issuelinks,parent,subtasks,status,resolution,reporter,assignee");
		
		if( customFields != null ) {
			getLog().debug("Looking up field IDs for custom fields: " + customFields);
			HashMap<String, String> fieldIdsByName = jiraApi.getFieldIdsByName(loginToken);
			for( String customFieldName : customFields.split(",") ) {
				appendCustomField( customFieldName, fieldNames, fieldIdsByName );
			}
		}
		
		// Download and parse Jira issue and generate tests locally.
		// A different strategy is adopted depending on which of featureIssueTypes and scenarioIssueTypes are defined. 
		if( featureIssueTypes != null && scenarioIssueTypes != null ) {
			generateStoriesWithFeatures( jiraApi, loginToken, fieldNames.toString(), generator );
		} else if( featureIssueTypes != null ) {
			generateStories( jiraApi, loginToken, fieldNames.toString(), generator );
		} else if( scenarioIssueTypes != null ) {
			generateTestScenarios( jiraApi, loginToken, fieldNames.toString(), generator );
		} else {
			throw new MojoFailureException("You must specify 'featureIssueTypes' and/or 'scenarioIssueTypes'");
		}
	}
	
	private void generateStoriesWithFeatures( JiraApi jiraApi, String loginToken, String fieldNames, TestGenerator generator ) throws RemoteException {
		LinkedList<Story> storyFiles = new LinkedList<Story>();
		HashMap<String, TestScenario> scenariosByKey = new HashMap<String, TestScenario>();
		
		// Download the Stories from Jira and parse into StoryFile objects
		for( JiraIssue issue : getIssues( loginToken, featureIssueTypes, labels, components, fieldNames.toString() ) ) {
			Story storyFile = generator.parseStory( issue );
			if( storyFile != null ) {
				storyFiles.add( storyFile );
			}
		}
		
		// Download the Test issues from Jira and parse into Scenario objects
		for( JiraIssue issue : getIssues( loginToken, scenarioIssueTypes, labels, components, fieldNames.toString() ) ) {
			TestScenario scenario = generator.parseScenario(issue);
			if( scenario != null ) {
				scenariosByKey.put( issue.getKey(), scenario );
			}
		}
		
		// As we add the parsed Scenarios into the Features, we remove them from orphannedScenarios.
		// Hopefully orphannedScenarios will be empty after this process, otherwise we will need to 
		// Create some new Features to contain each of the individual Scenarios. 
		HashMap<String, TestScenario> orphannedScenarios = new HashMap<String, TestScenario>();
		orphannedScenarios.putAll(scenariosByKey);
		
		for( Story storyFile : storyFiles ) {
			storyFile.addScenarios( scenariosByKey, orphannedScenarios );
		}
		
		if( !orphannedScenarios.isEmpty() ) {
			for( TestScenario scenario : orphannedScenarios.values() ) {
				storyFiles.add( generator.singleScenarioStory(scenario) );
			}
		}
		
		// Now write the Feature files
		for( Story storyFile : storyFiles ) {
			try {
				storyFile.writeToFile(outputDirectory);
			} catch (IOException e) {
				getLog().error("Failed to write feature to file", e);
			}
		}
	}
	
	private void generateStories( JiraApi jiraApi, String loginToken, String fieldNames, TestGenerator generator ) throws RemoteException {
		for( JiraIssue issue : getIssues( loginToken, featureIssueTypes, labels, components, fieldNames.toString() ) ) {
			Story storyFile = generator.parseStory( issue );
			if( storyFile != null ) {
				try {
					storyFile.writeToFile(outputDirectory);
				} catch (IOException e) {
					getLog().error("Failed to write feature to file", e);
				}
			}
		}
	}
	
	private void generateTestScenarios( JiraApi jiraApi, String loginToken, String fieldNames, TestGenerator generator ) throws RemoteException {
		for( JiraIssue issue : getIssues( loginToken, scenarioIssueTypes, labels, components, fieldNames.toString() ) ) {
			TestScenario scenario = generator.parseScenario(issue);
		
			if( scenario != null ) {
				try {
					scenario.writeToFile(outputDirectory);
				} catch (IOException e) {
					getLog().error("Failed to write test scenario to file", e);
				}
			}
		}
	}
	
	
	public String getFeatureFileUrlFieldName() {
		return featureFileUrlFieldName;
	}

	public String getNarrativeFieldName() {
		return narrativeFieldName;
	}

	public String getScenarioFieldName() {
		return scenarioFieldName;
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
	 * @throws RemoteException 
	 */
	private List<JiraIssue> getIssues( String loginToken, String issueType, String labels, String components, String fieldNames ) throws RemoteException {
		getLog().info("Loading " + issueType + " issues from JIRA project " + jiraProjectKey);
		getLog().info("  for labels: " + labels + ", components: " + components + ", fieldNames: " + fieldNames);
		StringBuilder jql = new StringBuilder("project=").append(jiraProjectKey);
		appendJqlFieldFilter( jql, "issuetype", issueType );
		appendJqlFieldFilter( jql, "labels", labels );
		appendJqlFieldFilter( jql, "component", components );
			
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
