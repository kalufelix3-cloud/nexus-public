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
package org.sonatype.nexus.bootstrap.spring;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import static java.util.Collections.emptyList;
import static java.util.Locale.ENGLISH;
import static org.sonatype.nexus.common.app.FeatureFlags.FEATURE_SPRING_ONLY;

@Component
@ConditionalOnProperty(value = FEATURE_SPRING_ONLY, havingValue = "true")
public class SpringComponentScan
    implements ApplicationListener<ContextRefreshedEvent>
{
  private static final Logger LOG = LoggerFactory.getLogger(SpringComponentScan.class);

  private static final List<String> SPRING_SCANNED_JAVA_PACKAGES =
      List.of("org.sonatype.licensing",
          "org.sonatype.nexus.bootstrap.entrypoint",
          "com.sonatype.nexus.bootstrap.entrypoint");

  private static final List<String> JAVA_PACKAGES_FOR_NEXUS_SCANNING =
      List.of("org.sonatype.nexus.bootstrap", "com.sonatype.nexus.bootstrap");

  private final ClassLoader classLoader = getClass().getClassLoader();

  private final ApplicationContext applicationContext;

  private final ApplicationEventPublisher applicationEventPublisher;

  @Autowired
  public SpringComponentScan(
      final ApplicationContext applicationContext,
      final ApplicationEventPublisher applicationEventPublisher)
  {
    this.applicationContext = applicationContext;
    this.applicationEventPublisher = applicationEventPublisher;
  }

  /**
   * Upon receipt of this event, the component scan done by the @SpringBootApplication and any @ComponentScan
   * annotations has completed. Meaning the app has started, selected the proper edition of nexus, and now needs to
   * scan the (org|com)/sonatype/nexus/bootstrap packages to scan for any @Configuration classes from modules that need
   * to be checked for loading. Each of those @Configuration classes may potentially do @ComponentScan of their own
   * module for injection is required.
   */
  @Override
  public void onApplicationEvent(ContextRefreshedEvent event) {
    if (event.getApplicationContext() == this.applicationContext &&
        applicationContext instanceof ConfigurableApplicationContext) {
      try {
        finishBootstrapComponentScanning();
      }
      catch (IOException e) {
        LOG.error("Failed to do additional programmatic component scan", e);
      }
    }
    else {
      LOG.error("ApplicationContext is null or not configurable ({}), cannot inject components", applicationContext);
    }
  }

  private void finishBootstrapComponentScanning() throws IOException {
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    Resource[] resources = resolver.getResources("classpath*:*.jar"); // Scan for all jars in classpath

    for (Resource resource : resources) {
      List<Class<?>> classesForInjection = new ArrayList<>(maybeGetClassesForInjection(resource));

      classesForInjection.sort((o1, o2) -> {
        int order1 = getOrder(o1);
        int order2 = getOrder(o2);
        return -Integer.compare(order1, order2);
      });

      classesForInjection.forEach(this::injectClass);
    }

    applicationEventPublisher
        .publishEvent(new NexusComponentScanCompleteEvent((ConfigurableApplicationContext) applicationContext));
  }

  private List<Class<?>> maybeGetClassesForInjection(final Resource resource) throws IOException {
    if (resource.isReadable()) {
      LOG.debug("Scanning embedded jar {} for component class names", resource.getURI());
      List<Class<?>> classNamesForInjection = new ArrayList<>();
      try (InputStream inputStream = resource.getInputStream();
          JarInputStream jarInputStream = new JarInputStream(
              inputStream)) {
        JarEntry jarEntry;
        while ((jarEntry = jarInputStream.getNextJarEntry()) != null && jarEntry.getName().endsWith(".class")) {
          String className = jarEntry.getName().replace('/', '.').replace(".class", "");

          Class<?> classForInjection = maybeGetClassForInjection(className);
          if (classForInjection != null) {
            classNamesForInjection.add(classForInjection);
          }
        }
      }
      return classNamesForInjection;
    }
    return emptyList();
  }

  private Class<?> maybeGetClassForInjection(final String className) {
    String packageName = className.lastIndexOf('.') > 0 ? className.substring(0, className.lastIndexOf('.')) : "";

    if (SPRING_SCANNED_JAVA_PACKAGES.stream().noneMatch(packageName::startsWith)) {
      if (JAVA_PACKAGES_FOR_NEXUS_SCANNING.stream().anyMatch(packageName::startsWith)) {
        try {
          return Class.forName(className, true, classLoader);
        }
        catch (ClassNotFoundException e) {
          LOG.error("Failed to load class name: {}", className, e);
        }
      }
      else {
        LOG.trace("Skipping class {} as it is not in a java package declared for scanning {}", className, packageName);
      }
    }
    else {
      LOG.trace("Skipping class {} as it is in a java package already scanned by spring {}", className, packageName);
    }
    return null;
  }

  private void injectClass(final Class<?> loadedClass) {
    ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext) applicationContext;
    BeanDefinitionRegistry registry = (BeanDefinitionRegistry) configurableApplicationContext;
    BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(loadedClass);
    BeanDefinition definition = builder.getBeanDefinition();
    String beanName = loadedClass.getSimpleName().toLowerCase(ENGLISH);
    registry.registerBeanDefinition(beanName, definition);
    LOG.debug("Injected component: {} ({})", beanName, loadedClass.getName());
  }

  private int getOrder(final Class<?> clazz) {
    Order orderAnnotation = clazz.getAnnotation(Order.class);
    // 0 is highest priority, MAX_INTEGER is the default value
    return orderAnnotation != null ? orderAnnotation.value() : Integer.MAX_VALUE;
  }
}
