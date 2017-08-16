package com.capestartproject.workflow.api;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * A configuration value for workflow operations.
 */
@XmlJavaTypeAdapter(WorkflowConfigurationImpl.Adapter.class)
public interface WorkflowConfiguration {
  String getKey();

  String getValue();
}
