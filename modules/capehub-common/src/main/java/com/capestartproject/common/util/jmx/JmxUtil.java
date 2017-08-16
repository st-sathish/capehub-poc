package com.capestartproject.common.util.jmx;

import java.lang.management.ManagementFactory;

import javax.management.Notification;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Functions supporting JMX.
 */
public final class JmxUtil {

	public static final String CAPEHUB_UPDATE_NOTIFICATION = "com.capestartproject.capehub.update";

	private static final String CAPEHUB_JMX_DOMAIN = "com.capestartproject.capehub";
  private static final Logger logger = LoggerFactory.getLogger(JmxUtil.class);

  private JmxUtil() {
  }

  public static <A> ObjectInstance registerMXBean(A bean, String type) {
    try {
      logger.info("Registering {} with JMX", bean.getClass().getName());
      return ManagementFactory.getPlatformMBeanServer().registerMBean(bean,
					new ObjectName(CAPEHUB_JMX_DOMAIN + ":type=" + type));
    } catch (Exception e) {
      logger.warn("Unable to register {} as an mbean: {}", bean, e);
    }
    return null;
  }

  public static void unregisterMXBean(ObjectInstance bean) {
    logger.info("Unregistering {} with JMX", bean.getClassName());
    try {
      ManagementFactory.getPlatformMBeanServer().unregisterMBean(bean.getObjectName());
    } catch (Exception e) {
      logger.warn("Unable to unregister mbean {}: {}", bean.getClassName(), e);
    }
  }

  public static Notification createUpdateNotification(Object source, long sequenceNumber, String message) {
		return new Notification(CAPEHUB_UPDATE_NOTIFICATION, source, sequenceNumber, System.currentTimeMillis(),
				message);
  }

}
