package com.george.plugins.jira;

import java.util.Comparator;

import com.george.plugins.jira.api.JiraVersion;

public class RemoteVersionComparator implements Comparator<JiraVersion> {

	@Override
	public int compare(JiraVersion o1, JiraVersion o2) {
		return doComparison(o1, o2);
	}

	public static int doComparison(JiraVersion o1, JiraVersion o2) {
		return -1 * o1.getName().compareToIgnoreCase(o2.getName());
	}

}
