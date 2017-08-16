package com.capestartproject.workflow.api;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * A single result of searching.
 */
@XmlJavaTypeAdapter(WorkflowSetImpl.Adapter.class)
public interface WorkflowSet {

  /**
   * The search item list
   *
   * @return Item list.
   */
  WorkflowInstance[] getItems();

  /**
   * Get the total number of items returned
   *
   * @return The number.
   */
  long size();

  /**
   * Get the start page.
   *
   * @return The start page.
   */
  long getStartPage();

  /**
   * Get the count limit.
   *
   * @return The count limit.
   */
  long getPageSize();

  /**
   * Get the search time.
   *
   * @return The time in ms.
   */
  long getSearchTime();

  /**
   * The total number of items without paging.
   *
   * @return The total number of items
   */
  long getTotalCount();

}
