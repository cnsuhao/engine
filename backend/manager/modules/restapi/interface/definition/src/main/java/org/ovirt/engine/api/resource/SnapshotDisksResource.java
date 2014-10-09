package org.ovirt.engine.api.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.jboss.resteasy.annotations.providers.jaxb.Formatted;
import org.ovirt.engine.api.model.Disks;

@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.APPLICATION_X_YAML})
public interface SnapshotDisksResource {

    @GET
    @Formatted
    public Disks list();

    @Path("{id}")
    public SnapshotDiskResource getDiskSubResource(@PathParam("id") String id);
}
