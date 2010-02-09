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

package org.artifactory.repo.index.locator;

import org.artifactory.api.repo.RepoPath;
import org.artifactory.jcr.JcrPath;
import org.artifactory.repo.jcr.StoringRepo;
import org.artifactory.util.PathUtils;

import java.io.File;

/**
 * @author freds
 * @date Oct 24, 2008
 */
public final class ExtensionBasedLocator extends ArtifactoryLocator {
    private final String expectedExtension;

    public ExtensionBasedLocator(StoringRepo repo, String expectedExtension) {
        super(repo);
        this.expectedExtension = expectedExtension;
    }

    public File locate(File source) {
        String sourcePath = source.getAbsolutePath();
        String targetPath = PathUtils.stripExtension(sourcePath) + expectedExtension;
        RepoPath repoPath = JcrPath.get().getRepoPath(targetPath);
        File target = getRepo().getJcrFile(repoPath);
        if (target == null) {
            //Cannot return null - return non existing file
            target = new File(targetPath) {
                @Override
                public boolean exists() {
                    return false;
                }
            };
        }
        return target;
    }
}