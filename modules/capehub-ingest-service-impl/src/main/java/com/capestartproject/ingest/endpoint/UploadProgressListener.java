package com.capestartproject.ingest.endpoint;

import javax.persistence.EntityManagerFactory;

import org.apache.commons.fileupload.ProgressListener;

/**
 *
 */
public class UploadProgressListener implements ProgressListener {

  // ProgressListeners can happen to be called with a high frequency, depending
  // on the ServeletEngine (see fileupload doc). So we save the job object only
  // after every X Kb that have arrived to avoid doing to many persist operations.
  private static final int SAVE_INTERVAL = 50 * 1024;

  private UploadJob job;
  @SuppressWarnings("unused")
  private EntityManagerFactory emf;
  private long lastSaved = 0L;

  public UploadProgressListener(UploadJob job, EntityManagerFactory emf) {
    this.job = job;
    this.emf = emf;
  }

  /**
   * Called by ServeletFileUpload on upload progress. Updates the job object. Persists the job object on upload
   * start/complete and after every X Kb that have arrived.
   *
   * @param rec
   * @param total
   * @param i
   */
  @Override
  public void update(long rec, long total, int i) {
    job.setBytesTotal(total);
    job.setBytesReceived(rec);
    if ((rec == 0L) || // persist job on upload start
            (rec - lastSaved >= SAVE_INTERVAL) || // after X Kb
            (rec == total)) { // on upload complete
      lastSaved = rec;
    }
  }
}
