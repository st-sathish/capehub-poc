package com.capestartproject.ingest.api;

import com.capestartproject.common.job.api.JobProducer;

/**
 * @author CS39
 *
 */
public interface IngestService extends JobProducer {

	String UTC_DATE_FORMAT = "yyyyMMdd'T'HHmmss'Z'";

}
