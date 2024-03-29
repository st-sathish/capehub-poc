package com.capestartproject.workflow.impl;

import static com.capestartproject.common.util.ReadinessIndicator.ARTIFACT;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.felix.fileinstall.ArtifactInstaller;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.capestartproject.common.util.ReadinessIndicator;
import com.capestartproject.workflow.api.WorkflowDefinition;
import com.capestartproject.workflow.api.WorkflowParser;

/**
 * Loads, unloads, and reloads {@link WorkflowDefinition}s from "*workflow.xml" files in any of fileinstall's watch
 * directories.
 */
public class WorkflowDefinitionScanner implements ArtifactInstaller {
  private static final Logger logger = LoggerFactory.getLogger(WorkflowDefinitionScanner.class);

  /** An internal collection of workflows that we have installed */
  protected Map<String, WorkflowDefinition> installedWorkflows = new HashMap<String, WorkflowDefinition>();

  /** An internal collection of artifact id, bind the workflow definition files and their id */
  protected Map<File, String> artifactIds = new HashMap<File, String>();

  /** List of artifact parsed with error */
  protected List<File> artifactsWithError = new ArrayList<File>();

  /** OSGi bundle context */
  private BundleContext bundleCtx = null;

  /** Tag to define if the the workflows definition have already been loaded */
  private boolean isWFSinitiliazed = false;

  /** The current workflow definition being installed */
  private WorkflowDefinition currentWFD = null;

  /**
   * OSGi callback on component activation. private boolean initialized = true;
   *
   * /** OSGi callback on component activation.
   *
   * @param ctx
   *          the bundle context
   */
  void activate(BundleContext ctx) {
		logger.info("Workflow scanner component activated");
    this.bundleCtx = ctx;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.felix.fileinstall.ArtifactInstaller#install(java.io.File)
   */
	@Override
  public void install(File artifact) throws Exception {
    WorkflowDefinition def = currentWFD;

    // If the current workflow definition is null, it means this is a first install and not an update...
    if (def == null) {
      // ... so we have to load the definition first
      def = parseWorkflowDefinitionFile(artifact);
      if (def == null) {
        logger.warn("Unable to install workflow from {}", artifact.getAbsolutePath());
        artifactsWithError.add(artifact);
        return;
      }
    }

    logger.info("Installing workflow from file {}", artifact.getAbsolutePath());
    artifactsWithError.remove(artifact);
    artifactIds.put(artifact, def.getId());
    putWorkflowDefinition(def.getId(), def);

    // Determine the number of available profiles
    String[] filesInDirectory = artifact.getParentFile().list(new FilenameFilter() {
      public boolean accept(File arg0, String name) {
        return name.endsWith(".xml");
      }
    });

    logger.info("Worfkflow definition '{}' from file {} installed", def.getId(), artifact.getAbsolutePath());

    // Once all profiles have been loaded, announce readiness
    if ((filesInDirectory.length - artifactsWithError.size()) == artifactIds.size() && !isWFSinitiliazed) {
      logger.info("{} Workflow definitions loaded, activating Workflow service", filesInDirectory.length - artifactsWithError.size());
      Dictionary<String, String> properties = new Hashtable<String, String>();
      properties.put(ARTIFACT, "workflowdefinition");
      logger.debug("Indicating readiness of workflow definitions");
      bundleCtx.registerService(ReadinessIndicator.class.getName(), new ReadinessIndicator(), properties);
      isWFSinitiliazed = true;
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.felix.fileinstall.ArtifactInstaller#uninstall(java.io.File)
   */
	@Override
  public void uninstall(File artifact) throws Exception {
    // Since the artifact is gone, we can't open it to read its ID. So we look in the local map.
    String id = artifactIds.remove(artifact);
    if (id != null) {
      WorkflowDefinition def = removeWorkflowDefinition(id);
      logger.info("Uninstalling workflow definition '{}' from file {}", def.getId(), artifact.getAbsolutePath());
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.felix.fileinstall.ArtifactInstaller#update(java.io.File)
   */
	@Override
  public void update(File artifact) throws Exception {
    currentWFD = parseWorkflowDefinitionFile(artifact);

    if (currentWFD != null) {
      uninstall(artifact);
      install(artifact);
      currentWFD = null;
    }
  }

  /**
   * Parse the given workflow definition file and return the related workflow definition
   *
   * @param artifact
   *          The workflow definition file to parse
   * @return the workflow definition if the given contained a valid one, or null if the file can not be parsed.
   */
  public WorkflowDefinition parseWorkflowDefinitionFile(File artifact) {
    InputStream stream = null;
    try {
      stream = new FileInputStream(artifact);
      WorkflowDefinition def = WorkflowParser.parseWorkflowDefinition(stream);
      if (def.getOperations().size() == 0)
        logger.warn("Workflow '{}' has no operations", def.getId());
      return def;
    } catch (Exception e) {
      logger.warn("Unable to parse workflow from file {}, {}", artifact.getAbsolutePath(), e.getMessage());
      return null;
    } finally {
      IOUtils.closeQuietly(stream);
    }
  }

  /**
   * Gets the workflow definitions with the given id.
   *
   * @param id
   * @return the workflow definition if exist or null
   */
  public WorkflowDefinition getWorkflowDefinition(String id) {
    return installedWorkflows.get(id);
  }

  /**
   * Get the list of installed workflow definitions.
   *
   * @return the collection of installed workflow definitions id
   */
  public Map<String, WorkflowDefinition> getWorkflowDefinitions() {
    return installedWorkflows;
  }

  /**
   * Add the given workflow definition to the installed workflow definition id.
   *
   * @param id
   *          the id of the workflow definition to add
   * @param wfd
   *          the workflow definition id
   */
  public void putWorkflowDefinition(String id, WorkflowDefinition wfd) {
    installedWorkflows.put(id, wfd);
  }

  /**
   * Remove the workflow definition with the given id from the installed definition list.
   *
   * @param id
   *          the workflow definition id
   * @return the removed workflow definition
   */
  public WorkflowDefinition removeWorkflowDefinition(String id) {
    return installedWorkflows.remove(id);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.felix.fileinstall.ArtifactListener#canHandle(java.io.File)
   */
	@Override
  public boolean canHandle(File artifact) {
    return "workflows".equals(artifact.getParentFile().getName()) && artifact.getName().endsWith(".xml");
  }
}
