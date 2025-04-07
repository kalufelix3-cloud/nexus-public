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
package org.sonatype.nexus.bootstrap.entrypoint.jetty;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.bootstrap.entrypoint.configuration.NexusProperties;
import org.sonatype.nexus.bootstrap.entrypoint.configuration.PropertyMap;
import org.sonatype.nexus.bootstrap.entrypoint.jvm.ShutdownDelegate;
import org.sonatype.nexus.bootstrap.jetty.ConnectorConfiguration;
import org.sonatype.nexus.bootstrap.jetty.ConnectorManager;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandler.Context;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.xml.XmlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.Nullable;

/**
 * Jetty server.
 */
@Singleton
@Named
@ConditionalOnProperty(value = "nexus.spring.only", havingValue = "true")
public class JettyServer
{
  private static final Logger LOG = LoggerFactory.getLogger(JettyServer.class);

  private final NexusProperties nexusProperties;

  private JettyMainThread thread;

  private ConnectorManager connectorManager;

  private ShutdownDelegate shutdownDelegate;

  @Inject
  public JettyServer(final NexusProperties nexusPropeties, final ShutdownDelegate shutdownDelegate) {
    this.nexusProperties = nexusPropeties;
    this.shutdownDelegate = shutdownDelegate;
  }

  private Exception propagateThrowable(final Throwable e) throws Exception {
    if (e instanceof RuntimeException) {
      throw (RuntimeException) e;
    }
    else if (e instanceof Exception) {
      throw (Exception) e;
    }
    else if (e instanceof Error) {
      throw (Error) e;
    }
    throw new Error(e);
  }

  public List<ConnectorConfiguration> defaultConnectors() {
    return connectorManager.defaultConnectors();
  }

  public void addCustomConnector(final ConnectorConfiguration connectorConfiguration) {
    connectorManager.addConnector(connectorConfiguration);
  }

  public void removeCustomConnector(final ConnectorConfiguration connectorConfiguration) {
    connectorManager.removeConnector(connectorConfiguration);
  }

  /**
   * Starts Jetty, in sync or async mode, depending on value of {@code waitForServer} parameter.
   *
   * @param waitForServer if {@code true}, method will block until Jetty is fully started, otherwise will return
   *          immediately.
   * @param callback optional, callback executed immediately after Jetty is fully started up.
   */
  public synchronized void start(final boolean waitForServer, @Nullable final Runnable callback) throws Exception {
    try {
      doStart(waitForServer, callback);
    }
    catch (Exception e) {
      LOG.error("Start failed", e);
      throw propagateThrowable(e);
    }
  }

  private void doStart(boolean waitForServer, @Nullable final Runnable callback) throws Exception { // NOSONAR
    if (thread != null) {
      throw new IllegalStateException("Already started");
    }

    LOG.info("Starting jetty");

    List<LifeCycle> components = new ArrayList<>();

    PropertyMap props = new PropertyMap();
    props.putAll(nexusProperties.get());

    String[] args = props.get("nexus-args").split(",");

    // For all arguments, load properties or parse XMLs
    XmlConfiguration last = null;
    for (String arg : args) {
      try (Resource resource = Resource.newResource(arg)) {
        URL url = resource.getURL();
        if (url.getFile().toLowerCase(Locale.ENGLISH).endsWith(".properties")) {
          LOG.info("Loading properties: {}", url);

          props.load(url);
        }
        else {
          LOG.info("Applying configuration: {}", url);

          XmlConfiguration configuration = new XmlConfiguration(resource);
          if (last != null) {
            configuration.getIdMap().putAll(last.getIdMap());
          }
          if (!props.isEmpty()) {
            configuration.getProperties().putAll(props);
          }
          Object component = configuration.configure();
          if (component instanceof LifeCycle) {
            components.add((LifeCycle) component);
          }
          last = configuration;
        }
      }
    }

    // complain if no components configured
    if (components.isEmpty()) {
      throw new Exception("Failed to configure any components");
    }

    Server server = null;
    for (Object object : components) {
      if (object instanceof Server) {
        server = (Server) object;
        break;
      }
    }

    connectorManager = new ConnectorManager(server, last.getIdMap());

    thread = new JettyMainThread(components, shutdownDelegate, callback);
    thread.startComponents(waitForServer);
  }

  public synchronized void stop() throws Exception {
    try {
      doStop();
    }
    catch (Exception e) {
      LOG.error("Stop failed", e);
      throw propagateThrowable(e);
    }
  }

  private void doStop() throws Exception {
    if (thread == null) {
      throw new IllegalStateException("Not started");
    }

    LOG.info("Stopping jetty");

    thread.stopComponents();
    thread = null;

    LOG.info("Stopped jetty");
  }

  /**
   * Jetty thread used to start components, wait for the server's threads to join and stop components.
   * <p>
   * Needed so that once {@link JettyServer#stop()} returns that we know that the server has actually stopped, which is
   * required for embedding.
   */
  private static class JettyMainThread
      extends Thread
  {
    private static final AtomicInteger INSTANCE_COUNTER = new AtomicInteger(1);

    private final List<LifeCycle> components;

    private final Runnable callback;

    private final CountDownLatch started;

    private final CountDownLatch stopped;

    private final ShutdownDelegate shutdownDelegate;

    private volatile Exception exception;

    public JettyMainThread(
        final List<LifeCycle> components,
        final ShutdownDelegate shutdownDelegate,
        @Nullable final Runnable callback)
    {
      super("jetty-main-" + INSTANCE_COUNTER.getAndIncrement());
      this.components = components;
      this.shutdownDelegate = shutdownDelegate;
      this.callback = callback;
      this.started = new CountDownLatch(1);
      this.stopped = new CountDownLatch(1);
    }

    @Override
    public void run() {
      Server server = null;
      try {
        try {
          for (LifeCycle component : components) {
            if (!component.isRunning()) {
              LOG.info("Starting: {}", component);
              component.start();
            }

            // capture the server reference
            if (component instanceof Server) {
              server = (Server) component;
            }
          }
        }
        catch (Exception e) {
          exception = e;
        }
        finally {
          started.countDown();
        }

        if (server != null) {
          logStartupBanner(server);
          if (callback != null) {
            callback.run();
          }
          server.join();
        }
      }
      catch (InterruptedException e) {
        // nothing
        LOG.info("caught interrupt signal, shutting down");
      }
      finally {
        stopped.countDown();
      }

      if (server == null) {
        LOG.error("Failed to start", exception);
        shutdownDelegate.exit(-1);
      }
    }

    public void startComponents(boolean waitForServer) throws Exception {
      start();

      if (waitForServer) {
        started.await();
      }

      if (exception != null) {
        throw exception;
      }
    }

    public void stopComponents() throws Exception {
      Collections.reverse(components);

      // if Jetty thread is still waiting for a component to start, this should unblock it
      interrupt();

      for (LifeCycle component : components) {
        if (component.isRunning()) {
          LOG.info("Stopping: {}", component);
          component.stop();
        }
      }

      components.clear();
      stopped.await();
    }

    private static void logStartupBanner(final Server server) {
      Object banner = null;

      ContextHandler contextHandler = server.getChildHandlerByClass(ContextHandler.class);
      if (contextHandler != null) {
        Context context = contextHandler.getServletContext();
        if (context != null) {
          banner = context.getAttribute("nexus-banner");
        }
      }

      StringBuilder buf = new StringBuilder();
      buf.append("\n-------------------------------------------------\n\n");
      buf.append("Started ").append(banner instanceof String ? banner : "Nexus Repository Manager");
      buf.append("\n\n-------------------------------------------------");
      LOG.info(buf.toString());
    }
  }
}
