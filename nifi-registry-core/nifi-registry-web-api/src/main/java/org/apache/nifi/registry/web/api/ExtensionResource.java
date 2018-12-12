/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.registry.web.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import org.apache.nifi.registry.event.EventService;
import org.apache.nifi.registry.extension.bundle.BundleType;
import org.apache.nifi.registry.extension.bundle.BundleTypeValues;
import org.apache.nifi.registry.extension.component.ExtensionFilterParams;
import org.apache.nifi.registry.extension.component.ExtensionMetadata;
import org.apache.nifi.registry.extension.component.ExtensionMetadataContainer;
import org.apache.nifi.registry.extension.component.TagCount;
import org.apache.nifi.registry.extension.component.manifest.ExtensionType;
import org.apache.nifi.registry.extension.component.manifest.ProvidedServiceAPI;
import org.apache.nifi.registry.security.authorization.RequestAction;
import org.apache.nifi.registry.service.AuthorizationService;
import org.apache.nifi.registry.service.RegistryService;
import org.apache.nifi.registry.web.link.LinkService;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;

@Component
@Path("/extensions")
@Api(
        value = "extensions",
        description = "Find and retrieve extensions.",
        authorizations = { @Authorization("Authorization") }
)
public class ExtensionResource extends AuthorizableApplicationResource {

    private final RegistryService registryService;
    private final LinkService linkService;

    public ExtensionResource(final AuthorizationService authorizationService,
                             final EventService eventService,
                             final RegistryService registryService,
                             final LinkService linkService) {
        super(authorizationService, eventService);
        this.registryService = registryService;
        this.linkService = linkService;
    }

    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Gets all extensions that match the filter params and are part of bundles located in buckets the current user is authorized for.",
            response = ExtensionMetadataContainer.class,
            responseContainer = "List"
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403),
            @ApiResponse(code = 404, message = HttpStatusMessages.MESSAGE_404),
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409) })
    public Response getExtensions(
            @QueryParam("bundleType")
            @ApiParam(value = "The type of bundles to return", allowableValues = BundleTypeValues.ALL_VALUES)
                final BundleType bundleType,
            @QueryParam("extensionType")
            @ApiParam(value = "The type of extensions to return")
                final ExtensionType extensionType,
            @QueryParam("tag")
            @ApiParam(value = "The tags to filter on, will be used in an OR statement")
                final Set<String> tags
            ) {

        final Set<String> authorizedBucketIds = getAuthorizedBucketIds(RequestAction.READ);
        if (authorizedBucketIds == null || authorizedBucketIds.isEmpty()) {
            // not authorized for any bucket, return empty list of items
            return Response.status(Response.Status.OK).entity(new ArrayList<>()).build();
        }

        final ExtensionFilterParams filterParams = new ExtensionFilterParams.Builder()
                .bundleType(bundleType)
                .extensionType(extensionType)
                .addTags(tags == null ? Collections.emptyList() : tags)
                .build();

        final SortedSet<ExtensionMetadata> extensionMetadata = registryService.getExtensionMetadata(authorizedBucketIds, filterParams);
        linkService.populateLinks(extensionMetadata);

        final ExtensionMetadataContainer container = new ExtensionMetadataContainer();
        container.setExtensions(extensionMetadata);
        container.setNumResults(extensionMetadata.size());
        container.setFilterParams(filterParams);

        return Response.status(Response.Status.OK).entity(container).build();
    }

    @GET
    @Path("provided-service-api")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Gets all extensions that provide the specified API and are part of bundles located in buckets the current user is authorized for.",
            response = ExtensionMetadataContainer.class,
            responseContainer = "List"
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403),
            @ApiResponse(code = 404, message = HttpStatusMessages.MESSAGE_404),
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409) })
    public Response getExtensionsProvidingServiceAPI(
            @QueryParam("className")
            @ApiParam(value = "The name of the service API class", required = true)
                final String className,
            @QueryParam("groupId")
            @ApiParam(value = "The groupId of the bundle containing the service API class", required = true)
                final String groupId,
            @QueryParam("artifactId")
            @ApiParam(value = "The artifactId of the bundle containing the service API class", required = true)
                final String artifactId,
            @QueryParam("version")
            @ApiParam(value = "The version of the bundle containing the service API class", required = true)
                final String version
    ) {

        final Set<String> authorizedBucketIds = getAuthorizedBucketIds(RequestAction.READ);
        if (authorizedBucketIds == null || authorizedBucketIds.isEmpty()) {
            // not authorized for any bucket, return empty list of items
            return Response.status(Response.Status.OK).entity(new ArrayList<>()).build();
        }

        final ProvidedServiceAPI serviceAPI = new ProvidedServiceAPI();
        serviceAPI.setClassName(className);
        serviceAPI.setGroupId(groupId);
        serviceAPI.setArtifactId(artifactId);
        serviceAPI.setVersion(version);

        final SortedSet<ExtensionMetadata> extensionMetadata = registryService.getExtensionMetadata(authorizedBucketIds, serviceAPI);
        linkService.populateLinks(extensionMetadata);

        final ExtensionMetadataContainer container = new ExtensionMetadataContainer();
        container.setExtensions(extensionMetadata);
        container.setNumResults(extensionMetadata.size());

        return Response.status(Response.Status.OK).entity(container).build();
    }

    @GET
    @Path("/tags")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Gets all the extension tags known to this NiFi Registry instance.",
            response = TagCount.class,
            responseContainer = "List"
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 401, message = HttpStatusMessages.MESSAGE_401),
            @ApiResponse(code = 403, message = HttpStatusMessages.MESSAGE_403),
            @ApiResponse(code = 404, message = HttpStatusMessages.MESSAGE_404),
            @ApiResponse(code = 409, message = HttpStatusMessages.MESSAGE_409) })
    public Response getTags() {
        final SortedSet<TagCount> tags = registryService.getExtensionTags();
        return Response.status(Response.Status.OK).entity(tags).build();
    }

}
