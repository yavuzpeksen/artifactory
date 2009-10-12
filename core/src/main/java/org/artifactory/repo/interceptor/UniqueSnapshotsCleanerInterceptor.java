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

package org.artifactory.repo.interceptor;

import org.apache.commons.collections15.SortedBag;
import org.apache.commons.collections15.bag.TreeBag;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.jcr.JcrRepoService;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.log.LoggerFactory;
import org.artifactory.maven.MavenModelUtils;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.jcr.StoringRepo;
import org.artifactory.resource.FileResource;
import org.artifactory.resource.RepoResource;
import org.artifactory.spring.InternalContextHelper;
import org.slf4j.Logger;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author yoav
 */
public class UniqueSnapshotsCleanerInterceptor implements RepoInterceptor {
    private static final Logger log = LoggerFactory.getLogger(UniqueSnapshotsCleanerInterceptor.class);

    public InputStream beforeResourceSave(RepoResource res, StoringRepo repo, InputStream in) throws Exception {
        return in;
    }

    /**
     * Cleanup old snapshots etc.
     *
     * @param fsItem
     * @param statusHolder
     */
    @SuppressWarnings({"OverlyComplexMethod"})
    public void onCreate(JcrFsItem fsItem, StatusHolder statusHolder) {
        StoringRepo repo = fsItem.getRepo();
        if (!(repo instanceof LocalRepo) || fsItem.isDirectory()) {
            return;
        }
        //If the resource has no size specified, this will update the size
        //(this can happen if we established the resource based on a HEAD request that failed to
        //return the content-length).
        RepoResource res = new FileResource((FileInfo) fsItem.getInfo());
        String path = res.getRepoPath().getPath();
        JcrFile file = repo.getLockedJcrFile(path, true);
        if (!MavenNaming.isUniqueSnapshot(path) || MavenNaming.isPom(path)) {
            return;
        }
        int maxUniqueSnapshots = ((LocalRepo) repo).getMaxUniqueSnapshots();
        if (maxUniqueSnapshots > 0) {
            //Read the last updated and delete old unique snapshot artifacts
            JcrFolder snapshotFolder = file.getParentFolder();
            JcrRepoService jcrRepoService = InternalContextHelper.get().getJcrRepoService();
            List<JcrFsItem> children = jcrRepoService.getChildren(snapshotFolder, true);
            List<ItemDesc> itemsByDate = new ArrayList<ItemDesc>();
            for (JcrFsItem child : children) {
                String name = child.getName();
                if (MavenNaming.isUniqueSnapshotFileName(name)) {
                    String childTimeStamp = MavenNaming.getUniqueSnapshotVersionTimestamp(name);
                    Date childLastUpdated = MavenModelUtils.timestampToDate(childTimeStamp);
                    //Add it to the sorted set - newer items closer to the head
                    ItemDesc newDesc = new ItemDesc(child, childLastUpdated);
                    itemsByDate.add(newDesc);
                }
            }
            Collections.sort(itemsByDate);
            //Traverse the ordered collection and stop when we collected enough items (maxUniqueSnapshots)
            int uniqueSnapshotsCount = 0;
            SortedBag<ItemDesc> itemsToKeep = new TreeBag<ItemDesc>();
            for (ItemDesc item : itemsByDate) {
                if (itemsToKeep.size() == 0 || item.compareTo(itemsToKeep.last()) != 0) {
                    uniqueSnapshotsCount++;
                }
                itemsToKeep.add(item);
                if (uniqueSnapshotsCount == maxUniqueSnapshots) {
                    break;
                }
            }
            itemsByDate.removeAll(itemsToKeep);
            for (ItemDesc itemDesc : itemsByDate) {
                itemDesc.item.bruteForceDelete();
                if (log.isInfoEnabled()) {
                    log.info("Removed old unique snapshot '" + itemDesc.item.getRelativePath() + "'.");
                }
            }
        }
    }

    public void onDelete(JcrFsItem fsItem, StatusHolder statusHolder) {
        //Nothing
    }

    private static class ItemDesc implements Comparable<ItemDesc>, Serializable {
        final JcrFsItem item;
        final Date lastModified;

        ItemDesc(JcrFsItem item, Date lastModified) {
            this.item = item;
            this.lastModified = lastModified;
        }

        public int compareTo(ItemDesc o) {
            return -1 * lastModified.compareTo(o.lastModified);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ItemDesc)) {
                return false;
            }
            ItemDesc itemDesc = (ItemDesc) o;
            return item.equals(itemDesc.item) && lastModified.equals(itemDesc.lastModified);

        }

        @Override
        public int hashCode() {
            int result;
            result = item.hashCode();
            result = 31 * result + lastModified.hashCode();
            return result;
        }
    }
}