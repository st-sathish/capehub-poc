package com.capestartproject.workflow.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Product type of a workflow definition and its parameters. */
public class ConfiguredWorkflow {
  private final WorkflowDefinition workflowDefinition;
  private final Map<String, String> parameters;

  private static final Map<String, String> noparams = Collections.unmodifiableMap(new HashMap<String, String>());

  /** Constructor. */
  public ConfiguredWorkflow(WorkflowDefinition workflowDefinition, Map<String, String> parameters) {
    this.workflowDefinition = workflowDefinition;
    this.parameters = parameters;
  }

  /** Create a workflow with parameters. */
  public static ConfiguredWorkflow workflow(WorkflowDefinition workflowDefinition, Map<String, String> parameters) {
    return new ConfiguredWorkflow(workflowDefinition, parameters);
  }

  /** Create a parameterless workflow. */
  public static ConfiguredWorkflow workflow(WorkflowDefinition workflowDefinition) {
    return new ConfiguredWorkflow(workflowDefinition, noparams);
  }

  /** Get the workflow definition. */
  public WorkflowDefinition getWorkflowDefinition() {
    return workflowDefinition;
  }

  /** Get the workflow's parameter map. */
  public Map<String, String> getParameters() {
    return parameters;
  }
}
