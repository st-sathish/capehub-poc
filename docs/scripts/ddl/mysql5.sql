CREATE TABLE SEQUENCE (
  SEQ_NAME VARCHAR(50) NOT NULL,
  SEQ_COUNT DECIMAL(38),
  PRIMARY KEY (SEQ_NAME)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO SEQUENCE(SEQ_NAME, SEQ_COUNT) values ('SEQ_GEN', 0);

CREATE TABLE ch_bundleinfo (
  id BIGINT(20) NOT NULL,
  bundle_name VARCHAR(128) NOT NULL,
  build_number VARCHAR(128) DEFAULT NULL,
  host VARCHAR(128) NOT NULL,
  bundle_id BIGINT(20) NOT NULL,
  bundle_version VARCHAR(128) NOT NULL,
  db_schema_version VARCHAR(128) DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT UNQ_ch_bundleinfo UNIQUE (host, bundle_name, bundle_version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE ch_organization (
  id VARCHAR(128) NOT NULL,
  anonymous_role VARCHAR(255),
  name VARCHAR(255),
  admin_role VARCHAR(255),
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE ch_organization_node (
  organization VARCHAR(128) NOT NULL,
  port int(11),
  name VARCHAR(255),
  PRIMARY KEY (organization, port, name),
  CONSTRAINT FK_ch_organization_node_organization FOREIGN KEY (organization) REFERENCES ch_organization (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX IX_ch_organization_node_pk ON ch_organization_node (organization);
CREATE INDEX IX_ch_organization_node_name ON ch_organization_node (name);
CREATE INDEX IX_ch_organization_node_port ON ch_organization_node (port);

CREATE TABLE ch_organization_property (
  organization VARCHAR(128) NOT NULL,
  name VARCHAR(255) NOT NULL,
  value TEXT(65535),
  PRIMARY KEY (organization, name),
  CONSTRAINT FK_ch_organization_property_organization FOREIGN KEY (organization) REFERENCES ch_organization (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX IX_ch_organization_property_pk ON ch_organization_property (organization);

CREATE TABLE ch_host_registration (
  id BIGINT NOT NULL,
  host VARCHAR(255) NOT NULL,
  maintenance TINYINT(1) DEFAULT 0 NOT NULL,
  online TINYINT(1) DEFAULT 1 NOT NULL,
  active TINYINT(1) DEFAULT 1 NOT NULL,
  max_jobs INTEGER NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT UNQ_ch_host_registration_0 UNIQUE (host)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX IX_ch_host_registration_online ON ch_host_registration (online);
CREATE INDEX IX_ch_host_registration_active ON ch_host_registration (active);

CREATE TABLE ch_service_registration (
  id BIGINT NOT NULL,
  path VARCHAR(255) NOT NULL,
  job_producer TINYINT(1) DEFAULT 0 NOT NULL,
  service_type VARCHAR(255) NOT NULL,
  online TINYINT(1) DEFAULT 1 NOT NULL,
  active TINYINT(1) DEFAULT 1 NOT NULL,
  online_from DATETIME,
  service_state int NOT NULL,
  state_changed DATETIME,
  warning_state_trigger BIGINT,
  error_state_trigger BIGINT,
  host_registration BIGINT,
  PRIMARY KEY (id),
  CONSTRAINT UNQ_ch_service_registration UNIQUE (host_registration, service_type),
  CONSTRAINT FK_ch_service_registration_host_registration FOREIGN KEY (host_registration) REFERENCES ch_host_registration (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX IX_ch_service_registration_service_type ON ch_service_registration (service_type);
CREATE INDEX IX_ch_service_registration_service_state ON ch_service_registration (service_state);
CREATE INDEX IX_ch_service_registration_active ON ch_service_registration (active);
CREATE INDEX IX_ch_service_registration_host_registration ON ch_service_registration (host_registration);

CREATE TABLE ch_role (
  id bigint(20) NOT NULL,
  description varchar(255) DEFAULT NULL,
  name varchar(128) DEFAULT NULL,
  organization varchar(128) DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT UNQ_ch_role UNIQUE (name, organization),
  CONSTRAINT FK_ch_role_organization FOREIGN KEY (organization) REFERENCES ch_organization (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE ch_group (
  id bigint(20) NOT NULL,
  group_id varchar(128) DEFAULT NULL,
  description varchar(255) DEFAULT NULL,
  name varchar(128) DEFAULT NULL,
  role varchar(255) DEFAULT NULL,
  organization varchar(128) DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT UNQ_ch_group UNIQUE (group_id, organization),
  CONSTRAINT FK_ch_group_organization FOREIGN KEY (organization) REFERENCES ch_organization (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE ch_group_member (
  JpaGroup_id bigint(20) NOT NULL,
  MEMBERS varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE ch_group_role (
  group_id bigint(20) NOT NULL,
  role_id bigint(20) NOT NULL,
  PRIMARY KEY (group_id,role_id),
  CONSTRAINT UNQ_mh_group_role UNIQUE (group_id, role_id),
  CONSTRAINT FK_ch_group_role_group_id FOREIGN KEY (group_id) REFERENCES ch_group (id),
  CONSTRAINT FK_ch_group_role_role_id FOREIGN KEY (role_id) REFERENCES ch_role (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE ch_user (
  id bigint(20) NOT NULL,
  username varchar(128) DEFAULT NULL,
  password text,
  name varchar(256) DEFAULT NULL,
  email varchar(256) DEFAULT NULL,
  organization varchar(128) DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT UNQ_ch_user UNIQUE (username, organization),
  CONSTRAINT FK_ch_user_organization FOREIGN KEY (organization) REFERENCES ch_organization (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX IX_mh_role_pk ON ch_role (name, organization);

CREATE TABLE ch_user_role (
  user_id bigint(20) NOT NULL,
  role_id bigint(20) NOT NULL,
  PRIMARY KEY (user_id,role_id),
  CONSTRAINT UNQ_ch_user_role UNIQUE (user_id, role_id),
  CONSTRAINT FK_ch_user_role_role_id FOREIGN KEY (role_id) REFERENCES ch_role (id),
  CONSTRAINT FK_ch_user_role_user_id FOREIGN KEY (user_id) REFERENCES ch_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE ch_user_ref (
  id bigint(20) NOT NULL,
  username varchar(128) DEFAULT NULL,
  last_login datetime DEFAULT NULL,
  email varchar(255) DEFAULT NULL,
  name varchar(255) DEFAULT NULL,
  login_mechanism varchar(255) DEFAULT NULL,
  organization varchar(128) DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT UNQ_ch_user_ref UNIQUE (username, organization),
  CONSTRAINT FK_ch_user_ref_organization FOREIGN KEY (organization) REFERENCES ch_organization (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE ch_user_ref_role (
  user_id bigint(20) NOT NULL,
  role_id bigint(20) NOT NULL,
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT UNQ_ch_user_ref_role UNIQUE (user_id, role_id),
  CONSTRAINT FK_ch_user_ref_role_role_id FOREIGN KEY (role_id) REFERENCES ch_role (id),
  CONSTRAINT FK_ch_user_ref_role_user_id FOREIGN KEY (user_id) REFERENCES ch_user_ref (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE ch_user_settings (
  id bigint(20) NOT NULL,
  setting_key VARCHAR(255) NOT NULL,
  setting_value text NOT NULL,
  username varchar(128) NOT NULL,
  organization varchar(128) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT UNQ_ch_user_settings UNIQUE (username, organization)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE ch_email_configuration (
  id BIGINT(20) NOT NULL,
  organization VARCHAR(128) NOT NULL,
  port INT(5) DEFAULT NULL,
  transport VARCHAR(255) DEFAULT NULL,
  username VARCHAR(255) DEFAULT NULL,
  server VARCHAR(255) NOT NULL,
  ssl_enabled TINYINT(1) NOT NULL DEFAULT '0',
  password VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT UNQ_ch_email_configuration UNIQUE (organization),
  CONSTRAINT FK_ch_email_configuration_organization FOREIGN KEY (organization) REFERENCES ch_organization (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX IX_mh_email_configuration_organization ON ch_email_configuration (organization);

CREATE TABLE ch_job (
  id BIGINT NOT NULL,
  status INTEGER,
  payload MEDIUMTEXT,
  date_started DATETIME,
  run_time BIGINT,
  creator TEXT(65535) NOT NULL,
  instance_version BIGINT,
  date_completed DATETIME,
  operation VARCHAR(128),
  dispatchable TINYINT(1) DEFAULT 1,
  organization VARCHAR(128) NOT NULL,
  date_created DATETIME,
  queue_time BIGINT,
  creator_service BIGINT,
  processor_service BIGINT,
  parent BIGINT,
  root BIGINT,
  PRIMARY KEY (id),
  CONSTRAINT FK_ch_job_creator_service FOREIGN KEY (creator_service) REFERENCES ch_service_registration (id) ON DELETE CASCADE,
  CONSTRAINT FK_ch_job_processor_service FOREIGN KEY (processor_service) REFERENCES ch_service_registration (id) ON DELETE CASCADE,
  CONSTRAINT FK_ch_job_parent FOREIGN KEY (parent) REFERENCES ch_job (id) ON DELETE CASCADE,
  CONSTRAINT FK_ch_job_root FOREIGN KEY (root) REFERENCES ch_job (id) ON DELETE CASCADE,
  CONSTRAINT FK_ch_job_organization FOREIGN KEY (organization) REFERENCES ch_organization (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX IX_ch_job_parent ON ch_job (parent);
CREATE INDEX IX_ch_job_root ON ch_job (root);
CREATE INDEX IX_ch_job_creator_service ON ch_job (creator_service);
CREATE INDEX IX_ch_job_processor_service ON ch_job (processor_service);
CREATE INDEX IX_ch_job_status ON ch_job (status);
CREATE INDEX IX_ch_job_date_created ON ch_job (date_created);
CREATE INDEX IX_ch_job_date_completed ON ch_job (date_completed);
CREATE INDEX IX_ch_job_dispatchable ON ch_job (dispatchable);
CREATE INDEX IX_ch_job_operation ON ch_job (operation);
CREATE INDEX IX_ch_job_statistics ON ch_job (processor_service, status, queue_time, run_time);