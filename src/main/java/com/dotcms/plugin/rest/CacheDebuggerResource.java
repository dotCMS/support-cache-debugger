package com.dotcms.rest;

import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.PathParam;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.CacheControl;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.javax.ws.rs.core.Response.ResponseBuilder;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import javax.servlet.http.HttpServletRequest;

@Path("/cacheDebugger")
public class CacheDebuggerResource  {

    private final WebResource webResource = new WebResource();

    /**
     * This resource is meant to look for a cache Key on different cache regions
     * if a match is found, it will return its information
     * along with the place where it should be located
     *
     * @param request
     * @param cacheKey
     * @return
     * @throws DotStateException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @GET
    @JSONP
    @Path("/cacheKey/{cacheKey}")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response loadJson(@Context HttpServletRequest request, @PathParam("cacheKey") String cacheKey) throws DotStateException {
        User user = APILocator.systemUser();
        String username = (user != null) ? user.getFullName() : " unknown ";
        ResponseBuilder builder = Response.ok("{\"result\":\"/test/" + username + " GET!\"}", "application/json");
        return builder.build();
    }

}