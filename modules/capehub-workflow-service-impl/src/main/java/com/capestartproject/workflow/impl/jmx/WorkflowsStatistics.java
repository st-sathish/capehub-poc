package com.capestartproject.workflow.impl.jmx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;

import com.capestartproject.common.util.jmx.JmxUtil;
import com.capestartproject.workflow.api.WorkflowInstance;
import com.capestartproject.workflow.api.WorkflowStatistics;

public class WorkflowsStatistics extends NotificationBroadcasterSupport implements WorkflowsStatisticsMXBean {

  private static final String DELIMITER = ";";
  private long sequenceNumber = 1;

  private Map<String, Long> workflowCounts = new HashMap<String, Long>();
  private WorkflowStatistics workflowStatistics;

  public WorkflowsStatistics(WorkflowStatistics workflowStatistics, List<WorkflowInstance> workflows) {
    updateWorkflow(workflowStatistics, workflows);
  }

  public void updateWorkflow(WorkflowStatistics workflowStatistics, List<WorkflowInstance> workflows) {
    this.workflowStatistics = workflowStatistics;
    for (WorkflowInstance wf : workflows) {
      Long count = workflowCounts.get(wf.getTemplate());
      if (count == null) {
        workflowCounts.put(wf.getTemplate(), 1L);
      } else {
        workflowCounts.put(wf.getTemplate(), count++);
      }
    }
    sendNotification(JmxUtil.createUpdateNotification(this, sequenceNumber++, "Workflow updated"));
  }

  @Override
  public MBeanNotificationInfo[] getNotificationInfo() {
		String[] types = new String[] { JmxUtil.CAPEHUB_UPDATE_NOTIFICATION };

    String name = Notification.class.getName();
    String description = "An update was executed";
    MBeanNotificationInfo info = new MBeanNotificationInfo(types, name, description);
    return new MBeanNotificationInfo[] { info };
  }

  	/**
	 * @see com.capestartproject.workflow.impl.jmx.WorkflowsStatisticsMXBean#getTotal()
	 */
  @Override
  public int getTotal() {
    return (int) workflowStatistics.getTotal();
  }

  	/**
	 * @see com.capestartproject.workflow.impl.jmx.WorkflowsStatisticsMXBean#getInstantiated()
	 */
  @Override
  public int getInstantiated() {
    return (int) workflowStatistics.getInstantiated();
  }

  	/**
	 * @see com.capestartproject.workflow.impl.jmx.WorkflowsStatisticsMXBean#getRunning()
	 */
  @Override
  public int getRunning() {
    return (int) workflowStatistics.getRunning();
  }

  	/**
	 * @see com.capestartproject.workflow.impl.jmx.WorkflowsStatisticsMXBean#getOnHold()
	 */
  @Override
  public int getOnHold() {
    return (int) workflowStatistics.getPaused();
  }

  	/**
	 * @see com.capestartproject.workflow.impl.jmx.WorkflowsStatisticsMXBean#getStopped()
	 */
  @Override
  public int getStopped() {
    return (int) workflowStatistics.getStopped();
  }

  	/**
	 * @see com.capestartproject.workflow.impl.jmx.WorkflowsStatisticsMXBean#getFinished()
	 */
  @Override
  public int getFinished() {
    return (int) workflowStatistics.getFinished();
  }

  	/**
	 * @see com.capestartproject.workflow.impl.jmx.WorkflowsStatisticsMXBean#getFailing()
	 */
  @Override
  public int getFailing() {
    return (int) workflowStatistics.getFailing();
  }

  	/**
	 * @see com.capestartproject.workflow.impl.jmx.WorkflowsStatisticsMXBean#getFailed()
	 */
  @Override
  public int getFailed() {
    return (int) workflowStatistics.getFailed();
  }

  	/**
	 * @see com.capestartproject.workflow.impl.jmx.WorkflowsStatisticsMXBean#getWorkflowsOnHold()
	 */
  @Override
  public String[] getWorkflowsOnHold() {
    List<String> operationList = new ArrayList<String>();
    for (Entry<String, Long> entry : workflowCounts.entrySet()) {
      operationList.add(entry.getKey() + DELIMITER + entry.getValue());
    }
    return operationList.toArray(new String[operationList.size()]);
  }

  	/**
	 * @see com.capestartproject.workflow.impl.jmx.WorkflowsStatisticsMXBean#getAverageWorkflowProcessingTime()
	 */
  @Override
  public String[] getAverageWorkflowProcessingTime() {
    // Not implemented yet
    return new String[0];
  }

  	/**
	 * @see com.capestartproject.workflow.impl.jmx.WorkflowsStatisticsMXBean#getAverageWorkflowQueueTime()
	 */
  @Override
  public String[] getAverageWorkflowQueueTime() {
    // Not implemented yet
    return new String[0];
  }

  	/**
	 * @see com.capestartproject.workflow.impl.jmx.WorkflowsStatisticsMXBean#getAverageWorkflowHoldTime()
	 */
  @Override
  public String[] getAverageWorkflowHoldTime() {
    // Not implemented yet
    return new String[0];
  }

}
