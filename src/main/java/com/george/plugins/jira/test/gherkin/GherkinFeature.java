package com.george.plugins.jira.test.gherkin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.atlassian.jira.rest.client.api.domain.Issue;
import com.george.plugins.jira.api.JiraIssue;
import com.george.plugins.jira.api.JiraIssueSummary;
import com.george.plugins.jira.test.TestScenario;
import com.george.plugins.jira.test.Story;

public class GherkinFeature extends Taggable implements Story {
	private String title;
	private String storyFileUrl;
	/** 
	 * Set by the constructor by concatenating <code>narrative</code> and <code>scenarioText</code>, or reading the <code>storyFileUrl</code>
	 * does not include the tags 
	 */
	private String featureText;
	private List<TestScenario> scenarios;
	/** populated when we first read the {@link Issue}'s subtasks */
	private List<String> scenarioKeys;
	
	
	private GherkinFeature( JiraIssue issue ) {
		super(issue);
		title = issue.getSummary();
		
		JiraIssueSummary parent = issue.getParent();
		if( parent != null ) {
			componentAndLabelTags.add( "@" + parent.getKey() );
		}
		
		scenarioKeys = issue.getSubtaskKeys();
	}
	
	public GherkinFeature( String title ) {
		this.title = title;
	}
	
	public GherkinFeature( String storyFileUrl, JiraIssue issue ) {
		this(issue);
		this.storyFileUrl = storyFileUrl;
	}
	
	/**
	 * @param narrative - should follow the "As a &lt;ROLE> I want &lt;FEATURE> So that &lt;Benefit>" convention
	 * @param scenarioText - should start with "Scenario: " or "Scenario Outline: " or will be ignored
	 * @param issue
	 */
	public GherkinFeature( String narrative, String scenarioText, JiraIssue issue ) {
		this(issue);
		
//		IssueField field = issue.getFieldByName("User Story");
//		if( field != null ) {
//			storyFileUrl = (String)field.getValue();
//		}
		
		StringBuilder str = new StringBuilder();
		str.append("Feature: ").append(title).append(EOL);
		if( narrative != null ) {
			str.append(narrative).append(EOL).append(EOL);
		}
		if( scenarioText != null ) {
			scenarioText = scenarioText.trim();
			if( scenarioText.startsWith("Scenario: ") || scenarioText.startsWith("Scenario Outline: ") ) {
				str.append(scenarioText).append(EOL).append(EOL);
			}
		}
		featureText = str.toString();
	}

	public void addScenario( TestScenario scenario ) {
		if( scenarios == null ) {
			scenarios = new LinkedList<TestScenario>();
		}
		
		scenarios.add(scenario);
	}
	
	/**
	 * @param scenariosByKey
	 * @param orphannedScenarios - any Scenarios that belong to this Feature will be removed from this Map 
	 */
	public void addScenarios( Map<String, TestScenario> scenariosByKey, Map<String, TestScenario> orphannedScenarios ) {
		if( scenarios == null ) {
			scenarios = new LinkedList<TestScenario>();
		}
		
		for( String key : scenariosByKey.keySet() ) {
			if( scenarioKeys.contains(key) ) {
				scenarios.add( scenariosByKey.get(key) );
				orphannedScenarios.remove(key);
			}
		}
		
		// We don't need these anymore
		scenarioKeys.clear();
		scenarioKeys = null;
	}
		
	@Override
	public String formatForCucumber() {
		StringBuilder str = getTags(null);
		
		// Write the Feature title, narrative and any scenarios declared in the Story/Epic
		appendFeatureText( str );
		
		// Add scenarios from Test issues
		if( scenarios != null ) {
			for( TestScenario scenario : scenarios ) {
				str.append( ((GherkinScenario)scenario).formatForCucumber() ).append(EOL);
			}
		}
		
		return str.toString();
	}
	
	/**
	 * @param directory - eg: "target/generated-test-sources/cucumber"
	 * @throws IOException 
	 */
	public void writeToFile( String directory ) throws IOException {
		BufferedWriter out = null;
		try {
			new File( directory ).mkdirs();
			File file = new File( directory, title.replaceAll("[^\\w\\d]", "_") + ".feature" );
			out = new BufferedWriter( new FileWriter(file) );
			out.write( formatForCucumber() );
		} finally {
			if( out != null ) {
				out.close();
			}
		}
	}

	private void appendFeatureText( StringBuilder str ) {
		if( storyFileUrl != null && !storyFileUrl.isEmpty() ) {
			BufferedReader in = null;
			try {
				URL url = new URL(storyFileUrl);
				in = new BufferedReader( new InputStreamReader(url.openStream()) );
				
				String line;
				while( (line = in.readLine()) != null ) {
					str.append(line);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				if( in != null ) {
					try {
						in.close();
					} catch (IOException e) {}
				}
			}
		} else if( featureText != null ){
			str.append(featureText);
		}
	}
}
