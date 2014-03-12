package com.george.plugins.jira.test;

import java.io.IOException;
import java.util.Map;

/**
 * A collection of {@link TestScenario}s
 */
public interface Story {
	public void addScenario( TestScenario scenario );
	public void addScenarios( Map<String, TestScenario> scenariosByKey, 
							Map<String, TestScenario> orphannedScenarios );
	
	/**
	 * @param directory - eg: "target/generated-test-sources/cucumber"
	 * @throws IOException 
	 */
	public void writeToFile( String directory ) throws IOException;
}
