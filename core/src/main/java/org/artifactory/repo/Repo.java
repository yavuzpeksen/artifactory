/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.repo;

import org.artifactory.api.repo.exception.FileExpectedException;
import org.artifactory.common.ResourceStreamHandle;
import org.artifactory.descriptor.DescriptorAware;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.jcr.md.MetadataService;
import org.artifactory.repo.context.RequestContext;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.resource.RepoResource;

import java.io.IOException;
import java.io.Serializable;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public interface Repo<T extends RepoDescriptor> extends DescriptorAware<T>, Serializable {
    String getKey();

    String getDescription();

    void init();

    void destroy();

    /**
     * @see org.artifactory.descriptor.repo.RepoDescriptor#isReal()
     */
    boolean isReal();

    boolean isLocal();

    boolean isCache();

    InternalRepositoryService getRepositoryService();

    MetadataService getMetadataService();

    /**
     * Returns the resource info (not the resource content but the metadata)
     *
     * @param context Additional parameters to pass to the repository.
     * @return RepoResource. UnfoundRepoResource will be returned if the resource not found in this repo.
     */
    RepoResource getInfo(RequestContext context) throws FileExpectedException;

    /**
     * Get the checksum of the resource based on the repository checksum policy (if there is one).
     *
     * @param checksumPath The url to the checksum file
     * @param res          The repo resource of the file the checksum is requested on
     * @return The checksum value.
     * @throws IOException If tried and failed to retrieve the checksum from db/remote source.
     */
    String getChecksum(String checksumPath, RepoResource res) throws IOException;

    ResourceStreamHandle getResourceStreamHandle(RepoResource res) throws IOException, FileExpectedException;
}