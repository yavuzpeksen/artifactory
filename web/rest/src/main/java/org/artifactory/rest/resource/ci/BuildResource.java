/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.rest.resource.ci;

import com.google.common.base.Function;
import com.google.common.collect.Sets;
import com.sun.jersey.api.core.ExtendedUriInfo;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.rest.AuthorizationRestException;
import org.artifactory.addon.rest.RestAddon;
import org.artifactory.api.build.BuildNumberComparator;
import org.artifactory.api.build.BuildService;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.repo.exception.ItemNotFoundRuntimeException;
import org.artifactory.api.rest.artifact.PromotionResult;
import org.artifactory.api.rest.build.BuildInfo;
import org.artifactory.api.rest.build.Builds;
import org.artifactory.api.rest.build.BuildsByName;
import org.artifactory.api.rest.constant.BuildRestConstants;
import org.artifactory.api.rest.constant.RestConstants;
import org.artifactory.api.search.SearchService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.build.BuildRun;
import org.artifactory.exception.CancelException;
import org.artifactory.rest.common.list.StringList;
import org.artifactory.rest.util.RestUtils;
import org.artifactory.sapi.common.RepositoryRuntimeException;
import org.artifactory.util.DoesNotExistException;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildRetention;
import org.jfrog.build.api.dependency.BuildPatternArtifacts;
import org.jfrog.build.api.dependency.BuildPatternArtifactsRequest;
import org.jfrog.build.api.release.Promotion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.transform;
import static org.artifactory.rest.util.RestUtils.getServletContextUrl;

/**
 * A resource to manage the build actions
 *
 * @author Noam Y. Tenne
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Path(BuildRestConstants.PATH_ROOT)
@RolesAllowed({AuthorizationService.ROLE_ADMIN, AuthorizationService.ROLE_USER})
public class BuildResource {

    @Autowired
    private AddonsManager addonsManager;

    @Autowired
    private BuildService buildService;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private SearchService searchService;

    @Context
    private HttpServletRequest request;

    @Context
    private HttpServletResponse response;

    @Context
    private ExtendedUriInfo uriInfo;

    private static final Logger log = LoggerFactory.getLogger(BuildResource.class);

    /**
     * Assemble all, last created, available builds with the last
     *
     * @return Builds json object
     */
    @GET
    @Produces({BuildRestConstants.MT_BUILDS, MediaType.APPLICATION_JSON})
    public Builds getAllBuilds() throws IOException {
        Set<BuildRun> latestBuildsByName = searchService.getLatestBuilds();
        if (!latestBuildsByName.isEmpty()) {
            //Add our builds to the list of build resources
            Builds builds = new Builds();
            builds.slf = RestUtils.getBaseBuildsHref(request);

            for (BuildRun buildRun : latestBuildsByName) {
                String buildHref = RestUtils.getBuildRelativeHref(buildRun.getName());
                builds.builds.add(new Builds.Build(buildHref, buildRun.getStarted()));
            }
            return builds;

        }
        String msg = "No builds were found";
        response.sendError(HttpStatus.SC_NOT_FOUND, msg);
        return null;

    }

    /**
     * Get the build name from the request url and assemble all builds under that name.
     *
     * @return BuildsByName json object
     */
    @GET
    @Path("/{buildName: .+}")
    @Produces({BuildRestConstants.MT_BUILDS_BY_NAME, MediaType.APPLICATION_JSON})
    public BuildsByName getAllSpecificBuilds(@PathParam("buildName") String buildName) throws IOException {
        Set<BuildRun> buildsByName;
        try {
            buildsByName = buildService.searchBuildsByName(buildName);
        } catch (RepositoryRuntimeException e) {
            buildsByName = Sets.newHashSet();
        }
        if (!buildsByName.isEmpty()) {
            BuildsByName builds = new BuildsByName();
            builds.slf = RestUtils.getBaseBuildsHref(request) + RestUtils.getBuildRelativeHref(buildName);
            for (BuildRun buildRun : buildsByName) {
                String versionHref = RestUtils.getBuildNumberRelativeHref(buildRun.getNumber());
                builds.buildsNumbers.add(new BuildsByName.Build(versionHref, buildRun.getStarted()));
            }
            return builds;
        }
        String msg = String.format("No build was found for build name: %s", buildName);
        response.sendError(HttpStatus.SC_NOT_FOUND, msg);
        return null;
    }

    /**
     * Get the build name and number from the request url and send back the exact build for those parameters
     *
     * @return BuildInfo json object
     */
    @GET
    @Path("/{buildName: .+}/{buildNumber: .+}")
    @Produces({BuildRestConstants.MT_BUILD_INFO, BuildRestConstants.MT_BUILDS_DIFF, MediaType.APPLICATION_JSON})
    public Response getBuildInfo(
            @PathParam("buildName") String buildName,
            @PathParam("buildNumber") String buildNumber,
            @QueryParam("started") String buildStarted,
            @QueryParam("diff") String diffNumber) throws IOException {

        if (!authorizationService.canDeployToLocalRepository()) {
            throw new AuthorizationRestException();
        }

        Build build = null;
        if (StringUtils.isNotBlank(buildStarted)) {
            BuildRun buildRun = buildService.getBuildRun(buildName, buildNumber, buildStarted);
            if (buildRun != null) {
                build = buildService.getBuild(buildRun);
            }
        } else {
            build = buildService.getLatestBuildByNameAndNumber(buildName, buildNumber);
        }

        if (build == null) {
            String msg = String.format("No build was found for build name: %s, build number: %s %s",
                    buildName, buildNumber,
                    StringUtils.isNotBlank(buildStarted) ? ", build started: " + buildStarted : "");
            response.sendError(HttpStatus.SC_NOT_FOUND, msg);
            return null;
        }

        if (queryParamsContainKey("diff")) {
            Build secondBuild = buildService.getLatestBuildByNameAndNumber(buildName, diffNumber);
            if (secondBuild == null) {
                String msg = String.format("No build was found for build name: %s , build number: %s ", buildName,
                        diffNumber);
                response.sendError(HttpStatus.SC_NOT_FOUND, msg);
                return null;
            }
            BuildRun buildRun = buildService.getBuildRun(build.getName(), build.getNumber(), build.getStarted());
            BuildRun secondBuildRun = buildService.getBuildRun(secondBuild.getName(), secondBuild.getNumber(),
                    secondBuild.getStarted());
            BuildNumberComparator comparator = new BuildNumberComparator();
            if (comparator.compare(buildRun, secondBuildRun) < 0) {
                response.sendError(HttpStatus.SC_BAD_REQUEST,
                        "Build number should be greater than the build number to compare against.");
                return null;
            }
            return prepareBuildDiffResponse(build, secondBuild, request);
        } else {
            return prepareGetBuildResponse(build);
        }
    }

    private Response prepareGetBuildResponse(Build build) throws IOException {
        BuildInfo buildInfo = new BuildInfo();
        buildInfo.slf = RestUtils.getBuildInfoHref(request, build.getName(), build.getNumber());
        buildInfo.buildInfo = build;

        return Response.ok(buildInfo).build();
    }

    private Response prepareBuildDiffResponse(Build firstBuild, Build secondBuild, HttpServletRequest request) {
        RestAddon restAddon = addonsManager.addonByType(RestAddon.class);
        return restAddon.getBuildsDiff(firstBuild, secondBuild, request);
    }

    /**
     * Returns the outputs of build matching the request
     *
     * @param buildPatternArtifactsRequests contains build name and build number or keyword
     * @return build outputs (build dependencies and generated artifacts).
     *         The returned array will always be the same size as received, returning nulls on non-found builds.
     */
    @POST
    @Path("/patternArtifacts")
    @Consumes({BuildRestConstants.MT_BUILD_PATTERN_ARTIFACTS_REQUEST, RestConstants.MT_LEGACY_ARTIFACTORY_APP,
            MediaType.APPLICATION_JSON})
    @Produces({BuildRestConstants.MT_BUILD_PATTERN_ARTIFACTS_RESULT, MediaType.APPLICATION_JSON})
    public List<BuildPatternArtifacts> getBuildPatternArtifacts(
            final List<BuildPatternArtifactsRequest> buildPatternArtifactsRequests) {
        final RestAddon restAddon = addonsManager.addonByType(RestAddon.class);
        final String contextUrl = getServletContextUrl(request);
        return transform(buildPatternArtifactsRequests,
                new Function<BuildPatternArtifactsRequest, BuildPatternArtifacts>() {
                    @Override
                    public BuildPatternArtifacts apply(BuildPatternArtifactsRequest input) {
                        return restAddon.getBuildPatternArtifacts(input, contextUrl);
                    }
                });
    }


    /**
     * Adds the given build information to the DB
     *
     * @param build Build to add
     */
    @PUT
    @Consumes({BuildRestConstants.MT_BUILD_INFO, RestConstants.MT_LEGACY_ARTIFACTORY_APP, MediaType.APPLICATION_JSON})
    public void addBuild(Build build) throws Exception {
        log.info("Adding build '{} #{}'", build.getName(), build.getNumber());
        if (!authorizationService.canDeployToLocalRepository()) {
            response.sendError(HttpStatus.SC_FORBIDDEN);
            return;
        }

        try {
            buildService.addBuild(build);
        } catch (CancelException e) {
            if (log.isDebugEnabled()) {
                log.debug("An error occurred while adding the build '" + build.getName() + " #" + build.getNumber() +
                        "'.", e);
            }
            response.sendError(e.getErrorCode(), e.getMessage());
            return;
        }
        log.info("Added build '{} #{}'", build.getName(), build.getNumber());
        BuildRetention retention = build.getBuildRetention();
        if (retention != null) {
            RestAddon restAddon = addonsManager.addonByType(RestAddon.class);
            MultiStatusHolder multiStatusHolder = new MultiStatusHolder();
            restAddon.discardOldBuilds(build.getName(), retention, multiStatusHolder);
            if (multiStatusHolder.hasErrors()) {
                response.sendError(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Errors have occurred while maintaining " +
                        "build retention. Please review the system logs for further information.");
            } else if (multiStatusHolder.hasWarnings()) {
                response.sendError(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Warnings have been produced while " +
                        "maintaining build retention. Please review the system logs for further information.");
            }
        }
    }

    /**
     * Promotes a build
     *
     * @param buildName   Name of build to promote
     * @param buildNumber Number of build to promote
     * @param promotion   Promotion settings
     * @return Promotion result
     */
    @POST
    @Path("/promote/{buildName: .+}/{buildNumber: .+}")
    @Consumes({BuildRestConstants.MT_PROMOTION_REQUEST, MediaType.APPLICATION_JSON})
    @Produces({BuildRestConstants.MT_PROMOTION_RESULT, MediaType.APPLICATION_JSON})
    public Response promote(
            @PathParam("buildName") String buildName,
            @PathParam("buildNumber") String buildNumber, Promotion promotion) throws IOException {
        RestAddon restAddon = addonsManager.addonByType(RestAddon.class);
        try {
            if (RestUtils.shouldDecodeParams(request)) {
                buildName = URLDecoder.decode(buildName, "UTF-8");
                buildNumber = URLDecoder.decode(buildNumber, "UTF-8");
            }
            PromotionResult promotionResult = restAddon.promoteBuild(buildName, buildNumber, promotion);
            return Response.status(promotionResult.errorsOrWarningHaveOccurred() ?
                    HttpStatus.SC_BAD_REQUEST : HttpStatus.SC_OK).entity(promotionResult).build();
        } catch (IllegalArgumentException | ItemNotFoundRuntimeException iae) {
            response.sendError(HttpStatus.SC_BAD_REQUEST, iae.getMessage());
        } catch (DoesNotExistException dnee) {
            response.sendError(HttpStatus.SC_NOT_FOUND, dnee.getMessage());
        } catch (ParseException pe) {
            response.sendError(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Unable to parse given build start date: " +
                    pe.getMessage());
        }
        return null;
    }

    /**
     * Renames structure, content and properties of build info objects
     *
     * @param to Replacement build name
     */
    @POST
    @Path("/rename/{buildName: .+}")
    public String renameBuild(
            @PathParam("buildName") String buildName,
            @QueryParam("to") String to) throws IOException {
        RestAddon restAddon = addonsManager.addonByType(RestAddon.class);
        try {
            String from;
            if (RestUtils.shouldDecodeParams(request)) {
                from = URLDecoder.decode(buildName, "UTF-8");
            } else {
                from = buildName;
            }
            restAddon.renameBuilds(from, to);

            response.setStatus(HttpStatus.SC_OK);

            return String.format("Build renaming of '%s' to '%s' was successfully started.\n", from, to);
        } catch (IllegalArgumentException iae) {
            response.sendError(HttpStatus.SC_BAD_REQUEST, iae.getMessage());
        } catch (DoesNotExistException dnne) {
            response.sendError(HttpStatus.SC_NOT_FOUND, dnne.getMessage());
        }

        return null;
    }

    /**
     * Removes the build with the given name and number
     *
     * @return Status message
     */
    @DELETE
    @Path("/{buildName: .+}")
    public void deleteBuilds(
            @PathParam("buildName") String buildName,
            @QueryParam("artifacts") int artifacts,
            @QueryParam("buildNumbers") StringList buildNumbers,
            @QueryParam("deleteAll") int deleteAll) throws IOException {
        RestAddon restAddon = addonsManager.addonByType(RestAddon.class);
        try {
            if (RestUtils.shouldDecodeParams(request)) {
                buildName = URLDecoder.decode(buildName, "UTF-8");
            }

            restAddon.deleteBuilds(response, buildName, buildNumbers, artifacts, deleteAll);
        } catch (IllegalArgumentException iae) {
            response.sendError(HttpStatus.SC_BAD_REQUEST, iae.getMessage());
        } catch (DoesNotExistException dnne) {
            response.sendError(HttpStatus.SC_NOT_FOUND, dnne.getMessage());
        }
        response.flushBuffer();
    }

    private boolean queryParamsContainKey(String key) {
        MultivaluedMap<String, String> queryParameters = queryParams();
        return queryParameters.containsKey(key);
    }

    private MultivaluedMap<String, String> queryParams() {
        return uriInfo.getQueryParameters();
    }
}
