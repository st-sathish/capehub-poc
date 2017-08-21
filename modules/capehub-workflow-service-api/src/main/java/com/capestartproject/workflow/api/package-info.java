/**
 * Workflow service.
 */
@XmlSchema(elementFormDefault = XmlNsForm.QUALIFIED, attributeFormDefault = XmlNsForm.UNQUALIFIED, namespace = "http://workflow.capestartproject.com", xmlns = {
		@XmlNs(prefix = "ep", namespaceURI = "http://employeepackage.capestartproject.com"),
		@XmlNs(prefix = "wf", namespaceURI = "http://workflow.capestartproject.com"),
		@XmlNs(prefix = "sec", namespaceURI = "http://com.capestartproject.security") })
package com.capestartproject.workflow.api;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;

