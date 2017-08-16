package com.capestartproject.workflow.impl.jmx;

/**
 * JMX Bean interface exposing workflow statistics.
 */
public interface WorkflowsStatisticsMXBean {

  /**
   * Gets the total number of workflows
   *
   * @return the number of workflows
   */
  int getTotal();

  /**
   * Gets the number of running workflows
   *
   * @return the number of running workflows
   */
  int getRunning();

  /**
   * Gets the number of workflows on hold
   *
   * @return the number of workflows on hold
   */
  int getOnHold();

  /**
   * Gets the number of finished workflows
   *
   * @return the number of finished workflows
   */
  int getFinished();

  /**
   * Gets the number of failed workflows
   *
   * @return the number of failed workflows
   */
  int getFailed();

  /**
   * Gets the number of instantiated workflows
   *
   * @return the number of instantiated workflows
   */
  int getInstantiated();

  /**
   * Gets the number of stopped workflows
   *
   * @return the number of stopped workflows
   */
  int getStopped();

  /**
   * Gets the number of failing workflows
   *
   * @return the number of failing workflows
   */
  int getFailing();

  /**
   * Gets a list of workflows on hold
   *
   * @return an array including a list of workflows on hold
   */
  String[] getWorkflowsOnHold();

  /**
   * Gets a list of average workflow processing times
   *
   * @return an array including a list of average workflow processing times
   */
  String[] getAverageWorkflowProcessingTime();

  /**
   * Gets a list of average workflow queue times
   *
   * @return an array including a list of average workflow queue times
   */
  String[] getAverageWorkflowQueueTime();

  /**
   * Gets a list of average workflow hold times
   *
   * @return an array including a list of average workflow hold times
   */
  String[] getAverageWorkflowHoldTime();

}
