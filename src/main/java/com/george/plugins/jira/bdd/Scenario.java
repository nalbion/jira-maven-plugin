package com.george.plugins.jira.bdd;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.george.plugins.jira.api.JiraIssue;


public class Scenario extends Taggable {
	private static Pattern TITLE_PATTERN = Pattern.compile("(Scenario|Scenario Outline):\\s*([^\\r\\n]+)[\\r\\n]+(.*)", Pattern.DOTALL | Pattern.MULTILINE);
	
	/** "Scenario" or "Scenario Outline" */
	private String scenarioType;
	private String title;
	private String body;
	
	public Scenario( String description, JiraIssue issue ) {
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
}
