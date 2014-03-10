package com.george.plugins.jira.api;

import java.util.Calendar;

public class JiraVersion {
	private String name;
	private boolean released;
	private Calendar releaseDate;
	
	public JiraVersion( String name ) {
		this.name = name;
	}
	
	public JiraVersion( String name, boolean released, Calendar releaseDate ) {
		this.name = name;
		this.released = released;
		this.releaseDate = releaseDate;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public boolean isReleased() {
		return released;
	}
	public void setReleased(boolean released) {
		this.released = released;
	}
	public Calendar getReleaseDate() {
		return releaseDate;
	}
	public void setReleaseDate(Calendar releaseDate) {
		this.releaseDate = releaseDate;
	}
}
