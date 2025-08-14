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
package org.sonatype.nexus.internal.web;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.common.app.BaseUrlManager;
import org.sonatype.nexus.common.app.WebFilterPriority;
import org.sonatype.nexus.security.UserIdMdcHelper;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import org.eclipse.sisu.Hidden;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.net.HttpHeaders.CONTENT_SECURITY_POLICY;
import static com.google.common.net.HttpHeaders.SERVER;
import static com.google.common.net.HttpHeaders.STRICT_TRANSPORT_SECURITY;
import static com.google.common.net.HttpHeaders.X_CONTENT_TYPE_OPTIONS;

/**
 * Sets up the basic environment for web-requests.
 *
 * @since 3.0
 */
@Order(WebFilterPriority.WEB)
@WebFilter("/*")
@Component
@Hidden // hide from DynamicFilterChainManager because we statically install it in WebModule
@Singleton
public class EnvironmentFilter
    extends ComponentSupport
    implements Filter
{
  static final String SANDBOX =
      "sandbox allow-forms allow-modals allow-popups allow-presentation allow-scripts allow-top-navigation";

  static final String STS_VALUE = "max-age=31536000; includeSubDomains;";

  private final String serverHeader;

  private final BaseUrlManager baseUrlManager;

  private final String contextPath;

  @Inject
  public EnvironmentFilter(
      final ApplicationVersion applicationVersion,
      final BaseUrlManager baseUrlManager,
      @Value("nexus-context-path") final String contextPath)
  {
    // cache "Server" header value
    checkNotNull(applicationVersion);

    this.serverHeader = String.format("Nexus/%s (%s)",
        applicationVersion.getVersion(),
        applicationVersion.getEdition());

    this.baseUrlManager = checkNotNull(baseUrlManager);
    this.contextPath = resolveContextPath(contextPath);
  }

  @Override
  public void destroy() {
    // ignore
  }

  @Override
  public void doFilter(
      final ServletRequest request,
      final ServletResponse response,
      final FilterChain chain) throws IOException, ServletException
  {
    // start with default unknown user-id in MDC
    UserIdMdcHelper.unknown();

    // detect base-url
    baseUrlManager.detectAndHoldUrl();

    // fill in default response headers
    defaultHeaders((HttpServletRequest) request, (HttpServletResponse) response);

    try {
      chain.doFilter(request, response);
    }
    finally {
      // unset user-id MDC
      UserIdMdcHelper.unset();
    }
  }

  /**
   * Add default headers to servlet response.
   */
  private void defaultHeaders(final HttpServletRequest request, final HttpServletResponse response) {
    response.setHeader(SERVER, serverHeader);

    // NEXUS-5023 disable IE for sniffing into response content
    response.setHeader(X_CONTENT_TYPE_OPTIONS, "nosniff");

    if (request.getRequestURI().startsWith(contextPath + "repository")) {
      // user-submitted content gets a different CSP to prevent stored XSS
      response.setHeader(CONTENT_SECURITY_POLICY, SANDBOX);
    }
    else {
      response.setHeader(CONTENT_SECURITY_POLICY,
          "default-src " + request.getScheme() + ": data: blob: 'unsafe-inline'; script-src " + request.getScheme()
              + ": 'unsafe-inline' 'unsafe-eval'");
    }

    if ("https".equals(request.getScheme())) {
      response.setHeader(STRICT_TRANSPORT_SECURITY, STS_VALUE);
    }
  }

  /**
   * Guarantee that the contextPath ends with a trailing slash.
   *
   * @param contextPath
   * @return never null, either "/" or the contextPath with a trailing slash
   */
  private String resolveContextPath(final String contextPath) {
    if (contextPath == null || contextPath.isEmpty()) {
      return "/";
    }
    else if (contextPath.endsWith("/")) {
      return contextPath;
    }
    else {
      return contextPath + "/";
    }
  }
}
