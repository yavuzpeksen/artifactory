/*
 * This file is part of Artifactory.
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

import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.exception.FileExpectedException;
import org.artifactory.common.ConstantValues;
import org.artifactory.descriptor.repo.ChecksumPolicyType;
import org.artifactory.descriptor.repo.LocalCacheRepoDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.io.checksum.policy.ChecksumPolicy;
import org.artifactory.io.checksum.policy.ChecksumPolicyBase;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.context.RequestContext;
import org.artifactory.repo.jcr.JcrRepoBase;
import org.artifactory.resource.ExpiredRepoResource;
import org.artifactory.resource.RepoResource;
import org.slf4j.Logger;

public class JcrCacheRepo extends JcrRepoBase<LocalCacheRepoDescriptor> implements LocalCacheRepo {
    private static final Logger log = LoggerFactory.getLogger(LocalCacheRepo.class);

    private RemoteRepo remoteRepo;

    private ChecksumPolicy checksumPolicy;

    public JcrCacheRepo(RemoteRepo<? extends RemoteRepoDescriptor> remoteRepo) {
        super(remoteRepo.getRepositoryService());
        this.remoteRepo = remoteRepo;
        // create descriptor on-the-fly since this repo is created by a remote repo
        LocalCacheRepoDescriptor descriptor = new LocalCacheRepoDescriptor();
        descriptor.setDescription(remoteRepo.getDescription() + " (local file cache)");
        descriptor.setKey(remoteRepo.getKey() + PATH_SUFFIX);
        descriptor.setRemoteRepo(remoteRepo.getDescriptor());
        setDescriptor(descriptor);
        ChecksumPolicyType checksumPolicyType = remoteRepo.getDescriptor().getChecksumPolicyType();
        checksumPolicy = ChecksumPolicyBase.getByType(checksumPolicyType);
    }

    @Override
    public RepoResource getInfo(RequestContext context) throws FileExpectedException {
        RepoResource repoResource = super.getInfo(context);
        if (repoResource.isFound()) {
            //Check for expiry
            boolean expired = isExpired(repoResource);
            if (expired) {
                log.debug("Returning expired resource {} ", context.getResourcePath());
                repoResource = new ExpiredRepoResource(repoResource);
            } else {
                log.debug("Returning cached resource {}.", context.getResourcePath());
            }
        }
        return repoResource;
    }

    @Override
    public boolean isCache() {
        return true;
    }

    @Override
    public int getMaxUniqueSnapshots() {
        return remoteRepo.getMaxUniqueSnapshots();
    }

    public RemoteRepo getRemoteRepo() {
        return remoteRepo;
    }

    @Override
    public ChecksumPolicy getChecksumPolicy() {
        return checksumPolicy;
    }

    public void onCreate(JcrFsItem fsItem) {
    }

    public void unexpire(String path) {
        // TODO: Change this mechanism since the last updated is used for artifact popularity measurement
        //Reset the resource age so it is kept being cached
        JcrFsItem item = getLockedJcrFsItem(path);
        item.setLastUpdated(System.currentTimeMillis());
        log.debug("Unexpired '{}' from local cache '{}'.", path, getKey());
    }

    public void zap(RepoPath repoPath) {
        //Zap all nodes recursively from all retrieval caches
        JcrFsItem fsItem = getLockedJcrFsItem(repoPath);
        if (fsItem != null && !fsItem.isDeleted()) {
            // Exists and not deleted... Let's roll
            long retrievalCahePeriodMillis = remoteRepo.getRetrievalCachePeriodSecs() * 1000L;
            long expiredLastUpdated = System.currentTimeMillis() - retrievalCahePeriodMillis;
            int itemsZapped = fsItem.zap(expiredLastUpdated);
            // now remove all the caches related to this path and any sub paths
            remoteRepo.removeFromCaches(fsItem.getRelativePath(), true);
            log.info("Zapped '{}' from local cache: {} items zapped.", repoPath, itemsZapped);
        }
    }

    @Override
    public void updateCache(JcrFsItem fsItem) {
        super.updateCache(fsItem);
        remoteRepo.removeFromCaches(fsItem.getRelativePath(), false);
    }

    /**
     * Check that the item has not expired yet, unless it's a release which never expires or a unique snapshot.
     *
     * @param repoResource The resource to check for expiery
     * @return boolean - True is resource is expired. False if not
     */
    protected boolean isExpired(RepoResource repoResource) {
        String path = repoResource.getRepoPath().getPath();
        //Never expire releases, unique snapshots and non-snapshot metadata
        if (MavenNaming.isRelease(path)) {
            return false;
        } else if (MavenNaming.isUniqueSnapshot(path)) {
            return false;
        } else if (MavenNaming.isMavenMetadata(path) && !MavenNaming.isSnapshotMavenMetadata(path)) {
            return false;
        }
        long retrievalCahePeriodMillis;
        retrievalCahePeriodMillis = getRetrievalCahePeriodMillis(path);
        long cacheAge = repoResource.getCacheAge();
        return cacheAge > retrievalCahePeriodMillis || cacheAge == -1;
    }

    private long getRetrievalCahePeriodMillis(String path) {
        long retrievalCahePeriodMillis;
        if (MavenNaming.isIndex(path) &&
                remoteRepo.getUrl().contains(ConstantValues.mvnCentralHostPattern.getString())) {
            //If it is a central maven index use the hardcoded cache value
            long centralmaxQueryIntervalSecs = ConstantValues.mvnCentralIndexerMaxQueryIntervalSecs.getLong();
            retrievalCahePeriodMillis = centralmaxQueryIntervalSecs * 1000L;
        } else {
            //It is a non-unique snapshot or snapshot metadata
            retrievalCahePeriodMillis = remoteRepo.getRetrievalCachePeriodSecs() * 1000L;
        }
        return retrievalCahePeriodMillis;
    }
}