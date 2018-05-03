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
import com.dotmarketing.business.Role;
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
    public Response loadPermissionsFromCache(@Context HttpServletRequest request, @PathParam("cacheKey") String cacheKey)
            throws DotStateException, DotDataException, DotSecurityException, JSONException {

        JSONArray finalOutput;

        //Retrieve info from DB.
        List<Map<String, String>> occurrenceInIdentifierTable = findIdentiferObjectInDB(cacheKey);
        if (occurrenceInIdentifierTable != null && occurrenceInIdentifierTable.size() > 0) {

            finalOutput = generateJSONOutput(occurrenceInIdentifierTable, true);

            ResponseBuilder builder = Response.ok(finalOutput.toString(), "application/json");
            return builder.build();

        }
        String username = (sysUser != null) ? sysUser.getFullName() : " unknown ";
        ResponseBuilder builder = Response.ok("{\"result\":\"/test/" + username + " GET!\"}", "application/json");
        return builder.build();
    }

    /**
     * This resource is meant to look for permissions in DB
     * if a match is found, it will return its information
     *
     * @param request
     * @param assetId
     * @return
     * @throws DotStateException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @GET
    @JSONP
    @Path("/assetId/{assetId}")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response loadPermissionsFromDB(@Context HttpServletRequest request, @PathParam("assetId") String assetId)
            throws DotStateException, DotDataException, DotSecurityException, JSONException {

        JSONArray finalOutput;

        //Retrieve info from DB.
        List<Map<String, String>> occurrenceInIdentifierTable = findIdentiferObjectInDB(assetId);
        if (occurrenceInIdentifierTable != null && occurrenceInIdentifierTable.size() > 0) {

            finalOutput = generateJSONOutput(occurrenceInIdentifierTable, false);

            ResponseBuilder builder = Response.ok(finalOutput.toString(), "application/json");
            return builder.build();

        }
        String username = (sysUser != null) ? sysUser.getFullName() : " unknown ";
        ResponseBuilder builder = Response.ok("{\"result\":\"/test/" + username + " GET!\"}", "application/json");
        return builder.build();
    }

    private JSONArray generateJSONOutput(List<Map<String, String>> occurrenceInIdentifierTable, boolean lookupPermissionOnCache)
            throws DotDataException, JSONException, DotSecurityException {

        ContentletVersionInfo conVerInfo;
        Versionable assetVersion;
        List<Permission> permissionList = new ArrayList<>();
        Language defaultLang = langAPI.getDefaultLanguage();

        JSONArray output = new JSONArray();
        JSONObject generatedOutput = new JSONObject();

        String id = occurrenceInIdentifierTable.get(0).get("id");
        String siteIdentifier = occurrenceInIdentifierTable.get(0).get("host_inode");
        String parentPath = occurrenceInIdentifierTable.get(0).get("parent_path");
        String assetName = occurrenceInIdentifierTable.get(0).get("asset_name");
        String assetType = occurrenceInIdentifierTable.get(0).get("asset_type");

        generatedOutput.append("IdentifierObject",id);
        generatedOutput.append("SiteIdentifier", siteIdentifier);
        generatedOutput.append("ParentPath",parentPath);
        generatedOutput.append("AssetName", assetName);
        generatedOutput.append("AssetType",assetType);

        output.add(generatedOutput);

        if(lookupPermissionOnCache){
            //Lookup is made in Cache

            //Depending on asset type, we retrieve its Versionable Info using the default language
            if("contentlet".equalsIgnoreCase(assetType)){
                conVerInfo = versionableAPI.getContentletVersionInfo(id,defaultLang.getId());

                Contentlet cachedContent = cc.get(conVerInfo.getWorkingInode());

                permissionList = permAPI.getPermissions(cachedContent);

            } else if (!"folder".equalsIgnoreCase(assetType)){
                assetVersion = versionableAPI.findWorkingVersion(id,sysUser,false);
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

            int i = 1;

            for (Permission p: permissionList) {

                generatedOutput = new JSONObject();
                generatedOutput.append(i + "_PermissionableIdFromCache",p.getInode());
                generatedOutput.append(i + "PermissionableTypeFromCache",p.getClass().getName());
                generatedOutput.append(i + "_Role/UserFromCache",roleAPI.loadRoleById(p.getRoleId()).getName());
                generatedOutput.append(i +"_PermissionLevelFromCache",p.getPermission());
                output.add(generatedOutput);
                i++;
            }

        } else {
            //lookup is made in DB
            List<Map<String, String>> permissionFromDB = findPermissionFromDB(id);

            if(permissionFromDB.size() == 0) {
                permissionFromDB = findPermissionGivenPermissionReference(id);
            }

            if(permissionFromDB.size() > 0) {
                for (int k = 0 ; k <  permissionFromDB.size() ; k++) {
                    generatedOutput = new JSONObject();
                    Role permissionRole = roleAPI.loadRoleById( permissionFromDB.get(k).get("roleid"));
                    generatedOutput.append(k + "_PermissionableIdFromDB", permissionFromDB.get(k).get("inode_id"));
                    generatedOutput.append(k + "PermissionableTypeFromDB", permissionFromDB.get(k).get("permission_type"));
                    generatedOutput.append(k + "_Role/UserFromDB", permissionRole.getName());
                    generatedOutput.append(k +"_PermissionLevelFromDB", permissionFromDB.get(k).get("permission"));
                    output.add(generatedOutput);
                }
            }
        }

        return output;

    }

    /**
     * Retrieve the Identifier Object from DB
     * @param id The asset id
     * @return The object pulled from DB
     */
    private List<Map<String, String>> findIdentiferObjectInDB (String id) throws DotDataException {
        DotConnect dc = new DotConnect();
        dc.setSQL("SELECT * FROM identifier WHERE id = ?");
        dc.addParam(id);
        return dc.loadResults();
    }

    /**
     * Retrieve the Permssion Object from DB
     * This is useful for looking at permissions for contents with individual permissions
     * @param id The asset id
     * @return The object pulled from DB
     */
    private List<Map<String, String>> findPermissionFromDB (String id) throws DotDataException {
        DotConnect dc = new DotConnect();
        dc.setSQL("SELECT * FROM permission WHERE inode_id = ?");
        dc.addParam(id);
        return dc.loadResults();
    }

    /**
     * Retrieve the Parent Permissions of an Object on DB, given a child asset identifier
     * This is useful for looking at permissions for contents that are inheriting permissions
     * @param id The asset id
     * @return The object pulled from DB
     */
    private List<Map<String, String>> findPermissionGivenPermissionReference (String id) throws DotDataException {
        DotConnect dc = new DotConnect();
        dc.setSQL("SELECT * FROM permission WHERE inode_id in "
                + "(SELECT reference_id from permission_reference"
                + " where asset_id = ?)");
        dc.addParam(id);
        return dc.loadResults();
    }

}