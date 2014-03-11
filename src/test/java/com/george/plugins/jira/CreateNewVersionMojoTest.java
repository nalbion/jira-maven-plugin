/**
 * 
 */
package com.george.plugins.jira;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.george.plugins.jira.api.JiraApi;
import com.george.plugins.jira.api.JiraSoapApi;
import com.george.plugins.jira.api.JiraVersion;


/**
 * JUnit test case for Jira version MOJO
 * 
 * @author george
 * 
 */
public class CreateNewVersionMojoTest {

	private static final String LOGIN_TOKEN = "TEST_TOKEN";
	private static final List<JiraVersion> VERSIONS = new ArrayList<JiraVersion>();
	
	static {
		VERSIONS.add( new JiraVersion("1.0", false, null) );
		VERSIONS.add( new JiraVersion("2.0", false, null) );
		VERSIONS.add( new JiraVersion("3.0", false, null) );
		VERSIONS.add( new JiraVersion("3.1", false, null) );
	}

	private CreateNewVersionMojo jiraVersionMojo;
	private JiraApi jiraStub;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		this.jiraVersionMojo = new CreateNewVersionMojo();
		jiraVersionMojo.jiraUser = "user";
		jiraVersionMojo.jiraPassword = "password";

		// This removes the locator coupling
		jiraStub = EasyMock.createStrictMock(JiraApi.class);
		this.jiraVersionMojo.jiraApi = jiraStub;
	}

	/**
	 * Test method for {@link CreateNewVersionMojo#execute()}
	 * 
	 * @throws Exception
	 */
	@Test
	public void testExecuteWithNewDevVersion() throws Exception {
		jiraVersionMojo.developmentVersion = "5.0";
		doLoginBehavior();
		// Chama o getVersions
		expect(
				jiraStub.getVersions(LOGIN_TOKEN,
						jiraVersionMojo.jiraProjectKey)).andReturn(VERSIONS)
				.once();

		// Adiciona a nova versao
		expect(
				jiraStub.addVersion(LOGIN_TOKEN,
									jiraVersionMojo.jiraProjectKey, 
									jiraVersionMojo.developmentVersion))
						.andReturn(VERSIONS.get(0));
		doLogoutBehavior();
		// Habilita o controle para inicio dos testes
		EasyMock.replay(jiraStub);

		jiraVersionMojo.execute();
	}

	@Test
	public void testExecuteWithNewDevVersionIncludingQualifierAndSnapshot()
			throws Exception {
		jiraVersionMojo.developmentVersion = "5.0-beta-2-SNAPSHOT";
		doLoginBehavior();
		// Chama o getVersions
		expect(
				jiraStub.getVersions(LOGIN_TOKEN,
						jiraVersionMojo.jiraProjectKey)).andReturn(VERSIONS)
				.once();

		// Adiciona a nova versao
		expect(
				jiraStub.addVersion(LOGIN_TOKEN,
						jiraVersionMojo.jiraProjectKey, "5.0 Beta 2"))
				.andReturn(VERSIONS.get(0));
		doLogoutBehavior();
		// Habilita o controle para inicio dos testes
		EasyMock.replay(jiraStub);

		jiraVersionMojo.execute();
	}

	@Test
	public void testExecuteWithNewDevVersionAndUseFinalNameForVersionSetToTrue()
			throws Exception {
		jiraVersionMojo.developmentVersion = "5.0-beta-2-SNAPSHOT";
		jiraVersionMojo.finalNameUsedForVersion = true;
		jiraVersionMojo.finalName = "my-component-5.0-beta-2-SNAPSHOT";
		doLoginBehavior();
		// Chama o getVersions
		expect(
				jiraStub.getVersions(LOGIN_TOKEN,
						jiraVersionMojo.jiraProjectKey)).andReturn(VERSIONS)
				.once();

		// Adiciona a nova versao
		expect(
				jiraStub.addVersion(LOGIN_TOKEN,
							jiraVersionMojo.jiraProjectKey, 
							"My Component 5.0 Beta 2")).
						andReturn(VERSIONS.get(0));
		doLogoutBehavior();
		// Habilita o controle para inicio dos testes
		EasyMock.replay(jiraStub);

		jiraVersionMojo.execute();
	}

	/**
	 * Test method for {@link ReleaseVersionMojo#execute()}
	 * 
	 * @throws Exception
	 */
	@Test
	public void testExecuteWithExistentDevVersion() throws Exception {
		jiraVersionMojo.developmentVersion = "2.0";
		doLoginBehavior();
		// Chama o getVersions
		expect(
				jiraStub.getVersions(LOGIN_TOKEN,
						jiraVersionMojo.jiraProjectKey)).andReturn(VERSIONS)
				.once();

		doLogoutBehavior();
		// Habilita o controle para inicio dos testes
		replay(jiraStub);

		jiraVersionMojo.execute();
	}

	/**
	 * Set up logout mock behavior
	 * 
	 * @throws RemoteException
	 */
	private void doLogoutBehavior() throws Exception {
		expect(jiraStub.logout(LOGIN_TOKEN)).andReturn(Boolean.TRUE).once();
	}

	/**
	 * Set up login mock behavior
	 */
	private void doLoginBehavior() throws RemoteException {
		expect(jiraStub.login("user", "password")).andReturn(LOGIN_TOKEN)
				.once();
	}

	@After
	public void tearDown() {
		this.jiraVersionMojo = null;
	}
}
