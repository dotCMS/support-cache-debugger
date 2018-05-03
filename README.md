
# README
----
This is an OSGi Plugin that enables certain REST Endpoints for Troubleshooting purposes


## How to build this example
----

To install all you need to do is build the JAR. To do this run from this directory:

`./gradlew jar`

or for windows

`.\gradlew.bat jar`

This will build a jar in the build/libs directory

### To install this bundle

Copy the bundle jar file inside the Felix OSGI container (dotCMS/felix/load).
        OR
Upload the bundle jar file using the dotCMS UI (CMS Admin->Dynamic Plugins->Upload Plugin).

### To uninstall this bundle:

Remove the bundle jar file from the Felix OSGI container (dotCMS/felix/load).
        OR
Undeploy the bundle using the dotCMS UI (CMS Admin->Dynamic Plugins->Undeploy).



## How to test
----

Once installed, you can access this resource by (this assumes you are on localhost)

`http://localhost:8080/api/supportDebugger/cacheKey/{anyAssetIdentifier}`

This will return some basic information of an asset, like

- Asset Identifier.
- Permissions Stored in Cache.

`http://localhost:8080/api/supportDebugger/assetId/{anyAssetIdentifier}`

This will return some basic information of an asset, like

- Asset Identifier.
- Permissions Stored directly in DB. In case this asset inherits permissions from a parent permissionable, it will permissions from it instead.

`http://localhost:8080/api/supportDebugger/fullReindex/amountOfShards/{amountOfShards}`

This will either kick a full reindex, or return progress of current reindexation. This requires authentication in request headers.

You can try running via curl command:

`curl -u admin@dotcms.com:admin -XGET http://localhost:8080/api/supportDebugger/fullReindex/amountOfShards/{amountOfShards}`

