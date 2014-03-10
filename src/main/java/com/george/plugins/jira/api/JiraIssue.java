package com.george.plugins.jira.api;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class JiraIssue extends JiraIssueSummary {
	private JiraIssueSummary parent;
	private List<String> subtaskKeys;
	private List<String> components;
	private Set<String> labels;
	private Map<String, Object> fieldsByName;
	private String description;
	private String status;
	private String resolution;
	private String issueType;
	private String reporter;
	private String assignee;
	
	
	public JiraIssue( String key, String summary ) {
		super( key, summary );
	}
	
	
	public JiraIssueSummary getParent() {
		return parent;
	}

	public void setParent(JiraIssueSummary parent) {
		this.parent = parent;
	}

	public List<String> getSubtaskKeys() {
		return subtaskKeys;
	}

	public void setSubtaskKeys(List<String> subtaskKeys) {
		this.subtaskKeys = subtaskKeys;
	}

	public String getDescription() {
		return description;
	}
	
	public String getFieldByName( String fieldName ) {
		if( fieldsByName == null ) { return null; }
		return (String)fieldsByName.get(fieldName);
	}

	public List<String> getComponents() {
		return components;
	}

	public void setComponents(List<String> components) {
		this.components = components;
	}

	public Set<String> getLabels() {
		return labels;
	}

	public void setLabels(Set<String> labels) {
		this.labels = labels;
	}

	public Map<String, Object> getFieldsByName() {
		return fieldsByName;
	}

	public void setFieldsByName(Map<String, Object> fieldsByName) {
		this.fieldsByName = fieldsByName;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getResolution() {
		return resolution;
	}

	public void setResolution(String resolution) {
		this.resolution = resolution;
	}

	public String getIssueType() {
		return issueType;
	}

	public void setIssueType(String issueType) {
		this.issueType = issueType;
	}

	public String getReporter() {
		return reporter;
	}

	public void setReporter(String reporter) {
		this.reporter = reporter;
	}

	public String getAssignee() {
		return assignee;
	}

	public void setAssignee(String assignee) {
		this.assignee = assignee;
	}

	public void setDescription(String description) {
		this.description = description;
	}	
}
