/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.imesh.sample.jclouds.softlayer;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.*;
import org.jclouds.domain.Location;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.softlayer.compute.options.SoftLayerTemplateOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * SoftLayer IaaS provider implementation for jclouds.
 */
public class SoftLayerIaaSProvider {
    private static final Log log = LogFactory.getLog(SoftLayerIaaSProvider.class);

    public static final String PAYLOAD = "PAYLOAD";

    public final String userName;
    public final String apiKey;
    private ComputeService compute;

    public SoftLayerIaaSProvider(String userName, String apiKey) {
        this.userName = userName;
        this.apiKey = apiKey;
        createComputeService();
    }

    private void createComputeService() {
        log.info("Creating compute service...");

        Properties properties = new Properties();

        Iterable<Module> modules = ImmutableSet.<Module>of(
                new SLF4JLoggingModule());

        compute = ContextBuilder.newBuilder("softlayer")
                    .credentials(userName, apiKey)
                    .overrides(properties)
                    .modules(modules)
                    .buildView(ComputeServiceContext.class)
                    .getComputeService();
    }

    private Template buildTemplate(String imageId, String locationId, String hardwareId, OsFamily osFamily, String osVersion, String domainName, String hostName, String payload, ComputeService compute) {
        log.info("Building template...");

        TemplateBuilder templateBuilder = compute.templateBuilder();

        templateBuilder.osFamily(osFamily);
        templateBuilder.imageId(imageId);
        templateBuilder.locationId(locationId);
        templateBuilder.hardwareId(hardwareId);
        templateBuilder.osVersionMatches(osVersion);

        templateBuilder.options(buildTemplateOptions(domainName, hostName, payload));

        return templateBuilder.build();
    }

    private SoftLayerTemplateOptions buildTemplateOptions(String domainName, String hostName, String payload) {
        log.info("Building template options...");

        SoftLayerTemplateOptions options = new SoftLayerTemplateOptions();

        options.domainName(domainName);
        options.userMetadata(PAYLOAD, payload);
        options.inboundPorts(22, 80);
        List<String> names = new ArrayList<String>();
        names.add(hostName);
        options.nodeNames(names);

        return options;
    }

    public Set<? extends Location> listLocations() {
        try {
            log.info("Listing locations...");

            Set<? extends Location> list = compute.listAssignableLocations();
            return list;
        } catch (Exception e) {
            log.error(e);
        }
        return null;
    }

    public Set<? extends Hardware> listHardware() {
        try {
            log.info("Listing hardware...");

             Set<? extends Hardware> list = compute.listHardwareProfiles();
            return list;
        } catch (Exception e) {
            log.error(e);
        }
        return null;
    }

    public Set<? extends Image> listImages() {
        try {
            log.info("Listing images...");

            Set<? extends Image> list = compute.listImages();
            return list;
        } catch (Exception e) {
            log.error(e);
        }
        return null;
    }

    public Set<? extends ComputeMetadata> listInstances() {
        try {
            log.info("Listing instances...");

            Set<? extends ComputeMetadata> nodes = compute.listNodes();
            return nodes;
        } catch (Exception e) {
            log.error(e);
        }
        return null;
    }

    public void createInstance(String imageId, String locationId, String hardwareId, OsFamily osFamily, String osVersion, String domainName, String hostName, String payload) {
        try {
            log.info(String.format("Creating new instance: [name] %s", hostName));

            log.info("Checking hostname availability...");
            Set<? extends ComputeMetadata> existingNodes = listInstances();
            boolean notAvailable = FluentIterable.from(existingNodes)
                    .transform(new Function<ComputeMetadata, Object>() {
                        @Override
                        public Object apply(ComputeMetadata input) {
                            return input.getName();
                        }
                    }).contains(hostName);
            if (notAvailable) {
                log.error(String.format("An instance with hostname %s already exits", hostName));
                return;
            }

            Template template = buildTemplate(imageId, locationId, hardwareId, osFamily, osVersion, domainName, hostName, payload, compute);
            Set<? extends NodeMetadata> nodes = compute.createNodesInGroup("jclouds", 1, template);
            for (ComputeMetadata node : nodes) {
                log.info(String.format("Instance created: [id] %s [name] %s", node.getId(), node.getName()));
            }
        } catch (Exception e) {
            log.error("Could not create instance", e);
        }
    }

    public void terminateInstanceByName(String instanceName) {
        try {

            log.info(String.format("Terminating instance: [instance] %s", instanceName));

            boolean found = false;
            Set<? extends ComputeMetadata> nodes = compute.listNodes();
            for (ComputeMetadata node : nodes) {
                if (node.getName().equals(instanceName)) {
                    found = true;
                    log.info(String.format("Terminating instance: [id] %s [name] %s", node.getId(), node.getName()));
                    compute.destroyNode(node.getId());
                    log.info(String.format("Instance terminated: [id] %s [name] %s", node.getId(), node.getName()));
                }
            }
            if (found) {
                log.info("Termination successfully completed");
            } else {
                log.warn("No active instances found to terminate");
            }
        } catch (Exception e) {
            log.error("Could not terminated instance", e);
        }
    }

    public void terminateInstanceById(String instanceId) {
        try {
            log.info(String.format("Terminating instance: [id] %s", instanceId));
            compute.destroyNode(instanceId);
            log.info(String.format("Instance terminated: [id] %s", instanceId));

        } catch (Exception e) {
            log.error("Could not terminated instance", e);
        }
    }
}
