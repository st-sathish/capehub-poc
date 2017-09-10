package com.capestartproject.workspace.impl.jmx;

import com.capestartproject.common.util.data.Option.Match;
import com.capestartproject.workspace.api.Workspace;

public class WorkspaceBean implements WorkspaceMXBean {

  private final Workspace workspace;

  public WorkspaceBean(Workspace workspace) {
    this.workspace = workspace;
  }

  	/**
	 * @see com.capestartproject.workspace.impl.jmx.WorkspaceMXBean#getFreeSpace()
	 */
  @Override
  public long getFreeSpace() {
    return workspace.getUsableSpace().fold(new Match<Long, Long>() {
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
	 * @see com.capestartproject.workspace.impl.jmx.WorkspaceMXBean#getUsedSpace()
	 */
  @Override
  public long getUsedSpace() {
    return workspace.getUsedSpace().fold(new Match<Long, Long>() {
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
	 * @see com.capestartproject.workspace.impl.jmx.WorkspaceMXBean#getTotalSpace()
	 */
  @Override
  public long getTotalSpace() {
    return workspace.getTotalSpace().fold(new Match<Long, Long>() {
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
