package com.george.plugins.jira;

import java.net.URISyntaxException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;

import com.george.plugins.jira.api.JiraApi;
import com.george.plugins.jira.api.JiraRestApi;
//import com.george.plugins.jira.api.JiraSoapApi;


/**
 * This class allows the use of {@link JiraSoapService} in JIRA Actions
 * 
 * @author george
 * 
 */
public abstract class AbstractJiraMojo extends AbstractMojo {

	/**
	 * @parameter expression="${settings}"
	 */
	Settings settings;

	/**
	 * Server's id in settings.xml to look up username and password.
	 * 
	 * @parameter expression="${settingsKey}"
	 */
	private String settingsKey;

	/**
	 * JIRA Installation URL. If not informed, it will use the
	 * project.issueManagement.url info.
	 * 
	 * @parameter expression="${jiraURL}"
	 *            default-value="${project.issueManagement.url}"
	 * @required
	 */
	protected String jiraURL;

	/**
	 * JIRA Authentication User.
	 * 
	 * @parameter expression="${jiraUser}" default-value="${scmUsername}"
	 */
	protected String jiraUser;

	/**
	 * JIRA Authentication Password.
	 * 
	 * @parameter expression="${jiraPassword}" default-value="${scmPassword}"
	 */
	protected String jiraPassword;

	/**
	 * JIRA Project Key.
	 * 
	 * @parameter expression="${jiraProjectKey}"
	 */
	protected String jiraProjectKey;

	/**
	 * Returns if this plugin is enabled for this context
	 * 
	 * @parameter expression="${skip}"
	 */
	protected boolean skip;
	
	/**
	 * "SOAP" or "REST"
	 * @parameter default-value="REST" 
	 */
	protected String apiType;
	
	protected JiraApi jiraApi;
	

	

	/**
	 * Load username password from settings if user has not set them in JVM
	 * properties
	 */
	void loadUserInfoFromSettings() {
		if (settingsKey == null) {
			settingsKey = jiraURL;
		}
		if ((jiraUser == null || jiraPassword == null) && (settings != null)) {
			Server server = settings.getServer(this.settingsKey);

			if (server != null) {
				if (jiraUser == null) {
					jiraUser = server.getUsername();
				}

				if (jiraPassword == null) {
					jiraPassword = server.getPassword();
				}
			}
		}
	}
	
	protected JiraApi initialiseJiraApi() throws URISyntaxException {
//		if( "SOAP".equals(apiType) ) {
//			jiraApi = new JiraSoapApi( jiraURL );
//		} else {
			jiraApi = new JiraRestApi( jiraURL );
//		}
		return jiraApi;
	}

	@Override
	public final void execute() throws MojoExecutionException,
			MojoFailureException {
		Log log = getLog();
		if (isSkip()) {
			log.info("Skipping Plugin execution.");
			return;
		}
		try {
			jiraApi = initialiseJiraApi();
			loadUserInfoFromSettings();
			log.debug("Logging in JIRA");
			String loginToken = jiraApi.login(jiraUser, jiraPassword);
			log.debug("Logged in JIRA");
			try {
				doExecute(jiraApi, loginToken);
			} finally {
				log.debug("Logging out from JIRA");
				jiraApi.logout(loginToken);
				log.debug("Logged out from JIRA");
			}
		} catch (Exception e) {
			log.error("Error when executing mojo", e);
			// XXX: Por enquanto nao faz nada.
		}
	}

	public abstract void doExecute(JiraApi jiraApi, String loginToken) throws Exception;

	public boolean isSkip() {
		return skip;
	}

	public void setJiraProjectKey(String jiraProjectKey) {
		this.jiraProjectKey = jiraProjectKey;
	}

	public void setJiraPassword(String jiraPassword) {
		this.jiraPassword = jiraPassword;
	}

	public void setJiraURL(String jiraURL) {
		this.jiraURL = jiraURL;
	}

	public void setJiraUser(String jiraUser) {
		this.jiraUser = jiraUser;
	}

	public void setSettings(Settings settings) {
		this.settings = settings;
	}

	public void setSettingsKey(String settingsKey) {
		this.settingsKey = settingsKey;
	}

}