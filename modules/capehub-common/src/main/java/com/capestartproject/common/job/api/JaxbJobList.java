package com.capestartproject.common.job.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * A wrapper for job collections.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "jobs", namespace = "http://job.capestartproject.com")
@XmlRootElement(name = "jobs", namespace = "http://job.capestartproject.com")
public class JaxbJobList {
  /** A list of jobs */
  @XmlElement(name = "job")
  protected List<JaxbJob> jobs = new ArrayList<JaxbJob>();

  public JaxbJobList() {
  }

  public JaxbJobList(JaxbJob job) {
    this.jobs.add(job);
  }

  public JaxbJobList(Collection<Job> jobs) {
    if (jobs != null) {
      for (Job job : jobs) {
        this.jobs.add((JaxbJob) job);
      }
    }
  }

  /**
   * @return the jobs
   */
  public List<JaxbJob> getJobs() {
    return jobs;
  }

  /**
   * @param jobs
   *          the jobs to set
   */
  public void setJobs(List<JaxbJob> jobs) {
    this.jobs = jobs;
  }

  public void add(Job job) {
    if (job instanceof JaxbJob) {
      jobs.add((JaxbJob) job);
    } else {
      throw new IllegalArgumentException("Jobs must be an instance of JaxbJob");
    }
  }
}
