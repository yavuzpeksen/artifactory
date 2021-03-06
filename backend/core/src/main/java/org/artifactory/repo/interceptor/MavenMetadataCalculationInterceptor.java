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

package org.artifactory.repo.interceptor;

import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.module.ModuleInfo;
import org.artifactory.common.MutableStatusHolder;
import org.artifactory.descriptor.repo.SnapshotVersionBehavior;
import org.artifactory.mime.MavenNaming;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.interceptor.storage.StorageInterceptorAdapter;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.sapi.fs.VfsItem;
import org.artifactory.storage.fs.VfsItemFactory;
import org.artifactory.util.RepoPathUtils;

/**
 * Interceptor which handles maven metadata calculation upon creation and removal
 *
 * @author Noam Tenne
 */
public class MavenMetadataCalculationInterceptor extends StorageInterceptorAdapter {

    /**
     * If the newly created item is a pom file, this method will calculate the maven metadata of it's parent folder
     *
     * @param fsItem       Newly created item
     * @param statusHolder StatusHolder
     */
    //@Async(delayUntilAfterCommit = true, transactional = true, shared = true, failIfNotScheduledFromTransaction = true)
    @Override
    public void afterCreate(VfsItem fsItem, MutableStatusHolder statusHolder) {
        if (shouldRecalculateOnCreate(fsItem)) {
            // calculate maven metadata on the grandparent folder (the artifact id node)
            if (MavenNaming.isUniqueSnapshot(fsItem.getPath())) {
                // unique snapshots require instant metadata calculation since it is used to calculate future snapshots
                RepoPath parentFolder = fsItem.getRepoPath().getParent();
                ContextHelper.get().getRepositoryService().calculateMavenMetadata(parentFolder);
            }

            if (isPomFile(fsItem)) {
                // for pom files we need to trigger metadata calculation on the grandparent -
                // potential new version and snapshot.
                // this can be done asynchronously since it's usually not requires instant update
                RepoPath grandparentFolder = RepoPathUtils.getAncestor(fsItem.getRepoPath(), 2);
                ContextHelper.get().getRepositoryService().calculateMavenMetadataAsync(grandparentFolder);
            }
        }
    }

    private boolean shouldRecalculateOnCreate(VfsItem fsItem) {
        if (!fsItem.isFile()) {
            return false;
        }
        LocalRepo localRepo = ContextHelper.get().beanForType(InternalRepositoryService.class)
                .localOrCachedRepositoryByKey(fsItem.getRepoKey());
        if (localRepo == null || !isLocalNonCachedRepository(localRepo)) {
            return false;
        }

        // it's a local non-cache repository, check the snapshot behavior
        ModuleInfo moduleInfo = localRepo.getItemModuleInfo(fsItem.getPath());
        if (moduleInfo.isIntegration() &&
                SnapshotVersionBehavior.DEPLOYER.equals(localRepo.getMavenSnapshotVersionBehavior())) {
            return false;
        }
        return true;
    }

    /**
     * Checks that the given repo is a local non-cache repo, since it is the only kind that metadata calculation
     * can be performed on.
     *
     * @param localRepo Repo to check
     * @return boolean - True if calculation is allowed on this type of repo. False if not
     */
    private boolean isLocalNonCachedRepository(VfsItemFactory localRepo) {
        return localRepo.isLocal() && (!localRepo.isCache());
    }

    private boolean isPomFile(VfsItem fsItem) {
        return MavenNaming.isPom(fsItem.getRepoPath().getPath());
    }

}
