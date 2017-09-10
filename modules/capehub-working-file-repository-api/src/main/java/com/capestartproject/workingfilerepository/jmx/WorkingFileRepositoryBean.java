package com.capestartproject.workingfilerepository.jmx;

import com.capestartproject.common.util.data.Option.Match;
import com.capestartproject.workingfilerepository.api.WorkingFileRepository;

public class WorkingFileRepositoryBean implements WorkingFileRepositoryMXBean {

  private final WorkingFileRepository workingFileRepository;

  public WorkingFileRepositoryBean(WorkingFileRepository workingFileRepository) {
    this.workingFileRepository = workingFileRepository;
  }

  	/**
	 * @see com.capestartproject.workingfilerepository.jmx.WorkingFileRepositoryMXBean#getFreeSpace()
	 */
  @Override
  public long getFreeSpace() {
    return workingFileRepository.getUsableSpace().fold(new Match<Long, Long>() {
      @Override
      public Long some(Long a) {
        return a;
      }

      @Override
      public Long none() {
        return -1L;
      }
    });
  }

  	/**
	 * @see com.capestartproject.workingfilerepository.jmx.WorkingFileRepositoryMXBean#getUsedSpace()
	 */
  @Override
  public long getUsedSpace() {
    return workingFileRepository.getUsedSpace().fold(new Match<Long, Long>() {
      @Override
      public Long some(Long a) {
        return a;
      }

      @Override
      public Long none() {
        return -1L;
      }
    });
  }

  	/**
	 * @see com.capestartproject.workingfilerepository.jmx.WorkingFileRepositoryMXBean#getTotalSpace()
	 */
  @Override
  public long getTotalSpace() {
    return workingFileRepository.getTotalSpace().fold(new Match<Long, Long>() {
      @Override
      public Long some(Long a) {
        return a;
      }

      @Override
      public Long none() {
        return -1L;
      }
    });
  }

}
