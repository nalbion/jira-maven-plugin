package com.george.plugins.jira.test;

import java.io.IOException;

public interface TestScenario {

	/**
	 * @param directory - eg: "target/generated-test-sources/cucumber"
	 * @throws IOException 
	 */
	public void writeToFile( String directory ) throws IOException;
}
