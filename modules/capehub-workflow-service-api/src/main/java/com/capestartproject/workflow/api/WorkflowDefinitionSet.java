package com.capestartproject.workflow.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * A collection of workflow definitions.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "definitions", namespace = "http://workflow.capestartproject.org")
public class WorkflowDefinitionSet {

  @XmlElement(name = "definition")
  protected List<WorkflowDefinition> definitions = null;

  public WorkflowDefinitionSet(Collection<WorkflowDefinition> definitions) {
    this.definitions = new ArrayList<WorkflowDefinition>();
    if (definitions != null)
      this.definitions.addAll(definitions);
  }

  public WorkflowDefinitionSet() {
    this(null);
  }

  public List<WorkflowDefinition> getDefinitions() {
    return definitions;
  }

  public void setDefinitions(List<WorkflowDefinition> definitions) {
    this.definitions = definitions;
  }

}
