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