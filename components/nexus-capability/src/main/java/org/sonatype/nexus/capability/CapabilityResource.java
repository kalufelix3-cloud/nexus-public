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
package org.sonatype.nexus.capability;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.rest.Resource;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.sonatype.nexus.capability.CapabilityReferenceFilterBuilder.capabilities;
import static org.sonatype.nexus.capability.CapabilityType.capabilityType;
import static org.sonatype.nexus.rest.APIConstants.V1_API_PREFIX;

@Named
@Singleton
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(CapabilityResource.RESOURCE_PATH)
public class CapabilityResource
    extends ComponentSupport
    implements Resource, CapabilityResourceDoc
{
  static final String RESOURCE_PATH = V1_API_PREFIX + "/capabilities";

  private final CapabilityRegistry capabilityRegistry;

  private final CapabilityDescriptorRegistry capabilityDescriptorRegistry;

  @Inject
  public CapabilityResource(
      final CapabilityRegistry capabilityRegistry,
      final CapabilityDescriptorRegistry capabilityDescriptorRegistry)
  {
    this.capabilityRegistry = capabilityRegistry;
    this.capabilityDescriptorRegistry = capabilityDescriptorRegistry;
  }

  @RequiresAuthentication
  @RequiresPermissions("nexus:capabilities:read")
  @GET
  public List<CapabilityResponse> getCapabilities() {
    capabilityRegistry.pullAndRefreshReferencesFromDB();
    return capabilityRegistry.get(capabilities()).stream().map(this::asCapability).collect(toList());

  }

  @RequiresAuthentication
  @RequiresPermissions("nexus:capabilities:read")
  @Path("descriptors")
  @GET
  public List<String> getCapabilityDescriptors() {
    return Arrays.stream(capabilityDescriptorRegistry.getAll())
        .filter(capability -> capability.isExposed() && !capability.isHidden())
        .map(CapabilityDescriptor::name)
        .filter(Objects::nonNull)
        .collect(toList());
  }

  @RequiresPermissions("nexus:capabilities:create")
  @RequiresAuthentication
  @POST
  public void createCapability(final CapabilityCreate capability) {
    Arrays.stream(capabilityDescriptorRegistry.getAll())
        .filter(descriptor -> descriptor.name() != null && descriptor.name().equals(capability.name))
        .findFirst()
        .ifPresent(descriptor -> {
          asCapability(capabilityRegistry.add(capabilityType(descriptor.type().toString()), capability.enabled(),
              capability.notes(), capability.properties()));
        });
  }

  private CapabilityResponse asCapability(CapabilityReference capabilityReference) {
    CapabilityDescriptor descriptor = capabilityReference.context().descriptor();
    Capability capability = capabilityReference.capability();
    String state = "disabled";
    if (capabilityReference.context().isEnabled() && capabilityReference.context().hasFailure()) {
      state = "error";
    }
    else if (capabilityReference.context().isEnabled() && capabilityReference.context().isActive()) {
      state = "active";
    }
    else if (capabilityReference.context().isEnabled() && !capabilityReference.context().isActive()) {
      state = "passive";
    }

    Set<Tag> tags = new HashSet<>();
    Map<String, String> tagsMap = new HashMap<>();
    if (descriptor instanceof Taggable && ((Taggable) descriptor).getTags() != null) {
      tags.addAll(((Taggable) descriptor).getTags());
    }
    if (capability instanceof Taggable && ((Taggable) capability).getTags() != null) {
      tags.addAll(((Taggable) capability).getTags());
    }
    if (!tags.isEmpty()) {
      tagsMap = tags.stream().collect(toMap(Tag::key, Tag::value));
    }

    return new CapabilityResponse(
        descriptor.name(),
        capabilityReference.context().isEnabled(),
        capabilityReference.context().notes(),
        capabilityReference.context().properties(),
        capabilityReference.context().isActive(),
        capabilityReference.context().hasFailure(),
        capability.description(),
        state,
        capabilityReference.context().stateDescription(),
        capability.status(),
        tagsMap,
        descriptor.getDisableWarningMessage(),
        descriptor.getDeleteWarningMessage());
  }

  public record CapabilityResponse(
      String typeName,
      Boolean enabled,
      String notes,
      Map<String, String> properties,
      Boolean active,
      Boolean error,
      String description,
      String state,
      String stateDescription,
      String status,
      Map<String, String> tags,
      String disableWarningMessage,
      String deleteWarningMessage)
  {
  }

  public record CapabilityCreate(
      String name,
      boolean enabled,
      String notes,
      Map<String, String> properties)
  {
  }
}
