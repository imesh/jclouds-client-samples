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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.Hardware;
import org.jclouds.compute.domain.Image;
import org.jclouds.compute.domain.internal.NodeMetadataImpl;
import org.jclouds.domain.Location;

import java.util.Set;

/**
 * Sample jclouds softlayer client.
 */
public class Main {
    private static final Log log = LogFactory.getLog(Main.class);

    public static final String userName = ""; // Add softlayer identity here
    public static final String apiKey = ""; // Add softlayer api key here
    public static final String imageId = "UBUNTU_12_64"; // Ubuntu Linux 12.04 LTS Precise Pangolin - Minimal Install (64 bit)
    public static final String locationId = "dal01"; // Dallas 1
    public static final String hardwareId = "cpu=1,memory=4096,disk=25,type=SAN";

    public static void main(String[] args) {
        SoftLayerIaaSProvider provider = new SoftLayerIaaSProvider(userName, apiKey);

        // Un-comment required method calls to test their functionality

        Set<? extends Location> locations = provider.listLocations();
        log.info("Locations: " + locations.toString());
        Set<? extends Hardware> hardware = provider.listHardware();
        log.info("Hardware: " + hardware.toString());
        Set<? extends Image> images = provider.listImages();
        log.info("Images: " + images.toString());
        Set<? extends ComputeMetadata> instances = provider.listInstances();
        printInstances(instances);

        //provider.createInstance(imageId, locationId, hardwareId, OsFamily.UBUNTU, "12.04", "service.com", "vm-1", "A=1234,B=1234,C=1234");
        //provider.terminateInstanceByName("vm-1");
    }

    private static void printInstances(Set<? extends ComputeMetadata> instances) {
        if(instances != null) {
            for(ComputeMetadata instance: instances) {
                NodeMetadataImpl instance1 = (NodeMetadataImpl) instance;
                log.info(instance.getId() + " " + instance.getName() + " " + instance1.getOperatingSystem() + " " + instance1.getStatus());
            }
        }
    }
}
