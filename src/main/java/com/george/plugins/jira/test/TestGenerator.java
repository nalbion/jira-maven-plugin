package com.george.plugins.jira.test;

import com.atlassian.jira.rest.client.api.domain.Issue;
import com.george.plugins.jira.GenerateTestsMojo;
import com.george.plugins.jira.api.JiraIssue;

public interface TestGenerator {
	
	/** Extract required configuration from the mojo */
	public void configureFromMojo( GenerateTestsMojo mojo );
	
	/**
	 * Supports BDD Features defined directly in the Story's "User Story" and "Acceptance Criteria" fields
	 * OR at a URL provided in the <code>featureFileUrlFieldName</code> field.
	 * 
	 * @param issue
	 * @return a StoryFile - Contains the title, narrative and multiple Scenarios
	 */
	public Story parseStory( JiraIssue issue );
	
	public Story singleScenarioStory( TestScenario scenario );
	
	/**
	 * In order to parse a {@link TestScenario} from an {@link Issue} we need to find the <code>Given, When, Then</code>
	 * statements.
	 * 
	 * @param issue - a Test issue from Jira
	 * @return a test Scenario (Given ... When ... Then ...)
	 */
	public TestScenario parseScenario( JiraIssue issue );
}
