package com.capestartproject.workspace.impl;

import java.util.Date;

import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Trigger;
import org.quartz.TriggerUtils;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.capestartproject.common.util.data.Option;
import com.capestartproject.workspace.api.Workspace;

/** Clear outdated workspace files {@link Workspace}. */
public class WorkspaceCleaner {

  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(WorkspaceCleaner.class);

	private static final String JOB_NAME = "ch-workspace-cleaner-job";
	private static final String JOB_GROUP = "ch-workspace-cleaner-job-group";
	private static final String TRIGGER_NAME = "ch-workspace-cleaner-trigger";
	private static final String TRIGGER_GROUP = "ch-workspace-cleaner-trigger-group";
  private static final String JOB_PARAM_PARENT = "parent";

  private final org.quartz.Scheduler quartz;

  private final Workspace workspace;
  private final Option<Integer> maxAge;
  private int schedulerPeriod;

  protected WorkspaceCleaner(Workspace workspace, int schedulerPeriod, int maxAge) {
    this.workspace = workspace;
    this.schedulerPeriod = schedulerPeriod;
    if (maxAge > 0) {
      this.maxAge = Option.some(maxAge);
    } else {
      this.maxAge = Option.<Integer> none();
    }

    try {
      quartz = new StdSchedulerFactory().getScheduler();
      quartz.start();
      // create and set the job. To actually run it call schedule(..)
      final JobDetail job = new JobDetail(JOB_NAME, JOB_GROUP, Runner.class);
      job.setDurability(false);
      job.setVolatility(true);
      job.getJobDataMap().put(JOB_PARAM_PARENT, this);
      quartz.addJob(job, true);
    } catch (org.quartz.SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  public Workspace getWorkspace() {
    return workspace;
  }

  public Option<Integer> getMaxAge() {
    return maxAge;
  }

  /**
   * Set the schedule and start or restart the scheduler.
   */
  public void schedule() {
    logger.debug("Workspace cleaner is run every hour.");
    try {
      final Trigger trigger = TriggerUtils.makeSecondlyTrigger(schedulerPeriod);
      trigger.setStartTime(new Date());
      trigger.setName(TRIGGER_NAME);
      trigger.setGroup(TRIGGER_GROUP);
      trigger.setJobName(JOB_NAME);
      trigger.setJobGroup(JOB_GROUP);
      if (quartz.getTriggersOfJob(JOB_NAME, JOB_GROUP).length == 0) {
        quartz.scheduleJob(trigger);
      } else {
        quartz.rescheduleJob(TRIGGER_NAME, TRIGGER_GROUP, trigger);
      }
    } catch (Exception e) {
      logger.error("Error scheduling Quartz job", e);
    }
  }

  /** Shutdown the scheduler. */
  public void shutdown() {
    try {
      quartz.shutdown();
    } catch (org.quartz.SchedulerException ignore) {
    }
  }

  // just to make sure Quartz is being shut down...
  @Override
  protected void finalize() throws Throwable {
    super.finalize();
    shutdown();
  }

  // --

  /** Quartz work horse. */
  public static class Runner implements Job {

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
      logger.debug("Start workspace cleaner");
      try {
        execute((WorkspaceCleaner) jobExecutionContext.getJobDetail().getJobDataMap().get(JOB_PARAM_PARENT));
      } catch (Exception e) {
        throw new JobExecutionException("An error occurred while cleaning workspace", e);
      }
      logger.debug("Finished workspace cleaner");
    }

    private void execute(WorkspaceCleaner workspaceCleaner) {
      workspaceCleaner.getWorkspace().cleanup(workspaceCleaner.getMaxAge());
    }

  }

}
