/*
 * *** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 * Java(TM), hosted at https://github.com/gunterze/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * J4Care.
 * Portions created by the Initial Developer are Copyright (C) 2013
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See @authors listed below
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * *** END LICENSE BLOCK *****
 */

package org.dcm4chee.arc.conf.rs;

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.ConfigurationNotFoundException;
import org.dcm4che3.conf.api.DicomConfiguration;
import org.dcm4che3.conf.json.ConfigurationDelegate;
import org.dcm4che3.conf.json.JsonConfiguration;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.DeviceInfo;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @since Nov 2015
 */
@Path("/devices")
@RequestScoped
public class ConfigurationRS {

    @Inject
    private DicomConfiguration conf;

    @Inject
    private JsonConfiguration jsonConf;

    private ConfigurationDelegate configDelegate = new ConfigurationDelegate() {
        @Override
        public Device findDevice(String name) throws ConfigurationException {
            return conf.findDevice(name);
        }
    };

    @GET
    @Path("/{DeviceName}")
    @Produces("application/json")
    public StreamingOutput getDevice(@PathParam("DeviceName") String deviceName) throws Exception {
        final Device device;
        try {
            device = conf.findDevice(deviceName);
        } catch (ConfigurationNotFoundException e) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        return new StreamingOutput() {
            @Override
            public void write(OutputStream out) throws IOException {
                JsonGenerator w = Json.createGenerator(out);
                jsonConf.writeTo(device, w);
                w.flush();
            }
        };
    }

    @GET
    @Path("/")
    @Produces("application/json")
    public StreamingOutput listDevices() throws Exception {
        final DeviceInfo[] deviceInfos = conf.listDeviceInfos(null);
        return new StreamingOutput() {
            @Override
            public void write(OutputStream out) throws IOException {
                JsonGenerator gen = Json.createGenerator(out);
                gen.writeStartArray();
                for (DeviceInfo deviceInfo : deviceInfos)
                    jsonConf.writeTo(deviceInfo, gen);
                gen.writeEnd();
                gen.flush();
            }
        };
    }

    @PUT
    @Path("/{DeviceName}")
    @Consumes("application/json")
    public void createOrUpdateDevice(@PathParam("DeviceName") String deviceName, Reader content) throws Exception {
        Device device = jsonConf.loadDeviceFrom(Json.createParser(content), configDelegate);
        if (!device.getDeviceName().equals(deviceName))
            throw new WebApplicationException(
                    "Device name in content[" + device.getDeviceName() + "] does not match Device name in URL",
                    Response.Status.BAD_REQUEST);
        try {
            conf.merge(device);
        } catch (ConfigurationNotFoundException e) {
            conf.persist(device);
        }
    }

    @DELETE
    @Path("/{DeviceName}")
    public void deleteDevice(@PathParam("DeviceName") String deviceName) throws Exception {
        try {
            conf.removeDevice(deviceName);
        } catch (ConfigurationNotFoundException e) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
    }

 }
