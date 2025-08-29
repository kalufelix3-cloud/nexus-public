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

import java.net.SocketAddress;
import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.TimeZone;

import org.sonatype.goodies.testsupport.Test5Support;

import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.server.ConnectionMetaData;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog.Writer;
import org.eclipse.jetty.server.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NexusRepositoryRequestLogTest
    extends Test5Support
{
  private static final String EXPECTED_FORMAT =
      "%{client}a - %{nexus.user.id}attr [%{responseTimestamp}attr] \"%r\" %s %{Content-Length}i %O %T %{User-Agent}i [%{threadName}attr]";

  public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(
      "dd/MMM/yyyy:HH:mm:ss Z")
      .withZone(TimeZone.getDefault().toZoneId());

  @Mock
  private Request request;

  @Mock
  private Response response;

  @Mock
  private Writer writer;

  @BeforeEach
  public void setUp() {
    when(response.getStatus()).thenReturn(200);
    ConnectionMetaData connectionMetaData = mock(ConnectionMetaData.class);
    SocketAddress socketAddress = mock(SocketAddress.class);
    when(socketAddress.toString()).thenReturn("127.0.0.1");
    when(connectionMetaData.getRemoteSocketAddress()).thenReturn(socketAddress);
    HttpURI uri = HttpURI.build("/path/to/foo");
    when(request.getHttpURI()).thenReturn(uri);
    when(request.getHeaders()).thenReturn(HttpFields.EMPTY);
    when(request.getConnectionMetaData()).thenReturn(connectionMetaData);
  }

  @Test
  public void testLogIncludesThreadName() {
    NexusRepositoryRequestLog requestLog = new NexusRepositoryRequestLog(writer, EXPECTED_FORMAT);

    requestLog.log(request, response);

    verify(request).setAttribute(eq("threadName"), eq(Thread.currentThread().getName()));
  }

  @Test
  public void testLogWithResponseTime() {
    Clock clock = Clock.fixed(Instant.now(), TimeZone.getDefault().toZoneId());
    NexusRepositoryRequestLog requestLog = new NexusRepositoryRequestLog(
        writer, EXPECTED_FORMAT, clock::millis);

    requestLog.log(request, response);

    verify(request).setAttribute(eq("responseTimestamp"),
        argThat(value -> {
          var truncated = truncateInstant(value, ChronoUnit.SECONDS);
          var now = Instant.now(clock).truncatedTo(ChronoUnit.SECONDS);

          return truncated.equals(now);
        }));
  }

  private Instant truncateInstant(final Object value, TemporalUnit unit) {
    return ZonedDateTime.parse(value.toString(), DATE_FORMATTER)
        .truncatedTo(unit)
        .toInstant();
  }
}
