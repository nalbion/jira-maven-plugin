package com.george.plugins.jira;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.rpc.ServiceException;

import org.apache.maven.plugin.logging.Log;

import com.george.plugins.jira.api.JiraApi;
import com.george.plugins.jira.api.JiraSoapApi;
import com.george.plugins.jira.api.JiraVersion;

/**
 * Goal that creates a version in a JIRA project . NOTE: SOAP access must be
 * enabled in your JIRA installation. Check JIRA docs for more info.
 * 
 * @goal release-jira-version
 * @phase deploy
 * 
 * @author George Gastaldi
 */
public class ReleaseVersionMojo extends AbstractJiraMojo {

	/**
	 * Released Version
	 * 
	 * @parameter expression="${releaseVersion}"
	 *            default-value="${project.version}"
	 */
	String releaseVersion;

	/**
	 * Auto Discover latest release and release it.
	 * 
	 * @parameter expression="${autoDiscoverLatestRelease}" default-value="true"
	 */
	boolean autoDiscoverLatestRelease;

	/**
	 * Comparator for discovering the latest release
	 * 
	 * @parameter 
	 *            implementation="com.george.plugins.jira.StringComparator"
	 */
	Comparator<JiraVersion> remoteVersionComparator = new RemoteVersionComparator();
	
	
	protected JiraApi initialiseJiraApi() throws URISyntaxException, MalformedURLException, ServiceException {
		// releaseVersion() is not yet implemented in JiraRestApi
		jiraApi = new JiraSoapApi( jiraURL );
		return jiraApi;
	}

	@Override
	public void doExecute(JiraApi jiraApi, String loginToken)
			throws Exception {
		Log log = getLog();
		log.debug("Login Token returned: " + loginToken);
		List<JiraVersion> versions = jiraApi.getVersions(loginToken,
				jiraProjectKey);
		String thisReleaseVersion = (autoDiscoverLatestRelease)
				? calculateLatestReleaseVersion(versions)
				: releaseVersion;
		if (thisReleaseVersion != null) {
			log.info("Releasing Version " + this.releaseVersion);
			markVersionAsReleased(jiraApi, loginToken, versions,
					thisReleaseVersion);
		}
	}

	/**
	 * Returns the latest unreleased version
	 * 
	 * @param versions
	 * @return
	 */
	String calculateLatestReleaseVersion(List<JiraVersion> versions) {
		Collections.sort(versions, remoteVersionComparator);

		for (JiraVersion remoteVersion : versions) {
			if (!remoteVersion.isReleased())
				return remoteVersion.getName();
		}
		return null;
	}

	/**
	 * Check if version is already present on array
	 * 
	 * @param versions
	 * @param newDevVersion
	 * @return
	 */
	boolean isVersionAlreadyPresent(List<JiraVersion> versions,
			String newDevVersion) {
		boolean versionExists = false;
		if (versions != null) {
			// Creating new Version (if not already created)
			for (JiraVersion remoteVersion : versions) {
				if (remoteVersion.getName().equalsIgnoreCase(newDevVersion)) {
					versionExists = true;
					break;
				}
			}
		}
		// existant
		return versionExists;
	}

	/**
	 * Release Version
	 * 
	 * @param log
	 * @param jiraApi
	 * @param loginToken
	 * @throws RemoteException
	 * @throws RemotePermissionException
	 * @throws RemoteAuthenticationException
	 * @throws com.atlassian.jira.rpc.soap.client.RemoteException
	 */
	JiraVersion markVersionAsReleased(JiraApi jiraApi,
			String loginToken, List<JiraVersion> versions, String releaseVersion)
			throws RemoteException {
		JiraVersion ret = null;
		if (versions != null) {
			for (JiraVersion remoteReleasedVersion : versions) {
				if (releaseVersion.equalsIgnoreCase(remoteReleasedVersion
						.getName()) && !remoteReleasedVersion.isReleased()) {
					// Mark as released
					remoteReleasedVersion.setReleased(true);
					remoteReleasedVersion
							.setReleaseDate(Calendar.getInstance());
					jiraApi.releaseVersion(loginToken, jiraProjectKey,
							remoteReleasedVersion);
					getLog().info(
							"Version " + remoteReleasedVersion.getName()
									+ " was released in JIRA.");
					ret = remoteReleasedVersion;
					break;
				}
			}
		}
		return ret;
	}
}
