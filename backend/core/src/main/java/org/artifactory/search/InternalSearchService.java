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

package org.artifactory.search;

import org.artifactory.api.repo.Async;
import org.artifactory.api.search.ItemSearchResults;
import org.artifactory.api.search.SearchService;
import org.artifactory.api.search.deployable.VersionUnitSearchControls;
import org.artifactory.api.search.deployable.VersionUnitSearchResult;
import org.artifactory.jcr.JcrSearchService;
import org.artifactory.repo.RepoPath;
import org.artifactory.sapi.common.Lock;
import org.artifactory.spring.ContextReadinessListener;
import org.artifactory.spring.ReloadableBean;

import javax.jcr.RepositoryException;
import java.util.List;

/**
 * @author Noam Tenne
 */
public interface InternalSearchService extends JcrSearchService, SearchService, ReloadableBean,
        ContextReadinessListener {

    @Lock(transactional = true)
    void transactionalIndexMarkedArchives();

    void markArchivesForIndexing(boolean force);

    void markArchivesForIndexing(RepoPath searchPath, boolean force);

    @Async(delayUntilAfterCommit = true)
    void index(List<RepoPath> archiveRepoPaths);

    @Lock(transactional = true)
    void index(RepoPath archiveRepoPath);

    @Async(delayUntilAfterCommit = true)
    void asyncIndex(RepoPath archiveRepoPath);

    /**
     * Searches for version units within the given path
     *
     * @param controls Search controls
     * @return Search results
     */
    @Lock(transactional = true)
    ItemSearchResults<VersionUnitSearchResult> searchVersionUnits(VersionUnitSearchControls controls)
            throws RepositoryException;
}