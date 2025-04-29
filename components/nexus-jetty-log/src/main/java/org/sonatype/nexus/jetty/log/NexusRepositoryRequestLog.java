/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.jetty.log;

import java.util.TimeZone;

import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Response;

public class NexusRepositoryRequestLog
    extends CustomRequestLog
{
  public NexusRepositoryRequestLog(RequestLog.Writer writer, String formatString) {
    super(writer, formatString);
  }

  @Override
  public void log(Request request, Response response) {
    request.setAttribute("threadName", Thread.currentThread().getName());
    super.log(request, response);
  }

  /**
   * Returns the default time zone ID. This method is used in
   * `jetty-requestlog.xml` to dynamically set the time zone for request
   * logs to the system's default time zone.
   *
   * @return The ID of the default time zone.
   */
  public static String getDefaultTimeZoneId() {
    return TimeZone.getDefault().getID();
  }
}
