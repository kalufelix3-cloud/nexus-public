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
package org.sonatype.nexus.bootstrap.application;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.bootstrap.jetty.JettyServer;
import org.sonatype.nexus.spring.application.NexusProperties;
import org.sonatype.nexus.spring.application.PropertyMap;
import org.sonatype.nexus.spring.application.ShutdownHelper;
import org.sonatype.nexus.spring.application.classpath.components.JettyConfigurationComponentList;
import org.sonatype.nexus.spring.application.classpath.components.MybatisDAOComponentList;
import org.sonatype.nexus.spring.application.classpath.components.SisuComponentMap;
import org.sonatype.nexus.spring.application.classpath.finder.FeatureFlagEnabledClassFinderFilter;
import org.sonatype.nexus.spring.application.classpath.walker.ClasspathWalker;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.sonatype.nexus.NexusDirectoryConfiguration.BASEDIR_SYS_PROP;
import static org.sonatype.nexus.NexusDirectoryConfiguration.DATADIR_SYS_PROP;

/**
 * Nexus bootstrap launcher.
 */
@Named
@Singleton
public class Launcher
{
  private static final String LOGGING_OVERRIDE_PREFIX = "nexus.logging.level.";

  public static final String IGNORE_SHUTDOWN_HELPER = ShutdownHelper.class.getName() + ".ignore";

  public static final String SYSTEM_USERID = "*SYSTEM";

  private final JettyServer server;

  private final JettyConfigurationComponentList jettyConfigurationComponentList;

  @Inject
  public Launcher(
      final NexusProperties nexusProperties,
      final ClasspathWalker classpathWalker,
      final FeatureFlagEnabledClassFinderFilter featureFlagEnabledClassFinderFilter,
      final SisuComponentMap sisuComponentMap,
      final MybatisDAOComponentList mybatisDAOComponentList,
      final JettyConfigurationComponentList jettyConfigurationComponentList) throws Exception
  {
    configureLogging();

    ClassLoader cl = getClass().getClassLoader();

    PropertyMap nexusPropertiesMap = nexusProperties.get();
    System.getProperties().put(DATADIR_SYS_PROP, nexusPropertiesMap.get(DATADIR_SYS_PROP));
    System.getProperties().put(BASEDIR_SYS_PROP, nexusPropertiesMap.get(BASEDIR_SYS_PROP));

    // log critical information about the runtime environment
    Logger log = LoggerFactory.getLogger(Launcher.class);
    if (log.isInfoEnabled()) {
      log.info("Java: {}, {}, {}, {}", System.getProperty("java.version"), System.getProperty("java.vm.name"),
          System.getProperty("java.vm.vendor"), System.getProperty("java.vm.version"));
      log.info("OS: {}, {}, {}", System.getProperty("os.name"), System.getProperty("os.version"),
          System.getProperty("os.arch"));
      log.info("User: {}, {}, {}", System.getProperty("user.name"), System.getProperty("user.language"),
          System.getProperty("user.home"));
      log.info("CWD: {}", System.getProperty("user.dir"));
    }

    // ensure the temporary directory is sane
    File tmpdir = TemporaryDirectory.get();
    log.info("TMP: {}", tmpdir);

    if (!"false".equalsIgnoreCase(getProperty(IGNORE_SHUTDOWN_HELPER, "false"))) {
      log.warn("ShutdownHelper requests will be ignored!");
      ShutdownHelper.setDelegate(ShutdownHelper.NOOP);
    }

    String args = nexusPropertiesMap.get("nexus-args");
    if (args == null || args.trim().isEmpty()) {
      throw new IllegalArgumentException("Missing nexus-args");
    }

    configureInitialLoggingOverrides(nexusPropertiesMap);

    classpathWalker.walk(getBase());

    Map<String, Object> objectsForContext = new HashMap<>();
    objectsForContext.put("nexusProperties", nexusProperties);
    objectsForContext.put("sisuComponentMap", sisuComponentMap);
    objectsForContext.put("mybatisDAOComponentList", mybatisDAOComponentList);
    objectsForContext.put("jettyConfigurationComponentList", jettyConfigurationComponentList);
    objectsForContext.put("featureFlagEnabledClassFinderFilter", featureFlagEnabledClassFinderFilter);

    this.server = new JettyServer(cl, nexusPropertiesMap, args.split(","), objectsForContext);
    this.jettyConfigurationComponentList = jettyConfigurationComponentList;
  }

  public JettyServer getServer() {
    return server;
  }

  private File getBase() throws URISyntaxException {
    CodeSource codeSource = org.springframework.boot.loader.launch.Launcher.class.getProtectionDomain().getCodeSource();
    if (codeSource == null) {
      throw new IllegalStateException("Unable to determine code source archive");
    }
    return Paths.get(codeSource.getLocation().toURI()).toFile();
  }

  /**
   * Starts Jetty without waiting for it to fully start up.
   *
   * @param callback optional, callback executed immediately after Jetty is fully started up.
   * @see JettyServer#start(boolean, Runnable)
   */
  public void startAsync(@Nullable final Runnable callback) throws Exception {
    start(false, callback);
  }

  private void start(final boolean waitForServer, @Nullable final Runnable callback) throws Exception {
    server.start(waitForServer, callback);
  }

  private String getProperty(final String name, final String defaultValue) {
    String value = System.getProperty(name, System.getenv(name));
    if (value == null) {
      value = defaultValue;
    }
    return value;
  }

  public void stop() throws Exception {
    server.stop();
  }

  /**
   * Customize logging of the application as necessary.
   */
  private void configureLogging() {
    org.slf4j.bridge.SLF4JBridgeHandler.removeHandlersForRootLogger();
    org.slf4j.bridge.SLF4JBridgeHandler.install();
  }

  private static class Property
  {
    private String key;

    private String value;

    private Property(final Entry<String, String> entry) {
      key = entry.getKey();
      value = entry.getValue();
    }

    private Property withKeyPrefixRemoved() {
      if (Launcher.LOGGING_OVERRIDE_PREFIX.length() < key.length()) {
        key = key.substring(Launcher.LOGGING_OVERRIDE_PREFIX.length());
      }
      return this;
    }
  }

  /**
   * Customize logging overrides presented as properties. These will be superseded by any logback overrides.
   */
  private void configureInitialLoggingOverrides(final Map<String, String> props) {
    LoggerContext loggerContext = loggerContext();
    if (props != null) {
      props.entrySet()
          .stream()
          .map(Property::new)
          .filter(p -> p.key.startsWith(LOGGING_OVERRIDE_PREFIX))
          .filter(p -> !p.value.isEmpty())
          .map(Property::withKeyPrefixRemoved)
          .forEach(p -> setLoggerLevel(loggerContext, p.key, p.value));
    }
  }

  private void setLoggerLevel(final LoggerContext loggerContext, final String logger, final String level) {
    loggerContext.getLogger(Launcher.class).debug("Initialising logger: {} = {}", logger, level);
    loggerContext.getLogger(logger).setLevel(Level.valueOf(level));
  }

  private LoggerContext loggerContext() {
    return LogbackContextProvider.get();
  }
}
