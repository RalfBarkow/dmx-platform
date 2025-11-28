package systems.dmx.webservice;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import systems.dmx.core.service.CoreService;

/**
 * Minimal health/version endpoint for DMX.
 *
 * Exposed at: GET /dmx/version
 */
@Path("/dmx")
public class VersionResource {

    private final CoreService core;

    // CoreService should be injected by DMX/OSGi (adapt to your pattern)
    public VersionResource(CoreService core) {
        this.core = core;
    }

    @GET
    @Path("/version")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getVersion() {
        // --- Choose how to get the version string ---

        // Option A (if such a method exists in your CoreService):
        // String version = core.getApplicationVersion();

        // Option B: hard-code for now (you can refactor later):
        String version = "5.3.5";

        return Response.ok(version).build();
    }
}
