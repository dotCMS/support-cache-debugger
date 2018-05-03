package com.dotcms.rest;

import com.dotcms.content.elasticsearch.business.ContentletIndexAPI;
import com.dotcms.content.elasticsearch.util.ESReindexationProcessStatus;
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
import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletCache;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.MaintenanceUtil;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

@Path("/supportDebugger")
public class SupportDebuggerResource  {

    private final ContainerAPI containerAPI = APILocator.getContainerAPI();
    private final ContentletAPI contentletAPI = APILocator.getContentletAPI();
    private ContentletIndexAPI indexAPI = APILocator.getContentletIndexAPI();
    private final LanguageAPI langAPI =  APILocator.getLanguageAPI();
    private final PermissionAPI permAPI = APILocator.getPermissionAPI();
    private final RoleAPI roleAPI = APILocator.getRoleAPI();
    private final TemplateAPI templateAPI =  APILocator.getTemplateAPI();
    private final UserAPI userAPI = APILocator.getUserAPI();
    private final VersionableAPI versionableAPI =  APILocator.getVersionableAPI();
    private final User sysUser = APILocator.systemUser();
    private final ContentletCache cc = CacheLocator.getContentletCache();

    private final WebResource webResource = new WebResource();

    /**
     * This resource is meant to look for a cache Key on different cache regions
     * if a match is found, it will return its information
     * along with the place where it should be located
     *
     * @param request
     * @param cacheKey the key/id of asset to look up in cache
     * @return
     * @throws DotStateException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @GET
    @JSONP
    @Path("/cacheKey/{cacheKey}/languageId/{langId}")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response loadPermissionsFromCache(@Context HttpServletRequest request,
            @PathParam("cacheKey") String cacheKey,
            @PathParam("langId") long langId)
            throws DotStateException, DotDataException, DotSecurityException, JSONException {

        InitDataObject initData = webResource.init(null, true, request, false, null);

        JSONArray finalOutput;

        //Retrieve info from DB.
        List<Map<String, String>> occurrenceInIdentifierTable = findIdentifierObjectInDB(cacheKey);
        if (occurrenceInIdentifierTable != null && occurrenceInIdentifierTable.size() > 0) {

            finalOutput = generateJSONOutput(occurrenceInIdentifierTable, langId, true);

            ResponseBuilder builder = Response.ok(finalOutput.toString(4), "application/json");
            return builder.build();

        }

        JSONObject errMsg = new JSONObject();
        errMsg.append("Nothing to see here", "exactly");
        errMsg.append("Why?", "The Asset Id passed in is invalid");
        ResponseBuilder builder = Response.ok(errMsg.toString(4), "application/json");
        return builder.build();
    }


    /**
     * This resource is meant to look for permissions in DB
     * if a match is found, it will return its information
     *
     * @param request
     * @param assetId the asset identifier
     * @return
     * @throws DotStateException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @GET
    @JSONP
    @Path("/assetId/{assetId}")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response loadPermissionsFromDB(@Context HttpServletRequest request,
            @PathParam("assetId") String assetId,
            @PathParam("langId") long langId)
            throws DotStateException, DotDataException, DotSecurityException, JSONException {

        InitDataObject initData = webResource.init(null, true, request, false, null);

        JSONArray finalOutput;

        //Retrieve info from DB.
        List<Map<String, String>> occurrenceInIdentifierTable = findIdentifierObjectInDB(assetId);
        if (occurrenceInIdentifierTable != null && occurrenceInIdentifierTable.size() > 0) {

            finalOutput = generateJSONOutput(occurrenceInIdentifierTable, langId, false);

            ResponseBuilder builder = Response.ok(finalOutput.toString(4), "application/json");
            return builder.build();

        }

        JSONObject errMsg = new JSONObject();
        errMsg.append("Nothing to see here", "exactly");
        errMsg.append("Why?", "The Asset Id passed in is invalid");
        ResponseBuilder builder = Response.ok(errMsg.toString(4), "application/json");
        return builder.build();
    }

    /**
     * This resource will flush cache given a specific region
     * if "all" is passed in, it will flush all cache regions.
     * Requires Authentication and CMS Administrator role
     *
     * @param request
     * @param cacheRegion Name of Cache Region
     * @return
     * @throws DotStateException
     * @throws DotDataException
     * @throws DotSecurityException
     * @see CacheLocator#getCacheIndexes()
     */
    @GET
    @JSONP
    @Path("/clearCache/{cacheRegion}")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response clearCacheRegion(@Context HttpServletRequest request, @PathParam("cacheRegion") String cacheRegion)
            throws DotStateException, DotDataException, JSONException {

        InitDataObject initData = webResource.init(null, true, request, true, null);

        User user = initData.getUser();

        JSONObject finalOutput = new JSONObject();

        Cachable cacheRegionObj = null;

        if(user != null && userAPI.isCMSAdmin(user)){
            try{
                if ("null".equalsIgnoreCase(cacheRegion) || "all".equalsIgnoreCase(cacheRegion)){
                    cacheRegion = "All";
                    MaintenanceUtil.flushCache();

                } else {
                    cacheRegionObj = CacheLocator.getCache(cacheRegion);
                    if(cacheRegionObj != null) {
                        cacheRegionObj.clearCache();
                    }
                }
            } catch (Exception e){
                Logger.error(this, "There was a problem clearing caches: " + e.getMessage());
            }

            finalOutput.append("FlushCacheCall","requested");

            if(cacheRegionObj != null || "all".equalsIgnoreCase(cacheRegion)) {
                finalOutput.append("RegionsCleared",cacheRegion);
            } else {
                finalOutput.append("RegionsCleared","None. Cache Region does not exist.");
            }

            ResponseBuilder builder = Response.ok(finalOutput.toString(4), "application/json");
            return builder.build();
        }

        finalOutput.append("Nothing to see here", "exactly");
        finalOutput.append("Why?", "You need to be a CMS Admin User");
        ResponseBuilder builder = Response.ok(finalOutput.toString(4), "application/json");
        return builder.build();
    }

    /**
     * This resource will kick a full reindex given an amount of shards
     * If amount of shards is set to zero, it will fallback to value as specified in config
     * If a Full Reindex is already running, it will show its progress instead
     * Requires Authentication and CMS Administrator role
     *
     * @param request
     * @param amountOfShards amount of Shards for the newly created ES Index
     * @return
     * @throws DotStateException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @GET
    @JSONP
    @Path("/fullReindex/amountOfShards/{amountOfShards}")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response runFullReindex(@Context HttpServletRequest request, @PathParam("amountOfShards") int amountOfShards)
            throws DotStateException, DotDataException, JSONException {

        InitDataObject initData = webResource.init(null, true, request, true, null);

        User user = initData.getUser();

        JSONObject finalOutput = new JSONObject();

        if(user != null && userAPI.isCMSAdmin(user)){

            if(amountOfShards == 0) {
                amountOfShards = Config.getIntProperty("es.index.number_of_shards", 2);
            }

            if(indexAPI.isInFullReindex()) {
                Map<String, Object> progressStats = ESReindexationProcessStatus.getProcessIndexationMap();
                finalOutput.append("IsReindexRunning", "yes");
                finalOutput.append("ContentCountToIndex",progressStats.get("contentCountToIndex"));
                finalOutput.append("LastIndexationProgress",progressStats.get("lastIndexationProgress"));
                finalOutput.append("CurrentIndexPath",progressStats.get("currentIndexPath"));
                finalOutput.append("NewIndexPath",progressStats.get("newIndexPath"));
            } else {
                Logger.info(this, "Running Contentlet Reindex.");
                finalOutput.append("IsReindexRunning", "No. It has been triggered. Please review log files");
                finalOutput.append("AmountOfShards", amountOfShards);
                HibernateUtil.startTransaction();
                contentletAPI.reindex();
                HibernateUtil.closeAndCommitTransaction();
                AdminLogger.log(SupportDebuggerResource.class, "runFullReindex", "Running Contentlet Reindex");
            }

            ResponseBuilder builder = Response.ok(finalOutput.toString(4), "application/json");
            return builder.build();
        }

        finalOutput.append("Nothing to see here", "exactly");
        finalOutput.append("Why?", "You need to be a CMS Admin User");
        ResponseBuilder builder = Response.ok(finalOutput.toString(4), "application/json");
        return builder.build();
    }


    /**
     * Generates the JSONArray which is going to be returned by the REST endpoints
     * @param occurrenceInIdentifierTable The asset id pulled from DB
     * @param lookupPermissionOnCache true to pull permissions from cache. false to pull permissions directly from DB
     * @return a JSONArray with identifier info and list of permissions this asset has
     */
    private JSONArray generateJSONOutput(List<Map<String, String>> occurrenceInIdentifierTable, long langId, boolean lookupPermissionOnCache)
            throws DotDataException, JSONException, DotSecurityException {

        ContentletVersionInfo conVerInfo;
        Versionable assetVersion;
        List<Permission> permissionList = new ArrayList<>();
        Language assetLang;
        if(langId <= 0) {
            assetLang = langAPI.getDefaultLanguage();
        } else {
            assetLang = langAPI.getLanguage(langId);
            if(assetLang == null) {
                assetLang = langAPI.getDefaultLanguage();
            }
        }

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
                conVerInfo = versionableAPI.getContentletVersionInfo(id,assetLang.getId());

                if (conVerInfo != null) {
                    generatedOutput = new JSONObject();
                    generatedOutput.append("WorkingVersionGivenLanguage","Found");
                    generatedOutput.append("LanguageId",conVerInfo.getLang());
                    output.add(generatedOutput);
                    Contentlet cachedContent = cc.get(conVerInfo.getWorkingInode());
                    permissionList = permAPI.getPermissions(cachedContent);
                } else {
                    //No Working version for content.
                    generatedOutput = new JSONObject();
                    generatedOutput.append("WorkingVersionGivenLanguage","Not Found");
                    output.add(generatedOutput);
                }

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
                generatedOutput.append(i + "_PermissionableTypeFromCache",p.getClass().getName());
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
                    generatedOutput.append(k + "_PermissionableTypeFromDB", permissionFromDB.get(k).get("permission_type"));
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
    private List<Map<String, String>> findIdentifierObjectInDB (String id) throws DotDataException {
        DotConnect dc = new DotConnect();
        dc.setSQL("SELECT * FROM identifier WHERE id = ?");
        dc.addParam(id);
        return dc.loadResults();
    }

    /**
     * Retrieve the Permission Object from DB
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