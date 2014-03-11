/**
 * 
 */
package com.george.plugins.jira;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.xml.rpc.ServiceException;

import org.easymock.EasyMock;
import org.easymock.internal.matchers.ArrayEquals;
import org.easymock.internal.matchers.Equals;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.atlassian.jira.rpc.soap.client.RemoteAuthenticationException;
import com.atlassian.jira.rpc.soap.client.RemoteVersion;
import com.george.plugins.jira.api.JiraApi;
import com.george.plugins.jira.api.JiraSoapApi;
import com.george.plugins.jira.api.JiraVersion;

/**
 * JUnit test case for Jira version MOJO
 * 
 * @author george
 * 
 */
public class ReleaseVersionMojoTest {

	private static final String LOGIN_TOKEN = "TEST_TOKEN";
	private static final List<JiraVersion> VERSIONS = new ArrayList<JiraVersion>();
	
	static {
		VERSIONS.add( new JiraVersion("1.0", false, null) );
		VERSIONS.add( new JiraVersion("2.0", false, null) );
		VERSIONS.add( new JiraVersion("3.0", false, null) );
		VERSIONS.add( new JiraVersion("3.1", false, null) );
	}

	private Calendar cal = Calendar.getInstance();
	private JiraVersion RELEASED_VERSION = new JiraVersion("3.0", true, cal);
	private ReleaseVersionMojo jiraVersionMojo;
	private JiraApi jiraStub;
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		this.jiraVersionMojo = new ReleaseVersionMojo();
		jiraVersionMojo.jiraUser = "user";
		jiraVersionMojo.jiraPassword = "password";

		// This removes the locator coupling
		jiraStub = EasyMock.createStrictMock(JiraApi.class);
		this.jiraVersionMojo.jiraApi = jiraStub;
	}

	/**
	 * Test method for
	 * {@link com.george.plugins.jira.ReleaseVersionMojo#discoverJiraWSURL()}.
	 * @throws ServiceException 
	 * @throws MalformedURLException 
	 */
	@Test
	public void testDiscoverJiraWSURL() throws Exception {
		// Exercise the SUT (System under test)
		String baseUrl = "http://jira.george.com";
		String actual = discoverJiraWSURL(baseUrl);
		String expected = baseUrl + JiraSoapApi.JIRA_SOAP_SUFFIX;
		assertEquals("JIRA WS URL does not match", expected, actual);
	}

	/**
	 * Test method for
	 * {@link com.george.plugins.jira.ReleaseVersionMojo#isVersionAlreadyPresent()}
	 * .
	 */
	@Test
	public void testVersionAlreadyPresent() {
		boolean actual = jiraVersionMojo.isVersionAlreadyPresent(VERSIONS,
				"1.0");
		boolean expected = true;
		assertEquals(expected, actual);
	}

	/**
	 * Test method for
	 * {@link com.george.plugins.jira.ReleaseVersionMojo#isVersionAlreadyPresent()}
	 * .
	 */
	@Test
	public void testVersionNotPresent() {
		boolean actual = jiraVersionMojo.isVersionAlreadyPresent(VERSIONS,
				"4.0");
		boolean expected = false;
		assertEquals(expected, actual);
	}

	@Test
	public void testLatestVersionInfo() throws Exception {
		jiraVersionMojo.remoteVersionComparator = new RemoteVersionComparator();
		String expected = "3.1";
		String actual = jiraVersionMojo.calculateLatestReleaseVersion(VERSIONS);
		assertEquals(expected, actual);
	}

	@Test
	public void testDiscoverWithProjectKey() throws Exception {
		String expected = "http://www.trt12.jus.br/jira" + JiraSoapApi.JIRA_SOAP_SUFFIX;
		String url = "http://www.trt12.jus.br/jira/browse/FERIAS";
		
		JiraSoapApi jira = new JiraSoapApi(url);
		Method discoverJiraWSURL = jira.getClass().getDeclaredMethod("discoverJiraWSURL", String.class);
		discoverJiraWSURL.setAccessible(true);
		String actual = (String)discoverJiraWSURL.invoke(jira, url);
		
		assertEquals(expected, actual);
		assertEquals("FERIAS", jira.getProjectKey());
	}

	@Test
	public void testDiscoverWithoutProjectKey() throws Exception {
		jiraVersionMojo.jiraProjectKey = "FERIAS";
		String expected = "http://www.trt12.jus.br/jira"
				+ JiraSoapApi.JIRA_SOAP_SUFFIX;
		String actual = discoverJiraWSURL("http://www.trt12.jus.br/jira");
		assertEquals(expected, actual);
		assertEquals("FERIAS", jiraVersionMojo.jiraProjectKey);
	}

	@Test
	public void testExecuteWithReleaseVersion() throws Exception {
		jiraVersionMojo.releaseVersion = "3.0";
		jiraVersionMojo.jiraURL = "http://www.trt12.jus.br/jira";

		// Chama o login
		doLoginBehavior();
		// Chama o getVersions
		expect(
				jiraStub.getVersions(LOGIN_TOKEN,
						jiraVersionMojo.jiraProjectKey)).andReturn(VERSIONS)
				.once();

		// Chama o releaseVersion
		EasyMock.reportMatcher(new Equals(LOGIN_TOKEN));
		EasyMock.reportMatcher(new Equals(jiraVersionMojo.jiraProjectKey));
		// Tem que fazer assim porque o equals compara com Calendar
		EasyMock.reportMatcher(new RemoteVersionMatcher(RELEASED_VERSION));
		jiraStub.releaseVersion(LOGIN_TOKEN, jiraVersionMojo.jiraProjectKey,
				RELEASED_VERSION);
		expectLastCall();
		// Faz logout
		doLogoutBehavior();
		// Habilita o controle para inicio dos testes
		replay(jiraStub);

		jiraVersionMojo.execute();
	}

	/**
	 * Set up logout mock behavior
	 * @throws Exception 
	 */
	private void doLogoutBehavior() throws Exception {
		expect(jiraStub.logout(LOGIN_TOKEN)).andReturn(Boolean.TRUE).once();
	}

	/**
	 * Set up login mock behavior
	 */
	private void doLoginBehavior() throws RemoteException,
			RemoteAuthenticationException,
			com.atlassian.jira.rpc.soap.client.RemoteException {
		expect(jiraStub.login("user", "password")).andReturn(LOGIN_TOKEN)
				.once();
	}
	
	private String discoverJiraWSURL( String url ) throws Exception {
		JiraSoapApi jira = new JiraSoapApi(url);
		Method discoverJiraWSURL = jira.getClass().getDeclaredMethod("discoverJiraWSURL", String.class);
		discoverJiraWSURL.setAccessible(true);
		return (String)discoverJiraWSURL.invoke(jira, url);
	}

	@After
	public void tearDown() {
		this.jiraVersionMojo = null;
	}

	private static class RemoteVersionMatcher extends ArrayEquals {

		public RemoteVersionMatcher(Object obj) {
			super(obj);
		}

		@Override
		public boolean matches(Object actual) {
			if (actual instanceof RemoteVersion) {
				return (RemoteVersionComparator.doComparison(
						(JiraVersion) getExpected(), (JiraVersion) actual) == 0);
			}
			return super.matches(actual);
		}

	}
}
