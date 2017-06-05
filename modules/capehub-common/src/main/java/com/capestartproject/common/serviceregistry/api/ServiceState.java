package com.capestartproject.common.serviceregistry.api;

public enum ServiceState {

  /** Service running normally */
  NORMAL,

  /** Service encountered a problem (job failure, ..) */
  WARNING,

  /** Service running no more correctly */
  ERROR

}
