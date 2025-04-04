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
package org.sonatype.nexus.spring.application.classpath.walker;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import static java.util.stream.Collectors.toList;

/**
 * !!!! DEPRECATED no longer a needed process with everything injected into spring now. This class should be
 * removed when the previous DI architecture is removed. Until then changes should primarily be done on the newer
 * "nexus.spring.only=true" impl, then only brought back to this class if necessary
 */
@Deprecated(since = "4/1/2025", forRemoval = true)
@ConditionalOnProperty(value = "nexus.spring.only", havingValue = "false", matchIfMissing = true)
@Named
@Singleton
public class ClasspathWalker
{
  private static final Logger LOG = LoggerFactory.getLogger(ClasspathWalker.class);

  private final List<ClasspathVisitor> classpathVisitors;

  private final ApplicationJarFilter applicationJarFilter;

  @Inject
  public ClasspathWalker(
      final List<ClasspathVisitor> classpathVisitors,
      final ApplicationJarFilter applicationJarFilter)
  {
    this.classpathVisitors = classpathVisitors;
    this.applicationJarFilter = applicationJarFilter;
  }

  public void walk(final File base) throws Exception {
    LOG.debug("Building the IoC classpath index(es) from: {}", base);
    List<String> applicationJarPaths = getApplicationJarPaths(base);

    if (!applicationJarPaths.isEmpty()) {
      for (String applicationJarPath : applicationJarPaths) {
        LOG.debug("Walking classpath of: {} from base {}", applicationJarPath, base);
        if (base.isFile()) {
          try (JarFile baseJar = new JarFile(base)) {
            visitJarEntries(applicationJarPath, baseJar);
          }
        }
      }
    }
    else {
      List<Path> paths = getApplicationClasspath().stream().map(Paths::get).toList();
      for (Path cpEntry : paths) {
        LOG.debug("Walking classpath entry: {}", cpEntry);
        if (Files.isDirectory(cpEntry)) {
          visitDirectory(cpEntry);
        }
        else {
          try (InputStream in = new BufferedInputStream(Files.newInputStream(cpEntry))) {
            visitJarEntries(in, cpEntry.toString());
          }
        }
      }
    }
  }

  private void visitJarEntries(final String applicationJarPath, final JarFile baseJar) throws IOException {
    JarEntry nestedJar = baseJar.getJarEntry(applicationJarPath);
    try (InputStream in = baseJar.getInputStream(nestedJar)) {
      visitJarEntries(in, applicationJarPath);
    }
  }

  private void visitJarEntries(final InputStream in, final String applicationJarPath) throws IOException {
    try (JarInputStream applicationJarInputStream = new JarInputStream(in)) {
      JarEntry nestedJarEntry = applicationJarInputStream.getNextJarEntry();
      while (nestedJarEntry != null) {
        LOG.debug("Visiting entry: {} in {}", nestedJarEntry.getName(), applicationJarPath);
        for (ClasspathVisitor classpathVisitor : classpathVisitors) {
          // with single inputstream only one visitor can successfully visit
          if (classpathVisitor.visit(nestedJarEntry.getName(), applicationJarPath, applicationJarInputStream)) {
            break;
          }
        }
        nestedJarEntry = applicationJarInputStream.getNextJarEntry();
      }
    }
  }

  private void visitDirectory(final Path directory) throws IOException {
    try (Stream<Path> files = Files.walk(directory)) {
      files.filter(Files::isRegularFile).forEach(file -> {
        String path = directory.relativize(file).toString().replace('\\', '/');
        LOG.debug("Visiting entry: {} in {}", path, directory);
        try (InputStream in = new BufferedInputStream(Files.newInputStream(file))) {
          for (ClasspathVisitor classpathVisitor : classpathVisitors) {
            // with single inputstream only one visitor can successfully visit
            if (classpathVisitor.visit(path, directory.toString(), in)) {
              break;
            }
          }
        }
        catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      });
    }
  }

  private List<String> getApplicationJarPaths(final File base) throws IOException {
    String springBootClasspathIndexPath = "BOOT-INF/classpath.idx";
    if (base.isFile()) {
      try (JarFile jarFile = new JarFile(base)) {
        JarEntry indexEntry = jarFile.getJarEntry(springBootClasspathIndexPath);
        if (indexEntry != null) {
          return filterForSupportedApplications(fileToStringList(jarFile.getInputStream(indexEntry)));
        }
      }
    }
    else if (base.isDirectory()) {
      File indexFile = new File(base, springBootClasspathIndexPath);
      if (indexFile.exists() && indexFile.isFile()) {
        return filterForSupportedApplications(fileToStringList(Files.newInputStream(indexFile.toPath())));
      }
    }
    return List.of();
  }

  private List<String> getApplicationClasspath() {
    return filterForSupportedApplications(
        Arrays.asList(System.getProperty("java.class.path").split(File.pathSeparator)));
  }

  private List<String> filterForSupportedApplications(final List<String> applicationJarPaths) {
    return applicationJarPaths.stream().filter(applicationJarFilter::allowed).collect(toList());
  }

  private List<String> fileToStringList(final InputStream inputStream) throws IOException {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
      return reader
          .lines()
          .filter(line -> !line.trim().isEmpty())
          .map(line -> line.trim().replace("- ", "").replace("\"", ""))
          .collect(toList());
    }
  }
}
