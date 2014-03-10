package com.george.plugins.jira.api;

public class JiraIssueSummary {
	private String key;
	private String summary;
	
	public JiraIssueSummary( String key, String summary ) {
		this.key = key;
		this.summary = summary;
	}
	
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public String getSummary() {
		return summary;
	}
	public void setSummary(String summary) {
		this.summary = summary;
	}
}
