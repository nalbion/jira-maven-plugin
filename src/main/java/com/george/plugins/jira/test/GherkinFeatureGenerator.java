package com.george.plugins.jira.test;

import com.george.plugins.jira.GenerateTestsMojo;
import com.george.plugins.jira.api.JiraIssue;
import com.george.plugins.jira.test.gherkin.GherkinFeature;
import com.george.plugins.jira.test.gherkin.GherkinScenario;

/** Generates .feature files for Gherkin, Cucumber etc */
public class GherkinFeatureGenerator implements TestGenerator {
	private String featureFileUrlFieldName; 
	private String narrativeFieldName;
	private String scenarioFieldName;
	
	public void configureFromMojo( GenerateTestsMojo mojo ) {
		featureFileUrlFieldName = mojo.getFeatureFileUrlFieldName();
		narrativeFieldName = mojo.getNarrativeFieldName();
		scenarioFieldName = mojo.getScenarioFieldName();
	}
	
	@Override
	public Story parseStory( JiraIssue issue ) {
		if( featureFileUrlFieldName != null ) {
			String storyFileUrl = issue.getFieldByName(featureFileUrlFieldName);
			if( storyFileUrl != null && !storyFileUrl.isEmpty() ) {
				return new GherkinFeature(storyFileUrl, issue);
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
			return new GherkinFeature(narrative, acceptanceCriteria, issue);
		}
		
		// Didn't find anything
		return null;
	}
	
	@Override
	public Story singleScenarioStory( TestScenario scenario ) {
		GherkinFeature feature = new GherkinFeature( null, null );
		feature.addScenario(scenario);
		return feature;
	}

	@Override
	public GherkinScenario parseScenario( JiraIssue issue ) {
		String description = issue.getDescription();
		if( description == null ) {
			description = issue.getFieldByName("Description");
		}
		if( description != null ) {
			description = description.trim();
			if( description.startsWith("Scenario") || description.startsWith("Given ") || description.startsWith("When ") ) {
				return new GherkinScenario(description, issue);
			}
		}
		return null;
	}
}
