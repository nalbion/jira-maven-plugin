package com.george.plugins.jira.test.gherkin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.george.plugins.jira.api.JiraIssue;


public class GherkinScenario extends Taggable implements com.george.plugins.jira.test.TestScenario {
	private static Pattern TITLE_PATTERN = Pattern.compile("(Scenario|Scenario Outline):\\s*([^\\r\\n]+)[\\r\\n]+(.*)", Pattern.DOTALL | Pattern.MULTILINE);
	
	/** "Scenario" or "Scenario Outline" */
	private String scenarioType;
	private String title;
	private String body;
	
	public GherkinScenario( String description, JiraIssue issue ) {
		super(issue);
		
		description = description.trim();
		
		Matcher matcher = TITLE_PATTERN.matcher(description);
		if( matcher.matches() ) {
			scenarioType = matcher.group(1);
			title = matcher.group(2);
			body = matcher.group(3);
		} else {
			scenarioType = "Scenario";
			title = issue.getSummary();
			body = description;
		}
	}
	
	@Override
	public String formatForCucumber() {
		StringBuilder str = getTags("  ");
		
		str.append("  ").append(scenarioType).append(": ").append(title).append(EOL);
		
		// Indent each line
		String[] lines = body.split("\\r\\n|\\r|\\n");
		for( String line : lines ) {
			if( !line.startsWith("    ") ) {
				str.append("    ");
			}
			str.append(line).append(EOL);
		}
		
		return str.toString();
	}
	
	/**
	 * @param directory - eg: "target/generated-test-sources/cucumber"
	 * @throws IOException 
	 */
	public void writeToFile( String directory ) throws IOException {
//		throw new IllegalAccessError("Scenarios must belong to a Feature - you must define featureIssueTypes in the plugin configuration");
		BufferedWriter out = null;
		try {
			new File( directory ).mkdirs();
			File file = new File( directory, title.replaceAll("[^\\w\\d]", "_") + ".feature" );
			out = new BufferedWriter( new FileWriter(file) );
			
			out.write( "Feature: " + title );
			out.write( EOL );
			out.write( EOL );
			out.write( formatForCucumber() );
		} finally {
			if( out != null ) {
				out.close();
			}
		}
	}
}
