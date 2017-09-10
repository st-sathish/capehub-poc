package com.capestartproject.serviceregistry.command;

import com.capestartproject.common.serviceregistry.api.ServiceRegistration;
import com.capestartproject.common.serviceregistry.api.ServiceRegistry;
import com.capestartproject.common.serviceregistry.api.ServiceRegistryException;
import com.capestartproject.common.util.NotFoundException;

/**
 * An interactive shell command for putting Maintainable services in and out of maintenance mode
 *
 */
public class MaintenanceCommand {
  protected ServiceRegistry serviceRegistry;

  public void setRemoteServiceManager(ServiceRegistry remoteServiceManager) {
    this.serviceRegistry = remoteServiceManager;
  }

  public String set(String baseUrl, boolean maintenanceMode) {
    try {
      serviceRegistry.setMaintenanceStatus(baseUrl, maintenanceMode);
      if (maintenanceMode) {
        return baseUrl + " is now in maintenance mode\n";
      } else {
        return baseUrl + " has returned to service\n";
      }
    } catch (ServiceRegistryException e) {
      return "Error setting maintenance mode: " + e.getMessage() + "\n";
    } catch (NotFoundException e) {
      return "Error setting maintenance mode, host " + baseUrl + " not found";
    }
  }

  public String list() {
    try {
      StringBuilder sb = new StringBuilder();
      for (ServiceRegistration reg : serviceRegistry.getServiceRegistrations()) {
        sb.append(reg.getServiceType());
        sb.append("@");
        sb.append(reg.getHost());
        if (reg.isInMaintenanceMode()) {
          sb.append(" (maintenance mode)");
        }
        sb.append("\n");
      }
      return sb.toString();
    } catch (ServiceRegistryException e) {
      return "Error: " + e.getMessage() + "\n";
    }
  }

}
