package com.dotcms.rest;

import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.PathParam;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.javax.ws.rs.core.Response.ResponseBuilder;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletCache;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

@Path("/cacheDebugger")
public class CacheDebuggerResource  {

    private final WebResource webResource = new WebResource();
    private final ContainerAPI containerAPI = APILocator.getContainerAPI();
    private final LanguageAPI langAPI =  APILocator.getLanguageAPI();
    private PermissionAPI permAPI = APILocator.getPermissionAPI();
    private RoleAPI roleAPI = APILocator.getRoleAPI();
    private final TemplateAPI templateAPI =  APILocator.getTemplateAPI();
    private final VersionableAPI versionableAPI =  APILocator.getVersionableAPI();
    private final User sysUser = APILocator.systemUser();
    private ContentletCache cc = CacheLocator.getContentletCache();

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
    public Response loadJson(@Context HttpServletRequest request, @PathParam("cacheKey") String cacheKey)
            throws DotStateException, DotDataException, DotSecurityException, JSONException {

        String id;
        String siteIdentifier;
        String parentPath;
        String assetName;
        String assetType;
        ContentletVersionInfo conVerInfo;
        Versionable assetVersion;
        List<Permission> permissionList = new ArrayList<>();
        JSONObject generatedOutput = new JSONObject();
        Language defaultLang = langAPI.getDefaultLanguage();

        //Retrieve info from DB.
        List<Map<String, String>> occurrenceInIdentifierTable = findIdentiferObjectInDB(cacheKey);
        if (occurrenceInIdentifierTable != null && occurrenceInIdentifierTable.size() > 0) {
            //get the identifier's assetType
            id = occurrenceInIdentifierTable.get(0).get("id");
            siteIdentifier = occurrenceInIdentifierTable.get(0).get("host_inode");
            parentPath = occurrenceInIdentifierTable.get(0).get("parent_path");
            assetName = occurrenceInIdentifierTable.get(0).get("asset_name");
            assetType = occurrenceInIdentifierTable.get(0).get("asset_type");

            //Depending on asset type, we retrieve its Versionable Info using the default language
            if("contentlet".equalsIgnoreCase(assetType)){
                conVerInfo = versionableAPI.getContentletVersionInfo(cacheKey,defaultLang.getId());

                Contentlet cachedContent = cc.get(conVerInfo.getWorkingInode());

                permissionList = permAPI.getPermissions(cachedContent);

            } else if (!"folder".equalsIgnoreCase(assetType)){
                assetVersion = versionableAPI.findWorkingVersion(cacheKey,sysUser,false);
                if("template".equalsIgnoreCase(assetType)){
                    Template template =  templateAPI.find(assetVersion.getInode(),sysUser,false);
                    permissionList = permAPI.getPermissions(template);
                }
                if("containers".equalsIgnoreCase(assetType)){
                    Container container =  containerAPI.find(assetVersion.getInode(),sysUser,false);
                    permissionList = permAPI.getPermissions(container);
                }
                if("template".equalsIgnoreCase(assetType)){
                    Template template =  templateAPI.find(assetVersion.getInode(),sysUser,false);
                    permissionList = permAPI.getPermissions(template);
                }
            }

            JSONArray finalOutput = new JSONArray();

            generatedOutput.append("IdentifierObject",id);
            generatedOutput.append("SiteIdentifier", siteIdentifier);
            generatedOutput.append("ParentPath",parentPath);
            generatedOutput.append("AssetName", assetName);
            generatedOutput.append("AssetType",assetType);

            finalOutput.add(generatedOutput);

            int i = 1;

            for (Permission p: permissionList) {

                generatedOutput = new JSONObject();
                generatedOutput.append(i + "_PermissionableId",p.getInode());
                generatedOutput.append(i + "_PermissionableType",p.getType());
                generatedOutput.append(i + "PermissionableClassName",p.getClass().getName());
                generatedOutput.append(i + "_Role/User",roleAPI.loadRoleById(p.getRoleId()).getName());
                generatedOutput.append(i +"_PermissionLevel",p.getPermission());
                finalOutput.add(generatedOutput);
                i++;
            }

            ResponseBuilder builder = Response.ok(finalOutput.toString(), "application/json");
            return builder.build();

        }
        String username = (sysUser != null) ? sysUser.getFullName() : " unknown ";
        ResponseBuilder builder = Response.ok("{\"result\":\"/test/" + username + " GET!\"}", "application/json");
        return builder.build();
    }

    private List<Map<String, String>> findIdentiferObjectInDB (String id) throws DotDataException {
        DotConnect dc = new DotConnect();
        dc.setSQL("SELECT * FROM identifier WHERE id = ?");
        dc.addParam(id);
        return dc.loadResults();
    }

}