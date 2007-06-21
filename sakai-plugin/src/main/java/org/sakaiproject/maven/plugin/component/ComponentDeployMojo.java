package org.sakaiproject.maven.plugin.component;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;

/**
 * Generate the exploded webapp
 * 
 * @goal deploy
 * @requiresDependencyResolution runtime
 */
public class ComponentDeployMojo extends AbstractComponentMojo {
	
	/**
	 * The directory where the webapp is built.
	 * 
	 * @parameter expression="${maven.tomcat.home}/components/${project.build.finalName}"
	 * @required
	 */
	private File deployDirectory;
	
	public File getDeployDirectory() {
		return deployDirectory;
	}

	public void setDeployDirectory(File deployDirectory) {
		this.deployDirectory = deployDirectory;
	}
	
	public void execute() throws MojoExecutionException, MojoFailureException {
		deployToContainer();
	}

	public void deployToContainer() throws MojoExecutionException,
			MojoFailureException

	{
		try {
			Set artifacts = project.getDependencyArtifacts();
			// iterate through the this to extract dependencies and deploy

			String packaging = project.getPackaging();
			File deployDir = getDeployDirectory();
			if (deployDir == null) {
				throw new MojoFailureException(
						"deployDirectory has not been set");
			}
			if ("sakai-component".equals(packaging)) {
				// UseCase: Sakai component in a pom
				// deploy to component and unpack as a
				getLog().info(
						"Deploying " + project.getGroupId() + ":"
								+ project.getArtifactId() + ":"
								+ project.getPackaging()
								+ " as an unpacked component");
				File destination = new File(deployDir, "components/");
				String fileName = project.getArtifactId();
				File destinationDir = new File(destination, fileName);
				Artifact artifact = project.getArtifact();
				if (artifact == null) {
					getLog().error(
							"No Artifact found in project " + getProjectId());
					throw new MojoFailureException(
							"No Artifact found in project");
				}
				File artifactFile = artifact.getFile();
				if (artifactFile == null) {
					artifactResolver.resolve(artifact, remoteRepositories,
							artifactRepository);
					artifactFile = artifact.getFile();
				}
				if (artifactFile == null) {
					getLog().error(
							"Artifact File is null for " + getProjectId());
					throw new MojoFailureException("Artifact File is null ");
				}
				getLog().info(
						"Unpacking " + artifactFile + " to " + destinationDir);
				deleteAll(destinationDir);
				destinationDir.mkdirs();
				unpack(artifactFile, destinationDir, "war");
			} else if ("war".equals(packaging)) {
				// UseCase: war webapp
				// deploy to webapps but dont unpack
				getLog().info(
						"Deploying " + project.getGroupId() + ":"
								+ project.getArtifactId() + ":"
								+ project.getPackaging() + " as a webapp");
				deployProjectArtifact(new File(deployDir, "webapps/"), false,
						true);

			} else if ("jar".equals(packaging)) {
				// UseCase: jar, marked with a property
				// deploy the target
				Properties p = project.getProperties();
				String deployTarget = p.getProperty("deploy.target");
				if ("shared".equals(deployTarget)) {
					deployProjectArtifact(new File(deployDir, "shared/lib/"),
							true, false);
				} else if ("common".equals(deployTarget)) {
					deployProjectArtifact(new File(deployDir, "common/lib/"),
							true, false);
				} else if ("server".equals(deployTarget)) {
					deployProjectArtifact(new File(deployDir, "server/lib/"),
							true, false);
				} else {
					getLog().info(
							"No deployment specification -- skipping "
									+ getProjectId());
				}
			} else if ("pom".equals(packaging)) {
				// UseCase: pom, marked with a property
				// deploy the contents
				getLog().info("Packaging is POM");
				Properties p = project.getProperties();
				String deployTarget = p.getProperty("deploy.target");
				getLog().info("Deploy Taarget is " + deployTarget);
				if ("shared".equals(deployTarget)) {
					File destinationDir = new File(deployDir, "shared/lib/");
					destinationDir.mkdirs();
					deployArtifacts(artifacts, destinationDir);
				} else if ("common".equals(deployTarget)) {
					File destinationDir = new File(deployDir, "common/lib/");
					destinationDir.mkdirs();
					deployArtifacts(artifacts, destinationDir);
				} else if ("server".equals(deployTarget)) {
					File destinationDir = new File(deployDir, "server/lib/");
					destinationDir.mkdirs();
					deployArtifacts(artifacts, destinationDir);
				} else {
					getLog().info(
							"No deployment specification -- skipping "
									+ getProjectId());
				}
			} else {
				getLog().info(
						"No deployment specification -- skipping "
								+ getProjectId());
			}
		} catch (IOException ex) {
			getLog().debug("Failed to deploy to container ", ex);
			throw new MojoFailureException("Fialed to deploy to container :"
					+ ex.getMessage());
		} catch (NoSuchArchiverException ex) {
			getLog().debug("Failed to deploy to container ", ex);
			throw new MojoFailureException("Fialed to deploy to container :"
					+ ex.getMessage());
		} catch (AbstractArtifactResolutionException ex) {
			getLog().debug("Failed to deploy to container ", ex);
			throw new MojoFailureException("Fialed to deploy to container :"
					+ ex.getMessage());
		}

	}

	protected void deployArtifacts(Set artifacts, File destination)
			throws IOException, MojoFailureException,
			AbstractArtifactResolutionException {
		for (Iterator iter = artifacts.iterator(); iter.hasNext();) {
			Artifact artifact = (Artifact) iter.next();
			if (artifact == null) {
				getLog().error(
						"Null Artifact found, sould never happen, in artifacts for project "
								+ getProjectId());
				throw new MojoFailureException(
						"Null Artifact found, sould never happen, in artifacts for project ");
			}
			File artifactFile = artifact.getFile();
			if (artifactFile == null) {
				artifactResolver.resolve(artifact, remoteRepositories,
						artifactRepository);
				artifactFile = artifact.getFile();
			}
			if (artifactFile == null) {
				getLog().error(
						"Artifact File is null for dependency "
								+ artifact.getId() + " in " + getProjectId());
				throw new MojoFailureException(
						"Artifact File is null for dependency "
								+ artifact.getId() + " in " + getProjectId());
			}
			String targetFileName = getDefaultFinalName(artifact);

			getLog().debug("Processing: " + targetFileName);
			File destinationFile = new File(destination, targetFileName);
			if ("provided".equals(artifact.getScope())
					|| "test".equals(artifact.getScope())) {
				getLog().info(
						"Skipping " + artifactFile + " Scope "
								+ artifact.getScope());

			} else {
				getLog()
						.info("Copy " + artifactFile + " to " + destinationFile);
				copyFileIfModified(artifact.getFile(), destinationFile);
			}
		}

	}

	private void deployProjectArtifact(File destination, boolean withVersion,
			boolean deleteStub) throws MojoFailureException, IOException,
			AbstractArtifactResolutionException {
		Artifact artifact = project.getArtifact();
		String fileName = null;
		String stubName = null;
		if (withVersion) {
			fileName = project.getArtifactId() + "-" + project.getVersion()
					+ "." + project.getPackaging();
			stubName = project.getArtifactId() + "-" + project.getVersion();
		} else {
			fileName = project.getArtifactId() + "." + project.getPackaging();
			stubName = project.getArtifactId();
		}
		File destinationFile = new File(destination, fileName);
		File stubFile = new File(destination, stubName);
		Set artifacts = project.getArtifacts();
		getLog().info("Found " + artifacts.size() + " artifacts");
		for (Iterator i = artifacts.iterator(); i.hasNext();) {
			Artifact a = (Artifact) i.next();
			getLog()
					.info(
							"Artifact Id " + a.getArtifactId() + " file "
									+ a.getFile());
		}
		if (artifact == null) {
			getLog().error("No Artifact found in project " + getProjectId());
			throw new MojoFailureException(
					"No Artifact found in project, target was "
							+ destinationFile);
		}
		File artifactFile = artifact.getFile();
		if (artifactFile == null) {
			artifactResolver.resolve(artifact, remoteRepositories,
					artifactRepository);
			artifactFile = artifact.getFile();
		}
		if (artifactFile == null) {
			getLog().error(
					"Artifact File is null for " + getProjectId()
							+ ", target was " + destinationFile);
			throw new MojoFailureException("Artifact File is null ");
		}
		getLog().info("Copy " + artifactFile + " to " + destinationFile);
		destinationFile.getParentFile().mkdirs();
		if (deleteStub && stubFile.exists()) {
			deleteAll(stubFile);
		}
		copyFileIfModified(artifactFile, destinationFile);
	}

}
